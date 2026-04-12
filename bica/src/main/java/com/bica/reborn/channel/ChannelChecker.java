package com.bica.reborn.channel;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.duality.DualityChecker;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.morphism.Morphism;
import com.bica.reborn.morphism.MorphismChecker;
import com.bica.reborn.statespace.ProductStateSpace;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;

/**
 * Channel duality checker (Step 157a port).
 *
 * <p>Given a session type S (role A's view), computes {@code dual(S)} (role B's view),
 * constructs the channel product {@code L(S) × L(dual(S))}, and verifies:
 * <ul>
 *   <li>Channel product is a lattice</li>
 *   <li>Duality isomorphism: {@code L(S) ≅ L(dual(S))}</li>
 *   <li>Branch complementarity: branch in L(S) ↔ selection in L(dual(S))</li>
 *   <li>Role embeddings: each role embeds into the product</li>
 * </ul>
 */
public final class ChannelChecker {

    private ChannelChecker() {}

    // =========================================================================
    // Build channel product
    // =========================================================================

    /**
     * Build the channel product {@code L(S) × L(dual(S))}.
     *
     * @param type the session type S (role A's view)
     * @return the product state space
     */
    public static StateSpace buildChannelProduct(SessionType type) {
        SessionType d = DualityChecker.dual(type);
        StateSpace ssA = StateSpaceBuilder.build(type);
        StateSpace ssB = StateSpaceBuilder.build(d);
        return ProductStateSpace.product(ssA, ssB);
    }

    // =========================================================================
    // Full channel duality check
    // =========================================================================

    /**
     * Check all channel duality properties for session type {@code type}.
     *
     * @param type the session type S (role A's view)
     * @return a {@link ChannelResult} with all verification outcomes
     */
    public static ChannelResult checkChannelDuality(SessionType type) {
        SessionType d = DualityChecker.dual(type);
        StateSpace ssA = StateSpaceBuilder.build(type);
        StateSpace ssB = StateSpaceBuilder.build(d);
        StateSpace channelSS = ProductStateSpace.product(ssA, ssB);

        // 1. Lattice check on channel product
        LatticeResult latticeResult = LatticeChecker.checkLattice(channelSS);

        // 2. Isomorphism check
        Morphism iso = MorphismChecker.findIsomorphism(ssA, ssB);

        // 3. Branch complementarity
        boolean complementary = false;
        if (iso != null) {
            complementary = checkBranchComplementarity(ssA, ssB, iso.mapping());
        }

        return new ChannelResult(
                latticeResult.isLattice(),
                iso != null,
                complementary,
                channelSS,
                ssA,
                ssB);
    }

    // =========================================================================
    // Role embedding
    // =========================================================================

    /**
     * Compute the role embedding {@code ι: L(S) → L(S) × L(dual(S))}.
     *
     * <p>For role "A" (left), maps {@code s ↦ (s, ⊤_B)}.
     * For role "B" (right), maps {@code s ↦ (⊤_A, s)}.
     *
     * @param type the session type S
     * @param role "A" for the left inclusion, "B" for the right inclusion
     * @return the embedding mapping from local state IDs to product state IDs
     */
    public static Map<Integer, Integer> roleEmbedding(SessionType type, String role) {
        SessionType d = DualityChecker.dual(type);
        StateSpace ssA = StateSpaceBuilder.build(type);
        StateSpace ssB = StateSpaceBuilder.build(d);

        // Reconstruct pair-to-id mapping (same order as ProductStateSpace)
        Map<Long, Integer> pairToId = new HashMap<>();
        int nextId = 0;
        for (int s1 : ssA.states()) {
            for (int s2 : ssB.states()) {
                pairToId.put(packPair(s1, s2), nextId++);
            }
        }

        Map<Integer, Integer> embedding = new HashMap<>();
        if ("A".equals(role)) {
            // ι_A(s) = (s, ⊤_B)
            int fixedB = ssB.top();
            for (int s : ssA.states()) {
                embedding.put(s, pairToId.get(packPair(s, fixedB)));
            }
        } else {
            // ι_B(s) = (⊤_A, s)
            int fixedA = ssA.top();
            for (int s : ssB.states()) {
                embedding.put(s, pairToId.get(packPair(fixedA, s)));
            }
        }

        return Map.copyOf(embedding);
    }

    // =========================================================================
    // Internal: branch complementarity
    // =========================================================================

    /**
     * Verify that branch/selection annotations are complementary under the isomorphism.
     *
     * <p>For each transition in L(S), if it is a branch, the corresponding transition
     * in L(dual(S)) should be a selection, and vice versa.
     */
    static boolean checkBranchComplementarity(
            StateSpace ssA, StateSpace ssB, Map<Integer, Integer> iso) {

        // Build transition lookup for the dual state space
        Map<String, Boolean> dualTransLookup = new HashMap<>();
        for (var t : ssB.transitions()) {
            String key = t.source() + ":" + t.label() + ":" + t.target();
            dualTransLookup.put(key, t.kind() == TransitionKind.SELECTION);
        }

        for (var t : ssA.transitions()) {
            boolean isSelA = t.kind() == TransitionKind.SELECTION;
            int mappedSrc = iso.get(t.source());
            int mappedTgt = iso.get(t.target());

            String key = mappedSrc + ":" + t.label() + ":" + mappedTgt;
            Boolean isSelB = dualTransLookup.get(key);

            if (isSelB == null) return false; // no corresponding transition
            if (isSelA == isSelB) return false; // not complementary
        }

        return true;
    }

    private static long packPair(int a, int b) {
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }
}
