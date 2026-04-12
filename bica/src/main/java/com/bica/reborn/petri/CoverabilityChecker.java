package com.bica.reborn.petri;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Coverability tree construction for Petri nets (Step 24 port).
 *
 * <p>Implements the Karp-Miller coverability tree algorithm. For session-type
 * nets in state-machine encoding, the tree always coincides with the
 * reachability graph (1-safe, no omega).
 */
public final class CoverabilityChecker {

    private CoverabilityChecker() {}

    /** Sentinel for unbounded places. */
    public static final int OMEGA = Integer.MAX_VALUE;

    /**
     * A node in the coverability tree.
     */
    public record CoverabilityNode(
            int id,
            Map<Integer, Integer> marking,
            int parent,
            int transitionFromParent,
            List<Integer> children,
            boolean isDuplicate) {}

    // =========================================================================
    // Omega-marking utilities
    // =========================================================================

    static boolean dominates(Map<Integer, Integer> m1, Map<Integer, Integer> m2) {
        Set<Integer> allPlaces = new HashSet<>(m1.keySet());
        allPlaces.addAll(m2.keySet());
        boolean strictlyGreater = false;
        for (int p : allPlaces) {
            int v1 = m1.getOrDefault(p, 0);
            int v2 = m2.getOrDefault(p, 0);
            if (v1 < v2) return false;
            if (v1 > v2) strictlyGreater = true;
        }
        return strictlyGreater;
    }

    static boolean covers(Map<Integer, Integer> m, Map<Integer, Integer> target) {
        Set<Integer> allPlaces = new HashSet<>(m.keySet());
        allPlaces.addAll(target.keySet());
        for (int p : allPlaces) {
            if (m.getOrDefault(p, 0) < target.getOrDefault(p, 0)) return false;
        }
        return true;
    }

    static boolean omegaEnabled(PetriNet net, Map<Integer, Integer> marking, int tid) {
        if (!net.pre().containsKey(tid)) return false;
        for (var arc : net.pre().get(tid)) {
            if (marking.getOrDefault(arc.placeId(), 0) < arc.weight()) return false;
        }
        return true;
    }

    static Map<Integer, Integer> omegaFire(PetriNet net, Map<Integer, Integer> marking, int tid) {
        if (!omegaEnabled(net, marking, tid)) return null;
        Map<Integer, Integer> result = new HashMap<>(marking);
        for (var arc : net.pre().get(tid)) {
            int v = result.getOrDefault(arc.placeId(), 0);
            if (v != OMEGA) {
                int newVal = v - arc.weight();
                if (newVal == 0) result.remove(arc.placeId());
                else result.put(arc.placeId(), newVal);
            }
        }
        for (var arc : net.post().getOrDefault(tid, Set.of())) {
            int v = result.getOrDefault(arc.placeId(), 0);
            if (v != OMEGA) {
                result.put(arc.placeId(), v + arc.weight());
            }
        }
        return result;
    }

    static String freezeOmega(Map<Integer, Integer> m) {
        return m.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + (e.getValue() == OMEGA ? "w" : e.getValue()))
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b);
    }

    // =========================================================================
    // Build coverability tree
    // =========================================================================

    /**
     * Build the Karp-Miller coverability tree.
     *
     * @param net             The Petri net.
     * @param initialMarking  Starting marking (null = net's initial marking).
     * @param maxNodes        Safety bound on tree size.
     * @return CoverabilityResult.
     */
    public static CoverabilityResult buildCoverabilityTree(
            PetriNet net, Map<Integer, Integer> initialMarking, int maxNodes) {

        if (initialMarking == null) initialMarking = net.initialMarking();

        Map<Integer, CoverabilityNode> nodes = new HashMap<>();
        int nextId = 0;

        Map<Integer, Integer> rootMarking = new HashMap<>(initialMarking);
        List<Integer> rootChildren = new ArrayList<>();
        CoverabilityNode root = new CoverabilityNode(nextId, rootMarking, -1, -1, rootChildren, false);
        nodes.put(nextId, root);
        nextId++;

        Deque<Integer> worklist = new ArrayDeque<>();
        worklist.add(root.id());
        Set<String> distinctMarkings = new HashSet<>();
        distinctMarkings.add(freezeOmega(rootMarking));

        while (!worklist.isEmpty() && nextId < maxNodes) {
            int currentId = worklist.poll();
            CoverabilityNode currentNode = nodes.get(currentId);
            if (currentNode.isDuplicate()) continue;

            // Check duplicate on path
            List<Map<Integer, Integer>> ancestors = getAncestorMarkings(nodes, currentId);
            String frozenCurrent = freezeOmega(currentNode.marking());
            boolean isDup = ancestors.stream().anyMatch(a -> freezeOmega(a).equals(frozenCurrent));
            if (isDup) {
                nodes.put(currentId, new CoverabilityNode(currentId, currentNode.marking(),
                        currentNode.parent(), currentNode.transitionFromParent(),
                        currentNode.children(), true));
                continue;
            }

            List<Integer> transIds = new ArrayList<>(net.transitions().keySet());
            Collections.sort(transIds);

            for (int tid : transIds) {
                Map<Integer, Integer> newMarking = omegaFire(net, currentNode.marking(), tid);
                if (newMarking == null) continue;

                // Omega-acceleration
                List<Map<Integer, Integer>> allAnc = new ArrayList<>(ancestors);
                allAnc.add(currentNode.marking());
                for (Map<Integer, Integer> anc : allAnc) {
                    if (dominates(newMarking, anc)) {
                        Set<Integer> allPlaces = new HashSet<>(newMarking.keySet());
                        allPlaces.addAll(anc.keySet());
                        for (int p : allPlaces) {
                            if (newMarking.getOrDefault(p, 0) > anc.getOrDefault(p, 0)) {
                                newMarking.put(p, OMEGA);
                            }
                        }
                        break;
                    }
                }

                List<Integer> childChildren = new ArrayList<>();
                CoverabilityNode child = new CoverabilityNode(
                        nextId, newMarking, currentId, tid, childChildren, false);
                nodes.put(nextId, child);
                currentNode.children().add(nextId);
                distinctMarkings.add(freezeOmega(newMarking));
                worklist.add(nextId);
                nextId++;
            }
        }

        // Compute summary
        Set<Integer> allPlaces = new HashSet<>(net.places().keySet());
        Map<Integer, Integer> maxTokens = new HashMap<>();
        for (int p : allPlaces) maxTokens.put(p, 0);
        Set<Integer> unbounded = new HashSet<>();

        for (var node : nodes.values()) {
            for (int p : allPlaces) {
                int v = node.marking().getOrDefault(p, 0);
                if (v == OMEGA) {
                    maxTokens.put(p, OMEGA);
                    unbounded.add(p);
                } else if (maxTokens.get(p) != OMEGA) {
                    maxTokens.merge(p, v, Integer::max);
                }
            }
        }

        boolean bounded = unbounded.isEmpty();
        boolean oneSafe = bounded && maxTokens.values().stream().allMatch(v -> v <= 1);

        return new CoverabilityResult(nodes.size(), distinctMarkings.size(),
                bounded, unbounded, oneSafe);
    }

    /**
     * Build coverability tree with default parameters.
     */
    public static CoverabilityResult buildCoverabilityTree(PetriNet net) {
        return buildCoverabilityTree(net, null, 10_000);
    }

    // =========================================================================
    // Coverability check
    // =========================================================================

    /**
     * Check whether a target marking is coverable.
     */
    public static boolean isCoverable(PetriNet net, Map<Integer, Integer> targetMarking) {
        CoverabilityResult result = buildCoverabilityTree(net);
        // We don't store node markings in the result, so rebuild the tree to check
        // For simplicity, use the reachability graph for 1-safe nets
        ReachabilityGraph rg = PetriNetBuilder.buildReachabilityGraph(net);
        for (String fm : rg.markings()) {
            Map<Integer, Integer> m = parseMarking(fm);
            if (covers(m, targetMarking)) return true;
        }
        return false;
    }

    /**
     * Check whether a Petri net is bounded.
     */
    public static CoverabilityResult checkBoundedness(PetriNet net) {
        return buildCoverabilityTree(net);
    }

    // =========================================================================
    // Convenience: StateSpace -> full analysis
    // =========================================================================

    /**
     * Analyze coverability for a session type state space.
     */
    public static CoverabilityResult analyzeCoverability(StateSpace ss) {
        PetriNet net = PetriNetBuilder.buildPetriNet(ss);
        return buildCoverabilityTree(net);
    }

    // =========================================================================
    // Internal
    // =========================================================================

    private static List<Map<Integer, Integer>> getAncestorMarkings(
            Map<Integer, CoverabilityNode> nodes, int nodeId) {
        List<Map<Integer, Integer>> ancestors = new ArrayList<>();
        int current = nodes.get(nodeId).parent();
        while (current >= 0) {
            ancestors.add(nodes.get(current).marking());
            current = nodes.get(current).parent();
        }
        Collections.reverse(ancestors);
        return ancestors;
    }

    private static Map<Integer, Integer> parseMarking(String frozen) {
        Map<Integer, Integer> m = new HashMap<>();
        if (frozen.isEmpty()) return m;
        for (String part : frozen.split(",")) {
            String[] kv = part.split("=");
            if (kv.length == 2) {
                m.put(Integer.parseInt(kv[0].trim()), Integer.parseInt(kv[1].trim()));
            }
        }
        return m;
    }
}
