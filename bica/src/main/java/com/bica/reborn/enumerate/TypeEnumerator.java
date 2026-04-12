package com.bica.reborn.enumerate;

import com.bica.reborn.ast.*;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.termination.TerminationChecker;

import java.util.*;
import java.util.function.Consumer;

/**
 * Exhaustive enumeration of session types for universality checking.
 *
 * <p>Generates all structurally distinct session types up to a given depth
 * and width, builds their state spaces, and checks whether L(S) is always a lattice.
 */
public final class TypeEnumerator {

    private TypeEnumerator() {}

    /**
     * Enumerate all session types up to the configured bounds.
     */
    public static List<SessionType> enumerate(EnumerationConfig config) {
        List<SessionType> result = new ArrayList<>();
        enumerate(config.maxDepth(), config, null, result::add);
        return result;
    }

    /**
     * Check universality: is L(S) a lattice for all enumerated types?
     */
    public static UniversalityResult checkUniversality(EnumerationConfig config) {
        int total = 0;
        int lattices = 0;
        List<UniversalityResult.Counterexample> counterexamples = new ArrayList<>();

        for (SessionType ast : enumerate(config)) {
            if (config.requireTerminating()) {
                if (!TerminationChecker.isTerminating(ast)) {
                    continue;
                }
            }

            total++;
            StateSpace ss;
            try {
                ss = StateSpaceBuilder.build(ast);
            } catch (Exception e) {
                continue; // skip malformed types
            }

            LatticeResult lr;
            try {
                lr = LatticeChecker.checkLattice(ss);
            } catch (Exception e) {
                continue;
            }

            if (lr.isLattice()) {
                lattices++;
            } else {
                counterexamples.add(new UniversalityResult.Counterexample(
                        PrettyPrinter.pretty(ast), lr));
            }
        }

        return new UniversalityResult(total, lattices, counterexamples, config);
    }

    // -----------------------------------------------------------------------
    // Internal enumeration
    // -----------------------------------------------------------------------

    private static void enumerate(
            int depth,
            EnumerationConfig config,
            String recVar,
            Consumer<SessionType> sink) {

        // Base: terminals
        sink.accept(new End());
        if (recVar != null) {
            sink.accept(new Var(recVar));
        }
        if (depth <= 0) return;

        List<String> labels = config.labels();
        int maxWidth = Math.min(config.maxBranchWidth(), labels.size());

        // Branch: &{...}
        List<SessionType> subTypes = collectSub(depth - 1, config, recVar);
        for (List<String> labelCombo : combinations(labels, maxWidth)) {
            cartesian(subTypes, labelCombo.size(), choices -> {
                List<Branch.Choice> cs = new ArrayList<>();
                for (int i = 0; i < labelCombo.size(); i++) {
                    cs.add(new Branch.Choice(labelCombo.get(i), choices.get(i)));
                }
                sink.accept(new Branch(cs));
            });
        }

        // Selection: +{...}
        if (config.includeSelection()) {
            for (List<String> labelCombo : combinations(labels, maxWidth)) {
                cartesian(subTypes, labelCombo.size(), choices -> {
                    List<Branch.Choice> cs = new ArrayList<>();
                    for (int i = 0; i < labelCombo.size(); i++) {
                        cs.add(new Branch.Choice(labelCombo.get(i), choices.get(i)));
                    }
                    sink.accept(new Select(cs));
                });
            }
        }

        // Parallel: (S1 || S2)
        if (config.includeParallel()) {
            List<SessionType> parSub = collectSub(depth - 1, config, recVar);
            for (SessionType left : parSub) {
                for (SessionType right : parSub) {
                    sink.accept(new Parallel(left, right));
                }
            }
        }

        // Recursion: rec X . S
        if (config.includeRecursion() && recVar == null) {
            String varName = "X";
            List<SessionType> recSub = collectSub(depth - 1, config, varName);
            for (SessionType body : recSub) {
                if (containsVar(body, varName) && !(body instanceof Var)) {
                    sink.accept(new Rec(varName, body));
                }
            }
        }
    }

    private static List<SessionType> collectSub(
            int depth, EnumerationConfig config, String recVar) {
        List<SessionType> result = new ArrayList<>();
        enumerate(depth, config, recVar, result::add);
        return result;
    }

    private static boolean containsVar(SessionType node, String varName) {
        return switch (node) {
            case Var v -> v.name().equals(varName);
            case End e -> false;
            case Branch b -> b.choices().stream()
                    .anyMatch(c -> containsVar(c.body(), varName));
            case Select s -> s.choices().stream()
                    .anyMatch(c -> containsVar(c.body(), varName));
            case Parallel p -> containsVar(p.left(), varName)
                    || containsVar(p.right(), varName);
            case Rec r -> containsVar(r.body(), varName);
            case Sequence s -> containsVar(s.left(), varName)
                    || containsVar(s.right(), varName);
            case Chain ch -> containsVar(ch.cont(), varName);
        };
    }

    // -----------------------------------------------------------------------
    // Combinatorial helpers
    // -----------------------------------------------------------------------

    /** Generate all C(n,k) combinations for k=1..maxWidth. */
    private static List<List<String>> combinations(List<String> labels, int maxWidth) {
        List<List<String>> result = new ArrayList<>();
        for (int k = 1; k <= maxWidth; k++) {
            combinationsHelper(labels, k, 0, new ArrayList<>(), result);
        }
        return result;
    }

    private static void combinationsHelper(
            List<String> labels, int k, int start,
            List<String> current, List<List<String>> result) {
        if (current.size() == k) {
            result.add(List.copyOf(current));
            return;
        }
        for (int i = start; i < labels.size(); i++) {
            current.add(labels.get(i));
            combinationsHelper(labels, k, i + 1, current, result);
            current.removeLast();
        }
    }

    /** Generate all cartesian products of size `repeat` from `elements`. */
    private static void cartesian(
            List<SessionType> elements, int repeat,
            Consumer<List<SessionType>> sink) {
        cartesianHelper(elements, repeat, new ArrayList<>(), sink);
    }

    private static void cartesianHelper(
            List<SessionType> elements, int repeat,
            List<SessionType> current,
            Consumer<List<SessionType>> sink) {
        if (current.size() == repeat) {
            sink.accept(List.copyOf(current));
            return;
        }
        for (SessionType e : elements) {
            current.add(e);
            cartesianHelper(elements, repeat, current, sink);
            current.removeLast();
        }
    }
}
