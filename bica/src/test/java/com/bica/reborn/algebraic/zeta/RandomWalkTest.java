package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RandomWalk: port of Python tests/test_random_walk.py (65 tests).
 */
class RandomWalkTest {

    private static final double EPS = 1e-8;

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Transition matrix
    // -----------------------------------------------------------------------

    @Nested
    class TransitionMatrix {

        @Test
        void endType1x1() {
            var ss = build("end");
            double[][] P = RandomWalk.transitionMatrix(ss);
            assertEquals(1, P.length);
            assertEquals(1, P[0].length);
            assertEquals(1.0, P[0][0], EPS);
        }

        @Test
        void singleBranch2x2() {
            var ss = build("&{a: end}");
            double[][] P = RandomWalk.transitionMatrix(ss);
            assertEquals(2, P.length);
        }

        @Test
        void rowSumsSingleBranch() {
            double[][] P = RandomWalk.transitionMatrix(build("&{a: end}"));
            for (double[] row : P) {
                double s = 0;
                for (double v : row) s += v;
                assertEquals(1.0, s, EPS);
            }
        }

        @Test
        void rowSumsTwoBranches() {
            double[][] P = RandomWalk.transitionMatrix(build("&{a: end, b: end}"));
            for (double[] row : P) {
                double s = 0;
                for (double v : row) s += v;
                assertEquals(1.0, s, EPS);
            }
        }

        @Test
        void entriesInUnitInterval() {
            double[][] P = RandomWalk.transitionMatrix(build("&{a: &{c: end}, b: end}"));
            for (double[] row : P)
                for (double v : row)
                    assertTrue(v >= 0.0 && v <= 1.0 + 1e-12);
        }

        @Test
        void uniformProbabilities() {
            double[][] P = RandomWalk.transitionMatrix(build("&{a: end, b: end}"));
            int nonAbsorbing = 0;
            for (int i = 0; i < P.length; i++) if (P[i][i] < 0.99) nonAbsorbing++;
            assertTrue(nonAbsorbing >= 1);
        }

        @Test
        void absorbingStateSelfLoop() {
            var ss = build("&{a: end}");
            double[][] P = RandomWalk.transitionMatrix(ss);
            List<Integer> states = Zeta.stateList(ss);
            int bi = states.indexOf(ss.bottom());
            assertEquals(1.0, P[bi][bi], EPS);
        }

        @Test
        void dimensionsMatchStates() {
            var ss = build("&{a: &{b: end}, c: end}");
            double[][] P = RandomWalk.transitionMatrix(ss);
            int n = ss.states().size();
            assertEquals(n, P.length);
            for (double[] row : P) assertEquals(n, row.length);
        }

        @Test
        void chainType() {
            double[][] P = RandomWalk.transitionMatrix(build("&{a: &{b: &{c: end}}}"));
            for (double[] row : P) {
                double s = 0;
                for (double v : row) s += v;
                assertEquals(1.0, s, EPS);
            }
        }

        @Test
        void diamondRowSums() {
            double[][] P = RandomWalk.transitionMatrix(build("&{a: &{c: end}, b: &{c: end}}"));
            for (double[] row : P) {
                double s = 0;
                for (double v : row) s += v;
                assertEquals(1.0, s, EPS);
            }
        }

        @Test
        void parallelRowSums() {
            double[][] P = RandomWalk.transitionMatrix(build("(&{a: end} || &{b: end})"));
            for (double[] row : P) {
                double s = 0;
                for (double v : row) s += v;
                assertEquals(1.0, s, EPS);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Stationary distribution
    // -----------------------------------------------------------------------

    @Nested
    class Stationary {

        @Test
        void endType() {
            double[] pi = RandomWalk.stationaryDistribution(build("end"));
            assertEquals(1, pi.length);
            assertEquals(1.0, pi[0], EPS);
        }

        @Test
        void sumsToOne() {
            double[] pi = RandomWalk.stationaryDistribution(build("&{a: end, b: end}"));
            double s = 0;
            for (double v : pi) s += v;
            assertEquals(1.0, s, EPS);
        }

        @Test
        void isActuallyStationary() {
            var ss = build("&{a: &{c: end}, b: end}");
            double[] pi = RandomWalk.stationaryDistribution(ss);
            double[][] P = RandomWalk.transitionMatrix(ss);
            int n = pi.length;
            double[] piP = new double[n];
            for (int j = 0; j < n; j++)
                for (int i = 0; i < n; i++)
                    piP[j] += pi[i] * P[i][j];
            for (int j = 0; j < n; j++) assertEquals(pi[j], piP[j], 1e-8);
        }

        @Test
        void absorbingStateConcentration() {
            var ss = build("&{a: end}");
            double[] pi = RandomWalk.stationaryDistribution(ss);
            int bi = Zeta.stateList(ss).indexOf(ss.bottom());
            assertEquals(1.0, pi[bi], 0.01);
        }

        @Test
        void nonNegative() {
            double[] pi = RandomWalk.stationaryDistribution(build("&{a: &{b: end}, c: end}"));
            for (double p : pi) assertTrue(p >= -1e-12);
        }

        @Test
        void chainConcentratesAtBottom() {
            var ss = build("&{a: &{b: &{c: end}}}");
            double[] pi = RandomWalk.stationaryDistribution(ss);
            int bi = Zeta.stateList(ss).indexOf(ss.bottom());
            assertTrue(pi[bi] > 0.9);
        }

        @Test
        void parallelSumsToOne() {
            double[] pi = RandomWalk.stationaryDistribution(build("(&{a: end} || &{b: end})"));
            double s = 0;
            for (double v : pi) s += v;
            assertEquals(1.0, s, EPS);
        }
    }

    // -----------------------------------------------------------------------
    // Spectral gap
    // -----------------------------------------------------------------------

    @Nested
    class SpectralGap {

        @Test
        void endTypeGapOne() {
            assertEquals(1.0, RandomWalk.spectralGap(build("end")), EPS);
        }

        @Test
        void nonNegative() {
            assertTrue(RandomWalk.spectralGap(build("&{a: end, b: end}")) >= -1e-12);
        }

        @Test
        void atMostOne() {
            assertTrue(RandomWalk.spectralGap(build("&{a: &{c: end}, b: end}")) <= 1.0 + 1e-12);
        }

        @Test
        void chainGap() {
            double gap = RandomWalk.spectralGap(build("&{a: &{b: end}}"));
            assertTrue(gap >= 0.0 && gap <= 1.0);
        }

        @Test
        void diamondGap() {
            double gap = RandomWalk.spectralGap(build("&{a: &{c: end}, b: &{c: end}}"));
            assertTrue(gap >= 0.0 && gap <= 1.0);
        }
    }

    // -----------------------------------------------------------------------
    // Eigenvalues
    // -----------------------------------------------------------------------

    @Nested
    class Eigenvalues {

        @Test
        void endTypeEigenvalueOne() {
            double[] eigs = RandomWalk.eigenvaluesOfP(build("end"));
            assertEquals(1, eigs.length);
            assertEquals(1.0, eigs[0], EPS);
        }

        @Test
        void largestEigenvalueIsOne() {
            double[] eigs = RandomWalk.eigenvaluesOfP(build("&{a: end, b: end}"));
            assertEquals(1.0, Math.abs(eigs[0]), 0.05);
        }

        @Test
        void countMatchesStates() {
            var ss = build("&{a: &{b: end}, c: end}");
            double[] eigs = RandomWalk.eigenvaluesOfP(ss);
            assertEquals(ss.states().size(), eigs.length);
        }

        @Test
        void sortedDescendingMagnitude() {
            double[] eigs = RandomWalk.eigenvaluesOfP(build("&{a: &{c: end}, b: &{c: end}}"));
            for (int i = 0; i < eigs.length - 1; i++)
                assertTrue(Math.abs(eigs[i]) >= Math.abs(eigs[i + 1]) - 1e-8);
        }

        @Test
        void allMagnitudesAtMostOne() {
            double[] eigs = RandomWalk.eigenvaluesOfP(build("&{a: &{b: end}, c: end}"));
            for (double e : eigs) assertTrue(Math.abs(e) <= 1.0 + 0.1);
        }
    }

    // -----------------------------------------------------------------------
    // Hitting time
    // -----------------------------------------------------------------------

    @Nested
    class HittingTime {

        @Test
        void selfHittingTimeZero() {
            var ss = build("&{a: end}");
            assertEquals(0.0, RandomWalk.hittingTime(ss, ss.top(), ss.top()));
        }

        @Test
        void topToBottomPositive() {
            var ss = build("&{a: end}");
            assertTrue(RandomWalk.hittingTime(ss, ss.top(), ss.bottom()) > 0.0);
        }

        @Test
        void chainHittingTime() {
            var ss = build("&{a: &{b: end}}");
            assertEquals(2.0, RandomWalk.hittingTime(ss, ss.top(), ss.bottom()), 0.1);
        }

        @Test
        void singleStep() {
            var ss = build("&{a: end}");
            assertEquals(1.0, RandomWalk.hittingTime(ss, ss.top(), ss.bottom()), 0.1);
        }

        @Test
        void bottomToTopInfinite() {
            var ss = build("&{a: &{b: end}}");
            if (ss.top() != ss.bottom()) {
                assertTrue(Double.isInfinite(RandomWalk.hittingTime(ss, ss.bottom(), ss.top())));
            }
        }
    }

    // -----------------------------------------------------------------------
    // All hitting times
    // -----------------------------------------------------------------------

    @Nested
    class AllHittingTimes {

        @Test
        void diagonalZero() {
            var ss = build("&{a: end, b: end}");
            var ht = RandomWalk.allHittingTimes(ss);
            for (int s : ss.states()) {
                assertEquals(0.0, ht.get(new Zeta.Pair(s, s)));
            }
        }

        @Test
        void dictSize() {
            var ss = build("&{a: end}");
            var ht = RandomWalk.allHittingTimes(ss);
            int n = ss.states().size();
            assertEquals(n * n, ht.size());
        }
    }

    // -----------------------------------------------------------------------
    // Return time
    // -----------------------------------------------------------------------

    @Nested
    class ReturnTime {

        @Test
        void endType() {
            var ss = build("end");
            assertEquals(1.0, RandomWalk.returnTime(ss, ss.top()), EPS);
        }

        @Test
        void absorbingBottom() {
            var ss = build("&{a: end}");
            assertEquals(1.0, RandomWalk.returnTime(ss, ss.bottom()), 0.1);
        }

        @Test
        void transientStateLarge() {
            var ss = build("&{a: &{b: end}}");
            if (ss.top() != ss.bottom()) {
                double rt = RandomWalk.returnTime(ss, ss.top());
                assertTrue(rt > 10.0 || Double.isInfinite(rt));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Commute time
    // -----------------------------------------------------------------------

    @Nested
    class CommuteTime {

        @Test
        void selfCommuteZero() {
            var ss = build("&{a: end}");
            assertEquals(0.0, RandomWalk.commuteTime(ss, ss.top(), ss.top()));
        }

        @Test
        void dagInfiniteCommute() {
            var ss = build("&{a: end}");
            if (ss.top() != ss.bottom()) {
                assertTrue(Double.isInfinite(RandomWalk.commuteTime(ss, ss.top(), ss.bottom())));
            }
        }

        @Test
        void endCommute() {
            var ss = build("end");
            assertEquals(0.0, RandomWalk.commuteTime(ss, ss.top(), ss.top()));
        }
    }

    // -----------------------------------------------------------------------
    // Mixing time
    // -----------------------------------------------------------------------

    @Nested
    class MixingTime {

        @Test
        void endTypeZero() {
            assertEquals(0, RandomWalk.mixingTimeBound(build("end")));
        }

        @Test
        void positiveForNontrivial() {
            assertTrue(RandomWalk.mixingTimeBound(build("&{a: end}")) >= 1);
        }

        @Test
        void integerResult() {
            int v = RandomWalk.mixingTimeBound(build("&{a: end, b: end}"));
            assertTrue(v >= 0); // is-int trivially holds in Java
        }

        @Test
        void chainLargerThanBranch() {
            int shortM = RandomWalk.mixingTimeBound(build("&{a: end}"));
            int longM = RandomWalk.mixingTimeBound(build("&{a: &{b: &{c: end}}}"));
            assertTrue(longM >= shortM);
        }
    }

    // -----------------------------------------------------------------------
    // Cover time
    // -----------------------------------------------------------------------

    @Nested
    class CoverTime {

        @Test
        void endTypeZero() {
            assertEquals(0.0, RandomWalk.coverTimeBound(build("end")));
        }

        @Test
        void nonNegative() {
            assertTrue(RandomWalk.coverTimeBound(build("&{a: end, b: end}")) >= 0.0);
        }

        @Test
        void atLeastHitting() {
            var ss = build("&{a: &{c: end}, b: end}");
            double cover = RandomWalk.coverTimeBound(ss);
            var ht = RandomWalk.allHittingTimes(ss);
            for (var e : ht.entrySet()) {
                if (e.getKey().x() == ss.top() && !Double.isInfinite(e.getValue())) {
                    assertTrue(cover >= e.getValue() - 1e-6);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Ergodicity
    // -----------------------------------------------------------------------

    @Nested
    class Ergodic {

        @Test
        void endTypeErgodic() {
            assertTrue(RandomWalk.isErgodic(build("end")));
        }

        @Test
        void dagNotErgodic() {
            var ss = build("&{a: end}");
            if (ss.top() != ss.bottom()) assertFalse(RandomWalk.isErgodic(ss));
        }

        @Test
        void chainNotErgodic() {
            assertFalse(RandomWalk.isErgodic(build("&{a: &{b: end}}")));
        }

        @Test
        void diamondNotErgodic() {
            assertFalse(RandomWalk.isErgodic(build("&{a: &{c: end}, b: &{c: end}}")));
        }

        @Test
        void recursiveType() {
            assertFalse(RandomWalk.isErgodic(build("rec X . &{a: X, b: end}")));
        }
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    @Nested
    class Analyze {

        @Test
        void resultType() {
            var r = RandomWalk.analyzeRandomWalk(build("&{a: end}"));
            assertNotNull(r);
        }

        @Test
        void endTypeAnalysis() {
            var r = RandomWalk.analyzeRandomWalk(build("end"));
            assertTrue(r.isErgodic());
            assertEquals(1.0, r.spectralGap(), EPS);
            assertEquals(0, r.mixingTimeBound());
            double s = 0;
            for (double v : r.stationaryDistribution()) s += v;
            assertEquals(1.0, s, EPS);
        }

        @Test
        void branchAnalysis() {
            var ss = build("&{a: end, b: end}");
            var r = RandomWalk.analyzeRandomWalk(ss);
            assertEquals(ss.states().size(), r.transitionMatrix().length);
            double s = 0;
            for (double v : r.stationaryDistribution()) s += v;
            assertEquals(1.0, s, EPS);
            assertTrue(r.spectralGap() >= 0.0);
            assertTrue(r.mixingTimeBound() >= 1);
            assertTrue(r.coverTimeBound() >= 0.0);
        }

        @Test
        void diamondAnalysis() {
            var ss = build("&{a: &{c: end}, b: &{c: end}}");
            var r = RandomWalk.analyzeRandomWalk(ss);
            assertEquals(ss.states().size(), r.eigenvalues().length);
            assertFalse(r.isErgodic());
        }

        @Test
        void parallelAnalysis() {
            var ss = build("(&{a: end} || &{b: end})");
            var r = RandomWalk.analyzeRandomWalk(ss);
            assertEquals(ss.states().size(), r.transitionMatrix().length);
            double s = 0;
            for (double v : r.stationaryDistribution()) s += v;
            assertEquals(1.0, s, EPS);
        }

        @Test
        void recursiveAnalysis() {
            var ss = build("rec X . &{a: X, b: end}");
            var r = RandomWalk.analyzeRandomWalk(ss);
            assertEquals(ss.states().size(), r.eigenvalues().length);
            assertTrue(r.coverTimeBound() >= 0.0);
        }

        @Test
        void hittingTimesInResult() {
            var ss = build("&{a: end}");
            var r = RandomWalk.analyzeRandomWalk(ss);
            int n = ss.states().size();
            assertEquals(n * n, r.hittingTimes().size());
        }
    }

    // -----------------------------------------------------------------------
    // Benchmarks
    // -----------------------------------------------------------------------

    @Nested
    class Benchmarks {

        @Test
        void iterator() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var r = RandomWalk.analyzeRandomWalk(ss);
            assertTrue(r.transitionMatrix().length > 0);
            double s = 0;
            for (double v : r.stationaryDistribution()) s += v;
            assertEquals(1.0, s, EPS);
            assertFalse(r.isErgodic());
        }

        @Test
        void smtp() {
            var ss = build("&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}");
            RandomWalk.analyzeRandomWalk(ss);
            double h = RandomWalk.hittingTime(ss, ss.top(), ss.bottom());
            assertEquals(4.0, h, 0.5);
        }

        @Test
        void fileObject() {
            var ss = build("&{open: &{read: end, write: end}}");
            var r = RandomWalk.analyzeRandomWalk(ss);
            double s = 0;
            for (double v : r.stationaryDistribution()) s += v;
            assertEquals(1.0, s, EPS);
            assertTrue(r.coverTimeBound() >= 0.0);
        }

        @Test
        void parallelFile() {
            var ss = build("(&{read: end} || &{write: end})");
            var r = RandomWalk.analyzeRandomWalk(ss);
            assertEquals(ss.states().size(), r.transitionMatrix().length);
            for (double[] row : r.transitionMatrix()) {
                double s = 0;
                for (double v : row) s += v;
                assertEquals(1.0, s, EPS);
            }
        }

        @Test
        void selectionType() {
            var ss = build("+{OK: end, ERR: end}");
            var r = RandomWalk.analyzeRandomWalk(ss);
            double s = 0;
            for (double v : r.stationaryDistribution()) s += v;
            assertEquals(1.0, s, EPS);
        }
    }
}
