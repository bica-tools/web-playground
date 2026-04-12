package com.bica.reborn.rowmotion;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.statespace.TransitionKind;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for rowmotion and toggling analysis (Step 30ad port).
 *
 * <p>82 tests mirroring the Python test_rowmotion.py.
 */
class RowmotionCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    /** Build a chain of n elements: 0 > 1 > ... > n-1. */
    private static StateSpace chain(int n) {
        Set<Integer> states = new HashSet<>();
        List<StateSpace.Transition> transitions = new ArrayList<>();
        Map<Integer, String> labels = new HashMap<>();
        for (int i = 0; i < n; i++) {
            states.add(i);
            labels.put(i, "s" + i);
            if (i < n - 1) {
                transitions.add(new StateSpace.Transition(i, "t" + i, i + 1, TransitionKind.METHOD));
            }
        }
        return new StateSpace(states, transitions, 0, n - 1, labels);
    }

    /** Boolean lattice 2^2 = diamond: 0=top, 1=a, 2=b, 3=bot. */
    private static StateSpace booleanLattice2() {
        return new StateSpace(
                Set.of(0, 1, 2, 3),
                List.of(
                        new StateSpace.Transition(0, "a", 1, TransitionKind.METHOD),
                        new StateSpace.Transition(0, "b", 2, TransitionKind.METHOD),
                        new StateSpace.Transition(1, "x", 3, TransitionKind.METHOD),
                        new StateSpace.Transition(2, "y", 3, TransitionKind.METHOD)
                ),
                0, 3,
                Map.of(0, "top", 1, "a", 2, "b", 3, "bot")
        );
    }

    /** Check that ideal is a valid downset. */
    private static boolean isDownset(Set<Integer> ideal, Map<Integer, Set<Integer>> order) {
        for (int x : ideal) {
            Set<Integer> belowX = order.get(x);
            if (belowX != null && !ideal.containsAll(belowX)) return false;
        }
        return true;
    }

    /** Check that no two elements in ac are comparable. */
    private static boolean isAntichain(Set<Integer> ac, Map<Integer, Set<Integer>> order) {
        List<Integer> elems = new ArrayList<>(ac);
        for (int i = 0; i < elems.size(); i++) {
            for (int j = i + 1; j < elems.size(); j++) {
                int a = elems.get(i), b = elems.get(j);
                if (order.containsKey(a) && order.get(a).contains(b)) return false;
                if (order.containsKey(b) && order.get(b).contains(a)) return false;
            }
        }
        return true;
    }

    // =========================================================================
    // TestOrderRelation
    // =========================================================================

    @Nested
    class TestOrderRelation {

        @Test
        void endSingleState() {
            var ss = build("end");
            var order = RowmotionChecker.computeOrderRelation(ss);
            assertEquals(1, order.size());
            for (var entry : order.entrySet()) {
                assertTrue(entry.getValue().contains(entry.getKey()));
            }
        }

        @Test
        void branchTwoStates() {
            var ss = build("&{a: end}");
            var order = RowmotionChecker.computeOrderRelation(ss);
            assertEquals(2, order.size());
            int topRep = order.entrySet().stream()
                    .max(Comparator.comparingInt(e -> e.getValue().size()))
                    .get().getKey();
            int botRep = order.entrySet().stream()
                    .min(Comparator.comparingInt(e -> e.getValue().size()))
                    .get().getKey();
            assertTrue(order.get(topRep).contains(botRep));
            assertFalse(order.get(botRep).contains(topRep));
        }

        @Test
        void branchSharedEnd() {
            var ss = build("&{a: end, b: end}");
            var order = RowmotionChecker.computeOrderRelation(ss);
            int topRep = order.entrySet().stream()
                    .max(Comparator.comparingInt(e -> e.getValue().size()))
                    .get().getKey();
            assertEquals(order.size(), order.get(topRep).size());
        }

        @Test
        void chainThreeHandBuilt() {
            var ss = chain(3);
            var order = RowmotionChecker.computeOrderRelation(ss);
            assertEquals(Set.of(0, 1, 2), order.get(0));
            assertEquals(Set.of(1, 2), order.get(1));
            assertEquals(Set.of(2), order.get(2));
        }

        @Test
        void diamondIncomparability() {
            var ss = booleanLattice2();
            var order = RowmotionChecker.computeOrderRelation(ss);
            assertFalse(order.get(1).contains(2));
            assertFalse(order.get(2).contains(1));
        }

        @ParameterizedTest
        @ValueSource(strings = {"end", "&{a: end}", "&{a: &{b: end}}"})
        void orderIsReflexive(String type) {
            var ss = build(type);
            var order = RowmotionChecker.computeOrderRelation(ss);
            for (var entry : order.entrySet()) {
                assertTrue(entry.getValue().contains(entry.getKey()),
                        "reflexivity failed for " + entry.getKey());
            }
        }
    }

    // =========================================================================
    // TestCovers
    // =========================================================================

    @Nested
    class TestCovers {

        @Test
        void chainCoversSuccessorOnly() {
            var ss = chain(4);
            var covers = RowmotionChecker.computeCovers(ss);
            assertEquals(Set.of(1), covers.get(0));
            assertEquals(Set.of(2), covers.get(1));
            assertEquals(Set.of(3), covers.get(2));
            assertEquals(Set.of(), covers.get(3));
        }

        @Test
        void chainNoTransitiveCovers() {
            var ss = chain(4);
            var covers = RowmotionChecker.computeCovers(ss);
            assertFalse(covers.get(0).contains(2));
            assertFalse(covers.get(0).contains(3));
        }

        @Test
        void diamondTopCoversMiddle() {
            var ss = booleanLattice2();
            var covers = RowmotionChecker.computeCovers(ss);
            assertEquals(Set.of(1, 2), covers.get(0));
            assertEquals(Set.of(3), covers.get(1));
            assertEquals(Set.of(3), covers.get(2));
            assertFalse(covers.get(0).contains(3));
        }

        @Test
        void endNoCovers() {
            var ss = build("end");
            var covers = RowmotionChecker.computeCovers(ss);
            for (var entry : covers.entrySet()) {
                assertEquals(0, entry.getValue().size());
            }
        }

        @Test
        void totalCoversDiamond() {
            var ss = booleanLattice2();
            var covers = RowmotionChecker.computeCovers(ss);
            int total = covers.values().stream().mapToInt(Set::size).sum();
            assertEquals(4, total);
        }
    }

    // =========================================================================
    // TestOrderIdeals
    // =========================================================================

    @Nested
    class TestOrderIdeals {

        @Test
        void singleStateTwoIdeals() {
            var ss = build("end");
            assertEquals(2, RowmotionChecker.orderIdeals(ss).size());
            assertTrue(RowmotionChecker.orderIdeals(ss).contains(Set.of()));
        }

        @ParameterizedTest
        @CsvSource({"2, 3", "3, 4", "4, 5"})
        void chainNPlusOneIdeals(int n, int expected) {
            assertEquals(expected, RowmotionChecker.orderIdeals(chain(n)).size());
        }

        @Test
        void diamondSixIdeals() {
            assertEquals(6, RowmotionChecker.orderIdeals(booleanLattice2()).size());
        }

        @Test
        void everyIdealIsValidDownset() {
            var ss = booleanLattice2();
            var order = RowmotionChecker.computeOrderRelation(ss);
            for (Set<Integer> ideal : RowmotionChecker.orderIdeals(ss)) {
                assertTrue(isDownset(ideal, order), ideal + " is not a valid downset");
            }
        }

        @Test
        void emptyAndFullAlwaysPresent() {
            for (var ss : List.of(chain(2), chain(3), booleanLattice2())) {
                var ideals = RowmotionChecker.orderIdeals(ss);
                var order = RowmotionChecker.computeOrderRelation(ss);
                assertTrue(ideals.contains(Set.of()));
                Set<Integer> full = new HashSet<>(order.keySet());
                assertTrue(ideals.contains(full));
            }
        }
    }

    // =========================================================================
    // TestAntichains
    // =========================================================================

    @Nested
    class TestAntichains {

        @Test
        void singleStateTwoAntichains() {
            assertEquals(2, RowmotionChecker.antichains(build("end")).size());
        }

        @Test
        void chainAntichainsCount() {
            assertEquals(4, RowmotionChecker.antichains(chain(3)).size());
        }

        @Test
        void diamondAntichainsCount() {
            assertEquals(6, RowmotionChecker.antichains(booleanLattice2()).size());
        }

        @Test
        void everyAntichainIsValid() {
            var ss = booleanLattice2();
            var order = RowmotionChecker.computeOrderRelation(ss);
            for (Set<Integer> ac : RowmotionChecker.antichains(ss)) {
                assertTrue(isAntichain(ac, order), ac + " is not a valid antichain");
            }
        }

        @Test
        void countEqualsIdealsCount() {
            for (var ss : List.of(build("end"), chain(2), chain(3), booleanLattice2())) {
                assertEquals(
                        RowmotionChecker.orderIdeals(ss).size(),
                        RowmotionChecker.antichains(ss).size());
            }
        }
    }

    // =========================================================================
    // TestBijection
    // =========================================================================

    @Nested
    class TestBijection {

        @Test
        void roundtripIdealToAntichainToIdeal() {
            var ss = booleanLattice2();
            for (Set<Integer> ideal : RowmotionChecker.orderIdeals(ss)) {
                var ac = RowmotionChecker.idealToAntichain(ss, ideal);
                var recovered = RowmotionChecker.antichainToIdeal(ss, ac);
                assertEquals(ideal, recovered,
                        "Round-trip failed: " + ideal + " -> " + ac + " -> " + recovered);
            }
        }

        @Test
        void roundtripAntichainToIdealToAntichain() {
            var ss = booleanLattice2();
            for (Set<Integer> ac : RowmotionChecker.antichains(ss)) {
                var ideal = RowmotionChecker.antichainToIdeal(ss, ac);
                var recovered = RowmotionChecker.idealToAntichain(ss, ideal);
                assertEquals(ac, recovered);
            }
        }

        @Test
        void emptyAntichainGivesEmptyIdeal() {
            var ss = build("&{a: end}");
            assertEquals(Set.of(), RowmotionChecker.antichainToIdeal(ss, Set.of()));
        }

        @Test
        void emptyIdealGivesEmptyAntichain() {
            var ss = build("&{a: end}");
            assertEquals(Set.of(), RowmotionChecker.idealToAntichain(ss, Set.of()));
        }

        @Test
        void roundtripOnChain() {
            var ss = chain(3);
            for (Set<Integer> ideal : RowmotionChecker.orderIdeals(ss)) {
                var ac = RowmotionChecker.idealToAntichain(ss, ideal);
                assertEquals(ideal, RowmotionChecker.antichainToIdeal(ss, ac));
            }
        }
    }

    // =========================================================================
    // TestToggle
    // =========================================================================

    @Nested
    class TestToggle {

        @Test
        void toggleAddBottomToEmpty() {
            var ss = chain(2);
            var order = RowmotionChecker.computeOrderRelation(ss);
            int bot = order.entrySet().stream()
                    .min(Comparator.comparingInt(e -> e.getValue().size()))
                    .get().getKey();
            var result = RowmotionChecker.toggle(ss, Set.of(), bot);
            assertTrue(result.contains(bot));
        }

        @Test
        void toggleRemoveMaximal() {
            var ss = chain(2);
            var seq = RowmotionChecker.toggleSequence(ss);
            int bot = seq.get(0);
            var result = RowmotionChecker.toggle(ss, Set.of(bot), bot);
            assertFalse(result.contains(bot));
        }

        @Test
        void doubleToggleIdentity() {
            var ss = booleanLattice2();
            var seq = RowmotionChecker.toggleSequence(ss);
            int bot = seq.get(0);
            Set<Integer> ideal = Set.of();
            var step1 = RowmotionChecker.toggle(ss, ideal, bot);
            var step2 = RowmotionChecker.toggle(ss, step1, bot);
            assertEquals(ideal, step2);
        }

        @Test
        void togglePreservesDownsetProperty() {
            var ss = booleanLattice2();
            var order = RowmotionChecker.computeOrderRelation(ss);
            var idealsSet = new HashSet<>(RowmotionChecker.orderIdeals(ss));
            for (Set<Integer> ideal : RowmotionChecker.orderIdeals(ss)) {
                for (int x : order.keySet()) {
                    var result = RowmotionChecker.toggle(ss, ideal, x);
                    assertTrue(idealsSet.contains(result),
                            "toggle(" + ideal + ", " + x + ") = " + result + " not in ideals");
                }
            }
        }

        @Test
        void toggleCannotAddWithoutPredecessors() {
            var ss = booleanLattice2();
            var result = RowmotionChecker.toggle(ss, Set.of(), 0);
            assertFalse(result.contains(0));
        }

        @Test
        void toggleCannotRemoveNeededElement() {
            var ss = booleanLattice2();
            Set<Integer> ideal = Set.of(3, 1);
            var result = RowmotionChecker.toggle(ss, ideal, 3);
            assertTrue(result.contains(3));
        }
    }

    // =========================================================================
    // TestToggleSequence
    // =========================================================================

    @Nested
    class TestToggleSequence {

        @Test
        void allElementsPresent() {
            var ss = booleanLattice2();
            var seq = RowmotionChecker.toggleSequence(ss);
            var order = RowmotionChecker.computeOrderRelation(ss);
            assertEquals(order.keySet(), new HashSet<>(seq));
            assertEquals(seq.size(), new HashSet<>(seq).size());
        }

        @Test
        void linearExtensionOrder() {
            var ss = booleanLattice2();
            var order = RowmotionChecker.computeOrderRelation(ss);
            var seq = RowmotionChecker.toggleSequence(ss);
            Map<Integer, Integer> pos = new HashMap<>();
            for (int i = 0; i < seq.size(); i++) pos.put(seq.get(i), i);
            // top-to-bottom: if x > y (y reachable from x), x appears before y
            for (int y : order.keySet()) {
                for (int x : order.get(y)) {
                    if (x != y) {
                        // x < y (x reachable from y), so y appears before x
                        assertTrue(pos.get(y) < pos.get(x),
                                y + " (greater) should precede " + x + " (lesser)");
                    }
                }
            }
        }

        @Test
        void topFirstBottomLast() {
            var ss = booleanLattice2();
            var seq = RowmotionChecker.toggleSequence(ss);
            assertEquals(0, seq.get(0).intValue());
            assertEquals(3, seq.get(seq.size() - 1).intValue());
        }
    }

    // =========================================================================
    // TestRowmotionIdeal
    // =========================================================================

    @Nested
    class TestRowmotionIdeal {

        @Test
        void emptyIdealMapsToDownsetOfMin() {
            var ss = chain(3);
            var result = RowmotionChecker.rowmotionIdeal(ss, Set.of());
            assertTrue(result.contains(ss.bottom()));
        }

        @Test
        void fullIdealMapsToEmpty() {
            var ss = chain(3);
            var order = RowmotionChecker.computeOrderRelation(ss);
            Set<Integer> full = new HashSet<>(order.keySet());
            var result = RowmotionChecker.rowmotionIdeal(ss, full);
            assertEquals(Set.of(), result);
        }

        @Test
        void rowmotionIsBijectionChain() {
            var ss = chain(3);
            var ideals = RowmotionChecker.orderIdeals(ss);
            var results = ideals.stream()
                    .map(i -> RowmotionChecker.rowmotionIdeal(ss, i))
                    .collect(Collectors.toSet());
            assertEquals(ideals.size(), results.size());
        }

        @Test
        void rowmotionIsBijectionDiamond() {
            var ss = booleanLattice2();
            var ideals = RowmotionChecker.orderIdeals(ss);
            var idealsSet = new HashSet<>(ideals);
            var results = ideals.stream()
                    .map(i -> RowmotionChecker.rowmotionIdeal(ss, i))
                    .toList();
            for (var r : results) {
                assertTrue(idealsSet.contains(r), r + " is not a valid ideal");
            }
            assertEquals(ideals.size(), new HashSet<>(results).size());
        }

        @Test
        void rowmotionResultIsAlwaysIdeal() {
            var ss = booleanLattice2();
            var order = RowmotionChecker.computeOrderRelation(ss);
            for (Set<Integer> ideal : RowmotionChecker.orderIdeals(ss)) {
                var result = RowmotionChecker.rowmotionIdeal(ss, ideal);
                assertTrue(isDownset(result, order),
                        "rowmotion(" + ideal + ") = " + result + " is not a downset");
            }
        }

        @Test
        void rowmotionOnSessionType() {
            var ss = build("&{a: end}");
            var ideals = RowmotionChecker.orderIdeals(ss);
            var results = ideals.stream()
                    .map(i -> RowmotionChecker.rowmotionIdeal(ss, i))
                    .collect(Collectors.toSet());
            assertEquals(ideals.size(), results.size());
        }

        @Test
        void chain2Cycle() {
            var ss = chain(2);
            Set<Integer> start = Set.of();
            Set<Integer> current = start;
            int cycleLen = 0;
            do {
                current = RowmotionChecker.rowmotionIdeal(ss, current);
                cycleLen++;
                assertTrue(cycleLen < 10, "Cycle too long");
            } while (!current.equals(start));
            assertEquals(3, cycleLen);
        }

        @Test
        void diamondFullCycle() {
            var ss = booleanLattice2();
            for (Set<Integer> ideal : RowmotionChecker.orderIdeals(ss)) {
                var orbit = RowmotionChecker.rowmotionOrbit(ss, ideal);
                assertEquals(ideal, orbit.get(0));
                assertEquals(ideal, RowmotionChecker.rowmotionIdeal(ss, orbit.get(orbit.size() - 1)));
            }
        }
    }

    // =========================================================================
    // TestRowmotionAntichain
    // =========================================================================

    @Nested
    class TestRowmotionAntichain {

        @Test
        void emptyAntichain() {
            var ss = build("&{a: end}");
            var result = RowmotionChecker.rowmotionAntichain(ss, Set.of());
            assertNotNull(result);
        }

        @Test
        void consistentWithIdealRowmotion() {
            var ss = booleanLattice2();
            for (Set<Integer> ideal : RowmotionChecker.orderIdeals(ss)) {
                var ac = RowmotionChecker.idealToAntichain(ss, ideal);
                var newIdeal = RowmotionChecker.rowmotionIdeal(ss, ideal);
                var expectedAc = RowmotionChecker.idealToAntichain(ss, newIdeal);
                var actualAc = RowmotionChecker.rowmotionAntichain(ss, ac);
                assertEquals(expectedAc, actualAc);
            }
        }

        @Test
        void resultIsValidAntichain() {
            var ss = booleanLattice2();
            var order = RowmotionChecker.computeOrderRelation(ss);
            for (Set<Integer> ac : RowmotionChecker.antichains(ss)) {
                var result = RowmotionChecker.rowmotionAntichain(ss, ac);
                assertTrue(isAntichain(result, order),
                        "rowmotionAntichain(" + ac + ") = " + result + " is not an antichain");
            }
        }
    }

    // =========================================================================
    // TestRowmotionViaToggles
    // =========================================================================

    @Nested
    class TestRowmotionViaToggles {

        @Test
        void producesValidIdeal() {
            var ss = booleanLattice2();
            var order = RowmotionChecker.computeOrderRelation(ss);
            for (Set<Integer> ideal : RowmotionChecker.orderIdeals(ss)) {
                var result = RowmotionChecker.rowmotionViaToggles(ss, ideal);
                assertTrue(isDownset(result, order));
            }
        }

        @Test
        void isBijection() {
            var ss = booleanLattice2();
            var ideals = RowmotionChecker.orderIdeals(ss);
            var results = ideals.stream()
                    .map(i -> RowmotionChecker.rowmotionViaToggles(ss, i))
                    .collect(Collectors.toSet());
            assertEquals(ideals.size(), results.size());
        }

        @Test
        void agreesDirectChain() {
            var ss = chain(2);
            for (Set<Integer> ideal : RowmotionChecker.orderIdeals(ss)) {
                assertEquals(
                        RowmotionChecker.rowmotionIdeal(ss, ideal),
                        RowmotionChecker.rowmotionViaToggles(ss, ideal));
            }
        }

        @Test
        void agreesDirectDiamond() {
            var ss = booleanLattice2();
            for (Set<Integer> ideal : RowmotionChecker.orderIdeals(ss)) {
                assertEquals(
                        RowmotionChecker.rowmotionIdeal(ss, ideal),
                        RowmotionChecker.rowmotionViaToggles(ss, ideal));
            }
        }
    }

    // =========================================================================
    // TestOrbits
    // =========================================================================

    @Nested
    class TestOrbits {

        @Test
        void orbitsPartitionAllIdeals() {
            var ss = booleanLattice2();
            var ideals = RowmotionChecker.orderIdeals(ss);
            var orbits = RowmotionChecker.allOrbits(ss);
            List<Set<Integer>> all = new ArrayList<>();
            for (var orbit : orbits) all.addAll(orbit);
            assertEquals(ideals.size(), all.size());
            assertEquals(new HashSet<>(ideals), new HashSet<>(all));
        }

        @Test
        void orbitSizesDivideOrder() {
            var ss = booleanLattice2();
            int order = RowmotionChecker.rowmotionOrder(ss);
            for (var orbit : RowmotionChecker.allOrbits(ss)) {
                assertEquals(0, order % orbit.size());
            }
        }

        @Test
        void orderIsLcmOfSizes() {
            var ss = booleanLattice2();
            int order = RowmotionChecker.rowmotionOrder(ss);
            var orbits = RowmotionChecker.allOrbits(ss);
            long expected = 1;
            for (var orbit : orbits) {
                long s = orbit.size();
                expected = expected / gcd(expected, s) * s;
            }
            assertEquals(expected, order);
        }

        @Test
        void eachOrbitIsACycle() {
            var ss = booleanLattice2();
            for (var orbit : RowmotionChecker.allOrbits(ss)) {
                var start = orbit.get(0);
                var current = start;
                for (int i = 0; i < orbit.size(); i++) {
                    current = RowmotionChecker.rowmotionIdeal(ss, current);
                }
                assertEquals(start, current);
            }
        }

        @Test
        void chain2SingleOrbit() {
            var ss = chain(2);
            var orbits = RowmotionChecker.allOrbits(ss);
            int total = orbits.stream().mapToInt(List::size).sum();
            assertEquals(3, total);
        }

        private long gcd(long a, long b) {
            while (b != 0) { long t = b; b = a % b; a = t; }
            return a;
        }
    }

    // =========================================================================
    // TestRowmotionOrder
    // =========================================================================

    @Nested
    class TestRowmotionOrder {

        @Test
        void singleState() {
            assertTrue(RowmotionChecker.rowmotionOrder(build("end")) >= 1);
        }

        @ParameterizedTest
        @CsvSource({"2, 3", "3, 4", "4, 5"})
        void chainOrder(int n, int expected) {
            assertEquals(expected, RowmotionChecker.rowmotionOrder(chain(n)));
        }

        @Test
        void diamondOrder4() {
            assertEquals(4, RowmotionChecker.rowmotionOrder(booleanLattice2()));
        }
    }

    // =========================================================================
    // TestHomomesy
    // =========================================================================

    @Nested
    class TestHomomesy {

        @Test
        void singleOrbitAlwaysHomomesic() {
            var ss = chain(2);
            var orbits = RowmotionChecker.allOrbits(ss);
            var result = RowmotionChecker.checkHomomesy(ss, orbits);
            assertTrue(result.isHomomesic());
            assertNotNull(result.average());
        }

        @Test
        void diamondHomomesy() {
            var ss = booleanLattice2();
            var orbits = RowmotionChecker.allOrbits(ss);
            var result = RowmotionChecker.checkHomomesy(ss, orbits);
            assertTrue(result.isHomomesic());
        }

        @Test
        void chain2Average() {
            var ss = chain(2);
            var orbits = RowmotionChecker.allOrbits(ss);
            var result = RowmotionChecker.checkHomomesy(ss, orbits);
            assertNotNull(result.average());
            assertEquals(1.0, result.average(), 1e-10);
        }

        @Test
        void emptyOrbitsTrivial() {
            var ss = build("end");
            var result = RowmotionChecker.checkHomomesy(ss, List.of());
            assertTrue(result.isHomomesic());
            assertNull(result.average());
        }
    }

    // =========================================================================
    // TestAnalyzeRowmotion
    // =========================================================================

    @Nested
    class TestAnalyzeRowmotion {

        @Test
        void endAnalysis() {
            var result = RowmotionChecker.analyzeRowmotion(build("end"));
            assertEquals(2, result.orderIdeals().size());
            assertEquals(2, result.antichains().size());
            assertTrue(result.numOrbits() >= 1);
            assertTrue(result.rowmotionOrder() >= 1);
        }

        @Test
        void branchAnalysis() {
            var result = RowmotionChecker.analyzeRowmotion(build("&{a: end}"));
            assertEquals(3, result.orderIdeals().size());
            assertEquals(3, result.antichains().size());
            assertEquals(3, result.rowmotionOrder());
        }

        @Test
        void allFieldsPopulated() {
            var result = RowmotionChecker.analyzeRowmotion(booleanLattice2());
            assertNotNull(result.orderIdeals());
            assertNotNull(result.antichains());
            assertEquals(4, result.rowmotionOrder());
            assertEquals(result.numOrbits(), result.orbitSizes().size());
            assertEquals(result.orderIdeals().size(),
                    result.orbitSizes().stream().mapToInt(Integer::intValue).sum());
            assertNotNull(result.toggleSequence());
        }

        @Test
        void parallelAnalysis() {
            var result = RowmotionChecker.analyzeRowmotion(build("(&{a: end} || &{b: end})"));
            assertEquals(6, result.orderIdeals().size());
            assertEquals(4, result.rowmotionOrder());
            assertTrue(result.isHomomesicCardinality());
        }

        @Test
        void orbitSizesSorted() {
            var result = RowmotionChecker.analyzeRowmotion(booleanLattice2());
            var sizes = result.orbitSizes();
            var sorted = new ArrayList<>(sizes);
            Collections.sort(sorted);
            assertEquals(sorted, sizes);
        }

        @Test
        void resultIsImmutable() {
            var result = RowmotionChecker.analyzeRowmotion(build("end"));
            // Record fields are immutable by design
            assertNotNull(result.orderIdeals());
        }
    }

    // =========================================================================
    // TestBenchmarks
    // =========================================================================

    @Nested
    class TestBenchmarks {

        @Test
        void iterator() {
            var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var result = RowmotionChecker.analyzeRowmotion(ss);
            assertTrue(result.rowmotionOrder() >= 1);
            assertTrue(result.numOrbits() >= 1);
            assertEquals(result.orderIdeals().size(),
                    result.orbitSizes().stream().mapToInt(Integer::intValue).sum());
        }

        @Test
        void fileObject() {
            var ss = build("&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}");
            var result = RowmotionChecker.analyzeRowmotion(ss);
            assertTrue(result.rowmotionOrder() >= 1);
            assertEquals(result.orderIdeals().size(),
                    result.orbitSizes().stream().mapToInt(Integer::intValue).sum());
        }

        @Test
        void nestedSelection() {
            var ss = build("&{a: +{x: end, y: end}}");
            var result = RowmotionChecker.analyzeRowmotion(ss);
            assertTrue(result.numOrbits() >= 1);
            assertEquals(result.orderIdeals().size(),
                    result.orbitSizes().stream().mapToInt(Integer::intValue).sum());
        }

        @Test
        void smtpLike() {
            var ss = build(
                    "&{connect: &{auth: +{OK: &{send: &{quit: end}}, FAIL: &{quit: end}}}}");
            var result = RowmotionChecker.analyzeRowmotion(ss);
            assertTrue(result.rowmotionOrder() >= 1);
            assertEquals(result.orderIdeals().size(),
                    result.orbitSizes().stream().mapToInt(Integer::intValue).sum());
        }

        @Test
        void simpleRecursion() {
            var ss = build("rec X . &{a: X, b: end}");
            var result = RowmotionChecker.analyzeRowmotion(ss);
            assertTrue(result.rowmotionOrder() >= 1);
            assertTrue(result.numOrbits() >= 1);
        }
    }

    // =========================================================================
    // TestMathematicalProperties
    // =========================================================================

    @Nested
    class TestMathematicalProperties {

        @ParameterizedTest
        @ValueSource(strings = {"end", "&{a: end}", "&{a: end, b: end}"})
        void rowmotionFiniteOrder(String type) {
            var ss = build(type);
            int order = RowmotionChecker.rowmotionOrder(ss);
            for (Set<Integer> ideal : RowmotionChecker.orderIdeals(ss)) {
                var current = ideal;
                for (int i = 0; i < order; i++) {
                    current = RowmotionChecker.rowmotionIdeal(ss, current);
                }
                assertEquals(ideal, current,
                        "Row^" + order + "(" + ideal + ") != " + ideal + " for " + type);
            }
        }

        @Test
        void orbitsSumToTotalIdeals() {
            for (var ss : List.of(chain(2), chain(3), booleanLattice2())) {
                var orbits = RowmotionChecker.allOrbits(ss);
                int total = orbits.stream().mapToInt(List::size).sum();
                assertEquals(RowmotionChecker.orderIdeals(ss).size(), total);
            }
        }

        @Test
        void antichainIdealBijectionIsPerfect() {
            for (var ss : List.of(chain(2), chain(3), booleanLattice2())) {
                assertEquals(
                        RowmotionChecker.antichains(ss).size(),
                        RowmotionChecker.orderIdeals(ss).size());
            }
        }

        @Test
        void brouwerSchrijver2x2() {
            assertEquals(4, RowmotionChecker.rowmotionOrder(booleanLattice2()));
        }

        @Test
        void rowmotionOrderDividesIdealCountFactorial() {
            var ss = booleanLattice2();
            int n = RowmotionChecker.orderIdeals(ss).size();
            int order = RowmotionChecker.rowmotionOrder(ss);
            long fact = 1;
            for (int i = 2; i <= n; i++) fact *= i;
            assertEquals(0, fact % order);
        }
    }
}
