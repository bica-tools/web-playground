package com.bica.reborn.algebraic.tropical;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.statespace.StateSpace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Tropical eigenvalue analysis for session type lattices (port of Python
 * {@code reticulate.tropical_eigen}, Step 30l).
 *
 * <p>Builds on {@link Tropical} to provide a full max-plus eigen analysis:
 * eigenspace basis via Kleene star, subeigenspace, critical-graph SCCs,
 * cyclicity (gcd of critical cycle lengths), CSR decomposition, Kleene
 * star, definiteness, tropical characteristic polynomial, and tropical
 * trace.
 *
 * <p>All matrices are {@code double[][]}. Tropical zero is
 * {@link Double#NEGATIVE_INFINITY} (consistent with {@link Tropical}).
 */
public final class TropicalEigen {

    public static final double NEG_INF = Double.NEGATIVE_INFINITY;
    public static final double INF = Double.POSITIVE_INFINITY;
    private static final double EPS = 1e-9;

    private TropicalEigen() {}

    // =======================================================================
    // Result type
    // =======================================================================

    /** Complete tropical eigenvalue analysis. */
    public record TropicalEigenResult(
            double eigenvalue,
            List<double[]> eigenvectors,
            int eigenspaceDim,
            List<double[]> subeigenvectors,
            List<List<Integer>> criticalComponents,
            int cyclicity,
            int csrIndex,
            int csrPeriod,
            double[][] kleeneStar,
            boolean isDefinite,
            Map<String, Object> visualizationData) {}

    // =======================================================================
    // Internal matrix helpers
    // =======================================================================

    static double[][] tropicalIdentity(int n) {
        double[][] I = new double[n][n];
        for (double[] row : I) Arrays.fill(row, NEG_INF);
        for (int i = 0; i < n; i++) I[i][i] = 0.0;
        return I;
    }

    static double[][] tropicalMatrixAdd(double[][] A, double[][] B) {
        int n = A.length;
        if (n == 0) return new double[0][0];
        int m = A[0].length;
        double[][] C = new double[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                C[i][j] = Tropical.tropicalAdd(A[i][j], B[i][j]);
            }
        }
        return C;
    }

    static double[][] tropicalMatrixScalar(double[][] A, double s) {
        int n = A.length;
        if (n == 0) return new double[0][0];
        int m = A[0].length;
        double[][] C = new double[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                C[i][j] = Tropical.tropicalMul(A[i][j], s);
            }
        }
        return C;
    }

    static boolean matricesEqual(double[][] A, double[][] B) {
        return matricesEqual(A, B, EPS);
    }

    static boolean matricesEqual(double[][] A, double[][] B, double tol) {
        int n = A.length;
        if (n != B.length) return false;
        for (int i = 0; i < n; i++) {
            if (A[i].length != B[i].length) return false;
            for (int j = 0; j < A[i].length; j++) {
                double a = A[i][j];
                double b = B[i][j];
                if (a == NEG_INF && b == NEG_INF) continue;
                if (a == NEG_INF || b == NEG_INF) return false;
                if (Math.abs(a - b) > tol) return false;
            }
        }
        return true;
    }

    static double[] tropicalMatrixVec(double[][] A, double[] v) {
        int n = A.length;
        double[] result = new double[n];
        Arrays.fill(result, NEG_INF);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < v.length; j++) {
                double val = Tropical.tropicalMul(A[i][j], v[j]);
                result[i] = Tropical.tropicalAdd(result[i], val);
            }
        }
        return result;
    }

    static int gcdList(List<Integer> values) {
        if (values.isEmpty()) return 0;
        int result = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            result = gcd(result, values.get(i));
            if (result == 1) return 1;
        }
        return result;
    }

    private static int gcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            int t = b;
            b = a % b;
            a = t;
        }
        return a;
    }

    // =======================================================================
    // Critical components
    // =======================================================================

    /**
     * SCCs of the critical graph (max cycle-mean subgraph). Each component
     * is a sorted list of state IDs. Only non-trivial SCCs (size &gt; 1) or
     * single states with self-loops are returned.
     */
    public static List<List<Integer>> criticalComponents(StateSpace ss) {
        List<int[]> critEdges = Tropical.criticalGraph(ss);
        if (critEdges.isEmpty()) return List.of();

        Set<Integer> critNodes = new TreeSet<>();
        for (int[] e : critEdges) {
            critNodes.add(e[0]);
            critNodes.add(e[1]);
        }

        Map<Integer, List<Integer>> critAdj = new HashMap<>();
        for (int n : critNodes) critAdj.put(n, new ArrayList<>());
        for (int[] e : critEdges) {
            List<Integer> nbrs = critAdj.get(e[0]);
            if (!nbrs.contains(e[1])) nbrs.add(e[1]);
        }

        // Iterative Tarjan's SCC.
        int[] indexCounter = {0};
        Map<Integer, Integer> index = new HashMap<>();
        Map<Integer, Integer> lowlink = new HashMap<>();
        List<Integer> stack = new ArrayList<>();
        Set<Integer> onStack = new HashSet<>();
        List<List<Integer>> components = new ArrayList<>();

        for (int start : critNodes) {
            if (index.containsKey(start)) continue;
            // work: (node, next-neighbour-index)
            List<int[]> work = new ArrayList<>();
            work.add(new int[]{start, 0});
            while (!work.isEmpty()) {
                int[] top = work.get(work.size() - 1);
                int v = top[0];
                int ci = top[1];
                if (!index.containsKey(v)) {
                    index.put(v, indexCounter[0]);
                    lowlink.put(v, indexCounter[0]);
                    indexCounter[0]++;
                    stack.add(v);
                    onStack.add(v);
                }
                List<Integer> neighbors = critAdj.getOrDefault(v, List.of());
                boolean recurse = false;
                int i = ci;
                while (i < neighbors.size()) {
                    int w = neighbors.get(i);
                    if (!critNodes.contains(w)) {
                        i++;
                        continue;
                    }
                    if (!index.containsKey(w)) {
                        top[1] = i + 1;
                        work.add(new int[]{w, 0});
                        recurse = true;
                        break;
                    } else if (onStack.contains(w)) {
                        lowlink.put(v, Math.min(lowlink.get(v), index.get(w)));
                    }
                    i++;
                }
                if (recurse) continue;
                if (lowlink.get(v).intValue() == index.get(v).intValue()) {
                    List<Integer> scc = new ArrayList<>();
                    while (true) {
                        int w = stack.remove(stack.size() - 1);
                        onStack.remove(w);
                        scc.add(w);
                        if (w == v) break;
                    }
                    boolean hasSelfLoop = false;
                    for (int v2 : scc) {
                        if (critAdj.getOrDefault(v2, List.of()).contains(v2)) {
                            hasSelfLoop = true;
                            break;
                        }
                    }
                    if (scc.size() > 1 || hasSelfLoop) {
                        Collections.sort(scc);
                        components.add(scc);
                    }
                }
                work.remove(work.size() - 1);
                if (!work.isEmpty()) {
                    int parent = work.get(work.size() - 1)[0];
                    lowlink.put(parent, Math.min(lowlink.get(parent), lowlink.get(v)));
                }
            }
        }

        components.sort((a, b) -> Integer.compare(a.get(0), b.get(0)));
        return components;
    }

    // =======================================================================
    // Cyclicity
    // =======================================================================

    /**
     * GCD of lengths of all simple cycles in the critical graph. Returns 0
     * for acyclic state spaces (no critical graph), 1 for a single
     * self-loop, otherwise the gcd of the critical cycle lengths.
     */
    public static int cyclicity(StateSpace ss) {
        List<int[]> critEdges = Tropical.criticalGraph(ss);
        if (critEdges.isEmpty()) return 0;

        Set<Integer> critNodes = new TreeSet<>();
        for (int[] e : critEdges) {
            critNodes.add(e[0]);
            critNodes.add(e[1]);
        }
        Map<Integer, List<Integer>> critAdj = new HashMap<>();
        for (int n : critNodes) critAdj.put(n, new ArrayList<>());
        for (int[] e : critEdges) {
            List<Integer> nbrs = critAdj.get(e[0]);
            if (!nbrs.contains(e[1])) nbrs.add(e[1]);
        }

        List<Integer> cycleLens = cycleLengthsInSubgraph(critNodes, critAdj);

        for (int n : critNodes) {
            if (critAdj.getOrDefault(n, List.of()).contains(n)) {
                cycleLens.add(1);
            }
        }

        if (cycleLens.isEmpty()) {
            List<List<Integer>> comps = criticalComponents(ss);
            for (List<Integer> c : comps) {
                if (!c.isEmpty()) cycleLens.add(c.size());
            }
        }

        if (cycleLens.isEmpty()) return 1;
        return gcdList(cycleLens);
    }

    /**
     * Find lengths of all simple cycles in the given subgraph. Mirrors the
     * Python reference: starts DFS from each node in sorted order and only
     * explores neighbours greater than the start node to avoid duplicates.
     */
    private static List<Integer> cycleLengthsInSubgraph(
            Set<Integer> nodes, Map<Integer, List<Integer>> adj) {
        List<Integer> lengths = new ArrayList<>();
        List<Integer> nodeList = new ArrayList<>(nodes);
        Collections.sort(nodeList);

        for (int start : nodeList) {
            // Stack frames: (node, nextNeighbourIndex, path)
            List<Object[]> stack = new ArrayList<>();
            List<Integer> initialPath = new ArrayList<>();
            initialPath.add(start);
            stack.add(new Object[]{start, 0, initialPath});
            while (!stack.isEmpty()) {
                Object[] frame = stack.get(stack.size() - 1);
                int node = (int) frame[0];
                int idx = (int) frame[1];
                @SuppressWarnings("unchecked")
                List<Integer> path = (List<Integer>) frame[2];
                List<Integer> neighbors = new ArrayList<>();
                for (int n : adj.getOrDefault(node, List.of())) {
                    if (nodes.contains(n)) neighbors.add(n);
                }
                boolean foundNext = false;
                for (int i = idx; i < neighbors.size(); i++) {
                    int nb = neighbors.get(i);
                    if (nb == start && path.size() > 1) {
                        lengths.add(path.size());
                        continue;
                    }
                    if (!path.contains(nb) && nb > start) {
                        frame[1] = i + 1;
                        List<Integer> newPath = new ArrayList<>(path);
                        newPath.add(nb);
                        stack.add(new Object[]{nb, 0, newPath});
                        foundNext = true;
                        break;
                    }
                }
                if (!foundNext) stack.remove(stack.size() - 1);
            }
        }
        return lengths;
    }

    // =======================================================================
    // Kleene star
    // =======================================================================

    /**
     * Kleene star A* = I &oplus; A &oplus; A&sup2; &oplus; ... Returns
     * {@code null} when the tropical spectral radius is strictly positive
     * (the series diverges).
     */
    public static double[][] kleeneStar(StateSpace ss) {
        double lam = Tropical.tropicalEigenvalue(ss);
        if (lam > 0.0) return null;

        double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
        int n = A.length;
        if (n == 0) return new double[0][0];

        double[][] result = tropicalIdentity(n);
        double[][] power = tropicalIdentity(n);
        for (int k = 0; k < n; k++) {
            power = Tropical.tropicalMatrixMul(power, A);
            result = tropicalMatrixAdd(result, power);
        }
        return result;
    }

    // =======================================================================
    // Eigenspace
    // =======================================================================

    /**
     * Basis of the tropical eigenspace. For acyclic graphs returns the
     * trivial all-zeros vector.
     */
    public static List<double[]> tropicalEigenspace(StateSpace ss) {
        double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
        int n = A.length;
        List<double[]> trivial = new ArrayList<>();
        trivial.add(new double[n]); // all zeros
        if (n == 0) return List.of();

        double lam = Tropical.tropicalEigenvalue(ss);
        List<Integer> states = Zeta.stateList(ss);

        if (lam == 0.0) {
            return trivial;
        }

        double[][] B = new double[n][n];
        for (double[] row : B) Arrays.fill(row, NEG_INF);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (A[i][j] > NEG_INF) B[i][j] = A[i][j] - lam;
            }
        }

        double[][] bStar = tropicalIdentity(n);
        double[][] power = tropicalIdentity(n);
        for (int k = 0; k < n; k++) {
            power = Tropical.tropicalMatrixMul(power, B);
            bStar = tropicalMatrixAdd(bStar, power);
        }

        List<List<Integer>> comps = criticalComponents(ss);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < states.size(); i++) idx.put(states.get(i), i);

        List<double[]> basis = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (List<Integer> comp : comps) {
            int rep = comp.get(0);
            Integer colIdx = idx.get(rep);
            if (colIdx == null || seen.contains(colIdx)) continue;
            seen.add(colIdx);
            double[] col = new double[n];
            for (int i = 0; i < n; i++) col[i] = bStar[i][colIdx];
            basis.add(col);
        }

        if (basis.isEmpty()) {
            double[] ev = Tropical.tropicalEigenvector(ss);
            if (ev.length > 0) {
                basis.add(ev);
            }
        }

        return basis.isEmpty() ? trivial : basis;
    }

    /**
     * Subeigenspace: all vectors v with A &otimes; v &le; &lambda; &otimes; v.
     * Generated by all finite columns of the Kleene star of B = &lambda;&#8315;&sup1; &otimes; A.
     */
    public static List<double[]> tropicalSubeigenspace(StateSpace ss) {
        double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
        int n = A.length;
        List<double[]> trivial = new ArrayList<>();
        trivial.add(new double[n]);
        if (n == 0) return List.of();

        double lam = Tropical.tropicalEigenvalue(ss);
        double[][] B;
        if (lam == 0.0) {
            B = new double[n][n];
            for (int i = 0; i < n; i++) B[i] = A[i].clone();
        } else {
            B = new double[n][n];
            for (double[] row : B) Arrays.fill(row, NEG_INF);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (A[i][j] > NEG_INF) B[i][j] = A[i][j] - lam;
                }
            }
        }

        double[][] bStar = tropicalIdentity(n);
        double[][] power = tropicalIdentity(n);
        for (int k = 0; k < n; k++) {
            power = Tropical.tropicalMatrixMul(power, B);
            bStar = tropicalMatrixAdd(bStar, power);
        }

        List<double[]> basis = new ArrayList<>();
        for (int j = 0; j < n; j++) {
            double[] col = new double[n];
            boolean anyFinite = false;
            for (int i = 0; i < n; i++) {
                col[i] = bStar[i][j];
                if (col[i] > NEG_INF) anyFinite = true;
            }
            if (anyFinite) basis.add(col);
        }
        return basis.isEmpty() ? trivial : basis;
    }

    /**
     * Dimension of the tropical eigenspace: number of critical SCCs, or 1
     * for acyclic graphs.
     */
    public static int eigenspaceDimension(StateSpace ss) {
        List<List<Integer>> comps = criticalComponents(ss);
        if (comps.isEmpty()) return 1;
        return comps.size();
    }

    // =======================================================================
    // CSR decomposition
    // =======================================================================

    /**
     * CSR (cyclically stationary regime) index and period: returns
     * {@code (k0, c)} such that A^{k+c} = &lambda;^c &otimes; A^k for
     * every k &ge; k0. Acyclic graphs return {@code (0, 1)}.
     */
    public static int[] csrDecomposition(StateSpace ss) {
        double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
        int n = A.length;
        if (n == 0) return new int[]{0, 1};

        double lam = Tropical.tropicalEigenvalue(ss);
        if (lam == 0.0) return new int[]{0, 1};

        int c = cyclicity(ss);
        if (c == 0) c = 1;

        double lambdaC = lam * c;

        int maxK = n * n + 1;
        List<double[][]> powers = new ArrayList<>();
        double[][] current = tropicalIdentity(n);
        powers.add(current);
        for (int i = 1; i <= maxK + c; i++) {
            current = Tropical.tropicalMatrixMul(current, A);
            powers.add(current);
        }

        for (int k0 = 0; k0 < maxK; k0++) {
            if (k0 + c >= powers.size()) break;
            double[][] aKc = powers.get(k0 + c);
            double[][] aKShifted = tropicalMatrixScalar(powers.get(k0), lambdaC);
            if (matricesEqual(aKc, aKShifted)) {
                return new int[]{k0, c};
            }
        }
        return new int[]{n, c};
    }

    // =======================================================================
    // Definiteness
    // =======================================================================

    /**
     * A matrix is definite iff A* exists (spectral radius &le; 0) and
     * A = A*.
     */
    public static boolean isDefinite(StateSpace ss) {
        double[][] aStar = kleeneStar(ss);
        if (aStar == null) return false;
        double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
        return matricesEqual(A, aStar);
    }

    // =======================================================================
    // Tropical characteristic polynomial
    // =======================================================================

    /**
     * Coefficients [c_0, ..., c_n] of the tropical characteristic polynomial
     * where c_k is the maximum weight of a perfect matching over all
     * (n-k)-element principal submatrices of A. c_n is 0 (tropical
     * identity for multiplication).
     */
    public static double[] tropicalCharacteristicPolynomial(StateSpace ss) {
        double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
        int n = A.length;
        if (n == 0) return new double[]{0.0};

        double[] coeffs = new double[n + 1];
        for (int k = 0; k <= n; k++) {
            int size = n - k;
            if (size == 0) {
                coeffs[k] = 0.0;
                continue;
            }
            coeffs[k] = enumerateSubsetsMatching(A, n, size);
        }
        return coeffs;
    }

    private static double enumerateSubsetsMatching(double[][] A, int n, int size) {
        if (size == 0) return 0.0;
        if (size > 10) return greedyMatchingWeight(A, rangeList(n));
        double[] best = {NEG_INF};
        subsetsRec(A, n, size, 0, new ArrayList<>(), best);
        return best[0];
    }

    private static void subsetsRec(
            double[][] A, int n, int size, int start,
            List<Integer> chosen, double[] best) {
        if (chosen.size() == size) {
            double w = maxWeightMatching(A, chosen);
            if (w > best[0]) best[0] = w;
            return;
        }
        int remaining = size - chosen.size();
        for (int i = start; i <= n - remaining; i++) {
            chosen.add(i);
            subsetsRec(A, n, size, i + 1, chosen, best);
            chosen.remove(chosen.size() - 1);
        }
    }

    private static double maxWeightMatching(double[][] A, List<Integer> indices) {
        int k = indices.size();
        if (k == 0) return 0.0;
        if (k == 1) return A[indices.get(0)][indices.get(0)];
        if (k <= 8) return permSearchResult(A, indices, k);
        return greedyMatchingWeight(A, indices);
    }

    private static double permSearchResult(double[][] A, List<Integer> indices, int k) {
        double[] best = {NEG_INF};
        permSearch(A, indices, k, 0, 0, 0.0, best);
        return best[0];
    }

    private static void permSearch(
            double[][] A, List<Integer> indices, int k,
            int row, int used, double total, double[] best) {
        if (row == k) {
            if (total > best[0]) best[0] = total;
            return;
        }
        for (int col = 0; col < k; col++) {
            if ((used & (1 << col)) != 0) continue;
            double val = A[indices.get(row)][indices.get(col)];
            if (val == NEG_INF) continue;
            permSearch(A, indices, k, row + 1, used | (1 << col), total + val, best);
        }
    }

    private static double greedyMatchingWeight(double[][] A, List<Integer> indices) {
        int k = indices.size();
        Set<Integer> usedCols = new HashSet<>();
        double total = 0.0;
        for (int rowIdx = 0; rowIdx < k; rowIdx++) {
            int i = indices.get(rowIdx);
            double bestVal = NEG_INF;
            int bestCol = -1;
            for (int colIdx = 0; colIdx < k; colIdx++) {
                if (usedCols.contains(colIdx)) continue;
                int j = indices.get(colIdx);
                if (A[i][j] > bestVal) {
                    bestVal = A[i][j];
                    bestCol = colIdx;
                }
            }
            if (bestCol >= 0 && bestVal > NEG_INF) {
                usedCols.add(bestCol);
                total += bestVal;
            } else {
                return NEG_INF;
            }
        }
        return total;
    }

    private static List<Integer> rangeList(int n) {
        List<Integer> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(i);
        return out;
    }

    // =======================================================================
    // Tropical trace
    // =======================================================================

    /** Tropical trace: maximum diagonal of A^k. */
    public static double tropicalTrace(StateSpace ss, int k) {
        double[][] A = Tropical.maxPlusAdjacencyMatrix(ss);
        int n = A.length;
        if (n == 0) return NEG_INF;
        double[][] Ak = Tropical.tropicalMatrixPower(A, k);
        double result = NEG_INF;
        for (int i = 0; i < n; i++) {
            result = Tropical.tropicalAdd(result, Ak[i][i]);
        }
        return result;
    }

    // =======================================================================
    // Full analysis
    // =======================================================================

    /** Complete tropical eigenvalue analysis. */
    public static TropicalEigenResult analyzeTropicalEigen(StateSpace ss) {
        double lam = Tropical.tropicalEigenvalue(ss);
        List<double[]> eigvecs = tropicalEigenspace(ss);
        int dim = eigenspaceDimension(ss);
        List<double[]> subeigvecs = tropicalSubeigenspace(ss);
        List<List<Integer>> comps = criticalComponents(ss);
        int cyc = cyclicity(ss);
        int[] csr = csrDecomposition(ss);
        double[][] kstar = kleeneStar(ss);
        boolean definite = isDefinite(ss);

        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();

        List<Integer> criticalNodes = new ArrayList<>();
        Set<Integer> nodeSet = new TreeSet<>();
        for (List<Integer> comp : comps) nodeSet.addAll(comp);
        criticalNodes.addAll(nodeSet);

        Map<String, Object> viz = new LinkedHashMap<>();
        viz.put("num_states", n);
        viz.put("eigenvalue", lam);
        viz.put("eigenspace_dim", dim);
        viz.put("cyclicity", cyc);
        viz.put("csr_index", csr[0]);
        viz.put("csr_period", csr[1]);
        viz.put("critical_nodes", criticalNodes);
        viz.put("has_kleene_star", kstar != null);
        viz.put("is_definite", definite);

        return new TropicalEigenResult(
                lam, eigvecs, dim, subeigvecs, comps, cyc,
                csr[0], csr[1], kstar, definite, viz);
    }
}
