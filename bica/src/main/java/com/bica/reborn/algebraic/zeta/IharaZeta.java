package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Ihara zeta function analysis for session type lattices
 * (port of Python {@code reticulate.ihara}, Step 30s).
 *
 * <p>The Ihara zeta function of a graph {@code G} is
 * {@code Z_G(u) = ∏_[p] (1 - u^|p|)^{-1}} where the product runs over
 * equivalence classes of prime (backtrackless, tailless) closed walks.
 * Bass's generalisation of Ihara's theorem gives the closed form
 *
 * <pre>
 *   Z_G(u)^{-1} = (1 - u^2)^{r-1} · det(I - A u + (D - I) u^2)
 * </pre>
 *
 * where {@code A} is the adjacency matrix of the undirected version of
 * the state-space graph, {@code D} the degree matrix, and
 * {@code r = |E| - |V| + components} the cycle rank.
 *
 * <p>This class is a faithful port of the Python module. All methods are
 * static, matrices are {@code double[][]}, and all computations are pure
 * Java (no external linear-algebra dependency).
 */
public final class IharaZeta {

    private IharaZeta() {}

    // =======================================================================
    // Result type
    // =======================================================================

    /** Complete Ihara zeta analysis result. */
    public record IharaResult(
            double iharaDeterminant,
            List<double[]> reciprocalPoles, // each pole encoded as [real, imag]
            int numPrimeCycles,
            List<Integer> primeCycleLengths,
            int rank,
            boolean isRamanujan,
            double complexity,
            double[][] bassHashimotoMatrix) {}

    // =======================================================================
    // Undirected edge helpers
    // =======================================================================

    /** Extract undirected edges from the state space (self-loops excluded, dedup). */
    public static List<int[]> undirectedEdges(StateSpace ss) {
        Set<Long> seen = new HashSet<>();
        List<int[]> edges = new ArrayList<>();
        for (var t : ss.transitions()) {
            int src = t.source();
            int tgt = t.target();
            if (src == tgt) continue;
            int lo = Math.min(src, tgt);
            int hi = Math.max(src, tgt);
            long key = (((long) lo) << 32) | (hi & 0xffffffffL);
            if (seen.add(key)) {
                edges.add(new int[]{lo, hi});
            }
        }
        return edges;
    }

    /** Undirected adjacency list. */
    public static Map<Integer, List<Integer>> undirectedAdjacency(StateSpace ss) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new ArrayList<>());
        for (int[] e : undirectedEdges(ss)) {
            int u = e[0], v = e[1];
            if (!adj.get(u).contains(v)) adj.get(u).add(v);
            if (!adj.get(v).contains(u)) adj.get(v).add(u);
        }
        return adj;
    }

    // =======================================================================
    // Adjacency / degree matrices
    // =======================================================================

    /** Undirected adjacency matrix; returns {A, states}. */
    public static double[][] adjacencyMatrix(StateSpace ss, List<Integer> outStates) {
        List<Integer> states = Zeta.stateList(ss);
        outStates.clear();
        outStates.addAll(states);
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);
        double[][] A = new double[n][n];
        for (int[] e : undirectedEdges(ss)) {
            int ui = idx.get(e[0]);
            int vi = idx.get(e[1]);
            A[ui][vi] = 1.0;
            A[vi][ui] = 1.0;
        }
        return A;
    }

    /** Degree matrix (diagonal). */
    public static double[][] degreeMatrix(StateSpace ss) {
        List<Integer> states = new ArrayList<>();
        double[][] A = adjacencyMatrix(ss, states);
        int n = states.size();
        double[][] D = new double[n][n];
        for (int i = 0; i < n; i++) {
            double s = 0.0;
            for (int j = 0; j < n; j++) s += A[i][j];
            D[i][i] = s;
        }
        return D;
    }

    // =======================================================================
    // Linear algebra (pure Java)
    // =======================================================================

    public static double[][] matIdentity(int n) {
        double[][] I = new double[n][n];
        for (int i = 0; i < n; i++) I[i][i] = 1.0;
        return I;
    }

    public static double[][] matAdd(double[][] A, double[][] B) {
        int n = A.length;
        double[][] C = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                C[i][j] = A[i][j] + B[i][j];
        return C;
    }

    public static double[][] matSub(double[][] A, double[][] B) {
        int n = A.length;
        double[][] C = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                C[i][j] = A[i][j] - B[i][j];
        return C;
    }

    public static double[][] matScale(double[][] A, double c) {
        int n = A.length;
        double[][] C = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                C[i][j] = A[i][j] * c;
        return C;
    }

    public static double[][] matMul(double[][] A, double[][] B) {
        int n = A.length;
        if (n == 0) return new double[0][0];
        int p = B.length;
        int m = B[0].length;
        double[][] C = new double[n][m];
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < p; k++) {
                double aik = A[i][k];
                if (aik == 0.0) continue;
                for (int j = 0; j < m; j++) {
                    C[i][j] += aik * B[k][j];
                }
            }
        }
        return C;
    }

    /** Determinant via LU with partial pivoting. */
    public static double matDet(double[][] M) {
        int n = M.length;
        if (n == 0) return 1.0;
        if (n == 1) return M[0][0];
        if (n == 2) return M[0][0] * M[1][1] - M[0][1] * M[1][0];
        double[][] A = new double[n][n];
        for (int i = 0; i < n; i++) A[i] = M[i].clone();
        double sign = 1.0;
        for (int col = 0; col < n; col++) {
            double maxVal = Math.abs(A[col][col]);
            int maxRow = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(A[row][col]) > maxVal) {
                    maxVal = Math.abs(A[row][col]);
                    maxRow = row;
                }
            }
            if (maxVal < 1e-15) return 0.0;
            if (maxRow != col) {
                double[] tmp = A[col];
                A[col] = A[maxRow];
                A[maxRow] = tmp;
                sign = -sign;
            }
            double pivot = A[col][col];
            for (int row = col + 1; row < n; row++) {
                double factor = A[row][col] / pivot;
                for (int j = col + 1; j < n; j++) {
                    A[row][j] -= factor * A[col][j];
                }
                A[row][col] = 0.0;
            }
        }
        double det = sign;
        for (int i = 0; i < n; i++) det *= A[i][i];
        return det;
    }

    /** Eigenvalues of a real symmetric matrix via shifted QR iteration. */
    public static double[] eigenvaluesSymmetric(double[][] M) {
        int n = M.length;
        if (n == 0) return new double[0];
        if (n == 1) return new double[]{M[0][0]};
        if (n == 2) {
            double a = M[0][0], b = M[0][1], d = M[1][1];
            double trace = a + d;
            double det = a * d - b * b;
            double disc = trace * trace - 4 * det;
            if (disc < 0) disc = 0.0;
            double sq = Math.sqrt(disc);
            double e1 = (trace + sq) / 2.0;
            double e2 = (trace - sq) / 2.0;
            double[] r = new double[]{Math.max(e1, e2), Math.min(e1, e2)};
            return r;
        }
        double[][] A = new double[n][n];
        for (int i = 0; i < n; i++) A[i] = M[i].clone();

        int maxIter = 300 * n;
        for (int iter = 0; iter < maxIter; iter++) {
            double shift = A[n - 1][n - 1];
            for (int i = 0; i < n; i++) A[i][i] -= shift;

            // QR via modified Gram-Schmidt; Q stored as columns
            double[][] Qcols = new double[n][n];
            double[][] R = new double[n][n];
            for (int j = 0; j < n; j++) {
                double[] v = new double[n];
                for (int i = 0; i < n; i++) v[i] = A[i][j];
                for (int k = 0; k < j; k++) {
                    double[] qk = Qcols[k];
                    double dot = 0.0;
                    for (int i = 0; i < n; i++) dot += qk[i] * v[i];
                    R[k][j] = dot;
                    for (int i = 0; i < n; i++) v[i] -= dot * qk[i];
                }
                double norm = 0.0;
                for (double x : v) norm += x * x;
                norm = Math.sqrt(norm);
                if (norm < 1e-15) {
                    R[j][j] = 0.0;
                    Qcols[j] = new double[n];
                } else {
                    R[j][j] = norm;
                    double[] col = new double[n];
                    for (int i = 0; i < n; i++) col[i] = v[i] / norm;
                    Qcols[j] = col;
                }
            }

            // A_new = R * Q^T + shift*I
            double[][] An = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    double s = 0.0;
                    for (int k = 0; k < n; k++) s += R[i][k] * Qcols[k][j];
                    An[i][j] = s;
                }
            }
            for (int i = 0; i < n; i++) An[i][i] += shift;
            A = An;

            boolean converged = true;
            for (int i = 1; i < n; i++) {
                if (Math.abs(A[i][i - 1]) > 1e-10) { converged = false; break; }
            }
            if (converged) break;
        }
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = A[i][i];
        // Sort descending
        Arrays.sort(out);
        double[] rev = new double[n];
        for (int i = 0; i < n; i++) rev[i] = out[n - 1 - i];
        return rev;
    }

    // =======================================================================
    // Core Ihara
    // =======================================================================

    /** Cycle rank r = |E| - |V| + components (undirected). */
    public static int cycleRank(StateSpace ss) {
        List<int[]> edges = undirectedEdges(ss);
        int nE = edges.size();
        int nV = ss.states().size();
        Map<Integer, List<Integer>> adj = undirectedAdjacency(ss);
        Set<Integer> visited = new HashSet<>();
        int components = 0;
        List<Integer> sorted = new ArrayList<>(ss.states());
        Collections.sort(sorted);
        for (int s : sorted) {
            if (visited.contains(s)) continue;
            components++;
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(s);
            while (!stack.isEmpty()) {
                int u = stack.pop();
                if (!visited.add(u)) continue;
                for (int v : adj.getOrDefault(u, List.of())) {
                    if (!visited.contains(v)) stack.push(v);
                }
            }
        }
        return nE - nV + components;
    }

    /** Evaluate 1/Z_G(u) via Bass's formula. */
    public static double iharaDeterminant(StateSpace ss, double u) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        if (n == 0) return 1.0;
        if (Math.abs(u) < 1e-15) return 1.0;

        List<Integer> tmp = new ArrayList<>();
        double[][] A = adjacencyMatrix(ss, tmp);
        double[][] D = degreeMatrix(ss);
        int r = cycleRank(ss);

        double[][] I = matIdentity(n);
        double[][] Au = matScale(A, u);
        double[][] DmI = matSub(D, I);
        double[][] DmIu2 = matScale(DmI, u * u);
        double[][] M = matAdd(matSub(I, Au), DmIu2);
        double detM = matDet(M);

        double base = 1.0 - u * u;
        int exp = r - 1;
        double prefix;
        if (Math.abs(base) < 1e-15 && exp < 0) {
            return detM != 0.0 ? Double.POSITIVE_INFINITY : Double.NaN;
        }
        if (Math.abs(base) > 1e-15 || exp >= 0) {
            prefix = Math.pow(base, exp);
        } else {
            prefix = 1.0;
        }
        return prefix * detM;
    }

    /** Build the Hashimoto (non-backtracking edge adjacency) matrix. */
    public static double[][] bassHashimotoMatrix(StateSpace ss) {
        List<int[]> undirected = undirectedEdges(ss);
        List<int[]> oriented = new ArrayList<>();
        for (int[] e : undirected) {
            oriented.add(new int[]{e[0], e[1]});
            oriented.add(new int[]{e[1], e[0]});
        }
        int m = oriented.size();
        if (m == 0) return new double[0][0];
        double[][] B = new double[m][m];
        for (int i = 0; i < m; i++) {
            int u1 = oriented.get(i)[0];
            int v1 = oriented.get(i)[1];
            for (int j = 0; j < m; j++) {
                int u2 = oriented.get(j)[0];
                int v2 = oriented.get(j)[1];
                if (v1 == u2 && !(u2 == v1 && v2 == u1)) {
                    B[i][j] = 1.0;
                }
            }
        }
        return B;
    }

    // =======================================================================
    // Prime cycles
    // =======================================================================

    public static int countPrimeCycles(StateSpace ss) { return countPrimeCycles(ss, 10); }

    public static int countPrimeCycles(StateSpace ss, int maxLength) {
        return primeCycleLengths(ss, maxLength).size();
    }

    public static List<Integer> primeCycleLengths(StateSpace ss) { return primeCycleLengths(ss, 10); }

    public static List<Integer> primeCycleLengths(StateSpace ss, int maxLength) {
        Map<Integer, List<Integer>> adj = undirectedAdjacency(ss);
        List<Integer> states = new ArrayList<>(ss.states());
        Collections.sort(states);
        if (states.isEmpty()) return List.of();

        List<List<Integer>> allCycles = new ArrayList<>();
        for (int start : states) {
            List<Integer> path = new ArrayList<>();
            path.add(start);
            findBacktracklessCycles(adj, start, path, -1, maxLength, allCycles);
        }

        Set<List<Integer>> canonical = new HashSet<>();
        List<Integer> lengths = new ArrayList<>();
        for (List<Integer> cycle : allCycles) {
            // cycle is v0, v1, ..., vk where v0 == vk
            List<Integer> pathOnly = cycle.subList(0, cycle.size() - 1);
            int k = pathOnly.size();
            if (k == 0) continue;
            if (isProperPower(pathOnly)) continue;
            List<Integer> canon = canonicalRotation(pathOnly);
            if (canonical.add(canon)) {
                lengths.add(k);
            }
        }
        Collections.sort(lengths);
        return lengths;
    }

    private static void findBacktracklessCycles(
            Map<Integer, List<Integer>> adj,
            int start,
            List<Integer> path,
            int prev,
            int maxLength,
            List<List<Integer>> result) {
        int current = path.get(path.size() - 1);
        int length = path.size() - 1;
        if (length > maxLength) return;
        for (int neighbor : adj.getOrDefault(current, List.of())) {
            if (neighbor == prev) continue;
            if (neighbor == start && length >= 3) {
                List<Integer> c = new ArrayList<>(path);
                c.add(start);
                result.add(c);
                continue;
            }
            if (neighbor == start && length < 3) continue;
            // skip revisiting any non-start vertex already in path[1:]
            boolean seen = false;
            for (int i = 1; i < path.size(); i++) {
                if (path.get(i) == neighbor) { seen = true; break; }
            }
            if (seen) continue;
            path.add(neighbor);
            findBacktracklessCycles(adj, start, path, current, maxLength, result);
            path.remove(path.size() - 1);
        }
    }

    public static boolean isProperPower(List<Integer> path) {
        int n = path.size();
        for (int d = 1; d < n; d++) {
            if (n % d != 0) continue;
            if (d == n) continue;
            boolean isPower = true;
            for (int i = 0; i < n; i++) {
                if (!path.get(i).equals(path.get(i % d))) { isPower = false; break; }
            }
            if (isPower) return true;
        }
        return false;
    }

    public static List<Integer> canonicalRotation(List<Integer> path) {
        int n = path.size();
        if (n == 0) return List.of();
        List<Integer> best = new ArrayList<>(path);
        for (int i = 1; i < n; i++) {
            List<Integer> rotated = new ArrayList<>(n);
            for (int j = 0; j < n; j++) rotated.add(path.get((i + j) % n));
            if (compareLists(rotated, best) < 0) best = rotated;
        }
        return best;
    }

    private static int compareLists(List<Integer> a, List<Integer> b) {
        int n = Math.min(a.size(), b.size());
        for (int i = 0; i < n; i++) {
            int c = Integer.compare(a.get(i), b.get(i));
            if (c != 0) return c;
        }
        return Integer.compare(a.size(), b.size());
    }

    // =======================================================================
    // Ramanujan
    // =======================================================================

    public static boolean isRamanujan(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        if (n <= 1) return true;
        List<int[]> edges = undirectedEdges(ss);
        if (edges.isEmpty()) return true;
        List<Integer> tmp = new ArrayList<>();
        double[][] A = adjacencyMatrix(ss, tmp);
        double[] degrees = new double[n];
        double dMax = 0, dMin = Double.POSITIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            double s = 0.0;
            for (int j = 0; j < n; j++) s += A[i][j];
            degrees[i] = s;
            if (s > dMax) dMax = s;
            if (s < dMin) dMin = s;
        }
        if (dMax < 1) return true;
        boolean isRegular = (dMax == dMin);
        double[] eigenvalues = eigenvaluesSymmetric(A);

        if (isRegular) {
            double q = dMax - 1;
            double bound = q > 0 ? 2.0 * Math.sqrt(q) : 0.0;
            for (double ev : eigenvalues) {
                if (Math.abs(Math.abs(ev) - dMax) > 1e-8) {
                    if (Math.abs(ev) > bound + 1e-8) return false;
                }
            }
            return true;
        } else {
            if (eigenvalues.length < 2) return true;
            double[] absEigs = new double[eigenvalues.length];
            for (int i = 0; i < eigenvalues.length; i++) absEigs[i] = Math.abs(eigenvalues[i]);
            Arrays.sort(absEigs);
            double second = absEigs[absEigs.length - 2];
            double bound = dMax > 1 ? 2.0 * Math.sqrt(dMax - 1) : 0.0;
            return second <= bound + 1e-8;
        }
    }

    // =======================================================================
    // Poles
    // =======================================================================

    /** Poles of 1/Z_G(u); each pole is {real, imag}. */
    public static List<double[]> iharaPoles(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        if (n == 0) return new ArrayList<>();
        List<int[]> edges = undirectedEdges(ss);
        if (edges.isEmpty()) return new ArrayList<>();

        List<Integer> tmp = new ArrayList<>();
        double[][] A = adjacencyMatrix(ss, tmp);
        double[] eigenvalues = eigenvaluesSymmetric(A);

        double[] degrees = new double[n];
        double dMax = 0, dMin = Double.POSITIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            double s = 0.0;
            for (int j = 0; j < n; j++) s += A[i][j];
            degrees[i] = s;
            if (s > dMax) dMax = s;
            if (s < dMin) dMin = s;
        }
        boolean isRegular = (dMax == dMin) && dMax > 0;
        List<double[]> poles = new ArrayList<>();

        int r = cycleRank(ss);
        if (r > 0) {
            for (int i = 0; i < r - 1; i++) {
                poles.add(new double[]{1.0, 0.0});
                poles.add(new double[]{-1.0, 0.0});
            }
        }

        if (isRegular) {
            double q = dMax - 1;
            for (double lam : eigenvalues) {
                if (q == 0) {
                    if (Math.abs(lam) > 1e-15) poles.add(new double[]{1.0 / lam, 0.0});
                    continue;
                }
                double disc = lam * lam - 4 * q;
                if (disc >= 0) {
                    double sq = Math.sqrt(disc);
                    poles.add(new double[]{(lam + sq) / (2 * q), 0.0});
                    poles.add(new double[]{(lam - sq) / (2 * q), 0.0});
                } else {
                    double sq = Math.sqrt(-disc);
                    poles.add(new double[]{lam / (2 * q), sq / (2 * q)});
                    poles.add(new double[]{lam / (2 * q), -sq / (2 * q)});
                }
            }
        } else {
            // Irregular: mirror Python's approximation loop
            double[][] B = bassHashimotoMatrix(ss);
            if (B.length > 0) {
                double sumD = 0.0;
                for (double d : degrees) sumD += d;
                double avgD = n > 0 ? sumD / n : 0.0;
                double qEff = avgD > 1 ? avgD - 1 : 0.5;
                for (double lam : eigenvalues) {
                    if (qEff > 0) {
                        double disc = lam * lam - 4 * qEff;
                        if (disc >= 0) {
                            double sq = Math.sqrt(disc);
                            poles.add(new double[]{(lam + sq) / (2 * qEff), 0.0});
                            poles.add(new double[]{(lam - sq) / (2 * qEff), 0.0});
                        } else {
                            double sq = Math.sqrt(-disc);
                            poles.add(new double[]{lam / (2 * qEff), sq / (2 * qEff)});
                            poles.add(new double[]{lam / (2 * qEff), -sq / (2 * qEff)});
                        }
                    }
                }
            }
        }

        poles.sort((p, q) -> {
            double mp = Math.hypot(p[0], p[1]);
            double mq = Math.hypot(q[0], q[1]);
            int c = Double.compare(mp, mq);
            if (c != 0) return c;
            c = Double.compare(p[0], q[0]);
            if (c != 0) return c;
            return Double.compare(p[1], q[1]);
        });
        return poles;
    }

    // =======================================================================
    // Graph complexity
    // =======================================================================

    public static double graphComplexity(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        if (n <= 1) return 0.0;
        List<int[]> edges = undirectedEdges(ss);
        if (edges.isEmpty()) return 0.0;
        int r = cycleRank(ss);
        if (r == 0) return 0.0;

        List<Integer> tmp = new ArrayList<>();
        double[][] A = adjacencyMatrix(ss, tmp);
        double sumD = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) sumD += A[i][j];
        }
        double avgD = sumD / n;
        double q = avgD > 1 ? avgD - 1 : 0.5;
        double u = q > 0 ? 1.0 / Math.sqrt(q) : 0.5;
        if (u >= 1.0) u = 0.9;

        double detVal = iharaDeterminant(ss, u);
        if (Math.abs(detVal) < 1e-15) return Double.POSITIVE_INFINITY;
        double reciprocal = Math.abs(detVal);
        if (reciprocal < 1e-15) return Double.POSITIVE_INFINITY;
        return reciprocal > 0 ? -Math.log(reciprocal) : 0.0;
    }

    // =======================================================================
    // Main entry point
    // =======================================================================

    public static IharaResult analyzeIhara(StateSpace ss) {
        int r = cycleRank(ss);
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();

        double detVal;
        if (n > 0) {
            List<Integer> tmp = new ArrayList<>();
            double[][] A = adjacencyMatrix(ss, tmp);
            double sumD = 0.0;
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++) sumD += A[i][j];
            double avgD = sumD / n;
            double q = avgD > 1 ? avgD - 1 : 0.5;
            double uEval = q > 0 ? 1.0 / (q + 1) : 0.5;
            if (uEval >= 1.0) uEval = 0.5;
            detVal = iharaDeterminant(ss, uEval);
        } else {
            detVal = 1.0;
        }

        List<double[]> poles = iharaPoles(ss);
        List<Integer> lens = primeCycleLengths(ss, 10);
        int num = lens.size();
        boolean ram = isRamanujan(ss);
        double compl = graphComplexity(ss);
        double[][] B = bassHashimotoMatrix(ss);

        return new IharaResult(detVal, poles, num, lens, r, ram, compl, B);
    }
}
