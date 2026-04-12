package com.bica.reborn.endomorphism;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;

/**
 * Transitions as lattice endomorphisms (Step 10).
 *
 * <p>For each transition label m in L(S), the partial function f_m maps each
 * state s to the unique target t where s --m--&gt; t (if the transition exists).
 *
 * <p>This checker verifies whether these partial functions are:
 * <ol>
 *   <li><b>Order-preserving:</b> s₁ ≥ s₂ ⟹ f_m(s₁) ≥ f_m(s₂)</li>
 *   <li><b>Meet-preserving:</b> f_m(s₁ ∧ s₂) = f_m(s₁) ∧ f_m(s₂)</li>
 *   <li><b>Join-preserving:</b> f_m(s₁ ∨ s₂) = f_m(s₁) ∨ f_m(s₂)</li>
 * </ol>
 *
 * <p>A partial function that preserves meets and joins is a lattice homomorphism.
 * A lattice endomorphism is a lattice homomorphism from a lattice to itself.
 */
public final class EndomorphismChecker {

    private EndomorphismChecker() {}

    // =========================================================================
    // Transition map extraction
    // =========================================================================

    /**
     * Extract the partial function for each transition label.
     *
     * <p>For label m, f_m(s) = t iff (s, m, t) ∈ transitions.
     */
    public static List<TransitionMap> extractTransitionMaps(StateSpace ss) {
        Map<String, Map<Integer, Integer>> labelMaps = new LinkedHashMap<>();
        Map<String, Boolean> labelIsSel = new HashMap<>();

        for (var t : ss.transitions()) {
            labelMaps.computeIfAbsent(t.label(), k -> new LinkedHashMap<>())
                    .put(t.source(), t.target());
            if (t.kind() == TransitionKind.SELECTION) {
                labelIsSel.put(t.label(), true);
            } else {
                labelIsSel.putIfAbsent(t.label(), false);
            }
        }

        List<TransitionMap> result = new ArrayList<>();
        List<String> sortedLabels = new ArrayList<>(labelMaps.keySet());
        Collections.sort(sortedLabels);

        for (String label : sortedLabels) {
            Map<Integer, Integer> mapping = labelMaps.get(label);
            result.add(new TransitionMap(
                    label, mapping, mapping.size(),
                    labelIsSel.getOrDefault(label, false)));
        }
        return result;
    }

    // =========================================================================
    // Endomorphism checking
    // =========================================================================

    /**
     * Check whether a transition map is an order-preserving / lattice endomorphism.
     */
    public static EndomorphismResult checkEndomorphism(StateSpace ss, TransitionMap tmap) {
        Map<Integer, Integer> f = tmap.mapping();
        List<Integer> domain = new ArrayList<>(f.keySet());
        Collections.sort(domain);

        // Order-preserving: for all a, b in domain where a ≥ b, f(a) ≥ f(b)
        boolean orderOk = true;
        int[] orderCx = null;
        outer1:
        for (int a : domain) {
            Set<Integer> reachA = ss.reachableFrom(a);
            for (int b : domain) {
                if (b != a && reachA.contains(b)) {
                    int fa = f.get(a), fb = f.get(b);
                    if (fa != fb && !ss.reachableFrom(fa).contains(fb)) {
                        orderOk = false;
                        orderCx = new int[]{a, b};
                        break outer1;
                    }
                }
            }
        }

        // Meet-preserving: f(a ∧ b) = f(a) ∧ f(b)
        boolean meetOk = true;
        int[] meetCx = null;
        outer2:
        for (int i = 0; i < domain.size(); i++) {
            int a = domain.get(i);
            for (int j = i + 1; j < domain.size(); j++) {
                int b = domain.get(j);
                Integer mAb = LatticeChecker.computeMeet(ss, a, b);
                if (mAb != null && f.containsKey(mAb)) {
                    int fa = f.get(a), fb = f.get(b);
                    int fMeet = f.get(mAb);
                    Integer meetFaFb = LatticeChecker.computeMeet(ss, fa, fb);
                    if (meetFaFb == null || meetFaFb != fMeet) {
                        meetOk = false;
                        meetCx = new int[]{a, b};
                        break outer2;
                    }
                }
            }
        }

        // Join-preserving: f(a ∨ b) = f(a) ∨ f(b)
        boolean joinOk = true;
        int[] joinCx = null;
        outer3:
        for (int i = 0; i < domain.size(); i++) {
            int a = domain.get(i);
            for (int j = i + 1; j < domain.size(); j++) {
                int b = domain.get(j);
                Integer jAb = LatticeChecker.computeJoin(ss, a, b);
                if (jAb != null && f.containsKey(jAb)) {
                    int fa = f.get(a), fb = f.get(b);
                    int fJoin = f.get(jAb);
                    Integer joinFaFb = LatticeChecker.computeJoin(ss, fa, fb);
                    if (joinFaFb == null || joinFaFb != fJoin) {
                        joinOk = false;
                        joinCx = new int[]{a, b};
                        break outer3;
                    }
                }
            }
        }

        return new EndomorphismResult(
                tmap.label(), tmap.domainSize(),
                orderOk, meetOk, joinOk,
                meetOk && joinOk,
                orderCx, meetCx, joinCx);
    }

    /**
     * Check endomorphism properties for all transition labels.
     */
    public static EndomorphismSummary checkAll(StateSpace ss) {
        List<TransitionMap> tmaps = extractTransitionMaps(ss);
        List<EndomorphismResult> results = new ArrayList<>();
        for (TransitionMap tm : tmaps) {
            results.add(checkEndomorphism(ss, tm));
        }

        return new EndomorphismSummary(
                results,
                results.stream().allMatch(EndomorphismResult::isOrderPreserving),
                results.stream().allMatch(EndomorphismResult::isMeetPreserving),
                results.stream().allMatch(EndomorphismResult::isJoinPreserving),
                results.stream().allMatch(EndomorphismResult::isEndomorphism),
                results.size());
    }
}
