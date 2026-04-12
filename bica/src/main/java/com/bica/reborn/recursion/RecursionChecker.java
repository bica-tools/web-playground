package com.bica.reborn.recursion;

import com.bica.reborn.ast.*;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.subtyping.SubtypingChecker;
import com.bica.reborn.termination.TerminationChecker;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Recursive type analysis (Phase 12).
 *
 * <p>Provides deeper analysis of how recursion interacts with lattice structure:
 * <ol>
 *   <li><b>Guardedness:</b> every recursive variable occurs under a constructor</li>
 *   <li><b>Contractivity:</b> recursive body makes observable progress</li>
 *   <li><b>Unfolding:</b> one-step and multi-step unfolding</li>
 *   <li><b>Tail recursion:</b> all recursive calls in tail position</li>
 *   <li><b>SCC analysis:</b> characterize strongly-connected components</li>
 *   <li><b>Metrics:</b> nesting depth, binder count, recursive variables</li>
 * </ol>
 */
public final class RecursionChecker {

    private RecursionChecker() {}

    // =========================================================================
    // Guardedness
    // =========================================================================

    /**
     * Check whether all recursive variables are guarded.
     *
     * <p>A variable X is guarded in {@code rec X . S} if every occurrence of X
     * in S is under at least one constructor (Branch, Select, Parallel, Sequence).
     */
    public static boolean isGuarded(SessionType s) {
        return checkGuardedness(s).isGuarded();
    }

    /** Check guardedness with detailed results. */
    public static GuardednessResult checkGuardedness(SessionType s) {
        List<String> unguarded = new ArrayList<>();
        checkGuardedRec(s, Set.of(), unguarded);
        List<String> sorted = unguarded.stream().distinct().sorted().toList();
        return new GuardednessResult(sorted.isEmpty(), sorted);
    }

    private static void checkGuardedRec(
            SessionType s, Set<String> unguardedVars, List<String> result) {
        switch (s) {
            case End e -> {}
            case Var v -> {
                if (unguardedVars.contains(v.name())) {
                    result.add(v.name());
                }
            }
            case Branch b -> {
                for (var c : b.choices()) {
                    checkGuardedRec(c.body(), Set.of(), result);
                }
            }
            case Select sel -> {
                for (var c : sel.choices()) {
                    checkGuardedRec(c.body(), Set.of(), result);
                }
            }
            case Parallel p -> {
                checkGuardedRec(p.left(), Set.of(), result);
                checkGuardedRec(p.right(), Set.of(), result);
            }
            case Sequence seq -> {
                checkGuardedRec(seq.left(), Set.of(), result);
                checkGuardedRec(seq.right(), Set.of(), result);
            }
            case Rec r -> {
                var newUnguarded = new HashSet<>(unguardedVars);
                newUnguarded.add(r.var());
                checkGuardedRec(r.body(), Set.copyOf(newUnguarded), result);
            }
            case Chain ch -> {
                // Chain is a constructor (labelled step), so it guards its continuation
                checkGuardedRec(ch.cont(), Set.of(), result);
            }
        }
    }

    // =========================================================================
    // Contractivity
    // =========================================================================

    /**
     * Check whether the type is contractive.
     *
     * <p>A recursive type {@code rec X . S} is contractive if S is not itself
     * a Var or Rec — the body must begin with an observable action.
     */
    public static boolean isContractive(SessionType s) {
        return checkContractivity(s).isContractive();
    }

    /** Check contractivity with detailed results. */
    public static ContractivityResult checkContractivity(SessionType s) {
        List<String> nonContractive = new ArrayList<>();
        checkContractiveRec(s, nonContractive);
        List<String> sorted = nonContractive.stream().distinct().sorted().toList();
        return new ContractivityResult(sorted.isEmpty(), sorted);
    }

    private static void checkContractiveRec(SessionType s, List<String> result) {
        switch (s) {
            case End e -> {}
            case Var v -> {}
            case Branch b -> {
                for (var c : b.choices()) checkContractiveRec(c.body(), result);
            }
            case Select sel -> {
                for (var c : sel.choices()) checkContractiveRec(c.body(), result);
            }
            case Parallel p -> {
                checkContractiveRec(p.left(), result);
                checkContractiveRec(p.right(), result);
            }
            case Sequence seq -> {
                checkContractiveRec(seq.left(), result);
                checkContractiveRec(seq.right(), result);
            }
            case Rec r -> {
                if (r.body() instanceof Var || r.body() instanceof Rec) {
                    result.add(r.var());
                }
                checkContractiveRec(r.body(), result);
            }
            case Chain ch -> checkContractiveRec(ch.cont(), result);
        }
    }

    // =========================================================================
    // Unfolding (delegates to SubtypingChecker)
    // =========================================================================

    /** Unfold one level of recursion: {@code rec X . S → S[X := rec X . S]}. */
    public static SessionType unfold(SessionType s) {
        return SubtypingChecker.unfold(s);
    }

    /** Unfold recursion n times. */
    public static SessionType unfoldDepth(SessionType s, int n) {
        SessionType result = s;
        for (int i = 0; i < n; i++) {
            if (!(result instanceof Rec)) break;
            result = unfold(result);
        }
        return result;
    }

    /** Substitute var with replacement in body. Delegates to SubtypingChecker. */
    public static SessionType substitute(SessionType body, String var, SessionType replacement) {
        return SubtypingChecker.substitute(body, var, replacement);
    }

    // =========================================================================
    // Metrics
    // =========================================================================

    /** Maximum nesting depth of rec binders. */
    public static int recDepth(SessionType s) {
        return switch (s) {
            case End e -> 0;
            case Var v -> 0;
            case Branch b -> b.choices().stream()
                    .mapToInt(c -> recDepth(c.body())).max().orElse(0);
            case Select sel -> sel.choices().stream()
                    .mapToInt(c -> recDepth(c.body())).max().orElse(0);
            case Parallel p -> Math.max(recDepth(p.left()), recDepth(p.right()));
            case Sequence seq -> Math.max(recDepth(seq.left()), recDepth(seq.right()));
            case Rec r -> 1 + recDepth(r.body());
            case Chain ch -> recDepth(ch.cont());
        };
    }

    /** Count the number of rec binders in a type. */
    public static int countRecBinders(SessionType s) {
        return switch (s) {
            case End e -> 0;
            case Var v -> 0;
            case Branch b -> b.choices().stream()
                    .mapToInt(c -> countRecBinders(c.body())).sum();
            case Select sel -> sel.choices().stream()
                    .mapToInt(c -> countRecBinders(c.body())).sum();
            case Parallel p -> countRecBinders(p.left()) + countRecBinders(p.right());
            case Sequence seq -> countRecBinders(seq.left()) + countRecBinders(seq.right());
            case Rec r -> 1 + countRecBinders(r.body());
            case Chain ch -> countRecBinders(ch.cont());
        };
    }

    /** Collect all variables bound by rec binders. */
    public static Set<String> recursiveVars(SessionType s) {
        Set<String> result = new TreeSet<>();
        collectRecVars(s, result);
        return result;
    }

    private static void collectRecVars(SessionType s, Set<String> result) {
        switch (s) {
            case End e -> {}
            case Var v -> {}
            case Branch b -> {
                for (var c : b.choices()) collectRecVars(c.body(), result);
            }
            case Select sel -> {
                for (var c : sel.choices()) collectRecVars(c.body(), result);
            }
            case Parallel p -> {
                collectRecVars(p.left(), result);
                collectRecVars(p.right(), result);
            }
            case Sequence seq -> {
                collectRecVars(seq.left(), result);
                collectRecVars(seq.right(), result);
            }
            case Rec r -> {
                result.add(r.var());
                collectRecVars(r.body(), result);
            }
            case Chain ch -> collectRecVars(ch.cont(), result);
        }
    }

    // =========================================================================
    // Tail recursion
    // =========================================================================

    /**
     * Check whether all recursive calls are in tail position.
     *
     * <p>A recursive call X is in tail position if it appears as a direct
     * continuation of a Branch or Select choice — not nested inside Parallel
     * or the left side of Sequence.
     */
    public static boolean isTailRecursive(SessionType s) {
        return switch (s) {
            case End e -> true;
            case Var v -> true;
            case Branch b -> b.choices().stream()
                    .allMatch(c -> isTailRecursive(c.body()));
            case Select sel -> sel.choices().stream()
                    .allMatch(c -> isTailRecursive(c.body()));
            case Parallel p -> isTailRecursive(p.left()) && isTailRecursive(p.right());
            case Sequence seq -> isTailRecursive(seq.left()) && isTailRecursive(seq.right());
            case Rec r -> allUsesTail(r.body(), r.var());
            case Chain ch -> isTailRecursive(ch.cont());
        };
    }

    private static boolean allUsesTail(SessionType s, String var) {
        return switch (s) {
            case End e -> true;
            case Var v -> true; // a Var reference is fine — it IS tail position
            case Branch b -> b.choices().stream()
                    .allMatch(c -> allUsesTail(c.body(), var));
            case Select sel -> sel.choices().stream()
                    .allMatch(c -> allUsesTail(c.body(), var));
            case Parallel p -> {
                // Var inside parallel is NOT tail recursive
                if (TerminationChecker.freeVars(p.left()).contains(var)
                        || TerminationChecker.freeVars(p.right()).contains(var)) {
                    yield false;
                }
                yield true;
            }
            case Sequence seq -> {
                // Var in left of sequence is NOT tail (has continuation)
                if (TerminationChecker.freeVars(seq.left()).contains(var)) {
                    yield false;
                }
                yield allUsesTail(seq.right(), var);
            }
            case Rec r -> {
                if (r.var().equals(var)) {
                    yield true; // shadowed
                }
                yield allUsesTail(r.body(), var);
            }
            case Chain ch -> allUsesTail(ch.cont(), var);
        };
    }

    // =========================================================================
    // SCC analysis
    // =========================================================================

    /**
     * Analyze SCC structure of a session type's state space.
     *
     * @return [sccCount, nonTrivialSccSizes] where non-trivial SCCs have size &gt; 1
     */
    public static int[] analyzeSccs(SessionType s) {
        StateSpace ss = StateSpaceBuilder.build(s);
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        Map<Integer, Integer> sccMap = lr.sccMap();

        // Count SCCs and their sizes
        Map<Integer, Integer> sccMembers = new HashMap<>();
        for (int rep : sccMap.values()) {
            sccMembers.merge(rep, 1, Integer::sum);
        }

        int sccCount = sccMembers.size();
        List<Integer> nonTrivial = sccMembers.values().stream()
                .filter(size -> size > 1)
                .sorted(Comparator.reverseOrder())
                .toList();

        int[] result = new int[1 + nonTrivial.size()];
        result[0] = sccCount;
        for (int i = 0; i < nonTrivial.size(); i++) {
            result[i + 1] = nonTrivial.get(i);
        }
        return result;
    }

    // =========================================================================
    // Complete analysis
    // =========================================================================

    /** Perform complete recursion analysis on a session type. */
    public static RecursionAnalysis analyzeRecursion(SessionType s) {
        GuardednessResult guardedness = checkGuardedness(s);
        ContractivityResult contractivity = checkContractivity(s);
        int[] sccInfo = analyzeSccs(s);
        int sccCount = sccInfo[0];
        List<Integer> sccSizes = new ArrayList<>();
        for (int i = 1; i < sccInfo.length; i++) {
            sccSizes.add(sccInfo[i]);
        }

        return new RecursionAnalysis(
                countRecBinders(s),
                recDepth(s),
                guardedness.isGuarded(),
                contractivity.isContractive(),
                recursiveVars(s),
                sccCount,
                sccSizes,
                isTailRecursive(s));
    }
}
