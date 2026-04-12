package com.bica.reborn.wavelets;

import com.bica.reborn.statespace.StateSpace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Wavelet analysis of session type lattices (Step 31j) -- Java port of
 * {@code reticulate/reticulate/wavelets.py}.
 *
 * <p>Provides three wavelet decompositions of signals f: states -> R on a
 * session-type state space:
 * <ul>
 *   <li><b>Spectral</b>: graph-Laplacian eigenbasis (Hammond et al. 2011).</li>
 *   <li><b>Diffusion</b>: powers of T = D^-1 A (Coifman &amp; Maggioni 2006).</li>
 *   <li><b>Lattice</b>: novel ball-average construction native to the lattice.</li>
 * </ul>
 *
 * <p>All numerics use {@code double}; eigendecomposition is a self-contained
 * QR iteration so the class has no external linear-algebra dependency.
 */
public final class WaveletsChecker {

    private WaveletsChecker() {}

    // -----------------------------------------------------------------------
    // Data types
    // -----------------------------------------------------------------------

    /** A single wavelet coefficient at (state, scale). */
    public record WaveletCoefficient(int state, int scale, double value) {}

    /** Multi-scale decomposition of a signal on the lattice. */
    public record ScaleDecomposition(
            Map<Integer, Double> original,
            Map<Integer, Double> coarse,
            List<Map<Integer, Double>> details,
            List<WaveletCoefficient> coefficients,
            int nScales,
            String approach,
            List<Double> energyPerScale) {
        public ScaleDecomposition {
            original = Map.copyOf(original);
            coarse = Map.copyOf(coarse);
            details = List.copyOf(details);
            coefficients = List.copyOf(coefficients);
            energyPerScale = List.copyOf(energyPerScale);
        }
    }

    /** Full wavelet analysis comparing all three approaches. */
    public record WaveletAnalysis(
            ScaleDecomposition spectral,
            ScaleDecomposition diffusion,
            ScaleDecomposition lattice,
            Map<String, Double> reconstructionErrors,
            Map<String, Integer> dominantScale,
            int numStates) {
        public WaveletAnalysis {
            reconstructionErrors = Map.copyOf(reconstructionErrors);
            dominantScale = Map.copyOf(dominantScale);
        }
    }

    // -----------------------------------------------------------------------
    // Internal: graph infrastructure
    // -----------------------------------------------------------------------

    private static Map<Integer, Set<Integer>> undirectedAdj(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new HashSet<>());
        for (var t : ss.transitions()) {
            adj.get(t.source()).add(t.target());
            adj.get(t.target()).add(t.source());
        }
        return adj;
    }

    private static Map<Integer, Integer> bfsDistance(StateSpace ss, int center) {
        Map<Integer, Set<Integer>> adj = undirectedAdj(ss);
        Map<Integer, Integer> dist = new HashMap<>();
        dist.put(center, 0);
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(center);
        while (!queue.isEmpty()) {
            int s = queue.removeFirst();
            for (int t : adj.getOrDefault(s, Set.of())) {
                if (!dist.containsKey(t)) {
                    dist.put(t, dist.get(s) + 1);
                    queue.addLast(t);
                }
            }
        }
        int n = ss.states().size();
        for (int s : ss.states()) {
            if (!dist.containsKey(s)) dist.put(s, n);
        }
        return dist;
    }

    private static List<Integer> sortedStates(StateSpace ss) {
        List<Integer> states = new ArrayList<>(ss.states());
        Collections.sort(states);
        return states;
    }

    private static double[][] laplacian(List<Integer> states, StateSpace ss) {
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);
        Map<Integer, Set<Integer>> adj = undirectedAdj(ss);
        double[][] L = new double[n][n];
        for (int i = 0; i < n; i++) {
            int s = states.get(i);
            int deg = adj.get(s).size();
            L[i][i] = deg;
            for (int t : adj.get(s)) L[i][idx.get(t)] -= 1.0;
        }
        return L;
    }

    /** QR-iteration eigendecomposition of a real symmetric matrix.
     *  Returns eigenvalues sorted ascending and corresponding eigenvectors as rows. */
    static EigenResult eigenDecomposition(double[][] A, int maxIter) {
        int n = A.length;
        if (n == 0) return new EigenResult(new double[0], new double[0][0]);
        if (n == 1) return new EigenResult(new double[]{A[0][0]}, new double[][]{{1.0}});

        double[][] M = new double[n][n];
        double[][] V = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, M[i], 0, n);
            V[i][i] = 1.0;
        }

        for (int iter = 0; iter < maxIter; iter++) {
            double[][] Q = new double[n][n];
            double[][] R = new double[n][n];
            for (int j = 0; j < n; j++) {
                double[] v = new double[n];
                for (int i = 0; i < n; i++) v[i] = M[i][j];
                for (int i = 0; i < j; i++) {
                    double dot = 0.0;
                    for (int k = 0; k < n; k++) dot += Q[k][i] * v[k];
                    R[i][j] = dot;
                    for (int k = 0; k < n; k++) v[k] -= dot * Q[k][i];
                }
                double norm = 0.0;
                for (double x : v) norm += x * x;
                norm = Math.sqrt(norm);
                R[j][j] = norm;
                if (norm > 1e-14) {
                    for (int k = 0; k < n; k++) Q[k][j] = v[k] / norm;
                } else {
                    for (int k = 0; k < n; k++) Q[k][j] = (k == j) ? 1.0 : 0.0;
                }
            }
            double[][] Mnew = new double[n][n];
            double[][] Vnew = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    double sM = 0.0, sV = 0.0;
                    for (int k = 0; k < n; k++) {
                        sM += R[i][k] * Q[k][j];
                        sV += V[i][k] * Q[k][j];
                    }
                    Mnew[i][j] = sM;
                    Vnew[i][j] = sV;
                }
            }
            M = Mnew;
            V = Vnew;
            double off = 0.0;
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    if (i != j) off += Math.abs(M[i][j]);
            if (off < 1e-10 * n) break;
        }

        double[] eigenvalues = new double[n];
        for (int i = 0; i < n; i++) eigenvalues[i] = M[i][i];
        // eigenvectors[k][i] = V[i][k]
        double[][] eigenvectors = new double[n][n];
        for (int k = 0; k < n; k++)
            for (int i = 0; i < n; i++)
                eigenvectors[k][i] = V[i][k];

        // Sort by eigenvalue
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> Double.compare(eigenvalues[a], eigenvalues[b]));
        double[] evSorted = new double[n];
        double[][] vecSorted = new double[n][n];
        for (int i = 0; i < n; i++) {
            evSorted[i] = eigenvalues[order[i]];
            vecSorted[i] = eigenvectors[order[i]];
        }
        return new EigenResult(evSorted, vecSorted);
    }

    static record EigenResult(double[] eigenvalues, double[][] eigenvectors) {}

    private static double waveletKernel(double t, double lam) {
        double x = t * lam;
        if (x > 50) return 0.0;
        return x * Math.exp(-x);
    }

    private static double scalingKernel(double t, double lam) {
        double x = t * lam;
        if (x > 50) return 0.0;
        return Math.exp(-x);
    }

    // -----------------------------------------------------------------------
    // Approach A: Spectral graph wavelets
    // -----------------------------------------------------------------------

    public static ScaleDecomposition spectralWavelets(StateSpace ss, Map<Integer, Double> f) {
        return spectralWavelets(ss, f, null);
    }

    public static ScaleDecomposition spectralWavelets(
            StateSpace ss, Map<Integer, Double> f, double[] scales) {
        List<Integer> states = sortedStates(ss);
        int n = states.size();
        if (n <= 1) return trivialDecomposition(f, "spectral");

        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);

        double[][] L = laplacian(states, ss);
        EigenResult eig = eigenDecomposition(L, 200);
        double[] eigenvalues = eig.eigenvalues();
        double[][] eigenvectors = eig.eigenvectors();

        if (scales == null) {
            double lamMax = 1e-10;
            for (double lam : eigenvalues) if (lam > lamMax) lamMax = lam;
            int count = Math.max(1, (int) (Math.log(n) / Math.log(2)));
            scales = new double[count];
            for (int k = 0; k < count; k++) scales[k] = Math.pow(2.0, k) / lamMax;
        }

        // f̂_k = Σ_i φ_k(i) · f(states[i])
        double[] fHat = new double[n];
        for (int k = 0; k < n; k++) {
            double s = 0.0;
            for (int i = 0; i < n; i++) {
                s += eigenvectors[k][i] * f.getOrDefault(states.get(i), 0.0);
            }
            fHat[k] = s;
        }

        List<WaveletCoefficient> coefficients = new ArrayList<>();
        List<Map<Integer, Double>> details = new ArrayList<>();
        List<Double> energyPerScale = new ArrayList<>();

        for (int scaleIdx = 0; scaleIdx < scales.length; scaleIdx++) {
            double t = scales[scaleIdx];
            Map<Integer, Double> detail = new LinkedHashMap<>();
            double scaleEnergy = 0.0;
            for (int sId : states) {
                int i = idx.get(sId);
                double val = 0.0;
                for (int k = 0; k < n; k++) {
                    val += waveletKernel(t, eigenvalues[k]) * eigenvectors[k][i] * fHat[k];
                }
                detail.put(sId, val);
                coefficients.add(new WaveletCoefficient(sId, scaleIdx, val));
                scaleEnergy += val * val;
            }
            details.add(detail);
            energyPerScale.add(scaleEnergy);
        }

        double tMax = scales.length > 0 ? scales[scales.length - 1] : 1.0;
        Map<Integer, Double> coarse = new LinkedHashMap<>();
        for (int sId : states) {
            int i = idx.get(sId);
            double val = 0.0;
            for (int k = 0; k < n; k++) {
                val += scalingKernel(tMax, eigenvalues[k]) * eigenvectors[k][i] * fHat[k];
            }
            coarse.put(sId, val);
        }

        double total = 0.0;
        for (double e : energyPerScale) total += e;
        if (total == 0.0) total = 1.0;
        List<Double> normEnergy = new ArrayList<>(energyPerScale.size());
        for (double e : energyPerScale) normEnergy.add(e / total);

        return new ScaleDecomposition(copy(f), coarse, details, coefficients,
                scales.length, "spectral", normEnergy);
    }

    // -----------------------------------------------------------------------
    // Approach B: Diffusion wavelets
    // -----------------------------------------------------------------------

    public static ScaleDecomposition diffusionWavelets(StateSpace ss, Map<Integer, Double> f) {
        return diffusionWavelets(ss, f, null);
    }

    public static ScaleDecomposition diffusionWavelets(
            StateSpace ss, Map<Integer, Double> f, int[] scales) {
        Map<Integer, Set<Integer>> adj = undirectedAdj(ss);
        List<Integer> states = sortedStates(ss);
        int n = states.size();
        if (n <= 1) return trivialDecomposition(f, "diffusion");

        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);

        if (scales == null) {
            int count = Math.max(1, (int) (Math.log(n) / Math.log(2)) + 1);
            scales = new int[count];
            for (int k = 0; k < count; k++) scales[k] = k;
        }

        double[][] T = new double[n][n];
        for (int i = 0; i < n; i++) {
            int s = states.get(i);
            int deg = adj.get(s).size();
            if (deg > 0) {
                for (int t : adj.get(s)) T[i][idx.get(t)] = 1.0 / deg;
            }
        }

        int maxScale = 0;
        for (int k : scales) if (k > maxScale) maxScale = k;

        double[] current = new double[n];
        for (int i = 0; i < n; i++) current[i] = f.getOrDefault(states.get(i), 0.0);
        List<double[]> smoothed = new ArrayList<>();
        smoothed.add(current.clone());
        for (int k = 0; k < maxScale + 1; k++) {
            double[] next = new double[n];
            for (int i = 0; i < n; i++) {
                double s = 0.0;
                for (int j = 0; j < n; j++) s += T[i][j] * current[j];
                next[i] = s;
            }
            current = next;
            smoothed.add(current.clone());
        }

        List<Map<Integer, Double>> details = new ArrayList<>();
        List<WaveletCoefficient> coefficients = new ArrayList<>();
        List<Double> energyPerScale = new ArrayList<>();

        for (int scaleIdx = 0; scaleIdx < scales.length; scaleIdx++) {
            int k = scales[scaleIdx];
            Map<Integer, Double> detail = new LinkedHashMap<>();
            double scaleEnergy = 0.0;
            for (int i = 0; i < n; i++) {
                int sId = states.get(i);
                double val;
                if (k + 1 < smoothed.size()) val = smoothed.get(k)[i] - smoothed.get(k + 1)[i];
                else val = smoothed.get(k)[i];
                detail.put(sId, val);
                coefficients.add(new WaveletCoefficient(sId, scaleIdx, val));
                scaleEnergy += val * val;
            }
            details.add(detail);
            energyPerScale.add(scaleEnergy);
        }

        int maxK = (scales.length > 0 ? maxScale : 0) + 1;
        int coarseIdx = Math.min(maxK, smoothed.size() - 1);
        Map<Integer, Double> coarse = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) coarse.put(states.get(i), smoothed.get(coarseIdx)[i]);

        double total = 0.0;
        for (double e : energyPerScale) total += e;
        if (total == 0.0) total = 1.0;
        List<Double> normEnergy = new ArrayList<>();
        for (double e : energyPerScale) normEnergy.add(e / total);

        return new ScaleDecomposition(copy(f), coarse, details, coefficients,
                scales.length, "diffusion", normEnergy);
    }

    // -----------------------------------------------------------------------
    // Approach C: Lattice-native wavelets
    // -----------------------------------------------------------------------

    public static ScaleDecomposition latticeWavelets(StateSpace ss, Map<Integer, Double> f) {
        return latticeWavelets(ss, f, null);
    }

    public static ScaleDecomposition latticeWavelets(
            StateSpace ss, Map<Integer, Double> f, int[] scales) {
        List<Integer> states = sortedStates(ss);
        int n = states.size();
        if (n <= 1) return trivialDecomposition(f, "lattice");

        Map<Integer, Map<Integer, Integer>> distMatrix = new HashMap<>();
        for (int s : states) distMatrix.put(s, bfsDistance(ss, s));

        int maxDist = 0;
        for (int s : states) {
            for (int t : states) {
                int d = distMatrix.get(s).get(t);
                if (d < n && d > maxDist) maxDist = d;
            }
        }

        if (scales == null) {
            scales = new int[maxDist + 1];
            for (int k = 0; k <= maxDist; k++) scales[k] = k;
        }

        int maxScale = 0;
        for (int k : scales) if (k > maxScale) maxScale = k;

        List<Map<Integer, Double>> averages = new ArrayList<>();
        for (int t = 0; t < maxScale + 2; t++) {
            Map<Integer, Double> avg = new LinkedHashMap<>();
            for (int s : states) {
                List<Integer> ball = new ArrayList<>();
                for (int x : states) {
                    if (distMatrix.get(s).getOrDefault(x, n) <= t) ball.add(x);
                }
                if (!ball.isEmpty()) {
                    double sum = 0.0;
                    for (int x : ball) sum += f.getOrDefault(x, 0.0);
                    avg.put(s, sum / ball.size());
                } else {
                    avg.put(s, f.getOrDefault(s, 0.0));
                }
            }
            averages.add(avg);
        }

        List<Map<Integer, Double>> details = new ArrayList<>();
        List<WaveletCoefficient> coefficients = new ArrayList<>();
        List<Double> energyPerScale = new ArrayList<>();

        for (int scaleIdx = 0; scaleIdx < scales.length; scaleIdx++) {
            int t = scales[scaleIdx];
            Map<Integer, Double> detail = new LinkedHashMap<>();
            double scaleEnergy = 0.0;
            for (int s : states) {
                double val;
                if (t < averages.size() && t + 1 < averages.size())
                    val = averages.get(t).get(s) - averages.get(t + 1).get(s);
                else if (t < averages.size()) val = averages.get(t).get(s);
                else val = 0.0;
                detail.put(s, val);
                coefficients.add(new WaveletCoefficient(s, scaleIdx, val));
                scaleEnergy += val * val;
            }
            details.add(detail);
            energyPerScale.add(scaleEnergy);
        }

        int maxT = scales.length > 0 ? Math.min(maxScale + 1, averages.size() - 1) : 0;
        Map<Integer, Double> coarse = new LinkedHashMap<>(averages.get(maxT));

        double total = 0.0;
        for (double e : energyPerScale) total += e;
        if (total == 0.0) total = 1.0;
        List<Double> normEnergy = new ArrayList<>();
        for (double e : energyPerScale) normEnergy.add(e / total);

        return new ScaleDecomposition(copy(f), coarse, details, coefficients,
                scales.length, "lattice", normEnergy);
    }

    // -----------------------------------------------------------------------
    // Reconstruction / errors / thresholding
    // -----------------------------------------------------------------------

    public static Map<Integer, Double> reconstruct(ScaleDecomposition decomp) {
        Map<Integer, Double> result = new LinkedHashMap<>(decomp.coarse());
        for (Map<Integer, Double> detail : decomp.details()) {
            for (var e : detail.entrySet()) {
                result.merge(e.getKey(), e.getValue(), Double::sum);
            }
        }
        return result;
    }

    public static double reconstructionError(ScaleDecomposition decomp) {
        Map<Integer, Double> recon = reconstruct(decomp);
        double sum = 0.0;
        for (var e : decomp.original().entrySet()) {
            double diff = e.getValue() - recon.getOrDefault(e.getKey(), 0.0);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    public static ScaleDecomposition thresholdCoefficients(
            ScaleDecomposition decomp, double threshold) {
        List<Map<Integer, Double>> newDetails = new ArrayList<>();
        List<WaveletCoefficient> newCoeffs = new ArrayList<>();
        List<Double> newEnergy = new ArrayList<>();
        for (int scaleIdx = 0; scaleIdx < decomp.details().size(); scaleIdx++) {
            Map<Integer, Double> detail = decomp.details().get(scaleIdx);
            Map<Integer, Double> newDetail = new LinkedHashMap<>();
            double scaleE = 0.0;
            for (var e : detail.entrySet()) {
                double v = e.getValue();
                double nv = (Math.abs(v) >= threshold) ? v : 0.0;
                newDetail.put(e.getKey(), nv);
                if (Math.abs(v) >= threshold) scaleE += v * v;
                newCoeffs.add(new WaveletCoefficient(e.getKey(), scaleIdx, nv));
            }
            newDetails.add(newDetail);
            newEnergy.add(scaleE);
        }
        double total = 0.0;
        for (double e : newEnergy) total += e;
        if (total == 0.0) total = 1.0;
        List<Double> normEnergy = new ArrayList<>();
        for (double e : newEnergy) normEnergy.add(e / total);
        return new ScaleDecomposition(decomp.original(), decomp.coarse(),
                newDetails, newCoeffs, decomp.nScales(), decomp.approach(), normEnergy);
    }

    // -----------------------------------------------------------------------
    // Embedding / anomaly / change impact
    // -----------------------------------------------------------------------

    public static Map<Integer, double[]> waveletEmbedding(
            StateSpace ss, Map<Integer, Double> f, int nScales) {
        ScaleDecomposition decomp = latticeWavelets(ss, f);
        Map<Integer, double[]> embedding = new LinkedHashMap<>();
        int k = Math.min(nScales, decomp.details().size());
        for (int s : ss.states()) {
            double[] vec = new double[k];
            for (int i = 0; i < k; i++) vec[i] = decomp.details().get(i).getOrDefault(s, 0.0);
            embedding.put(s, vec);
        }
        return embedding;
    }

    public static Map<Integer, double[]> waveletEmbedding(StateSpace ss, Map<Integer, Double> f) {
        return waveletEmbedding(ss, f, 3);
    }

    public static List<WaveletCoefficient> anomalyLocalize(
            StateSpace ss, Map<Integer, Double> fExpected, Map<Integer, Double> fObserved) {
        Map<Integer, Double> residual = new HashMap<>();
        for (int s : ss.states()) {
            residual.put(s, fObserved.getOrDefault(s, 0.0) - fExpected.getOrDefault(s, 0.0));
        }
        ScaleDecomposition decomp = latticeWavelets(ss, residual);
        List<WaveletCoefficient> sorted = new ArrayList<>(decomp.coefficients());
        sorted.sort((a, b) -> Double.compare(Math.abs(b.value()), Math.abs(a.value())));
        return sorted;
    }

    public static Map<Integer, Double> changeImpact(
            StateSpace ss, Map<Integer, Double> fBefore, Map<Integer, Double> fAfter) {
        Map<Integer, Double> delta = new HashMap<>();
        for (int s : ss.states()) {
            delta.put(s, fAfter.getOrDefault(s, 0.0) - fBefore.getOrDefault(s, 0.0));
        }
        ScaleDecomposition decomp = latticeWavelets(ss, delta);
        Map<Integer, Double> out = new LinkedHashMap<>();
        for (int i = 0; i < decomp.energyPerScale().size(); i++) {
            out.put(i, decomp.energyPerScale().get(i));
        }
        return out;
    }

    // -----------------------------------------------------------------------
    // Default signal & decompose & full analysis
    // -----------------------------------------------------------------------

    public static Map<Integer, Double> defaultSignal(StateSpace ss) {
        Map<Integer, Set<Integer>> rev = new HashMap<>();
        for (int s : ss.states()) rev.put(s, new HashSet<>());
        for (var t : ss.transitions()) rev.get(t.target()).add(t.source());
        Map<Integer, Integer> dist = new HashMap<>();
        if (ss.states().contains(ss.bottom())) {
            dist.put(ss.bottom(), 0);
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(ss.bottom());
            while (!queue.isEmpty()) {
                int s = queue.removeFirst();
                for (int pred : rev.getOrDefault(s, Set.of())) {
                    if (!dist.containsKey(pred)) {
                        dist.put(pred, dist.get(s) + 1);
                        queue.addLast(pred);
                    }
                }
            }
        }
        Map<Integer, Double> out = new LinkedHashMap<>();
        for (int s : ss.states()) out.put(s, (double) dist.getOrDefault(s, 0));
        return out;
    }

    private static ScaleDecomposition trivialDecomposition(Map<Integer, Double> f, String approach) {
        return new ScaleDecomposition(copy(f), copy(f), List.of(), List.of(),
                0, approach, List.of());
    }

    private static Map<Integer, Double> copy(Map<Integer, Double> f) {
        return new LinkedHashMap<>(f);
    }

    public static ScaleDecomposition decompose(StateSpace ss) {
        return decompose(ss, null, "lattice");
    }

    public static ScaleDecomposition decompose(StateSpace ss, Map<Integer, Double> f, String approach) {
        if (f == null) f = defaultSignal(ss);
        switch (approach) {
            case "spectral": return spectralWavelets(ss, f);
            case "diffusion": return diffusionWavelets(ss, f);
            case "lattice": return latticeWavelets(ss, f);
            default:
                throw new IllegalArgumentException(
                        "Unknown approach: " + approach + ". Use 'spectral', 'diffusion', or 'lattice'.");
        }
    }

    public static WaveletAnalysis analyzeWavelets(StateSpace ss) {
        Map<Integer, Double> f = defaultSignal(ss);
        ScaleDecomposition spec = spectralWavelets(ss, f);
        ScaleDecomposition diff = diffusionWavelets(ss, f);
        ScaleDecomposition latt = latticeWavelets(ss, f);

        Map<String, Double> errors = new LinkedHashMap<>();
        errors.put("spectral", reconstructionError(spec));
        errors.put("diffusion", reconstructionError(diff));
        errors.put("lattice", reconstructionError(latt));

        Map<String, Integer> dom = new LinkedHashMap<>();
        dom.put("spectral", dominant(spec));
        dom.put("diffusion", dominant(diff));
        dom.put("lattice", dominant(latt));

        return new WaveletAnalysis(spec, diff, latt, errors, dom, ss.states().size());
    }

    private static int dominant(ScaleDecomposition decomp) {
        if (decomp.energyPerScale().isEmpty()) return 0;
        int best = 0;
        double bestVal = decomp.energyPerScale().get(0);
        for (int i = 1; i < decomp.energyPerScale().size(); i++) {
            if (decomp.energyPerScale().get(i) > bestVal) {
                bestVal = decomp.energyPerScale().get(i);
                best = i;
            }
        }
        return best;
    }
}
