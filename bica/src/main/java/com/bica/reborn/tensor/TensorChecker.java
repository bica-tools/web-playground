package com.bica.reborn.tensor;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.morphism.MorphismChecker;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.*;

/**
 * Tensor product: external (inter-object) product of session type lattices.
 *
 * <p>Distinguishes two kinds of lattice product:
 * <ul>
 *   <li><b>Internal product</b> (∥): one object, concurrent capabilities.
 *       Already in {@code ProductStateSpace}.</li>
 *   <li><b>External product</b> (⊗): multiple objects, one program.
 *       Labels are object-qualified: "name.method".</li>
 * </ul>
 *
 * <p>Key properties:
 * <ol>
 *   <li>Lattice closure: ⊗ of lattices is a lattice.</li>
 *   <li>Projections: πᵢ is a surjective lattice homomorphism.</li>
 * </ol>
 */
public final class TensorChecker {

    private TensorChecker() {}

    // =========================================================================
    // Core: tensor product construction
    // =========================================================================

    /**
     * Construct the external (tensor) product of multiple objects.
     *
     * @param participants pairs of (objectName, stateSpace)
     * @return TensorResult with the program-level product state space
     * @throws IllegalArgumentException if fewer than 2 participants or duplicate names
     */
    @SafeVarargs
    public static TensorResult tensorProduct(Map.Entry<String, StateSpace>... participants) {
        if (participants.length < 2) {
            throw new IllegalArgumentException("Tensor product requires at least 2 participants");
        }

        List<String> names = new ArrayList<>();
        Map<String, StateSpace> ssMap = new LinkedHashMap<>();
        for (var p : participants) {
            if (ssMap.containsKey(p.getKey())) {
                throw new IllegalArgumentException("Duplicate participant name: " + p.getKey());
            }
            names.add(p.getKey());
            ssMap.put(p.getKey(), p.getValue());
        }

        // Verify each is a lattice
        for (var entry : ssMap.entrySet()) {
            var lr = LatticeChecker.checkLattice(entry.getValue());
            if (!lr.isLattice()) {
                throw new IllegalArgumentException(
                        "Participant '" + entry.getKey() + "' state space is not a lattice");
            }
        }

        StateSpace tensorSs = buildTensorNary(names, ssMap);
        Map<String, Map<Integer, Integer>> projections = buildProjections(names, ssMap, tensorSs);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (var entry : ssMap.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().states().size());
        }

        return new TensorResult(ssMap, tensorSs, true, projections, counts);
    }

    /**
     * Check that a projection πᵢ is a surjective, order-preserving lattice homomorphism.
     */
    public static boolean checkBilinearity(TensorResult result, String name) {
        Map<Integer, Integer> proj = result.projections().get(name);
        StateSpace target = result.stateSpaces().get(name);

        // Surjectivity
        Set<Integer> image = new HashSet<>(proj.values());
        if (!image.equals(target.states())) return false;

        // Order preservation
        return MorphismChecker.isOrderPreserving(result.tensor(), target, proj);
    }

    // =========================================================================
    // Internal: N-ary tensor construction
    // =========================================================================

    private static StateSpace buildTensorNary(
            List<String> names, Map<String, StateSpace> ssMap) {
        List<StateSpace> ordered = new ArrayList<>();
        for (String n : names) ordered.add(ssMap.get(n));

        // Pre-compute adjacency lists
        List<Map<Integer, List<Map.Entry<String, Integer>>>> adjs = new ArrayList<>();
        for (StateSpace ss : ordered) {
            Map<Integer, List<Map.Entry<String, Integer>>> adj = new HashMap<>();
            for (int s : ss.states()) adj.put(s, new ArrayList<>());
            for (var t : ss.transitions()) {
                adj.get(t.source()).add(Map.entry(t.label(), t.target()));
            }
            adjs.add(adj);
        }

        // Enumerate all state tuples
        List<List<Integer>> stateLists = new ArrayList<>();
        for (StateSpace ss : ordered) {
            List<Integer> sorted = new ArrayList<>(ss.states());
            Collections.sort(sorted);
            stateLists.add(sorted);
        }

        List<int[]> allTuples = cartesianProduct(stateLists);

        int nextId = 0;
        Map<String, Integer> tupleToId = new HashMap<>();
        Map<Integer, int[]> idToTuple = new HashMap<>();
        Map<Integer, String> idLabels = new HashMap<>();

        for (int[] tup : allTuples) {
            String key = Arrays.toString(tup);
            int sid = nextId++;
            tupleToId.put(key, sid);
            idToTuple.put(sid, tup);
            StringBuilder label = new StringBuilder("(");
            for (int i = 0; i < tup.length; i++) {
                if (i > 0) label.append(", ");
                label.append(ordered.get(i).labels().getOrDefault(tup[i], String.valueOf(tup[i])));
            }
            label.append(")");
            idLabels.put(sid, label.toString());
        }

        // Build transitions
        List<Transition> transitions = new ArrayList<>();
        for (int[] tup : allTuples) {
            int src = tupleToId.get(Arrays.toString(tup));
            for (int i = 0; i < names.size(); i++) {
                for (var edge : adjs.get(i).get(tup[i])) {
                    int[] newTup = tup.clone();
                    newTup[i] = edge.getValue();
                    String qualified = names.get(i) + "." + edge.getKey();
                    int tgt = tupleToId.get(Arrays.toString(newTup));
                    transitions.add(new Transition(src, qualified, tgt));
                }
            }
        }

        // Top and bottom tuples
        int[] topTup = new int[names.size()];
        int[] botTup = new int[names.size()];
        for (int i = 0; i < ordered.size(); i++) {
            topTup[i] = ordered.get(i).top();
            botTup[i] = ordered.get(i).bottom();
        }

        return new StateSpace(
                new HashSet<>(tupleToId.values()),
                transitions,
                tupleToId.get(Arrays.toString(topTup)),
                tupleToId.get(Arrays.toString(botTup)),
                idLabels);
    }

    private static Map<String, Map<Integer, Integer>> buildProjections(
            List<String> names, Map<String, StateSpace> ssMap, StateSpace tensorSs) {
        // We need to reconstruct tuples from the tensor state IDs.
        // Rebuild the tupleToId / idToTuple mapping.
        List<StateSpace> ordered = new ArrayList<>();
        for (String n : names) ordered.add(ssMap.get(n));

        List<List<Integer>> stateLists = new ArrayList<>();
        for (StateSpace ss : ordered) {
            List<Integer> sorted = new ArrayList<>(ss.states());
            Collections.sort(sorted);
            stateLists.add(sorted);
        }

        List<int[]> allTuples = cartesianProduct(stateLists);
        Map<Integer, int[]> idToTuple = new HashMap<>();
        int nextId = 0;
        for (int[] tup : allTuples) {
            idToTuple.put(nextId++, tup);
        }

        Map<String, Map<Integer, Integer>> projections = new LinkedHashMap<>();
        for (int i = 0; i < names.size(); i++) {
            Map<Integer, Integer> proj = new HashMap<>();
            for (var entry : idToTuple.entrySet()) {
                proj.put(entry.getKey(), entry.getValue()[i]);
            }
            projections.put(names.get(i), proj);
        }
        return projections;
    }

    private static List<int[]> cartesianProduct(List<List<Integer>> lists) {
        List<int[]> result = new ArrayList<>();
        cartesianHelper(lists, 0, new int[lists.size()], result);
        return result;
    }

    private static void cartesianHelper(
            List<List<Integer>> lists, int depth, int[] current, List<int[]> result) {
        if (depth == lists.size()) {
            result.add(current.clone());
            return;
        }
        for (int val : lists.get(depth)) {
            current[depth] = val;
            cartesianHelper(lists, depth + 1, current, result);
        }
    }
}
