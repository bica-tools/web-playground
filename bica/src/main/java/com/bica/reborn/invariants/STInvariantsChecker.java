package com.bica.reborn.invariants;

import com.bica.reborn.ast.*;
import com.bica.reborn.recursion.RecursionChecker;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Computes session-type-specific invariants from AST and state space.
 *
 * <p>Java port of Python {@code st_invariants.py} adapted to session type
 * AST-level analysis (branch/select/parallel/rec counting, depth, width)
 * combined with state-space-level metrics (reconvergence degree).
 */
public final class STInvariantsChecker {

    private STInvariantsChecker() {}

    // -----------------------------------------------------------------------
    // AST counters
    // -----------------------------------------------------------------------

    /** Count Branch nodes in the AST. */
    public static int countBranches(SessionType node) {
        return switch (node) {
            case End ignored -> 0;
            case Var ignored -> 0;
            case Branch b -> 1 + b.choices().stream()
                    .mapToInt(c -> countBranches(c.body())).sum();
            case Select s -> s.choices().stream()
                    .mapToInt(c -> countBranches(c.body())).sum();
            case Parallel p -> countBranches(p.left()) + countBranches(p.right());
            case Rec r -> countBranches(r.body());
            case Sequence seq -> countBranches(seq.left()) + countBranches(seq.right());
            case Chain ch -> countBranches(ch.cont());
        };
    }

    /** Count Select nodes in the AST. */
    public static int countSelects(SessionType node) {
        return switch (node) {
            case End ignored -> 0;
            case Var ignored -> 0;
            case Branch b -> b.choices().stream()
                    .mapToInt(c -> countSelects(c.body())).sum();
            case Select s -> 1 + s.choices().stream()
                    .mapToInt(c -> countSelects(c.body())).sum();
            case Parallel p -> countSelects(p.left()) + countSelects(p.right());
            case Rec r -> countSelects(r.body());
            case Sequence seq -> countSelects(seq.left()) + countSelects(seq.right());
            case Chain ch -> countSelects(ch.cont());
        };
    }

    /** Count Parallel nodes in the AST. */
    public static int countParallels(SessionType node) {
        return switch (node) {
            case End ignored -> 0;
            case Var ignored -> 0;
            case Branch b -> b.choices().stream()
                    .mapToInt(c -> countParallels(c.body())).sum();
            case Select s -> s.choices().stream()
                    .mapToInt(c -> countParallels(c.body())).sum();
            case Parallel p -> 1 + countParallels(p.left()) + countParallels(p.right());
            case Rec r -> countParallels(r.body());
            case Sequence seq -> countParallels(seq.left()) + countParallels(seq.right());
            case Chain ch -> countParallels(ch.cont());
        };
    }

    /** Count Rec nodes in the AST. */
    public static int countRecs(SessionType node) {
        return switch (node) {
            case End ignored -> 0;
            case Var ignored -> 0;
            case Branch b -> b.choices().stream()
                    .mapToInt(c -> countRecs(c.body())).sum();
            case Select s -> s.choices().stream()
                    .mapToInt(c -> countRecs(c.body())).sum();
            case Parallel p -> countRecs(p.left()) + countRecs(p.right());
            case Rec r -> 1 + countRecs(r.body());
            case Sequence seq -> countRecs(seq.left()) + countRecs(seq.right());
            case Chain ch -> countRecs(ch.cont());
        };
    }

    // -----------------------------------------------------------------------
    // Width metrics
    // -----------------------------------------------------------------------

    /** Maximum number of choices in any Branch node. */
    public static int maxBranchWidth(SessionType node) {
        return switch (node) {
            case End ignored -> 0;
            case Var ignored -> 0;
            case Branch b -> {
                int childMax = b.choices().stream()
                        .mapToInt(c -> maxBranchWidth(c.body())).max().orElse(0);
                yield Math.max(b.choices().size(), childMax);
            }
            case Select s -> s.choices().stream()
                    .mapToInt(c -> maxBranchWidth(c.body())).max().orElse(0);
            case Parallel p -> Math.max(maxBranchWidth(p.left()), maxBranchWidth(p.right()));
            case Rec r -> maxBranchWidth(r.body());
            case Sequence seq -> Math.max(maxBranchWidth(seq.left()), maxBranchWidth(seq.right()));
            case Chain ch -> maxBranchWidth(ch.cont());
        };
    }

    /** Maximum number of choices in any Select node. */
    public static int maxSelectWidth(SessionType node) {
        return switch (node) {
            case End ignored -> 0;
            case Var ignored -> 0;
            case Branch b -> b.choices().stream()
                    .mapToInt(c -> maxSelectWidth(c.body())).max().orElse(0);
            case Select s -> {
                int childMax = s.choices().stream()
                        .mapToInt(c -> maxSelectWidth(c.body())).max().orElse(0);
                yield Math.max(s.choices().size(), childMax);
            }
            case Parallel p -> Math.max(maxSelectWidth(p.left()), maxSelectWidth(p.right()));
            case Rec r -> maxSelectWidth(r.body());
            case Sequence seq -> Math.max(maxSelectWidth(seq.left()), maxSelectWidth(seq.right()));
            case Chain ch -> maxSelectWidth(ch.cont());
        };
    }

    // -----------------------------------------------------------------------
    // Depth
    // -----------------------------------------------------------------------

    /** Maximum nesting depth of the AST. */
    public static int maxDepth(SessionType node) {
        return maxDepth(node, 0);
    }

    private static int maxDepth(SessionType node, int current) {
        return switch (node) {
            case End ignored -> current;
            case Var ignored -> current;
            case Branch b -> b.choices().stream()
                    .mapToInt(c -> maxDepth(c.body(), current + 1)).max().orElse(current);
            case Select s -> s.choices().stream()
                    .mapToInt(c -> maxDepth(c.body(), current + 1)).max().orElse(current);
            case Parallel p -> Math.max(
                    maxDepth(p.left(), current + 1),
                    maxDepth(p.right(), current + 1));
            case Rec r -> maxDepth(r.body(), current + 1);
            case Sequence seq -> Math.max(
                    maxDepth(seq.left(), current + 1),
                    maxDepth(seq.right(), current + 1));
            case Chain ch -> maxDepth(ch.cont(), current + 1);
        };
    }

    // -----------------------------------------------------------------------
    // Reconvergence degree (state-space level)
    // -----------------------------------------------------------------------

    /**
     * Count states with in-degree > 1 in the state space.
     *
     * <p>Reconvergence points are states reached by multiple paths, indicating
     * where divergent protocol branches rejoin.
     */
    public static int reconvergenceDegree(StateSpace ss) {
        Map<Integer, Integer> inDegree = new HashMap<>();
        for (int s : ss.states()) {
            inDegree.put(s, 0);
        }
        for (var t : ss.transitions()) {
            inDegree.merge(t.target(), 1, Integer::sum);
        }
        int count = 0;
        for (int deg : inDegree.values()) {
            if (deg > 1) count++;
        }
        return count;
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    /**
     * Compute all session-type-specific invariants.
     *
     * @param type the session type AST
     * @param ss   the corresponding state space
     * @return an {@link STInvariants} record with all computed invariants
     */
    public static STInvariants computeInvariants(SessionType type, StateSpace ss) {
        return new STInvariants(
                countBranches(type),
                countSelects(type),
                countParallels(type),
                countRecs(type),
                maxBranchWidth(type),
                maxSelectWidth(type),
                maxDepth(type),
                countParallels(type) > 0,
                countRecs(type) > 0,
                RecursionChecker.isTailRecursive(type),
                reconvergenceDegree(ss));
    }
}
