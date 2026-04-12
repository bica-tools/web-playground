package com.bica.reborn.coverage;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.statespace.TransitionKind;
import com.bica.reborn.testgen.PathEnumerator;
import com.bica.reborn.testgen.PathEnumerator.*;
import com.bica.reborn.testgen.TestGenConfig;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for test coverage analysis.
 */
class CoverageCheckerTest {

    private StateSpace buildSS(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Basic coverage
    // -----------------------------------------------------------------------

    @Nested
    class BasicTests {

        @Test
        void emptyStateSpaceHasFullCoverage() {
            StateSpace ss = buildSS("end");
            CoverageResult cr = CoverageChecker.computeCoverage(ss,
                    List.of(), List.of(), List.of());
            assertEquals(1.0, cr.transitionCoverage());
            assertEquals(1.0, cr.stateCoverage());
        }

        @Test
        void singleTransitionCoveredByOnePath() {
            StateSpace ss = buildSS("&{m: end}");
            List<Step> steps = List.of(new Step("m", ss.bottom()));
            ValidPath path = new ValidPath(steps);
            CoverageResult cr = CoverageChecker.computeCoverage(ss,
                    List.of(path), List.of(), List.of());
            assertEquals(1.0, cr.transitionCoverage());
            assertEquals(1.0, cr.stateCoverage());
        }

        @Test
        void uncoveredTransitionReported() {
            StateSpace ss = buildSS("&{m: end, n: end}");
            // Only cover 'm'
            int endState = ss.bottom();
            List<Step> steps = List.of(new Step("m", endState));
            ValidPath path = new ValidPath(steps);
            CoverageResult cr = CoverageChecker.computeCoverage(ss,
                    List.of(path), List.of(), List.of());
            assertEquals(0.5, cr.transitionCoverage());
            assertFalse(cr.uncoveredTransitions().isEmpty());
        }

        @Test
        void fullCoverageWithAllPaths() {
            StateSpace ss = buildSS("&{m: end, n: end}");
            int endState = ss.bottom();
            ValidPath p1 = new ValidPath(List.of(new Step("m", endState)));
            ValidPath p2 = new ValidPath(List.of(new Step("n", endState)));
            CoverageResult cr = CoverageChecker.computeCoverage(ss,
                    List.of(p1, p2), List.of(), List.of());
            assertEquals(1.0, cr.transitionCoverage());
            assertTrue(cr.uncoveredTransitions().isEmpty());
        }

        @Test
        void coverageCountsStates() {
            StateSpace ss = buildSS("&{m: end}");
            CoverageResult cr = CoverageChecker.computeCoverage(ss,
                    List.of(), List.of(), List.of());
            assertEquals(0.0, cr.transitionCoverage());
            assertTrue(cr.coveredStates().isEmpty());
            assertEquals(ss.states().size(), cr.uncoveredStates().size());
        }
    }

    // -----------------------------------------------------------------------
    // Coverage from violations and incomplete prefixes
    // -----------------------------------------------------------------------

    @Nested
    class MixedPathTests {

        @Test
        void violationPathContributesToCoverage() {
            StateSpace ss = buildSS("&{m: &{n: end}}");
            // Find intermediate state
            int midState = -1;
            for (Transition t : ss.transitions()) {
                if (t.source() == ss.top() && t.label().equals("m")) {
                    midState = t.target();
                }
            }
            assertTrue(midState >= 0);

            ViolationPoint vp = new ViolationPoint(
                    midState, "x", Set.of("n"),
                    List.of(new Step("m", midState)));
            CoverageResult cr = CoverageChecker.computeCoverage(ss,
                    List.of(), List.of(vp), List.of());
            assertTrue(cr.transitionCoverage() > 0);
            assertTrue(cr.coveredTransitions().size() >= 1);
        }

        @Test
        void incompletePrefixContributesToCoverage() {
            StateSpace ss = buildSS("&{m: &{n: end}}");
            int midState = -1;
            for (Transition t : ss.transitions()) {
                if (t.source() == ss.top() && t.label().equals("m")) {
                    midState = t.target();
                }
            }
            assertTrue(midState >= 0);

            IncompletePrefix ip = new IncompletePrefix(
                    List.of(new Step("m", midState)), Set.of("n"));
            CoverageResult cr = CoverageChecker.computeCoverage(ss,
                    List.of(), List.of(), List.of(ip));
            assertTrue(cr.transitionCoverage() > 0);
        }
    }

    // -----------------------------------------------------------------------
    // Integration with PathEnumerator
    // -----------------------------------------------------------------------

    @Nested
    class IntegrationTests {

        @Test
        void enumerationResultGivesFullCoverageOnSimple() {
            StateSpace ss = buildSS("&{m: end}");
            EnumerationResult er = PathEnumerator.enumerate(ss, TestGenConfig.withDefaults("Test"));
            CoverageResult cr = CoverageChecker.computeCoverage(ss, er);
            assertEquals(1.0, cr.transitionCoverage());
            assertEquals(1.0, cr.stateCoverage());
        }

        @Test
        void branchingProtocolCoverage() {
            StateSpace ss = buildSS("&{m: end, n: end}");
            EnumerationResult er = PathEnumerator.enumerate(ss, TestGenConfig.withDefaults("Test"));
            CoverageResult cr = CoverageChecker.computeCoverage(ss, er);
            assertEquals(1.0, cr.transitionCoverage());
        }

        @Test
        void sequentialProtocolCoverage() {
            StateSpace ss = buildSS("&{open: &{read: &{close: end}}}");
            EnumerationResult er = PathEnumerator.enumerate(ss, TestGenConfig.withDefaults("Test"));
            CoverageResult cr = CoverageChecker.computeCoverage(ss, er);
            assertEquals(1.0, cr.transitionCoverage());
            assertEquals(1.0, cr.stateCoverage());
        }

        @Test
        void coverageResultFieldsConsistent() {
            StateSpace ss = buildSS("&{m: end, n: end}");
            EnumerationResult er = PathEnumerator.enumerate(ss, TestGenConfig.withDefaults("Test"));
            CoverageResult cr = CoverageChecker.computeCoverage(ss, er);
            assertEquals(ss.transitions().size(),
                    cr.coveredTransitions().size() + cr.uncoveredTransitions().size());
            assertEquals(ss.states().size(),
                    cr.coveredStates().size() + cr.uncoveredStates().size());
        }

        @Test
        void recursiveProtocolCoverage() {
            StateSpace ss = buildSS("rec X . &{data: X, done: end}");
            EnumerationResult er = PathEnumerator.enumerate(ss, TestGenConfig.withDefaults("Test"));
            CoverageResult cr = CoverageChecker.computeCoverage(ss, er);
            assertEquals(1.0, cr.transitionCoverage());
        }
    }

    // -----------------------------------------------------------------------
    // CoverageResult record
    // -----------------------------------------------------------------------

    @Nested
    class ResultTests {

        @Test
        void setsAreImmutable() {
            CoverageResult cr = new CoverageResult(
                    Set.of(), Set.of(), Set.of(), Set.of(), 1.0, 1.0);
            assertThrows(UnsupportedOperationException.class,
                    () -> cr.coveredStates().add(42));
        }

        @Test
        void coverageValuesInRange() {
            StateSpace ss = buildSS("&{m: end, n: end}");
            int endState = ss.bottom();
            ValidPath p1 = new ValidPath(List.of(new Step("m", endState)));
            CoverageResult cr = CoverageChecker.computeCoverage(ss,
                    List.of(p1), List.of(), List.of());
            assertTrue(cr.transitionCoverage() >= 0.0);
            assertTrue(cr.transitionCoverage() <= 1.0);
            assertTrue(cr.stateCoverage() >= 0.0);
            assertTrue(cr.stateCoverage() <= 1.0);
        }
    }
}
