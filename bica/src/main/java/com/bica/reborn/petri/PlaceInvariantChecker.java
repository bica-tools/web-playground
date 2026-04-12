package com.bica.reborn.petri;

import java.util.*;

/**
 * S-invariants and T-invariants for Petri nets (Step 23 port).
 *
 * <p>A place invariant (P-invariant) y satisfies y^T * C = 0 where C is the
 * incidence matrix. The weighted sum sum_p y(p)*M(p) is constant across
 * all reachable markings.
 */
public final class PlaceInvariantChecker {

    private PlaceInvariantChecker() {}

    /**
     * A place invariant with weights and verification status.
     */
    public record PlaceInvariant(
            Map<Integer, Integer> weights,
            boolean isConservative,
            Integer tokenCount,
            Set<Integer> support) {

        public PlaceInvariant {
            weights = Map.copyOf(weights);
            support = Set.copyOf(support);
        }
    }

    /**
     * Result of full place-invariant analysis.
     */
    public record PlaceInvariantResult(
            List<PlaceInvariant> invariants,
            boolean allConservative,
            Set<Integer> coveredPlaces,
            int numPlaces,
            boolean isFullyCovered,
            int nullSpaceDimension) {

        public PlaceInvariantResult {
            invariants = List.copyOf(invariants);
            coveredPlaces = Set.copyOf(coveredPlaces);
        }
    }

    // =========================================================================
    // Incidence matrix
    // =========================================================================

    /**
     * Build the incidence matrix C[place][transition] = post - pre.
     */
    public static Map<Integer, Map<Integer, Integer>> computeIncidenceMatrix(PetriNet net) {
        List<Integer> placeIds = new ArrayList<>(net.places().keySet());
        Collections.sort(placeIds);
        List<Integer> transIds = new ArrayList<>(net.transitions().keySet());
        Collections.sort(transIds);

        Map<Integer, Map<Integer, Integer>> matrix = new HashMap<>();
        for (int p : placeIds) matrix.put(p, new HashMap<>());

        for (int tid : transIds) {
            Map<Integer, Integer> preMap = new HashMap<>();
            for (var arc : net.pre().getOrDefault(tid, Set.of())) {
                preMap.merge(arc.placeId(), arc.weight(), Integer::sum);
            }
            Map<Integer, Integer> postMap = new HashMap<>();
            for (var arc : net.post().getOrDefault(tid, Set.of())) {
                postMap.merge(arc.placeId(), arc.weight(), Integer::sum);
            }

            Set<Integer> allPlaces = new HashSet<>(preMap.keySet());
            allPlaces.addAll(postMap.keySet());
            for (int pid : allPlaces) {
                int val = postMap.getOrDefault(pid, 0) - preMap.getOrDefault(pid, 0);
                if (val != 0 && matrix.containsKey(pid)) {
                    matrix.get(pid).put(tid, val);
                }
            }
        }
        return matrix;
    }

    // =========================================================================
    // Null space computation
    // =========================================================================

    /**
     * Compute minimal P-invariants: vectors y such that y^T * C = 0.
     */
    public static List<Map<Integer, Integer>> computeSInvariants(PetriNet net) {
        List<Integer> placeIds = new ArrayList<>(net.places().keySet());
        Collections.sort(placeIds);
        List<Integer> transIds = new ArrayList<>(net.transitions().keySet());
        Collections.sort(transIds);

        if (placeIds.isEmpty()) return List.of();
        if (transIds.isEmpty()) {
            List<Map<Integer, Integer>> result = new ArrayList<>();
            for (int pid : placeIds) result.add(Map.of(pid, 1));
            return result;
        }

        Map<Integer, Map<Integer, Integer>> matrix = computeIncidenceMatrix(net);

        // Build C^T as dense matrix: rows=transitions, cols=places
        int nRows = transIds.size();
        int nCols = placeIds.size();
        int[][] ct = new int[nRows][nCols];
        for (int r = 0; r < nRows; r++) {
            int tid = transIds.get(r);
            for (int c = 0; c < nCols; c++) {
                int pid = placeIds.get(c);
                ct[r][c] = matrix.getOrDefault(pid, Map.of()).getOrDefault(tid, 0);
            }
        }

        List<int[]> basis = integerNullSpace(ct, nRows, nCols);

        List<Map<Integer, Integer>> result = new ArrayList<>();
        for (int[] vec : basis) {
            Map<Integer, Integer> d = new HashMap<>();
            for (int i = 0; i < nCols; i++) {
                if (vec[i] != 0) d.put(placeIds.get(i), vec[i]);
            }
            if (!d.isEmpty()) result.add(d);
        }
        return result;
    }

    /**
     * Compute T-invariants: vectors x such that C * x = 0.
     * These correspond to firing sequences that reproduce the initial marking.
     */
    public static List<Map<Integer, Integer>> computeTInvariants(PetriNet net) {
        List<Integer> placeIds = new ArrayList<>(net.places().keySet());
        Collections.sort(placeIds);
        List<Integer> transIds = new ArrayList<>(net.transitions().keySet());
        Collections.sort(transIds);

        if (transIds.isEmpty()) return List.of();
        if (placeIds.isEmpty()) {
            List<Map<Integer, Integer>> result = new ArrayList<>();
            for (int tid : transIds) result.add(Map.of(tid, 1));
            return result;
        }

        Map<Integer, Map<Integer, Integer>> matrix = computeIncidenceMatrix(net);

        // Build C as dense matrix: rows=places, cols=transitions
        int nRows = placeIds.size();
        int nCols = transIds.size();
        int[][] c = new int[nRows][nCols];
        for (int r = 0; r < nRows; r++) {
            int pid = placeIds.get(r);
            for (int col = 0; col < nCols; col++) {
                int tid = transIds.get(col);
                c[r][col] = matrix.getOrDefault(pid, Map.of()).getOrDefault(tid, 0);
            }
        }

        List<int[]> basis = integerNullSpace(c, nRows, nCols);

        List<Map<Integer, Integer>> result = new ArrayList<>();
        for (int[] vec : basis) {
            Map<Integer, Integer> d = new HashMap<>();
            for (int i = 0; i < nCols; i++) {
                if (vec[i] != 0) d.put(transIds.get(i), vec[i]);
            }
            if (!d.isEmpty()) result.add(d);
        }
        return result;
    }

    // =========================================================================
    // Conservation check
    // =========================================================================

    /**
     * Verify that y^T * M = const for all reachable markings.
     */
    public static boolean verifyInvariant(PetriNet net, Map<Integer, Integer> weights) {
        ReachabilityGraph rg = PetriNetBuilder.buildReachabilityGraph(net);
        Integer expected = null;
        for (String fm : rg.markings()) {
            Map<Integer, Integer> marking = parseMarking(fm);
            int total = 0;
            for (var e : weights.entrySet()) {
                total += e.getValue() * marking.getOrDefault(e.getKey(), 0);
            }
            if (expected == null) expected = total;
            else if (total != expected) return false;
        }
        return true;
    }

    // =========================================================================
    // Full analysis
    // =========================================================================

    /**
     * Full place-invariant analysis.
     */
    public static PlaceInvariantResult analyzePlaceInvariants(PetriNet net) {
        List<Map<Integer, Integer>> rawInvariants = computeSInvariants(net);
        List<PlaceInvariant> invariants = new ArrayList<>();
        Set<Integer> covered = new HashSet<>();
        boolean allConservative = true;

        for (Map<Integer, Integer> weights : rawInvariants) {
            boolean isCons = verifyInvariant(net, weights);
            Set<Integer> support = new HashSet<>();
            for (var e : weights.entrySet()) {
                if (e.getValue() != 0) support.add(e.getKey());
            }
            covered.addAll(support);

            // Compute token count
            Integer tokenCount = null;
            if (isCons) {
                Map<Integer, Integer> initMark = net.initialMarking();
                int total = 0;
                for (var e : weights.entrySet()) {
                    total += e.getValue() * initMark.getOrDefault(e.getKey(), 0);
                }
                tokenCount = total;
            }

            invariants.add(new PlaceInvariant(weights, isCons, tokenCount, support));
            if (!isCons) allConservative = false;
        }

        return new PlaceInvariantResult(
                invariants, allConservative, covered,
                net.places().size(),
                covered.size() == net.places().size(),
                rawInvariants.size());
    }

    // =========================================================================
    // Internal: integer null space via Gaussian elimination
    // =========================================================================

    static List<int[]> integerNullSpace(int[][] rows, int nRows, int nCols) {
        if (nRows == 0) {
            List<int[]> basis = new ArrayList<>();
            for (int i = 0; i < nCols; i++) {
                int[] v = new int[nCols];
                v[i] = 1;
                basis.add(v);
            }
            return basis;
        }

        int[][] mat = new int[nRows][];
        for (int r = 0; r < nRows; r++) mat[r] = Arrays.copyOf(rows[r], nCols);

        Map<Integer, Integer> pivotRowForCol = new LinkedHashMap<>();
        int currentRow = 0;

        for (int col = 0; col < nCols && currentRow < nRows; col++) {
            int pivot = -1;
            for (int r = currentRow; r < nRows; r++) {
                if (mat[r][col] != 0) { pivot = r; break; }
            }
            if (pivot == -1) continue;

            if (pivot != currentRow) {
                int[] tmp = mat[pivot];
                mat[pivot] = mat[currentRow];
                mat[currentRow] = tmp;
            }
            pivotRowForCol.put(col, currentRow);

            int pivotVal = mat[currentRow][col];
            for (int r = 0; r < nRows; r++) {
                if (r == currentRow || mat[r][col] == 0) continue;
                int factor = mat[r][col];
                for (int c = 0; c < nCols; c++) {
                    mat[r][c] = mat[r][c] * pivotVal - factor * mat[currentRow][c];
                }
                int rowGcd = 0;
                for (int c = 0; c < nCols; c++) {
                    if (mat[r][c] != 0) rowGcd = gcd(rowGcd, Math.abs(mat[r][c]));
                }
                if (rowGcd > 1) {
                    for (int c = 0; c < nCols; c++) mat[r][c] /= rowGcd;
                }
            }
            currentRow++;
        }

        Set<Integer> pivotCols = pivotRowForCol.keySet();
        List<Integer> freeCols = new ArrayList<>();
        for (int c = 0; c < nCols; c++) {
            if (!pivotCols.contains(c)) freeCols.add(c);
        }

        List<int[]> basis = new ArrayList<>();
        for (int fc : freeCols) {
            int[] vec = new int[nCols];
            int scale = 1;
            for (var e : pivotRowForCol.entrySet()) {
                int pc = e.getKey();
                int r = e.getValue();
                if (mat[r][fc] != 0) {
                    scale = lcm(scale, Math.abs(mat[r][pc]));
                }
            }
            vec[fc] = scale;
            for (var e : pivotRowForCol.entrySet()) {
                int pc = e.getKey();
                int r = e.getValue();
                if (mat[r][fc] != 0) {
                    vec[pc] = -(mat[r][fc] * (scale / mat[r][pc]));
                }
            }

            // Normalize
            int vecGcd = 0;
            for (int v : vec) if (v != 0) vecGcd = gcd(vecGcd, Math.abs(v));
            if (vecGcd > 1) {
                for (int i = 0; i < nCols; i++) vec[i] /= vecGcd;
            }
            // Make first non-zero positive
            for (int v : vec) {
                if (v != 0) {
                    if (v < 0) { for (int i = 0; i < nCols; i++) vec[i] = -vec[i]; }
                    break;
                }
            }
            basis.add(vec);
        }
        return basis;
    }

    private static int gcd(int a, int b) {
        a = Math.abs(a); b = Math.abs(b);
        while (b != 0) { int t = b; b = a % b; a = t; }
        return a;
    }

    private static int lcm(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return Math.abs(a * b) / gcd(a, b);
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
