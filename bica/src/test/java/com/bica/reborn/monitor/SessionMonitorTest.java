package com.bica.reborn.monitor;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SessionMonitor} — runtime protocol conformance monitor.
 */
class SessionMonitorTest {

    // -- helpers ---------------------------------------------------------------

    private static SessionMonitor monitor(String sessionType) {
        StateSpace ss = StateSpaceBuilder.build(Parser.parse(sessionType));
        return new SessionMonitor(ss);
    }

    // =========================================================================
    // Basic state tracking
    // =========================================================================

    @Nested
    class BasicStateTracking {

        @Test
        void initialStateIsTop() {
            var m = monitor("&{a: end}");
            assertEquals(m.stateSpace().top(), m.currentState());
        }

        @Test
        void notCompleteAtStart() {
            var m = monitor("&{a: end}");
            assertFalse(m.isComplete());
        }

        @Test
        void notViolatedAtStart() {
            var m = monitor("&{a: end}");
            assertFalse(m.isViolation());
        }

        @Test
        void emptyTraceAtStart() {
            var m = monitor("&{a: end}");
            assertTrue(m.trace().isEmpty());
        }
    }

    // =========================================================================
    // Valid transitions
    // =========================================================================

    @Nested
    class ValidTransitions {

        @Test
        void singleTransitionToEnd() {
            var m = monitor("&{a: end}");
            MonitorResult r = m.step("a");
            assertTrue(r.success());
            assertTrue(m.isComplete());
        }

        @Test
        void resultContainsCorrectFromState() {
            var m = monitor("&{a: end}");
            int top = m.currentState();
            MonitorResult r = m.step("a");
            assertEquals(top, r.fromState());
        }

        @Test
        void resultContainsCorrectToState() {
            var m = monitor("&{a: end}");
            MonitorResult r = m.step("a");
            assertEquals(m.stateSpace().bottom(), r.toState());
        }

        @Test
        void twoStepPath() {
            var m = monitor("&{a: &{b: end}}");
            MonitorResult r1 = m.step("a");
            assertTrue(r1.success());
            assertFalse(m.isComplete());

            MonitorResult r2 = m.step("b");
            assertTrue(r2.success());
            assertTrue(m.isComplete());
        }

        @Test
        void branchChoice() {
            var m = monitor("&{a: end, b: end}");
            MonitorResult r = m.step("b");
            assertTrue(r.success());
            assertTrue(m.isComplete());
        }

        @Test
        void selectionTransition() {
            var m = monitor("+{ok: end, err: end}");
            MonitorResult r = m.step("ok");
            assertTrue(r.success());
            assertTrue(m.isComplete());
        }
    }

    // =========================================================================
    // Violations
    // =========================================================================

    @Nested
    class Violations {

        @Test
        void invalidMethodCausesViolation() {
            var m = monitor("&{a: end}");
            MonitorResult r = m.step("b");
            assertFalse(r.success());
            assertTrue(m.isViolation());
        }

        @Test
        void violationDoesNotAdvanceState() {
            var m = monitor("&{a: end}");
            int before = m.currentState();
            m.step("b");
            assertEquals(before, m.currentState());
        }

        @Test
        void cannotTransitionAfterViolation() {
            var m = monitor("&{a: end}");
            m.step("b"); // violation
            MonitorResult r = m.step("a"); // even valid method fails
            assertFalse(r.success());
        }

        @Test
        void transitionAfterEnd() {
            var m = monitor("&{a: end}");
            m.step("a"); // complete
            assertTrue(m.isComplete());
            MonitorResult r = m.step("a");
            assertFalse(r.success());
            assertTrue(m.isViolation());
        }

        @Test
        void violationResultShowsEnabledMethods() {
            var m = monitor("&{a: end, b: end}");
            MonitorResult r = m.step("c"); // not enabled
            assertFalse(r.success());
            assertTrue(r.wasEnabled().contains("a"));
            assertTrue(r.wasEnabled().contains("b"));
        }
    }

    // =========================================================================
    // Reset
    // =========================================================================

    @Nested
    class Reset {

        @Test
        void resetGoesBackToInitialState() {
            var m = monitor("&{a: &{b: end}}");
            m.step("a");
            m.reset();
            assertEquals(m.stateSpace().top(), m.currentState());
        }

        @Test
        void resetClearsViolation() {
            var m = monitor("&{a: end}");
            m.step("bad");
            assertTrue(m.isViolation());
            m.reset();
            assertFalse(m.isViolation());
        }

        @Test
        void resetClearsTrace() {
            var m = monitor("&{a: end}");
            m.step("a");
            assertFalse(m.trace().isEmpty());
            m.reset();
            assertTrue(m.trace().isEmpty());
        }

        @Test
        void canContinueAfterReset() {
            var m = monitor("&{a: end}");
            m.step("a");
            assertTrue(m.isComplete());
            m.reset();
            assertFalse(m.isComplete());
            MonitorResult r = m.step("a");
            assertTrue(r.success());
            assertTrue(m.isComplete());
        }
    }

    // =========================================================================
    // Trace
    // =========================================================================

    @Nested
    class Trace {

        @Test
        void traceRecordsSuccessfulTransitions() {
            var m = monitor("&{a: &{b: end}}");
            m.step("a");
            m.step("b");
            assertEquals(2, m.trace().size());
            assertEquals("a", m.trace().get(0));
            assertEquals("b", m.trace().get(1));
        }

        @Test
        void traceDoesNotRecordViolations() {
            var m = monitor("&{a: end}");
            m.step("bad");
            assertTrue(m.trace().isEmpty());
        }

        @Test
        void traceIsImmutableCopy() {
            var m = monitor("&{a: end}");
            m.step("a");
            var trace = m.trace();
            assertThrows(UnsupportedOperationException.class, () -> trace.add("x"));
        }
    }

    // =========================================================================
    // Enabled methods
    // =========================================================================

    @Nested
    class EnabledMethods {

        @Test
        void enabledMethodsAtStart() {
            var m = monitor("&{a: end, b: end}");
            Set<String> enabled = m.enabledMethods();
            assertTrue(enabled.contains("a"));
            assertTrue(enabled.contains("b"));
            assertEquals(2, enabled.size());
        }

        @Test
        void enabledMethodsAfterTransition() {
            var m = monitor("&{a: &{b: end, c: end}}");
            m.step("a");
            Set<String> enabled = m.enabledMethods();
            assertTrue(enabled.contains("b"));
            assertTrue(enabled.contains("c"));
            assertFalse(enabled.contains("a"));
        }

        @Test
        void noEnabledMethodsAtEnd() {
            var m = monitor("&{a: end}");
            m.step("a");
            assertTrue(m.enabledMethods().isEmpty());
        }

        @Test
        void noEnabledMethodsAfterViolation() {
            var m = monitor("&{a: end}");
            m.step("bad");
            assertTrue(m.enabledMethods().isEmpty());
        }
    }

    // =========================================================================
    // Completeness
    // =========================================================================

    @Nested
    class Completeness {

        @Test
        void endTypeIsImmediatelyComplete() {
            var m = monitor("end");
            assertTrue(m.isComplete());
        }

        @Test
        void completeAfterFullPath() {
            var m = monitor("&{open: &{read: end, close: end}}");
            m.step("open");
            assertFalse(m.isComplete());
            m.step("read");
            assertTrue(m.isComplete());
        }
    }

    // =========================================================================
    // Recursive types
    // =========================================================================

    @Nested
    class RecursiveTypes {

        @Test
        void recursiveTypeCanLoop() {
            var m = monitor("rec X . &{next: X, done: end}");
            m.step("next");
            assertFalse(m.isComplete());
            m.step("next");
            assertFalse(m.isComplete());
            m.step("done");
            assertTrue(m.isComplete());
        }

        @Test
        void iteratorPattern() {
            var m = monitor("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            m.step("hasNext");
            m.step("TRUE");
            m.step("next");
            // back to start
            m.step("hasNext");
            m.step("FALSE");
            assertTrue(m.isComplete());
        }
    }
}
