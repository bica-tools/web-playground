package com.bica.reborn.congruence;

import com.bica.reborn.congruence.FactorizationChecker.FactorPair;
import com.bica.reborn.congruence.FactorizationChecker.FactorizationAnalysis;
import com.bica.reborn.congruence.FactorizationChecker.IndexedFactorPair;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for lattice factorization theory (Step 363f).
 * Mirrors all 38 Python tests from test_factorize.py.
 */
class FactorizationCheckerTest {

    private record Built(StateSpace ss, LatticeResult lr) {}

    private Built build(String type) {
        StateSpace ss = StateSpaceBuilder.build(Parser.parse(type));
        return new Built(ss, LatticeChecker.checkLattice(ss));
    }

    private StateSpace ssOf(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Indecomposable
    // -----------------------------------------------------------------------

    @Nested
    class Indecomposable {

        @Test
        void endIndecomposable() {
            assertFalse(FactorizationChecker.isDirectlyDecomposable(ssOf("end")));
        }

        @Test
        void singleBranchIndecomposable() {
            assertFalse(FactorizationChecker.isDirectlyDecomposable(ssOf("&{a: end}")));
        }

        @Test
        void chain2Indecomposable() {
            var factors = FactorizationChecker.factorize(ssOf("&{a: end}"));
            assertEquals(1, factors.size());
        }

        @Test
        void chain3Indecomposable() {
            StateSpace ss = ssOf("&{a: &{b: end}}");
            assertFalse(FactorizationChecker.isDirectlyDecomposable(ss));
            assertEquals(1, FactorizationChecker.factorize(ss).size());
        }

        @Test
        void branchTwoOptionsIndecomposable() {
            assertFalse(FactorizationChecker.isDirectlyDecomposable(ssOf("&{a: end, b: end}")));
        }

        @Test
        void selectTwoOptionsIndecomposable() {
            assertFalse(FactorizationChecker.isDirectlyDecomposable(ssOf("+{ok: end, err: end}")));
        }

        @Test
        void recursiveIteratorIndecomposable() {
            StateSpace ss = ssOf(
                    "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            assertFalse(FactorizationChecker.isDirectlyDecomposable(ss));
        }

        @Test
        void factorizeEndReturnsSelf() {
            StateSpace ss = ssOf("end");
            var factors = FactorizationChecker.factorize(ss);
            assertEquals(1, factors.size());
            assertSame(ss, factors.get(0));
        }
    }

    // -----------------------------------------------------------------------
    // Decomposable
    // -----------------------------------------------------------------------

    @Nested
    class Decomposable {

        @Test
        void parallelTwoChains() {
            var b = build("(&{a: end} || &{b: end})");
            assertTrue(b.lr().isLattice());
            assertTrue(FactorizationChecker.isDirectlyDecomposable(b.ss(), b.lr()));
        }

        @Test
        void parallelFactorCount() {
            var factors = FactorizationChecker.factorize(ssOf("(&{a: end} || &{b: end})"));
            assertEquals(2, factors.size());
        }

        @Test
        void parallelFactorSizes() {
            var factors = FactorizationChecker.factorize(ssOf("(&{a: end} || &{b: end})"));
            List<Integer> sizes = new ArrayList<>();
            for (var f : factors) sizes.add(f.states().size());
            sizes.sort(Integer::compareTo);
            assertEquals(List.of(2, 2), sizes);
        }

        @Test
        void parallelFactorCongruencesExist() {
            var pairs = FactorizationChecker.findFactorCongruences(
                    ssOf("(&{a: end} || &{b: end})"));
            assertTrue(pairs.size() >= 1);
        }

        @Test
        void parallelIndexedPairs() {
            StateSpace ss = ssOf("(&{a: end} || &{b: end})");
            var indexed = FactorizationChecker.findFactorCongruencesIndexed(ss);
            assertTrue(indexed.size() >= 1);
            var conLat = CongruenceChecker.congruenceLattice(ss);
            int n = conLat.congruences().size();
            for (var p : indexed) {
                assertTrue(p.i() >= 0 && p.i() < n);
                assertTrue(p.j() >= 0 && p.j() < n);
            }
        }

        @Test
        void parallelAsymmetricBranches() {
            var b = build("(&{a: end} || &{b: &{c: end}})");
            assertTrue(b.lr().isLattice());
            assertTrue(FactorizationChecker.isDirectlyDecomposable(b.ss(), b.lr()));
            var factors = FactorizationChecker.factorize(b.ss(), b.lr());
            assertEquals(2, factors.size());
            List<Integer> sizes = new ArrayList<>();
            for (var f : factors) sizes.add(f.states().size());
            sizes.sort(Integer::compareTo);
            assertEquals(List.of(2, 3), sizes);
        }
    }

    // -----------------------------------------------------------------------
    // Factor congruence pair properties
    // -----------------------------------------------------------------------

    @Nested
    class FactorCongruenceProperties {

        @Test
        void meetIsIdentity() {
            StateSpace ss = ssOf("(&{a: end} || &{b: end})");
            var pairs = FactorizationChecker.findFactorCongruences(ss);
            assertTrue(pairs.size() >= 1);
            FactorPair p = pairs.get(0);
            Congruence meet = FactorizationChecker.meetCongruences(ss, p.theta(), p.phi());
            for (Set<Integer> c : meet.classes()) assertEquals(1, c.size());
        }

        @Test
        void joinIsTotal() {
            StateSpace ss = ssOf("(&{a: end} || &{b: end})");
            var pairs = FactorizationChecker.findFactorCongruences(ss);
            assertTrue(pairs.size() >= 1);
            FactorPair p = pairs.get(0);
            Congruence join = FactorizationChecker.joinCongruences(ss, p.theta(), p.phi());
            assertEquals(1, join.numClasses());
        }

        @Test
        void noFactorPairsForChain() {
            var pairs = FactorizationChecker.findFactorCongruences(ssOf("&{a: &{b: end}}"));
            assertTrue(pairs.isEmpty());
        }
    }

    // -----------------------------------------------------------------------
    // Meet congruence helper
    // -----------------------------------------------------------------------

    @Nested
    class MeetCongruences {

        @Test
        void meetWithIdentity() {
            StateSpace ss = ssOf("(&{a: end} || &{b: end})");
            var conLat = CongruenceChecker.congruenceLattice(ss);
            Congruence identity = null;
            for (Congruence c : conLat.congruences()) {
                if (c.isTrivialBottom()) { identity = c; break; }
            }
            assertNotNull(identity);
            for (Congruence c : conLat.congruences()) {
                Congruence meet = FactorizationChecker.meetCongruences(ss, c, identity);
                assertTrue(FactorizationChecker.congruencesEqual(meet, identity));
            }
        }

        @Test
        void meetWithTotal() {
            StateSpace ss = ssOf("(&{a: end} || &{b: end})");
            var conLat = CongruenceChecker.congruenceLattice(ss);
            Congruence total = conLat.congruences().get(conLat.top());
            for (Congruence c : conLat.congruences()) {
                Congruence meet = FactorizationChecker.meetCongruences(ss, c, total);
                assertTrue(FactorizationChecker.congruencesEqual(meet, c));
            }
        }

        @Test
        void meetIsCommutative() {
            StateSpace ss = ssOf("(&{a: end} || &{b: end})");
            var congs = CongruenceChecker.enumerateCongruences(ss);
            if (congs.size() >= 2) {
                Congruence c1 = congs.get(0), c2 = congs.get(1);
                Congruence m1 = FactorizationChecker.meetCongruences(ss, c1, c2);
                Congruence m2 = FactorizationChecker.meetCongruences(ss, c2, c1);
                assertTrue(FactorizationChecker.congruencesEqual(m1, m2));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Analysis dataclass
    // -----------------------------------------------------------------------

    @Nested
    class Analysis {

        @Test
        void analysisIndecomposable() {
            FactorizationAnalysis r = FactorizationChecker.analyzeFactorization(ssOf("&{a: end}"));
            assertNotNull(r);
            assertFalse(r.isDecomposable());
            assertTrue(r.isIndecomposable());
            assertEquals(1, r.factorCount());
            assertEquals(List.of(2), r.factorSizes());
            assertTrue(r.factorCongruencePairs().isEmpty());
        }

        @Test
        void analysisDecomposable() {
            FactorizationAnalysis r = FactorizationChecker.analyzeFactorization(
                    ssOf("(&{a: end} || &{b: end})"));
            assertTrue(r.isDecomposable());
            assertFalse(r.isIndecomposable());
            assertEquals(2, r.factorCount());
            List<Integer> sizes = new ArrayList<>(r.factorSizes());
            sizes.sort(Integer::compareTo);
            assertEquals(List.of(2, 2), sizes);
            assertTrue(r.factorCongruencePairs().size() >= 1);
        }

        @Test
        void analysisEnd() {
            FactorizationAnalysis r = FactorizationChecker.analyzeFactorization(ssOf("end"));
            assertFalse(r.isDecomposable());
            assertTrue(r.isIndecomposable());
            assertEquals(1, r.factorCount());
        }

        @Test
        void analysisNonLattice() {
            // Manually build a possibly-non-lattice state space (4 states diamond shape).
            var transitions = List.of(
                    new StateSpace.Transition(0, "a", 1),
                    new StateSpace.Transition(0, "b", 2),
                    new StateSpace.Transition(1, "c", 3),
                    new StateSpace.Transition(2, "d", 3)
            );
            StateSpace ss = new StateSpace(Set.of(0, 1, 2, 3), transitions, 0, 3, Map.of());
            FactorizationAnalysis r = FactorizationChecker.analyzeFactorization(ss);
            assertNotNull(r);
            assertTrue(r.factorCount() >= 1);
        }

        @Test
        void analysisFactorsAreStateSpaces() {
            FactorizationAnalysis r = FactorizationChecker.analyzeFactorization(
                    ssOf("(&{a: end} || &{b: end})"));
            for (var f : r.factors()) assertNotNull(f);
        }
    }

    // -----------------------------------------------------------------------
    // Benchmarks
    // -----------------------------------------------------------------------

    @Nested
    class Benchmarks {

        @Test
        void nonParallelBenchmarksIndecomposable() {
            // Mirror: at least one small non-parallel benchmark should be indecomposable.
            int count = 0;
            String[] types = {
                    "&{a: end}",
                    "&{a: &{b: end}}",
                    "+{ok: end, err: end}",
            };
            for (String t : types) {
                StateSpace ss = ssOf(t);
                LatticeResult lr = LatticeChecker.checkLattice(ss);
                if (!lr.isLattice() || ss.states().size() > 5) continue;
                if (!FactorizationChecker.isDirectlyDecomposable(ss, lr)) count++;
            }
            assertTrue(count >= 1);
        }

        @Test
        void smallParallelBenchmark() {
            var b = build("(&{a: end} || &{b: end})");
            assertTrue(b.lr().isLattice());
            FactorizationAnalysis r = FactorizationChecker.analyzeFactorization(b.ss(), b.lr());
            assertEquals(2, r.factorCount());
        }

        @Test
        void javaIteratorIndecomposable() {
            var b = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            assertTrue(b.lr().isLattice());
            assertFalse(FactorizationChecker.isDirectlyDecomposable(b.ss(), b.lr()));
        }

        @Test
        void fileObjectIndecomposable() {
            var b = build("&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}");
            assertTrue(b.lr().isLattice());
            assertFalse(FactorizationChecker.isDirectlyDecomposable(b.ss(), b.lr()));
        }

        @Test
        void httpConnectionIndecomposable() {
            var b = build("&{connect: rec X . &{request: +{OK200: &{readBody: X}, "
                    + "ERR4xx: X, ERR5xx: X}, close: end}}");
            assertTrue(b.lr().isLattice());
            assertFalse(FactorizationChecker.isDirectlyDecomposable(b.ss(), b.lr()));
        }

        @Test
        void a2aIndecomposable() {
            // A2A from BENCHMARKS — small, should be indecomposable when ≤ 6 states.
            var b = build("&{discover: &{negotiate: +{accept: &{exchange: end}, reject: end}}}");
            if (b.lr().isLattice() && b.ss().states().size() <= 6) {
                assertFalse(FactorizationChecker.isDirectlyDecomposable(b.ss(), b.lr()));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Unique factorization
    // -----------------------------------------------------------------------

    @Nested
    class UniqueFactorization {

        @Test
        void productSizeEqualsFactorProduct() {
            StateSpace ss = ssOf("(&{a: end} || &{b: end})");
            var factors = FactorizationChecker.factorize(ss);
            int product = 1;
            for (var f : factors) product *= f.states().size();
            assertEquals(ss.states().size(), product);
        }

        @Test
        void asymmetricProductSize() {
            StateSpace ss = ssOf("(&{a: end} || &{b: &{c: end}})");
            var factors = FactorizationChecker.factorize(ss);
            int product = 1;
            for (var f : factors) product *= f.states().size();
            assertEquals(ss.states().size(), product);
        }

        @Test
        void factorAreLattices() {
            var factors = FactorizationChecker.factorize(ssOf("(&{a: end} || &{b: end})"));
            for (var f : factors) {
                assertTrue(LatticeChecker.checkLattice(f).isLattice());
            }
        }

        @Test
        void factorsAreIndecomposable() {
            var factors = FactorizationChecker.factorize(ssOf("(&{a: end} || &{b: end})"));
            for (var f : factors) {
                assertFalse(FactorizationChecker.isDirectlyDecomposable(f));
            }
        }

        @Test
        void factorizeIdempotent() {
            StateSpace ss = ssOf("&{a: &{b: end}}");
            var factors = FactorizationChecker.factorize(ss);
            assertEquals(1, factors.size());
            var factors2 = FactorizationChecker.factorize(factors.get(0));
            assertEquals(1, factors2.size());
        }
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Nested
    class EdgeCases {

        @Test
        void singleStateEnd() {
            FactorizationAnalysis r = FactorizationChecker.analyzeFactorization(ssOf("end"));
            assertEquals(1, r.factorCount());
            assertFalse(r.isDecomposable());
        }

        @Test
        void deeplyNestedBranch() {
            assertFalse(FactorizationChecker.isDirectlyDecomposable(
                    ssOf("&{a: &{b: &{c: &{d: end}}}}")));
        }

        @Test
        void selectionChain() {
            assertFalse(FactorizationChecker.isDirectlyDecomposable(ssOf("+{a: +{b: end}}")));
        }

        @Test
        void findFactorCongruencesNonLattice() {
            StateSpace ss = new StateSpace(Set.of(0), List.of(), 0, 0, Map.of());
            var pairs = FactorizationChecker.findFactorCongruences(ss);
            assertTrue(pairs.isEmpty());
        }
    }
}
