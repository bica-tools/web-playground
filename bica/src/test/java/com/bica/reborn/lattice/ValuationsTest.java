package com.bica.reborn.lattice;

import com.bica.reborn.lattice.Valuations.FKGResult;
import com.bica.reborn.lattice.Valuations.ValuationAnalysis;
import com.bica.reborn.lattice.Valuations.ValuationResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for lattice valuations and the FKG inequality.
 *
 * <p>Ported from {@code reticulate/tests/test_valuation.py} (Step 30y).
 */
class ValuationsTest {

    // -- helpers ---------------------------------------------------------------

    private static StateSpace build(String source) {
        return StateSpaceBuilder.build(Parser.parse(source));
    }

    private static ToDoubleFunction<Integer> constant(double c) {
        return s -> c;
    }

    private static ToDoubleFunction<Integer> fromMap(Map<Integer, ? extends Number> map) {
        return s -> map.get(s).doubleValue();
    }

    // =========================================================================
    // Rank function
    // =========================================================================

    @Nested
    class RankFunction {
        @Test
        void end() {
            StateSpace ss = build("end");
            Map<Integer, Integer> r = Valuations.rankFunction(ss);
            assertEquals(0, r.get(ss.bottom()));
        }

        @Test
        void simpleBranch() {
            StateSpace ss = build("&{a: end, b: end}");
            Map<Integer, Integer> r = Valuations.rankFunction(ss);
            assertEquals(0, r.get(ss.bottom()));
            assertTrue(r.get(ss.top()) >= 1);
        }

        @Test
        void deepChain() {
            StateSpace ss = build("&{a: &{b: &{c: end}}}");
            Map<Integer, Integer> r = Valuations.rankFunction(ss);
            assertEquals(0, r.get(ss.bottom()));
            assertEquals(3, r.get(ss.top()));
        }

        @Test
        void parallel() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            Map<Integer, Integer> r = Valuations.rankFunction(ss);
            assertEquals(0, r.get(ss.bottom()));
            assertTrue(r.get(ss.top()) >= 2);
        }

        @Test
        void monotone() {
            StateSpace ss = build("&{a: end, b: end}");
            Map<Integer, Integer> r = Valuations.rankFunction(ss);
            assertTrue(r.get(ss.top()) >= r.get(ss.bottom()));
        }
    }

    // =========================================================================
    // Height function
    // =========================================================================

    @Nested
    class HeightFunction {
        @Test
        void end() {
            StateSpace ss = build("end");
            Map<Integer, Integer> h = Valuations.heightFunction(ss);
            assertEquals(0, h.get(ss.bottom()));
        }

        @Test
        void deepChain() {
            StateSpace ss = build("&{a: &{b: &{c: end}}}");
            Map<Integer, Integer> h = Valuations.heightFunction(ss);
            assertEquals(0, h.get(ss.bottom()));
            assertEquals(3, h.get(ss.top()));
        }

        @Test
        void simpleBranch() {
            StateSpace ss = build("&{a: end, b: end}");
            Map<Integer, Integer> h = Valuations.heightFunction(ss);
            assertEquals(0, h.get(ss.bottom()));
            assertTrue(h.get(ss.top()) >= 1);
        }

        @Test
        void iterator() {
            StateSpace ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            Map<Integer, Integer> h = Valuations.heightFunction(ss);
            assertEquals(0, h.get(ss.bottom()));
        }
    }

    // =========================================================================
    // Valuation checking
    // =========================================================================

    @Nested
    class CheckValuation {
        @Test
        void constantIsValuation() {
            StateSpace ss = build("&{a: end, b: end}");
            ValuationResult r = Valuations.checkValuation(ss, constant(1.0));
            assertTrue(r.isValuation());
        }

        @Test
        void rankOnChain() {
            StateSpace ss = build("&{a: &{b: &{c: end}}}");
            Map<Integer, Integer> rk = Valuations.rankFunction(ss);
            ValuationResult r = Valuations.checkValuation(ss, s -> (double) rk.get(s));
            assertTrue(r.isValuation());
        }

        @Test
        void resultStructure() {
            StateSpace ss = build("&{a: end, b: end}");
            ValuationResult r = Valuations.checkValuation(ss, constant(0.0));
            assertNotNull(r);
            assertTrue(r.maxDefect() >= 0);
        }

        @Test
        void rankOnParallel() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            Map<Integer, Integer> rk = Valuations.rankFunction(ss);
            ValuationResult r = Valuations.checkValuation(ss, s -> (double) rk.get(s));
            assertTrue(r.isValuation());
        }
    }

    // =========================================================================
    // isRankValuation
    // =========================================================================

    @Nested
    class IsRankValuation {
        @Test
        void end() {
            assertTrue(Valuations.isRankValuation(build("end")));
        }

        @Test
        void chain() {
            assertTrue(Valuations.isRankValuation(build("&{a: &{b: &{c: end}}}")));
        }

        @Test
        void simpleBranch() {
            assertTrue(Valuations.isRankValuation(build("&{a: end, b: end}")));
        }

        @Test
        void parallel() {
            assertTrue(Valuations.isRankValuation(build("(&{a: end} || &{b: end})")));
        }
    }

    // =========================================================================
    // FKG inequality
    // =========================================================================

    @Nested
    class FKG {
        @Test
        void uniformMeasure() {
            StateSpace ss = build("&{a: end, b: end}");
            FKGResult r = Valuations.checkFkg(ss, constant(1.0), constant(1.0), constant(1.0));
            assertTrue(r.holds());
            assertNotNull(r);
        }

        @Test
        void rankAndHeight() {
            StateSpace ss = build("&{a: &{b: &{c: end}}}");
            Map<Integer, Integer> rk = Valuations.rankFunction(ss);
            Map<Integer, Integer> ht = Valuations.heightFunction(ss);
            FKGResult r = Valuations.checkFkg(
                    ss, s -> (double) rk.get(s), s -> (double) ht.get(s), constant(1.0));
            assertTrue(r.holds());
        }

        @Test
        void fkgOnParallel() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            Map<Integer, Integer> rk = Valuations.rankFunction(ss);
            ToDoubleFunction<Integer> f = s -> (double) rk.get(s);
            FKGResult r = Valuations.checkFkg(ss, f, f, constant(1.0));
            assertTrue(r.holds());
            assertTrue(r.correlation() >= -1e-10);
        }

        @Test
        void resultStructure() {
            StateSpace ss = build("&{a: end, b: end}");
            FKGResult r = Valuations.checkFkg(ss, constant(1.0), constant(1.0), constant(1.0));
            // records with primitive doubles — just sanity check finiteness
            assertTrue(Double.isFinite(r.eF()));
            assertTrue(Double.isFinite(r.eG()));
            assertTrue(Double.isFinite(r.eFg()));
            assertTrue(Double.isFinite(r.correlation()));
        }
    }

    // =========================================================================
    // Log-supermodularity
    // =========================================================================

    @Nested
    class LogSupermodular {
        @Test
        void uniformIsLogSuper() {
            StateSpace ss = build("&{a: end, b: end}");
            assertTrue(Valuations.isLogSupermodular(ss, constant(1.0)));
        }

        @Test
        void rankBasedMeasure() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            Map<Integer, Integer> rk = Valuations.rankFunction(ss);
            ToDoubleFunction<Integer> mu = s -> Math.pow(2.0, rk.get(s));
            assertTrue(Valuations.isLogSupermodular(ss, mu));
        }
    }

    // =========================================================================
    // Monotone correlation
    // =========================================================================

    @Nested
    class MonotoneCorrelation {
        @Test
        void end() {
            double c = Valuations.monotoneCorrelation(build("end"));
            assertTrue(Double.isFinite(c));
        }

        @Test
        void chainPerfect() {
            double c = Valuations.monotoneCorrelation(build("&{a: &{b: &{c: end}}}"));
            assertTrue(Math.abs(c - 1.0) < 1e-10);
        }

        @Test
        void simpleBranch() {
            double c = Valuations.monotoneCorrelation(build("&{a: end, b: end}"));
            assertTrue(c >= -1.0 - 1e-10);
            assertTrue(c <= 1.0 + 1e-10);
        }
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    @Nested
    class AnalyzeValuations {
        @Test
        void end() {
            ValuationAnalysis a = Valuations.analyzeValuations(build("end"));
            assertNotNull(a);
            assertEquals(1, a.numStates());
        }

        @Test
        void simpleBranch() {
            ValuationAnalysis a = Valuations.analyzeValuations(build("&{a: end, b: end}"));
            assertTrue(a.rankIsValuation());
            assertTrue(a.numStates() >= 2);
        }

        @Test
        void deepChain() {
            ValuationAnalysis a = Valuations.analyzeValuations(build("&{a: &{b: &{c: end}}}"));
            assertTrue(a.rankIsValuation());
            assertTrue(a.isGraded());
            assertTrue(a.rankDefect() < 1e-10);
        }

        @Test
        void parallel() {
            ValuationAnalysis a = Valuations.analyzeValuations(build("(&{a: end} || &{b: end})"));
            assertTrue(a.rankIsValuation());
            assertTrue(a.isModular());
        }

        @Test
        void iterator() {
            ValuationAnalysis a = Valuations.analyzeValuations(
                    build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"));
            assertNotNull(a);
        }
    }

    // =========================================================================
    // Benchmarks
    // =========================================================================

    @Nested
    class Benchmarks {
        @ParameterizedTest(name = "{1}")
        @CsvSource({
                "'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}',Iterator",
                "'&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}',File",
                "'&{a: end, b: end}',SimpleBranch",
                "'(&{a: end} || &{b: end})',SimpleParallel",
                "'&{a: &{b: &{c: end}}}',DeepChain",
                "'+{ok: end, err: end}',SimpleSelect",
                "'&{get: +{OK: end, NOT_FOUND: end}, put: +{OK: end, ERR: end}}',REST",
                "'rec X . &{request: +{OK: X, ERR: end}}',RetryLoop",
        })
        void analysisOnBenchmarks(String typeString, String name) {
            StateSpace ss = build(typeString);
            ValuationAnalysis a = Valuations.analyzeValuations(ss);
            assertNotNull(a, name);
            assertTrue(a.numStates() >= 1, name);
        }

        @ParameterizedTest(name = "{1}")
        @CsvSource({
                "'&{a: end, b: end}',SimpleBranch",
                "'(&{a: end} || &{b: end})',SimpleParallel",
                "'&{a: &{b: &{c: end}}}',DeepChain",
                "'+{ok: end, err: end}',SimpleSelect",
        })
        void fkgOnBenchmarks(String typeString, String name) {
            StateSpace ss = build(typeString);
            Map<Integer, Integer> rk = Valuations.rankFunction(ss);
            ToDoubleFunction<Integer> f = s -> (double) rk.get(s);
            FKGResult r = Valuations.checkFkg(ss, f, f, constant(1.0));
            assertTrue(r.holds(), name + " FKG should hold");
        }
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    @Nested
    class EdgeCases {
        @Test
        void singleState() {
            StateSpace ss = build("end");
            assertTrue(Valuations.isRankValuation(ss));
            ValuationAnalysis a = Valuations.analyzeValuations(ss);
            assertTrue(a.rankDefect() < 1e-10);
        }

        @Test
        void pureRecursion() {
            StateSpace ss = build("rec X . &{a: X}");
            Map<Integer, Integer> r = Valuations.rankFunction(ss);
            assertNotNull(r);
        }

        @Test
        void emptyMeasure() {
            StateSpace ss = build("&{a: end, b: end}");
            FKGResult r = Valuations.checkFkg(ss, constant(0.0), constant(0.0), constant(0.0));
            assertTrue(r.holds());
        }

        @Test
        void zeroValuation() {
            StateSpace ss = build("&{a: end, b: end}");
            ValuationResult r = Valuations.checkValuation(ss, constant(0.0));
            assertTrue(r.isValuation());
        }
    }
}
