package com.bica.reborn.spectral_sparsify;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for spectral protocol compression (Step 31g).
 * Java port of Python {@code test_spectral_sparsify.py}.
 */
class SpectralSparsifyCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Effective resistance
    // -----------------------------------------------------------------------

    @Test
    void effectiveResistanceEnd() {
        Map<Integer, Double> r = SpectralSparsifyChecker.effectiveResistance(build("end"));
        assertTrue(r.isEmpty());
    }

    @Test
    void effectiveResistanceSingleEdge() {
        Map<Integer, Double> r = SpectralSparsifyChecker.effectiveResistance(build("&{a: end}"));
        assertEquals(1, r.size());
        assertTrue(r.get(0) > 0.0);
    }

    @Test
    void effectiveResistanceChain() {
        Map<Integer, Double> r = SpectralSparsifyChecker.effectiveResistance(build("&{a: &{b: end}}"));
        assertEquals(2, r.size());
        for (double v : r.values()) {
            assertTrue(v > 0.0);
        }
    }

    @Test
    void effectiveResistanceDiamond() {
        Map<Integer, Double> r = SpectralSparsifyChecker.effectiveResistance(
                build("&{a: &{c: end}, b: &{c: end}}"));
        assertTrue(r.size() >= 2);
        for (double v : r.values()) {
            assertTrue(v >= 0.0);
        }
    }

    @Test
    void effectiveResistanceParallel() {
        Map<Integer, Double> r = SpectralSparsifyChecker.effectiveResistance(
                build("(&{a: end} || &{b: end})"));
        assertTrue(r.size() >= 2);
    }

    // -----------------------------------------------------------------------
    // Spectral sparsify
    // -----------------------------------------------------------------------

    @Test
    void sparsifyEnd() {
        var result = SpectralSparsifyChecker.spectralSparsify(build("end"));
        assertNotNull(result);
        assertEquals(0, result.originalEdges());
        assertEquals(0, result.sparsifiedEdges());
    }

    @Test
    void sparsifySimpleBranchNoCompression() {
        var result = SpectralSparsifyChecker.spectralSparsify(build("&{a: end}"));
        assertEquals(result.originalEdges(), result.sparsifiedEdges());
    }

    @Test
    void sparsifyChainNoCompression() {
        var result = SpectralSparsifyChecker.spectralSparsify(build("&{a: &{b: end}}"));
        assertEquals(result.originalEdges(), result.sparsifiedEdges());
        assertEquals(1.0, result.compressionRatio(), 0.001);
    }

    @Test
    void sparsifyDiamondSomeCompression() {
        var result = SpectralSparsifyChecker.spectralSparsify(
                build("&{a: &{c: end}, b: &{c: end}}"));
        assertTrue(result.sparsifiedEdges() <= result.originalEdges());
        assertTrue(result.compressionRatio() <= 1.0);
    }

    @Test
    void sparsifyParallel() {
        var result = SpectralSparsifyChecker.spectralSparsify(
                build("(&{a: end} || &{b: end})"));
        assertNotNull(result);
        assertTrue(result.sparsifiedEdges() <= result.originalEdges());
    }

    @Test
    void fiedlerPreserved() {
        StateSpace ss = build("&{a: &{c: end}, b: &{c: end}}");
        var result = SpectralSparsifyChecker.spectralSparsify(ss, 0.3);
        if (result.fiedlerOriginal() > 1e-10) {
            assertTrue(result.fiedlerRatio() >= 0.5);
        }
    }

    @Test
    void epsilonControlsQuality() {
        StateSpace ss = build("&{a: &{c: end}, b: &{c: end}}");
        var tight = SpectralSparsifyChecker.spectralSparsify(ss, 0.01);
        var loose = SpectralSparsifyChecker.spectralSparsify(ss, 0.9);
        assertTrue(tight.sparsifiedEdges() >= loose.sparsifiedEdges());
    }

    @Test
    void keptEdgesSubset() {
        StateSpace ss = build("&{a: &{c: end}, b: &{c: end}}");
        var result = SpectralSparsifyChecker.spectralSparsify(ss);
        for (int[] e : result.keptEdges()) {
            assertTrue(ss.states().contains(e[0]));
            assertTrue(ss.states().contains(e[1]));
        }
    }

    @Test
    void edgeWeightsPositive() {
        var result = SpectralSparsifyChecker.spectralSparsify(
                build("(&{a: end} || &{b: end})"));
        for (double w : result.edgeWeights()) {
            assertTrue(w > 0.0);
        }
    }

    @Test
    void recursiveSparsify() {
        var result = SpectralSparsifyChecker.spectralSparsify(
                build("rec X . &{a: X, b: end}"));
        assertNotNull(result);
    }

    // -----------------------------------------------------------------------
    // Compression quality
    // -----------------------------------------------------------------------

    @Test
    void identityQuality() {
        StateSpace ss = build("&{a: &{b: end}}");
        List<int[]> edges = SpectralSparsifyChecker.hasseEdges(ss);
        var q = SpectralSparsifyChecker.compressionQuality(ss, edges, null, 0.3);
        assertNotNull(q);
        assertTrue(q.maxDistortion() < 1e-6);
        assertTrue(q.isGoodSparsifier());
    }

    @Test
    void diamondSubgraphQuality() {
        StateSpace ss = build("&{a: &{c: end}, b: &{c: end}}");
        var result = SpectralSparsifyChecker.spectralSparsify(ss);
        var q = SpectralSparsifyChecker.compressionQuality(
                ss, result.keptEdges(), result.edgeWeights(), 0.3);
        assertNotNull(q);
    }

    @Test
    void eigenvalueDistancesNonNeg() {
        StateSpace ss = build("&{a: &{c: end}, b: &{c: end}}");
        var result = SpectralSparsifyChecker.spectralSparsify(ss);
        var q = SpectralSparsifyChecker.compressionQuality(
                ss, result.keptEdges(), result.edgeWeights(), 0.3);
        for (double d : q.eigenvalueDistances()) {
            assertTrue(d >= -1e-10);
        }
    }

    // -----------------------------------------------------------------------
    // Preserve Fiedler
    // -----------------------------------------------------------------------

    @Test
    void fullEdgesPreserveFiedler() {
        StateSpace ss = build("&{a: &{b: end}}");
        List<int[]> edges = SpectralSparsifyChecker.hasseEdges(ss);
        assertTrue(SpectralSparsifyChecker.preserveFiedler(ss, edges, null, 0.1));
    }

    @Test
    void sparsifiedPreservesFiedler() {
        StateSpace ss = build("&{a: &{c: end}, b: &{c: end}}");
        var result = SpectralSparsifyChecker.spectralSparsify(ss, 0.1);
        assertTrue(SpectralSparsifyChecker.preserveFiedler(
                ss, result.keptEdges(), result.edgeWeights(), 0.2));
    }

    @Test
    void emptyEdgesNoPreserve() {
        StateSpace ss = build("&{a: &{b: end}}");
        boolean result = SpectralSparsifyChecker.preserveFiedler(ss, List.of(), 0.1);
        // Either fiedler_orig is 0 or not preserved -- just test it returns a boolean
        assertNotNull(result);
    }

    // -----------------------------------------------------------------------
    // Sparsification potential
    // -----------------------------------------------------------------------

    @Test
    void potentialEnd() {
        var p = SpectralSparsifyChecker.sparsificationPotential(build("end"));
        assertNotNull(p);
        assertEquals(0, p.numEdges());
        assertEquals(0, p.maxRemovable());
    }

    @Test
    void potentialChainNoRemovable() {
        var p = SpectralSparsifyChecker.sparsificationPotential(build("&{a: &{b: end}}"));
        assertEquals(0, p.maxRemovable());
        assertEquals(0.0, p.removableFraction(), 0.001);
    }

    @Test
    void potentialDiamondHasRemovable() {
        var p = SpectralSparsifyChecker.sparsificationPotential(
                build("&{a: &{c: end}, b: &{c: end}}"));
        assertTrue(p.numEdges() >= p.minEdgesNeeded());
    }

    @Test
    void potentialParallel() {
        var p = SpectralSparsifyChecker.sparsificationPotential(
                build("(&{a: end} || &{b: end})"));
        assertTrue(p.numStates() >= 3);
        assertTrue(p.numEdges() >= 2);
    }

    @Test
    void potentialResistancesPopulated() {
        var p = SpectralSparsifyChecker.sparsificationPotential(
                build("&{a: &{c: end}, b: &{c: end}}"));
        assertEquals(p.numEdges(), p.effectiveResistances().size());
    }

    @Test
    void potentialMinEdgesIsNMinus1() {
        var p = SpectralSparsifyChecker.sparsificationPotential(
                build("&{a: &{b: &{c: end}}}"));
        assertEquals(p.numStates() - 1, p.minEdgesNeeded());
    }
}
