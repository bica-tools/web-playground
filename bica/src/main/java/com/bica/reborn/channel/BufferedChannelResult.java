package com.bica.reborn.channel;

import com.bica.reborn.lattice.LatticeResult;

import java.util.Map;
import java.util.Objects;

/**
 * Result of buffered channel analysis for a session type.
 *
 * @param typeStr             Pretty-printed session type.
 * @param capacity            Buffer bound K.
 * @param unbufferedStates    Number of states in L(S) (synchronous).
 * @param bufferedStates      Number of states in the buffered state space.
 * @param bufferedTransitions Number of transitions in the buffered state space.
 * @param isLattice           True iff buffered state space is a lattice.
 * @param isBufferSafe        True iff no overflow or underflow in reachable states.
 * @param overflowStates      Number of overflow-prone states.
 * @param underflowStates     Number of underflow-prone states.
 * @param maxBufferOccupancy  Maximum buffer fill level observed.
 * @param bufferDistribution  buffer_length -> count of states with that fill.
 * @param deadlockStates      Number of deadlock states.
 * @param latticeResult       Full LatticeResult for detailed inspection.
 */
public record BufferedChannelResult(
        String typeStr,
        int capacity,
        int unbufferedStates,
        int bufferedStates,
        int bufferedTransitions,
        boolean isLattice,
        boolean isBufferSafe,
        int overflowStates,
        int underflowStates,
        int maxBufferOccupancy,
        Map<Integer, Integer> bufferDistribution,
        int deadlockStates,
        LatticeResult latticeResult) {

    public BufferedChannelResult {
        Objects.requireNonNull(typeStr);
        Objects.requireNonNull(bufferDistribution);
        Objects.requireNonNull(latticeResult);
        bufferDistribution = Map.copyOf(bufferDistribution);
    }
}
