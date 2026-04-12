package com.bica.reborn.audio;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.statespace.StateSpace;

import java.util.List;
import java.util.Objects;

/**
 * Result of routing verification.
 *
 * @param isVerified         true iff every signal reaches master output (lattice holds)
 * @param sessionTypeString  the session type encoding of the routing graph
 * @param ast                parsed session type AST
 * @param stateSpace         constructed state space
 * @param latticeResult      lattice check result
 * @param deadSignals        list of node names that cannot reach master output
 * @param feedbackLoops      list of cycles (each a list of node names)
 * @param feedbackSafe       true iff all feedback loops are guarded
 * @param numNodes           number of nodes in the audio graph
 * @param numRoutes          number of routes
 */
public record AudioRoutingResult(
        boolean isVerified,
        String sessionTypeString,
        SessionType ast,
        StateSpace stateSpace,
        LatticeResult latticeResult,
        List<String> deadSignals,
        List<List<String>> feedbackLoops,
        boolean feedbackSafe,
        int numNodes,
        int numRoutes) {

    public AudioRoutingResult {
        Objects.requireNonNull(sessionTypeString);
        Objects.requireNonNull(ast);
        Objects.requireNonNull(stateSpace);
        Objects.requireNonNull(latticeResult);
        Objects.requireNonNull(deadSignals);
        Objects.requireNonNull(feedbackLoops);
        deadSignals = List.copyOf(deadSignals);
        feedbackLoops = feedbackLoops.stream().map(List::copyOf).toList();
    }
}
