package com.bica.reborn.snf;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Smith Normal Form analysis for session type lattices (Step 30ae).
 *
 * <p>The Smith Normal Form (SNF) of an integer matrix A is a diagonal matrix
 * D = U @ A @ V where U, V are unimodular (det = +/-1) integer matrices
 * and D = diag(d1, d2, ..., dk, 0, ..., 0) with d1 | d2 | ... | dk
 * (each invariant factor divides the next).
 *
 * <p>For session type lattices we apply SNF to three matrices derived from
 * the state space (quotient DAG after SCC collapse):
 * <ol>
 *   <li><b>Adjacency matrix</b> -- reveals rank, nullity, torsion of the
 *       directed graph as a Z-module.</li>
 *   <li><b>Incidence matrix</b> (states x edges, signed) -- connects to
 *       simplicial / chain-complex homology of the state space.</li>
 *   <li><b>Laplacian matrix</b> L = D_out - A -- its SNF yields the critical
 *       group (sandpile group / chip-firing group), whose order equals the
 *       number of spanning trees by the Matrix Tree Theorem.</li>
 * </ol>
 *
 * <p>All computations use exact integer arithmetic (no floating point).
 */
public final class SmithNormalFormChecker {

    private SmithNormalFormChecker() {}

    // -----------------------------------------------------------------------
    // Result record
    // -----------------------------------------------------------------------

    /**
     * Complete SNF analysis for a session type state space.
     *
     * @param adjacencySnf        invariant factors of the adjacency matrix (non-zero diagonal entries)
     * @param laplacianSnf        invariant factors of the Laplacian matrix
     * @param incidenceSnf        invariant factors of the incidence matrix
     * @param criticalGroup       non-unit factors of the reduced Laplacian SNF
     * @param criticalGroupOrder  product of critical group factors (= number of spanning trees)
     * @param matrixRank          rank of the adjacency matrix
     * @param nullity             nullity of the adjacency matrix (n - rank)
     */
    public record SmithNormalFormResult(
            List<Integer> adjacencySnf,
            List<Integer> laplacianSnf,
            List<Integer> incidenceSnf,
            List<Integer> criticalGroup,
            int criticalGroupOrder,
            int matrixRank,
            int nullity) {

        public SmithNormalFormResult {
            Objects.requireNonNull(adjacencySnf, "adjacencySnf must not be null");
            Objects.requireNonNull(laplacianSnf, "laplacianSnf must not be null");
            Objects.requireNonNull(incidenceSnf, "incidenceSnf must not be null");
            Objects.requireNonNull(criticalGroup, "criticalGroup must not be null");
            adjacencySnf = List.copyOf(adjacencySnf);
            laplacianSnf = List.copyOf(laplacianSnf);
            incidenceSnf = List.copyOf(incidenceSnf);
            criticalGroup = List.copyOf(criticalGroup);
        }
    }

    /**
     * Result of the Smith Normal Form decomposition: D = U * A * V.
     *
     * @param u the left unimodular transform (m x m)
     * @param d the diagonal result (m x n)
     * @param v the right unimodular transform (n x n)
     */
    public record SnfDecomposition(int[][] u, int[][] d, int[][] v) {
        public SnfDecomposition {
            Objects.requireNonNull(u, "u must not be null");
            Objects.requireNonNull(d, "d must not be null");
            Objects.requireNonNull(v, "v must not be null");
        }
    }

    // -----------------------------------------------------------------------
    // Helpers: deep copy, extended GCD, identity
    // -----------------------------------------------------------------------

    /**
     * Return a deep copy of a 2D integer matrix.
     */
    static int[][] deepCopy(int[][] matrix) {
        int[][] copy = new int[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            copy[i] = matrix[i].clone();
        }
        return copy;
    }

    /**
     * Extended Euclidean algorithm.
     * Returns [g, x, y] such that a*x + b*y = g = gcd(a, b), g >= 0.
     */
    static int[] extendedGcd(int a, int b) {
        if (b == 0) {
            if (a >= 0) return new int[]{a, 1, 0};
            else return new int[]{-a, -1, 0};
        }
        int oldR = a, r = b;
        int oldS = 1, s = 0;
        int oldT = 0, t = 1;
        while (r != 0) {
            int q = oldR / r;
            int tmpR = r; r = oldR - q * r; oldR = tmpR;
            int tmpS = s; s = oldS - q * s; oldS = tmpS;
            int tmpT = t; t = oldT - q * t; oldT = tmpT;
        }
        // Ensure gcd is positive
        if (oldR < 0) {
            oldR = -oldR; oldS = -oldS; oldT = -oldT;
        }
        return new int[]{oldR, oldS, oldT};
    }

    /**
     * Return an n x n identity matrix.
     */
    static int[][] identity(int n) {
        int[][] I = new int[n][n];
        for (int i = 0; i < n; i++) I[i][i] = 1;
        return I;
    }

    // -----------------------------------------------------------------------
    // SCC computation (iterative Tarjan's)
    // -----------------------------------------------------------------------

    private record SccData(
            Map<Integer, Integer> sccMap,
            Map<Integer, Set<Integer>> sccMembers) {}

    private static SccData computeSccs(StateSpace ss) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) {
            adj.put(s, new ArrayList<>());
        }
        for (var t : ss.transitions()) {
            adj.get(t.source()).add(t.target());
        }

        int[] indexCounter = {0};
        Deque<Integer> stack = new ArrayDeque<>();
        Set<Integer> onStack = new HashSet<>();
        Map<Integer, Integer> index = new HashMap<>();
        Map<Integer, Integer> lowlink = new HashMap<>();
        List<Set<Integer>> sccs = new ArrayList<>();

        List<Integer> sortedStates = new ArrayList<>(ss.states());
        Collections.sort(sortedStates);

        for (int start : sortedStates) {
            if (index.containsKey(start)) continue;

            Deque<int[]> callStack = new ArrayDeque<>();
            index.put(start, indexCounter[0]);
            lowlink.put(start, indexCounter[0]);
            indexCounter[0]++;
            stack.push(start);
            onStack.add(start);
            callStack.push(new int[]{start, 0});

            while (!callStack.isEmpty()) {
                int[] frame = callStack.peek();
                int v = frame[0];
                int ni = frame[1];
                List<Integer> neighbors = adj.getOrDefault(v, List.of());

                if (ni < neighbors.size()) {
                    frame[1] = ni + 1;
                    int w = neighbors.get(ni);

                    if (!index.containsKey(w)) {
                        index.put(w, indexCounter[0]);
                        lowlink.put(w, indexCounter[0]);
                        indexCounter[0]++;
                        stack.push(w);
                        onStack.add(w);
                        callStack.push(new int[]{w, 0});
                    } else if (onStack.contains(w)) {
                        lowlink.put(v, Math.min(lowlink.get(v), index.get(w)));
                    }
                } else {
                    if (lowlink.get(v).equals(index.get(v))) {
                        Set<Integer> sccMembers = new HashSet<>();
                        while (true) {
                            int w = stack.pop();
                            onStack.remove(w);
                            sccMembers.add(w);
                            if (w == v) break;
                        }
                        sccs.add(sccMembers);
                    }
                    callStack.pop();
                    if (!callStack.isEmpty()) {
                        int parent = callStack.peek()[0];
                        lowlink.put(parent, Math.min(lowlink.get(parent), lowlink.get(v)));
                    }
                }
            }
        }

        Map<Integer, Integer> sccMap = new HashMap<>();
        Map<Integer, Set<Integer>> sccMembersMap = new HashMap<>();
        for (Set<Integer> scc : sccs) {
            int rep = Collections.min(scc);
            sccMembersMap.put(rep, scc);
            for (int s : scc) {
                sccMap.put(s, rep);
            }
        }
        return new SccData(sccMap, sccMembersMap);
    }

    // -----------------------------------------------------------------------
    // Quotient data
    // -----------------------------------------------------------------------

    private static List<Integer> quotientReps(SccData scc) {
        List<Integer> reps = new ArrayList<>(scc.sccMembers.keySet());
        Collections.sort(reps);
        return reps;
    }

    private static List<int[]> quotientEdges(StateSpace ss, List<Integer> reps, Map<Integer, Integer> sccMap) {
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new HashSet<>());
        for (var t : ss.transitions()) adj.get(t.source()).add(t.target());

        Set<Long> edgeSet = new HashSet<>();
        List<int[]> edges = new ArrayList<>();
        for (int s : ss.states()) {
            int sr = sccMap.get(s);
            for (int t : adj.get(s)) {
                int tr = sccMap.get(t);
                if (sr != tr) {
                    long key = ((long) sr << 32) | (tr & 0xFFFFFFFFL);
                    if (edgeSet.add(key)) {
                        edges.add(new int[]{sr, tr});
                    }
                }
            }
        }
        edges.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
        return edges;
    }

    // -----------------------------------------------------------------------
    // Matrix construction from state space
    // -----------------------------------------------------------------------

    /**
     * Build adjacency matrix on SCC quotient.
     * A[i][j] = 1 if there is a quotient edge from rep_i to rep_j.
     */
    public static int[][] adjacencyMatrix(StateSpace ss) {
        SccData scc = computeSccs(ss);
        List<Integer> reps = quotientReps(scc);
        int n = reps.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(reps.get(i), i);

        int[][] A = new int[n][n];

        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new HashSet<>());
        for (var t : ss.transitions()) adj.get(t.source()).add(t.target());

        for (int s : ss.states()) {
            int sr = scc.sccMap.get(s);
            for (int t : adj.get(s)) {
                int tr = scc.sccMap.get(t);
                if (sr != tr) {
                    A[idx.get(sr)][idx.get(tr)] = 1;
                }
            }
        }
        return A;
    }

    /**
     * Build Laplacian L = D_out - A on SCC quotient.
     * L[i][i] = out-degree, L[i][j] = -A[i][j] for i != j.
     */
    public static int[][] laplacianMatrix(StateSpace ss) {
        int[][] A = adjacencyMatrix(ss);
        int n = A.length;
        int[][] L = new int[n][n];
        for (int i = 0; i < n; i++) {
            int outDeg = 0;
            for (int j = 0; j < n; j++) outDeg += A[i][j];
            for (int j = 0; j < n; j++) {
                if (i == j) L[i][j] = outDeg;
                else L[i][j] = -A[i][j];
            }
        }
        return L;
    }

    /**
     * Build signed incidence matrix (states x edges) on SCC quotient.
     * For each directed edge (u, v): M[u][e] = +1, M[v][e] = -1.
     */
    public static int[][] incidenceMatrix(StateSpace ss) {
        SccData scc = computeSccs(ss);
        List<Integer> reps = quotientReps(scc);
        int n = reps.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(reps.get(i), i);

        List<int[]> edges = quotientEdges(ss, reps, scc.sccMap);
        int m = edges.size();

        if (m == 0) {
            int[][] M = new int[n][];
            for (int i = 0; i < n; i++) M[i] = new int[0];
            return M;
        }

        int[][] M = new int[n][m];
        for (int e = 0; e < m; e++) {
            M[idx.get(edges.get(e)[0])][e] = 1;
            M[idx.get(edges.get(e)[1])][e] = -1;
        }
        return M;
    }

    // -----------------------------------------------------------------------
    // Smith Normal Form computation
    // -----------------------------------------------------------------------

    /**
     * Compute Smith Normal Form: returns (U, D, V) where D = U * A * V.
     * All matrices are integer. U is m x m, D is m x n, V is n x n.
     * U and V are unimodular (|det| = 1).
     */
    public static SnfDecomposition smithNormalForm(int[][] matrix) {
        if (matrix.length == 0) {
            return new SnfDecomposition(new int[0][0], new int[0][0], new int[0][0]);
        }
        int m = matrix.length;
        int n = matrix[0].length;
        if (n == 0) {
            return new SnfDecomposition(identity(m), deepCopy(matrix), new int[0][0]);
        }

        int[][] D = deepCopy(matrix);
        int[][] U = identity(m);
        int[][] V = identity(n);

        int size = Math.min(m, n);
        for (int k = 0; k < size; k++) {
            int maxIterations = 100 * (m + n);
            for (int iter = 0; iter < maxIterations; iter++) {
                // Find smallest non-zero entry in submatrix D[k:, k:]
                int pivotVal = 0, pivotR = -1, pivotC = -1;
                for (int i = k; i < m; i++) {
                    for (int j = k; j < n; j++) {
                        if (D[i][j] != 0) {
                            int av = Math.abs(D[i][j]);
                            if (pivotVal == 0 || av < pivotVal) {
                                pivotVal = av;
                                pivotR = i;
                                pivotC = j;
                            }
                        }
                    }
                }

                if (pivotVal == 0) break;

                // Move pivot to (k, k)
                if (pivotR != k) { swapRows(D, k, pivotR); swapRows(U, k, pivotR); }
                if (pivotC != k) { swapCols(D, k, pivotC); swapCols(V, k, pivotC); }

                // Eliminate column and row
                eliminateColumn(D, U, k, m);
                eliminateRow(D, V, k, n);

                // Check if both column k and row k are clean
                boolean colClean = true;
                for (int i = k + 1; i < m; i++) { if (D[i][k] != 0) { colClean = false; break; } }
                boolean rowClean = true;
                for (int j = k + 1; j < n; j++) { if (D[k][j] != 0) { rowClean = false; break; } }

                if (!colClean || !rowClean) continue;

                // Check divisibility
                if (!checkDivisibility(D, k, m, n)) {
                    fixDivisibility(D, U, V, k, m, n);
                    continue;
                }

                // Make diagonal entry positive
                if (D[k][k] < 0) {
                    for (int j = 0; j < n; j++) D[k][j] = -D[k][j];
                    for (int jj = 0; jj < m; jj++) U[k][jj] = -U[k][jj];
                }
                break;
            }
        }

        return new SnfDecomposition(U, D, V);
    }

    private static void swapRows(int[][] M, int i, int j) {
        int[] tmp = M[i]; M[i] = M[j]; M[j] = tmp;
    }

    private static void swapCols(int[][] M, int i, int j) {
        for (int[] row : M) {
            int tmp = row[i]; row[i] = row[j]; row[j] = tmp;
        }
    }

    private static void eliminateColumn(int[][] D, int[][] U, int k, int m) {
        int n = D[0].length;
        for (int i = k + 1; i < m; i++) {
            if (D[i][k] == 0) continue;
            int[] gxy = extendedGcd(D[k][k], D[i][k]);
            int g = gxy[0], x = gxy[1], y = gxy[2];
            int aOverG = D[k][k] / g;
            int bOverG = D[i][k] / g;

            int[] newK = new int[n];
            int[] newI = new int[n];
            for (int j = 0; j < n; j++) {
                newK[j] = x * D[k][j] + y * D[i][j];
                newI[j] = -bOverG * D[k][j] + aOverG * D[i][j];
            }
            D[k] = newK;
            D[i] = newI;

            int colsU = U[0].length;
            int[] newUk = new int[colsU];
            int[] newUi = new int[colsU];
            for (int j = 0; j < colsU; j++) {
                newUk[j] = x * U[k][j] + y * U[i][j];
                newUi[j] = -bOverG * U[k][j] + aOverG * U[i][j];
            }
            U[k] = newUk;
            U[i] = newUi;
        }
    }

    private static void eliminateRow(int[][] D, int[][] V, int k, int n) {
        int m = D.length;
        for (int j = k + 1; j < n; j++) {
            if (D[k][j] == 0) continue;

            if (D[k][k] != 0 && D[k][j] % D[k][k] == 0) {
                // Simple case: col_j -= q * col_k
                int q = D[k][j] / D[k][k];
                for (int i = 0; i < m; i++) D[i][j] -= q * D[i][k];
                int rowsV = V.length;
                for (int i = 0; i < rowsV; i++) V[i][j] -= q * V[i][k];
            } else {
                // General case: extended GCD column operation
                int[] gxy = extendedGcd(D[k][k], D[k][j]);
                int g = gxy[0], x = gxy[1], y = gxy[2];
                int aOverG = D[k][k] / g;
                int bOverG = D[k][j] / g;

                for (int i = 0; i < m; i++) {
                    int oldK = D[i][k], oldJ = D[i][j];
                    D[i][k] = x * oldK + y * oldJ;
                    D[i][j] = -bOverG * oldK + aOverG * oldJ;
                }
                int rowsV = V.length;
                for (int i = 0; i < rowsV; i++) {
                    int oldK = V[i][k], oldJ = V[i][j];
                    V[i][k] = x * oldK + y * oldJ;
                    V[i][j] = -bOverG * oldK + aOverG * oldJ;
                }
            }
        }
    }

    private static boolean checkDivisibility(int[][] D, int k, int m, int n) {
        int d = D[k][k];
        if (d == 0) return true;
        for (int i = k + 1; i < m; i++) {
            for (int j = k + 1; j < n; j++) {
                if (D[i][j] % d != 0) return false;
            }
        }
        return true;
    }

    private static void fixDivisibility(int[][] D, int[][] U, int[][] V, int k, int m, int n) {
        int d = D[k][k];
        for (int i = k + 1; i < m; i++) {
            for (int j = k + 1; j < n; j++) {
                if (D[i][j] % d != 0) {
                    int cols = D[0].length;
                    for (int c = 0; c < cols; c++) D[k][c] += D[i][c];
                    int colsU = U[0].length;
                    for (int c = 0; c < colsU; c++) U[k][c] += U[i][c];
                    return;
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Invariant factors extraction
    // -----------------------------------------------------------------------

    /**
     * Extract invariant factors (non-zero diagonal entries of SNF).
     * Returns [d1, d2, ..., dk] where d1 | d2 | ... | dk > 0.
     */
    public static List<Integer> invariantFactors(int[][] matrix) {
        if (matrix.length == 0) return List.of();
        if (matrix[0].length == 0) return List.of();

        SnfDecomposition snf = smithNormalForm(matrix);
        int[][] D = snf.d();
        int m = D.length;
        int n = D[0].length;
        List<Integer> factors = new ArrayList<>();
        for (int i = 0; i < Math.min(m, n); i++) {
            int d = D[i][i];
            if (d != 0) factors.add(Math.abs(d));
        }
        factors.sort(Integer::compareTo);
        return factors;
    }

    // -----------------------------------------------------------------------
    // Critical group (sandpile group)
    // -----------------------------------------------------------------------

    /**
     * Compute the critical group (sandpile group) from reduced Laplacian SNF.
     * Returns list of factors > 1, e.g. [2, 6] means Z/2Z x Z/6Z.
     */
    public static List<Integer> criticalGroup(StateSpace ss) {
        int[][] L = laplacianMatrix(ss);
        int n = L.length;
        if (n <= 1) return List.of();

        // Find bottom state's index in the quotient
        SccData scc = computeSccs(ss);
        List<Integer> reps = quotientReps(scc);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < reps.size(); i++) idx.put(reps.get(i), i);

        int bottom = ss.bottom();
        int bottomRep = scc.sccMap.getOrDefault(bottom, reps.get(reps.size() - 1));
        int bottomIdx = idx.getOrDefault(bottomRep, n - 1);

        // Build reduced Laplacian: delete row and column of bottom
        int[][] reduced = new int[n - 1][n - 1];
        int ri = 0;
        for (int i = 0; i < n; i++) {
            if (i == bottomIdx) continue;
            int rj = 0;
            for (int j = 0; j < n; j++) {
                if (j == bottomIdx) continue;
                reduced[ri][rj] = L[i][j];
                rj++;
            }
            ri++;
        }

        if (reduced.length == 0) return List.of();

        List<Integer> factors = invariantFactors(reduced);
        List<Integer> result = new ArrayList<>();
        for (int f : factors) {
            if (f > 1) result.add(f);
        }
        return result;
    }

    /**
     * Order of the critical group = number of spanning trees (Matrix Tree Theorem).
     */
    public static int criticalGroupOrder(StateSpace ss) {
        List<Integer> cg = criticalGroup(ss);
        if (!cg.isEmpty()) {
            int product = 1;
            for (int f : cg) product *= f;
            return product;
        }

        // Compute from reduced Laplacian determinant
        int[][] L = laplacianMatrix(ss);
        int n = L.length;
        if (n <= 1) return 1;

        SccData scc = computeSccs(ss);
        List<Integer> reps = quotientReps(scc);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < reps.size(); i++) idx.put(reps.get(i), i);

        int bottom = ss.bottom();
        int bottomRep = scc.sccMap.getOrDefault(bottom, reps.get(reps.size() - 1));
        int bottomIdx = idx.getOrDefault(bottomRep, n - 1);

        int[][] reduced = new int[n - 1][n - 1];
        int ri = 0;
        for (int i = 0; i < n; i++) {
            if (i == bottomIdx) continue;
            int rj = 0;
            for (int j = 0; j < n; j++) {
                if (j == bottomIdx) continue;
                reduced[ri][rj] = L[i][j];
                rj++;
            }
            ri++;
        }

        if (reduced.length == 0) return 1;
        int det = determinant(reduced);
        return det != 0 ? Math.abs(det) : 1;
    }

    // -----------------------------------------------------------------------
    // Determinant (Bareiss algorithm)
    // -----------------------------------------------------------------------

    /**
     * Compute determinant of a square integer matrix using fraction-free
     * Gaussian elimination (Bareiss algorithm).
     */
    static int determinant(int[][] matrix) {
        int n = matrix.length;
        if (n == 0) return 1;
        if (n == 1) return matrix[0][0];

        int[][] M = deepCopy(matrix);
        int sign = 1;

        for (int k = 0; k < n; k++) {
            // Find pivot
            int pivotRow = -1;
            for (int i = k; i < n; i++) {
                if (M[i][k] != 0) { pivotRow = i; break; }
            }
            if (pivotRow == -1) return 0; // Singular

            if (pivotRow != k) {
                int[] tmp = M[k]; M[k] = M[pivotRow]; M[pivotRow] = tmp;
                sign = -sign;
            }

            for (int i = k + 1; i < n; i++) {
                for (int j = k + 1; j < n; j++) {
                    M[i][j] = M[k][k] * M[i][j] - M[i][k] * M[k][j];
                    if (k > 0) {
                        int prev = M[k - 1][k - 1];
                        if (prev != 0) M[i][j] /= prev;
                    }
                }
                M[i][k] = 0;
            }
        }

        return sign * M[n - 1][n - 1];
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    /**
     * Full SNF analysis of a session type state space.
     */
    public static SmithNormalFormResult analyze(StateSpace ss) {
        int[][] A = adjacencyMatrix(ss);
        int[][] L = laplacianMatrix(ss);
        int[][] Inc = incidenceMatrix(ss);

        List<Integer> adjFactors = invariantFactors(A);
        List<Integer> lapFactors = invariantFactors(L);
        List<Integer> incFactors = invariantFactors(Inc);

        List<Integer> cg = criticalGroup(ss);
        int cgOrder = criticalGroupOrder(ss);

        int n = A.length;
        int rank = adjFactors.size();
        int nullity = n - rank;

        return new SmithNormalFormResult(
                adjFactors, lapFactors, incFactors, cg, cgOrder, rank, nullity);
    }

    // -----------------------------------------------------------------------
    // Matrix multiplication (for test verification)
    // -----------------------------------------------------------------------

    /**
     * Integer matrix multiplication. Exposed as package-private for tests.
     */
    static int[][] matmul(int[][] A, int[][] B) {
        int m = A.length;
        int p = B.length;
        int n = (p > 0) ? B[0].length : 0;
        int[][] C = new int[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < p; k++) {
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return C;
    }
}
