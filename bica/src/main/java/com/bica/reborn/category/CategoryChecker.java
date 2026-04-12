package com.bica.reborn.category;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.statespace.ProductStateSpace;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Category-theoretic checks for the category <b>SessLat</b> of session-type lattices.
 *
 * <p>Objects are state-space lattices; morphisms are lattice homomorphisms
 * (maps preserving meets and joins). The parallel constructor {@code ∥}
 * produces categorical products: {@code L(S₁ ∥ S₂) = L(S₁) × L(S₂)}
 * satisfies the universal property with canonical projections.
 */
public final class CategoryChecker {

    private CategoryChecker() {}

    // =========================================================================
    // Homomorphism check
    // =========================================================================

    /**
     * Check if {@code mapping} is a lattice homomorphism from {@code src} to {@code tgt}.
     *
     * <p>Both source and target must be lattices. The mapping must cover all source
     * states, land in target states, and preserve meets and joins.
     */
    public static boolean isLatticeHomomorphism(
            StateSpace src, StateSpace tgt, Map<Integer, Integer> mapping) {

        // Validate mapping covers all source states and lands in target
        for (int s : src.states()) {
            if (!mapping.containsKey(s)) return false;
            if (!tgt.states().contains(mapping.get(s))) return false;
        }

        LatticeResult resultSrc = LatticeChecker.checkLattice(src);
        LatticeResult resultTgt = LatticeChecker.checkLattice(tgt);
        if (!resultSrc.isLattice() || !resultTgt.isLattice()) return false;

        // Check meet preservation: f(a ∧ b) = f(a) ∧ f(b)
        if (!checkMeetPreservation(src, tgt, mapping, resultSrc.sccMap(), resultTgt.sccMap())) {
            return false;
        }

        // Check join preservation: f(a ∨ b) = f(a) ∨ f(b)
        return checkJoinPreservation(src, tgt, mapping, resultSrc.sccMap(), resultTgt.sccMap());
    }

    // =========================================================================
    // Identity morphism
    // =========================================================================

    /**
     * Return the identity lattice homomorphism on {@code ss}.
     */
    public static LatticeHomomorphism identityMorphism(StateSpace ss) {
        Map<Integer, Integer> id = new HashMap<>();
        for (int s : ss.states()) {
            id.put(s, s);
        }
        return new LatticeHomomorphism(id, ss, ss, true, true);
    }

    // =========================================================================
    // Composition
    // =========================================================================

    /**
     * Compose two lattice homomorphisms: {@code g ∘ f}.
     *
     * <p>Requires {@code f.target()} states to be compatible with {@code g.source()}.
     *
     * @throws IllegalArgumentException if domains are incompatible
     */
    public static LatticeHomomorphism compose(
            LatticeHomomorphism f, LatticeHomomorphism g) {

        Map<Integer, Integer> composed = new HashMap<>();
        for (int s : f.source().states()) {
            int fs = f.mapping().get(s);
            if (!g.mapping().containsKey(fs)) {
                throw new IllegalArgumentException(
                        "f.target state " + fs + " not in g.source mapping");
            }
            composed.put(s, g.mapping().get(fs));
        }

        return new LatticeHomomorphism(
                composed, f.source(), g.target(),
                f.preservesMeets() && g.preservesMeets(),
                f.preservesJoins() && g.preservesJoins());
    }

    // =========================================================================
    // Projections from product state spaces
    // =========================================================================

    /**
     * Extract projection homomorphisms {@code π₁, π₂} from a product state space.
     *
     * <p>The product must have been built via {@link ProductStateSpace#product(StateSpace, StateSpace)}.
     * The projections map each product state {@code (s₁, s₂)} to its components.
     *
     * @param product the product state space
     * @param factor1 left factor
     * @param factor2 right factor
     * @return list of two projection homomorphisms [π₁, π₂]
     */
    public static List<LatticeHomomorphism> findProjections(
            StateSpace product, StateSpace factor1, StateSpace factor2) {

        // Reconstruct pair-to-id mapping (same order as ProductStateSpace)
        Map<Long, Integer> pairToId = new HashMap<>();
        Map<Integer, long[]> idToPair = new HashMap<>();
        int nextId = 0;
        for (int s1 : factor1.states()) {
            for (int s2 : factor2.states()) {
                int sid = nextId++;
                long key = packPair(s1, s2);
                pairToId.put(key, sid);
                idToPair.put(sid, new long[]{s1, s2});
            }
        }

        // Build projection mappings
        Map<Integer, Integer> pi1Map = new HashMap<>();
        Map<Integer, Integer> pi2Map = new HashMap<>();
        for (int sid : product.states()) {
            long[] pair = idToPair.get(sid);
            if (pair == null) {
                throw new IllegalArgumentException(
                        "product state " + sid + " has no coordinate mapping");
            }
            pi1Map.put(sid, (int) pair[0]);
            pi2Map.put(sid, (int) pair[1]);
        }

        LatticeHomomorphism pi1 = new LatticeHomomorphism(
                pi1Map, product, factor1, true, true);
        LatticeHomomorphism pi2 = new LatticeHomomorphism(
                pi2Map, product, factor2, true, true);

        return List.of(pi1, pi2);
    }

    // =========================================================================
    // Universal property check
    // =========================================================================

    /**
     * Verify the categorical product universal property for a product state space.
     *
     * <p>Checks:
     * <ol>
     *   <li>Projections exist and are lattice homomorphisms</li>
     *   <li>The mediating morphism {@code ⟨π₁, π₂⟩} equals the identity</li>
     *   <li>Projections preserve top and bottom</li>
     * </ol>
     */
    public static boolean checkProductUniversalProperty(
            StateSpace product, StateSpace factor1, StateSpace factor2) {

        try {
            List<LatticeHomomorphism> projections = findProjections(product, factor1, factor2);

            // Verify each projection is a lattice homomorphism
            for (LatticeHomomorphism pi : projections) {
                if (!isLatticeHomomorphism(pi.source(), pi.target(), pi.mapping())) {
                    return false;
                }
            }

            // Verify ⟨π₁, π₂⟩ = id: reconstructing product state from projections
            // gives back the same state
            Map<Long, Integer> pairToId = new HashMap<>();
            int nextId = 0;
            for (int s1 : factor1.states()) {
                for (int s2 : factor2.states()) {
                    pairToId.put(packPair(s1, s2), nextId++);
                }
            }

            LatticeHomomorphism pi1 = projections.get(0);
            LatticeHomomorphism pi2 = projections.get(1);

            for (int sid : product.states()) {
                int c1 = pi1.mapping().get(sid);
                int c2 = pi2.mapping().get(sid);
                Integer reconstructed = pairToId.get(packPair(c1, c2));
                if (reconstructed == null || reconstructed != sid) {
                    return false;
                }
            }

            // Verify projections preserve top and bottom
            if (pi1.mapping().get(product.top()) != factor1.top()) return false;
            if (pi1.mapping().get(product.bottom()) != factor1.bottom()) return false;
            if (pi2.mapping().get(product.top()) != factor2.top()) return false;
            if (pi2.mapping().get(product.bottom()) != factor2.bottom()) return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================================
    // Product detection
    // =========================================================================

    /**
     * Check if {@code ss} decomposes as a product of two smaller lattices.
     *
     * <p>A state space is a product if its state count equals the product of
     * two factors' counts and the product construction reproduces it.
     * This is a heuristic check: it tries all factor-size splits.
     */
    public static boolean isProduct(StateSpace ss) {
        int n = ss.states().size();
        if (n < 4) return false; // need at least 2x2

        // Try all factor splits
        for (int f1Size = 2; f1Size <= n / 2; f1Size++) {
            if (n % f1Size != 0) continue;
            int f2Size = n / f1Size;
            if (f2Size < 2) continue;

            // Check if any pair of factor state spaces produces this product
            // For now, check the structural property: all states have consistent
            // out-degrees compatible with a product
            if (checkProductStructure(ss, f1Size, f2Size)) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private static boolean checkMeetPreservation(
            StateSpace src, StateSpace tgt, Map<Integer, Integer> mapping,
            Map<Integer, Integer> sccSrc, Map<Integer, Integer> sccTgt) {

        List<Integer> reps = new ArrayList<>(new TreeSet<>(sccSrc.values()));
        for (int i = 0; i < reps.size(); i++) {
            int a = reps.get(i);
            for (int j = i; j < reps.size(); j++) {
                int b = reps.get(j);
                Integer mSrc = LatticeChecker.computeMeet(src, a, b);
                if (mSrc == null) continue;
                int fa = mapping.get(a);
                int fb = mapping.get(b);
                Integer mTgt = LatticeChecker.computeMeet(tgt, fa, fb);
                int fm = mapping.get(mSrc);
                if (mTgt == null) return false;
                Integer fmScc = sccTgt.get(fm);
                Integer mTgtScc = sccTgt.get(mTgt);
                if (!Objects.equals(fmScc, mTgtScc)) return false;
            }
        }
        return true;
    }

    private static boolean checkJoinPreservation(
            StateSpace src, StateSpace tgt, Map<Integer, Integer> mapping,
            Map<Integer, Integer> sccSrc, Map<Integer, Integer> sccTgt) {

        List<Integer> reps = new ArrayList<>(new TreeSet<>(sccSrc.values()));
        for (int i = 0; i < reps.size(); i++) {
            int a = reps.get(i);
            for (int j = i; j < reps.size(); j++) {
                int b = reps.get(j);
                Integer jSrc = LatticeChecker.computeJoin(src, a, b);
                if (jSrc == null) continue;
                int fa = mapping.get(a);
                int fb = mapping.get(b);
                Integer jTgt = LatticeChecker.computeJoin(tgt, fa, fb);
                int fj = mapping.get(jSrc);
                if (jTgt == null) return false;
                Integer fjScc = sccTgt.get(fj);
                Integer jTgtScc = sccTgt.get(jTgt);
                if (!Objects.equals(fjScc, jTgtScc)) return false;
            }
        }
        return true;
    }

    private static boolean checkProductStructure(StateSpace ss, int f1Size, int f2Size) {
        // A product of f1 x f2 lattices has |states| = f1 * f2
        // and bottom should have zero out-degree, top should have at most
        // (outDeg factor1 top + outDeg factor2 top) transitions.
        // This is a necessary but not sufficient structural check.
        int expectedStates = f1Size * f2Size;
        if (ss.states().size() != expectedStates) return false;

        // Check that bottom has no outgoing transitions
        boolean bottomSink = ss.enabled(ss.bottom()).isEmpty();
        if (!bottomSink) return false;

        // The top should have outgoing transitions
        int topOut = ss.enabled(ss.top()).size();
        return topOut >= 2; // at least one from each factor
    }

    private static long packPair(int a, int b) {
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }
}
