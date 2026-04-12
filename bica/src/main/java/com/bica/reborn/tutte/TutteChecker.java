package com.bica.reborn.tutte;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Tutte polynomial and protocol reliability analysis (port of Python {@code reticulate.tutte}).
 *
 * <p>The Tutte polynomial T(G; x, y) is a two-variable polynomial that
 * generalises the chromatic polynomial, the flow polynomial, and the
 * reliability polynomial.  For a graph G = (V, E):
 *
 * <pre>
 *     T(G; x, y) = sum over spanning subgraphs A:
 *         (x - 1)^{r(E) - r(A)} * (y - 1)^{|A| - r(A)}
 * </pre>
 *
 * <p>where r(A) = |V| - c(A) is the rank and c(A) is the number of
 * connected components of the subgraph (V, A).
 *
 * <p>All computations are exact (integer arithmetic for Tutte coefficients).
 */
public final class TutteChecker {

    private TutteChecker() {}

    // =======================================================================
    // Result types
    // =======================================================================

    /** Complete Tutte polynomial analysis result. */
    public record TutteResult(
            Map<Pair, Integer> coefficients,
            int numSpanningTrees,
            int numSpanningForests,
            int numConnectedSpanning,
            int chromaticAt2,
            List<Double> reliabilityCoeffs,
            int numStates,
            int numEdges,
            boolean isTree,
            int laplacianSpanningTrees) {}

    /** Integer (i, j) pair for polynomial exponents, usable as map key. */
    public record Pair(int i, int j) {}

    /** Undirected graph representation: nodes and edges. */
    record UndirectedGraph(List<Integer> nodes, List<int[]> edges) {}

    // =======================================================================
    // Graph helpers
    // =======================================================================

    /**
     * Extract the quotient Hasse diagram as an undirected graph.
     */
    static UndirectedGraph hasseUndirected(StateSpace ss) {
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Set<Integer>> sccMembers = sccR.sccMembers();
        List<Zeta.Pair> covers = Zeta.coveringRelation(ss);

        List<Integer> reps = new ArrayList<>(sccMembers.keySet());
        Collections.sort(reps);

        Set<Long> edgeSet = new HashSet<>();
        List<int[]> edges = new ArrayList<>();
        for (Zeta.Pair cover : covers) {
            int rx = sccMap.get(cover.x());
            int ry = sccMap.get(cover.y());
            if (rx != ry) {
                int lo = Math.min(rx, ry);
                int hi = Math.max(rx, ry);
                long key = ((long) lo << 32) | (hi & 0xFFFFFFFFL);
                if (edgeSet.add(key)) {
                    edges.add(new int[]{lo, hi});
                }
            }
        }
        edges.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
        return new UndirectedGraph(reps, edges);
    }

    /**
     * Count connected components via union-find.
     */
    static int connectedComponents(List<Integer> nodes, List<int[]> edges) {
        Map<Integer, Integer> parent = new HashMap<>();
        for (int n : nodes) parent.put(n, n);

        for (int[] e : edges) {
            union(parent, e[0], e[1]);
        }

        Set<Integer> roots = new HashSet<>();
        for (int n : nodes) roots.add(find(parent, n));
        return roots.size();
    }

    private static int find(Map<Integer, Integer> parent, int x) {
        while (parent.get(x) != x) {
            parent.put(x, parent.get(parent.get(x)));
            x = parent.get(x);
        }
        return x;
    }

    private static void union(Map<Integer, Integer> parent, int a, int b) {
        int ra = find(parent, a);
        int rb = find(parent, b);
        if (ra != rb) parent.put(ra, rb);
    }

    // =======================================================================
    // Tutte polynomial via subset expansion
    // =======================================================================

    /**
     * Compute rank and nullity of a spanning subgraph defined by a bitmask.
     */
    private static int[] rankAndNullity(List<Integer> nodes, List<int[]> allEdges, int mask) {
        List<int[]> subEdges = new ArrayList<>();
        int size = 0;
        for (int i = 0; i < allEdges.size(); i++) {
            if ((mask & (1 << i)) != 0) {
                subEdges.add(allEdges.get(i));
                size++;
            }
        }
        int c = connectedComponents(nodes, subEdges);
        int r = nodes.size() - c;
        return new int[]{r, size - r};
    }

    /**
     * Compute the Tutte polynomial T(G; x, y) via subset expansion.
     *
     * @return map from (i, j) to coefficient of x^i * y^j
     */
    public static Map<Pair, Integer> tuttePolynomial(StateSpace ss) {
        UndirectedGraph g = hasseUndirected(ss);
        int m = g.edges().size();
        int n = g.nodes().size();

        if (n == 0) {
            return Map.of(new Pair(0, 0), 1);
        }

        int cFull = connectedComponents(g.nodes(), g.edges());
        int rFull = n - cFull;

        Map<Pair, Integer> coeffs = new HashMap<>();

        for (int mask = 0; mask < (1 << m); mask++) {
            int[] rn = rankAndNullity(g.nodes(), g.edges(), mask);
            int rA = rn[0];
            int nullA = rn[1];
            int expX = rFull - rA;
            int expY = nullA;

            Map<Integer, Integer> xCoeffs = expandBinomial(expX);
            Map<Integer, Integer> yCoeffs = expandBinomial(expY);

            for (var xe : xCoeffs.entrySet()) {
                for (var ye : yCoeffs.entrySet()) {
                    Pair key = new Pair(xe.getKey(), ye.getKey());
                    coeffs.merge(key, xe.getValue() * ye.getValue(), Integer::sum);
                }
            }
        }

        coeffs.values().removeIf(v -> v == 0);
        return coeffs;
    }

    /**
     * Expand (x - 1)^exp as a polynomial in x.
     * @return map from power to coefficient
     */
    static Map<Integer, Integer> expandBinomial(int exp) {
        Map<Integer, Integer> coeffs = new HashMap<>();
        if (exp < 0) {
            coeffs.put(0, 1);
            return coeffs;
        }
        for (int k = 0; k <= exp; k++) {
            int c = comb(exp, k) * pow(-1, exp - k);
            if (c != 0) coeffs.put(k, c);
        }
        return coeffs;
    }

    /**
     * Binomial coefficient C(n, k).
     */
    static int comb(int n, int k) {
        if (k < 0 || k > n) return 0;
        if (k == 0 || k == n) return 1;
        int result = 1;
        int m = Math.min(k, n - k);
        for (int i = 0; i < m; i++) {
            result = result * (n - i) / (i + 1);
        }
        return result;
    }

    private static int pow(int base, int exp) {
        if (exp == 0) return 1;
        int result = 1;
        for (int i = 0; i < exp; i++) result *= base;
        return result;
    }

    // =======================================================================
    // Tutte polynomial evaluation
    // =======================================================================

    /**
     * Evaluate T(G; x, y) at given point.
     */
    public static double evalTutte(Map<Pair, Integer> coeffs, double x, double y) {
        double result = 0.0;
        for (var entry : coeffs.entrySet()) {
            result += entry.getValue() * Math.pow(x, entry.getKey().i()) * Math.pow(y, entry.getKey().j());
        }
        return result;
    }

    /** Count spanning trees: T(G; 1, 1). */
    public static int countSpanningTrees(StateSpace ss) {
        Map<Pair, Integer> coeffs = tuttePolynomial(ss);
        return (int) Math.round(evalTutte(coeffs, 1.0, 1.0));
    }

    /** Count spanning forests: T(G; 2, 1). */
    public static int countSpanningForests(StateSpace ss) {
        Map<Pair, Integer> coeffs = tuttePolynomial(ss);
        return (int) Math.round(evalTutte(coeffs, 2.0, 1.0));
    }

    /** Count connected spanning subgraphs: T(G; 1, 2). */
    public static int countConnectedSpanning(StateSpace ss) {
        Map<Pair, Integer> coeffs = tuttePolynomial(ss);
        return (int) Math.round(evalTutte(coeffs, 1.0, 2.0));
    }

    // =======================================================================
    // Reliability polynomial
    // =======================================================================

    /**
     * Compute the reliability polynomial R(G, p) coefficients.
     *
     * <p>R(G, p) = sum_{k=0}^{m} N_k * p^k * (1-p)^{m-k}
     * where N_k is the number of connected spanning subgraphs with k edges.
     *
     * @return coefficients as a list indexed by power of p
     */
    public static List<Double> reliabilityPolynomial(StateSpace ss) {
        UndirectedGraph g = hasseUndirected(ss);
        int m = g.edges().size();
        int n = g.nodes().size();

        if (n <= 1) {
            return List.of(1.0);
        }

        // Count connected spanning subgraphs by edge count
        Map<Integer, Integer> nk = new HashMap<>();
        for (int mask = 0; mask < (1 << m); mask++) {
            List<int[]> subEdges = new ArrayList<>();
            int edgeCount = 0;
            for (int i = 0; i < m; i++) {
                if ((mask & (1 << i)) != 0) {
                    subEdges.add(g.edges().get(i));
                    edgeCount++;
                }
            }
            int c = connectedComponents(g.nodes(), subEdges);
            if (c == 1) {
                nk.merge(edgeCount, 1, Integer::sum);
            }
        }

        // Build R(G, p) = sum N_k * p^k * (1-p)^{m-k}
        double[] coeffs = new double[m + 1];
        for (var entry : nk.entrySet()) {
            int k = entry.getKey();
            int count = entry.getValue();
            for (int j = 0; j <= m - k; j++) {
                int deg = k + j;
                double c = count * comb(m - k, j) * pow(-1, j);
                if (deg <= m) coeffs[deg] += c;
            }
        }

        List<Double> result = new ArrayList<>();
        for (double c : coeffs) result.add(c);
        return result;
    }

    /** Evaluate the reliability polynomial at probability p. */
    public static double evaluateReliability(StateSpace ss, double p) {
        List<Double> coeffs = reliabilityPolynomial(ss);
        double result = 0.0;
        for (int i = 0; i < coeffs.size(); i++) {
            result += coeffs.get(i) * Math.pow(p, i);
        }
        return result;
    }

    // =======================================================================
    // Kirchhoff's theorem (Laplacian determinant)
    // =======================================================================

    /**
     * Build the Laplacian matrix L = D - A for the undirected graph.
     */
    static int[][] laplacianMatrix(List<Integer> nodes, List<int[]> edges) {
        int n = nodes.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(nodes.get(i), i);
        int[][] L = new int[n][n];

        for (int[] e : edges) {
            int iu = idx.get(e[0]);
            int iv = idx.get(e[1]);
            L[iu][iv] -= 1;
            L[iv][iu] -= 1;
            L[iu][iu] += 1;
            L[iv][iv] += 1;
        }
        return L;
    }

    /**
     * Compute determinant via Gaussian elimination.
     */
    static double determinant(double[][] mat) {
        int n = mat.length;
        if (n == 0) return 1.0;
        if (n == 1) return mat[0][0];

        double[][] work = new double[n][];
        for (int i = 0; i < n; i++) work[i] = mat[i].clone();
        double det = 1.0;

        for (int col = 0; col < n; col++) {
            int pivotRow = -1;
            for (int row = col; row < n; row++) {
                if (Math.abs(work[row][col]) > 1e-12) {
                    pivotRow = row;
                    break;
                }
            }
            if (pivotRow == -1) return 0.0;

            if (pivotRow != col) {
                double[] tmp = work[col];
                work[col] = work[pivotRow];
                work[pivotRow] = tmp;
                det *= -1;
            }

            det *= work[col][col];

            for (int row = col + 1; row < n; row++) {
                if (Math.abs(work[row][col]) > 1e-12) {
                    double factor = work[row][col] / work[col][col];
                    for (int c = col; c < n; c++) {
                        work[row][c] -= factor * work[col][c];
                    }
                }
            }
        }
        return det;
    }

    /**
     * Count spanning trees via Kirchhoff's matrix-tree theorem.
     */
    public static int kirchhoffSpanningTrees(StateSpace ss) {
        UndirectedGraph g = hasseUndirected(ss);
        int n = g.nodes().size();

        if (n <= 1) return n == 1 ? 1 : 0;
        if (g.edges().isEmpty()) return 0;

        int[][] L = laplacianMatrix(g.nodes(), g.edges());

        // Delete first row and column to get cofactor
        double[][] cofactor = new double[n - 1][n - 1];
        for (int i = 1; i < n; i++) {
            for (int j = 1; j < n; j++) {
                cofactor[i - 1][j - 1] = L[i][j];
            }
        }
        double det = determinant(cofactor);
        return Math.max(0, (int) Math.round(det));
    }

    // =======================================================================
    // Full analysis
    // =======================================================================

    /** Complete Tutte polynomial analysis. */
    public static TutteResult analyzeTutte(StateSpace ss) {
        Map<Pair, Integer> coeffs = tuttePolynomial(ss);
        UndirectedGraph g = hasseUndirected(ss);

        int trees = countSpanningTrees(ss);
        int forests = countSpanningForests(ss);
        int connected = countConnectedSpanning(ss);
        int kirchhoff = kirchhoffSpanningTrees(ss);

        int n = g.nodes().size();
        int m = g.edges().size();
        int cFull = connectedComponents(g.nodes(), g.edges());
        int rFull = n - cFull;

        // P(G, 2) = (-1)^r * 2 * T(G; -1, 0)
        double tVal = evalTutte(coeffs, -1.0, 0.0);
        int chrom2 = (int) Math.round(pow(-1, rFull) * 2 * tVal);

        List<Double> relCoeffs = reliabilityPolynomial(ss);

        return new TutteResult(
                coeffs, trees, forests, connected, chrom2, relCoeffs,
                n, m, trees == 1 && m == n - 1, kirchhoff);
    }
}
