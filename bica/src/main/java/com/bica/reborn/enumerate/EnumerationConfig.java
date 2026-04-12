package com.bica.reborn.enumerate;

import java.util.List;
import java.util.Objects;

/**
 * Configuration for exhaustive session type enumeration.
 */
public record EnumerationConfig(
        int maxDepth,
        List<String> labels,
        int maxBranchWidth,
        boolean includeParallel,
        boolean includeRecursion,
        boolean includeSelection,
        boolean requireTerminating) {

    public static final List<String> DEFAULT_LABELS = List.of("a", "b");

    public EnumerationConfig {
        Objects.requireNonNull(labels, "labels must not be null");
        labels = List.copyOf(labels);
    }

    /** Default configuration: depth 2, labels {a,b}, width 2, all features on. */
    public static EnumerationConfig defaults() {
        return new EnumerationConfig(2, DEFAULT_LABELS, 2,
                true, true, true, true);
    }

    /** Builder-style helpers. */
    public EnumerationConfig withMaxDepth(int d) {
        return new EnumerationConfig(d, labels, maxBranchWidth,
                includeParallel, includeRecursion, includeSelection, requireTerminating);
    }

    public EnumerationConfig withLabels(List<String> l) {
        return new EnumerationConfig(maxDepth, l, maxBranchWidth,
                includeParallel, includeRecursion, includeSelection, requireTerminating);
    }

    public EnumerationConfig withMaxBranchWidth(int w) {
        return new EnumerationConfig(maxDepth, labels, w,
                includeParallel, includeRecursion, includeSelection, requireTerminating);
    }

    public EnumerationConfig withIncludeParallel(boolean v) {
        return new EnumerationConfig(maxDepth, labels, maxBranchWidth,
                v, includeRecursion, includeSelection, requireTerminating);
    }

    public EnumerationConfig withIncludeRecursion(boolean v) {
        return new EnumerationConfig(maxDepth, labels, maxBranchWidth,
                includeParallel, v, includeSelection, requireTerminating);
    }

    public EnumerationConfig withIncludeSelection(boolean v) {
        return new EnumerationConfig(maxDepth, labels, maxBranchWidth,
                includeParallel, includeRecursion, v, requireTerminating);
    }

    public EnumerationConfig withRequireTerminating(boolean v) {
        return new EnumerationConfig(maxDepth, labels, maxBranchWidth,
                includeParallel, includeRecursion, includeSelection, v);
    }
}
