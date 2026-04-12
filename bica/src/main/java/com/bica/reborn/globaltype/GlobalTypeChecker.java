package com.bica.reborn.globaltype;

import com.bica.reborn.statespace.ProductStateSpace;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;

/**
 * Checker for multiparty global types: role extraction and state-space construction.
 */
public final class GlobalTypeChecker {

    private GlobalTypeChecker() {}

    // -----------------------------------------------------------------------
    // Role extraction
    // -----------------------------------------------------------------------

    /**
     * Extract the set of all role names from a global type.
     */
    public static Set<String> roles(GlobalType g) {
        Set<String> result = new HashSet<>();
        collectRoles(g, result);
        return Collections.unmodifiableSet(result);
    }

    private static void collectRoles(GlobalType g, Set<String> result) {
        switch (g) {
            case GEnd e -> {}
            case GVar v -> {}
            case GMessage m -> {
                result.add(m.sender());
                result.add(m.receiver());
                for (GMessage.Choice c : m.choices()) {
                    collectRoles(c.body(), result);
                }
            }
            case GParallel p -> {
                collectRoles(p.left(), result);
                collectRoles(p.right(), result);
            }
            case GRec r -> collectRoles(r.body(), result);
        }
    }

    // -----------------------------------------------------------------------
    // State-space construction
    // -----------------------------------------------------------------------

    /**
     * Build the state space of a global type.
     *
     * <p>Transition labels are role-annotated: {@code "sender->receiver:method"}.
     * The resulting StateSpace can be checked for lattice properties.
     */
    public static StateSpace buildStateSpace(GlobalType g) {
        Builder builder = new Builder();
        return builder.build(g);
    }

    private static final class Builder {
        private int nextId = 0;
        private final Map<Integer, String> states = new HashMap<>();
        private final List<Transition> transitions = new ArrayList<>();

        private int fresh(String label) {
            int id = nextId++;
            states.put(id, label);
            return id;
        }

        StateSpace build(GlobalType g) {
            int endId = fresh("end");
            int topId = buildNode(g, Map.of(), endId);

            Set<Integer> reachable = reachable(topId);
            List<Transition> reachableTransitions = transitions.stream()
                    .filter(t -> reachable.contains(t.source()))
                    .toList();
            Map<Integer, String> reachableLabels = new HashMap<>();
            for (int s : reachable) {
                if (states.containsKey(s)) {
                    reachableLabels.put(s, states.get(s));
                }
            }
            return new StateSpace(reachable, reachableTransitions,
                    topId, endId, reachableLabels);
        }

        private int buildNode(GlobalType g, Map<String, Integer> env, int endId) {
            return switch (g) {
                case GEnd e -> endId;

                case GVar v -> {
                    Integer target = env.get(v.name());
                    if (target == null) {
                        throw new IllegalArgumentException(
                                "unbound global type variable: " + v.name());
                    }
                    yield target;
                }

                case GMessage m -> {
                    String stateLabel;
                    if (m.choices().size() == 1) {
                        stateLabel = m.sender() + "->" + m.receiver()
                                + ":" + m.choices().getFirst().label();
                    } else {
                        String labels = m.choices().stream()
                                .map(GMessage.Choice::label)
                                .collect(java.util.stream.Collectors.joining(","));
                        stateLabel = m.sender() + "->" + m.receiver()
                                + ":{" + labels + "}";
                    }
                    int entry = fresh(stateLabel);
                    for (GMessage.Choice c : m.choices()) {
                        int target = buildNode(c.body(), env, endId);
                        String trLabel = m.sender() + "->" + m.receiver()
                                + ":" + c.label();
                        transitions.add(new Transition(entry, trLabel, target,
                                TransitionKind.METHOD));
                    }
                    yield entry;
                }

                case GParallel p -> buildParallel(p.left(), p.right(), endId);

                case GRec r -> {
                    int placeholder = fresh("rec_" + r.var());
                    Map<String, Integer> newEnv = new HashMap<>(env);
                    newEnv.put(r.var(), placeholder);
                    int bodyEntry = buildNode(r.body(), newEnv, endId);
                    if (bodyEntry != placeholder) {
                        merge(placeholder, bodyEntry);
                        yield bodyEntry;
                    }
                    yield placeholder;
                }
            };
        }

        private int buildParallel(GlobalType left, GlobalType right, int endId) {
            StateSpace leftSs = buildStateSpace(left);
            StateSpace rightSs = buildStateSpace(right);
            StateSpace prod = ProductStateSpace.product(leftSs, rightSs);

            Map<Integer, Integer> remap = new HashMap<>();
            for (int sid : prod.states()) {
                if (sid == prod.bottom()) {
                    remap.put(sid, endId);
                } else {
                    remap.put(sid, fresh(prod.labels().getOrDefault(sid, "?")));
                }
            }

            for (Transition t : prod.transitions()) {
                transitions.add(new Transition(
                        remap.get(t.source()),
                        t.label(),
                        remap.get(t.target()),
                        t.kind()));
            }

            return remap.get(prod.top());
        }

        private void merge(int oldId, int newId) {
            List<Transition> updated = new ArrayList<>(transitions.size());
            for (Transition t : transitions) {
                int src = t.source() == oldId ? newId : t.source();
                int tgt = t.target() == oldId ? newId : t.target();
                if (src != t.source() || tgt != t.target()) {
                    updated.add(new Transition(src, t.label(), tgt, t.kind()));
                } else {
                    updated.add(t);
                }
            }
            transitions.clear();
            transitions.addAll(updated);
            states.remove(oldId);
        }

        private Set<Integer> reachable(int start) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(start);
            while (!stack.isEmpty()) {
                int s = stack.pop();
                if (!visited.add(s)) continue;
                for (Transition t : transitions) {
                    if (t.source() == s) {
                        stack.push(t.target());
                    }
                }
            }
            return visited;
        }
    }
}
