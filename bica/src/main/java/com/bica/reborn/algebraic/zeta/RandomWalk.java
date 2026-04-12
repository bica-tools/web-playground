package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Random walk mixing time analysis for session type lattices
 * (port of Python {@code reticulate.random_walk}, Step 30p).
 *
 * <p>Builds the uniform transition matrix P of the state-space graph
 * (absorbing states get a self-loop), then computes stationary
 * distribution, eigenvalues (via QR iteration), spectral gap, mixing
 * time bound, hitting times, return time, commute time, cover-time
 * bound, and ergodicity. All computations use pure {@code double[][]}
 * arithmetic and target small matrices ({@literal <} 50 states).
 *
 * <p>Results are bit-for-bit consistent with the Python reference
 * implementation up to numerical tolerance.
 */
public final class RandomWalk {

    private RandomWalk() {}

    // =======================================================================
    // Result type
    // =======================================================================

    /** Complete random walk analysis. */
    public record RandomWalkResult(
            double[][] transitionMatrix,
            double[] stationaryDistribution,
            double spectralGap,
            int mixingTimeBound,
            Map<Zeta.Pair, Double> hittingTimes,
            double coverTimeBound,
            boolean isErgodic,
            double[] eigenvalues) {}

    // =======================================================================
    // Matrix utilities
    // =======================================================================

    static double[][] matMul(double[][] A, double[][] B) {
        int n = A.length;
        double[][] C = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < n; k++) {
                double aik = A[i][k];
                if (aik == 0.0) continue;
                for (int j = 0; j < n; j++) {
                    C[i][j] += aik * B[k][j];
                }
            }
        }
        return C;
    }

    static double[] vecMat(double[] v, double[][] A) {
        int n = A.length;
        double[] r = new double[n];
        for (int j = 0; j < n; j++) {
            double s = 0.0;
            for (int i = 0; i < n; i++) s += v[i] * A[i][j];
            r[j] = s;
        }
        return r;
    }

    static double[][] identity(int n) {
        double[][] M = new double[n][n];
        for (int i = 0; i < n; i++) M[i][i] = 1.0;
        return M;
    }

    // =======================================================================
    // QR eigenvalues
    // =======================================================================

    static double[][][] householderQr(double[][] A) {
        int n = A.length;
        double[][] R = new double[n][n];
        for (int i = 0; i < n; i++) R[i] = A[i].clone();
        double[][] Q = identity(n);

        for (int k = 0; k < n - 1; k++) {
            double[] x = new double[n];
            for (int i = 0; i < n; i++) x[i] = (i >= k) ? R[i][k] : 0.0;
            double sumSq = 0.0;
            for (int i = k; i < n; i++) sumSq += x[i] * x[i];
            double normX = Math.sqrt(sumSq);
            if (normX < 1e-15) continue;
            double sign = (x[k] >= 0) ? 1.0 : -1.0;
            x[k] += sign * normX;
            double sumV = 0.0;
            for (int i = k; i < n; i++) sumV += x[i] * x[i];
            double normV = Math.sqrt(sumV);
            if (normV < 1e-15) continue;
            for (int i = k; i < n; i++) x[i] /= normV;

            // Apply H to R from left
            for (int j = k; j < n; j++) {
                double dot = 0.0;
                for (int i = k; i < n; i++) dot += x[i] * R[i][j];
                for (int i = k; i < n; i++) R[i][j] -= 2.0 * x[i] * dot;
            }
            // Apply H to Q from right
            for (int i = 0; i < n; i++) {
                double dot = 0.0;
                for (int j = k; j < n; j++) dot += Q[i][j] * x[j];
                for (int j = k; j < n; j++) Q[i][j] -= 2.0 * dot * x[j];
            }
        }
        return new double[][][]{Q, R};
    }

    static double[] qrEigenvalues(double[][] A, int maxIter) {
        int n = A.length;
        if (n == 0) return new double[0];
        if (n == 1) return new double[]{A[0][0]};
        double[][] M = new double[n][n];
        for (int i = 0; i < n; i++) M[i] = A[i].clone();
        for (int iter = 0; iter < maxIter; iter++) {
            double[][][] qr = householderQr(M);
            M = matMul(qr[1], qr[0]);
            double off = 0.0;
            for (int i = 0; i < n; i++)
                for (int j = 0; j < i; j++)
                    off += M[i][j] * M[i][j];
            if (off < 1e-20) break;
        }
        Double[] eigs = new Double[n];
        for (int i = 0; i < n; i++) eigs[i] = M[i][i];
        Arrays.sort(eigs, (a, b) -> Double.compare(Math.abs(b), Math.abs(a)));
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = eigs[i];
        return out;
    }

    static double[] qrEigenvalues(double[][] A) {
        return qrEigenvalues(A, 200);
    }

    // =======================================================================
    // Transition matrix
    // =======================================================================

    /** Uniform random walk transition matrix; absorbing states get a self-loop. */
    public static double[][] transitionMatrix(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        if (n == 0) return new double[0][0];
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);
        Map<Integer, List<Integer>> adj = Zeta.adjacency(ss);

        double[][] P = new double[n][n];
        for (int s : states) {
            Set<Integer> targets = new HashSet<>(adj.getOrDefault(s, List.of()));
            int i = idx.get(s);
            int deg = targets.size();
            if (deg == 0) {
                P[i][i] = 1.0;
            } else {
                double prob = 1.0 / deg;
                for (int t : targets) {
                    P[i][idx.get(t)] += prob;
                }
            }
        }
        return P;
    }

    // =======================================================================
    // Stationary distribution
    // =======================================================================

    public static double[] stationaryDistribution(StateSpace ss) {
        double[][] P = transitionMatrix(ss);
        int n = P.length;
        if (n == 0) return new double[0];
        if (n == 1) return new double[]{1.0};
        double[] pi = new double[n];
        Arrays.fill(pi, 1.0 / n);
        for (int iter = 0; iter < 1000; iter++) {
            double[] piNew = vecMat(pi, P);
            double total = 0.0;
            for (double v : piNew) total += v;
            if (total > 0) {
                for (int i = 0; i < n; i++) piNew[i] /= total;
            }
            double diff = 0.0;
            for (int i = 0; i < n; i++) diff += Math.abs(piNew[i] - pi[i]);
            pi = piNew;
            if (diff < 1e-12) break;
        }
        return pi;
    }

    // =======================================================================
    // Eigenvalues / spectral gap
    // =======================================================================

    public static double[] eigenvaluesOfP(StateSpace ss) {
        double[][] P = transitionMatrix(ss);
        if (P.length == 0) return new double[0];
        return qrEigenvalues(P);
    }

    public static double spectralGap(StateSpace ss) {
        double[] eigs = eigenvaluesOfP(ss);
        if (eigs.length <= 1) return 1.0;
        double lambda2 = Math.abs(eigs[1]);
        double gap = 1.0 - lambda2;
        return Math.max(0.0, Math.min(1.0, gap));
    }

    // =======================================================================
    // Mixing time
    // =======================================================================

    public static int mixingTimeBound(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        if (n <= 1) return 0;
        double gap = spectralGap(ss);
        if (gap < 1e-12) return n * n;
        double bound = (1.0 / gap) * Math.log(4.0 * n);
        return Math.max(1, (int) Math.ceil(bound));
    }

    // =======================================================================
    // Hitting times
    // =======================================================================

    public static double hittingTime(StateSpace ss, int src, int tgt) {
        if (src == tgt) return 0.0;
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);
        if (!idx.containsKey(src) || !idx.containsKey(tgt)) return Double.POSITIVE_INFINITY;
        double[][] P = transitionMatrix(ss);
        int ti = idx.get(tgt);

        Set<Integer> reachable = ss.reachableFrom(src);
        if (!reachable.contains(tgt)) return Double.POSITIVE_INFINITY;

        double[] h = new double[n];
        for (int i = 0; i < n; i++) if (i != ti) h[i] = 1.0;

        for (int iter = 0; iter < 2000; iter++) {
            double maxDelta = 0.0;
            for (int i = 0; i < n; i++) {
                if (i == ti) continue;
                double newH = 1.0;
                for (int j = 0; j < n; j++) newH += P[i][j] * h[j];
                double delta = Math.abs(newH - h[i]);
                if (delta > maxDelta) maxDelta = delta;
                h[i] = newH;
            }
            if (maxDelta < 1e-8) break;
        }
        double val = h[idx.get(src)];
        if (val > 1e10) return Double.POSITIVE_INFINITY;
        return val;
    }

    public static Map<Zeta.Pair, Double> allHittingTimes(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        double[][] P = transitionMatrix(ss);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);

        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : states) reach.put(s, ss.reachableFrom(s));

        Map<Zeta.Pair, Double> result = new HashMap<>();
        for (int tgt : states) {
            int ti = idx.get(tgt);
            double[] h = new double[n];
            for (int i = 0; i < n; i++) if (i != ti) h[i] = 1.0;
            for (int iter = 0; iter < 2000; iter++) {
                double maxDelta = 0.0;
                for (int i = 0; i < n; i++) {
                    if (i == ti) continue;
                    double newH = 1.0;
                    for (int j = 0; j < n; j++) newH += P[i][j] * h[j];
                    double delta = Math.abs(newH - h[i]);
                    if (delta > maxDelta) maxDelta = delta;
                    h[i] = newH;
                }
                if (maxDelta < 1e-8) break;
            }
            for (int src : states) {
                Zeta.Pair key = new Zeta.Pair(src, tgt);
                if (src == tgt) {
                    result.put(key, 0.0);
                } else if (!reach.get(src).contains(tgt)) {
                    result.put(key, Double.POSITIVE_INFINITY);
                } else {
                    double v = h[idx.get(src)];
                    result.put(key, (v > 1e10) ? Double.POSITIVE_INFINITY : v);
                }
            }
        }
        return result;
    }

    // =======================================================================
    // Return / commute time
    // =======================================================================

    public static double returnTime(StateSpace ss, int state) {
        double[] pi = stationaryDistribution(ss);
        List<Integer> states = Zeta.stateList(ss);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < states.size(); i++) idx.put(states.get(i), i);
        if (!idx.containsKey(state)) return Double.POSITIVE_INFINITY;
        double p = pi[idx.get(state)];
        if (p < 1e-15) return Double.POSITIVE_INFINITY;
        return 1.0 / p;
    }

    public static double commuteTime(StateSpace ss, int u, int v) {
        double huv = hittingTime(ss, u, v);
        double hvu = hittingTime(ss, v, u);
        if (Double.isInfinite(huv) || Double.isInfinite(hvu)) return Double.POSITIVE_INFINITY;
        return huv + hvu;
    }

    // =======================================================================
    // Cover time
    // =======================================================================

    public static double coverTimeBound(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        if (n <= 1) return 0.0;
        Map<Zeta.Pair, Double> ht = allHittingTimes(ss);
        double hMax = 0.0;
        for (var e : ht.entrySet()) {
            int s = e.getKey().x();
            int t = e.getKey().y();
            double h = e.getValue();
            if (s != t && !Double.isInfinite(h) && h > hMax) hMax = h;
        }
        if (hMax == 0.0) return 0.0;
        double harmonic = 0.0;
        for (int k = 1; k <= n; k++) harmonic += 1.0 / k;
        return hMax * harmonic;
    }

    // =======================================================================
    // Ergodicity
    // =======================================================================

    static boolean isIrreducible(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        if (n <= 1) return true;
        Map<Integer, List<Integer>> adj = Zeta.adjacency(ss);
        Map<Integer, List<Integer>> adjLoops = new HashMap<>();
        for (int s : states) {
            List<Integer> nbrs = adj.getOrDefault(s, List.of());
            if (nbrs.isEmpty()) {
                List<Integer> l = new ArrayList<>();
                l.add(s);
                adjLoops.put(s, l);
            } else {
                adjLoops.put(s, new ArrayList<>(nbrs));
            }
        }
        int start = states.get(0);
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> q = new ArrayDeque<>();
        q.add(start);
        while (!q.isEmpty()) {
            int u = q.poll();
            if (!visited.add(u)) continue;
            for (int v : adjLoops.getOrDefault(u, List.of())) {
                if (!visited.contains(v)) q.add(v);
            }
        }
        if (visited.size() < n) return false;

        Map<Integer, List<Integer>> rev = new HashMap<>();
        for (int s : states) rev.put(s, new ArrayList<>());
        for (int s : states) for (int t : adjLoops.get(s)) rev.get(t).add(s);

        Set<Integer> v2 = new HashSet<>();
        Deque<Integer> q2 = new ArrayDeque<>();
        q2.add(start);
        while (!q2.isEmpty()) {
            int u = q2.poll();
            if (!v2.add(u)) continue;
            for (int v : rev.getOrDefault(u, List.of())) {
                if (!v2.contains(v)) q2.add(v);
            }
        }
        return v2.size() >= n;
    }

    static int gcd(int a, int b) {
        while (b != 0) { int t = b; b = a % b; a = t; }
        return a;
    }

    static int computePeriod(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        if (states.isEmpty()) return 1;
        Map<Integer, List<Integer>> adj = Zeta.adjacency(ss);
        Map<Integer, List<Integer>> adjLoops = new HashMap<>();
        for (int s : states) {
            List<Integer> nbrs = adj.getOrDefault(s, List.of());
            if (nbrs.isEmpty()) {
                List<Integer> l = new ArrayList<>();
                l.add(s);
                adjLoops.put(s, l);
            } else {
                adjLoops.put(s, new ArrayList<>(new LinkedHashSet<>(nbrs)));
            }
        }
        int start = states.get(0);
        Map<Integer, Integer> dist = new HashMap<>();
        dist.put(start, 0);
        Deque<Integer> q = new ArrayDeque<>();
        q.add(start);
        int period = 0;
        while (!q.isEmpty()) {
            int u = q.poll();
            for (int v : adjLoops.getOrDefault(u, List.of())) {
                if (!dist.containsKey(v)) {
                    dist.put(v, dist.get(u) + 1);
                    q.add(v);
                }
                int cycleLen = dist.get(u) + 1 - dist.get(v);
                if (cycleLen > 0) period = gcd(period, cycleLen);
            }
        }
        return period > 0 ? period : 1;
    }

    public static boolean isErgodic(StateSpace ss) {
        if (!isIrreducible(ss)) return false;
        return computePeriod(ss) == 1;
    }

    // =======================================================================
    // Full analysis
    // =======================================================================

    public static RandomWalkResult analyzeRandomWalk(StateSpace ss) {
        double[][] P = transitionMatrix(ss);
        double[] pi = stationaryDistribution(ss);
        double[] eigs = eigenvaluesOfP(ss);
        double gap = spectralGap(ss);
        int mix = mixingTimeBound(ss);
        Map<Zeta.Pair, Double> ht = allHittingTimes(ss);
        double cover = coverTimeBound(ss);
        boolean erg = isErgodic(ss);
        return new RandomWalkResult(P, pi, gap, mix, ht, cover, erg, eigs);
    }
}
