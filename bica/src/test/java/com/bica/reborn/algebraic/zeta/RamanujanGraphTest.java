package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.algebraic.zeta.RamanujanGraph.DegreeStats;
import com.bica.reborn.algebraic.zeta.RamanujanGraph.RamanujanResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RamanujanGraph: port of Python tests/test_ramanujan.py
 * (36 tests, Step 31f). Cross-checked numerically against the Python
 * reference for the diamond, chain, and Alon-Boppana bound at d=3.
 */
class RamanujanGraphTest {

    private static final double TOL = 1e-9;

    private StateSpace ss(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // ---------------------------------------------------------------------
    // Degree statistics
    // ---------------------------------------------------------------------

    @Nested
    class DegreeStatistics {
        @Test
        void endStats() {
            DegreeStats stats = RamanujanGraph.degreeStats(ss("end"));
            assertEquals(0, stats.minDegree());
            assertEquals(0, stats.maxDegree());
            assertTrue(stats.isRegular());
        }

        @Test
        void simpleBranchStats() {
            DegreeStats stats = RamanujanGraph.degreeStats(ss("&{a: end}"));
            assertTrue(stats.minDegree() >= 1);
            assertTrue(stats.maxDegree() >= 1);
            assertEquals(2, stats.degreeSequence().size());
        }

        @Test
        void chainStats() {
            DegreeStats stats = RamanujanGraph.degreeStats(ss("&{a: &{b: end}}"));
            assertEquals(1, stats.minDegree());
            assertEquals(2, stats.maxDegree());
            assertFalse(stats.isRegular());
        }

        @Test
        void diamondStats() {
            DegreeStats stats =
                    RamanujanGraph.degreeStats(ss("&{a: &{c: end}, b: &{c: end}}"));
            assertTrue(stats.maxDegree() >= 2);
            assertTrue(stats.avgDegree() > 0);
        }

        @Test
        void parallelStats() {
            DegreeStats stats =
                    RamanujanGraph.degreeStats(ss("(&{a: end} || &{b: end})"));
            assertTrue(stats.maxDegree() >= 2);
        }
    }

    // ---------------------------------------------------------------------
    // Adjacency eigenvalues
    // ---------------------------------------------------------------------

    @Nested
    class AdjacencyEigenvalues {
        @Test
        void endEigenvalues() {
            double[] eigs = RamanujanGraph.adjacencyEigenvalues(ss("end"));
            assertEquals(1, eigs.length);
            assertTrue(Math.abs(eigs[0]) < 1e-10);
        }

        @Test
        void simpleBranchEigenvalues() {
            double[] eigs = RamanujanGraph.adjacencyEigenvalues(ss("&{a: end}"));
            assertEquals(2, eigs.length);
            assertTrue(Math.abs(eigs[0] - (-1.0)) < 0.1);
            assertTrue(Math.abs(eigs[1] - 1.0) < 0.1);
        }

        @Test
        void chainEigenvalues() {
            double[] eigs = RamanujanGraph.adjacencyEigenvalues(ss("&{a: &{b: end}}"));
            assertEquals(3, eigs.length);
            for (int i = 0; i < eigs.length - 1; i++) {
                assertTrue(eigs[i] <= eigs[i + 1] + 1e-10);
            }
        }

        @Test
        void eigenvaluesSorted() {
            double[] eigs = RamanujanGraph.adjacencyEigenvalues(
                    ss("&{a: &{c: end}, b: &{c: end}}"));
            for (int i = 0; i < eigs.length - 1; i++) {
                assertTrue(eigs[i] <= eigs[i + 1] + 1e-10);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Alon-Boppana bound
    // ---------------------------------------------------------------------

    @Nested
    class AlonBoppanaBound {
        @Test
        void degree1() {
            assertEquals(0.0, RamanujanGraph.alonBoppanaBound(1));
        }

        @Test
        void degree0() {
            assertEquals(0.0, RamanujanGraph.alonBoppanaBound(0));
        }

        @Test
        void degree2() {
            assertEquals(2.0, RamanujanGraph.alonBoppanaBound(2), 1e-10);
        }

        @Test
        void degree3() {
            double expected = 2.0 * Math.sqrt(2);
            assertEquals(expected, RamanujanGraph.alonBoppanaBound(3), 1e-10);
        }

        @Test
        void degree5() {
            assertEquals(4.0, RamanujanGraph.alonBoppanaBound(5), 1e-10);
        }

        @Test
        void monotone() {
            for (int d = 2; d < 10; d++) {
                assertTrue(RamanujanGraph.alonBoppanaBound(d)
                        <= RamanujanGraph.alonBoppanaBound(d + 1));
            }
        }
    }

    // ---------------------------------------------------------------------
    // is_ramanujan
    // ---------------------------------------------------------------------

    @Nested
    class IsRamanujan {
        @Test
        void endIsRamanujan() {
            assertTrue(RamanujanGraph.isRamanujan(ss("end")));
        }

        @Test
        void simpleBranchRamanujan() {
            assertTrue(RamanujanGraph.isRamanujan(ss("&{a: end}")));
        }

        @Test
        void chainRamanujan() {
            // Just check it returns a boolean (no exception).
            RamanujanGraph.isRamanujan(ss("&{a: &{b: end}}"));
        }

        @Test
        void diamondRamanujan() {
            RamanujanGraph.isRamanujan(ss("&{a: &{c: end}, b: &{c: end}}"));
        }

        @Test
        void parallelRamanujan() {
            RamanujanGraph.isRamanujan(ss("(&{a: end} || &{b: end})"));
        }

        @Test
        void recursiveRamanujan() {
            RamanujanGraph.isRamanujan(ss("rec X . &{a: X, b: end}"));
        }
    }

    // ---------------------------------------------------------------------
    // ramanujan_gap
    // ---------------------------------------------------------------------

    @Nested
    class RamanujanGap {
        @Test
        void endGapZero() {
            assertEquals(0.0, RamanujanGraph.ramanujanGap(ss("end")));
        }

        @Test
        void simpleBranchGap() {
            double gap = RamanujanGraph.ramanujanGap(ss("&{a: end}"));
            assertTrue(Double.isFinite(gap));
        }

        @Test
        void chainGap() {
            double gap = RamanujanGraph.ramanujanGap(ss("&{a: &{b: end}}"));
            assertTrue(Double.isFinite(gap));
        }

        @Test
        void ramanujanHasNonpositiveGap() {
            StateSpace s = ss("&{a: end}");
            if (RamanujanGraph.isRamanujan(s)) {
                assertTrue(RamanujanGraph.ramanujanGap(s) <= 1e-9);
            }
        }
    }

    // ---------------------------------------------------------------------
    // ramanujan_ratio
    // ---------------------------------------------------------------------

    @Nested
    class RamanujanRatio {
        @Test
        void endRatioZero() {
            assertEquals(0.0, RamanujanGraph.ramanujanRatio(ss("end")));
        }

        @Test
        void ratioNonnegative() {
            double ratio = RamanujanGraph.ramanujanRatio(ss("&{a: &{b: end}}"));
            assertTrue(ratio >= 0.0);
        }

        @Test
        void ramanujanRatioAtMostOne() {
            StateSpace s = ss("&{a: end}");
            if (RamanujanGraph.isRamanujan(s)) {
                assertTrue(RamanujanGraph.ramanujanRatio(s) <= 1.0 + 1e-9);
            }
        }
    }

    // ---------------------------------------------------------------------
    // optimal_expansion_check
    // ---------------------------------------------------------------------

    @Nested
    class OptimalExpansionCheck {
        @Test
        void endAnalysis() {
            RamanujanResult r = RamanujanGraph.optimalExpansionCheck(ss("end"));
            assertEquals(1, r.numStates());
            assertTrue(r.isRamanujan());
        }

        @Test
        void simpleBranchAnalysis() {
            RamanujanResult r = RamanujanGraph.optimalExpansionCheck(ss("&{a: end}"));
            assertEquals(2, r.numStates());
            assertTrue(r.numEdges() >= 1);
        }

        @Test
        void chainAnalysis() {
            RamanujanResult r =
                    RamanujanGraph.optimalExpansionCheck(ss("&{a: &{b: end}}"));
            assertEquals(3, r.numStates());
            assertTrue(r.degreeStats().maxDegree() >= 1);
            assertEquals(3, r.eigenvalues().length);
        }

        @Test
        void diamondAnalysis() {
            RamanujanResult r = RamanujanGraph.optimalExpansionCheck(
                    ss("&{a: &{c: end}, b: &{c: end}}"));
            assertTrue(r.alonBoppana() >= 0.0);
            // Cross-checked against Python: 4 states, 4 edges, 2-regular,
            // eigs [-2,0,0,2], nontrivial radius = 2.0, bound = 2.0, ratio = 1.0.
            assertEquals(4, r.numStates());
            assertEquals(4, r.numEdges());
            assertTrue(r.degreeStats().isRegular());
            assertEquals(2, r.degreeStats().maxDegree());
            assertEquals(2.0, r.alonBoppana(), 1e-9);
            assertEquals(2.0, r.nontrivialRadius(), 1e-9);
            assertTrue(r.isRamanujan());
            assertEquals(1.0, r.ramanujanRatio(), 1e-9);
            assertTrue(r.optimalExpansion());
        }

        @Test
        void parallelAnalysis() {
            RamanujanResult r = RamanujanGraph.optimalExpansionCheck(
                    ss("(&{a: end} || &{b: end})"));
            assertTrue(r.numStates() >= 3);
        }

        @Test
        void recursiveAnalysis() {
            RamanujanResult r = RamanujanGraph.optimalExpansionCheck(
                    ss("rec X . &{a: X, b: end}"));
            assertNotNull(r);
        }

        @Test
        void gapConsistentWithIsRamanujan() {
            StateSpace s = ss("&{a: &{c: end}, b: &{c: end}}");
            RamanujanResult r = RamanujanGraph.optimalExpansionCheck(s);
            if (r.isRamanujan()) {
                assertTrue(r.ramanujanGap() <= 1e-9);
            } else {
                assertTrue(r.ramanujanGap() > -1e-9);
            }
        }

        @Test
        void ratioConsistentWithIsRamanujan() {
            StateSpace s = ss("&{a: &{b: end}}");
            RamanujanResult r = RamanujanGraph.optimalExpansionCheck(s);
            if (r.isRamanujan() && r.alonBoppana() > 1e-15) {
                assertTrue(r.ramanujanRatio() <= 1.0 + 1e-9);
            }
        }
    }
}
