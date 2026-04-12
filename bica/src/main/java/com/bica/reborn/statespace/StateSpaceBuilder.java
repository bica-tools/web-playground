package com.bica.reborn.statespace;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Constructs a {@link StateSpace} (labeled transition system) from a session type AST.
 *
 * <p>Handles all constructors: {@code End}, {@code Var}, {@code Branch}, {@code Select},
 * {@code Rec} (with cycles), {@code Sequence} (chaining), and {@code Parallel}
 * (product construction via {@link ProductStateSpace}).
 */
public final class StateSpaceBuilder {

    private StateSpaceBuilder() {}

    /**
     * Construct the labeled transition system for a session type.
     *
     * @throws IllegalArgumentException on unbound type variables
     */
    public static StateSpace build(SessionType sessionType) {
        var builder = new Builder();
        return builder.build(sessionType);
    }

    private static final class Builder {

        private int nextId = 0;
        private final Map<Integer, String> states = new HashMap<>();
        private final List<Transition> transitions = new ArrayList<>();

        private int fresh(String label) {
            int sid = nextId++;
            states.put(sid, label);
            return sid;
        }

        StateSpace build(SessionType node) {
            int endId = fresh("end");
            int topId = buildNode(node, Map.of(), endId);

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

            return new StateSpace(reachable, reachableTransitions, topId, endId, reachableLabels);
        }

        private int buildNode(SessionType node, Map<String, Integer> env, int endId) {
            return switch (node) {
                case End ignored -> endId;

                case Var v -> {
                    if (!env.containsKey(v.name())) {
                        throw new IllegalArgumentException(
                                "unbound type variable: '" + v.name() + "'");
                    }
                    yield env.get(v.name());
                }

                case Branch b -> {
                    String label;
                    if (b.choices().size() == 1) {
                        label = b.choices().getFirst().label();
                    } else {
                        label = "&{" + b.choices().stream()
                                .map(Choice::label)
                                .collect(Collectors.joining(", ")) + "}";
                    }
                    int entry = fresh(label);
                    for (var choice : b.choices()) {
                        int target = buildNode(choice.body(), env, endId);
                        transitions.add(new Transition(entry, choice.label(), target));
                    }
                    yield entry;
                }

                case Select s -> {
                    String label = "+{" + s.choices().stream()
                            .map(Choice::label)
                            .collect(Collectors.joining(", ")) + "}";
                    int entry = fresh(label);
                    for (var choice : s.choices()) {
                        int target = buildNode(choice.body(), env, endId);
                        transitions.add(new Transition(entry, choice.label(), target, TransitionKind.SELECTION));
                    }
                    yield entry;
                }

                case Rec r -> {
                    int placeholder = fresh("rec_" + r.var());
                    var newEnv = new HashMap<>(env);
                    newEnv.put(r.var(), placeholder);
                    int bodyEntry = buildNode(r.body(), newEnv, endId);
                    if (bodyEntry != placeholder) {
                        merge(placeholder, bodyEntry);
                        yield bodyEntry;
                    }
                    yield placeholder;
                }

                case Sequence seq -> {
                    int rightEntry = buildNode(seq.right(), env, endId);
                    int leftEntry = buildNode(seq.left(), env, rightEntry);
                    yield leftEntry;
                }

                case Chain ch -> {
                    // T2b (2026-04-11): Chain(m, S) is a labelled one-step,
                    // state-space equivalent to single-arm Branch but syntactically
                    // distinct. See ast/Chain.java.
                    int target = buildNode(ch.cont(), env, endId);
                    int entry = fresh(ch.method());
                    transitions.add(new Transition(entry, ch.method(), target));
                    yield entry;
                }

                case Parallel p -> buildParallel(p.left(), p.right(), endId);
            };
        }

        private void merge(int oldId, int newId) {
            var newTransitions = new ArrayList<Transition>(transitions.size());
            for (var t : transitions) {
                int src = t.source() == oldId ? newId : t.source();
                int tgt = t.target() == oldId ? newId : t.target();
                newTransitions.add(new Transition(src, t.label(), tgt, t.kind()));
            }
            transitions.clear();
            transitions.addAll(newTransitions);
            states.remove(oldId);
        }

        private int buildParallel(SessionType left, SessionType right, int endId) {
            StateSpace leftSs = StateSpaceBuilder.build(left);
            StateSpace rightSs = StateSpaceBuilder.build(right);
            StateSpace prod = ProductStateSpace.product(leftSs, rightSs);

            Map<Integer, Integer> remap = new HashMap<>();
            for (int sid : prod.states()) {
                if (sid == prod.bottom()) {
                    remap.put(sid, endId);
                } else {
                    remap.put(sid, fresh(prod.labels().getOrDefault(sid, "?")));
                }
            }

            for (var t : prod.transitions()) {
                transitions.add(new Transition(
                        remap.get(t.source()),
                        t.label(),
                        remap.get(t.target()),
                        t.kind()));
            }

            return remap.get(prod.top());
        }

        private Set<Integer> reachable(int start) {
            var visited = new HashSet<Integer>();
            var stack = new ArrayDeque<Integer>();
            stack.push(start);
            while (!stack.isEmpty()) {
                int s = stack.pop();
                if (visited.add(s)) {
                    for (var t : transitions) {
                        if (t.source() == s) {
                            stack.push(t.target());
                        }
                    }
                }
            }
            return visited;
        }
    }
}
