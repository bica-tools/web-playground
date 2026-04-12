package com.bica.reborn.extensions;

import com.bica.reborn.statespace.StateSpace;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Result of replication analysis for S^n (n copies of S in parallel).
 *
 * <p>The replication operator is syntactic sugar for n-ary parallel:
 * {@code S^3 = (S || S || S)}. The product lattice theorem gives
 * {@code L(S^n) = L(S)^n}.
 *
 * @param replicated       the n-fold product state space
 * @param originalStates   number of states in L(S)
 * @param replicatedStates number of states in L(S^n)
 * @param count            the replication count n
 * @param diagonalStates   states where all n components are equal
 * @param orbitCount       number of distinct S_n symmetry orbits
 * @param symmetryReduction compression ratio: replicatedStates / orbitCount
 */
public record ReplicationResult(
        StateSpace replicated,
        int originalStates,
        int replicatedStates,
        int count,
        Set<List<Integer>> diagonalStates,
        int orbitCount,
        double symmetryReduction) {

    public ReplicationResult {
        Objects.requireNonNull(replicated, "replicated must not be null");
        Objects.requireNonNull(diagonalStates, "diagonalStates must not be null");
        diagonalStates = Set.copyOf(diagonalStates);
    }
}
