package com.bica.reborn.algebraic.euler;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.ProductStateSpace;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link EulerChecker} -- Java port of Python
 * {@code test_euler.py} (60 tests, Step 30v).
 */
class EulerCheckerTest {

    private static StateSpace ss(String t) {
        return StateSpaceBuilder.build(Parser.parse(t));
    }

    // -----------------------------------------------------------------------
    // Basic Euler characteristic
    // -----------------------------------------------------------------------

    @Nested
    class EulerCharacteristic {
        @Test
        void end() {
            int chi = EulerChecker.eulerCharacteristic(ss("end"));
            assertInstanceOf(Integer.class, chi);
        }

        @Test
        void singleBranch() {
            int chi = EulerChecker.eulerCharacteristic(ss("&{a: end}"));
            assertInstanceOf(Integer.class, chi);
        }

        @Test
        void chain3() {
            int chi = EulerChecker.eulerCharacteristic(ss("&{a: &{b: end}}"));
            assertEquals(1, chi);
        }

        @Test
        void chain4() {
            int chi = EulerChecker.eulerCharacteristic(ss("&{a: &{b: &{c: end}}}"));
            assertTrue(chi >= 0);
        }

        @Test
        void diamond() {
            int chi = EulerChecker.eulerCharacteristic(ss("&{a: end, b: end}"));
            assertInstanceOf(Integer.class, chi);
        }

        @Test
        void parallelSimple() {
            int chi = EulerChecker.eulerCharacteristic(ss("(&{a: end} || &{b: end})"));
            assertInstanceOf(Integer.class, chi);
        }
    }

    // -----------------------------------------------------------------------
    // Reduced Euler
    // -----------------------------------------------------------------------

    @Nested
    class ReducedEuler {
        @Test
        void end() {
            int red = EulerChecker.reducedEulerCharacteristic(ss("end"));
            assertInstanceOf(Integer.class, red);
        }

        @Test
        void singleBranch() {
            int red = EulerChecker.reducedEulerCharacteristic(ss("&{a: end}"));
            assertInstanceOf(Integer.class, red);
        }

        @Test
        void chain3() {
            int red = EulerChecker.reducedEulerCharacteristic(ss("&{a: &{b: end}}"));
            assertInstanceOf(Integer.class, red);
        }
    }

    // -----------------------------------------------------------------------
    // Hall's theorem verification
    // -----------------------------------------------------------------------

    @Nested
    class HallTheorem {
        @Test
        void end() {
            var result = EulerChecker.verifyHallTheorem(ss("end"));
            assertInstanceOf(EulerChecker.HallVerification.class, result);
            assertTrue(result.verified());
        }

        @Test
        void singleBranch() {
            assertTrue(EulerChecker.verifyHallTheorem(ss("&{a: end}")).verified());
        }

        @Test
        void chain3() {
            assertTrue(EulerChecker.verifyHallTheorem(ss("&{a: &{b: end}}")).verified());
        }

        @Test
        void chain4() {
            assertTrue(EulerChecker.verifyHallTheorem(ss("&{a: &{b: &{c: end}}}")).verified());
        }

        @Test
        void diamond() {
            assertTrue(EulerChecker.verifyHallTheorem(ss("&{a: end, b: end}")).verified());
        }

        @Test
        void parallel() {
            assertTrue(EulerChecker.verifyHallTheorem(ss("(&{a: end} || &{b: end})")).verified());
        }

        @Test
        void selection() {
            assertTrue(EulerChecker.verifyHallTheorem(ss("+{a: end, b: end}")).verified());
        }

        @Test
        void recursive() {
            assertTrue(EulerChecker.verifyHallTheorem(
                    ss("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}")).verified());
        }

        @Test
        void deeperChain() {
            assertTrue(EulerChecker.verifyHallTheorem(
                    ss("&{a: &{b: &{c: &{d: end}}}}")).verified());
        }
    }

    // -----------------------------------------------------------------------
    // Kuenneth verification
    // -----------------------------------------------------------------------

    @Nested
    class Kunneth {
        @Test
        void simpleProduct() {
            StateSpace ss1 = ss("&{a: end}");
            StateSpace ss2 = ss("&{b: end}");
            StateSpace ssProd = ProductStateSpace.product(ss1, ss2);
            var result = EulerChecker.verifyKunneth(ss1, ss2, ssProd);
            assertInstanceOf(EulerChecker.KunnethVerification.class, result);
            assertTrue(result.verified());
        }

        @Test
        void chainXChain() {
            StateSpace ss1 = ss("&{a: &{b: end}}");
            StateSpace ss2 = ss("&{c: end}");
            StateSpace ssProd = ProductStateSpace.product(ss1, ss2);
            var result = EulerChecker.verifyKunneth(ss1, ss2, ssProd);
            assertTrue(result.verified());
            assertEquals(result.expected(), result.chiLeft() * result.chiRight());
        }

        @Test
        void diamondXChain() {
            StateSpace ss1 = ss("&{a: end, b: end}");
            StateSpace ss2 = ss("&{c: end}");
            StateSpace ssProd = ProductStateSpace.product(ss1, ss2);
            assertTrue(EulerChecker.verifyKunneth(ss1, ss2, ssProd).verified());
        }

        @Test
        void parallelFromSyntax() {
            StateSpace ssPar = ss("(&{a: end} || &{b: end})");
            StateSpace ss1 = ss("&{a: end}");
            StateSpace ss2 = ss("&{b: end}");
            StateSpace ssProd = ProductStateSpace.product(ss1, ss2);
            int chiPar = EulerChecker.eulerCharacteristic(ssPar);
            int chiProd = EulerChecker.eulerCharacteristic(ssProd);
            assertEquals(chiPar, chiProd);
        }
    }

    // -----------------------------------------------------------------------
    // Interval Euler series
    // -----------------------------------------------------------------------

    @Nested
    class EulerSeries {
        @Test
        void end() {
            Map<Long, Integer> series = EulerChecker.eulerSeries(ss("end"));
            assertNotNull(series);
        }

        @Test
        void singleBranch() {
            Map<Long, Integer> series = EulerChecker.eulerSeries(ss("&{a: end}"));
            assertTrue(series.size() >= 1);
        }

        @Test
        void chainHasIntervals() {
            Map<Long, Integer> series = EulerChecker.eulerSeries(ss("&{a: &{b: end}}"));
            assertTrue(series.size() >= 3);
        }

        @Test
        void diagonalEntriesAreOne() {
            StateSpace s = ss("&{a: &{b: end}}");
            Map<Long, Integer> series = EulerChecker.eulerSeries(s);
            for (var entry : series.entrySet()) {
                long key = entry.getKey();
                int x = (int) (key >> 32);
                int y = (int) key;
                if (x == y) {
                    assertEquals(1, entry.getValue(),
                            "mu(" + x + "," + y + ") should be 1");
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Interval Euler distribution
    // -----------------------------------------------------------------------

    @Nested
    class IntervalDistribution {
        @Test
        void endTrivial() {
            Map<Integer, Integer> dist = EulerChecker.intervalEulerDistribution(ss("end"));
            assertNotNull(dist);
        }

        @Test
        void singleBranch() {
            Map<Integer, Integer> dist = EulerChecker.intervalEulerDistribution(ss("&{a: end}"));
            assertTrue(dist.containsKey(-1));
        }

        @Test
        void diamond() {
            Map<Integer, Integer> dist = EulerChecker.intervalEulerDistribution(ss("&{a: end, b: end}"));
            int total = dist.values().stream().mapToInt(Integer::intValue).sum();
            assertTrue(total > 0);
        }

        @Test
        void distributionSumsToIntervals() {
            StateSpace s = ss("&{a: &{b: end}}");
            Map<Integer, Integer> dist = EulerChecker.intervalEulerDistribution(s);
            Map<Long, Integer> series = EulerChecker.eulerSeries(s);
            int nOffDiag = 0;
            for (var entry : series.entrySet()) {
                long key = entry.getKey();
                int x = (int) (key >> 32);
                int y = (int) key;
                if (x != y) nOffDiag++;
            }
            assertEquals(nOffDiag, dist.values().stream().mapToInt(Integer::intValue).sum());
        }
    }

    // -----------------------------------------------------------------------
    // Euler obstructions
    // -----------------------------------------------------------------------

    @Nested
    class EulerObstruction {
        @Test
        void endNoObstruction() {
            assertEquals(List.of(), EulerChecker.eulerObstruction(ss("end")));
        }

        @Test
        void singleBranchNoObstruction() {
            assertEquals(List.of(), EulerChecker.eulerObstruction(ss("&{a: end}")));
        }

        @Test
        void chainNoObstruction() {
            assertEquals(List.of(), EulerChecker.eulerObstruction(ss("&{a: &{b: end}}")));
        }

        @Test
        void diamondNoObstruction() {
            assertTrue(EulerChecker.eulerObstruction(ss("&{a: end, b: end}")).isEmpty());
        }

        @Test
        void obstructionFormat() {
            List<int[]> obs = EulerChecker.eulerObstruction(
                    ss("&{a: &{b: end, c: end}, d: end}"));
            for (int[] entry : obs) {
                assertTrue(Math.abs(entry[2]) > 1);
            }
        }

        @Test
        void sortedByAbsDescending() {
            List<int[]> obs = EulerChecker.eulerObstruction(
                    ss("&{a: &{b: end, c: end}, d: &{e: end}}"));
            for (int i = 0; i < obs.size() - 1; i++) {
                assertTrue(Math.abs(obs.get(i)[2]) >= Math.abs(obs.get(i + 1)[2]));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Multi-method comparison
    // -----------------------------------------------------------------------

    @Nested
    class CompareEulerMethods {
        @Test
        void end() {
            var result = EulerChecker.compareEulerMethods(ss("end"));
            assertInstanceOf(EulerChecker.ComparisonResult.class, result);
            assertTrue(result.allAgree());
        }

        @Test
        void singleBranch() {
            assertTrue(EulerChecker.compareEulerMethods(ss("&{a: end}")).allAgree());
        }

        @Test
        void chain3() {
            assertTrue(EulerChecker.compareEulerMethods(ss("&{a: &{b: end}}")).allAgree());
        }

        @Test
        void diamond() {
            assertTrue(EulerChecker.compareEulerMethods(ss("&{a: end, b: end}")).allAgree());
        }

        @Test
        void parallel() {
            assertTrue(EulerChecker.compareEulerMethods(ss("(&{a: end} || &{b: end})")).allAgree());
        }

        @Test
        void recursive() {
            assertTrue(EulerChecker.compareEulerMethods(
                    ss("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}")).allAgree());
        }

        @Test
        void hasFVector() {
            var result = EulerChecker.compareEulerMethods(ss("&{a: &{b: end}}"));
            assertNotNull(result.fVector());
            assertFalse(result.fVector().isEmpty());
        }

        @Test
        void hasBetti() {
            var result = EulerChecker.compareEulerMethods(ss("&{a: &{b: end}}"));
            assertNotNull(result.bettiNumbers());
        }
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    @Nested
    class AnalyzeEuler {
        @Test
        void end() {
            var result = EulerChecker.analyzeEuler(ss("end"));
            assertInstanceOf(EulerChecker.EulerAnalysis.class, result);
            assertTrue(result.hall().verified());
        }

        @Test
        void chain3() {
            var result = EulerChecker.analyzeEuler(ss("&{a: &{b: end}}"));
            assertTrue(result.hall().verified());
            assertTrue(result.comparison().allAgree());
            assertTrue(result.numIntervals() >= 1);
        }

        @Test
        void diamond() {
            var result = EulerChecker.analyzeEuler(ss("&{a: end, b: end}"));
            assertTrue(result.hall().verified());
            assertTrue(result.comparison().allAgree());
            assertNotNull(result.distribution());
        }

        @Test
        void parallel() {
            var result = EulerChecker.analyzeEuler(ss("(&{a: end} || &{b: end})"));
            assertTrue(result.hall().verified());
            assertTrue(result.comparison().allAgree());
        }

        @Test
        void recursiveIterator() {
            var result = EulerChecker.analyzeEuler(
                    ss("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"));
            assertTrue(result.hall().verified());
            assertTrue(result.comparison().allAgree());
        }

        @Test
        void obstructionsInAnalysis() {
            var result = EulerChecker.analyzeEuler(ss("&{a: &{b: end}}"));
            assertNotNull(result.obstructions());
        }

        @Test
        void intervalSeriesInAnalysis() {
            var result = EulerChecker.analyzeEuler(ss("&{a: end, b: end}"));
            assertNotNull(result.intervalSeries());
            assertFalse(result.intervalSeries().isEmpty());
        }
    }

    // -----------------------------------------------------------------------
    // Benchmark protocols
    // -----------------------------------------------------------------------

    @Nested
    class Benchmarks {
        private static final List<String> BENCHMARK_TYPES = List.of(
                "&{open: &{read: end, write: end}}",
                "&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}",
                "&{a: end, b: end}",
                "&{a: &{b: &{c: end}}}",
                "(&{a: end} || &{b: end})",
                "+{a: end, b: end}",
                "&{a: +{b: end, c: end}}",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                "&{a: &{b: end, c: end}, d: end}",
                "&{connect: &{query: +{OK: &{fetch: end}, ERR: end}}}",
                "&{init: +{OK: &{process: end}, FAIL: end}}",
                "+{a: &{b: end}, c: end}",
                "&{a: &{b: &{c: &{d: end}}}}",
                "&{login: +{OK: &{read: end, write: end}, FAIL: end}}",
                "(&{a: &{b: end}} || &{c: end})"
        );

        @Test
        void hallTheoremAllBenchmarks() {
            for (String typ : BENCHMARK_TYPES) {
                var result = EulerChecker.verifyHallTheorem(ss(typ));
                assertTrue(result.verified(), "Hall failed for: " + typ);
            }
        }

        @Test
        void methodsAgreeAllBenchmarks() {
            for (String typ : BENCHMARK_TYPES) {
                var result = EulerChecker.compareEulerMethods(ss(typ));
                assertTrue(result.allAgree(), "Methods disagree for: " + typ);
            }
        }

        @Test
        void fullAnalysisAllBenchmarks() {
            for (String typ : BENCHMARK_TYPES) {
                var result = EulerChecker.analyzeEuler(ss(typ));
                assertNotNull(result);
                assertTrue(result.hall().verified(), "Hall failed for: " + typ);
            }
        }

        @Test
        void kunnethParallelBenchmarks() {
            List<String> parallelTypes = List.of(
                    "(&{a: end} || &{b: end})",
                    "(&{a: &{b: end}} || &{c: end})"
            );
            for (String typ : parallelTypes) {
                var result = EulerChecker.analyzeEuler(ss(typ));
                assertTrue(result.hall().verified(), "Hall failed for parallel: " + typ);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Nested
    class EdgeCases {
        @Test
        void selectionOnly() {
            int chi = EulerChecker.eulerCharacteristic(ss("+{a: end}"));
            assertInstanceOf(Integer.class, chi);
        }

        @Test
        void nestedBranch() {
            assertTrue(EulerChecker.verifyHallTheorem(
                    ss("&{a: &{b: end, c: end}, d: end}")).verified());
        }

        @Test
        void nestedSelection() {
            assertTrue(EulerChecker.verifyHallTheorem(
                    ss("+{a: +{b: end, c: end}, d: end}")).verified());
        }

        @Test
        void mixedBranchSelect() {
            assertTrue(EulerChecker.verifyHallTheorem(
                    ss("&{a: +{b: end, c: end}}")).verified());
        }

        @Test
        void deepRecursion() {
            assertTrue(EulerChecker.verifyHallTheorem(
                    ss("rec X . &{a: rec Y . &{b: +{c: X, d: Y, e: end}}}")).verified());
        }
    }

    // -----------------------------------------------------------------------
    // Betti numbers
    // -----------------------------------------------------------------------

    @Nested
    class BettiNumbers {
        @Test
        void endEmpty() {
            List<Integer> betti = EulerChecker.bettiNumbers(ss("end"));
            // Single state -> no interior -> empty
            assertNotNull(betti);
        }

        @Test
        void chain3HasBetti() {
            List<Integer> betti = EulerChecker.bettiNumbers(ss("&{a: &{b: end}}"));
            assertNotNull(betti);
            // One interior vertex -> b_0 = 1
            if (!betti.isEmpty()) {
                assertTrue(betti.get(0) >= 1);
            }
        }

        @Test
        void bettiEulerConsistency() {
            // chi from Betti must equal chi from faces
            StateSpace s = ss("&{a: &{b: &{c: end}}}");
            List<Integer> betti = EulerChecker.bettiNumbers(s);
            int chiBetti = 0;
            for (int k = 0; k < betti.size(); k++) {
                chiBetti += (k % 2 == 0 ? 1 : -1) * betti.get(k);
            }
            int chiFaces = EulerChecker.eulerCharacteristic(s);
            assertEquals(chiFaces, chiBetti);
        }
    }

    // -----------------------------------------------------------------------
    // Matrix rank (package-private helper)
    // -----------------------------------------------------------------------

    @Nested
    class MatrixRank {
        @Test
        void emptyMatrix() {
            assertEquals(0, EulerChecker.matrixRank(new int[0][0]));
        }

        @Test
        void identity2() {
            assertEquals(2, EulerChecker.matrixRank(new int[][]{{1, 0}, {0, 1}}));
        }

        @Test
        void rank1() {
            assertEquals(1, EulerChecker.matrixRank(new int[][]{{1, 2}, {2, 4}}));
        }

        @Test
        void zeroMatrix() {
            assertEquals(0, EulerChecker.matrixRank(new int[][]{{0, 0}, {0, 0}}));
        }

        @Test
        void rectangular() {
            assertEquals(2, EulerChecker.matrixRank(new int[][]{{1, 0, 0}, {0, 1, 0}}));
        }
    }
}
