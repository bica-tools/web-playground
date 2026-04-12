package com.bica.reborn.statespace;

import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.*;

/**
 * Product construction for parallel session types.
 *
 * <p>Given two state spaces L1 and L2, constructs the product poset L1 x L2
 * with componentwise transitions. See spec Section 4.2.
 */
public final class ProductStateSpace {

    private ProductStateSpace() {}

    /**
     * Construct the product state space L1 x L2.
     *
     * <p>The result has {@code |L1| x |L2|} states, with top = (left.top, right.top)
     * and bottom = (left.bottom, right.bottom). Transitions advance one component at a time.
     */
    public static StateSpace product(StateSpace left, StateSpace right) {
        // Pre-compute adjacency lists (carry full Transition for kind propagation)
        Map<Integer, List<Transition>> leftAdj = new HashMap<>();
        for (int s : left.states()) {
            leftAdj.put(s, new ArrayList<>());
        }
        for (var t : left.transitions()) {
            leftAdj.get(t.source()).add(t);
        }

        Map<Integer, List<Transition>> rightAdj = new HashMap<>();
        for (int s : right.states()) {
            rightAdj.put(s, new ArrayList<>());
        }
        for (var t : right.transitions()) {
            rightAdj.get(t.source()).add(t);
        }

        // Assign fresh IDs to product states
        int nextId = 0;
        Map<Long, Integer> pairToId = new HashMap<>();
        Map<Integer, String> idLabels = new HashMap<>();

        for (int s1 : left.states()) {
            for (int s2 : right.states()) {
                int sid = nextId++;
                long key = packPair(s1, s2);
                pairToId.put(key, sid);
                String l1 = left.labels().getOrDefault(s1, String.valueOf(s1));
                String l2 = right.labels().getOrDefault(s2, String.valueOf(s2));
                idLabels.put(sid, "(" + l1 + ", " + l2 + ")");
            }
        }

        // Build transitions
        List<Transition> transitions = new ArrayList<>();

        for (int s1 : left.states()) {
            for (int s2 : right.states()) {
                int src = pairToId.get(packPair(s1, s2));
                // Left-component transitions: (s1, s2) --[l]--> (s1', s2)
                for (var lt : leftAdj.get(s1)) {
                    int tgt = pairToId.get(packPair(lt.target(), s2));
                    transitions.add(new Transition(src, lt.label(), tgt, lt.kind()));
                }
                // Right-component transitions: (s1, s2) --[l]--> (s1, s2')
                for (var rt : rightAdj.get(s2)) {
                    int tgt = pairToId.get(packPair(s1, rt.target()));
                    transitions.add(new Transition(src, rt.label(), tgt, rt.kind()));
                }
            }
        }

        int top = pairToId.get(packPair(left.top(), right.top()));
        int bottom = pairToId.get(packPair(left.bottom(), right.bottom()));

        return new StateSpace(
                new HashSet<>(pairToId.values()),
                transitions,
                top,
                bottom,
                idLabels);
    }

    private static long packPair(int a, int b) {
        return ((long) a << 32) | (b & 0xFFFFFFFFL);
    }
}
