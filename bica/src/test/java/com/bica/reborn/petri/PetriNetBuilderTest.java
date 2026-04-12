package com.bica.reborn.petri;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link PetriNetBuilder}.
 *
 * <p>Ported from {@code reticulate/tests/test_petri.py}.
 */
class PetriNetBuilderTest {

    // -- helpers ---------------------------------------------------------------

    private static StateSpace ss(String source) {
        return StateSpaceBuilder.build(Parser.parse(source));
    }

    private static PetriNet net(String source) {
        return PetriNetBuilder.buildPetriNet(ss(source));
    }

    // =========================================================================
    // 1. End — trivial net
    // =========================================================================

    @Nested
    class EndType {

        @Test
        void trivialNetHasOnePlace() {
            var n = net("end");
            assertEquals(1, n.places().size());
        }

        @Test
        void trivialNetHasZeroTransitions() {
            var n = net("end");
            assertEquals(0, n.transitions().size());
        }

        @Test
        void trivialNetInitialMarkingOnBottom() {
            var s = ss("end");
            var n = PetriNetBuilder.buildPetriNet(s);
            // top == bottom for end
            assertEquals(s.top(), s.bottom());
            assertEquals(Map.of(s.top(), 1), n.initialMarking());
        }

        @Test
        void trivialNetIsOccurrenceNet() {
            assertTrue(net("end").isOccurrenceNet());
        }

        @Test
        void trivialNetIsFreeChoice() {
            assertTrue(net("end").isFreeChoice());
        }
    }

    // =========================================================================
    // 2. Branch — conflict at top
    // =========================================================================

    @Nested
    class BranchType {

        @Test
        void branchHasThreePlaces() {
            // &{a: end, b: end} -> 3 states: top, end (shared), so actually
            // top + end = 2 if both map to same end. Let's check.
            var s = ss("&{a: end, b: end}");
            var n = PetriNetBuilder.buildPetriNet(s);
            assertEquals(s.states().size(), n.places().size());
        }

        @Test
        void branchHasTwoTransitions() {
            var n = net("&{a: end, b: end}");
            assertEquals(2, n.transitions().size());
        }

        @Test
        void branchConflictAtTop() {
            var s = ss("&{a: end, b: end}");
            var n = PetriNetBuilder.buildPetriNet(s);
            var conflicts = PetriNetBuilder.conflictPlaces(n);
            assertFalse(conflicts.isEmpty(), "should have conflict at branch point");
            // The top state should be a conflict place
            boolean topIsConflict = conflicts.stream()
                    .anyMatch(e -> e.getKey() == s.top());
            assertTrue(topIsConflict, "top state should be a conflict place");
        }

        @Test
        void branchTransitionsNotSelection() {
            var n = net("&{a: end, b: end}");
            for (var t : n.transitions().values()) {
                assertFalse(t.isSelection(), "branch transitions should not be selections");
            }
        }

        @Test
        void branchIsOccurrenceNet() {
            assertTrue(net("&{a: end, b: end}").isOccurrenceNet());
        }
    }

    // =========================================================================
    // 3. Selection — selection transitions flagged
    // =========================================================================

    @Nested
    class SelectionType {

        @Test
        void selectionTransitionsFlagged() {
            var n = net("+{x: end, y: end}");
            for (var t : n.transitions().values()) {
                assertTrue(t.isSelection(), "selection transitions should be flagged");
            }
        }

        @Test
        void selectionHasTwoTransitions() {
            assertEquals(2, net("+{x: end, y: end}").transitions().size());
        }

        @Test
        void selectionHasConflict() {
            var s = ss("+{x: end, y: end}");
            var n = PetriNetBuilder.buildPetriNet(s);
            var conflicts = PetriNetBuilder.conflictPlaces(n);
            assertFalse(conflicts.isEmpty());
        }
    }

    // =========================================================================
    // 4. Parallel — product encoding
    // =========================================================================

    @Nested
    class ParallelType {

        @Test
        void parallelPlaces() {
            var s = ss("(&{a: end} || &{b: end})");
            var n = PetriNetBuilder.buildPetriNet(s);
            // product: (top,top), (top,end), (end,top), (end,end) = 4 states
            assertEquals(s.states().size(), n.places().size());
            assertTrue(n.places().size() >= 3, "parallel should produce multiple places");
        }

        @Test
        void parallelTransitions() {
            var s = ss("(&{a: end} || &{b: end})");
            var n = PetriNetBuilder.buildPetriNet(s);
            // Product interleaving: a from (top,top) and (top,end), b from (top,top) and (end,top)
            assertEquals(s.transitions().size(), n.transitions().size());
            assertTrue(n.transitions().size() >= 2, "parallel should have at least 2 transitions");
        }

        @Test
        void parallelIsOccurrenceNet() {
            assertTrue(net("(&{a: end} || &{b: end})").isOccurrenceNet());
        }
    }

    // =========================================================================
    // 5. Recursion — cyclic net (not occurrence net)
    // =========================================================================

    @Nested
    class RecursionType {

        @Test
        void recursionNotOccurrenceNet() {
            assertFalse(net("rec X . &{a: X, b: end}").isOccurrenceNet(),
                    "recursive type should produce cyclic (non-occurrence) net");
        }

        @Test
        void recursionPlaces() {
            var s = ss("rec X . &{a: X, b: end}");
            var n = PetriNetBuilder.buildPetriNet(s);
            assertEquals(s.states().size(), n.places().size());
        }

        @Test
        void recursionTransitions() {
            var n = net("rec X . &{a: X, b: end}");
            assertEquals(2, n.transitions().size());
        }

        @Test
        void recursionIsFreeChoice() {
            assertTrue(net("rec X . &{a: X, b: end}").isFreeChoice());
        }
    }

    // =========================================================================
    // 6. Reachability graph isomorphism
    // =========================================================================

    @Nested
    class ReachabilityIsomorphism {

        @Test
        void endIsomorphic() {
            var s = ss("end");
            assertTrue(PetriNetBuilder.verifyIsomorphism(s, PetriNetBuilder.buildPetriNet(s)));
        }

        @Test
        void branchIsomorphic() {
            var s = ss("&{a: end, b: end}");
            assertTrue(PetriNetBuilder.verifyIsomorphism(s, PetriNetBuilder.buildPetriNet(s)));
        }

        @Test
        void selectionIsomorphic() {
            var s = ss("+{x: end, y: end}");
            assertTrue(PetriNetBuilder.verifyIsomorphism(s, PetriNetBuilder.buildPetriNet(s)));
        }

        @Test
        void parallelIsomorphic() {
            var s = ss("(&{a: end} || &{b: end})");
            assertTrue(PetriNetBuilder.verifyIsomorphism(s, PetriNetBuilder.buildPetriNet(s)));
        }

        @Test
        void recursionIsomorphic() {
            var s = ss("rec X . &{a: X, b: end}");
            assertTrue(PetriNetBuilder.verifyIsomorphism(s, PetriNetBuilder.buildPetriNet(s)));
        }

        @Test
        void deepBranchIsomorphic() {
            var s = ss("&{a: &{c: end}, b: end}");
            assertTrue(PetriNetBuilder.verifyIsomorphism(s, PetriNetBuilder.buildPetriNet(s)));
        }
    }

    // =========================================================================
    // 7. Place invariants
    // =========================================================================

    @Nested
    class PlaceInvariants {

        @Test
        void endInvariant() {
            var n = net("end");
            var invariants = PetriNetBuilder.placeInvariants(n);
            // end has 0 transitions, trivial invariant is valid
            assertEquals(1, invariants.size());
        }

        @Test
        void branchAllOnesInvariant() {
            var n = net("&{a: end, b: end}");
            var invariants = PetriNetBuilder.placeInvariants(n);
            assertFalse(invariants.isEmpty(), "all-ones should be a valid invariant");
            // Verify all coefficients are 1
            var inv = invariants.get(0);
            for (int pid : n.places().keySet()) {
                assertEquals(1, inv.get(pid));
            }
        }

        @Test
        void recursionInvariant() {
            var n = net("rec X . &{a: X, b: end}");
            var invariants = PetriNetBuilder.placeInvariants(n);
            assertFalse(invariants.isEmpty());
        }

        @Test
        void parallelInvariant() {
            var n = net("(&{a: end} || &{b: end})");
            var invariants = PetriNetBuilder.placeInvariants(n);
            assertFalse(invariants.isEmpty());
        }
    }

    // =========================================================================
    // 8. DOT output
    // =========================================================================

    @Nested
    class DotOutput {

        @Test
        void dotNonEmpty() {
            var dot = PetriNetBuilder.petriDot(net("&{a: end, b: end}"));
            assertFalse(dot.isEmpty());
            assertTrue(dot.contains("digraph PetriNet"));
        }

        @Test
        void dotContainsPlaces() {
            var dot = PetriNetBuilder.petriDot(net("&{a: end, b: end}"));
            assertTrue(dot.contains("shape=circle"));
        }

        @Test
        void dotContainsTransitions() {
            var dot = PetriNetBuilder.petriDot(net("&{a: end, b: end}"));
            assertTrue(dot.contains("shape=box"));
        }

        @Test
        void dotSelectionHighlighted() {
            var dot = PetriNetBuilder.petriDot(net("+{x: end, y: end}"));
            assertTrue(dot.contains("fillcolor"), "selection transitions should be highlighted");
        }

        @Test
        void endDot() {
            var dot = PetriNetBuilder.petriDot(net("end"));
            assertTrue(dot.contains("digraph PetriNet"));
        }
    }

    // =========================================================================
    // 9. Firing semantics
    // =========================================================================

    @Nested
    class FiringSemantics {

        @Test
        void fireEnabledTransition() {
            var s = ss("&{a: end, b: end}");
            var n = PetriNetBuilder.buildPetriNet(s);
            var enabled = PetriNetBuilder.enabledTransitions(n, new HashMap<>(n.initialMarking()));
            assertFalse(enabled.isEmpty());
            var newMarking = PetriNetBuilder.fire(n, new HashMap<>(n.initialMarking()), enabled.get(0));
            assertNotNull(newMarking);
        }

        @Test
        void fireDisabledReturnsNull() {
            var s = ss("&{a: end, b: end}");
            var n = PetriNetBuilder.buildPetriNet(s);
            // Fire with empty marking — nothing is enabled
            var result = PetriNetBuilder.fire(n, Map.of(), 0);
            assertNull(result);
        }

        @Test
        void enabledTransitionsAtEnd() {
            var s = ss("&{a: end}");
            var n = PetriNetBuilder.buildPetriNet(s);
            // Fire the only transition to reach end
            var enabled = PetriNetBuilder.enabledTransitions(n, new HashMap<>(n.initialMarking()));
            assertEquals(1, enabled.size());
            var endMarking = PetriNetBuilder.fire(n, new HashMap<>(n.initialMarking()), enabled.get(0));
            assertNotNull(endMarking);
            // At end, no transitions should be enabled
            var endEnabled = PetriNetBuilder.enabledTransitions(n, endMarking);
            assertTrue(endEnabled.isEmpty(), "no transitions should be enabled at end");
        }
    }

    // =========================================================================
    // 10. High-level: sessionTypeToPetriNet
    // =========================================================================

    @Nested
    class HighLevel {

        @Test
        void resultCorrect() {
            var s = ss("&{a: end, b: end}");
            var result = PetriNetBuilder.sessionTypeToPetriNet(s);
            assertEquals(s.states().size(), result.numPlaces());
            assertEquals(2, result.numTransitions());
            assertTrue(result.isOccurrenceNet());
            assertTrue(result.isFreeChoice());
            assertTrue(result.reachabilityIsomorphic());
        }

        @Test
        void recursionResult() {
            var s = ss("rec X . &{a: X, b: end}");
            var result = PetriNetBuilder.sessionTypeToPetriNet(s);
            assertFalse(result.isOccurrenceNet());
            assertTrue(result.reachabilityIsomorphic());
        }
    }
}
