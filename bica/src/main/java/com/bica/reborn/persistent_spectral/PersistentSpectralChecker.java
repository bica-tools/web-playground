package com.bica.reborn.persistent_spectral;

import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Persistent spectral protocol fingerprints (Step 31k) — Java port of
 * {@code reticulate/reticulate/persistent_spectral.py}.
 *
 * <p>Combines spectral graph theory with persistent filtrations. For
 * each filtration level {@code t}, the subgraph {@code G_t} is formed
 * from all vertices and Hasse edges with filtration value at most
 * {@code t}; we then compute the spectrum of its (graph) Laplacian.
 * Tracking how each eigenvalue evolves as {@code t} grows produces a
 * persistent spectral fingerprint of the protocol.
 */
public final class PersistentSpectralChecker {

    private static final double EPS = 1e-12;
    private static final double ZERO_TOL = 1e-9;

    private PersistentSpectralChecker() {}

    // -----------------------------------------------------------------------
    // Data types
    // -----------------------------------------------------------------------

    /** Spectrum of G_t at one filtration level. */
    public static final class SpectralSnapshot {
        public final double level;
        public final int numVertices;
        public final int numEdges;
        public final double[] eigenvalues;

        public SpectralSnapshot(double level, int numVertices, int numEdges, double[] eigenvalues) {
            this.level = level;
            this.numVertices = numVertices;
            this.numEdges = numEdges;
            this.eigenvalues = eigenvalues;
        }

        /** Second-smallest eigenvalue (algebraic connectivity of G_t). */
        public double fiedler() {
            if (eigenvalues.length < 2) return 0.0;
            return eigenvalues[1];
        }

        /** Largest Laplacian eigenvalue. */
        public double spectralRadius() {
            if (eigenvalues.length == 0) return 0.0;
            double m = eigenvalues[0];
            for (double e : eigenvalues) if (e > m) m = e;
            return m;
        }

        /** Number of (near-)zero eigenvalues = number of components. */
        public int zeroMultiplicity() {
            int c = 0;
            for (double e : eigenvalues) if (Math.abs(e) < ZERO_TOL) c++;
            return c;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof SpectralSnapshot s)) return false;
            return Double.compare(level, s.level) == 0
                    && numVertices == s.numVertices
                    && numEdges == s.numEdges
                    && Arrays.equals(eigenvalues, s.eigenvalues);
        }

        @Override
        public int hashCode() {
            return Objects.hash(level, numVertices, numEdges, Arrays.hashCode(eigenvalues));
        }
    }

    /** Birth/death of a tracked eigenvalue across the filtration. */
    public static final class SpectralPersistencePair {
        public final int index;
        public final double birth;
        public final double death;
        public final double birthValue;
        public final double deathValue;

        public SpectralPersistencePair(int index, double birth, double death,
                                       double birthValue, double deathValue) {
            this.index = index;
            this.birth = birth;
            this.death = death;
            this.birthValue = birthValue;
            this.deathValue = deathValue;
        }

        public double persistence() {
            if (Double.isInfinite(death)) return Double.POSITIVE_INFINITY;
            return death - birth;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof SpectralPersistencePair p)) return false;
            return index == p.index
                    && Double.compare(birth, p.birth) == 0
                    && Double.compare(death, p.death) == 0
                    && Double.compare(birthValue, p.birthValue) == 0
                    && (Double.compare(deathValue, p.deathValue) == 0
                        || (Double.isNaN(deathValue) && Double.isNaN(p.deathValue)));
        }

        @Override
        public int hashCode() {
            return Objects.hash(index, birth, death, birthValue, deathValue);
        }
    }

    /** Compact fingerprint used for protocol comparison. */
    public static final class PersistentSpectralFingerprint {
        public final int numLevels;
        public final double[] fiedlerTrace;
        public final double[] radiusTrace;
        public final int[] zeroMultTrace;
        public final double[] energyTrace;
        public final double totalSpectralPersistence;
        public final double maxFiedler;
        public final double[] finalSpectrum;

        public PersistentSpectralFingerprint(int numLevels, double[] fiedlerTrace,
                                             double[] radiusTrace, int[] zeroMultTrace,
                                             double[] energyTrace,
                                             double totalSpectralPersistence,
                                             double maxFiedler, double[] finalSpectrum) {
            this.numLevels = numLevels;
            this.fiedlerTrace = fiedlerTrace;
            this.radiusTrace = radiusTrace;
            this.zeroMultTrace = zeroMultTrace;
            this.energyTrace = energyTrace;
            this.totalSpectralPersistence = totalSpectralPersistence;
            this.maxFiedler = maxFiedler;
            this.finalSpectrum = finalSpectrum;
        }

        /** Project the fingerprint to a flat numeric vector. */
        public double[] asVector() {
            int n = fiedlerTrace.length + radiusTrace.length
                    + zeroMultTrace.length + energyTrace.length + 2;
            double[] v = new double[n];
            int p = 0;
            for (double d : fiedlerTrace) v[p++] = d;
            for (double d : radiusTrace) v[p++] = d;
            for (int m : zeroMultTrace) v[p++] = m;
            for (double d : energyTrace) v[p++] = d;
            v[p++] = totalSpectralPersistence;
            v[p] = maxFiedler;
            return v;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PersistentSpectralFingerprint f)) return false;
            return numLevels == f.numLevels
                    && Double.compare(totalSpectralPersistence, f.totalSpectralPersistence) == 0
                    && Double.compare(maxFiedler, f.maxFiedler) == 0
                    && Arrays.equals(fiedlerTrace, f.fiedlerTrace)
                    && Arrays.equals(radiusTrace, f.radiusTrace)
                    && Arrays.equals(zeroMultTrace, f.zeroMultTrace)
                    && Arrays.equals(energyTrace, f.energyTrace)
                    && Arrays.equals(finalSpectrum, f.finalSpectrum);
        }

        @Override
        public int hashCode() {
            return Objects.hash(numLevels, Arrays.hashCode(fiedlerTrace),
                    Arrays.hashCode(radiusTrace), Arrays.hashCode(zeroMultTrace),
                    Arrays.hashCode(energyTrace), totalSpectralPersistence, maxFiedler,
                    Arrays.hashCode(finalSpectrum));
        }
    }

    /** Full persistent spectral analysis of a state space. */
    public static final class PersistentSpectralAnalysis {
        public final List<SpectralSnapshot> snapshots;
        public final List<SpectralPersistencePair> pairs;
        public final PersistentSpectralFingerprint fingerprint;
        public final int numStates;

        public PersistentSpectralAnalysis(List<SpectralSnapshot> snapshots,
                                          List<SpectralPersistencePair> pairs,
                                          PersistentSpectralFingerprint fingerprint,
                                          int numStates) {
            this.snapshots = List.copyOf(snapshots);
            this.pairs = List.copyOf(pairs);
            this.fingerprint = fingerprint;
            this.numStates = numStates;
        }
    }

    /** Result of the ψ backward morphism: engineering recommendations. */
    public static final class SpectralActions {
        public final boolean bottleneck;
        public final boolean complexityAlert;
        public final List<double[]> fragileLevels; // [index, fiedler]
        public final int monitoringLevel;
        public final double stabilityScore;

        public SpectralActions(boolean bottleneck, boolean complexityAlert,
                               List<double[]> fragileLevels, int monitoringLevel,
                               double stabilityScore) {
            this.bottleneck = bottleneck;
            this.complexityAlert = complexityAlert;
            this.fragileLevels = List.copyOf(fragileLevels);
            this.monitoringLevel = monitoringLevel;
            this.stabilityScore = stabilityScore;
        }

        /** Map-style accessor mirroring the Python dict. */
        public Map<String, Object> asMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("bottleneck", bottleneck);
            m.put("complexity_alert", complexityAlert);
            m.put("fragile_levels", fragileLevels);
            m.put("monitoring_level", monitoringLevel);
            m.put("stability_score", stabilityScore);
            return m;
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers (rank + covering relations) — local copies
    // -----------------------------------------------------------------------

    private static Map<Integer, Integer> rank(StateSpace ss) {
        Map<Integer, Set<Integer>> rev = new HashMap<>();
        for (int s : ss.states()) rev.put(s, new HashSet<>());
        for (Transition t : ss.transitions()) {
            rev.computeIfAbsent(t.target(), k -> new HashSet<>()).add(t.source());
        }
        Map<Integer, Integer> dist = new HashMap<>();
        if (ss.states().contains(ss.bottom())) {
            dist.put(ss.bottom(), 0);
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(ss.bottom());
            while (!queue.isEmpty()) {
                int s = queue.poll();
                for (int pred : rev.getOrDefault(s, Set.of())) {
                    if (!dist.containsKey(pred)) {
                        dist.put(pred, dist.get(s) + 1);
                        queue.add(pred);
                    }
                }
            }
        }
        for (int s : ss.states()) dist.putIfAbsent(s, 0);
        return dist;
    }

    private static List<int[]> coveringRelations(StateSpace ss) {
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int s : ss.states()) adj.put(s, new HashSet<>());
        for (Transition t : ss.transitions()) {
            adj.computeIfAbsent(t.source(), k -> new HashSet<>()).add(t.target());
        }
        Map<Integer, Set<Integer>> reach = new HashMap<>();
        for (int s : ss.states()) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(s);
            while (!stack.isEmpty()) {
                int v = stack.pop();
                if (visited.add(v)) {
                    for (int t : adj.getOrDefault(v, Set.of())) stack.push(t);
                }
            }
            reach.put(s, visited);
        }
        List<int[]> covers = new ArrayList<>();
        for (int s : ss.states()) {
            for (int t : adj.getOrDefault(s, Set.of())) {
                if (t == s) continue;
                boolean isCover = true;
                for (int u : adj.getOrDefault(s, Set.of())) {
                    if (u != t && u != s && reach.getOrDefault(u, Set.of()).contains(t)) {
                        isCover = false;
                        break;
                    }
                }
                if (isCover) covers.add(new int[]{s, t});
            }
        }
        return covers;
    }

    // -----------------------------------------------------------------------
    // Filtration plumbing
    // -----------------------------------------------------------------------

    public enum Filtration { RANK, REVERSE_RANK }

    public static Filtration filtrationFromString(String s) {
        if ("rank".equals(s)) return Filtration.RANK;
        if ("reverse_rank".equals(s)) return Filtration.REVERSE_RANK;
        throw new IllegalArgumentException("Unknown filtration: '" + s + "'");
    }

    private static Map<Integer, Double> vertexFiltration(StateSpace ss, Filtration f) {
        Map<Integer, Integer> r = rank(ss);
        Map<Integer, Double> out = new HashMap<>();
        if (f == Filtration.RANK) {
            for (int s : ss.states()) out.put(s, (double) r.getOrDefault(s, 0));
        } else {
            int max = 0;
            for (int v : r.values()) if (v > max) max = v;
            for (int s : ss.states()) out.put(s, (double) (max - r.getOrDefault(s, 0)));
        }
        return out;
    }

    private static List<Double> levels(Map<Integer, Double> vfilt, List<int[]> covers) {
        TreeSet<Double> levels = new TreeSet<>();
        for (double lv : vfilt.values()) levels.add(lv);
        for (int[] e : covers) {
            double a = vfilt.getOrDefault(e[0], 0.0);
            double b = vfilt.getOrDefault(e[1], 0.0);
            levels.add(Math.max(a, b));
        }
        return new ArrayList<>(levels);
    }

    private static double[][] laplacianOfSubgraph(List<Integer> vertices, List<int[]> edges) {
        int n = vertices.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(vertices.get(i), i);
        double[][] L = new double[n][n];
        for (int[] e : edges) {
            Integer i = idx.get(e[0]);
            Integer j = idx.get(e[1]);
            if (i == null || j == null || i.equals(j)) continue;
            L[i][j] -= 1.0;
            L[j][i] -= 1.0;
            L[i][i] += 1.0;
            L[j][j] += 1.0;
        }
        return L;
    }

    private static double[] subgraphEigenvalues(List<Integer> vertices, List<int[]> edges) {
        int n = vertices.size();
        if (n == 0) return new double[0];
        if (n == 1) return new double[]{0.0};
        double[][] L = laplacianOfSubgraph(vertices, edges);
        double[] eigs = symmetricEigenvalues(L);
        Arrays.sort(eigs);
        return eigs;
    }

    // -----------------------------------------------------------------------
    // Jacobi symmetric eigensolver (sorted ascending output)
    // -----------------------------------------------------------------------

    private static double[] symmetricEigenvalues(double[][] A) {
        int n = A.length;
        if (n == 0) return new double[0];
        double[][] M = new double[n][n];
        for (int i = 0; i < n; i++) System.arraycopy(A[i], 0, M[i], 0, n);
        int maxIter = 200;
        double tol = 1e-12;
        for (int iter = 0; iter < maxIter; iter++) {
            int p = 0, q = 1;
            double best = -1.0;
            for (int i = 0; i < n; i++)
                for (int j = i + 1; j < n; j++) {
                    double a = Math.abs(M[i][j]);
                    if (a > best) { best = a; p = i; q = j; }
                }
            if (best < tol) break;
            double app = M[p][p], aqq = M[q][q], apq = M[p][q];
            if (Math.abs(apq) < tol) break;
            double theta = (aqq - app) / (2.0 * apq);
            double t = (theta >= 0)
                    ? 1.0 / (theta + Math.sqrt(1.0 + theta * theta))
                    : 1.0 / (theta - Math.sqrt(1.0 + theta * theta));
            double c = 1.0 / Math.sqrt(1.0 + t * t);
            double s = t * c;
            for (int i = 0; i < n; i++) {
                if (i != p && i != q) {
                    double mip = M[i][p];
                    double miq = M[i][q];
                    M[i][p] = c * mip - s * miq;
                    M[p][i] = M[i][p];
                    M[i][q] = s * mip + c * miq;
                    M[q][i] = M[i][q];
                }
            }
            M[p][p] = c * c * app - 2.0 * s * c * apq + s * s * aqq;
            M[q][q] = s * s * app + 2.0 * s * c * apq + c * c * aqq;
            M[p][q] = 0.0;
            M[q][p] = 0.0;
        }
        double[] eigs = new double[n];
        for (int i = 0; i < n; i++) eigs[i] = M[i][i];
        return eigs;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public static List<SpectralSnapshot> persistentLaplacianSpectra(StateSpace ss) {
        return persistentLaplacianSpectra(ss, Filtration.RANK);
    }

    public static List<SpectralSnapshot> persistentLaplacianSpectra(StateSpace ss, Filtration filtration) {
        Map<Integer, Double> vfilt = vertexFiltration(ss, filtration);
        List<int[]> covers = coveringRelations(ss);
        List<Double> levels = levels(vfilt, covers);

        List<SpectralSnapshot> snapshots = new ArrayList<>();
        for (double t : levels) {
            List<Integer> verts = new ArrayList<>();
            for (int v : ss.states()) {
                if (vfilt.getOrDefault(v, 0.0) <= t + EPS) verts.add(v);
            }
            Set<Integer> vSet = new HashSet<>(verts);
            List<int[]> edges = new ArrayList<>();
            for (int[] e : covers) {
                double mx = Math.max(vfilt.getOrDefault(e[0], 0.0), vfilt.getOrDefault(e[1], 0.0));
                if (mx <= t + EPS && vSet.contains(e[0]) && vSet.contains(e[1])) {
                    edges.add(e);
                }
            }
            double[] eigs = subgraphEigenvalues(verts, edges);
            snapshots.add(new SpectralSnapshot(t, verts.size(), edges.size(), eigs));
        }
        return snapshots;
    }

    public static List<double[]> persistentFiedlerTrace(StateSpace ss) {
        return persistentFiedlerTrace(ss, Filtration.RANK);
    }

    public static List<double[]> persistentFiedlerTrace(StateSpace ss, Filtration filtration) {
        List<SpectralSnapshot> snaps = persistentLaplacianSpectra(ss, filtration);
        List<double[]> out = new ArrayList<>();
        for (SpectralSnapshot s : snaps) out.add(new double[]{s.level, s.fiedler()});
        return out;
    }

    public static List<SpectralPersistencePair> spectralPersistenceDiagram(StateSpace ss) {
        return spectralPersistenceDiagram(ss, Filtration.RANK);
    }

    public static List<SpectralPersistencePair> spectralPersistenceDiagram(StateSpace ss, Filtration filtration) {
        List<SpectralSnapshot> snaps = persistentLaplacianSpectra(ss, filtration);
        List<SpectralPersistencePair> pairs = new ArrayList<>();
        if (snaps.isEmpty()) return pairs;
        SpectralSnapshot finalSnap = snaps.get(snaps.size() - 1);
        int m = finalSnap.eigenvalues.length;
        for (int k = 0; k < m; k++) {
            double birthLevel = Double.POSITIVE_INFINITY;
            double birthVal = 0.0;
            double deathLevel = Double.POSITIVE_INFINITY;
            double deathVal = Double.NaN;
            for (SpectralSnapshot snap : snaps) {
                if (snap.eigenvalues.length > k) {
                    if (Double.isInfinite(birthLevel)) {
                        birthLevel = snap.level;
                        birthVal = snap.eigenvalues[k];
                    }
                    if (k == 0) continue;
                    if (snap.eigenvalues[k] > ZERO_TOL && Double.isInfinite(deathLevel)) {
                        deathLevel = snap.level;
                        deathVal = snap.eigenvalues[k];
                    }
                }
            }
            if (Double.isInfinite(birthLevel)) continue;
            pairs.add(new SpectralPersistencePair(k, birthLevel, deathLevel, birthVal, deathVal));
        }
        return pairs;
    }

    public static PersistentSpectralFingerprint persistentSpectralFingerprint(StateSpace ss) {
        return persistentSpectralFingerprint(ss, Filtration.RANK);
    }

    public static PersistentSpectralFingerprint persistentSpectralFingerprint(StateSpace ss, Filtration filtration) {
        List<SpectralSnapshot> snaps = persistentLaplacianSpectra(ss, filtration);
        if (snaps.isEmpty()) {
            return new PersistentSpectralFingerprint(0, new double[0], new double[0],
                    new int[0], new double[0], 0.0, 0.0, new double[0]);
        }
        int n = snaps.size();
        double[] fiedler = new double[n];
        double[] radius = new double[n];
        int[] zeros = new int[n];
        double[] energy = new double[n];
        for (int i = 0; i < n; i++) {
            SpectralSnapshot s = snaps.get(i);
            fiedler[i] = s.fiedler();
            radius[i] = s.spectralRadius();
            zeros[i] = s.zeroMultiplicity();
            double e = 0.0;
            for (double v : s.eigenvalues) e += Math.abs(v);
            energy[i] = e;
        }

        List<SpectralPersistencePair> pairs = spectralPersistenceDiagram(ss, filtration);
        double totalPers = 0.0;
        for (SpectralPersistencePair p : pairs) {
            if (p.index == 0) continue;
            if (Double.isInfinite(p.death)) continue;
            totalPers += Math.max(0.0, p.death - p.birth);
        }

        double maxF = fiedler[0];
        for (double v : fiedler) if (v > maxF) maxF = v;

        return new PersistentSpectralFingerprint(n, fiedler, radius, zeros, energy,
                totalPers, maxF, snaps.get(n - 1).eigenvalues);
    }

    public static double fingerprintDistance(PersistentSpectralFingerprint f1,
                                              PersistentSpectralFingerprint f2) {
        double[] v1 = f1.asVector();
        double[] v2 = f2.asVector();
        int n = Math.max(v1.length, v2.length);
        if (n == 0) return 0.0;
        double max = 0.0;
        for (int i = 0; i < n; i++) {
            double a = i < v1.length ? v1[i] : 0.0;
            double b = i < v2.length ? v2[i] : 0.0;
            double d = Math.abs(a - b);
            if (d > max) max = d;
        }
        return max;
    }

    // -----------------------------------------------------------------------
    // Bidirectional morphisms
    // -----------------------------------------------------------------------

    public static PersistentSpectralFingerprint phiLatticeToSpectrum(StateSpace ss) {
        return persistentSpectralFingerprint(ss);
    }

    public static SpectralActions psiSpectrumToAction(PersistentSpectralFingerprint fp) {
        return psiSpectrumToAction(fp, 0.5, 2.0);
    }

    public static SpectralActions psiSpectrumToAction(PersistentSpectralFingerprint fp,
                                                       double bottleneckThreshold,
                                                       double complexityThreshold) {
        double[] trace = fp.fiedlerTrace;
        List<double[]> fragile = new ArrayList<>();
        boolean bottleneck = false;
        for (int i = 0; i < trace.length; i++) {
            double f = trace[i];
            if (f < bottleneckThreshold && f > 0) fragile.add(new double[]{i, f});
            if (f < bottleneckThreshold && f > 0) bottleneck = true;
        }
        boolean complexityAlert = fp.totalSpectralPersistence > complexityThreshold;

        int monitorLevel = 0;
        if (trace.length > 0) {
            double minPos = Double.POSITIVE_INFINITY;
            int idx = -1;
            for (int i = 0; i < trace.length; i++) {
                if (trace[i] > 0 && trace[i] < minPos) {
                    minPos = trace[i];
                    idx = i;
                }
            }
            monitorLevel = idx >= 0 ? idx : 0;
        }

        double stability = trace.length == 0 ? 1.0
                : 1.0 - ((double) fragile.size() / trace.length);

        return new SpectralActions(bottleneck, complexityAlert, fragile, monitorLevel, stability);
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    public static PersistentSpectralAnalysis analyzePersistentSpectrum(StateSpace ss) {
        return analyzePersistentSpectrum(ss, Filtration.RANK);
    }

    public static PersistentSpectralAnalysis analyzePersistentSpectrum(StateSpace ss, Filtration filtration) {
        List<SpectralSnapshot> snaps = persistentLaplacianSpectra(ss, filtration);
        List<SpectralPersistencePair> pairs = spectralPersistenceDiagram(ss, filtration);
        PersistentSpectralFingerprint fp = persistentSpectralFingerprint(ss, filtration);
        return new PersistentSpectralAnalysis(snaps, pairs, fp, ss.states().size());
    }
}
