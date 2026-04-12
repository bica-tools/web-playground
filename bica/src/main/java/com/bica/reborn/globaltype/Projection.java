package com.bica.reborn.globaltype;

import com.bica.reborn.ast.*;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MPST projection: global type → local session type.
 *
 * <p>Projection maps a global type onto a single role's local view:
 * <ul>
 *   <li>(s → r : {mᵢ: Gᵢ}) ↓ s = +{mᵢ: Gᵢ↓s} — sender selects</li>
 *   <li>(s → r : {mᵢ: Gᵢ}) ↓ r = &{mᵢ: Gᵢ↓r} — receiver branches</li>
 *   <li>(s → r : {mᵢ: Gᵢ}) ↓ t = merge(Gᵢ↓t) — uninvolved role</li>
 *   <li>(G₁ ∥ G₂) ↓ r = (G₁↓r) ∥ (G₂↓r) if r in both</li>
 *   <li>(rec X . G) ↓ r = rec X . (G↓r) if r in G</li>
 * </ul>
 */
public final class Projection {

    private Projection() {}

    // -----------------------------------------------------------------------
    // Core projection
    // -----------------------------------------------------------------------

    /**
     * Project a global type onto a single role's local view.
     *
     * @throws ProjectionError if projection is undefined (merge failure)
     */
    public static SessionType project(GlobalType g, String role) {
        return doProject(g, role, new HashSet<>());
    }

    /**
     * Project a global type onto all participating roles.
     *
     * @return map from role name to local session type
     * @throws ProjectionError if any projection fails
     */
    public static Map<String, SessionType> projectAll(GlobalType g) {
        Set<String> allRoles = GlobalTypeChecker.roles(g);
        Map<String, SessionType> result = new TreeMap<>();
        for (String r : allRoles) {
            result.put(r, project(g, r));
        }
        return result;
    }

    /**
     * Project with error handling, returning a result object.
     */
    public static ProjectionResult checkProjection(GlobalType g, String role) {
        try {
            SessionType local = project(g, role);
            return new ProjectionResult(
                    role, local, PrettyPrinter.pretty(local), true);
        } catch (ProjectionError e) {
            return new ProjectionResult(
                    role, new End(), "end", false);
        }
    }

    // -----------------------------------------------------------------------
    // Morphism verification
    // -----------------------------------------------------------------------

    /**
     * Verify that projection induces a surjective order-preserving map
     * from the global state space to the local state space.
     */
    public static ProjectionMorphismResult verifyProjectionMorphism(
            GlobalType g, String role) {
        SessionType localType = project(g, role);

        StateSpace globalSs = GlobalTypeChecker.buildStateSpace(g);
        StateSpace localSs = StateSpaceBuilder.build(localType);

        LatticeResult globalLattice = LatticeChecker.checkLattice(globalSs);
        LatticeResult localLattice = LatticeChecker.checkLattice(localSs);

        boolean[] props = checkMorphismProperties(globalSs, localSs, role);

        return new ProjectionMorphismResult(
                role,
                globalSs.states().size(),
                localSs.states().size(),
                props[0],  // surjective
                props[1],  // order-preserving
                globalLattice.isLattice(),
                localLattice.isLattice());
    }

    // -----------------------------------------------------------------------
    // Internal projection
    // -----------------------------------------------------------------------

    private static SessionType doProject(
            GlobalType g, String role, Set<String> inProgress) {
        return switch (g) {
            case GEnd e -> new End();

            case GVar v -> new Var(v.name());

            case GMessage m -> {
                if (role.equals(m.sender())) {
                    // Sender: internal choice (Select)
                    List<Branch.Choice> choices = m.choices().stream()
                            .map(c -> new Branch.Choice(
                                    c.label(),
                                    doProject(c.body(), role, inProgress)))
                            .toList();
                    yield new Select(choices);
                } else if (role.equals(m.receiver())) {
                    // Receiver: external choice (Branch)
                    List<Branch.Choice> choices = m.choices().stream()
                            .map(c -> new Branch.Choice(
                                    c.label(),
                                    doProject(c.body(), role, inProgress)))
                            .toList();
                    yield new Branch(choices);
                } else {
                    // Uninvolved: merge all projections
                    List<SessionType> projections = m.choices().stream()
                            .map(c -> doProject(c.body(), role, inProgress))
                            .toList();
                    yield merge(projections, role);
                }
            }

            case GParallel p -> {
                Set<String> leftRoles = GlobalTypeChecker.roles(p.left());
                Set<String> rightRoles = GlobalTypeChecker.roles(p.right());
                boolean inLeft = leftRoles.contains(role);
                boolean inRight = rightRoles.contains(role);

                if (inLeft && inRight) {
                    yield new Parallel(
                            doProject(p.left(), role, inProgress),
                            doProject(p.right(), role, inProgress));
                } else if (inLeft) {
                    yield doProject(p.left(), role, inProgress);
                } else if (inRight) {
                    yield doProject(p.right(), role, inProgress);
                } else {
                    yield new End();
                }
            }

            case GRec r -> {
                Set<String> bodyRoles = GlobalTypeChecker.roles(r.body());
                if (!bodyRoles.contains(role)) {
                    yield new End();
                }
                if (inProgress.contains(r.var())) {
                    yield new Var(r.var());
                }
                Set<String> newInProgress = new HashSet<>(inProgress);
                newInProgress.add(r.var());
                SessionType projectedBody = doProject(r.body(), role, newInProgress);
                if (projectedBody.equals(new End())) {
                    yield new End();
                }
                yield new Rec(r.var(), projectedBody);
            }
        };
    }

    private static SessionType merge(List<SessionType> projections, String role) {
        if (projections.isEmpty()) {
            return new End();
        }
        SessionType first = projections.getFirst();
        for (int i = 1; i < projections.size(); i++) {
            if (!projections.get(i).equals(first)) {
                throw new ProjectionError(
                        "merge failure for role '" + role + "': projections differ ("
                        + PrettyPrinter.pretty(first) + " vs "
                        + PrettyPrinter.pretty(projections.get(i)) + ")",
                        role);
            }
        }
        return first;
    }

    // -----------------------------------------------------------------------
    // Morphism properties
    // -----------------------------------------------------------------------

    private static boolean[] checkMorphismProperties(
            StateSpace globalSs, StateSpace localSs, String role) {
        // Build mapping by BFS through global state space
        Map<Integer, Integer> mapping = new HashMap<>();
        mapping.put(globalSs.top(), localSs.top());
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(globalSs.top());

        // Build local adjacency for quick lookup
        Map<Integer, Map<String, Integer>> localAdj = new HashMap<>();
        for (int s : localSs.states()) {
            Map<String, Integer> adj = new HashMap<>();
            for (Transition t : localSs.transitionsFrom(s)) {
                adj.put(t.label(), t.target());
            }
            localAdj.put(s, adj);
        }

        while (!queue.isEmpty()) {
            int gs = queue.poll();
            if (!visited.add(gs)) continue;
            if (!mapping.containsKey(gs)) continue;

            int ls = mapping.get(gs);
            Map<String, Integer> localEnabled = localAdj.getOrDefault(ls, Map.of());

            for (Transition t : globalSs.transitionsFrom(gs)) {
                String method = stripRolePrefix(t.label(), role);
                if (method != null && localEnabled.containsKey(method)) {
                    mapping.putIfAbsent(t.target(), localEnabled.get(method));
                } else if (method == null) {
                    // Transition doesn't involve this role — map to same local state
                    mapping.putIfAbsent(t.target(), ls);
                }
                queue.add(t.target());
            }
        }

        // Ensure bottom maps to bottom
        mapping.putIfAbsent(globalSs.bottom(), localSs.bottom());

        // Surjectivity
        Set<Integer> image = new HashSet<>(mapping.values());
        boolean surjective = image.containsAll(localSs.states());

        // Order-preservation
        boolean orderPreserving = true;
        outer:
        for (var e1 : mapping.entrySet()) {
            Set<Integer> reachG = globalSs.reachableFrom(e1.getKey());
            for (var e2 : mapping.entrySet()) {
                if (reachG.contains(e2.getKey())) {
                    Set<Integer> reachL = localSs.reachableFrom(e1.getValue());
                    if (!reachL.contains(e2.getValue())) {
                        orderPreserving = false;
                        break outer;
                    }
                }
            }
        }

        return new boolean[]{surjective, orderPreserving};
    }

    private static String stripRolePrefix(String label, String role) {
        if (!label.contains("->")) return label;
        int colonIdx = label.indexOf(':');
        if (colonIdx < 0) return null;
        String rolePart = label.substring(0, colonIdx);
        String method = label.substring(colonIdx + 1);
        String[] parts = rolePart.split("->");
        if (parts.length != 2) return null;
        if (role.equals(parts[0]) || role.equals(parts[1])) return method;
        return null;
    }
}
