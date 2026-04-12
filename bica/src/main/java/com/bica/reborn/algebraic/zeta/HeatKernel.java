package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Heat kernel analysis for session type lattices (port of Python
 * {@code reticulate.heat_kernel}, Step 30q).
 *
 * <p>The heat kernel {@code H_t = exp(-tL)} where {@code L} is the graph
 * Laplacian of the undirected quotient graph describes heat diffusion on
 * the session-type state space. This is the diffusion-side companion to
 * {@link RandomWalk} (Step 30p).
 *
 * <p>All computations use pure {@code double[][]} arithmetic. The symmetric
 * eigendecomposition uses Jacobi rotation (suitable for small Laplacians,
 * up to ~50 states). Results match the Python reference up to numerical
 * tolerance.
 */
public final class HeatKernel {

    private HeatKernel() {}

    /** Default sample times, matching the Python module. */
    public static final double[] DEFAULT_SAMPLE_TIMES =
            {0.01, 0.1, 0.5, 1.0, 2.0, 5.0, 10.0};

    private static final double EPS = 1e-12;

    // =======================================================================
    // Result type
    // =======================================================================

    /** Complete heat kernel analysis. */
    public record HeatKernelResult(
            double[][] laplacian,
            double[] eigenvalues,
            double[][] eigenvectors,
            double[] heatTrace,
            double[] sampleTimes,
            double[][] hksDiagonal,
            double[] totalHeatContent,
            double[][] diffusionDistance) {}

    // =======================================================================
    // Undirected quotient adjacency
    // =======================================================================

    private record QuotientAdj(List<Integer> reps, Map<Integer, Set<Integer>> uAdj) {}

    private static QuotientAdj quotientStatesAndAdj(StateSpace ss) {
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Set<Integer>> sccMembers = sccR.sccMembers();
        Map<Integer, List<Integer>> adj = Zeta.adjacency(ss);

        List<Integer> reps = new ArrayList<>(sccMembers.keySet());
        Collections.sort(reps);
        Map<Integer, Set<Integer>> qAdj = new HashMap<>();
        for (int r : reps) qAdj.put(r, new HashSet<>());
        for (int s : ss.states()) {
            for (int t : adj.getOrDefault(s, List.of())) {
                int sr = sccMap.get(s);
                int tr = sccMap.get(t);
                if (sr != tr) qAdj.get(sr).add(tr);
            }
        }
        // Symmetrise
        Map<Integer, Set<Integer>> uAdj = new HashMap<>();
        for (int r : reps) uAdj.put(r, new HashSet<>());
        for (int r : reps) {
            for (int t : qAdj.get(r)) {
                uAdj.get(r).add(t);
                uAdj.get(t).add(r);
            }
        }
        return new QuotientAdj(reps, uAdj);
    }

    private static double[][] symmetricAdjacencyMatrix(
            List<Integer> reps, Map<Integer, Set<Integer>> uAdj) {
        int n = reps.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(reps.get(i), i);
        double[][] A = new double[n][n];
        for (int r : reps) {
            for (int t : uAdj.get(r)) {
                A[idx.get(r)][idx.get(t)] = 1.0;
            }
        }
        return A;
    }

    // =======================================================================
    // Graph Laplacian
    // =======================================================================

    /** Symmetric graph Laplacian {@code L = D - A} of the undirected quotient. */
    public static double[][] graphLaplacian(StateSpace ss) {
        QuotientAdj q = quotientStatesAndAdj(ss);
        int n = q.reps().size();
        if (n == 0) return new double[0][0];
        double[][] A = symmetricAdjacencyMatrix(q.reps(), q.uAdj());
        double[][] L = new double[n][n];
        for (int i = 0; i < n; i++) {
            double deg = 0.0;
            for (int j = 0; j < n; j++) deg += A[i][j];
            for (int j = 0; j < n; j++) {
                L[i][j] = (i == j) ? deg : -A[i][j];
            }
        }
        return L;
    }

    // =======================================================================
    // Jacobi eigendecomposition
    // =======================================================================

    /** Eigendecomposition of a real symmetric matrix via Jacobi rotation. */
    static Eigendecomp jacobiEigendecomposition(double[][] M, int maxIter) {
        int n = M.length;
        if (n == 0) return new Eigendecomp(new double[0], new double[0][0]);
        if (n == 1) {
            return new Eigendecomp(new double[]{M[0][0]}, new double[][]{{1.0}});
        }
        double[][] A = new double[n][n];
        for (int i = 0; i < n; i++) A[i] = M[i].clone();
        double[][] V = new double[n][n];
        for (int i = 0; i < n; i++) V[i][i] = 1.0;

        for (int iter = 0; iter < maxIter; iter++) {
            double maxVal = 0.0;
            int p = 0, qq = 1;
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double a = Math.abs(A[i][j]);
                    if (a > maxVal) { maxVal = a; p = i; qq = j; }
                }
            }
            if (maxVal < EPS) break;

            double theta;
            if (Math.abs(A[p][p] - A[qq][qq]) < EPS) {
                theta = Math.PI / 4.0;
            } else {
                theta = 0.5 * Math.atan2(2.0 * A[p][qq], A[p][p] - A[qq][qq]);
            }
            double c = Math.cos(theta);
            double s = Math.sin(theta);

            double[][] newA = new double[n][n];
            for (int i = 0; i < n; i++) newA[i] = A[i].clone();
            for (int i = 0; i < n; i++) {
                if (i != p && i != qq) {
                    newA[i][p] = c * A[i][p] + s * A[i][qq];
                    newA[p][i] = newA[i][p];
                    newA[i][qq] = -s * A[i][p] + c * A[i][qq];
                    newA[qq][i] = newA[i][qq];
                }
            }
            newA[p][p] = c * c * A[p][p] + 2 * s * c * A[p][qq] + s * s * A[qq][qq];
            newA[qq][qq] = s * s * A[p][p] - 2 * s * c * A[p][qq] + c * c * A[qq][qq];
            newA[p][qq] = 0.0;
            newA[qq][p] = 0.0;
            A = newA;

            for (int i = 0; i < n; i++) {
                double vp = V[i][p];
                double vq = V[i][qq];
                V[i][p] = c * vp + s * vq;
                V[i][qq] = -s * vp + c * vq;
            }
        }

        double[] eigenvalues = new double[n];
        for (int i = 0; i < n; i++) eigenvalues[i] = A[i][i];

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        final double[] evCopy = eigenvalues.clone();
        Arrays.sort(order, (a, b) -> Double.compare(evCopy[a], evCopy[b]));

        double[] sortedVals = new double[n];
        double[][] sortedVecs = new double[n][n];
        for (int idx = 0; idx < n; idx++) {
            int k = order[idx];
            sortedVals[idx] = evCopy[k];
            double[] vec = new double[n];
            for (int i = 0; i < n; i++) vec[i] = V[i][k];
            double norm = 0.0;
            for (double x : vec) norm += x * x;
            norm = Math.sqrt(norm);
            if (norm > EPS) {
                for (int i = 0; i < n; i++) vec[i] /= norm;
            }
            sortedVecs[idx] = vec;
        }
        // Clamp near-zero to zero
        for (int i = 0; i < n; i++) {
            if (Math.abs(sortedVals[i]) < EPS) sortedVals[i] = 0.0;
            else if (sortedVals[i] < 0.0) sortedVals[i] = 0.0;
        }
        return new Eigendecomp(sortedVals, sortedVecs);
    }

    /** Eigendecomposition of the graph Laplacian. */
    public static Eigendecomp laplacianEigendecomposition(StateSpace ss) {
        double[][] L = graphLaplacian(ss);
        if (L.length == 0) return new Eigendecomp(new double[0], new double[0][0]);
        return jacobiEigendecomposition(L, 200);
    }

    /** Eigenvalue/eigenvector pair returned from {@link #laplacianEigendecomposition}. */
    public record Eigendecomp(double[] eigenvalues, double[][] eigenvectors) {}

    // =======================================================================
    // Heat kernel matrix
    // =======================================================================

    /** Heat kernel matrix {@code H_t = exp(-tL)} via spectral decomposition. */
    public static double[][] heatKernelMatrix(StateSpace ss, double t) {
        Eigendecomp ed = laplacianEigendecomposition(ss);
        int n = ed.eigenvalues().length;
        if (n == 0) return new double[0][0];
        double[][] H = new double[n][n];
        for (int k = 0; k < n; k++) {
            double coeff = Math.exp(-t * ed.eigenvalues()[k]);
            double[] phi = ed.eigenvectors()[k];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    H[i][j] += coeff * phi[i] * phi[j];
                }
            }
        }
        return H;
    }

    // =======================================================================
    // Heat trace
    // =======================================================================

    /** Heat trace {@code Z(t) = tr(H_t) = sum_k exp(-t lam_k)}. */
    public static double heatTrace(StateSpace ss, double t) {
        Eigendecomp ed = laplacianEigendecomposition(ss);
        if (ed.eigenvalues().length == 0) return 0.0;
        double total = 0.0;
        for (double lam : ed.eigenvalues()) total += Math.exp(-t * lam);
        return total;
    }

    // =======================================================================
    // Heat kernel signature
    // =======================================================================

    private static int quotientIndex(StateSpace ss, int state) {
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        List<Integer> reps = new ArrayList<>(sccR.sccMembers().keySet());
        Collections.sort(reps);
        int rep = sccMap.getOrDefault(state, state);
        return reps.indexOf(rep);
    }

    /** Heat kernel signature {@code HKS(x, t) = H_t(x, x)} at given times. */
    public static double[] heatKernelSignature(StateSpace ss, int state, double[] times) {
        Eigendecomp ed = laplacianEigendecomposition(ss);
        int n = ed.eigenvalues().length;
        double[] result = new double[times.length];
        if (n == 0) return result;
        int idx = quotientIndex(ss, state);
        if (idx < 0) return result;
        for (int ti = 0; ti < times.length; ti++) {
            double t = times[ti];
            double val = 0.0;
            for (int k = 0; k < n; k++) {
                double phi = ed.eigenvectors()[k][idx];
                val += Math.exp(-t * ed.eigenvalues()[k]) * phi * phi;
            }
            result[ti] = val;
        }
        return result;
    }

    // =======================================================================
    // Diffusion distance
    // =======================================================================

    /** Diffusion distance matrix at time {@code t}. */
    public static double[][] diffusionDistance(StateSpace ss, double t) {
        double[][] H = heatKernelMatrix(ss, t);
        int n = H.length;
        if (n == 0) return new double[0][0];
        double[][] D = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double sq = H[i][i] + H[j][j] - 2.0 * H[i][j];
                D[i][j] = Math.sqrt(Math.max(0.0, sq));
            }
        }
        return D;
    }

    // =======================================================================
    // Total heat content
    // =======================================================================

    /** Total heat content {@code Q(t) = sum_{x,y} H_t(x,y)}. */
    public static double totalHeatContent(StateSpace ss, double t) {
        double[][] H = heatKernelMatrix(ss, t);
        if (H.length == 0) return 0.0;
        double s = 0.0;
        for (double[] row : H) for (double v : row) s += v;
        return s;
    }

    // =======================================================================
    // Heat kernel PageRank
    // =======================================================================

    /** Heat kernel PageRank: {@code p = exp(-tL) * (1/n * 1)}. */
    public static double[] heatKernelPagerank(StateSpace ss, double t) {
        double[][] H = heatKernelMatrix(ss, t);
        int n = H.length;
        if (n == 0) return new double[0];
        double uniform = 1.0 / n;
        double[] p = new double[n];
        for (int i = 0; i < n; i++) {
            double s = 0.0;
            for (int j = 0; j < n; j++) s += H[i][j];
            p[i] = s * uniform;
        }
        return p;
    }

    // =======================================================================
    // Return probability
    // =======================================================================

    /** Return probability {@code H_t(x,x)}. */
    public static double returnProbability(StateSpace ss, int state, double t) {
        Eigendecomp ed = laplacianEigendecomposition(ss);
        int n = ed.eigenvalues().length;
        if (n == 0) return 0.0;
        int idx = quotientIndex(ss, state);
        if (idx < 0) return 0.0;
        double val = 0.0;
        for (int k = 0; k < n; k++) {
            double phi = ed.eigenvectors()[k][idx];
            val += Math.exp(-t * ed.eigenvalues()[k]) * phi * phi;
        }
        return val;
    }

    // =======================================================================
    // Spectral gap
    // =======================================================================

    /** Spectral gap: smallest non-zero eigenvalue of the Laplacian. */
    public static double spectralGap(StateSpace ss) {
        Eigendecomp ed = laplacianEigendecomposition(ss);
        for (double lam : ed.eigenvalues()) {
            if (lam > EPS) return lam;
        }
        return 0.0;
    }

    // =======================================================================
    // Effective resistance
    // =======================================================================

    /** Effective resistance (Kirchhoff) matrix. */
    public static double[][] effectiveResistance(StateSpace ss) {
        Eigendecomp ed = laplacianEigendecomposition(ss);
        int n = ed.eigenvalues().length;
        if (n == 0) return new double[0][0];
        double[][] R = new double[n][n];
        for (int k = 0; k < n; k++) {
            double lam = ed.eigenvalues()[k];
            if (lam < EPS) continue;
            double inv = 1.0 / lam;
            double[] phi = ed.eigenvectors()[k];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    double d = phi[i] - phi[j];
                    R[i][j] += inv * d * d;
                }
            }
        }
        return R;
    }

    // =======================================================================
    // Kemeny constant
    // =======================================================================

    /** Kemeny constant: sum of {@code 1/lam_k} over non-zero eigenvalues. */
    public static double kemenyConstant(StateSpace ss) {
        Eigendecomp ed = laplacianEigendecomposition(ss);
        double total = 0.0;
        for (double lam : ed.eigenvalues()) {
            if (lam > EPS) total += 1.0 / lam;
        }
        return total;
    }

    // =======================================================================
    // Full analysis
    // =======================================================================

    public static HeatKernelResult analyzeHeatKernel(StateSpace ss) {
        return analyzeHeatKernel(ss, DEFAULT_SAMPLE_TIMES.clone());
    }

    public static HeatKernelResult analyzeHeatKernel(StateSpace ss, double[] sampleTimes) {
        double[][] L = graphLaplacian(ss);
        int n = L.length;
        if (n == 0) {
            return new HeatKernelResult(
                    new double[0][0],
                    new double[0],
                    new double[0][0],
                    new double[0],
                    sampleTimes,
                    new double[0][0],
                    new double[0],
                    new double[0][0]);
        }
        Eigendecomp ed = jacobiEigendecomposition(L, 200);
        double[] eigenvalues = ed.eigenvalues();
        double[][] eigenvectors = ed.eigenvectors();

        int T = sampleTimes.length;
        double[] traces = new double[T];
        double[] contents = new double[T];
        double[][] hks = new double[n][T];

        for (int ti = 0; ti < T; ti++) {
            double t = sampleTimes[ti];
            double[][] H = new double[n][n];
            for (int k = 0; k < n; k++) {
                double coeff = Math.exp(-t * eigenvalues[k]);
                double[] phi = eigenvectors[k];
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        H[i][j] += coeff * phi[i] * phi[j];
                    }
                }
            }
            double tr = 0.0;
            for (int i = 0; i < n; i++) tr += H[i][i];
            traces[ti] = tr;
            for (int i = 0; i < n; i++) hks[i][ti] = H[i][i];
            double sum = 0.0;
            for (double[] row : H) for (double v : row) sum += v;
            contents[ti] = sum;
        }

        double[][] dd = diffusionDistance(ss, 1.0);
        return new HeatKernelResult(
                L, eigenvalues, eigenvectors,
                traces, sampleTimes, hks, contents, dd);
    }
}
