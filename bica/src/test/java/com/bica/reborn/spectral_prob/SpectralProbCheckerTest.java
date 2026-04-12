package com.bica.reborn.spectral_prob;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for spectral-probabilistic analysis (Steps 30o-30r).
 * Java port of Python {@code test_spectral_prob.py}.
 */
class SpectralProbCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Cheeger constant (Step 30o)
    // -----------------------------------------------------------------------

    @Test
    void cheegerEnd() {
        assertEquals(0.0, SpectralProbChecker.cheegerConstant(build("end")));
    }

    @Test
    void cheegerSingleEdge() {
        double h = SpectralProbChecker.cheegerConstant(build("&{a: end}"));
        assertTrue(h >= 1.0, "K2 should have h >= 1, got " + h);
    }

    @ParameterizedTest
    @CsvSource({
            "&{a: end}",
            "&{a: &{b: end}}",
            "(&{a: end} || &{b: end})"
    })
    void cheegerBoundsConsistent(String type) {
        double[] bounds = SpectralProbChecker.cheegerBounds(build(type));
        assertTrue(bounds[0] <= bounds[1] + 0.01,
                "lower=" + bounds[0] + " upper=" + bounds[1]);
    }

    @ParameterizedTest
    @CsvSource({
            "&{a: end}",
            "&{a: &{b: end}}"
    })
    void cheegerBetweenBounds(String type) {
        StateSpace ss = build(type);
        double h = SpectralProbChecker.cheegerConstant(ss);
        double[] bounds = SpectralProbChecker.cheegerBounds(ss);
        assertTrue(bounds[0] - 0.01 <= h, "h=" + h + " < lower=" + bounds[0]);
        assertTrue(h <= bounds[1] + 0.5, "h=" + h + " > upper=" + bounds[1]);
    }

    // -----------------------------------------------------------------------
    // Mixing time (Step 30p)
    // -----------------------------------------------------------------------

    @Test
    void mixingTimeEnd() {
        assertEquals(0.0, SpectralProbChecker.mixingTimeBound(build("end")));
    }

    @ParameterizedTest
    @CsvSource({
            "&{a: end}",
            "(&{a: end} || &{b: end})"
    })
    void mixingTimePositiveForConnected(String type) {
        assertTrue(SpectralProbChecker.mixingTimeBound(build(type)) > 0);
    }

    @Test
    void chainLongerMixesSlower() {
        double t2 = SpectralProbChecker.mixingTimeBound(build("&{a: end}"));
        double t3 = SpectralProbChecker.mixingTimeBound(build("&{a: &{b: end}}"));
        assertTrue(t3 > t2, "t3=" + t3 + " should be > t2=" + t2);
    }

    // -----------------------------------------------------------------------
    // Stationary distribution
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "&{a: end}",
            "&{a: &{b: end}}",
            "(&{a: end} || &{b: end})"
    })
    void stationarySumsToOne(String type) {
        Map<Integer, Double> pi = SpectralProbChecker.stationaryDistribution(build(type));
        double sum = pi.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, sum, 0.01);
    }

    @Test
    void stationaryProportionalToDegree() {
        Map<Integer, Double> pi = SpectralProbChecker.stationaryDistribution(build("&{a: end}"));
        // K2: both vertices degree 1, so uniform
        for (double v : pi.values()) {
            assertEquals(0.5, v, 0.01);
        }
    }

    // -----------------------------------------------------------------------
    // Heat kernel (Step 30q)
    // -----------------------------------------------------------------------

    @Test
    void heatKernelEnd() {
        assertEquals(1.0, SpectralProbChecker.heatKernelTrace(build("end"), 1.0), 0.01);
    }

    @ParameterizedTest
    @CsvSource({
            "&{a: end}",
            "&{a: &{b: end}}"
    })
    void heatKernelAtZero(String type) {
        StateSpace ss = build(type);
        double Z = SpectralProbChecker.heatKernelTrace(ss, 0.0);
        assertEquals(ss.states().size(), Z, 0.01);
    }

    @Test
    void heatKernelDecreasingInT() {
        StateSpace ss = build("&{a: &{b: end}}");
        double Z1 = SpectralProbChecker.heatKernelTrace(ss, 0.5);
        double Z2 = SpectralProbChecker.heatKernelTrace(ss, 2.0);
        assertTrue(Z1 >= Z2, "Z(0.5)=" + Z1 + " should be >= Z(2.0)=" + Z2);
    }

    // -----------------------------------------------------------------------
    // Von Neumann entropy (Step 30r)
    // -----------------------------------------------------------------------

    @Test
    void vonNeumannEnd() {
        assertEquals(0.0, SpectralProbChecker.vonNeumannEntropy(build("end")));
    }

    @ParameterizedTest
    @CsvSource({
            "&{a: end}",
            "&{a: &{b: end}}",
            "(&{a: end} || &{b: end})"
    })
    void vonNeumannNonNegative(String type) {
        assertTrue(SpectralProbChecker.vonNeumannEntropy(build(type)) >= 0.0);
    }

    @ParameterizedTest
    @CsvSource({
            "&{a: &{b: end}}",
            "(&{a: end} || &{b: end})"
    })
    void normalizedEntropyRange(String type) {
        double sn = SpectralProbChecker.normalizedEntropy(build(type));
        assertTrue(sn >= 0.0 && sn <= 1.01, "normalized entropy=" + sn);
    }

    @Test
    void parallelHigherEntropy() {
        double sChain = SpectralProbChecker.vonNeumannEntropy(build("&{a: end}"));
        double sPar = SpectralProbChecker.vonNeumannEntropy(build("(&{a: end} || &{b: end})"));
        assertTrue(sPar >= sChain - 0.01,
                "parallel entropy=" + sPar + " should be >= chain=" + sChain);
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    @Test
    void analyzeEnd() {
        SpectralProbResult r = SpectralProbChecker.analyze(build("end"));
        assertEquals(1, r.numStates());
        assertEquals(0.0, r.vonNeumannEntropy());
    }

    @Test
    void analyzeBranch() {
        SpectralProbResult r = SpectralProbChecker.analyze(build("&{a: end}"));
        assertEquals(2, r.numStates());
        assertTrue(r.cheegerConstant() >= 1.0);
        assertTrue(r.mixingTimeBound() > 0);
    }

    @Test
    void analyzeParallel() {
        SpectralProbResult r = SpectralProbChecker.analyze(build("(&{a: end} || &{b: end})"));
        assertEquals(4, r.numStates());
        assertTrue(r.vonNeumannEntropy() > 0);
    }

    // -----------------------------------------------------------------------
    // Benchmarks
    // -----------------------------------------------------------------------

    static Stream<org.junit.jupiter.params.provider.Arguments> benchmarks() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("File Object", "&{open: &{read: end, write: end}}"),
                org.junit.jupiter.params.provider.Arguments.of("SMTP", "&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}"),
                org.junit.jupiter.params.provider.Arguments.of("Two-choice", "&{a: end, b: end}"),
                org.junit.jupiter.params.provider.Arguments.of("Nested", "&{a: &{b: &{c: end}}}"),
                org.junit.jupiter.params.provider.Arguments.of("Parallel", "(&{a: end} || &{b: end})")
        );
    }

    @ParameterizedTest
    @MethodSource("benchmarks")
    void benchmarkAnalysisRuns(String name, String type) {
        StateSpace ss = build(type);
        SpectralProbResult r = SpectralProbChecker.analyze(ss);
        assertEquals(ss.states().size(), r.numStates());
        assertTrue(r.vonNeumannEntropy() >= 0, name + ": entropy negative");
        assertTrue(r.heatTrace() > 0, name + ": heat trace must be positive");
        double piSum = r.stationaryDistribution().values().stream()
                .mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, piSum, 0.01, name + ": stationary dist must sum to 1");
    }
}
