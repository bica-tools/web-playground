package com.bica.reborn.fkg;

import com.bica.reborn.fkg.FkgChecker.CorrelationAnalysis;
import com.bica.reborn.fkg.FkgChecker.CorrelationResult;
import com.bica.reborn.fkg.FkgChecker.FourFunctionsResult;
import com.bica.reborn.fkg.FkgChecker.StochasticDominance;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java port of {@code reticulate/tests/test_fkg.py}.
 */
class FkgCheckerTest {

    private static StateSpace ss(String t) {
        return StateSpaceBuilder.build(Parser.parse(t));
    }

    // Helper to build constant function
    private static Map<Integer, Double> constant(StateSpace ss, double val) {
        Map<Integer, Double> f = new HashMap<>();
        for (int s : ss.states()) f.put(s, val);
        return f;
    }

    // -----------------------------------------------------------------------
    // Monotonicity
    // -----------------------------------------------------------------------

    @Test
    void constantIsMonotone() {
        StateSpace s = ss("&{a: end, b: end}");
        assertTrue(FkgChecker.isMonotoneIncreasing(s, constant(s, 1.0)));
    }

    @Test
    void rankIsMonotone() {
        StateSpace s = ss("&{a: &{b: &{c: end}}}");
        Map<Integer, Integer> r = FkgChecker.rank(s);
        Map<Integer, Double> f = new HashMap<>();
        for (var e : r.entrySet()) f.put(e.getKey(), (double) e.getValue());
        assertTrue(FkgChecker.isMonotoneIncreasing(s, f));
    }

    @Test
    void antiRankIsDecreasing() {
        StateSpace s = ss("&{a: &{b: &{c: end}}}");
        Map<Integer, Integer> r = FkgChecker.rank(s);
        Map<Integer, Double> f = new HashMap<>();
        for (var e : r.entrySet()) f.put(e.getKey(), -(double) e.getValue());
        assertTrue(FkgChecker.isMonotoneDecreasing(s, f));
    }

    @Test
    void randomMonotoneIsMonotone() {
        StateSpace s = ss("&{a: end, b: end}");
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 42);
        assertTrue(FkgChecker.isMonotoneIncreasing(s, f));
    }

    @Test
    void randomMonotoneDifferentSeeds() {
        StateSpace s = ss("&{a: end, b: end}");
        Map<Integer, Double> f1 = FkgChecker.generateRandomMonotone(s, 1);
        Map<Integer, Double> f2 = FkgChecker.generateRandomMonotone(s, 2);
        assertNotEquals(f1, f2);
    }

    @Test
    void nonMonotoneDetected() {
        StateSpace s = ss("&{a: &{b: &{c: end}}}");
        Map<Integer, Integer> r = FkgChecker.rank(s);
        Map<Integer, Double> f = new HashMap<>();
        for (var e : r.entrySet()) f.put(e.getKey(), -(double) e.getValue());
        assertFalse(FkgChecker.isMonotoneIncreasing(s, f));
    }

    // -----------------------------------------------------------------------
    // Harris inequality
    // -----------------------------------------------------------------------

    @Test
    void harrisEnd() {
        StateSpace s = ss("end");
        CorrelationResult r = FkgChecker.checkHarris(s, constant(s, 1.0), constant(s, 1.0));
        assertTrue(r.holds());
        assertEquals("harris", r.inequality());
    }

    @Test
    void harrisConstantFunctions() {
        StateSpace s = ss("&{a: end, b: end}");
        CorrelationResult r = FkgChecker.checkHarris(s, constant(s, 1.0), constant(s, 2.0));
        assertTrue(r.holds());
        assertTrue(Math.abs(r.gap()) < 1e-10);
    }

    @Test
    void harrisMonotonePair() {
        StateSpace s = ss("&{a: &{b: &{c: end}}}");
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 42);
        Map<Integer, Double> g = FkgChecker.generateRandomMonotone(s, 43);
        assertTrue(FkgChecker.checkHarris(s, f, g).holds());
    }

    @Test
    void harrisOnParallel() {
        StateSpace s = ss("(&{a: end} || &{b: end})");
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 1);
        Map<Integer, Double> g = FkgChecker.generateRandomMonotone(s, 2);
        assertTrue(FkgChecker.checkHarris(s, f, g).holds());
    }

    @Test
    void harrisResultStructure() {
        StateSpace s = ss("&{a: end, b: end}");
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 1);
        Map<Integer, Double> g = FkgChecker.generateRandomMonotone(s, 2);
        CorrelationResult r = FkgChecker.checkHarris(s, f, g);
        assertNotNull(r);
        assertInstanceOf(Double.class, r.gap());
    }

    @Test
    void harrisOnRest() {
        StateSpace s = ss("&{get: +{OK: end, NOT_FOUND: end}, put: +{OK: end, ERR: end}}");
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 10);
        Map<Integer, Double> g = FkgChecker.generateRandomMonotone(s, 20);
        assertTrue(FkgChecker.checkHarris(s, f, g).holds());
    }

    // -----------------------------------------------------------------------
    // FKG inequality
    // -----------------------------------------------------------------------

    @Test
    void fkgUniform() {
        StateSpace s = ss("&{a: end, b: end}");
        Map<Integer, Double> mu = constant(s, 1.0);
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 1);
        Map<Integer, Double> g = FkgChecker.generateRandomMonotone(s, 2);
        CorrelationResult r = FkgChecker.checkFkg(s, f, g, mu);
        assertTrue(r.holds());
        assertEquals("fkg", r.inequality());
    }

    @Test
    void fkgRankMeasure() {
        StateSpace s = ss("&{a: &{b: &{c: end}}}");
        Map<Integer, Double> mu = FkgChecker.rankExponentialMeasure(s);
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 1);
        Map<Integer, Double> g = FkgChecker.generateRandomMonotone(s, 2);
        assertTrue(FkgChecker.checkFkg(s, f, g, mu).holds());
    }

    @Test
    void fkgOnParallel() {
        StateSpace s = ss("(&{a: end} || &{b: end})");
        Map<Integer, Double> mu = FkgChecker.rankExponentialMeasure(s);
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 5);
        Map<Integer, Double> g = FkgChecker.generateRandomMonotone(s, 6);
        assertTrue(FkgChecker.checkFkg(s, f, g, mu).holds());
    }

    @Test
    void fkgGapNonnegative() {
        StateSpace s = ss("&{a: end, b: end}");
        Map<Integer, Double> mu = constant(s, 1.0);
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 1);
        Map<Integer, Double> g = FkgChecker.generateRandomMonotone(s, 2);
        assertTrue(FkgChecker.fkgGap(s, f, g, mu) >= -1e-10);
    }

    @Test
    void fkgZeroMeasure() {
        StateSpace s = ss("&{a: end, b: end}");
        Map<Integer, Double> mu = constant(s, 0.0);
        CorrelationResult r = FkgChecker.checkFkg(s, constant(s, 1.0), constant(s, 1.0), mu);
        assertTrue(r.holds());
    }

    // -----------------------------------------------------------------------
    // Log-supermodularity
    // -----------------------------------------------------------------------

    @Test
    void uniformIsLogSuper() {
        StateSpace s = ss("&{a: end, b: end}");
        assertTrue(FkgChecker.isLogSupermodular(s, constant(s, 1.0)));
    }

    @Test
    void rankExponentialIsLogSuper() {
        StateSpace s = ss("(&{a: end} || &{b: end})");
        Map<Integer, Double> mu = FkgChecker.rankExponentialMeasure(s);
        assertTrue(FkgChecker.isLogSupermodular(s, mu));
    }

    @Test
    void constantIsLogSuper() {
        StateSpace s = ss("&{a: &{b: &{c: end}}}");
        assertTrue(FkgChecker.isLogSupermodular(s, constant(s, 5.0)));
    }

    // -----------------------------------------------------------------------
    // Holley inequality
    // -----------------------------------------------------------------------

    @Test
    void holleyUniformVsRank() {
        StateSpace s = ss("&{a: end, b: end}");
        Map<Integer, Double> mu1 = FkgChecker.uniformMeasure(s);
        Map<Integer, Double> mu2 = FkgChecker.rankExponentialMeasure(s);
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 1);
        StochasticDominance r = FkgChecker.checkHolley(s, mu1, mu2, f);
        assertNotNull(r);
    }

    @Test
    void holleySameMeasure() {
        StateSpace s = ss("&{a: &{b: &{c: end}}}");
        Map<Integer, Double> mu = constant(s, 1.0);
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 1);
        StochasticDominance r = FkgChecker.checkHolley(s, mu, mu, f);
        assertTrue(r.holleyCondition());
        assertTrue(Math.abs(r.expectationGap()) < 1e-10);
    }

    @Test
    void holleyResultStructure() {
        StateSpace s = ss("&{a: end, b: end}");
        Map<Integer, Double> mu1 = constant(s, 1.0);
        Map<Integer, Double> mu2 = constant(s, 2.0);
        Map<Integer, Double> f = constant(s, 1.0);
        StochasticDominance r = FkgChecker.checkHolley(s, mu1, mu2, f);
        assertNotNull(r);
        assertInstanceOf(Boolean.class, r.dominates());
        assertInstanceOf(Boolean.class, r.holleyCondition());
        assertInstanceOf(Double.class, r.expectationGap());
    }

    // -----------------------------------------------------------------------
    // Ahlswede-Daykin
    // -----------------------------------------------------------------------

    @Test
    void adConstantFunctions() {
        StateSpace s = ss("&{a: end, b: end}");
        Map<Integer, Double> c = constant(s, 1.0);
        FourFunctionsResult r = FkgChecker.checkAhlswedeDaykin(s, c, c, c, c);
        assertTrue(r.holds());
        assertTrue(r.pointwiseHolds());
        assertTrue(Math.abs(r.lhs() - r.rhs()) < 1e-10);
    }

    @Test
    void adResultStructure() {
        StateSpace s = ss("&{a: end, b: end}");
        Map<Integer, Double> c = constant(s, 1.0);
        FourFunctionsResult r = FkgChecker.checkAhlswedeDaykin(s, c, c, c, c);
        assertNotNull(r);
        assertInstanceOf(Double.class, r.sumAlpha());
    }

    @Test
    void adOnChain() {
        StateSpace s = ss("&{a: &{b: &{c: end}}}");
        Map<Integer, Double> c = constant(s, 1.0);
        assertTrue(FkgChecker.checkAhlswedeDaykin(s, c, c, c, c).holds());
    }

    @Test
    void adMeasureDerived() {
        StateSpace s = ss("&{a: end, b: end}");
        Map<Integer, Double> mu = FkgChecker.rankExponentialMeasure(s);
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 1);
        Map<Integer, Double> g = FkgChecker.generateRandomMonotone(s, 2);
        Map<Integer, Double> alpha = new HashMap<>();
        Map<Integer, Double> beta = new HashMap<>();
        Map<Integer, Double> gamma = new HashMap<>();
        for (int st : s.states()) {
            alpha.put(st, mu.getOrDefault(st, 0.0) * f.getOrDefault(st, 0.0));
            beta.put(st, mu.getOrDefault(st, 0.0) * g.getOrDefault(st, 0.0));
            gamma.put(st, mu.getOrDefault(st, 0.0) * f.getOrDefault(st, 0.0) * g.getOrDefault(st, 0.0));
        }
        FourFunctionsResult r = FkgChecker.checkAhlswedeDaykin(s, alpha, beta, gamma, new HashMap<>(mu));
        assertNotNull(r);
    }

    // -----------------------------------------------------------------------
    // Correlation profile
    // -----------------------------------------------------------------------

    @Test
    void profileStructure() {
        StateSpace s = ss("&{a: end, b: end}");
        Map<String, Object> p = FkgChecker.correlationProfile(s, 5, 42);
        assertTrue(p.containsKey("harris_avg_gap"));
        assertTrue(p.containsKey("fkg_avg_gap"));
        assertTrue(p.containsKey("harris_all_hold"));
        assertTrue(p.containsKey("fkg_all_hold"));
        assertEquals(5, p.get("n_trials"));
    }

    @Test
    void profileOnChain() {
        StateSpace s = ss("&{a: &{b: &{c: end}}}");
        Map<String, Object> p = FkgChecker.correlationProfile(s, 10, 42);
        assertTrue((Boolean) p.get("harris_all_hold"));
        assertTrue((Boolean) p.get("fkg_all_hold"));
    }

    @Test
    void profileOnParallel() {
        StateSpace s = ss("(&{a: end} || &{b: end})");
        Map<String, Object> p = FkgChecker.correlationProfile(s, 5, 42);
        assertInstanceOf(Double.class, p.get("harris_avg_gap"));
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    @Test
    void analysisEnd() {
        StateSpace s = ss("end");
        CorrelationAnalysis a = FkgChecker.analyzeCorrelation(s, 42);
        assertNotNull(a);
        assertEquals(1, a.numStates());
    }

    @Test
    void analysisBranch() {
        StateSpace s = ss("&{a: end, b: end}");
        CorrelationAnalysis a = FkgChecker.analyzeCorrelation(s, 42);
        assertTrue(a.harris().holds());
        assertTrue(a.fkg().holds());
    }

    @Test
    void analysisChain() {
        StateSpace s = ss("&{a: &{b: &{c: end}}}");
        CorrelationAnalysis a = FkgChecker.analyzeCorrelation(s, 42);
        assertTrue(a.harris().holds());
        assertTrue(a.fkg().holds());
    }

    @Test
    void analysisParallel() {
        StateSpace s = ss("(&{a: end} || &{b: end})");
        CorrelationAnalysis a = FkgChecker.analyzeCorrelation(s, 42);
        assertTrue(a.harris().holds());
        assertInstanceOf(Double.class, a.avgFkgGap());
    }

    @Test
    void analysisIterator() {
        StateSpace s = ss("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
        CorrelationAnalysis a = FkgChecker.analyzeCorrelation(s, 42);
        assertNotNull(a);
    }

    // -----------------------------------------------------------------------
    // Benchmarks (Harris)
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
        "'&{a: end, b: end}', SimpleBranch",
        "'&{a: &{b: &{c: end}}}', DeepChain",
        "'+{ok: end, err: end}', SimpleSelect",
        "'(&{a: end} || &{b: end})', SimpleParallel",
        "'&{get: +{OK: end, NOT_FOUND: end}, put: +{OK: end, ERR: end}}', REST",
        "'rec X . &{request: +{OK: X, ERR: end}}', RetryLoop",
        "'&{a: end, b: end, c: end, d: end}', WideBranch",
        "'&{a: &{c: end, d: end}, b: &{e: end}}', Asymmetric",
    })
    void harrisOnBenchmarks(String typeString, String name) {
        StateSpace s = ss(typeString);
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 100);
        Map<Integer, Double> g = FkgChecker.generateRandomMonotone(s, 200);
        assertTrue(FkgChecker.checkHarris(s, f, g).holds(), "Harris should hold on " + name);
    }

    // -----------------------------------------------------------------------
    // Benchmarks (FKG)
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
        "'&{a: end, b: end}', SimpleBranch",
        "'&{a: &{b: &{c: end}}}', DeepChain",
        "'(&{a: end} || &{b: end})', SimpleParallel",
        "'&{get: +{OK: end, NOT_FOUND: end}, put: +{OK: end, ERR: end}}', REST",
    })
    void fkgOnBenchmarks(String typeString, String name) {
        StateSpace s = ss(typeString);
        Map<Integer, Double> mu = FkgChecker.rankExponentialMeasure(s);
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 100);
        Map<Integer, Double> g = FkgChecker.generateRandomMonotone(s, 200);
        assertTrue(FkgChecker.checkFkg(s, f, g, mu).holds(), "FKG should hold on " + name);
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Test
    void singleState() {
        StateSpace s = ss("end");
        CorrelationAnalysis a = FkgChecker.analyzeCorrelation(s, 42);
        assertTrue(a.harris().holds());
        assertTrue(a.fkg().holds());
    }

    @Test
    void twoStates() {
        StateSpace s = ss("&{a: end}");
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 1);
        assertTrue(FkgChecker.isMonotoneIncreasing(s, f));
        assertTrue(FkgChecker.checkHarris(s, f, f).holds());
    }

    @Test
    void identicalFunctions() {
        StateSpace s = ss("&{a: end, b: end}");
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 1);
        CorrelationResult r = FkgChecker.checkHarris(s, f, f);
        assertTrue(r.holds());
        assertTrue(r.gap() >= -1e-10);
    }

    @Test
    void zeroFunction() {
        StateSpace s = ss("&{a: end, b: end}");
        Map<Integer, Double> f = constant(s, 0.0);
        Map<Integer, Double> g = FkgChecker.generateRandomMonotone(s, 1);
        assertTrue(FkgChecker.checkHarris(s, f, g).holds());
    }

    @Test
    void recursiveType() {
        StateSpace s = ss("rec X . &{a: X}");
        Map<Integer, Double> f = FkgChecker.generateRandomMonotone(s, 1);
        assertTrue(FkgChecker.checkHarris(s, f, f).holds());
    }
}
