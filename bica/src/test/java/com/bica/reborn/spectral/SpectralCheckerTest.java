package com.bica.reborn.spectral;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for spectral feature extraction and clustering.
 */
class SpectralCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Feature extraction
    // -----------------------------------------------------------------------

    @Test
    void featuresEnd() {
        var ss = build("end");
        var f = SpectralChecker.extractFeatures(ss);
        assertEquals(1, f.numStates());
        assertEquals(0, f.numTransitions());
        assertEquals(0, f.height());
        assertEquals(1, f.width());
    }

    @Test
    void featuresSimpleBranch() {
        var ss = build("&{a: end}");
        var f = SpectralChecker.extractFeatures(ss);
        assertEquals(2, f.numStates());
        assertEquals(1, f.numTransitions());
        assertEquals(1, f.height());
    }

    @Test
    void featuresTwoBranch() {
        var ss = build("&{a: end, b: end}");
        var f = SpectralChecker.extractFeatures(ss);
        assertEquals(2, f.numStates());
        assertEquals(2, f.numTransitions());
    }

    @Test
    void featuresSpectralRadiusNonNegative() {
        var ss = build("&{a: end, b: end}");
        var f = SpectralChecker.extractFeatures(ss);
        assertTrue(f.spectralRadius() >= 0.0);
    }

    @Test
    void featuresFiedlerValueNonNegative() {
        var ss = build("&{a: &{b: end}, c: end}");
        var f = SpectralChecker.extractFeatures(ss);
        assertTrue(f.fiedlerValue() >= 0.0);
    }

    @Test
    void featuresEntropyNonNegative() {
        var ss = build("&{a: end, b: end}");
        var f = SpectralChecker.extractFeatures(ss);
        assertTrue(f.vonNeumannEntropy() >= 0.0);
    }

    @Test
    void featuresAsVector() {
        var ss = build("&{a: end}");
        var f = SpectralChecker.extractFeatures(ss);
        double[] v = f.asVector();
        assertEquals(7, v.length);
    }

    // -----------------------------------------------------------------------
    // Width and height
    // -----------------------------------------------------------------------

    @Test
    void widthEnd() {
        var ss = build("end");
        assertEquals(1, SpectralChecker.width(ss));
    }

    @Test
    void widthChain() {
        var ss = build("&{a: &{b: end}}");
        // Chain: top -> a -> end; width = 1
        assertEquals(1, SpectralChecker.width(ss));
    }

    @Test
    void heightEnd() {
        var ss = build("end");
        assertEquals(0, SpectralChecker.height(ss));
    }

    @Test
    void heightChain() {
        var ss = build("&{a: &{b: end}}");
        assertEquals(2, SpectralChecker.height(ss));
    }

    @Test
    void heightSimpleBranch() {
        var ss = build("&{a: end}");
        assertEquals(1, SpectralChecker.height(ss));
    }

    // -----------------------------------------------------------------------
    // Feature distance
    // -----------------------------------------------------------------------

    @Test
    void distanceSameFeatures() {
        var ss = build("&{a: end}");
        var f = SpectralChecker.extractFeatures(ss);
        assertEquals(0.0, SpectralChecker.featureDistance(f, f), 1e-9);
    }

    @Test
    void distanceDifferentFeatures() {
        var f1 = SpectralChecker.extractFeatures(build("end"));
        var f2 = SpectralChecker.extractFeatures(build("&{a: end, b: end}"));
        assertTrue(SpectralChecker.featureDistance(f1, f2) > 0.0);
    }

    // -----------------------------------------------------------------------
    // Clustering
    // -----------------------------------------------------------------------

    @Test
    void clusterEmptyList() {
        var result = SpectralChecker.cluster(List.of(), 3);
        assertTrue(result.isEmpty());
    }

    @Test
    void clusterSingleElement() {
        var f = SpectralChecker.extractFeatures(build("end"));
        var result = SpectralChecker.cluster(List.of(f), 1);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).size());
        assertEquals(0, result.get(0).get(0));
    }

    @Test
    void clusterTwoElements() {
        var f1 = SpectralChecker.extractFeatures(build("end"));
        var f2 = SpectralChecker.extractFeatures(build("&{a: end, b: end}"));
        var result = SpectralChecker.cluster(List.of(f1, f2), 2);
        assertEquals(2, result.size());
        // Both elements should be assigned
        int total = result.stream().mapToInt(List::size).sum();
        assertEquals(2, total);
    }

    @Test
    void clusterKLargerThanN() {
        var f = SpectralChecker.extractFeatures(build("end"));
        var result = SpectralChecker.cluster(List.of(f), 5);
        // k clamped to n=1
        int total = result.stream().mapToInt(List::size).sum();
        assertEquals(1, total);
    }

    // -----------------------------------------------------------------------
    // Similarity search
    // -----------------------------------------------------------------------

    @Test
    void findSimilarSingle() {
        var query = SpectralChecker.extractFeatures(build("&{a: end}"));
        var corpus = List.of(
                SpectralChecker.extractFeatures(build("end")),
                SpectralChecker.extractFeatures(build("&{a: end}")),
                SpectralChecker.extractFeatures(build("&{a: end, b: end}"))
        );
        var result = SpectralChecker.findSimilar(query, corpus, 1);
        assertEquals(1, result.size());
        // Closest to &{a:end} should be itself at index 1
        assertEquals(1, result.get(0));
    }

    @Test
    void findSimilarAll() {
        var query = SpectralChecker.extractFeatures(build("end"));
        var corpus = List.of(
                SpectralChecker.extractFeatures(build("end")),
                SpectralChecker.extractFeatures(build("&{a: end}"))
        );
        var result = SpectralChecker.findSimilar(query, corpus, 10);
        assertEquals(2, result.size()); // clamped to corpus size
    }

    @Test
    void findSimilarEmpty() {
        var query = SpectralChecker.extractFeatures(build("end"));
        var result = SpectralChecker.findSimilar(query, List.of(), 5);
        assertTrue(result.isEmpty());
    }
}
