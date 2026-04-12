package com.bica.reborn.invariants;

/**
 * Session-type-specific invariants beyond general lattice invariants.
 *
 * @param branchCount       number of Branch nodes in the AST
 * @param selectCount       number of Select nodes in the AST
 * @param parallelCount     number of Parallel nodes in the AST
 * @param recCount          number of Rec nodes in the AST
 * @param maxBranchWidth    maximum number of choices in any Branch
 * @param maxSelectWidth    maximum number of choices in any Select
 * @param maxDepth          maximum nesting depth of the AST
 * @param hasParallel       true if the type contains parallel composition
 * @param hasRecursion      true if the type contains rec
 * @param isTailRecursive   true if all recursive calls are in tail position
 * @param reconvergenceDegree number of states with in-degree > 1 in state space
 */
public record STInvariants(
        int branchCount,
        int selectCount,
        int parallelCount,
        int recCount,
        int maxBranchWidth,
        int maxSelectWidth,
        int maxDepth,
        boolean hasParallel,
        boolean hasRecursion,
        boolean isTailRecursive,
        int reconvergenceDegree) {
}
