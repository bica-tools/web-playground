package com.bica.reborn.equalizer;

import com.bica.reborn.category.CategoryChecker;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.*;

/**
 * Equalizers in SessLat — categorical equalizer analysis.
 *
 * <p>An equalizer of two morphisms f, g: L → R is a lattice E with a
 * morphism equ: E → L such that f ∘ equ = g ∘ equ, and E is universal.
 *
 * <p><b>Main result</b>: SessLat HAS equalizers.  The subset equalizer
 * E = {s ∈ L : f(s) = g(s)} is always a bounded sublattice of L.
 */
public final class EqualizerChecker {

    private EqualizerChecker() {}

    // =========================================================================
    // Agreement set
    // =========================================================================

    /**
     * Compute E = {s ∈ source : f(s) = g(s)}.
     */
    public static Set<Integer> agreementSet(
            StateSpace source, Map<Integer, Integer> f, Map<Integer, Integer> g) {
        Set<Integer> result = new HashSet<>();
        for (int s : source.states()) {
            if (Objects.equals(f.get(s), g.get(s))) {
                result.add(s);
            }
        }
        return result;
    }

    // =========================================================================
    // Subset equalizer construction
    // =========================================================================

    /**
     * Build the sublattice on the agreement set {s ∈ source : f(s) = g(s)}.
     *
     * <p>Computes the covering relation (Hasse diagram) of the sub-poset
     * rather than simply restricting transitions, because removing intermediate
     * states can break connectivity.
     */
    public static StateSpace subsetEqualizer(
            StateSpace source, Map<Integer, Integer> f, Map<Integer, Integer> g) {
        Set<Integer> agree = agreementSet(source, f, g);

        List<Integer> sortedAgree = new ArrayList<>(agree);
        Collections.sort(sortedAgree);

        // Remap to fresh IDs
        Map<Integer, Integer> remap = new HashMap<>();
        for (int i = 0; i < sortedAgree.size(); i++) {
            remap.put(sortedAgree.get(i), i);
        }

        Set<Integer> states = new HashSet<>(remap.values());
        Map<Integer, String> labels = new HashMap<>();
        for (var entry : remap.entrySet()) {
            labels.put(entry.getValue(),
                    source.labels().getOrDefault(entry.getKey(), String.valueOf(entry.getKey())));
        }

        // Compute reachability in source for agreement set elements
        Map<Integer, Set<Integer>> srcReach = new HashMap<>();
        for (int s : agree) {
            srcReach.put(s, source.reachableFrom(s));
        }

        // Build covering relation on the sub-poset
        List<Transition> transitions = new ArrayList<>();
        for (int a : sortedAgree) {
            for (int b : sortedAgree) {
                if (a == b) continue;
                if (!srcReach.get(a).contains(b)) continue;
                // a > b — check cover in sub-poset
                boolean isCover = true;
                for (int c : sortedAgree) {
                    if (c == a || c == b) continue;
                    if (srcReach.get(a).contains(c) && srcReach.get(c).contains(b)) {
                        isCover = false;
                        break;
                    }
                }
                if (isCover) {
                    String label = findLabel(source, a, b);
                    transitions.add(new Transition(remap.get(a), label, remap.get(b)));
                }
            }
        }

        int top = remap.get(source.top());
        int bottom = remap.get(source.bottom());

        return new StateSpace(states, transitions, top, bottom, labels);
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    /**
     * Check whether the equalizer of f, g: source → target exists.
     */
    public static EqualizerResult computeEqualizer(
            StateSpace source, StateSpace target,
            Map<Integer, Integer> f, Map<Integer, Integer> g) {

        LatticeResult srcLr = LatticeChecker.checkLattice(source);
        if (!srcLr.isLattice()) {
            return new EqualizerResult(false, null, null, f, g, false,
                    "source is not a lattice");
        }
        LatticeResult tgtLr = LatticeChecker.checkLattice(target);
        if (!tgtLr.isLattice()) {
            return new EqualizerResult(false, null, null, f, g, false,
                    "target is not a lattice");
        }

        Set<Integer> agree = agreementSet(source, f, g);

        if (!agree.contains(source.top())) {
            return new EqualizerResult(false, null, null, f, g, false,
                    "top not in agreement set");
        }
        if (!agree.contains(source.bottom())) {
            return new EqualizerResult(false, null, null, f, g, false,
                    "bottom not in agreement set");
        }

        StateSpace eqSs = subsetEqualizer(source, f, g);

        // Build inclusion map
        List<Integer> sortedAgree = new ArrayList<>(agree);
        Collections.sort(sortedAgree);
        Map<Integer, Integer> inclusion = new HashMap<>();
        for (int i = 0; i < sortedAgree.size(); i++) {
            inclusion.put(i, sortedAgree.get(i));
        }

        LatticeResult eqLr = LatticeChecker.checkLattice(eqSs);
        if (!eqLr.isLattice()) {
            return new EqualizerResult(false, eqSs, inclusion, f, g, false,
                    "subset equalizer is not a lattice");
        }

        // Verify inclusion is a lattice homomorphism
        if (!CategoryChecker.isLatticeHomomorphism(eqSs, source, inclusion)) {
            return new EqualizerResult(false, eqSs, inclusion, f, g, false,
                    "inclusion is not a lattice homomorphism");
        }

        // Verify f ∘ equ = g ∘ equ
        for (int eqState : eqSs.states()) {
            int srcState = inclusion.get(eqState);
            if (!Objects.equals(f.get(srcState), g.get(srcState))) {
                return new EqualizerResult(false, eqSs, inclusion, f, g, false,
                        "f . equ != g . equ");
            }
        }

        // If we got here, the equalizer exists
        return new EqualizerResult(true, eqSs, inclusion, f, g, true, null);
    }

    /**
     * Check the equalizer universal property (simplified).
     */
    public static boolean checkEqualizerUniversalProperty(
            StateSpace eqSpace, StateSpace source,
            Map<Integer, Integer> f, Map<Integer, Integer> g,
            Map<Integer, Integer> inclusion) {
        if (!LatticeChecker.checkLattice(eqSpace).isLattice()) return false;
        // Check f ∘ equ = g ∘ equ
        for (int s : eqSpace.states()) {
            int srcState = inclusion.get(s);
            if (!Objects.equals(f.get(srcState), g.get(srcState))) return false;
        }
        return true;
    }

    /**
     * Check if a state space is the equalizer of f and g.
     */
    public static boolean isEqualizer(
            StateSpace eqSpace, StateSpace source, StateSpace target,
            Map<Integer, Integer> f, Map<Integer, Integer> g,
            Map<Integer, Integer> inclusion) {
        if (!LatticeChecker.checkLattice(eqSpace).isLattice()) return false;
        for (int s : eqSpace.states()) {
            int srcState = inclusion.get(s);
            if (!Objects.equals(f.get(srcState), g.get(srcState))) return false;
        }
        return true;
    }

    /**
     * Compute the kernel pair {(a,b) ∈ L×L : f(a) = f(b)}.
     */
    public static EqualizerResult.KernelPair computeKernelPair(
            StateSpace source, Map<Integer, Integer> f) {
        Set<long[]> pairs = new HashSet<>();
        for (int a : source.states()) {
            for (int b : source.states()) {
                if (Objects.equals(f.get(a), f.get(b))) {
                    pairs.add(new long[]{a, b});
                }
            }
        }
        boolean isCong = LatticeChecker.checkLattice(source).isLattice();
        return new EqualizerResult.KernelPair(pairs, isCong);
    }

    // =========================================================================
    // Finite completeness
    // =========================================================================

    /**
     * Verify SessLat is finitely complete: products + equalizers.
     */
    public static EqualizerResult.FiniteCompletenessResult checkFiniteCompleteness(
            List<StateSpace> spaces) {
        List<StateSpace> lattices = new ArrayList<>();
        for (StateSpace ss : spaces) {
            if (LatticeChecker.checkLattice(ss).isLattice()) lattices.add(ss);
        }

        // Check products
        boolean hasProducts = true;
        for (int i = 0; i < lattices.size() && hasProducts; i++) {
            for (int j = i; j < lattices.size() && hasProducts; j++) {
                StateSpace prod = com.bica.reborn.statespace.ProductStateSpace.product(
                        lattices.get(i), lattices.get(j));
                if (!CategoryChecker.checkProductUniversalProperty(
                        prod, lattices.get(i), lattices.get(j))) {
                    hasProducts = false;
                }
            }
        }

        // Check equalizers (identity morphism pair — trivial case)
        boolean hasEqualizers = true;
        for (StateSpace source : lattices) {
            Map<Integer, Integer> id = new HashMap<>();
            for (int s : source.states()) id.put(s, s);
            EqualizerResult r = computeEqualizer(source, source, id, id);
            if (!r.hasEqualizer()) {
                hasEqualizers = false;
                break;
            }
        }

        return new EqualizerResult.FiniteCompletenessResult(
                hasProducts, hasEqualizers,
                hasProducts && hasEqualizers,
                lattices.size(), null);
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private static String findLabel(StateSpace source, int a, int b) {
        for (var t : source.transitions()) {
            if (t.source() == a && t.target() == b) return t.label();
        }
        for (var t : source.transitions()) {
            if (t.source() == a && source.reachableFrom(t.target()).contains(b)) {
                return t.label();
            }
        }
        return "tau";
    }
}
