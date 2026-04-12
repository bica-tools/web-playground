package com.bica.reborn.equivalence;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the process equivalence coincidence checker.
 *
 * <p>Verifies that CCS bisimulation, CSP trace equivalence, and CSP failure
 * equivalence coincide on deterministic session type state spaces.
 */
class ProcessEquivalenceCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Self-equivalence: identical state spaces must be equivalent
    // -----------------------------------------------------------------------

    @Test
    void selfEquivalenceEnd() {
        var ss = build("end");
        var result = ProcessEquivalenceChecker.checkAllEquivalences(ss, ss);
        assertTrue(result.bisimilar());
        assertTrue(result.traceEquivalent());
        assertTrue(result.failureEquivalent());
        assertTrue(result.allAgree());
    }

    @Test
    void selfEquivalenceSimpleBranch() {
        var ss = build("&{a: end, b: end}");
        var result = ProcessEquivalenceChecker.checkAllEquivalences(ss, ss);
        assertTrue(result.bisimilar());
        assertTrue(result.traceEquivalent());
        assertTrue(result.failureEquivalent());
        assertTrue(result.allAgree());
    }

    @Test
    void selfEquivalenceSelection() {
        var ss = build("+{OK: end, ERR: end}");
        var result = ProcessEquivalenceChecker.checkAllEquivalences(ss, ss);
        assertTrue(result.allAgree());
        assertTrue(result.bisimilar());
    }

    @Test
    void selfEquivalenceRecursive() {
        var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
        var result = ProcessEquivalenceChecker.checkAllEquivalences(ss, ss);
        assertTrue(result.allAgree());
        assertTrue(result.bisimilar());
    }

    @Test
    void selfEquivalenceNested() {
        var ss = build("&{a: &{b: end, c: end}, d: end}");
        var result = ProcessEquivalenceChecker.checkAllEquivalences(ss, ss);
        assertTrue(result.allAgree());
        assertTrue(result.bisimilar());
    }

    // -----------------------------------------------------------------------
    // Non-equivalence: different state spaces
    // -----------------------------------------------------------------------

    @Test
    void nonEquivalentDifferentBranches() {
        var ss1 = build("&{a: end, b: end}");
        var ss2 = build("&{a: end, c: end}");
        var result = ProcessEquivalenceChecker.checkAllEquivalences(ss1, ss2);
        assertFalse(result.bisimilar());
        assertFalse(result.traceEquivalent());
        assertFalse(result.failureEquivalent());
        assertTrue(result.allAgree());
    }

    @Test
    void nonEquivalentDifferentDepths() {
        var ss1 = build("&{a: end}");
        var ss2 = build("&{a: &{b: end}}");
        var result = ProcessEquivalenceChecker.checkAllEquivalences(ss1, ss2);
        assertFalse(result.bisimilar());
        assertTrue(result.allAgree());
    }

    @Test
    void nonEquivalentBranchVsSelection() {
        var ss1 = build("&{a: end, b: end}");
        var ss2 = build("+{a: end, b: end}");
        var result = ProcessEquivalenceChecker.checkAllEquivalences(ss1, ss2);
        // Bisimulation converts selection labels to tau_ prefix, so LTS actions differ
        assertFalse(result.bisimilar());
        // CSP trace/failure check uses raw state space labels (same for both),
        // so they appear trace-equivalent and failure-equivalent at the CSP level
        assertTrue(result.traceEquivalent());
        assertTrue(result.failureEquivalent());
        // This is a case where the three do NOT agree (bisim differs from trace/failure)
        assertFalse(result.allAgree());
    }

    // -----------------------------------------------------------------------
    // Coincidence check
    // -----------------------------------------------------------------------

    @Test
    void coincidenceSameSpaces() {
        var ss = build("&{open: &{read: end, write: end}}");
        assertTrue(ProcessEquivalenceChecker.checkCoincidence(ss, ss));
    }

    @Test
    void coincidenceDifferentSpaces() {
        var ss1 = build("&{a: end}");
        var ss2 = build("&{b: end}");
        assertTrue(ProcessEquivalenceChecker.checkCoincidence(ss1, ss2));
    }

    @Test
    void coincidenceWithDepth() {
        var ss = build("&{a: end, b: end}");
        assertTrue(ProcessEquivalenceChecker.checkCoincidence(ss, ss, 5));
    }

    // -----------------------------------------------------------------------
    // Determinism reporting
    // -----------------------------------------------------------------------

    @Test
    void determinismReportedCorrectly() {
        var ss = build("&{a: end, b: end}");
        var result = ProcessEquivalenceChecker.checkAllEquivalences(ss, ss);
        assertTrue(result.ss1Deterministic());
        assertTrue(result.ss2Deterministic());
    }

    // -----------------------------------------------------------------------
    // Details string
    // -----------------------------------------------------------------------

    @Test
    void detailsContainsAgreeWhenAllMatch() {
        var ss = build("end");
        var result = ProcessEquivalenceChecker.checkAllEquivalences(ss, ss);
        assertTrue(result.details().contains("agree"));
    }

    // -----------------------------------------------------------------------
    // Sub-state-space construction
    // -----------------------------------------------------------------------

    @Test
    void subStateSpaceFromTop() {
        var ss = build("&{a: end, b: end}");
        var sub = ProcessEquivalenceChecker.subStateSpace(ss, ss.top());
        assertTrue(sub.isPresent());
        assertEquals(ss.states().size(), sub.get().states().size());
    }

    @Test
    void subStateSpaceFromBottom() {
        var ss = build("&{a: end, b: end}");
        var sub = ProcessEquivalenceChecker.subStateSpace(ss, ss.bottom());
        assertTrue(sub.isPresent());
        assertEquals(1, sub.get().states().size());
    }

    // -----------------------------------------------------------------------
    // Structurally equivalent but independently built
    // -----------------------------------------------------------------------

    @Test
    void equivalentIndependentlyBuilt() {
        var ss1 = build("&{a: end, b: end}");
        var ss2 = build("&{a: end, b: end}");
        var result = ProcessEquivalenceChecker.checkAllEquivalences(ss1, ss2);
        assertTrue(result.bisimilar());
        assertTrue(result.traceEquivalent());
        assertTrue(result.failureEquivalent());
        assertTrue(result.allAgree());
    }
}
