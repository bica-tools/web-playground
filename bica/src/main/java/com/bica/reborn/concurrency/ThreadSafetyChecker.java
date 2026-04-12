package com.bica.reborn.concurrency;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;

import java.util.*;

/**
 * Checks thread safety for session types containing parallel composition.
 *
 * <p>For each {@code S₁ ∥ S₂} in a session type, every method reachable in
 * {@code S₁} may execute concurrently with every method reachable in {@code S₂}.
 * The checker verifies that all such cross-branch pairs are
 * {@linkplain ConcurrencyLattice#compatible compatible}.
 *
 * <p>Methods are classified via a {@link MethodClassifier}. Unknown methods
 * (classifier returns {@code null}) default to {@link ConcurrencyLevel#EXCLUSIVE}.
 */
public final class ThreadSafetyChecker {

    private ThreadSafetyChecker() {}

    /**
     * Extracts all method names reachable from the given session type.
     * Method names are the labels of {@link Branch} choices only.
     * {@link Select} labels are enum return values, not callable methods.
     */
    public static Set<String> extractMethods(SessionType type) {
        Objects.requireNonNull(type, "type must not be null");
        var methods = new TreeSet<String>();
        collectMethods(type, methods);
        return Collections.unmodifiableSet(methods);
    }

    /**
     * Checks thread safety of all parallel compositions in the session type.
     *
     * @param type       The session type to check.
     * @param classifier Classifies method names to concurrency levels.
     * @return The thread safety result.
     */
    public static ThreadSafetyResult check(SessionType type, MethodClassifier classifier) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(classifier, "classifier must not be null");

        var violations = new ArrayList<ThreadSafetyResult.Violation>();
        var trustBoundaries = new ArrayList<String>();
        var classificationMap = new LinkedHashMap<String, MethodClassification>();

        // Collect all methods and classify them
        Set<String> allMethods = extractMethods(type);
        for (String method : allMethods) {
            MethodClassification mc = classifier.classify(method);
            if (mc == null) {
                mc = new MethodClassification(method, ConcurrencyLevel.EXCLUSIVE, "default");
            }
            classificationMap.put(method, mc);
            if (mc.level() == ConcurrencyLevel.SHARED
                    || mc.level() == ConcurrencyLevel.SYNC) {
                trustBoundaries.add(method);
            }
        }

        // Walk AST to find Parallel nodes and check cross-branch pairs
        checkNode(type, classifier, classificationMap, violations);

        return new ThreadSafetyResult(
                violations.isEmpty(),
                violations,
                trustBoundaries,
                new ArrayList<>(classificationMap.values()));
    }

    // -- private: method extraction -------------------------------------------

    private static void collectMethods(SessionType type, Set<String> acc) {
        switch (type) {
            case End ignored -> {}
            case Var ignored -> {}
            case Branch b -> {
                for (Choice c : b.choices()) {
                    acc.add(c.label());
                    collectMethods(c.body(), acc);
                }
            }
            case Select s -> {
                for (Choice c : s.choices()) {
                    collectMethods(c.body(), acc);
                }
            }
            case Parallel p -> {
                collectMethods(p.left(), acc);
                collectMethods(p.right(), acc);
            }
            case Rec r -> collectMethods(r.body(), acc);
            case Sequence seq -> {
                collectMethods(seq.left(), acc);
                collectMethods(seq.right(), acc);
            }
            case Chain ch -> {
                acc.add(ch.method());
                collectMethods(ch.cont(), acc);
            }
        }
    }

    // -- private: thread safety check -----------------------------------------

    private static void checkNode(
            SessionType type,
            MethodClassifier classifier,
            Map<String, MethodClassification> classificationMap,
            List<ThreadSafetyResult.Violation> violations) {

        switch (type) {
            case End ignored -> {}
            case Var ignored -> {}
            case Branch b -> {
                for (Choice c : b.choices())
                    checkNode(c.body(), classifier, classificationMap, violations);
            }
            case Select s -> {
                for (Choice c : s.choices())
                    checkNode(c.body(), classifier, classificationMap, violations);
            }
            case Sequence seq -> {
                checkNode(seq.left(), classifier, classificationMap, violations);
                checkNode(seq.right(), classifier, classificationMap, violations);
            }
            case Rec r -> checkNode(r.body(), classifier, classificationMap, violations);
            case Parallel p -> {
                // Check cross-branch pairs
                Set<String> leftMethods = extractMethods(p.left());
                Set<String> rightMethods = extractMethods(p.right());
                checkCrossBranchPairs(leftMethods, rightMethods,
                        classificationMap, violations);

                // Recurse into branches
                checkNode(p.left(), classifier, classificationMap, violations);
                checkNode(p.right(), classifier, classificationMap, violations);
            }
            case Chain ch ->
                    checkNode(ch.cont(), classifier, classificationMap, violations);
        }
    }

    private static void checkCrossBranchPairs(
            Set<String> leftMethods,
            Set<String> rightMethods,
            Map<String, MethodClassification> classificationMap,
            List<ThreadSafetyResult.Violation> violations) {

        for (String left : leftMethods) {
            for (String right : rightMethods) {
                MethodClassification leftMc = classificationMap.get(left);
                MethodClassification rightMc = classificationMap.get(right);
                if (!ConcurrencyLattice.compatible(leftMc.level(), rightMc.level())) {
                    violations.add(new ThreadSafetyResult.Violation(
                            left, leftMc.level(), right, rightMc.level()));
                }
            }
        }
    }
}
