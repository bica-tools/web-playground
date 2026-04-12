package com.bica.reborn.distributive_quotient;

import com.bica.reborn.lattice.DistributivityChecker;
import com.bica.reborn.lattice.DistributivityResult;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Tests for distributive quotient computation (Step 301).
 *
 * <p>Java port of Python {@code reticulate/tests/test_distributive_quotient.py}.
 * 52+ tests across 8 test classes.
 */
class DistributiveQuotientCheckerTest {

    private static StateSpace build(String typeStr) {
        return StateSpaceBuilder.build(Parser.parse(typeStr));
    }

    // =========================================================================
    // Test 1: Direct distributivity check
    // =========================================================================

    @Nested
    class DirectDistributivityCheck {

        @Test
        void simpleChain() {
            StateSpace ss = build("&{a: end}");
            assertTrue(DistributiveQuotientChecker.directDistributivityCheck(ss));
        }

        @Test
        void twoBranch() {
            StateSpace ss = build("&{a: end, b: end}");
            assertTrue(DistributiveQuotientChecker.directDistributivityCheck(ss));
        }

        @Test
        void nestedBranch() {
            StateSpace ss = build("&{a: &{b: end}, c: end}");
            assertTrue(DistributiveQuotientChecker.directDistributivityCheck(ss));
        }

        @Test
        void productAlwaysDistributive() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            assertTrue(DistributiveQuotientChecker.directDistributivityCheck(ss));
        }

        @Test
        void iteratorDistributive() {
            StateSpace ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            assertTrue(DistributiveQuotientChecker.directDistributivityCheck(ss));
        }

        @Test
        void endType() {
            StateSpace ss = build("end");
            assertTrue(DistributiveQuotientChecker.directDistributivityCheck(ss));
        }

        @Test
        void selectOnly() {
            StateSpace ss = build("+{a: end, b: end}");
            assertTrue(DistributiveQuotientChecker.directDistributivityCheck(ss));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "&{a: end}",
                "&{a: end, b: end}",
                "+{a: end, b: end}",
                "(&{a: end} || &{b: end})",
                "&{a: &{b: end}, c: end}",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                "end",
                "&{a: &{b: &{c: end}}}",
                "((&{a: end} || &{b: end}) || &{c: end})"
        })
        void agreesWithCheckDistributive(String typeStr) {
            StateSpace ss = build(typeStr);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            if (!lr.isLattice()) {
                assertFalse(DistributiveQuotientChecker.directDistributivityCheck(ss));
                return;
            }
            DistributivityResult dr = DistributivityChecker.checkDistributive(ss);
            boolean direct = DistributiveQuotientChecker.directDistributivityCheck(ss);
            assertEquals(dr.isDistributive(), direct,
                    typeStr + ": direct=" + direct + ", checkDistributive=" + dr.isDistributive());
        }
    }

    // =========================================================================
    // Test 2: Is congruence
    // =========================================================================

    @Nested
    class IsCongruenceTests {

        @Test
        void identityIsCongruence() {
            StateSpace ss = build("&{a: end, b: end}");
            List<Set<Integer>> partition = new ArrayList<>();
            for (int s : ss.states()) partition.add(Set.of(s));
            assertTrue(DistributiveQuotientChecker.isCongruence(ss, partition));
        }

        @Test
        void allOneClassIsCongruence() {
            StateSpace ss = build("&{a: end, b: end}");
            List<Set<Integer>> partition = List.of(new HashSet<>(ss.states()));
            assertTrue(DistributiveQuotientChecker.isCongruence(ss, partition));
        }

        @Test
        void validCongruenceChain() {
            StateSpace ss = build("&{a: &{b: end}}");
            List<Set<Integer>> partition = new ArrayList<>();
            for (int s : ss.states()) partition.add(Set.of(s));
            assertTrue(DistributiveQuotientChecker.isCongruence(ss, partition));
        }

        @Test
        void invalidPartitionMissingStates() {
            StateSpace ss = build("&{a: end, b: end}");
            List<Integer> someStates = new ArrayList<>(ss.states());
            List<Set<Integer>> partition = List.of(Set.of(someStates.get(0)));
            assertFalse(DistributiveQuotientChecker.isCongruence(ss, partition));
        }

        @Test
        void congruenceOnProduct() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            List<Set<Integer>> partition = new ArrayList<>();
            for (int s : ss.states()) partition.add(Set.of(s));
            assertTrue(DistributiveQuotientChecker.isCongruence(ss, partition));
        }

        @Test
        void congruenceOnIterator() {
            StateSpace ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            List<Set<Integer>> partition = new ArrayList<>();
            for (int s : ss.states()) partition.add(Set.of(s));
            assertTrue(DistributiveQuotientChecker.isCongruence(ss, partition));
        }

        @Test
        void invalidRandomMergeRunsWithoutError() {
            StateSpace ss = build("&{a: &{b: end}, c: end}");
            List<Integer> statesList = new ArrayList<>(ss.states());
            Collections.sort(statesList);
            if (statesList.size() >= 3) {
                Set<Integer> merged = new HashSet<>(Set.of(statesList.get(0), statesList.get(1)));
                List<Set<Integer>> partition = new ArrayList<>();
                partition.add(merged);
                for (int i = 2; i < statesList.size(); i++) {
                    partition.add(Set.of(statesList.get(i)));
                }
                // Just verify it runs without error
                boolean result = DistributiveQuotientChecker.isCongruence(ss, partition);
                // result could be true or false depending on structure
                assertNotNull(result);
            }
        }
    }

    // =========================================================================
    // Test 3: Build quotient
    // =========================================================================

    @Nested
    class BuildQuotientTests {

        @Test
        void identityPartitionPreservesStates() {
            StateSpace ss = build("&{a: end, b: end}");
            List<Set<Integer>> partition = new ArrayList<>();
            for (int s : ss.states()) partition.add(Set.of(s));
            StateSpace quotient = DistributiveQuotientChecker.buildQuotientStateSpace(ss, partition);
            assertEquals(ss.states().size(), quotient.states().size());
        }

        @Test
        void allMergeGivesOneState() {
            StateSpace ss = build("&{a: end, b: end}");
            List<Set<Integer>> partition = List.of(new HashSet<>(ss.states()));
            StateSpace quotient = DistributiveQuotientChecker.buildQuotientStateSpace(ss, partition);
            assertEquals(1, quotient.states().size());
        }

        @Test
        void quotientHasTopAndBottom() {
            StateSpace ss = build("&{a: end, b: end}");
            List<Set<Integer>> partition = new ArrayList<>();
            for (int s : ss.states()) partition.add(Set.of(s));
            StateSpace quotient = DistributiveQuotientChecker.buildQuotientStateSpace(ss, partition);
            assertTrue(quotient.states().contains(quotient.top()));
            assertTrue(quotient.states().contains(quotient.bottom()));
        }

        @Test
        void quotientTransitionsSubset() {
            StateSpace ss = build("&{a: &{b: end}, c: end}");
            List<Set<Integer>> partition = new ArrayList<>();
            for (int s : ss.states()) partition.add(Set.of(s));
            StateSpace quotient = DistributiveQuotientChecker.buildQuotientStateSpace(ss, partition);
            for (var t : quotient.transitions()) {
                assertTrue(quotient.states().contains(t.source()));
                assertTrue(quotient.states().contains(t.target()));
            }
        }

        @Test
        void quotientDeduplicatesTransitions() {
            StateSpace ss = build("&{a: end, b: end}");
            List<Set<Integer>> partition = List.of(new HashSet<>(ss.states()));
            StateSpace quotient = DistributiveQuotientChecker.buildQuotientStateSpace(ss, partition);
            Set<String> seen = new HashSet<>();
            for (var t : quotient.transitions()) {
                String key = t.source() + ":" + t.label() + ":" + t.target();
                assertTrue(seen.add(key), "Duplicate transition: " + key);
            }
        }

        @Test
        void quotientPreservesSelections() {
            StateSpace ss = build("+{a: end, b: end}");
            List<Set<Integer>> partition = new ArrayList<>();
            for (int s : ss.states()) partition.add(Set.of(s));
            StateSpace quotient = DistributiveQuotientChecker.buildQuotientStateSpace(ss, partition);
            // Selection transitions should be preserved
            long origSelections = ss.transitions().stream()
                    .filter(t -> t.kind() == com.bica.reborn.statespace.TransitionKind.SELECTION)
                    .count();
            long quotSelections = quotient.transitions().stream()
                    .filter(t -> t.kind() == com.bica.reborn.statespace.TransitionKind.SELECTION)
                    .count();
            assertEquals(origSelections, quotSelections);
        }

        @Test
        void quotientLabelsForMerged() {
            StateSpace ss = build("&{a: end, b: end}");
            List<Integer> statesList = new ArrayList<>(ss.states());
            Collections.sort(statesList);
            List<Integer> nonBottom = new ArrayList<>();
            for (int s : statesList) {
                if (s != ss.bottom()) nonBottom.add(s);
            }
            if (nonBottom.size() >= 2) {
                Set<Integer> merged = new HashSet<>(nonBottom);
                List<Set<Integer>> partition = new ArrayList<>();
                partition.add(merged);
                partition.add(Set.of(ss.bottom()));
                StateSpace quotient = DistributiveQuotientChecker.buildQuotientStateSpace(ss, partition);
                int rep = Collections.min(nonBottom);
                assertTrue(quotient.labels().getOrDefault(rep, "").contains("{"));
            }
        }
    }

    // =========================================================================
    // Test 4: Distributive quotient main algorithm
    // =========================================================================

    @Nested
    class DistributiveQuotientMain {

        @Test
        void alreadyDistributiveChain() {
            StateSpace ss = build("&{a: end}");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertTrue(result.isAlreadyDistributive());
            assertTrue(result.isQuotientPossible());
            assertTrue(result.mergedPairs().isEmpty());
            assertNotNull(result.quotientStateSpace());
        }

        @Test
        void alreadyDistributiveProduct() {
            StateSpace ss = build("(&{a: end} || &{b: &{c: end}})");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertTrue(result.isAlreadyDistributive());
            assertEquals(0.0, result.entropyLost(), 1e-10);
        }

        @Test
        void alreadyDistributiveB3() {
            StateSpace ss = build("((&{a: end} || &{b: end}) || &{c: end})");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertTrue(result.isAlreadyDistributive());
        }

        @Test
        void alreadyDistributiveIterator() {
            StateSpace ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertTrue(result.isAlreadyDistributive());
            assertEquals(1, result.numMinimalQuotients());
        }

        @Test
        void m3DiamondImpossible() {
            StateSpace ss = build("&{a: +{OK: end, E: end}, b: +{OK: end, E: end}, c: +{OK: end, E: end}}");
            DistributivityResult dr = DistributivityChecker.checkDistributive(ss);
            assumeTrue(dr.isLattice(), "Not a lattice");
            assumeFalse(dr.isDistributive(), "Already distributive");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertFalse(result.isQuotientPossible());
            assertEquals(0, result.numMinimalQuotients());
        }

        @Test
        void n5FromBranchNesting() {
            StateSpace ss = build("&{a: &{b: +{x: end, y: end}}, c: +{x: end, y: end}}");
            DistributivityResult dr = DistributivityChecker.checkDistributive(ss);
            assumeTrue(dr.isLattice(), "Not a lattice");
            assumeFalse(dr.isDistributive(), "Already distributive");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertTrue(result.isQuotientPossible());
            assertNotNull(result.quotientStateSpace());
            assertTrue(result.quotientStates() < result.originalStates());
            assertTrue(result.entropyLost() > 0.0);
            // Verify quotient is actually distributive
            assertTrue(DistributiveQuotientChecker.directDistributivityCheck(
                    result.quotientStateSpace()));
        }

        @Test
        void resultHasCongruenceClasses() {
            StateSpace ss = build("&{a: &{b: +{x: end, y: end}}, c: +{x: end, y: end}}");
            DistributivityResult dr = DistributivityChecker.checkDistributive(ss);
            assumeTrue(dr.isLattice(), "Not a lattice");
            assumeFalse(dr.isDistributive(), "Already distributive");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            if (result.isQuotientPossible()) {
                assertFalse(result.congruenceClasses().isEmpty());
            }
        }

        @Test
        void endTypeDistributive() {
            StateSpace ss = build("end");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertTrue(result.isAlreadyDistributive());
        }

        @Test
        void selectTypeDistributive() {
            StateSpace ss = build("+{a: end, b: end}");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertTrue(result.isAlreadyDistributive());
        }

        @Test
        void deepChainDistributive() {
            StateSpace ss = build("&{a: &{b: &{c: end}}}");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertTrue(result.isAlreadyDistributive());
        }

        @Test
        void originalStatesCount() {
            StateSpace ss = build("&{a: end, b: end}");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertEquals(ss.states().size(), result.originalStates());
        }

        @Test
        void quotientStatesLeqOriginal() {
            StateSpace ss = build("&{a: &{b: +{x: end, y: end}}, c: +{x: end, y: end}}");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertTrue(result.quotientStates() <= result.originalStates());
        }
    }

    // =========================================================================
    // Test 5: Entropy calculation
    // =========================================================================

    @Nested
    class EntropyCalculation {

        @Test
        void entropyBeforePositive() {
            StateSpace ss = build("&{a: end, b: end}");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertTrue(result.entropyBefore() > 0.0);
        }

        @Test
        void entropySingleState() {
            StateSpace ss = build("end");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertEquals(0.0, result.entropyBefore(), 1e-10);
        }

        @Test
        void entropyLostZeroWhenDistributive() {
            StateSpace ss = build("&{a: end, b: end}");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertEquals(0.0, result.entropyLost(), 1e-10);
        }

        @Test
        void entropyLostPositiveWhenMerged() {
            StateSpace ss = build("&{a: &{b: +{x: end, y: end}}, c: +{x: end, y: end}}");
            DistributivityResult dr = DistributivityChecker.checkDistributive(ss);
            assumeTrue(dr.isLattice(), "Not a lattice");
            assumeFalse(dr.isDistributive(), "Already distributive");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            if (result.isQuotientPossible()) {
                assertTrue(result.entropyLost() > 0.0);
            }
        }

        @Test
        void entropyConsistency() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertEquals(result.entropyLost(),
                    result.entropyBefore() - result.entropyAfter(), 1e-10);
        }

        @Test
        void entropyTwoStates() {
            StateSpace ss = build("&{a: end}");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            double expected = Math.log(ss.states().size()) / Math.log(2);
            assertEquals(expected, result.entropyBefore(), 1e-10);
        }

        @Test
        void entropyFourStates() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            double expected = Math.log(ss.states().size()) / Math.log(2);
            assertEquals(expected, result.entropyBefore(), 1e-10);
        }

        @Test
        void entropyAfterLeqBefore() {
            StateSpace ss = build("&{a: &{b: +{x: end, y: end}}, c: +{x: end, y: end}}");
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertTrue(result.entropyAfter() <= result.entropyBefore() + 1e-10);
        }
    }

    // =========================================================================
    // Test 6: Count minimal quotients
    // =========================================================================

    @Nested
    class CountMinimalQuotients {

        @Test
        void alreadyDistributiveReturns1() {
            StateSpace ss = build("&{a: end}");
            assertEquals(1, DistributiveQuotientChecker.countMinimalQuotients(ss));
        }

        @Test
        void productReturns1() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            assertEquals(1, DistributiveQuotientChecker.countMinimalQuotients(ss));
        }

        @Test
        void m3DiamondReturns0() {
            StateSpace ss = build("&{a: +{OK: end, E: end}, b: +{OK: end, E: end}, c: +{OK: end, E: end}}");
            DistributivityResult dr = DistributivityChecker.checkDistributive(ss);
            assumeTrue(dr.isLattice(), "Not a lattice");
            assumeFalse(dr.isDistributive(), "Already distributive");
            assertEquals(0, DistributiveQuotientChecker.countMinimalQuotients(ss));
        }

        @Test
        void n5HasQuotient() {
            StateSpace ss = build("&{a: &{b: +{x: end, y: end}}, c: +{x: end, y: end}}");
            DistributivityResult dr = DistributivityChecker.checkDistributive(ss);
            assumeTrue(dr.isLattice(), "Not a lattice");
            assumeFalse(dr.isDistributive(), "Already distributive");
            int count = DistributiveQuotientChecker.countMinimalQuotients(ss);
            assertTrue(count >= 1);
        }

        @Test
        void iteratorReturns1() {
            StateSpace ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            assertEquals(1, DistributiveQuotientChecker.countMinimalQuotients(ss));
        }

        @Test
        void chainReturns1() {
            StateSpace ss = build("&{a: &{b: &{c: end}}}");
            assertEquals(1, DistributiveQuotientChecker.countMinimalQuotients(ss));
        }
    }

    // =========================================================================
    // Test 7: Benchmark protocols (parametrized)
    // =========================================================================

    @Nested
    class BenchmarkProtocols {

        @ParameterizedTest
        @ValueSource(strings = {
                "&{a: end}",
                "&{a: end, b: end}",
                "+{a: end, b: end}",
                "(&{a: end} || &{b: end})",
                "&{a: &{b: end}, c: end}",
                "&{a: &{b: &{c: end}}}",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                "end",
                "((&{a: end} || &{b: end}) || &{c: end})",
                "(&{a: end} || &{b: &{c: end}})"
        })
        void quotientRunsOnBenchmark(String typeStr) {
            StateSpace ss = build(typeStr);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assumeTrue(lr.isLattice(), "Not a lattice: " + typeStr);
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertNotNull(result);
            assertEquals(ss.states().size(), result.originalStates());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "&{a: end}",
                "&{a: end, b: end}",
                "+{a: end, b: end}",
                "(&{a: end} || &{b: end})",
                "&{a: &{b: end}, c: end}",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                "end"
        })
        void entropyNonNegativeOnBenchmark(String typeStr) {
            StateSpace ss = build(typeStr);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assumeTrue(lr.isLattice(), "Not a lattice: " + typeStr);
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertTrue(result.entropyLost() >= -1e-10);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "&{a: end}",
                "&{a: end, b: end}",
                "+{a: end, b: end}",
                "(&{a: end} || &{b: end})",
                "&{a: &{b: end}, c: end}",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                "end",
                "&{a: &{b: &{c: end}}}",
                "((&{a: end} || &{b: end}) || &{c: end})"
        })
        void distributiveBenchmarksNeedNoQuotient(String typeStr) {
            StateSpace ss = build(typeStr);
            DistributivityResult dr = DistributivityChecker.checkDistributive(ss);
            assumeTrue(dr.isDistributive(), "Not distributive: " + typeStr);
            DistributiveQuotientResult result =
                    DistributiveQuotientChecker.computeDistributiveQuotient(ss);
            assertTrue(result.isAlreadyDistributive());
        }
    }

    // =========================================================================
    // Test 8: Regression — direct check matches Birkhoff
    // =========================================================================

    @Nested
    class BirkhoffRegression {

        @ParameterizedTest
        @ValueSource(strings = {
                "&{a: end}",
                "&{a: end, b: end}",
                "+{a: end, b: end}",
                "(&{a: end} || &{b: end})",
                "&{a: &{b: end}, c: end}",
                "&{a: &{b: &{c: end}}}",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                "end",
                "((&{a: end} || &{b: end}) || &{c: end})",
                "(&{a: end} || &{b: &{c: end}})",
                "&{a: +{OK: end, E: end}, b: +{OK: end, E: end}, c: +{OK: end, E: end}}",
                "&{a: &{b: +{x: end, y: end}}, c: +{x: end, y: end}}"
        })
        void directMatchesBirkhoff(String typeStr) {
            StateSpace ss = build(typeStr);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            if (!lr.isLattice()) {
                assertFalse(DistributiveQuotientChecker.directDistributivityCheck(ss));
                return;
            }
            DistributivityResult dr = DistributivityChecker.checkDistributive(ss);
            boolean direct = DistributiveQuotientChecker.directDistributivityCheck(ss);
            assertEquals(dr.isDistributive(), direct,
                    typeStr + ": direct=" + direct + ", Birkhoff=" + dr.isDistributive());
        }
    }
}
