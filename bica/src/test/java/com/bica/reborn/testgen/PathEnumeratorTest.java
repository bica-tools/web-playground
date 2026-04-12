package com.bica.reborn.testgen;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.statespace.TransitionKind;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PathEnumeratorTest {

    private static StateSpace ss(String source) {
        return StateSpaceBuilder.build(Parser.parse(source));
    }

    private static TestGenConfig config(int maxRevisits, int maxPaths) {
        return new TestGenConfig("Obj", null, "obj", maxRevisits, maxPaths, ViolationStyle.DISABLED_ANNOTATION);
    }

    private static TestGenConfig defaultConfig() {
        return TestGenConfig.withDefaults("Obj");
    }

    // =========================================================================
    // Valid paths
    // =========================================================================

    @Nested
    class ValidPaths {

        @Test
        void endType_singleEmptyPath() {
            var paths = PathEnumerator.enumerateValidPaths(ss("end"), 2);
            assertEquals(1, paths.size());
            assertTrue(paths.getFirst().steps().isEmpty());
        }

        @Test
        void simpleChain_onePath() {
            var paths = PathEnumerator.enumerateValidPaths(ss("a . b . end"), 2);
            assertEquals(1, paths.size());
            assertEquals(List.of("a", "b"), paths.getFirst().labels());
        }

        @Test
        void branch_twoPaths() {
            var paths = PathEnumerator.enumerateValidPaths(ss("&{m: end, n: end}"), 2);
            assertEquals(2, paths.size());
            var labelSets = paths.stream().map(PathEnumerator.ValidPath::labels).collect(Collectors.toSet());
            assertTrue(labelSets.contains(List.of("m")));
            assertTrue(labelSets.contains(List.of("n")));
        }

        @Test
        void nestedBranch_fourPaths() {
            var paths = PathEnumerator.enumerateValidPaths(
                    ss("&{m: &{a: end, b: end}, n: &{c: end, d: end}}"), 2);
            assertEquals(4, paths.size());
        }

        @Test
        void recursion_maxRevisitsZero_onePath() {
            var paths = PathEnumerator.enumerateValidPaths(
                    ss("rec X . &{next: X, done: end}"), 0);
            assertEquals(1, paths.size());
            assertEquals(List.of("done"), paths.getFirst().labels());
        }

        @Test
        void recursion_maxRevisitsOne_twoPaths() {
            var paths = PathEnumerator.enumerateValidPaths(
                    ss("rec X . &{next: X, done: end}"), 1);
            assertEquals(2, paths.size());
            var labelSets = paths.stream().map(PathEnumerator.ValidPath::labels).collect(Collectors.toSet());
            assertTrue(labelSets.contains(List.of("done")));
            assertTrue(labelSets.contains(List.of("next", "done")));
        }

        @Test
        void recursion_maxRevisitsTwo_threePaths() {
            var paths = PathEnumerator.enumerateValidPaths(
                    ss("rec X . &{next: X, done: end}"), 2);
            assertEquals(3, paths.size());
        }

        @Test
        void select_onePath() {
            var paths = PathEnumerator.enumerateValidPaths(
                    ss("+{OK: end, ERR: end}"), 2);
            assertEquals(2, paths.size());
        }

        @Test
        void selectStepsHaveSelectionKind() {
            var paths = PathEnumerator.enumerateValidPaths(
                    ss("+{OK: end, ERR: end}"), 2);
            for (var path : paths) {
                for (var step : path.steps()) {
                    assertEquals(TransitionKind.SELECTION, step.kind(),
                            "Select steps should have SELECTION kind");
                }
            }
        }

        @Test
        void mixedProtocol_stepsHaveCorrectKinds() {
            var paths = PathEnumerator.enumerateValidPaths(
                    ss("m . +{OK: end, ERR: end}"), 2);
            for (var path : paths) {
                assertEquals(TransitionKind.METHOD, path.steps().getFirst().kind(),
                        "First step 'm' should be METHOD");
                assertEquals(TransitionKind.SELECTION, path.steps().getLast().kind(),
                        "Second step (OK/ERR) should be SELECTION");
            }
        }

        @Test
        void chainWithBranch() {
            var paths = PathEnumerator.enumerateValidPaths(
                    ss("open . &{read: close . end, write: close . end}"), 2);
            assertEquals(2, paths.size());
            for (var path : paths) {
                assertEquals("open", path.labels().getFirst());
                assertEquals("close", path.labels().getLast());
            }
        }

        @Test
        void pathLabelsHelper() {
            var paths = PathEnumerator.enumerateValidPaths(ss("a . b . c . end"), 2);
            assertEquals(1, paths.size());
            assertEquals(List.of("a", "b", "c"), paths.getFirst().labels());
        }

        @Test
        void maxPathsTruncation() {
            // Many paths: branch with 4 options
            var result = PathEnumerator.enumerate(
                    ss("&{a: end, b: end, c: end, d: end}"),
                    new TestGenConfig("Obj", null, "obj", 2, 2, ViolationStyle.DISABLED_ANNOTATION));
            assertEquals(2, result.validPaths().size());
            assertTrue(result.truncated());
        }

        @Test
        void noTruncationWhenUnderLimit() {
            var result = PathEnumerator.enumerate(ss("a . end"), defaultConfig());
            assertFalse(result.truncated());
        }
    }

    // =========================================================================
    // Violations
    // =========================================================================

    @Nested
    class Violations {

        @Test
        void endType_noViolations() {
            var violations = PathEnumerator.enumerateViolations(ss("end"));
            assertTrue(violations.isEmpty());
        }

        @Test
        void simpleChain_violationsAtEachState() {
            // a . b . end → top has 'a' enabled, middle has 'b' enabled
            // allLabels = {a, b}
            // At top: disabled = {b} → 1 violation
            // At middle: disabled = {a} → 1 violation
            // bottom is skipped
            var violations = PathEnumerator.enumerateViolations(ss("a . b . end"));
            assertEquals(2, violations.size());
        }

        @Test
        void branch_violationsIncludeDisabledAtAfterChoice() {
            // &{m: a.end, n: b.end}
            // allLabels = {m, n, a, b}
            // At top: enabled={m,n}, disabled={a,b} → 2 violations
            var violations = PathEnumerator.enumerateViolations(
                    ss("&{m: a.end, n: b.end}"));
            assertTrue(violations.size() >= 2);
        }

        @Test
        void violationHasCorrectPrefix() {
            var violations = PathEnumerator.enumerateViolations(ss("a . b . end"));
            // Find violation for 'a' at the middle state (after taking 'a')
            var vForA = violations.stream()
                    .filter(v -> v.disabledMethod().equals("a"))
                    .findFirst();
            assertTrue(vForA.isPresent());
            assertEquals(List.of("a"), vForA.get().prefixLabels());
        }

        @Test
        void violationEnabledMethods() {
            var violations = PathEnumerator.enumerateViolations(ss("a . b . end"));
            var vAtTop = violations.stream()
                    .filter(v -> v.prefixPath().isEmpty())
                    .findFirst();
            assertTrue(vAtTop.isPresent());
            assertEquals(Set.of("a"), vAtTop.get().enabledMethods());
        }

        @Test
        void bottomStateIsSkipped() {
            // No violations at bottom state
            var violations = PathEnumerator.enumerateViolations(ss("a . end"));
            for (var v : violations) {
                assertNotEquals(ss("a . end").bottom(), v.state());
            }
        }

        @Test
        void singleMethodNoViolations() {
            // Only one label 'a', at top it's enabled, bottom is skipped
            var violations = PathEnumerator.enumerateViolations(ss("a . end"));
            assertTrue(violations.isEmpty());
        }

        @Test
        void recursiveType_violationsExist() {
            var violations = PathEnumerator.enumerateViolations(
                    ss("rec X . &{next: X, done: end}"));
            // allLabels = {next, done}, at top both are enabled → no violations there
            // This is a simple recursive type, all labels enabled at the recursive state
            assertTrue(violations.isEmpty());
        }

        @Test
        void selectOnly_noViolations() {
            // +{OK: end, ERR: end} → no method labels → no violations
            var violations = PathEnumerator.enumerateViolations(
                    ss("+{OK: end, ERR: end}"));
            assertTrue(violations.isEmpty(),
                    "Pure selection protocols should have no violations (no callable methods)");
        }

        @Test
        void mixedProtocol_selectionLabelsNotViolations() {
            // m . +{OK: end, ERR: end}
            // allMethodLabels = {m}
            // At top: enabledMethods = {m}, disabled = {} → 0 violations
            // At select state: pure selection, skipped entirely
            var violations = PathEnumerator.enumerateViolations(
                    ss("m . +{OK: end, ERR: end}"));
            assertTrue(violations.isEmpty(),
                    "No violations: top has m enabled, select state is pure selection (skipped)");
        }
    }

    // =========================================================================
    // Incomplete prefixes
    // =========================================================================

    @Nested
    class IncompletePrefixes {

        @Test
        void endType_noIncomplete() {
            var validPaths = PathEnumerator.enumerateValidPaths(ss("end"), 2);
            var incomplete = PathEnumerator.enumerateIncompletePrefixes(ss("end"), validPaths);
            assertTrue(incomplete.isEmpty());
        }

        @Test
        void simpleChain_oneIncomplete() {
            var space = ss("a . b . end");
            var validPaths = PathEnumerator.enumerateValidPaths(space, 2);
            var incomplete = PathEnumerator.enumerateIncompletePrefixes(space, validPaths);
            assertEquals(1, incomplete.size());
            assertEquals(List.of("a"), incomplete.getFirst().labels());
        }

        @Test
        void longerChain_multipleIncomplete() {
            var space = ss("a . b . c . end");
            var validPaths = PathEnumerator.enumerateValidPaths(space, 2);
            var incomplete = PathEnumerator.enumerateIncompletePrefixes(space, validPaths);
            assertEquals(2, incomplete.size());
            var labelSets = incomplete.stream()
                    .map(PathEnumerator.IncompletePrefix::labels)
                    .collect(Collectors.toSet());
            assertTrue(labelSets.contains(List.of("a")));
            assertTrue(labelSets.contains(List.of("a", "b")));
        }

        @Test
        void branchDeduplicates() {
            // &{m: a.end, n: a.end} → paths [m,a] and [n,a]
            // prefixes: [m] and [n] — unique label sequences
            var space = ss("&{m: a.end, n: a.end}");
            var validPaths = PathEnumerator.enumerateValidPaths(space, 2);
            var incomplete = PathEnumerator.enumerateIncompletePrefixes(space, validPaths);
            // Each path has one proper prefix: [m] and [n]
            var labelSets = incomplete.stream()
                    .map(PathEnumerator.IncompletePrefix::labels)
                    .collect(Collectors.toSet());
            // No duplicate label sequences
            assertEquals(incomplete.size(), labelSets.size());
        }

        @Test
        void incompleteShowsRemainingMethods() {
            var space = ss("open . &{read: close . end, write: close . end}");
            var validPaths = PathEnumerator.enumerateValidPaths(space, 2);
            var incomplete = PathEnumerator.enumerateIncompletePrefixes(space, validPaths);
            // After "open", remaining methods are {read, write}
            var afterOpen = incomplete.stream()
                    .filter(p -> p.labels().equals(List.of("open")))
                    .findFirst();
            assertTrue(afterOpen.isPresent());
            assertEquals(Set.of("read", "write"), afterOpen.get().remainingMethods());
        }

        @Test
        void singleMethodChain_oneIncomplete() {
            var space = ss("a . end");
            var validPaths = PathEnumerator.enumerateValidPaths(space, 2);
            // Valid path: [a]. Only proper prefix is [] (empty), which has length 0.
            // But we start from length 1, so no prefixes.
            var incomplete = PathEnumerator.enumerateIncompletePrefixes(space, validPaths);
            assertTrue(incomplete.isEmpty());
        }
    }

    // =========================================================================
    // Client programs
    // =========================================================================

    @Nested
    class ClientPrograms {

        @Test
        void endType_singleTerminal() {
            var result = PathEnumerator.enumerateClientPrograms(ss("end"), 2, 100);
            assertEquals(1, result.programs().size());
            assertInstanceOf(PathEnumerator.ClientProgram.Terminal.class, result.programs().getFirst());
            assertFalse(result.truncated());
        }

        @Test
        void simpleChain_oneMethodCallChain() {
            var result = PathEnumerator.enumerateClientPrograms(ss("a . b . end"), 2, 100);
            assertEquals(1, result.programs().size());
            var p = result.programs().getFirst();
            assertInstanceOf(PathEnumerator.ClientProgram.MethodCall.class, p);
            var mc = (PathEnumerator.ClientProgram.MethodCall) p;
            assertEquals("a", mc.label());
            assertInstanceOf(PathEnumerator.ClientProgram.MethodCall.class, mc.next());
            var mc2 = (PathEnumerator.ClientProgram.MethodCall) mc.next();
            assertEquals("b", mc2.label());
            assertInstanceOf(PathEnumerator.ClientProgram.Terminal.class, mc2.next());
        }

        @Test
        void branch_twoProgramsFromClientChoice() {
            var result = PathEnumerator.enumerateClientPrograms(ss("&{m: end, n: end}"), 2, 100);
            assertEquals(2, result.programs().size());
            for (var p : result.programs()) {
                assertInstanceOf(PathEnumerator.ClientProgram.MethodCall.class, p);
                var mc = (PathEnumerator.ClientProgram.MethodCall) p;
                assertTrue(mc.label().equals("m") || mc.label().equals("n"));
                assertInstanceOf(PathEnumerator.ClientProgram.Terminal.class, mc.next());
            }
        }

        @Test
        void simpleSelection_oneSelectionSwitch() {
            // m . +{A: end, B: end} → 1 SelectionSwitch program
            var result = PathEnumerator.enumerateClientPrograms(
                    ss("m . +{A: end, B: end}"), 2, 100);
            assertEquals(1, result.programs().size());
            var p = result.programs().getFirst();
            assertInstanceOf(PathEnumerator.ClientProgram.SelectionSwitch.class, p);
            var sw = (PathEnumerator.ClientProgram.SelectionSwitch) p;
            assertEquals("m", sw.methodLabel());
            assertEquals(2, sw.branches().size());
            assertTrue(sw.branches().containsKey("A"));
            assertTrue(sw.branches().containsKey("B"));
            assertInstanceOf(PathEnumerator.ClientProgram.Terminal.class, sw.branches().get("A"));
            assertInstanceOf(PathEnumerator.ClientProgram.Terminal.class, sw.branches().get("B"));
        }

        @Test
        void selectionWithSubBranches_zipMatch() {
            // m . +{A: &{x: end, y: end}, B: end} → 2 programs (A has 2 options, B cycles 1)
            var result = PathEnumerator.enumerateClientPrograms(
                    ss("m . +{A: &{x: end, y: end}, B: end}"), 2, 100);
            assertEquals(2, result.programs().size());
            for (var p : result.programs()) {
                assertInstanceOf(PathEnumerator.ClientProgram.SelectionSwitch.class, p);
                var sw = (PathEnumerator.ClientProgram.SelectionSwitch) p;
                assertEquals("m", sw.methodLabel());
                // A arm differs between the two programs
                assertInstanceOf(PathEnumerator.ClientProgram.MethodCall.class, sw.branches().get("A"));
                // B arm is always Terminal (cycled)
                assertInstanceOf(PathEnumerator.ClientProgram.Terminal.class, sw.branches().get("B"));
            }
        }

        @Test
        void recursiveSelection_nestedSwitches() {
            // rec X . m . +{A: X, B: end} → nested SelectionSwitch bounded by maxRevisits
            var result = PathEnumerator.enumerateClientPrograms(
                    ss("rec X . m . +{A: X, B: end}"), 1, 100);
            assertEquals(1, result.programs().size());
            var p = result.programs().getFirst();
            assertInstanceOf(PathEnumerator.ClientProgram.SelectionSwitch.class, p);
            var sw = (PathEnumerator.ClientProgram.SelectionSwitch) p;
            assertEquals("m", sw.methodLabel());
            // A branch is a nested SelectionSwitch (recursive)
            assertInstanceOf(PathEnumerator.ClientProgram.SelectionSwitch.class, sw.branches().get("A"));
            // B branch is Terminal
            assertInstanceOf(PathEnumerator.ClientProgram.Terminal.class, sw.branches().get("B"));
        }

        @Test
        void topLevelSelection_noSwitch() {
            // +{A: end, B: end} → no preceding method, each branch is a separate program
            var result = PathEnumerator.enumerateClientPrograms(
                    ss("+{A: end, B: end}"), 2, 100);
            assertEquals(2, result.programs().size());
            for (var p : result.programs()) {
                assertInstanceOf(PathEnumerator.ClientProgram.Terminal.class, p);
            }
        }

        @Test
        void maxPathsTruncation() {
            // 4-way branch → 4 programs, truncate at 2
            var result = PathEnumerator.enumerateClientPrograms(
                    ss("&{a: end, b: end, c: end, d: end}"), 2, 2);
            assertEquals(2, result.programs().size());
            assertTrue(result.truncated());
        }

        @Test
        void fileHandleProtocol_noPureSelection() {
            // No selections → each valid path is a MethodCall chain
            var result = PathEnumerator.enumerateClientPrograms(
                    ss("open . &{read: close . end, write: close . end}"), 2, 100);
            assertEquals(2, result.programs().size());
            for (var p : result.programs()) {
                assertInstanceOf(PathEnumerator.ClientProgram.MethodCall.class, p);
            }
        }

        @Test
        void nestedSelectionProducesNestedSwitch() {
            // m . +{A: n . +{X: end, Y: end}, B: end} → 1 program, nested switch in A
            var result = PathEnumerator.enumerateClientPrograms(
                    ss("m . +{A: n . +{X: end, Y: end}, B: end}"), 2, 100);
            assertEquals(1, result.programs().size());
            var sw = (PathEnumerator.ClientProgram.SelectionSwitch) result.programs().getFirst();
            assertEquals("m", sw.methodLabel());
            // A arm should be a SelectionSwitch for n
            assertInstanceOf(PathEnumerator.ClientProgram.SelectionSwitch.class, sw.branches().get("A"));
            var inner = (PathEnumerator.ClientProgram.SelectionSwitch) sw.branches().get("A");
            assertEquals("n", inner.methodLabel());
            assertEquals(2, inner.branches().size());
        }

        @Test
        void recursiveSelection_maxRevisitsZero() {
            // rec X . m . +{A: X, B: end} with maxRevisits=0
            // First visit: m → select, A → revisit X (count=1 > 0 → dead end → Terminal)
            var result = PathEnumerator.enumerateClientPrograms(
                    ss("rec X . m . +{A: X, B: end}"), 0, 100);
            assertEquals(1, result.programs().size());
            var sw = (PathEnumerator.ClientProgram.SelectionSwitch) result.programs().getFirst();
            // Both A and B should be Terminal (A is dead end → Terminal fallback)
            assertInstanceOf(PathEnumerator.ClientProgram.Terminal.class, sw.branches().get("A"));
            assertInstanceOf(PathEnumerator.ClientProgram.Terminal.class, sw.branches().get("B"));
        }

        @Test
        void methodFollowedBySelection_thenBranch() {
            // a . +{X: &{p: end, q: end}, Y: end}
            // → 2 programs: switch with X→p and X→q
            var result = PathEnumerator.enumerateClientPrograms(
                    ss("a . +{X: &{p: end, q: end}, Y: end}"), 2, 100);
            assertEquals(2, result.programs().size());
            for (var p : result.programs()) {
                var sw = (PathEnumerator.ClientProgram.SelectionSwitch) p;
                assertEquals("a", sw.methodLabel());
                assertInstanceOf(PathEnumerator.ClientProgram.MethodCall.class, sw.branches().get("X"));
                assertInstanceOf(PathEnumerator.ClientProgram.Terminal.class, sw.branches().get("Y"));
            }
        }
    }

    // =========================================================================
    // Full enumeration
    // =========================================================================

    @Nested
    class FullEnumeration {

        @Test
        void endType() {
            var result = PathEnumerator.enumerate(ss("end"), defaultConfig());
            assertEquals(1, result.validPaths().size());
            assertTrue(result.violations().isEmpty());
            assertTrue(result.incompletePrefixes().isEmpty());
            assertFalse(result.truncated());
        }

        @Test
        void fileHandleProtocol() {
            var result = PathEnumerator.enumerate(
                    ss("open . &{read: close . end, write: close . end}"),
                    defaultConfig());
            assertEquals(2, result.validPaths().size());
            assertFalse(result.violations().isEmpty());
            assertFalse(result.incompletePrefixes().isEmpty());
        }

        @Test
        void recursiveProtocol() {
            var result = PathEnumerator.enumerate(
                    ss("rec X . &{next: X, done: end}"),
                    config(2, 100));
            assertEquals(3, result.validPaths().size());
        }

        @Test
        void selectProtocol() {
            var result = PathEnumerator.enumerate(
                    ss("+{OK: a.end, ERR: end}"),
                    defaultConfig());
            assertEquals(2, result.validPaths().size());
        }
    }
}
