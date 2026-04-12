package com.bica.reborn.spectral_gnn;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for spectral GNN features (Step 31b).
 * Java port of Python {@code test_spectral_gnn.py}.
 */
class SpectralGnnCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Feature extraction
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "end",
            "&{a: end}",
            "(&{a: end} || &{b: end})"
    })
    void featureLength(String type) {
        List<Double> f = SpectralGnnChecker.extractFeatures(build(type));
        assertEquals(20, f.size());
        assertEquals(20, SpectralGnnChecker.FEATURE_NAMES.size());
    }

    @Test
    void endFeatures() {
        List<Double> f = SpectralGnnChecker.extractFeatures(build("end"));
        assertEquals(1.0, f.get(0), 0.01);  // num_states
        assertEquals(0.0, f.get(2), 0.01);  // height
    }

    @Test
    void chainFeatures() {
        List<Double> f = SpectralGnnChecker.extractFeatures(build("&{a: end}"));
        assertEquals(2.0, f.get(0), 0.01);  // num_states
        assertTrue(f.get(2) >= 1.0);         // height >= 1
    }

    @Test
    void parallelFeatures() {
        List<Double> f = SpectralGnnChecker.extractFeatures(build("(&{a: end} || &{b: end})"));
        assertEquals(1.0, f.get(16), 0.01);  // has_parallel
        assertTrue(f.get(3) >= 2.0);          // width >= 2
    }

    static Stream<String> noNanTypes() {
        return Stream.of(
                "end",
                "&{a: end}",
                "&{a: &{b: end}}",
                "(&{a: end} || &{b: end})",
                "rec X . &{a: X, b: end}"
        );
    }

    @ParameterizedTest
    @MethodSource("noNanTypes")
    void noNans(String type) {
        List<Double> f = SpectralGnnChecker.extractFeatures(build(type));
        for (int i = 0; i < f.size(); i++) {
            assertFalse(Double.isNaN(f.get(i)),
                    "Feature " + SpectralGnnChecker.FEATURE_NAMES.get(i) + " is NaN for " + type);
            assertFalse(Double.isInfinite(f.get(i)),
                    "Feature " + SpectralGnnChecker.FEATURE_NAMES.get(i) + " is Inf for " + type);
        }
    }

    // -----------------------------------------------------------------------
    // Spectral moments
    // -----------------------------------------------------------------------

    @Test
    void momentsEmpty() {
        List<Double> m = SpectralGnnChecker.spectralMoments(new double[0], 4);
        assertEquals(List.of(0.0, 0.0, 0.0, 0.0), m);
    }

    @Test
    void momentsSingle() {
        List<Double> m = SpectralGnnChecker.spectralMoments(new double[]{2.0}, 4);
        assertEquals(2.0, m.get(0), 0.01);
        assertEquals(4.0, m.get(1), 0.01);
    }

    @Test
    void momentsSymmetric() {
        List<Double> m = SpectralGnnChecker.spectralMoments(new double[]{-1.0, 1.0}, 4);
        assertEquals(0.0, m.get(0), 0.01);  // m_1 = 0
        assertEquals(0.0, m.get(2), 0.01);  // m_3 = 0
    }

    // -----------------------------------------------------------------------
    // Similarity
    // -----------------------------------------------------------------------

    @Test
    void cosineIdentical() {
        List<Double> v = List.of(1.0, 2.0, 3.0);
        assertEquals(1.0, SpectralGnnChecker.cosineSimilarity(v, v), 0.01);
    }

    @Test
    void cosineOrthogonal() {
        assertEquals(0.0, SpectralGnnChecker.cosineSimilarity(
                List.of(1.0, 0.0), List.of(0.0, 1.0)), 0.01);
    }

    @Test
    void euclideanZero() {
        List<Double> v = List.of(1.0, 2.0, 3.0);
        assertTrue(SpectralGnnChecker.euclideanDistance(v, v) < 0.01);
    }

    @Test
    void euclideanPositive() {
        assertEquals(1.0, SpectralGnnChecker.euclideanDistance(
                List.of(0.0), List.of(1.0)), 0.01);
    }

    @Test
    void sameTypeSimilar() {
        List<Double> f1 = SpectralGnnChecker.extractFeatures(build("&{a: end}"));
        List<Double> f2 = SpectralGnnChecker.extractFeatures(build("&{a: end}"));
        assertTrue(SpectralGnnChecker.cosineSimilarity(f1, f2) > 0.99);
    }

    @Test
    void differentTypesLessSimilar() {
        List<Double> f1 = SpectralGnnChecker.extractFeatures(build("&{a: end}"));
        List<Double> f2 = SpectralGnnChecker.extractFeatures(build("(&{a: end} || &{b: end})"));
        double sim = SpectralGnnChecker.cosineSimilarity(f1, f2);
        assertTrue(sim < 1.0);
    }

    // -----------------------------------------------------------------------
    // Classification
    // -----------------------------------------------------------------------

    @Test
    void classifyChain() {
        assertEquals("chain", SpectralGnnChecker.classifyProtocol(build("&{a: end}")));
        assertEquals("chain", SpectralGnnChecker.classifyProtocol(build("&{a: &{b: end}}")));
    }

    @Test
    void classifyBranch() {
        assertEquals("branch", SpectralGnnChecker.classifyProtocol(build("&{a: end, b: end}")));
    }

    @Test
    void classifyParallel() {
        assertEquals("parallel", SpectralGnnChecker.classifyProtocol(
                build("(&{a: end} || &{b: end})")));
    }

    @Test
    void classifyRecursive() {
        String family = SpectralGnnChecker.classifyProtocol(
                build("rec X . &{a: X, b: end}"));
        assertTrue(Set.of("recursive", "branch", "chain").contains(family));
    }

    @Test
    void classifyEnd() {
        String family = SpectralGnnChecker.classifyProtocol(build("end"));
        assertTrue(Set.of("chain", "branch").contains(family));
    }

    // -----------------------------------------------------------------------
    // Analyze
    // -----------------------------------------------------------------------

    @Test
    void analyzeEnd() {
        var r = SpectralGnnChecker.analyzeSpectralGnn(build("end"));
        assertEquals(1, r.numStates());
        assertEquals(20, r.featureVector().size());
    }

    @Test
    void analyzeParallel() {
        var r = SpectralGnnChecker.analyzeSpectralGnn(build("(&{a: end} || &{b: end})"));
        assertEquals("parallel", r.protocolFamily());
        assertEquals(4, r.spectralMoments().size());
    }

    // -----------------------------------------------------------------------
    // Benchmarks
    // -----------------------------------------------------------------------

    static Stream<org.junit.jupiter.params.provider.Arguments> benchmarks() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        "Java Iterator", "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "File Object", "&{open: &{read: end, write: end}}"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "SMTP", "&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "Two-choice", "&{a: end, b: end}"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "Nested", "&{a: &{b: &{c: end}}}"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "Parallel", "(&{a: end} || &{b: end})")
        );
    }

    @ParameterizedTest
    @MethodSource("benchmarks")
    void benchmarkFeaturesValid(String name, String type) {
        StateSpace ss = build(type);
        var r = SpectralGnnChecker.analyzeSpectralGnn(ss);
        assertEquals(20, r.featureVector().size());
        assertTrue(Set.of("chain", "branch", "parallel", "recursive", "mixed")
                .contains(r.protocolFamily()));
        for (double v : r.featureVector()) {
            assertFalse(Double.isNaN(v), name + ": NaN in feature vector");
        }
    }

    @ParameterizedTest
    @MethodSource("benchmarks")
    void benchmarkSelfSimilarity(String name, String type) {
        StateSpace ss = build(type);
        List<Double> f = SpectralGnnChecker.extractFeatures(ss);
        assertTrue(SpectralGnnChecker.cosineSimilarity(f, f) > 0.99,
                name + ": self-similarity should be > 0.99");
    }
}
