package com.bica.reborn.csp;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CSP trace and failures semantics (Step 27/28 port).
 *
 * <p>Covers trace extraction, complete traces, alphabet, refusals, failures,
 * refinement, determinism, equivalence, must/may testing, and lattice-determines-failures.
 */
class CSPCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    // =========================================================================
    // Trace extraction
    // =========================================================================

    @Nested
    class TraceExtraction {

        @Test
        void endTypeHasOnlyEmptyTrace() {
            var ss = build("end");
            var traces = CSPChecker.extractTraces(ss, 10);
            assertEquals(Set.of(List.of()), traces);
        }

        @Test
        void branchHasEmptyPlusSingleStepTraces() {
            var ss = build("&{a: end, b: end}");
            var traces = CSPChecker.extractTraces(ss, 10);
            assertTrue(traces.contains(List.of()));
            assertTrue(traces.contains(List.of("a")));
            assertTrue(traces.contains(List.of("b")));
            assertEquals(3, traces.size());
        }

        @Test
        void selectHasEmptyPlusSingleStepTraces() {
            var ss = build("+{ok: end, err: end}");
            var traces = CSPChecker.extractTraces(ss, 10);
            assertTrue(traces.contains(List.of()));
            assertTrue(traces.contains(List.of("ok")));
            assertTrue(traces.contains(List.of("err")));
            assertEquals(3, traces.size());
        }

        @Test
        void nestedBranchProducesMultiStepTraces() {
            var ss = build("&{a: &{b: end}}");
            var traces = CSPChecker.extractTraces(ss, 10);
            assertTrue(traces.contains(List.of()));
            assertTrue(traces.contains(List.of("a")));
            assertTrue(traces.contains(List.of("a", "b")));
            assertEquals(3, traces.size());
        }

        @Test
        void parallelProducesInterleavings() {
            var ss = build("(&{a: end} || &{b: end})");
            var traces = CSPChecker.extractTraces(ss, 10);
            assertTrue(traces.contains(List.of()));
            // Both orderings of a and b should appear
            assertTrue(traces.contains(List.of("a", "b")));
            assertTrue(traces.contains(List.of("b", "a")));
        }

        @Test
        void recursiveTypeProducesRepeatedTraces() {
            var ss = build("rec X . &{a: X, b: end}");
            var traces = CSPChecker.extractTraces(ss, 3);
            assertTrue(traces.contains(List.of("a")));
            assertTrue(traces.contains(List.of("b")));
            assertTrue(traces.contains(List.of("a", "a")));
            assertTrue(traces.contains(List.of("a", "b")));
        }

        @Test
        void emptyTraceAlwaysPresent() {
            for (String type : List.of("end", "&{a: end}", "+{x: end}", "rec X . &{a: X, b: end}")) {
                var ss = build(type);
                var traces = CSPChecker.extractTraces(ss, 10);
                assertTrue(traces.contains(List.of()),
                        "Empty trace missing for type: " + type);
            }
        }
    }

    // =========================================================================
    // Complete traces
    // =========================================================================

    @Nested
    class CompleteTraces {

        @Test
        void endTypeHasEmptyCompleteTrace() {
            var ss = build("end");
            var ct = CSPChecker.extractCompleteTraces(ss, 10);
            assertEquals(Set.of(List.of()), ct);
        }

        @Test
        void branchCompleteTracesReachBottom() {
            var ss = build("&{a: end, b: end}");
            var ct = CSPChecker.extractCompleteTraces(ss, 10);
            assertEquals(Set.of(List.of("a"), List.of("b")), ct);
        }

        @Test
        void nestedBranchCompleteTrace() {
            var ss = build("&{a: &{b: end}}");
            var ct = CSPChecker.extractCompleteTraces(ss, 10);
            assertEquals(Set.of(List.of("a", "b")), ct);
        }

        @Test
        void recursiveTypeCompleteTracesEndWithTerminal() {
            var ss = build("rec X . &{a: X, b: end}");
            var ct = CSPChecker.extractCompleteTraces(ss, 5);
            // All complete traces must end with 'b'
            for (var trace : ct) {
                assertFalse(trace.isEmpty());
                assertEquals("b", trace.getLast());
            }
            assertTrue(ct.contains(List.of("b")));
            assertTrue(ct.contains(List.of("a", "b")));
        }
    }

    // =========================================================================
    // Alphabet
    // =========================================================================

    @Nested
    class AlphabetExtraction {

        @Test
        void endTypeHasEmptyAlphabet() {
            var ss = build("end");
            assertEquals(Set.of(), CSPChecker.alphabet(ss));
        }

        @Test
        void branchAlphabet() {
            var ss = build("&{a: end, b: end}");
            assertEquals(Set.of("a", "b"), CSPChecker.alphabet(ss));
        }

        @Test
        void nestedAlphabet() {
            var ss = build("&{open: &{read: end, write: end}}");
            assertEquals(Set.of("open", "read", "write"), CSPChecker.alphabet(ss));
        }
    }

    // =========================================================================
    // Refusals
    // =========================================================================

    @Nested
    class Refusals {

        @Test
        void bottomRefusesFullAlphabet() {
            var ss = build("&{a: end, b: end}");
            var refusals = CSPChecker.computeRefusals(ss, ss.bottom());
            assertEquals(Set.of("a", "b"), refusals);
        }

        @Test
        void topOfBranchRefusesNothing() {
            var ss = build("&{a: end, b: end}");
            var refusals = CSPChecker.computeRefusals(ss, ss.top());
            assertTrue(refusals.isEmpty(),
                    "Top of &{a: end, b: end} enables both a and b, refuses nothing");
        }

        @Test
        void allRefusalsCoversAllStates() {
            var ss = build("&{a: end, b: end}");
            var allRef = CSPChecker.computeAllRefusals(ss);
            assertEquals(ss.states().size(), allRef.size());
        }

        @Test
        void acceptanceSetAtTop() {
            var ss = build("&{a: end, b: end}");
            var acc = CSPChecker.acceptanceSet(ss, ss.top());
            assertEquals(Set.of("a", "b"), acc);
        }
    }

    // =========================================================================
    // Failures
    // =========================================================================

    @Nested
    class FailuresComputation {

        @Test
        void endTypeFailures() {
            var ss = build("end");
            var f = CSPChecker.failures(ss, 10);
            // Single failure: empty trace, empty refusals (empty alphabet)
            assertEquals(1, f.size());
            var failure = f.iterator().next();
            assertEquals(List.of(), failure.trace());
            assertTrue(failure.refusals().isEmpty());
        }

        @Test
        void branchFailuresIncludeBottomRefusal() {
            var ss = build("&{a: end, b: end}");
            var f = CSPChecker.failures(ss, 10);
            // After trace <a>, at bottom, refuse {a, b}
            assertTrue(f.contains(new Failure(List.of("a"), Set.of("a", "b"))));
            assertTrue(f.contains(new Failure(List.of("b"), Set.of("a", "b"))));
        }

        @Test
        void topFailureHasEmptyRefusal() {
            var ss = build("&{a: end, b: end}");
            var f = CSPChecker.failures(ss, 10);
            // At top, both a and b are enabled, so refusal is empty
            assertTrue(f.contains(new Failure(List.of(), Set.of())));
        }
    }

    // =========================================================================
    // Determinism
    // =========================================================================

    @Nested
    class Determinism {

        @Test
        void branchIsDeterministic() {
            var ss = build("&{a: end, b: end}");
            assertTrue(CSPChecker.isDeterministic(ss));
        }

        @Test
        void selectIsDeterministic() {
            // Session type selections are deterministic in state space
            var ss = build("+{ok: end, err: end}");
            assertTrue(CSPChecker.isDeterministic(ss));
        }

        @Test
        void nestedBranchIsDeterministic() {
            var ss = build("&{a: &{b: end}, c: end}");
            assertTrue(CSPChecker.isDeterministic(ss));
        }

        @Test
        void recursiveTypeIsDeterministic() {
            var ss = build("rec X . &{a: X, b: end}");
            assertTrue(CSPChecker.isDeterministic(ss));
        }
    }

    // =========================================================================
    // Trace refinement
    // =========================================================================

    @Nested
    class TraceRefinementTests {

        @Test
        void subtypeTracesSubsetOfSupertype() {
            // &{a: end} is a subtype of &{a: end, b: end} (fewer branches)
            // subtype traces should be a subset
            var sub = build("&{a: end}");
            var sup = build("&{a: end, b: end}");
            var subTraces = CSPChecker.extractTraces(sub, 10);
            var supTraces = CSPChecker.extractTraces(sup, 10);
            assertTrue(CSPChecker.traceRefinement(subTraces, supTraces));
        }

        @Test
        void supertypeTracesNotSubsetOfSubtype() {
            var sub = build("&{a: end}");
            var sup = build("&{a: end, b: end}");
            var subTraces = CSPChecker.extractTraces(sub, 10);
            var supTraces = CSPChecker.extractTraces(sup, 10);
            assertFalse(CSPChecker.traceRefinement(supTraces, subTraces));
        }

        @Test
        void identicalTypesRefineEachOther() {
            var ss1 = build("&{a: end, b: end}");
            var ss2 = build("&{a: end, b: end}");
            var t1 = CSPChecker.extractTraces(ss1, 10);
            var t2 = CSPChecker.extractTraces(ss2, 10);
            assertTrue(CSPChecker.traceRefinement(t1, t2));
            assertTrue(CSPChecker.traceRefinement(t2, t1));
        }
    }

    // =========================================================================
    // Failures refinement
    // =========================================================================

    @Nested
    class FailuresRefinement {

        @Test
        void sameTypeFailuresRefine() {
            var ss1 = build("&{a: end, b: end}");
            var ss2 = build("&{a: end, b: end}");
            var result = CSPChecker.checkFailuresRefinement(ss1, ss2, 10);
            assertTrue(result.isRefinement());
            assertTrue(result.tracesRefined());
            assertTrue(result.failuresRefined());
        }

        @Test
        void failuresRefinementCounterexamples() {
            // &{a: end, b: end} has more traces than &{a: end}, so
            // &{a: end, b: end} does NOT failure-refine &{a: end}
            var ss1 = build("&{a: end, b: end}");
            var ss2 = build("&{a: end}");
            var result = CSPChecker.checkFailuresRefinement(ss1, ss2, 10);
            assertFalse(result.isRefinement());
            assertFalse(result.traceCounterexamples().isEmpty());
        }
    }

    // =========================================================================
    // Trace and failures equivalence
    // =========================================================================

    @Nested
    class Equivalence {

        @Test
        void isomorphicTypesAreTraceEquivalent() {
            var ss1 = build("&{a: end, b: end}");
            var ss2 = build("&{b: end, a: end}");
            assertTrue(CSPChecker.traceEquivalent(ss1, ss2, 10));
        }

        @Test
        void isomorphicTypesAreFailuresEquivalent() {
            var ss1 = build("&{a: end, b: end}");
            var ss2 = build("&{b: end, a: end}");
            assertTrue(CSPChecker.failuresEquivalent(ss1, ss2, 10));
        }

        @Test
        void differentTypesNotTraceEquivalent() {
            var ss1 = build("&{a: end}");
            var ss2 = build("&{a: end, b: end}");
            assertFalse(CSPChecker.traceEquivalent(ss1, ss2, 10));
        }
    }

    // =========================================================================
    // CSP analysis pipeline
    // =========================================================================

    @Nested
    class CSPAnalysis {

        @Test
        void fullAnalysisForBranch() {
            var ss = build("&{a: end, b: end}");
            var result = CSPChecker.cspAnalysis(ss, 10);
            assertEquals(3, result.traces().size());
            assertEquals(2, result.completeTraces().size());
            assertEquals(Set.of("a", "b"), result.alphabet());
            assertFalse(result.failures().isEmpty());
        }

        @Test
        void fullAnalysisForEnd() {
            var ss = build("end");
            var result = CSPChecker.cspAnalysis(ss, 10);
            assertEquals(1, result.traces().size());
            assertEquals(1, result.completeTraces().size());
            assertTrue(result.alphabet().isEmpty());
        }
    }

    // =========================================================================
    // Must / May testing
    // =========================================================================

    @Nested
    class MustMayTesting {

        @Test
        void mustTestingAtInitial() {
            var ss = build("&{a: end, b: end}");
            var must = CSPChecker.mustTesting(ss, List.of());
            assertEquals(Set.of("a", "b"), must);
        }

        @Test
        void mustTestingAfterBranch() {
            var ss = build("&{a: &{c: end}, b: end}");
            var must = CSPChecker.mustTesting(ss, List.of("a"));
            assertEquals(Set.of("c"), must);
        }

        @Test
        void mustTestingAtBottom() {
            var ss = build("&{a: end}");
            var must = CSPChecker.mustTesting(ss, List.of("a"));
            assertTrue(must.isEmpty());
        }

        @Test
        void mayTestingAtInitial() {
            var ss = build("&{a: end, b: end}");
            var may = CSPChecker.mayTesting(ss, List.of());
            assertEquals(Set.of("a", "b"), may);
        }

        @Test
        void mustTestingNonexistentTraceReturnsEmpty() {
            var ss = build("&{a: end}");
            var must = CSPChecker.mustTesting(ss, List.of("z"));
            assertTrue(must.isEmpty());
        }

        @Test
        void mayTestingNonexistentTraceReturnsEmpty() {
            var ss = build("&{a: end}");
            var may = CSPChecker.mayTesting(ss, List.of("z"));
            assertTrue(may.isEmpty());
        }
    }

    // =========================================================================
    // Lattice determines failures
    // =========================================================================

    @Nested
    class LatticeDeterminesFailures {

        @Test
        void simpleBranchLatticeDeterminesFailures() {
            var ss = build("&{a: end, b: end}");
            assertTrue(CSPChecker.latticeDeterminesFailures(ss));
        }

        @Test
        void nestedBranchLatticeDeterminesFailures() {
            var ss = build("&{a: &{b: end}, c: end}");
            assertTrue(CSPChecker.latticeDeterminesFailures(ss));
        }

        @Test
        void selectLatticeDeterminesFailures() {
            var ss = build("+{ok: end, err: end}");
            assertTrue(CSPChecker.latticeDeterminesFailures(ss));
        }

        @Test
        void parallelLatticeDeterminesFailures() {
            var ss = build("(&{a: end} || &{b: end})");
            assertTrue(CSPChecker.latticeDeterminesFailures(ss));
        }

        @Test
        void recursiveLatticeDeterminesFailures() {
            var ss = build("rec X . &{a: X, b: end}");
            assertTrue(CSPChecker.latticeDeterminesFailures(ss));
        }
    }

    // =========================================================================
    // Failure analysis result
    // =========================================================================

    @Nested
    class FullFailureAnalysis {

        @Test
        void analysisResultFieldsPopulated() {
            var ss = build("&{a: end, b: end}");
            var result = CSPChecker.analyzeFailures(ss);
            assertNotNull(result.refusalMap());
            assertNotNull(result.acceptanceMap());
            assertTrue(result.deterministic());
            assertTrue(result.latticeDetermines());
            assertTrue(result.details().contains("Lattice"));
        }

        @Test
        void analysisAcceptanceMapCorrect() {
            var ss = build("&{a: end, b: end}");
            var result = CSPChecker.analyzeFailures(ss);
            assertEquals(Set.of("a", "b"), result.acceptanceMap().get(ss.top()));
            assertTrue(result.acceptanceMap().get(ss.bottom()).isEmpty());
        }
    }
}
