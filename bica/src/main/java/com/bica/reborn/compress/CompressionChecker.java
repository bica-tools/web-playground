package com.bica.reborn.compress;

import com.bica.reborn.ast.*;

import java.util.*;

/**
 * Session type compression: detects shared (structurally equal) subtrees
 * and factors them into named equations using {@link Rec}/{@link Var}.
 *
 * <p>This is the Java port of Python {@code compress.py}. The compression
 * is semantic-preserving: the compressed type unfolds to the same state space.
 *
 * <p>Shared subtrees correspond to reconvergence points in the state space.
 */
public final class CompressionChecker {

    /** Minimum subtree size worth factoring out (leaves are never shared). */
    private static final int MIN_SHARED_SIZE = 3;

    private CompressionChecker() {}

    // -----------------------------------------------------------------------
    // AST size
    // -----------------------------------------------------------------------

    /**
     * Count the number of AST nodes in a session type.
     */
    public static int astSize(SessionType node) {
        return switch (node) {
            case End ignored -> 1;
            case Var ignored -> 1;
            case Branch b -> 1 + b.choices().stream()
                    .mapToInt(c -> astSize(c.body()))
                    .sum();
            case Select s -> 1 + s.choices().stream()
                    .mapToInt(c -> astSize(c.body()))
                    .sum();
            case Parallel p -> 1 + astSize(p.left()) + astSize(p.right());
            case Rec r -> 1 + astSize(r.body());
            case Sequence seq -> 1 + astSize(seq.left()) + astSize(seq.right());
            case Chain ch -> 1 + astSize(ch.cont());
        };
    }

    // -----------------------------------------------------------------------
    // Subtree collection
    // -----------------------------------------------------------------------

    /**
     * Find all structurally equal subtrees that appear more than once.
     *
     * @param node the root AST node
     * @return map from subtree to occurrence count (only entries with count >= 2)
     */
    public static Map<SessionType, Integer> findSharedSubtrees(SessionType node) {
        var counts = new HashMap<SessionType, Integer>();
        collectSubtrees(node, counts);

        // Filter to shared subtrees large enough to be worth naming
        var shared = new LinkedHashMap<SessionType, Integer>();
        for (var entry : counts.entrySet()) {
            if (entry.getValue() >= 2 && astSize(entry.getKey()) >= MIN_SHARED_SIZE) {
                shared.put(entry.getKey(), entry.getValue());
            }
        }
        return shared;
    }

    private static void collectSubtrees(SessionType node, Map<SessionType, Integer> counts) {
        counts.merge(node, 1, Integer::sum);

        switch (node) {
            case End ignored -> {}
            case Var ignored -> {}
            case Branch b -> b.choices().forEach(c -> collectSubtrees(c.body(), counts));
            case Select s -> s.choices().forEach(c -> collectSubtrees(c.body(), counts));
            case Parallel p -> {
                collectSubtrees(p.left(), counts);
                collectSubtrees(p.right(), counts);
            }
            case Rec r -> collectSubtrees(r.body(), counts);
            case Sequence seq -> {
                collectSubtrees(seq.left(), counts);
                collectSubtrees(seq.right(), counts);
            }
            case Chain ch -> collectSubtrees(ch.cont(), counts);
        }
    }

    // -----------------------------------------------------------------------
    // Compression
    // -----------------------------------------------------------------------

    /**
     * Compress a session type by replacing shared subtrees with {@link Rec}/{@link Var}
     * definitions.
     *
     * <p>Each shared subtree is wrapped in {@code rec SN . body} at its first
     * occurrence and replaced with {@code Var("SN")} at subsequent occurrences.
     *
     * @param node the session type to compress
     * @return the compressed session type
     */
    public static SessionType compress(SessionType node) {
        var shared = findSharedSubtrees(node);
        if (shared.isEmpty()) {
            return node;
        }

        // Sort candidates by size descending
        var candidates = new ArrayList<>(shared.keySet());
        candidates.sort(Comparator.comparingInt(CompressionChecker::astSize).reversed());

        // Assign names
        var nameMap = new LinkedHashMap<SessionType, String>();
        for (int i = 0; i < candidates.size(); i++) {
            nameMap.put(candidates.get(i), "S" + i);
        }

        // Filter dominated subtrees
        var filtered = filterDominated(nameMap);
        if (filtered.isEmpty()) {
            return node;
        }

        // Track which subtrees have been seen (first occurrence gets Rec wrapper)
        var seen = new HashSet<SessionType>();
        return replaceShared(node, filtered, seen);
    }

    /**
     * Remove shared subtrees that are proper subtrees of larger shared subtrees.
     */
    private static Map<SessionType, String> filterDominated(Map<SessionType, String> shared) {
        var entries = new ArrayList<>(shared.entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<SessionType, String> e) ->
                astSize(e.getKey())).reversed());

        var kept = new LinkedHashMap<SessionType, String>();
        var keptNodes = new ArrayList<SessionType>();

        for (var entry : entries) {
            boolean isDominated = false;
            for (var parent : keptNodes) {
                if (contains(parent, entry.getKey())) {
                    isDominated = true;
                    break;
                }
            }
            if (!isDominated) {
                kept.put(entry.getKey(), entry.getValue());
                keptNodes.add(entry.getKey());
            }
        }
        return kept;
    }

    /**
     * Check if target appears as a proper subtree of parent.
     */
    private static boolean contains(SessionType parent, SessionType target) {
        if (parent.equals(target)) return false; // not a PROPER subtree

        return switch (parent) {
            case End ignored -> false;
            case Var ignored -> false;
            case Branch b -> b.choices().stream()
                    .anyMatch(c -> c.body().equals(target) || contains(c.body(), target));
            case Select s -> s.choices().stream()
                    .anyMatch(c -> c.body().equals(target) || contains(c.body(), target));
            case Parallel p ->
                    p.left().equals(target) || contains(p.left(), target)
                            || p.right().equals(target) || contains(p.right(), target);
            case Rec r -> r.body().equals(target) || contains(r.body(), target);
            case Sequence seq ->
                    seq.left().equals(target) || contains(seq.left(), target)
                            || seq.right().equals(target) || contains(seq.right(), target);
            case Chain ch -> ch.cont().equals(target) || contains(ch.cont(), target);
        };
    }

    /**
     * Replace shared subtrees: first occurrence gets {@code Rec(name, body)},
     * subsequent occurrences get {@code Var(name)}.
     */
    private static SessionType replaceShared(
            SessionType node,
            Map<SessionType, String> sharedMap,
            Set<SessionType> seen) {

        if (sharedMap.containsKey(node)) {
            String name = sharedMap.get(node);
            if (seen.contains(node)) {
                return new Var(name);
            }
            seen.add(node);
            // Build the body with further replacements
            SessionType body = replaceChildren(node, sharedMap, seen);
            return new Rec(name, body);
        }

        return replaceChildren(node, sharedMap, seen);
    }

    private static SessionType replaceChildren(
            SessionType node,
            Map<SessionType, String> sharedMap,
            Set<SessionType> seen) {

        return switch (node) {
            case End e -> e;
            case Var v -> v;
            case Branch b -> {
                var newChoices = b.choices().stream()
                        .map(c -> new Branch.Choice(c.label(), replaceShared(c.body(), sharedMap, seen)))
                        .toList();
                yield new Branch(newChoices);
            }
            case Select s -> {
                var newChoices = s.choices().stream()
                        .map(c -> new Branch.Choice(c.label(), replaceShared(c.body(), sharedMap, seen)))
                        .toList();
                yield new Select(newChoices);
            }
            case Parallel p -> new Parallel(
                    replaceShared(p.left(), sharedMap, seen),
                    replaceShared(p.right(), sharedMap, seen));
            case Rec r -> new Rec(r.var(), replaceShared(r.body(), sharedMap, seen));
            case Sequence seq -> new Sequence(
                    replaceShared(seq.left(), sharedMap, seen),
                    replaceShared(seq.right(), sharedMap, seen));
            case Chain ch -> new Chain(
                    ch.method(),
                    replaceShared(ch.cont(), sharedMap, seen));
        };
    }

    // -----------------------------------------------------------------------
    // Metrics
    // -----------------------------------------------------------------------

    /**
     * Compute the compression ratio: originalSize / compressedSize.
     *
     * <p>A ratio > 1.0 means sharing was found. A ratio of 1.0 means no sharing.
     */
    public static double compressionRatio(SessionType node) {
        int origSize = astSize(node);
        SessionType compressed = compress(node);
        int compSize = astSize(compressed);
        if (compSize == 0) return 1.0;
        return (double) origSize / compSize;
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    /**
     * Full compression analysis: compress + compute metrics.
     *
     * @param node the session type to analyse
     * @return a {@link CompressionResult} with compressed type and metrics
     */
    public static CompressionResult analyze(SessionType node) {
        int origSize = astSize(node);
        var shared = findSharedSubtrees(node);

        // Filter dominated to get actual count
        var nameMap = new LinkedHashMap<SessionType, String>();
        var candidates = new ArrayList<>(shared.keySet());
        candidates.sort(Comparator.comparingInt(CompressionChecker::astSize).reversed());
        for (int i = 0; i < candidates.size(); i++) {
            nameMap.put(candidates.get(i), "S" + i);
        }
        var filtered = filterDominated(nameMap);

        SessionType compressed = compress(node);
        int compSize = astSize(compressed);
        double ratio = compSize == 0 ? 1.0 : (double) origSize / compSize;

        return new CompressionResult(compressed, origSize, compSize, ratio, filtered.size());
    }
}
