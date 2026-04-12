package com.bica.reborn.extensions;

import com.bica.reborn.ast.*;
import com.bica.reborn.morphism.MorphismChecker;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.*;

/**
 * N-ary parallel composition beyond binary (Step 5c port).
 *
 * <p>Verifies that all binary nestings of n parallel branches produce
 * isomorphic state spaces, demonstrating that the distinction between
 * binary and n-ary parallel is purely syntactic.
 */
public final class NaryParallelChecker {

    private NaryParallelChecker() {}

    /**
     * Summary of n-ary parallel analysis.
     */
    public record NaryParallelResult(
            boolean isFlat,
            int numBranches,
            boolean allNestingsIsomorphic,
            int catalanCount,
            int nestingsChecked) {}

    // =========================================================================
    // Catalan number
    // =========================================================================

    /**
     * Compute the n-th Catalan number.
     */
    public static long catalan(int n) {
        if (n < 0) return 0;
        long result = 1;
        for (int i = 0; i < n; i++) {
            result = result * 2 * (2 * i + 1) / (i + 2);
        }
        return result;
    }

    // =========================================================================
    // Flatten and nest
    // =========================================================================

    /**
     * Check if a Parallel has no nested Parallel branches.
     */
    public static boolean isFlatParallel(SessionType ast) {
        if (!(ast instanceof Parallel p)) return true;
        return !(p.left() instanceof Parallel) && !(p.right() instanceof Parallel);
    }

    /**
     * Collect all leaf branches from nested Parallel nodes.
     */
    public static List<SessionType> collectBranches(SessionType ast) {
        if (ast instanceof Parallel p) {
            List<SessionType> result = new ArrayList<>();
            result.addAll(collectBranches(p.left()));
            result.addAll(collectBranches(p.right()));
            return result;
        }
        return List.of(ast);
    }

    /**
     * Convert n branches into a right-nested binary Parallel.
     */
    public static SessionType nestRight(List<SessionType> branches) {
        if (branches.size() == 1) return branches.getFirst();
        SessionType result = branches.getLast();
        for (int i = branches.size() - 2; i >= 0; i--) {
            result = new Parallel(branches.get(i), result);
        }
        return result;
    }

    /**
     * Convert n branches into a left-nested binary Parallel.
     */
    public static SessionType nestLeft(List<SessionType> branches) {
        if (branches.size() == 1) return branches.getFirst();
        SessionType result = branches.getFirst();
        for (int i = 1; i < branches.size(); i++) {
            result = new Parallel(result, branches.get(i));
        }
        return result;
    }

    /**
     * Enumerate all binary nestings (all full binary trees).
     */
    public static List<SessionType> allNestings(List<SessionType> leaves) {
        if (leaves.size() == 1) return List.of(leaves.getFirst());
        if (leaves.size() == 2) return List.of(new Parallel(leaves.get(0), leaves.get(1)));
        List<SessionType> results = new ArrayList<>();
        for (int k = 1; k < leaves.size(); k++) {
            List<SessionType> leftTrees = allNestings(leaves.subList(0, k));
            List<SessionType> rightTrees = allNestings(leaves.subList(k, leaves.size()));
            for (SessionType lt : leftTrees) {
                for (SessionType rt : rightTrees) {
                    results.add(new Parallel(lt, rt));
                }
            }
        }
        return results;
    }

    // =========================================================================
    // Build n-ary product
    // =========================================================================

    /**
     * Build the product state space for n parallel branches.
     */
    public static StateSpace buildNaryProduct(List<SessionType> branches) {
        if (branches.isEmpty()) throw new IllegalArgumentException("need at least 1 branch");
        if (branches.size() == 1) return StateSpaceBuilder.build(branches.getFirst());
        SessionType nested = nestRight(branches);
        return StateSpaceBuilder.build(nested);
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    /**
     * Verify all binary nestings produce isomorphic state spaces.
     */
    public static NaryParallelResult analyzeNaryParallel(SessionType ast) {
        if (!(ast instanceof Parallel)) {
            return new NaryParallelResult(true, 0, true, 1, 0);
        }

        List<SessionType> branches = collectBranches(ast);
        int n = branches.size();
        boolean isFlat = isFlatParallel(ast);
        long cat = catalan(n - 1);

        if (n > 6) {
            throw new IllegalArgumentException(
                    "too many branches (" + n + "); capped at 6 (C(5)=42)");
        }

        List<SessionType> nestings = allNestings(branches);
        List<StateSpace> spaces = new ArrayList<>();
        for (SessionType nesting : nestings) {
            spaces.add(StateSpaceBuilder.build(nesting));
        }

        boolean allIso = true;
        StateSpace ref = spaces.getFirst();
        for (int i = 1; i < spaces.size(); i++) {
            var iso = MorphismChecker.findIsomorphism(ref, spaces.get(i));
            if (iso == null) { allIso = false; break; }
        }

        return new NaryParallelResult(isFlat, n, allIso, (int) cat, nestings.size());
    }
}
