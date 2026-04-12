package com.bica.reborn.invariants;

import com.bica.reborn.ast.*;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for session-type-specific invariants.
 */
class STInvariantsCheckerTest {

    private SessionType parse(String type) {
        return Parser.parse(type);
    }

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Branch counting
    // -----------------------------------------------------------------------

    @Test
    void countBranchesEnd() {
        assertEquals(0, STInvariantsChecker.countBranches(new End()));
    }

    @Test
    void countBranchesSimple() {
        assertEquals(1, STInvariantsChecker.countBranches(parse("&{a: end}")));
    }

    @Test
    void countBranchesNested() {
        assertEquals(2, STInvariantsChecker.countBranches(parse("&{a: &{b: end}}")));
    }

    @Test
    void countBranchesInSelect() {
        // +{ok: &{a: end}} has 0 branch at select level, 1 branch inside
        assertEquals(1, STInvariantsChecker.countBranches(parse("+{ok: &{a: end}}")));
    }

    // -----------------------------------------------------------------------
    // Select counting
    // -----------------------------------------------------------------------

    @Test
    void countSelectsEnd() {
        assertEquals(0, STInvariantsChecker.countSelects(new End()));
    }

    @Test
    void countSelectsSimple() {
        assertEquals(1, STInvariantsChecker.countSelects(parse("+{ok: end}")));
    }

    @Test
    void countSelectsInsideBranch() {
        assertEquals(1, STInvariantsChecker.countSelects(parse("&{a: +{ok: end}}")));
    }

    // -----------------------------------------------------------------------
    // Parallel counting
    // -----------------------------------------------------------------------

    @Test
    void countParallelsNone() {
        assertEquals(0, STInvariantsChecker.countParallels(parse("&{a: end}")));
    }

    @Test
    void countParallelsOne() {
        var ast = new Parallel(
                new Branch(List.of(new Branch.Choice("a", new End()))),
                new Branch(List.of(new Branch.Choice("b", new End()))));
        assertEquals(1, STInvariantsChecker.countParallels(ast));
    }

    // -----------------------------------------------------------------------
    // Rec counting
    // -----------------------------------------------------------------------

    @Test
    void countRecsNone() {
        assertEquals(0, STInvariantsChecker.countRecs(parse("&{a: end}")));
    }

    @Test
    void countRecsOne() {
        assertEquals(1, STInvariantsChecker.countRecs(parse("rec X . &{a: X, b: end}")));
    }

    // -----------------------------------------------------------------------
    // Max branch width
    // -----------------------------------------------------------------------

    @Test
    void maxBranchWidthEnd() {
        assertEquals(0, STInvariantsChecker.maxBranchWidth(new End()));
    }

    @Test
    void maxBranchWidthTwo() {
        assertEquals(2, STInvariantsChecker.maxBranchWidth(parse("&{a: end, b: end}")));
    }

    @Test
    void maxBranchWidthNested() {
        // Outer has 1 choice, inner has 2
        assertEquals(2, STInvariantsChecker.maxBranchWidth(parse("&{a: &{x: end, y: end}}")));
    }

    // -----------------------------------------------------------------------
    // Max select width
    // -----------------------------------------------------------------------

    @Test
    void maxSelectWidthEnd() {
        assertEquals(0, STInvariantsChecker.maxSelectWidth(new End()));
    }

    @Test
    void maxSelectWidthTwo() {
        assertEquals(2, STInvariantsChecker.maxSelectWidth(parse("+{ok: end, err: end}")));
    }

    // -----------------------------------------------------------------------
    // Max depth
    // -----------------------------------------------------------------------

    @Test
    void maxDepthEnd() {
        assertEquals(0, STInvariantsChecker.maxDepth(new End()));
    }

    @Test
    void maxDepthSimpleBranch() {
        assertEquals(1, STInvariantsChecker.maxDepth(parse("&{a: end}")));
    }

    @Test
    void maxDepthNested() {
        assertEquals(2, STInvariantsChecker.maxDepth(parse("&{a: &{b: end}}")));
    }

    @Test
    void maxDepthRec() {
        // rec X . &{a: X} -> depth 2 (rec + branch)
        assertEquals(2, STInvariantsChecker.maxDepth(parse("rec X . &{a: X}")));
    }

    // -----------------------------------------------------------------------
    // Reconvergence degree
    // -----------------------------------------------------------------------

    @Test
    void reconvergenceDegreeEnd() {
        var ss = build("end");
        assertEquals(0, STInvariantsChecker.reconvergenceDegree(ss));
    }

    @Test
    void reconvergenceDegreeTwoBranch() {
        var ss = build("&{a: end, b: end}");
        // Both a and b lead to end, so end has in-degree 2
        assertEquals(1, STInvariantsChecker.reconvergenceDegree(ss));
    }

    @Test
    void reconvergenceDegreeChain() {
        var ss = build("&{a: &{b: end}}");
        // Chain: no reconvergence
        assertEquals(0, STInvariantsChecker.reconvergenceDegree(ss));
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    @Test
    void computeInvariantsSimple() {
        var type = parse("&{a: end, b: end}");
        var ss = build("&{a: end, b: end}");
        var inv = STInvariantsChecker.computeInvariants(type, ss);

        assertEquals(1, inv.branchCount());
        assertEquals(0, inv.selectCount());
        assertEquals(0, inv.parallelCount());
        assertEquals(0, inv.recCount());
        assertEquals(2, inv.maxBranchWidth());
        assertEquals(0, inv.maxSelectWidth());
        assertFalse(inv.hasParallel());
        assertFalse(inv.hasRecursion());
        assertTrue(inv.isTailRecursive()); // no rec => trivially tail recursive
    }

    @Test
    void computeInvariantsRec() {
        var type = parse("rec X . &{a: X, b: end}");
        var ss = build("rec X . &{a: X, b: end}");
        var inv = STInvariantsChecker.computeInvariants(type, ss);

        assertEquals(1, inv.branchCount());
        assertEquals(1, inv.recCount());
        assertTrue(inv.hasRecursion());
        assertTrue(inv.isTailRecursive());
    }

    @Test
    void computeInvariantsSelect() {
        var type = parse("+{ok: end, err: end}");
        var ss = build("+{ok: end, err: end}");
        var inv = STInvariantsChecker.computeInvariants(type, ss);

        assertEquals(0, inv.branchCount());
        assertEquals(1, inv.selectCount());
        assertEquals(2, inv.maxSelectWidth());
    }
}
