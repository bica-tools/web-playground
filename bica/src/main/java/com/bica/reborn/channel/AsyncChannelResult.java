package com.bica.reborn.channel;

import java.util.Map;
import java.util.Objects;

/**
 * Result of async channel analysis for a session type S.
 *
 * @param typeStr              Pretty-printed S.
 * @param dualStr              Pretty-printed dual(S).
 * @param capacity             Buffer bound K.
 * @param senderStates         |L(S)|.
 * @param receiverStates       |L(dual(S))|.
 * @param asyncChannelStates   Reachable (sA, sB, bufAB, bufBA) states.
 * @param syncChannelStates    |L(S) x L(dual(S))| for comparison.
 * @param isLattice            True iff async state space is a lattice.
 * @param syncEmbeds           True iff sync channel embeds in async.
 * @param maxBufferOccupancy   Max total |bufAB| + |bufBA| seen.
 * @param bufferDistribution   total_buffer_length -> state_count.
 */
public record AsyncChannelResult(
        String typeStr,
        String dualStr,
        int capacity,
        int senderStates,
        int receiverStates,
        int asyncChannelStates,
        int syncChannelStates,
        boolean isLattice,
        boolean syncEmbeds,
        int maxBufferOccupancy,
        Map<Integer, Integer> bufferDistribution) {

    public AsyncChannelResult {
        Objects.requireNonNull(typeStr);
        Objects.requireNonNull(dualStr);
        Objects.requireNonNull(bufferDistribution);
        bufferDistribution = Map.copyOf(bufferDistribution);
    }
}
