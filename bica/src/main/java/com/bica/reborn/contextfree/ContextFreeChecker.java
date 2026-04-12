package com.bica.reborn.contextfree;

import com.bica.reborn.ast.*;
import com.bica.reborn.recursion.RecursionChecker;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.termination.TerminationChecker;

import java.util.HashSet;
import java.util.Set;

/**
 * Chomsky hierarchy classification for session types (Phase 13).
 *
 * <p>Session types can be classified by the Chomsky hierarchy:
 * <ol>
 *   <li><b>Regular</b> (finite automaton): Types where recursion is tail-recursive.
 *       The state space is finite and corresponds to a regular language of traces.
 *       Most practical protocols fall here.</li>
 *   <li><b>Context-free</b> (pushdown automaton): Types where recursion is NOT
 *       tail-recursive — the recursive call has a continuation, requiring a stack.
 *       Example: {@code rec X . &{push: X . &{pop: end}}} (matched push/pop).</li>
 * </ol>
 *
 * <p>Key insight: ALL session types in our grammar produce finite state spaces
 * (recursion creates cycles, not infinite unfolding). The "context-free"
 * distinction is at the LANGUAGE level — the set of valid traces may be
 * context-free even though the state space is finite.
 */
public final class ContextFreeChecker {

    private ContextFreeChecker() {}

    // =========================================================================
    // Regularity
    // =========================================================================

    /**
     * Check whether a session type is regular.
     *
     * <p>A type is regular if it has no recursion, or all recursion is
     * tail-recursive (no continuation after recursive call).
     */
    public static boolean isRegular(SessionType s) {
        return classifyChomsky(s).isRegular();
    }

    // =========================================================================
    // Continuation recursion detection
    // =========================================================================

    /**
     * Check whether recursion occurs inside a Sequence left-hand side.
     *
     * <p>This is the key indicator of context-free behavior: after a recursive
     * call completes, there is more protocol to execute, requiring a stack.
     */
    public static boolean hasContinuationRecursion(SessionType s) {
        return findContinuationRecursion(s, Set.of());
    }

    private static boolean findContinuationRecursion(SessionType s, Set<String> boundVars) {
        return switch (s) {
            case End e -> false;
            case Var v -> false;
            case Branch b -> b.choices().stream()
                    .anyMatch(c -> findContinuationRecursion(c.body(), boundVars));
            case Select sel -> sel.choices().stream()
                    .anyMatch(c -> findContinuationRecursion(c.body(), boundVars));
            case Parallel p ->
                    findContinuationRecursion(p.left(), boundVars)
                    || findContinuationRecursion(p.right(), boundVars);
            case Sequence seq -> {
                // Check if the left side contains any bound recursive vars
                Set<String> leftFree = TerminationChecker.freeVars(seq.left());
                Set<String> intersection = new HashSet<>(leftFree);
                intersection.retainAll(boundVars);
                if (!intersection.isEmpty()) {
                    yield true;
                }
                yield findContinuationRecursion(seq.left(), boundVars)
                        || findContinuationRecursion(seq.right(), boundVars);
            }
            case Rec r -> {
                var newBound = new HashSet<>(boundVars);
                newBound.add(r.var());
                yield findContinuationRecursion(r.body(), Set.copyOf(newBound));
            }
            case Chain ch -> findContinuationRecursion(ch.cont(), boundVars);
        };
    }

    // =========================================================================
    // Stack depth
    // =========================================================================

    /**
     * Compute an upper bound on the stack depth needed.
     *
     * <p>For regular types this is 0. For context-free types it measures the
     * nesting depth of continuation recursion.
     */
    public static int stackDepthBound(SessionType s) {
        if (!hasContinuationRecursion(s)) {
            return 0;
        }
        return computeStackBound(s, Set.of(), 0);
    }

    private static int computeStackBound(SessionType s, Set<String> boundVars, int currentDepth) {
        return switch (s) {
            case End e -> currentDepth;
            case Var v -> currentDepth;
            case Branch b -> b.choices().stream()
                    .mapToInt(c -> computeStackBound(c.body(), boundVars, currentDepth))
                    .max().orElse(currentDepth);
            case Select sel -> sel.choices().stream()
                    .mapToInt(c -> computeStackBound(c.body(), boundVars, currentDepth))
                    .max().orElse(currentDepth);
            case Parallel p -> Math.max(
                    computeStackBound(p.left(), boundVars, currentDepth),
                    computeStackBound(p.right(), boundVars, currentDepth));
            case Sequence seq -> {
                Set<String> leftFree = TerminationChecker.freeVars(seq.left());
                Set<String> intersection = new HashSet<>(leftFree);
                intersection.retainAll(boundVars);
                int depth = intersection.isEmpty() ? currentDepth : currentDepth + 1;
                yield Math.max(
                        computeStackBound(seq.left(), boundVars, depth),
                        computeStackBound(seq.right(), boundVars, currentDepth));
            }
            case Rec r -> {
                var newBound = new HashSet<>(boundVars);
                newBound.add(r.var());
                yield computeStackBound(r.body(), Set.copyOf(newBound), currentDepth);
            }
            case Chain ch -> computeStackBound(ch.cont(), boundVars, currentDepth);
        };
    }

    // =========================================================================
    // AST predicates
    // =========================================================================

    /** Check if type contains Parallel constructor. */
    public static boolean containsParallel(SessionType s) {
        return switch (s) {
            case Parallel p -> true;
            case Branch b -> b.choices().stream()
                    .anyMatch(c -> containsParallel(c.body()));
            case Select sel -> sel.choices().stream()
                    .anyMatch(c -> containsParallel(c.body()));
            case Sequence seq -> containsParallel(seq.left()) || containsParallel(seq.right());
            case Rec r -> containsParallel(r.body());
            default -> false;
        };
    }

    /** Check if type contains Sequence constructor. */
    public static boolean containsSequence(SessionType s) {
        return switch (s) {
            case Sequence seq -> true;
            case Branch b -> b.choices().stream()
                    .anyMatch(c -> containsSequence(c.body()));
            case Select sel -> sel.choices().stream()
                    .anyMatch(c -> containsSequence(c.body()));
            case Parallel p -> containsSequence(p.left()) || containsSequence(p.right());
            case Rec r -> containsSequence(r.body());
            default -> false;
        };
    }

    // =========================================================================
    // Classification
    // =========================================================================

    /** Classify a session type in the Chomsky hierarchy. */
    public static ChomskyClassification classifyChomsky(SessionType s) {
        int nRec = RecursionChecker.countRecBinders(s);
        int depth = RecursionChecker.recDepth(s);
        boolean hasContRec = hasContinuationRecursion(s);

        if (nRec == 0) {
            return new ChomskyClassification(
                    "regular", true, false,
                    "finite (no recursion)",
                    0, 0, 0);
        }

        if (!hasContRec) {
            return new ChomskyClassification(
                    "regular", true, false,
                    "regular (tail-recursive)",
                    nRec, depth, 0);
        }

        int sdb = stackDepthBound(s);
        return new ChomskyClassification(
                "context-free", false, true,
                "context-free (continuation recursion)",
                nRec, depth, sdb);
    }

    // =========================================================================
    // Trace language analysis
    // =========================================================================

    /** Analyze the trace language properties of a session type. */
    public static TraceLanguageAnalysis analyzeTraceLanguage(SessionType s) {
        ChomskyClassification classification = classifyChomsky(s);
        StateSpace ss = StateSpaceBuilder.build(s);
        int nRec = RecursionChecker.countRecBinders(s);

        return new TraceLanguageAnalysis(
                classification.level(),
                nRec == 0,
                classification.isRegular(),
                nRec > 0,
                containsParallel(s),
                containsSequence(s),
                ss.states().size(),
                ss.transitions().size());
    }
}
