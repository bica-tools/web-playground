package com.bica.reborn.compress;

import com.bica.reborn.ast.*;
import com.bica.reborn.parser.Parser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for session type compression.
 */
class CompressionCheckerTest {

    // -----------------------------------------------------------------------
    // AST size
    // -----------------------------------------------------------------------

    @Test
    void astSizeEnd() {
        assertEquals(1, CompressionChecker.astSize(new End()));
    }

    @Test
    void astSizeVar() {
        assertEquals(1, CompressionChecker.astSize(new Var("X")));
    }

    @Test
    void astSizeSimpleBranch() {
        var ast = Parser.parse("&{a: end}");
        assertEquals(2, CompressionChecker.astSize(ast));
    }

    @Test
    void astSizeTwoBranch() {
        var ast = Parser.parse("&{a: end, b: end}");
        assertEquals(3, CompressionChecker.astSize(ast));
    }

    @Test
    void astSizeSelect() {
        var ast = Parser.parse("+{ok: end, err: end}");
        assertEquals(3, CompressionChecker.astSize(ast));
    }

    @Test
    void astSizeRec() {
        var ast = Parser.parse("rec X . &{a: X}");
        // Rec(1) + Branch(1) + Var(1) = 3
        assertEquals(3, CompressionChecker.astSize(ast));
    }

    @Test
    void astSizeNested() {
        var ast = Parser.parse("&{a: &{b: end}, c: end}");
        // outer Branch(1) + inner Branch(1) + End(1) + End(1) = 4
        assertEquals(4, CompressionChecker.astSize(ast));
    }

    @Test
    void astSizeSequence() {
        var left = new Branch(List.of(new Branch.Choice("a", new End())));
        var right = new End();
        var seq = new Sequence(left, right);
        // Sequence(1) + Branch(1) + End(1) + End(1) = 4
        assertEquals(4, CompressionChecker.astSize(seq));
    }

    // -----------------------------------------------------------------------
    // Find shared subtrees
    // -----------------------------------------------------------------------

    @Test
    void noSharingInSimpleType() {
        var ast = Parser.parse("&{a: end, b: end}");
        var shared = CompressionChecker.findSharedSubtrees(ast);
        // End appears twice but size=1 < MIN_SHARED_SIZE=3
        assertTrue(shared.isEmpty());
    }

    @Test
    void sharingDetected() {
        // Build AST with repeated subtree: &{a: end, b: end} appears twice
        var sub = new Branch(List.of(
                new Branch.Choice("a", new End()),
                new Branch.Choice("b", new End())));
        var root = new Branch(List.of(
                new Branch.Choice("x", sub),
                new Branch.Choice("y", sub)));
        var shared = CompressionChecker.findSharedSubtrees(root);
        assertTrue(shared.containsKey(sub));
        assertEquals(2, shared.get(sub));
    }

    @Test
    void noSharingWhenAllUnique() {
        var ast = Parser.parse("&{a: end}");
        var shared = CompressionChecker.findSharedSubtrees(ast);
        assertTrue(shared.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Compress
    // -----------------------------------------------------------------------

    @Test
    void compressNoSharing() {
        var ast = Parser.parse("&{a: end}");
        var compressed = CompressionChecker.compress(ast);
        assertEquals(ast, compressed);
    }

    @Test
    void compressWithSharing() {
        var sub = new Branch(List.of(
                new Branch.Choice("a", new End()),
                new Branch.Choice("b", new End())));
        var root = new Branch(List.of(
                new Branch.Choice("x", sub),
                new Branch.Choice("y", sub)));

        var compressed = CompressionChecker.compress(root);
        assertNotEquals(root, compressed);
        // Compressed should be smaller
        assertTrue(CompressionChecker.astSize(compressed) <= CompressionChecker.astSize(root));
    }

    @Test
    void compressEndIsUnchanged() {
        var compressed = CompressionChecker.compress(new End());
        assertEquals(new End(), compressed);
    }

    // -----------------------------------------------------------------------
    // Compression ratio
    // -----------------------------------------------------------------------

    @Test
    void ratioNoSharing() {
        var ast = Parser.parse("&{a: end}");
        assertEquals(1.0, CompressionChecker.compressionRatio(ast), 1e-9);
    }

    @Test
    void ratioWithSharing() {
        var sub = new Branch(List.of(
                new Branch.Choice("a", new End()),
                new Branch.Choice("b", new End())));
        var root = new Branch(List.of(
                new Branch.Choice("x", sub),
                new Branch.Choice("y", sub)));
        double ratio = CompressionChecker.compressionRatio(root);
        assertTrue(ratio >= 1.0, "Ratio should be >= 1.0, got " + ratio);
    }

    // -----------------------------------------------------------------------
    // Analyze
    // -----------------------------------------------------------------------

    @Test
    void analyzeNoSharing() {
        var ast = Parser.parse("&{a: end, b: end}");
        var result = CompressionChecker.analyze(ast);
        assertEquals(3, result.originalSize());
        assertEquals(0, result.sharedSubtrees());
        assertEquals(1.0, result.compressionRatio(), 1e-9);
    }

    @Test
    void analyzeWithSharing() {
        var sub = new Branch(List.of(
                new Branch.Choice("a", new End()),
                new Branch.Choice("b", new End())));
        var root = new Branch(List.of(
                new Branch.Choice("x", sub),
                new Branch.Choice("y", sub)));

        var result = CompressionChecker.analyze(root);
        assertEquals(7, result.originalSize()); // 1 + 2*(1+1+1)
        assertTrue(result.sharedSubtrees() > 0);
        assertTrue(result.compressionRatio() >= 1.0);
        assertNotNull(result.compressed());
    }

    @Test
    void analyzeRecType() {
        var ast = Parser.parse("rec X . &{a: X, b: end}");
        var result = CompressionChecker.analyze(ast);
        assertTrue(result.originalSize() > 0);
        assertNotNull(result.compressed());
    }
}
