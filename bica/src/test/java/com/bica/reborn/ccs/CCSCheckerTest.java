package com.bica.reborn.ccs;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CCS module: translation, LTS construction,
 * bisimulation, pretty-printing, and full analysis pipeline.
 */
class CCSCheckerTest {

    // -----------------------------------------------------------------------
    // Translation: End
    // -----------------------------------------------------------------------

    @Test
    void translateEnd() {
        CCSProcess result = CCSChecker.sessionToCCS(new End());
        assertInstanceOf(Nil.class, result);
    }

    // -----------------------------------------------------------------------
    // Translation: Var
    // -----------------------------------------------------------------------

    @Test
    void translateVar() {
        CCSProcess result = CCSChecker.sessionToCCS(new Var("X"));
        assertInstanceOf(CCSVar.class, result);
        assertEquals("X", ((CCSVar) result).name());
    }

    // -----------------------------------------------------------------------
    // Translation: Branch (single choice)
    // -----------------------------------------------------------------------

    @Test
    void translateBranchSingle() {
        SessionType ast = new Branch(List.of(new Choice("open", new End())));
        CCSProcess result = CCSChecker.sessionToCCS(ast);
        // Single choice -> ActionPrefix, not Sum
        assertInstanceOf(ActionPrefix.class, result);
        ActionPrefix ap = (ActionPrefix) result;
        assertEquals("open", ap.action());
        assertInstanceOf(Nil.class, ap.cont());
    }

    // -----------------------------------------------------------------------
    // Translation: Branch (multiple choices)
    // -----------------------------------------------------------------------

    @Test
    void translateBranchMultiple() {
        SessionType ast = new Branch(List.of(
                new Choice("open", new End()),
                new Choice("close", new End())));
        CCSProcess result = CCSChecker.sessionToCCS(ast);
        assertInstanceOf(Sum.class, result);
        Sum sum = (Sum) result;
        assertEquals(2, sum.procs().size());
    }

    // -----------------------------------------------------------------------
    // Translation: Select (single choice)
    // -----------------------------------------------------------------------

    @Test
    void translateSelectSingle() {
        SessionType ast = new Select(List.of(new Choice("OK", new End())));
        CCSProcess result = CCSChecker.sessionToCCS(ast);
        assertInstanceOf(ActionPrefix.class, result);
        ActionPrefix ap = (ActionPrefix) result;
        assertEquals("τ_OK", ap.action());
    }

    // -----------------------------------------------------------------------
    // Translation: Select (multiple choices)
    // -----------------------------------------------------------------------

    @Test
    void translateSelectMultiple() {
        SessionType ast = new Select(List.of(
                new Choice("OK", new End()),
                new Choice("ERROR", new End())));
        CCSProcess result = CCSChecker.sessionToCCS(ast);
        assertInstanceOf(Sum.class, result);
        Sum sum = (Sum) result;
        assertEquals(2, sum.procs().size());
        // Both should be tau-prefixed
        for (CCSProcess p : sum.procs()) {
            assertInstanceOf(ActionPrefix.class, p);
            assertTrue(((ActionPrefix) p).action().startsWith("τ_"));
        }
    }

    // -----------------------------------------------------------------------
    // Translation: Parallel
    // -----------------------------------------------------------------------

    @Test
    void translateParallel() {
        SessionType ast = new Parallel(
                new Branch(List.of(new Choice("read", new End()))),
                new Branch(List.of(new Choice("write", new End()))));
        CCSProcess result = CCSChecker.sessionToCCS(ast);
        assertInstanceOf(CCSPar.class, result);
    }

    // -----------------------------------------------------------------------
    // Translation: Rec
    // -----------------------------------------------------------------------

    @Test
    void translateRec() {
        SessionType ast = new Rec("X", new Branch(List.of(
                new Choice("next", new Var("X")))));
        CCSProcess result = CCSChecker.sessionToCCS(ast);
        assertInstanceOf(CCSRec.class, result);
        CCSRec rec = (CCSRec) result;
        assertEquals("X", rec.var());
        assertInstanceOf(ActionPrefix.class, rec.body());
    }

    // -----------------------------------------------------------------------
    // LTS: End -> single state, no transitions
    // -----------------------------------------------------------------------

    @Test
    void ltsEnd() {
        LTS lts = CCSChecker.ccsToLTS(new Nil());
        assertEquals(1, lts.states().size());
        assertTrue(lts.transitions().isEmpty());
    }

    // -----------------------------------------------------------------------
    // LTS: Single action prefix -> 2 states, 1 transition
    // -----------------------------------------------------------------------

    @Test
    void ltsActionPrefix() {
        CCSProcess proc = new ActionPrefix("a", new Nil());
        LTS lts = CCSChecker.ccsToLTS(proc);
        assertEquals(2, lts.states().size());
        assertEquals(1, lts.transitions().size());
        assertEquals("a", lts.transitions().get(0).label());
    }

    // -----------------------------------------------------------------------
    // LTS: Sum of two prefixes -> 3 states, 2 transitions
    // -----------------------------------------------------------------------

    @Test
    void ltsSumTwoPrefixes() {
        CCSProcess proc = new Sum(List.of(
                new ActionPrefix("a", new Nil()),
                new ActionPrefix("b", new Nil())));
        LTS lts = CCSChecker.ccsToLTS(proc);
        // Initial + Nil (shared for both a and b targets)
        assertEquals(2, lts.states().size());
        assertEquals(2, lts.transitions().size());
    }

    // -----------------------------------------------------------------------
    // LTS: Parallel -> interleaving product
    // -----------------------------------------------------------------------

    @Test
    void ltsParallel() {
        CCSProcess proc = new CCSPar(
                new ActionPrefix("a", new Nil()),
                new ActionPrefix("b", new Nil()));
        LTS lts = CCSChecker.ccsToLTS(proc);
        // States: (a.0|b.0), (0|b.0), (a.0|0), (0|0)
        assertEquals(4, lts.states().size());
        // Transitions: a from (a.0|b.0) -> (0|b.0), b from (a.0|b.0) -> (a.0|0),
        //              b from (0|b.0) -> (0|0), a from (a.0|0) -> (0|0)
        assertEquals(4, lts.transitions().size());
    }

    // -----------------------------------------------------------------------
    // LTS: Branch translation state count
    // -----------------------------------------------------------------------

    @Test
    void ltsBranch() {
        SessionType ast = Parser.parse("&{open: &{read: end, write: end}}");
        CCSProcess ccs = CCSChecker.sessionToCCS(ast);
        LTS lts = CCSChecker.ccsToLTS(ccs);
        // open.{read.0 + write.0}: states are initial, after open, after read/write (shared 0)
        assertTrue(lts.states().size() >= 3);
        assertTrue(lts.transitions().size() >= 3);
    }

    // -----------------------------------------------------------------------
    // LTS: Recursive type is finite
    // -----------------------------------------------------------------------

    @Test
    void ltsRecursive() {
        SessionType ast = Parser.parse("rec X . &{next: X, done: end}");
        CCSProcess ccs = CCSChecker.sessionToCCS(ast);
        LTS lts = CCSChecker.ccsToLTS(ccs);
        // Should have at least 2 states (initial cycle + end)
        assertTrue(lts.states().size() >= 2);
        assertTrue(lts.transitions().size() >= 2);
    }

    // -----------------------------------------------------------------------
    // Pretty-printing: Nil
    // -----------------------------------------------------------------------

    @Test
    void prettyNil() {
        assertEquals("0", CCSChecker.prettyCCS(new Nil()));
    }

    // -----------------------------------------------------------------------
    // Pretty-printing: ActionPrefix
    // -----------------------------------------------------------------------

    @Test
    void prettyActionPrefix() {
        String s = CCSChecker.prettyCCS(new ActionPrefix("a", new Nil()));
        assertEquals("a . 0", s);
    }

    // -----------------------------------------------------------------------
    // Pretty-printing: Sum
    // -----------------------------------------------------------------------

    @Test
    void prettySum() {
        String s = CCSChecker.prettyCCS(new Sum(List.of(
                new ActionPrefix("a", new Nil()),
                new ActionPrefix("b", new Nil()))));
        assertEquals("a . 0 + b . 0", s);
    }

    // -----------------------------------------------------------------------
    // Pretty-printing: Par
    // -----------------------------------------------------------------------

    @Test
    void prettyPar() {
        String s = CCSChecker.prettyCCS(new CCSPar(
                new ActionPrefix("a", new Nil()),
                new ActionPrefix("b", new Nil())));
        assertEquals("a . 0 | b . 0", s);
    }

    // -----------------------------------------------------------------------
    // Pretty-printing: CCSRec
    // -----------------------------------------------------------------------

    @Test
    void prettyRec() {
        String s = CCSChecker.prettyCCS(new CCSRec("X",
                new ActionPrefix("a", new CCSVar("X"))));
        assertEquals("fix X . a . X", s);
    }

    // -----------------------------------------------------------------------
    // Pretty-printing: CCSVar
    // -----------------------------------------------------------------------

    @Test
    void prettyVar() {
        assertEquals("X", CCSChecker.prettyCCS(new CCSVar("X")));
    }

    // -----------------------------------------------------------------------
    // Pretty-printing: Restrict
    // -----------------------------------------------------------------------

    @Test
    void prettyRestrict() {
        String s = CCSChecker.prettyCCS(new Restrict(
                new ActionPrefix("a", new Nil()), Set.of("b")));
        assertEquals("(a . 0) \\ {b}", s);
    }

    // -----------------------------------------------------------------------
    // Bisimulation: identical Nil -> bisimilar
    // -----------------------------------------------------------------------

    @Test
    void bisimulationIdenticalNil() {
        LTS lts1 = CCSChecker.ccsToLTS(new Nil());
        LTS lts2 = CCSChecker.ccsToLTS(new Nil());
        assertTrue(CCSChecker.checkBisimulation(lts1, lts2));
    }

    // -----------------------------------------------------------------------
    // Bisimulation: same single action -> bisimilar
    // -----------------------------------------------------------------------

    @Test
    void bisimulationSameAction() {
        CCSProcess p = new ActionPrefix("a", new Nil());
        LTS lts1 = CCSChecker.ccsToLTS(p);
        LTS lts2 = CCSChecker.ccsToLTS(p);
        assertTrue(CCSChecker.checkBisimulation(lts1, lts2));
    }

    // -----------------------------------------------------------------------
    // Bisimulation: different actions -> not bisimilar
    // -----------------------------------------------------------------------

    @Test
    void bisimulationDifferentActions() {
        LTS lts1 = CCSChecker.ccsToLTS(new ActionPrefix("a", new Nil()));
        LTS lts2 = CCSChecker.ccsToLTS(new ActionPrefix("b", new Nil()));
        assertFalse(CCSChecker.checkBisimulation(lts1, lts2));
    }

    // -----------------------------------------------------------------------
    // Bisimulation: different branch counts -> not bisimilar
    // -----------------------------------------------------------------------

    @Test
    void bisimulationDifferentBranching() {
        CCSProcess p1 = new Sum(List.of(
                new ActionPrefix("a", new Nil()),
                new ActionPrefix("b", new Nil())));
        CCSProcess p2 = new ActionPrefix("a", new Nil());
        LTS lts1 = CCSChecker.ccsToLTS(p1);
        LTS lts2 = CCSChecker.ccsToLTS(p2);
        assertFalse(CCSChecker.checkBisimulation(lts1, lts2));
    }

    // -----------------------------------------------------------------------
    // Bisimulation: same type parsed twice -> bisimilar
    // -----------------------------------------------------------------------

    @Test
    void bisimulationSameTypeParsed() {
        SessionType ast1 = Parser.parse("&{open: &{read: end, write: end}}");
        SessionType ast2 = Parser.parse("&{open: &{read: end, write: end}}");
        LTS lts1 = CCSChecker.ccsToLTS(CCSChecker.sessionToCCS(ast1));
        LTS lts2 = CCSChecker.ccsToLTS(CCSChecker.sessionToCCS(ast2));
        assertTrue(CCSChecker.checkBisimulation(lts1, lts2));
    }

    // -----------------------------------------------------------------------
    // Bisimulation: different types -> not bisimilar
    // -----------------------------------------------------------------------

    @Test
    void bisimulationDifferentTypes() {
        SessionType ast1 = Parser.parse("&{open: end}");
        SessionType ast2 = Parser.parse("&{close: end}");
        LTS lts1 = CCSChecker.ccsToLTS(CCSChecker.sessionToCCS(ast1));
        LTS lts2 = CCSChecker.ccsToLTS(CCSChecker.sessionToCCS(ast2));
        assertFalse(CCSChecker.checkBisimulation(lts1, lts2));
    }

    // -----------------------------------------------------------------------
    // Full analysis: End
    // -----------------------------------------------------------------------

    @Test
    void analyzeEnd() {
        CCSResult result = CCSChecker.analyzeCCS(new End());
        assertNotNull(result.ccsProcess());
        assertNotNull(result.lts());
        assertEquals(1, result.numStates());
        assertEquals(0, result.numTransitions());
        assertTrue(result.bisimilarToStateSpace());
    }

    // -----------------------------------------------------------------------
    // Full analysis: simple branch
    // -----------------------------------------------------------------------

    @Test
    void analyzeSimpleBranch() {
        SessionType ast = Parser.parse("&{open: end}");
        CCSResult result = CCSChecker.analyzeCCS(ast);
        assertInstanceOf(ActionPrefix.class, result.ccsProcess());
        assertTrue(result.numStates() >= 2);
        assertTrue(result.numTransitions() >= 1);
        assertTrue(result.bisimilarToStateSpace());
    }

    // -----------------------------------------------------------------------
    // Full analysis: select
    // -----------------------------------------------------------------------

    @Test
    void analyzeSelect() {
        SessionType ast = Parser.parse("+{OK: end, ERROR: end}");
        CCSResult result = CCSChecker.analyzeCCS(ast);
        assertTrue(result.bisimilarToStateSpace());
    }

    // -----------------------------------------------------------------------
    // Normalize: unfolds top-level rec
    // -----------------------------------------------------------------------

    @Test
    void normalizeUnfoldsRec() {
        CCSProcess rec = new CCSRec("X", new ActionPrefix("a", new CCSVar("X")));
        CCSProcess norm = CCSChecker.normalize(rec);
        // Should unfold: a . (fix X . a . X)
        assertInstanceOf(ActionPrefix.class, norm);
    }

    // -----------------------------------------------------------------------
    // Normalize: flattens nested sum
    // -----------------------------------------------------------------------

    @Test
    void normalizeFlattenSum() {
        CCSProcess nested = new Sum(List.of(
                new Sum(List.of(
                        new ActionPrefix("a", new Nil()),
                        new ActionPrefix("b", new Nil()))),
                new ActionPrefix("c", new Nil())));
        CCSProcess norm = CCSChecker.normalize(nested);
        assertInstanceOf(Sum.class, norm);
        assertEquals(3, ((Sum) norm).procs().size());
    }

    // -----------------------------------------------------------------------
    // SOS: Nil has no transitions
    // -----------------------------------------------------------------------

    @Test
    void sosNil() {
        assertTrue(CCSChecker.sosStep(new Nil()).isEmpty());
    }

    // -----------------------------------------------------------------------
    // SOS: Prefix has one transition
    // -----------------------------------------------------------------------

    @Test
    void sosPrefix() {
        var steps = CCSChecker.sosStep(new ActionPrefix("a", new Nil()));
        assertEquals(1, steps.size());
        assertEquals("a", steps.get(0).getKey());
    }

    // -----------------------------------------------------------------------
    // Translation from parsed type preserves structure
    // -----------------------------------------------------------------------

    @Test
    void translationPreservesStructure() {
        SessionType ast = Parser.parse("&{a: +{OK: end, ERR: end}, b: end}");
        CCSProcess ccs = CCSChecker.sessionToCCS(ast);
        assertInstanceOf(Sum.class, ccs);
        Sum sum = (Sum) ccs;
        assertEquals(2, sum.procs().size());
        // First summand: a . (tau_OK.0 + tau_ERR.0)
        ActionPrefix first = (ActionPrefix) sum.procs().get(0);
        assertEquals("a", first.action());
        assertInstanceOf(Sum.class, first.cont());
    }

    // -----------------------------------------------------------------------
    // Sealed interface: all subtypes are records
    // -----------------------------------------------------------------------

    @Test
    void allSubtypesAreRecords() {
        assertTrue(CCSProcess.class.isSealed());
        Class<?>[] permitted = CCSProcess.class.getPermittedSubclasses();
        assertEquals(7, permitted.length);
        for (Class<?> c : permitted) {
            assertTrue(c.isRecord(), c.getSimpleName() + " should be a record");
        }
    }
}
