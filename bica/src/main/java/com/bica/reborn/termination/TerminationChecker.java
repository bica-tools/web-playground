package com.bica.reborn.termination;

import com.bica.reborn.ast.*;

import java.util.*;

/**
 * AST-level termination checking and WF-Par well-formedness for session types.
 *
 * <p><b>Termination</b> (spec section 2.3.3): Every recursive type {@code rec X. S} must have
 * at least one syntactic path from the root of S to a leaf that is NOT {@code Var(X)}.
 *
 * <p><b>WF-Par</b> (spec section 5.1): For each {@code S1 || S2}:
 * <ol>
 *   <li>Both branches are terminating.</li>
 *   <li>No cross-branch recursion variables.</li>
 *   <li>No nested {@code ||} inside a {@code ||} branch.</li>
 * </ol>
 */
public final class TerminationChecker {

    private TerminationChecker() {}

    /** Return {@code true} iff every {@code Rec} in {@code sessionType} has an exit path. */
    public static boolean isTerminating(SessionType sessionType) {
        return collectNonTerminating(sessionType).isEmpty();
    }

    /** Full termination analysis of {@code sessionType}. */
    public static TerminationResult checkTermination(SessionType sessionType) {
        List<String> bad = collectNonTerminating(sessionType);
        return new TerminationResult(bad.isEmpty(), bad);
    }

    /** Check WF-Par well-formedness for every {@code Parallel} in {@code sessionType}. */
    public static WFParallelResult checkWfParallel(SessionType sessionType) {
        List<String> errors = collectWfParErrors(sessionType);
        return new WFParallelResult(errors.isEmpty(), errors);
    }

    // -- package-private for testing -----------------------------------------------

    static boolean hasExitPath(SessionType node, String forbidden) {
        return switch (node) {
            case End ignored -> true;
            case Var v -> !v.name().equals(forbidden);
            case Branch b -> b.choices().stream()
                    .anyMatch(c -> hasExitPath(c.body(), forbidden));
            case Select s -> s.choices().stream()
                    .anyMatch(c -> hasExitPath(c.body(), forbidden));
            case Sequence seq -> hasExitPath(seq.left(), forbidden)
                    && hasExitPath(seq.right(), forbidden);
            case Parallel p -> hasExitPath(p.left(), forbidden)
                    && hasExitPath(p.right(), forbidden);
            case Rec r -> hasExitPath(r.body(), forbidden);
            case Chain ch -> hasExitPath(ch.cont(), forbidden);
        };
    }

    public static Set<String> freeVars(SessionType node) {
        return switch (node) {
            case End ignored -> Set.of();
            case Var v -> Set.of(v.name());
            case Branch b -> {
                var result = new HashSet<String>();
                for (var c : b.choices()) result.addAll(freeVars(c.body()));
                yield result;
            }
            case Select s -> {
                var result = new HashSet<String>();
                for (var c : s.choices()) result.addAll(freeVars(c.body()));
                yield result;
            }
            case Sequence seq -> {
                var result = new HashSet<>(freeVars(seq.left()));
                result.addAll(freeVars(seq.right()));
                yield result;
            }
            case Parallel p -> {
                var result = new HashSet<>(freeVars(p.left()));
                result.addAll(freeVars(p.right()));
                yield result;
            }
            case Rec r -> {
                var result = new HashSet<>(freeVars(r.body()));
                result.remove(r.var());
                yield result;
            }
            case Chain ch -> freeVars(ch.cont());
        };
    }

    static Set<String> boundVars(SessionType node) {
        return switch (node) {
            case End ignored -> Set.of();
            case Var v -> Set.of();
            case Branch b -> {
                var result = new HashSet<String>();
                for (var c : b.choices()) result.addAll(boundVars(c.body()));
                yield result;
            }
            case Select s -> {
                var result = new HashSet<String>();
                for (var c : s.choices()) result.addAll(boundVars(c.body()));
                yield result;
            }
            case Sequence seq -> {
                var result = new HashSet<>(boundVars(seq.left()));
                result.addAll(boundVars(seq.right()));
                yield result;
            }
            case Parallel p -> {
                var result = new HashSet<>(boundVars(p.left()));
                result.addAll(boundVars(p.right()));
                yield result;
            }
            case Rec r -> {
                var result = new HashSet<>(boundVars(r.body()));
                result.add(r.var());
                yield result;
            }
            case Chain ch -> boundVars(ch.cont());
        };
    }

    static boolean containsParallel(SessionType node) {
        return switch (node) {
            case End ignored -> false;
            case Var v -> false;
            case Branch b -> b.choices().stream()
                    .anyMatch(c -> containsParallel(c.body()));
            case Select s -> s.choices().stream()
                    .anyMatch(c -> containsParallel(c.body()));
            case Sequence seq -> containsParallel(seq.left()) || containsParallel(seq.right());
            case Parallel p -> true;
            case Rec r -> containsParallel(r.body());
            case Chain ch -> containsParallel(ch.cont());
        };
    }

    // -- private: termination ---------------------------------------------------

    private static List<String> collectNonTerminating(SessionType node) {
        var result = new ArrayList<String>();
        walkForTermination(node, result);
        return result;
    }

    private static void walkForTermination(SessionType node, List<String> acc) {
        switch (node) {
            case End ignored -> {}
            case Var v -> {}
            case Branch b -> {
                for (var c : b.choices()) walkForTermination(c.body(), acc);
            }
            case Select s -> {
                for (var c : s.choices()) walkForTermination(c.body(), acc);
            }
            case Sequence seq -> {
                walkForTermination(seq.left(), acc);
                walkForTermination(seq.right(), acc);
            }
            case Parallel p -> {
                walkForTermination(p.left(), acc);
                walkForTermination(p.right(), acc);
            }
            case Rec r -> {
                if (!hasExitPath(r.body(), r.var())) {
                    acc.add(r.var());
                }
                walkForTermination(r.body(), acc);
            }
            case Chain ch -> walkForTermination(ch.cont(), acc);
        }
    }

    // -- private: WF-Par --------------------------------------------------------

    private static List<String> collectWfParErrors(SessionType node) {
        var errors = new ArrayList<String>();
        walkForWfPar(node, errors);
        return errors;
    }

    private static void walkForWfPar(SessionType node, List<String> errors) {
        switch (node) {
            case End ignored -> {}
            case Var v -> {}
            case Branch b -> {
                for (var c : b.choices()) walkForWfPar(c.body(), errors);
            }
            case Select s -> {
                for (var c : s.choices()) walkForWfPar(c.body(), errors);
            }
            case Sequence seq -> {
                walkForWfPar(seq.left(), errors);
                walkForWfPar(seq.right(), errors);
            }
            case Parallel p -> {
                checkWfParNode(p.left(), p.right(), errors);
                walkForWfPar(p.left(), errors);
                walkForWfPar(p.right(), errors);
            }
            case Rec r -> walkForWfPar(r.body(), errors);
            case Chain ch -> walkForWfPar(ch.cont(), errors);
        }
    }

    private static void checkWfParNode(
            SessionType left, SessionType right, List<String> errors) {
        // 1. Termination: both branches must be terminating
        List<String> leftBad = collectNonTerminating(left);
        if (!leftBad.isEmpty()) {
            errors.add("left branch of \u2225 is non-terminating "
                    + "(non-terminating vars: " + String.join(", ", leftBad) + ")");
        }
        List<String> rightBad = collectNonTerminating(right);
        if (!rightBad.isEmpty()) {
            errors.add("right branch of \u2225 is non-terminating "
                    + "(non-terminating vars: " + String.join(", ", rightBad) + ")");
        }

        // 2. No cross-branch variables
        Set<String> leftFree = freeVars(left);
        Set<String> rightBound = boundVars(right);
        Set<String> crossLR = new TreeSet<>(leftFree);
        crossLR.retainAll(rightBound);
        if (!crossLR.isEmpty()) {
            errors.add("cross-branch variable(s) " + String.join(", ", crossLR)
                    + ": free in left, bound in right");
        }

        Set<String> rightFree = freeVars(right);
        Set<String> leftBound = boundVars(left);
        Set<String> crossRL = new TreeSet<>(rightFree);
        crossRL.retainAll(leftBound);
        if (!crossRL.isEmpty()) {
            errors.add("cross-branch variable(s) " + String.join(", ", crossRL)
                    + ": free in right, bound in left");
        }

        // 3. No nested parallel
        if (containsParallel(left)) {
            errors.add("left branch of \u2225 contains nested \u2225");
        }
        if (containsParallel(right)) {
            errors.add("right branch of \u2225 contains nested \u2225");
        }
    }
}
