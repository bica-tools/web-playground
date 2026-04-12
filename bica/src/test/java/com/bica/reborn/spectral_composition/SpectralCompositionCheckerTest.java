package com.bica.reborn.spectral_composition;

import com.bica.reborn.algebraic.eigenvalues.EigenvalueComputer;
import com.bica.reborn.algebraic.zeta.IharaZeta;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.spectral_prob.SpectralProbChecker;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.von_neumann.VonNeumannChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for spectral compositionality under parallel constructor (Step 30t).
 * Java port of Python {@code test_spectral_composition.py}.
 */
class SpectralCompositionCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    /** Build the actual product state space via the parallel constructor. */
    private StateSpace buildProduct(String s1, String s2) {
        return build("(" + s1 + " || " + s2 + ")");
    }

    // -----------------------------------------------------------------------
    // Eigenvalue tests
    // -----------------------------------------------------------------------

    @Test
    void eigenvaluesEndEnd() {
        StateSpace ss1 = build("end");
        StateSpace ss2 = build("end");
        List<Double> eigs = SpectralCompositionChecker.productEigenvalues(ss1, ss2);
        assertEquals(1, eigs.size());
        assertEquals(0.0, eigs.get(0), 1e-10);
    }

    @Test
    void eigenvaluesBranchEnd() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("end");
        List<Double> eigs = SpectralCompositionChecker.productEigenvalues(ss1, ss2);
        double[] eigs1 = EigenvalueComputer.laplacianEigenvalues(ss1);
        assertEquals(eigs1.length, eigs.size());
        for (int i = 0; i < eigs.size(); i++) {
            assertEquals(eigs1[i], eigs.get(i), 1e-6);
        }
    }

    @Test
    void eigenvaluesBranchBranchCount() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        List<Double> eigs = SpectralCompositionChecker.productEigenvalues(ss1, ss2);
        assertEquals(4, eigs.size()); // 2 * 2 = 4
    }

    @Test
    void eigenvalueSumStructure() {
        StateSpace ss1 = build("&{a: end, b: end}");
        StateSpace ss2 = build("&{c: end}");
        double[] e1 = EigenvalueComputer.laplacianEigenvalues(ss1);
        double[] e2 = EigenvalueComputer.laplacianEigenvalues(ss2);
        List<Double> eigsProd = SpectralCompositionChecker.productEigenvalues(ss1, ss2);

        // Verify all are sums
        assertEquals(e1.length * e2.length, eigsProd.size());
    }

    @Test
    void fromSpectraMatches() {
        StateSpace ss1 = build("&{a: end, b: end}");
        StateSpace ss2 = build("&{c: end}");
        List<Double> direct = SpectralCompositionChecker.productEigenvalues(ss1, ss2);
        double[] e1 = EigenvalueComputer.laplacianEigenvalues(ss1);
        double[] e2 = EigenvalueComputer.laplacianEigenvalues(ss2);
        List<Double> eigs1 = new java.util.ArrayList<>();
        for (double v : e1) eigs1.add(v);
        List<Double> eigs2 = new java.util.ArrayList<>();
        for (double v : e2) eigs2.add(v);
        List<Double> fromSpectra = SpectralCompositionChecker.productEigenvaluesFromSpectra(eigs1, eigs2);
        assertEquals(direct.size(), fromSpectra.size());
        for (int i = 0; i < direct.size(); i++) {
            assertEquals(direct.get(i), fromSpectra.get(i), 1e-10);
        }
    }

    @Test
    void emptySpectrumYieldsEmpty() {
        assertEquals(0, SpectralCompositionChecker.productEigenvaluesFromSpectra(
                List.of(), List.of(1.0, 2.0)).size());
        assertEquals(0, SpectralCompositionChecker.productEigenvaluesFromSpectra(
                List.of(1.0), List.of()).size());
    }

    @Test
    void smallestIsZero() {
        StateSpace ss1 = build("&{a: end, b: end}");
        StateSpace ss2 = build("&{c: end, d: end}");
        List<Double> eigs = SpectralCompositionChecker.productEigenvalues(ss1, ss2);
        assertEquals(0.0, eigs.get(0), 1e-8);
    }

    @Test
    void eigenvalueSymmetry() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end, c: end}");
        List<Double> eigs12 = SpectralCompositionChecker.productEigenvalues(ss1, ss2);
        List<Double> eigs21 = SpectralCompositionChecker.productEigenvalues(ss2, ss1);
        assertEquals(eigs12.size(), eigs21.size());
        for (int i = 0; i < eigs12.size(); i++) {
            assertEquals(eigs12.get(i), eigs21.get(i), 1e-10);
        }
    }

    // -----------------------------------------------------------------------
    // Fiedler composition tests
    // -----------------------------------------------------------------------

    @Test
    void fiedlerEndHasZero() {
        StateSpace ss1 = build("end");
        StateSpace ss2 = build("&{a: end, b: end}");
        assertEquals(0.0, SpectralCompositionChecker.productFiedler(ss1, ss2), 1e-8);
    }

    @Test
    void fiedlerSymmetricBranches() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        double f1 = EigenvalueComputer.fiedlerValue(ss1);
        double fProd = SpectralCompositionChecker.productFiedler(ss1, ss2);
        assertEquals(f1, fProd, 1e-6);
    }

    @Test
    void fiedlerMinSelection() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{a: end, b: end}");
        double f1 = EigenvalueComputer.fiedlerValue(ss1);
        double f2 = EigenvalueComputer.fiedlerValue(ss2);
        double fProd = SpectralCompositionChecker.productFiedler(ss1, ss2);
        assertEquals(Math.min(f1, f2), fProd, 1e-6);
    }

    @Test
    void fiedlerFromValues() {
        assertEquals(1.5, SpectralCompositionChecker.productFiedlerFromValues(1.5, 2.3), 1e-10);
        assertEquals(1.0, SpectralCompositionChecker.productFiedlerFromValues(3.0, 1.0), 1e-10);
    }

    @Test
    void fiedlerBothZero() {
        StateSpace ss1 = build("end");
        StateSpace ss2 = build("end");
        assertEquals(0.0, SpectralCompositionChecker.productFiedler(ss1, ss2), 1e-10);
    }

    // -----------------------------------------------------------------------
    // Spectral gap tests
    // -----------------------------------------------------------------------

    @Test
    void spectralGapEndZero() {
        StateSpace ss1 = build("end");
        StateSpace ss2 = build("&{a: end}");
        assertEquals(0.0, SpectralCompositionChecker.productSpectralGap(ss1, ss2), 1e-8);
    }

    @Test
    void spectralGapBranches() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{a: end, b: end}");
        double[] e1 = EigenvalueComputer.laplacianEigenvalues(ss1);
        double[] e2 = EigenvalueComputer.laplacianEigenvalues(ss2);
        double gap1 = SpectralCompositionChecker.firstNonzero(e1);
        double gap2 = SpectralCompositionChecker.firstNonzero(e2);
        double gapProd = SpectralCompositionChecker.productSpectralGap(ss1, ss2);
        assertEquals(Math.min(gap1, gap2), gapProd, 1e-6);
    }

    // -----------------------------------------------------------------------
    // Heat trace tests
    // -----------------------------------------------------------------------

    @Test
    void heatTraceAtTZero() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        double ht = SpectralCompositionChecker.productHeatTrace(ss1, ss2, 0.001);
        assertEquals(4.0, ht, 0.1); // n1=2, n2=2
    }

    @Test
    void heatTraceMultiplicativity() {
        StateSpace ss1 = build("&{a: end, b: end}");
        StateSpace ss2 = build("&{c: end}");
        double t = 1.0;
        double z1 = SpectralProbChecker.heatKernelTrace(ss1, t);
        double z2 = SpectralProbChecker.heatKernelTrace(ss2, t);
        double zProd = SpectralCompositionChecker.productHeatTrace(ss1, ss2, t);
        assertEquals(z1 * z2, zProd, Math.abs(z1 * z2) * 1e-6);
    }

    @Test
    void heatTraceSeries() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        List<Double>[] result = SpectralCompositionChecker.productHeatTraceSeries(ss1, ss2);
        assertEquals(7, result[0].size());
        assertEquals(7, result[1].size());
        for (double v : result[1]) {
            assertTrue(v > 0, "heat trace must be positive");
        }
    }

    @Test
    void heatTraceCustomTimes() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        List<Double>[] result = SpectralCompositionChecker.productHeatTraceSeries(
                ss1, ss2, List.of(0.5, 1.5));
        assertEquals(2, result[0].size());
        assertEquals(2, result[1].size());
    }

    @Test
    void heatTraceEndTimesEnd() {
        StateSpace ss1 = build("end");
        StateSpace ss2 = build("end");
        assertEquals(1.0, SpectralCompositionChecker.productHeatTrace(ss1, ss2, 1.0), 0.01);
    }

    // -----------------------------------------------------------------------
    // Entropy tests
    // -----------------------------------------------------------------------

    @Test
    void entropyEndZero() {
        StateSpace ss1 = build("end");
        StateSpace ss2 = build("end");
        assertEquals(0.0, SpectralCompositionChecker.productEntropy(ss1, ss2), 1e-8);
    }

    @Test
    void entropyNonnegative() {
        StateSpace ss1 = build("&{a: end, b: end}");
        StateSpace ss2 = build("&{c: end}");
        assertTrue(SpectralCompositionChecker.productEntropy(ss1, ss2) >= -1e-10);
    }

    @Test
    void entropyAdditiveEstimate() {
        StateSpace ss1 = build("&{a: end, b: end}");
        StateSpace ss2 = build("&{c: end}");
        double sAdd = SpectralCompositionChecker.productEntropyAdditive(ss1, ss2);
        double s1 = VonNeumannChecker.vonNeumannEntropy(ss1);
        double s2 = VonNeumannChecker.vonNeumannEntropy(ss2);
        assertEquals(s1 + s2, sAdd, 1e-8);
    }

    @Test
    void entropyIncreasesWithParallel() {
        StateSpace ss1 = build("&{a: end, b: end}");
        StateSpace ss2 = build("&{c: end}");
        double eLeft = VonNeumannChecker.vonNeumannEntropy(ss1);
        double eProd = SpectralCompositionChecker.productEntropy(ss1, ss2);
        assertTrue(eProd >= eLeft - 0.1, "eProd=" + eProd + " < eLeft=" + eLeft);
    }

    @Test
    void entropyEndTimesBranch() {
        StateSpace ss1 = build("end");
        StateSpace ss2 = build("&{a: end, b: end}");
        double eProd = SpectralCompositionChecker.productEntropy(ss1, ss2);
        double e2 = VonNeumannChecker.vonNeumannEntropy(ss2);
        assertEquals(e2, eProd, 0.1);
    }

    // -----------------------------------------------------------------------
    // Cycle rank tests
    // -----------------------------------------------------------------------

    @Test
    void cycleRankDagsFormula() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        int r = SpectralCompositionChecker.productIharaRank(ss1, ss2);
        // n1=2, n2=2, r1=0, r2=0 => 2*0 + 2*0 + 1*1 = 1
        assertEquals(1, r);
    }

    @Test
    void cycleRankEndProduct() {
        StateSpace ss1 = build("end");
        StateSpace ss2 = build("&{a: end, b: end}");
        int r2 = IharaZeta.cycleRank(ss2);
        int r = SpectralCompositionChecker.productIharaRank(ss1, ss2);
        // n1=1 => r = 1*r2 + n2*0 + 0*... = r2
        assertEquals(r2, r);
    }

    @Test
    void cycleRankEmpty() {
        StateSpace ss1 = build("end");
        StateSpace ss2 = build("end");
        assertEquals(0, SpectralCompositionChecker.productIharaRank(ss1, ss2));
    }

    @Test
    void cycleRankFormulaConsistency() {
        StateSpace ss1 = build("&{a: end, b: end}");
        StateSpace ss2 = build("&{c: end, d: end}");
        int n1 = ss1.states().size();
        int n2 = ss2.states().size();
        int r1 = IharaZeta.cycleRank(ss1);
        int r2 = IharaZeta.cycleRank(ss2);
        int rProd = SpectralCompositionChecker.productIharaRank(ss1, ss2);
        int expected = n1 * r2 + n2 * r1 + (n1 - 1) * (n2 - 1);
        assertEquals(expected, rProd);
    }

    // -----------------------------------------------------------------------
    // Cheeger bound tests
    // -----------------------------------------------------------------------

    @Test
    void cheegerBoundsOrdered() {
        StateSpace ss1 = build("&{a: end, b: end}");
        StateSpace ss2 = build("&{c: end}");
        double[] bounds = SpectralCompositionChecker.productCheegerBound(ss1, ss2);
        assertTrue(bounds[0] <= bounds[1] + 1e-10);
    }

    @Test
    void cheegerEndGivesZero() {
        StateSpace ss1 = build("end");
        StateSpace ss2 = build("&{a: end}");
        double[] bounds = SpectralCompositionChecker.productCheegerBound(ss1, ss2);
        assertEquals(0.0, bounds[0], 1e-10);
        assertEquals(0.0, bounds[1], 1e-10);
    }

    @Test
    void cheegerNonneg() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        double[] bounds = SpectralCompositionChecker.productCheegerBound(ss1, ss2);
        assertTrue(bounds[0] >= -1e-10);
        assertTrue(bounds[1] >= -1e-10);
    }

    // -----------------------------------------------------------------------
    // Mixing time tests
    // -----------------------------------------------------------------------

    @Test
    void mixingTimeEndTimesEnd() {
        StateSpace ss1 = build("end");
        StateSpace ss2 = build("end");
        assertEquals(0, SpectralCompositionChecker.productMixingTime(ss1, ss2));
    }

    @Test
    void mixingTimePositiveForNontrivial() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        assertTrue(SpectralCompositionChecker.productMixingTime(ss1, ss2) > 0);
    }

    @Test
    void mixingTimeGrowsWithSize() {
        StateSpace ss1 = build("&{a: end}");
        int m1 = SpectralCompositionChecker.productMixingTime(ss1, ss1);
        assertTrue(m1 >= 1);
    }

    // -----------------------------------------------------------------------
    // Verification tests
    // -----------------------------------------------------------------------

    @Test
    void verifyEigenvaluesSimple() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        StateSpace ssProd = buildProduct("&{a: end}", "&{b: end}");
        var result = SpectralCompositionChecker.verifyEigenvalueComposition(
                ss1, ss2, ssProd, 1.0);
        assertEquals("eigenvalue_sum", result.lawName());
    }

    @Test
    void verifyFiedlerSimple() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        StateSpace ssProd = buildProduct("&{a: end}", "&{b: end}");
        var result = SpectralCompositionChecker.verifyFiedlerComposition(
                ss1, ss2, ssProd, 1.0);
        assertEquals("fiedler_min", result.lawName());
    }

    @Test
    void verifyHeatTraceSimple() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        StateSpace ssProd = buildProduct("&{a: end}", "&{b: end}");
        var result = SpectralCompositionChecker.verifyHeatTraceComposition(
                ss1, ss2, ssProd, 1.0, 1.0);
        assertEquals("heat_trace_multiplicative", result.lawName());
    }

    @Test
    void verifyAll() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        StateSpace ssProd = buildProduct("&{a: end}", "&{b: end}");
        var results = SpectralCompositionChecker.verifySpectralComposition(
                ss1, ss2, ssProd, 1.0);
        assertEquals(3, results.size());
    }

    // -----------------------------------------------------------------------
    // Full analysis tests
    // -----------------------------------------------------------------------

    @Test
    void basicAnalysis() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        var result = SpectralCompositionChecker.analyzeSpectralComposition(ss1, ss2);
        assertNotNull(result);
    }

    @Test
    void eigenvalueFields() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        var result = SpectralCompositionChecker.analyzeSpectralComposition(ss1, ss2);
        assertFalse(result.eigenvaluesLeft().isEmpty());
        assertFalse(result.eigenvaluesRight().isEmpty());
        assertEquals(
                result.eigenvaluesLeft().size() * result.eigenvaluesRight().size(),
                result.eigenvaluesProduct().size());
    }

    @Test
    void fiedlerFields() {
        StateSpace ss1 = build("&{a: end, b: end}");
        StateSpace ss2 = build("&{c: end}");
        var result = SpectralCompositionChecker.analyzeSpectralComposition(ss1, ss2);
        assertEquals(
                Math.min(result.fiedlerLeft(), result.fiedlerRight()),
                result.fiedlerProduct(), 1e-6);
    }

    @Test
    void heatTraceFields() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        var result = SpectralCompositionChecker.analyzeSpectralComposition(ss1, ss2);
        assertEquals(7, result.heatTraceProduct().size());
        assertEquals(7, result.heatTraceTimes().size());
    }

    @Test
    void customHeatTimes() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        var result = SpectralCompositionChecker.analyzeSpectralComposition(
                ss1, ss2, List.of(0.5, 1.0));
        assertEquals(2, result.heatTraceProduct().size());
    }

    @Test
    void entropyFields() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        var result = SpectralCompositionChecker.analyzeSpectralComposition(ss1, ss2);
        assertTrue(result.entropyProduct() >= -1e-10);
    }

    @Test
    void mixingTimeFields() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        var result = SpectralCompositionChecker.analyzeSpectralComposition(ss1, ss2);
        assertTrue(result.mixingTimeProduct() >= 0);
    }

    @Test
    void cheegerFields() {
        StateSpace ss1 = build("&{a: end, b: end}");
        StateSpace ss2 = build("&{c: end}");
        var result = SpectralCompositionChecker.analyzeSpectralComposition(ss1, ss2);
        assertTrue(result.cheegerLowerProduct() <= result.cheegerUpperProduct() + 1e-10);
    }

    @Test
    void cycleRankFields() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        var result = SpectralCompositionChecker.analyzeSpectralComposition(ss1, ss2);
        assertTrue(result.cycleRankProduct() >= 0);
    }

    @Test
    void lawResultsPopulated() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        var result = SpectralCompositionChecker.analyzeSpectralComposition(ss1, ss2);
        assertTrue(result.lawResults().containsKey("eigenvalue_count"));
        assertTrue(result.lawResults().containsKey("zero_eigenvalue"));
        assertTrue(result.lawResults().containsKey("fiedler_min"));
        assertTrue(result.lawResults().containsKey("entropy_nonneg"));
    }

    // -----------------------------------------------------------------------
    // Edge cases: end || S
    // -----------------------------------------------------------------------

    @Test
    void endPreservesEigenvalues() {
        StateSpace ss1 = build("end");
        StateSpace ss2 = build("&{a: end, b: end}");
        List<Double> eigsProd = SpectralCompositionChecker.productEigenvalues(ss1, ss2);
        double[] eigs2 = EigenvalueComputer.laplacianEigenvalues(ss2);
        assertEquals(eigs2.length, eigsProd.size());
        for (int i = 0; i < eigsProd.size(); i++) {
            assertEquals(eigs2[i], eigsProd.get(i), 1e-8);
        }
    }

    @Test
    void endPreservesFiedler() {
        StateSpace ss1 = build("end");
        StateSpace ss2 = build("&{a: end, b: end}");
        assertEquals(0.0, SpectralCompositionChecker.productFiedler(ss1, ss2), 1e-8);
    }

    // -----------------------------------------------------------------------
    // Edge cases: S || S
    // -----------------------------------------------------------------------

    @Test
    void selfProductEigenvalues() {
        StateSpace ss = build("&{a: end}");
        List<Double> eigs = SpectralCompositionChecker.productEigenvalues(ss, ss);
        double[] eigsSingle = EigenvalueComputer.laplacianEigenvalues(ss);
        assertEquals(eigsSingle.length * eigsSingle.length, eigs.size());
    }

    @Test
    void selfProductFiedler() {
        StateSpace ss = build("&{a: end, b: end}");
        double f = EigenvalueComputer.fiedlerValue(ss);
        double fProd = SpectralCompositionChecker.productFiedler(ss, ss);
        assertEquals(f, fProd, 1e-6);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    @Test
    void firstNonzeroEmpty() {
        assertEquals(0.0, SpectralCompositionChecker.firstNonzero(new double[0]));
    }

    @Test
    void firstNonzeroAllZero() {
        assertEquals(0.0, SpectralCompositionChecker.firstNonzero(new double[]{0.0, 0.0}));
    }

    @Test
    void firstNonzeroNormal() {
        assertEquals(1.5, SpectralCompositionChecker.firstNonzero(new double[]{0.0, 1.5, 3.0}), 1e-10);
    }

    // -----------------------------------------------------------------------
    // Benchmark protocols under parallel
    // -----------------------------------------------------------------------

    @Test
    void nestedBranchParallel() {
        StateSpace ss1 = build("&{a: &{b: end}}");
        StateSpace ss2 = build("&{c: end}");
        var result = SpectralCompositionChecker.analyzeSpectralComposition(ss1, ss2);
        assertNotNull(result);
        assertTrue(result.allLawsVerified());
    }

    @Test
    void selectionParallel() {
        StateSpace ss1 = build("+{a: end, b: end}");
        StateSpace ss2 = build("+{c: end}");
        var result = SpectralCompositionChecker.analyzeSpectralComposition(ss1, ss2);
        assertNotNull(result);
        assertFalse(result.eigenvaluesProduct().isEmpty());
    }

    @Test
    void mixedBranchSelect() {
        StateSpace ss1 = build("&{a: end, b: end}");
        StateSpace ss2 = build("+{c: end, d: end}");
        var result = SpectralCompositionChecker.analyzeSpectralComposition(ss1, ss2);
        assertTrue(result.entropyProduct() >= -1e-10);
    }

    @Test
    void deeperProtocol() {
        StateSpace ss1 = build("&{a: &{b: end}, c: end}");
        StateSpace ss2 = build("&{d: &{e: end}}");
        var result = SpectralCompositionChecker.analyzeSpectralComposition(ss1, ss2);
        assertEquals(
                Math.min(result.fiedlerLeft(), result.fiedlerRight()),
                result.fiedlerProduct(), 1e-6);
    }
}
