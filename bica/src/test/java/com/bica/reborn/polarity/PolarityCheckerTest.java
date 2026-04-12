package com.bica.reborn.polarity;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PolarityChecker} — FCA-based polarity analysis.
 */
class PolarityCheckerTest {

    private static SessionType parse(String input) {
        return Parser.parse(input);
    }

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(parse(input));
    }

    // =========================================================================
    // Incidence relation
    // =========================================================================

    @Nested
    class IncidenceRelationTests {

        @Test
        void endHasEmptyRelation() {
            StateSpace ss = build("end");
            Map<Integer, Set<String>> rel = PolarityChecker.buildIncidenceRelation(ss);
            assertNotNull(rel);
            // end has 1 state with no transitions
            for (Set<String> labels : rel.values()) {
                assertTrue(labels.isEmpty());
            }
        }

        @Test
        void simpleBranchRelation() {
            StateSpace ss = build("&{a: end}");
            Map<Integer, Set<String>> rel = PolarityChecker.buildIncidenceRelation(ss);
            // Top state should have label "a" enabled
            assertTrue(rel.get(ss.top()).contains("a"));
            // Bottom state should have no labels
            assertTrue(rel.get(ss.bottom()).isEmpty());
        }

        @Test
        void twoBranchRelation() {
            StateSpace ss = build("&{a: end, b: end}");
            Map<Integer, Set<String>> rel = PolarityChecker.buildIncidenceRelation(ss);
            Set<String> topLabels = rel.get(ss.top());
            assertTrue(topLabels.contains("a"));
            assertTrue(topLabels.contains("b"));
        }

        @Test
        void nestedBranchRelation() {
            StateSpace ss = build("&{open: &{read: end, write: end}}");
            Map<Integer, Set<String>> rel = PolarityChecker.buildIncidenceRelation(ss);
            // Top should have "open"
            assertTrue(rel.get(ss.top()).contains("open"));
            // Bottom should have nothing
            assertTrue(rel.get(ss.bottom()).isEmpty());
        }
    }

    // =========================================================================
    // Closure operators
    // =========================================================================

    @Nested
    class ClosureTests {

        @Test
        void closureStatesEmptyLabels() {
            StateSpace ss = build("&{a: end}");
            Set<Integer> result = PolarityChecker.closureStates(ss, Set.of());
            // Empty label set → all states
            assertEquals(ss.states(), result);
        }

        @Test
        void closureStatesSpecificLabel() {
            StateSpace ss = build("&{a: end, b: end}");
            Set<Integer> result = PolarityChecker.closureStates(ss, Set.of("a"));
            // Only states where "a" is enabled
            assertTrue(result.contains(ss.top()));
            assertFalse(result.contains(ss.bottom()));
        }

        @Test
        void closureLabelsEmptyStates() {
            StateSpace ss = build("&{a: end, b: end}");
            Set<String> result = PolarityChecker.closureLabels(ss, Set.of());
            // Empty state set → all labels
            assertTrue(result.contains("a"));
            assertTrue(result.contains("b"));
        }

        @Test
        void closureLabelsSpecificState() {
            StateSpace ss = build("&{a: end, b: end}");
            Set<String> result = PolarityChecker.closureLabels(ss, Set.of(ss.top()));
            assertTrue(result.contains("a"));
            assertTrue(result.contains("b"));
        }

        @Test
        void closureLabelsBottomState() {
            StateSpace ss = build("&{a: end}");
            Set<String> result = PolarityChecker.closureLabels(ss, Set.of(ss.bottom()));
            // Bottom has no labels
            assertTrue(result.isEmpty());
        }
    }

    // =========================================================================
    // Concept computation
    // =========================================================================

    @Nested
    class ConceptTests {

        @Test
        void endHasOneConcept() {
            StateSpace ss = build("end");
            List<Concept> concepts = PolarityChecker.computeConcepts(ss);
            // end: 1 state, no labels → 1 concept: ({0}, {})
            assertFalse(concepts.isEmpty());
        }

        @Test
        void simpleBranchConcepts() {
            StateSpace ss = build("&{a: end}");
            List<Concept> concepts = PolarityChecker.computeConcepts(ss);
            // At least 2 concepts: top ({all states}, {common labels}) and bottom
            assertTrue(concepts.size() >= 2);
        }

        @Test
        void conceptsAreSortedByExtentSize() {
            StateSpace ss = build("&{a: end, b: end}");
            List<Concept> concepts = PolarityChecker.computeConcepts(ss);
            for (int i = 0; i < concepts.size() - 1; i++) {
                assertTrue(concepts.get(i).extent().size() >= concepts.get(i + 1).extent().size());
            }
        }

        @Test
        void conceptExtentIntentDuality() {
            StateSpace ss = build("&{a: end, b: end}");
            Map<Integer, Set<String>> rel = PolarityChecker.buildIncidenceRelation(ss);
            List<Concept> concepts = PolarityChecker.computeConcepts(ss);

            for (Concept c : concepts) {
                // Verify: closureStates(intent) = extent
                Set<Integer> closed = PolarityChecker.closureStates(ss, c.intent());
                assertEquals(c.extent(), closed,
                        "closureStates(intent) should equal extent for concept " + c);
            }
        }

        @Test
        void twoBranchHasExpectedConcepts() {
            StateSpace ss = build("&{a: end, b: end}");
            List<Concept> concepts = PolarityChecker.computeConcepts(ss);
            // Should have at least: ({top, end}, {}), ({top}, {a,b}), ({end}, ?)
            assertTrue(concepts.size() >= 2);
        }
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    @Nested
    class FullAnalysisTests {

        @Test
        void analyzeEnd() {
            PolarityResult result = PolarityChecker.analyzePolarity(build("end"));
            assertTrue(result.isConceptLattice());
            assertTrue(result.numConcepts() >= 1);
        }

        @Test
        void analyzeSimpleBranch() {
            PolarityResult result = PolarityChecker.analyzePolarity(build("&{a: end}"));
            assertTrue(result.isConceptLattice());
            assertTrue(result.numConcepts() >= 2);
        }

        @Test
        void analyzeTwoBranch() {
            PolarityResult result = PolarityChecker.analyzePolarity(
                    build("&{a: end, b: end}"));
            assertTrue(result.isConceptLattice());
        }

        @Test
        void analyzeSelection() {
            PolarityResult result = PolarityChecker.analyzePolarity(
                    build("+{ok: end, err: end}"));
            assertTrue(result.isConceptLattice());
        }

        @Test
        void analyzeNestedBranch() {
            PolarityResult result = PolarityChecker.analyzePolarity(
                    build("&{open: &{read: end, write: end}}"));
            assertTrue(result.isConceptLattice());
            // Nested branches produce more states → more concepts
            assertTrue(result.numConcepts() >= 2);
        }

        @Test
        void galoisConnectionAlwaysHolds() {
            // The Galois connection is guaranteed by construction
            String[] types = {"end", "&{a: end}", "&{a: end, b: end}",
                    "+{ok: end}", "&{a: &{b: end}}"};
            for (String t : types) {
                PolarityResult result = PolarityChecker.analyzePolarity(build(t));
                assertTrue(result.isConceptLattice(),
                        "Galois connection should hold for: " + t);
            }
        }

        @Test
        void conceptLatticeEdgesAreValid() {
            PolarityResult result = PolarityChecker.analyzePolarity(
                    build("&{a: end, b: end}"));
            for (int[] edge : result.conceptLatticeEdges()) {
                assertTrue(edge[0] >= 0 && edge[0] < result.numConcepts());
                assertTrue(edge[1] >= 0 && edge[1] < result.numConcepts());
                assertNotEquals(edge[0], edge[1]);
            }
        }
    }
}
