package com.bica.reborn.endomorphism;

import java.util.List;
import java.util.Objects;

/**
 * Summary of endomorphism checks for all transition labels in a state space.
 *
 * @param results            per-label endomorphism results
 * @param allOrderPreserving true iff every label is order-preserving
 * @param allMeetPreserving  true iff every label is meet-preserving
 * @param allJoinPreserving  true iff every label is join-preserving
 * @param allEndomorphisms   true iff every label is a lattice endomorphism
 * @param numLabels          total number of distinct transition labels
 */
public record EndomorphismSummary(
        List<EndomorphismResult> results,
        boolean allOrderPreserving,
        boolean allMeetPreserving,
        boolean allJoinPreserving,
        boolean allEndomorphisms,
        int numLabels) {

    public EndomorphismSummary {
        Objects.requireNonNull(results, "results must not be null");
        results = List.copyOf(results);
    }
}
