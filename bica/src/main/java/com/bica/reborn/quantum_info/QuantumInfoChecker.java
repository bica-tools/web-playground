package com.bica.reborn.quantum_info;

import com.bica.reborn.globaltype.GlobalType;
import com.bica.reborn.globaltype.GlobalTypeChecker;
import com.bica.reborn.globaltype.Projection;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.statespace.TransitionKind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Quantum information theory of session types (Step 31l) — Java port of
 * {@code reticulate/reticulate/quantum_info.py}.
 *
 * <p>Encodes a session-type state space as a pure quantum state on a real
 * Hilbert space whose basis vectors are the reachable states. Provides
 * density matrices, von Neumann entropy via a small Jacobi eigensolver,
 * partial traces, tensor products, and a quantum mutual information
 * predictor for multiparty global types.
 */
public final class QuantumInfoChecker {

    private QuantumInfoChecker() {}

    // -----------------------------------------------------------------------
    // Matrix primitives
    // -----------------------------------------------------------------------

    public static double[][] zeros(int n) { return zeros(n, n); }
    public static double[][] zeros(int n, int m) { return new double[n][m]; }

    public static double[][] identity(int n) {
        double[][] M = new double[n][n];
        for (int i = 0; i < n; i++) M[i][i] = 1.0;
        return M;
    }

    public static double[][] outer(double[] psi) {
        int n = psi.length;
        double[][] M = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                M[i][j] = psi[i] * psi[j];
        return M;
    }

    public static double[][] kron(double[][] A, double[][] B) {
        int ra = A.length, ca = A[0].length;
        int rb = B.length, cb = B[0].length;
        double[][] R = new double[ra * rb][ca * cb];
        for (int i = 0; i < ra; i++)
            for (int j = 0; j < ca; j++) {
                double a = A[i][j];
                for (int k = 0; k < rb; k++)
                    for (int l = 0; l < cb; l++)
                        R[i * rb + k][j * cb + l] = a * B[k][l];
            }
        return R;
    }

    public static double[][] tensorDensity(double[][] rhoA, double[][] rhoB) {
        return kron(rhoA, rhoB);
    }

    public static double[][] partialTraceFirst(double[][] rho, int dimA, int dimB) {
        if (rho.length != dimA * dimB) throw new IllegalArgumentException("rho dim mismatch");
        double[][] rhoB = new double[dimB][dimB];
        for (int i = 0; i < dimB; i++)
            for (int j = 0; j < dimB; j++) {
                double s = 0.0;
                for (int k = 0; k < dimA; k++) s += rho[k * dimB + i][k * dimB + j];
                rhoB[i][j] = s;
            }
        return rhoB;
    }

    public static double[][] partialTraceSecond(double[][] rho, int dimA, int dimB) {
        if (rho.length != dimA * dimB) throw new IllegalArgumentException("rho dim mismatch");
        double[][] rhoA = new double[dimA][dimA];
        for (int i = 0; i < dimA; i++)
            for (int j = 0; j < dimA; j++) {
                double s = 0.0;
                for (int k = 0; k < dimB; k++) s += rho[i * dimB + k][j * dimB + k];
                rhoA[i][j] = s;
            }
        return rhoA;
    }

    // -----------------------------------------------------------------------
    // Jacobi eigensolver
    // -----------------------------------------------------------------------

    public static double[] symmetricEigenvalues(double[][] A) {
        return symmetricEigenvalues(A, 1e-12, 200);
    }

    public static double[] symmetricEigenvalues(double[][] A, double tol, int maxIter) {
        int n = A.length;
        if (n == 0) return new double[0];
        double[][] M = new double[n][n];
        for (int i = 0; i < n; i++) System.arraycopy(A[i], 0, M[i], 0, n);
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
        // sort descending
        Arrays.sort(eigs);
        for (int i = 0, j = n - 1; i < j; i++, j--) {
            double tmp = eigs[i]; eigs[i] = eigs[j]; eigs[j] = tmp;
        }
        return eigs;
    }

    private static double xlogx(double x) {
        if (x <= 1e-15) return 0.0;
        return x * Math.log(x);
    }

    public static double vonNeumannEntropy(double[][] rho) {
        double[] eigs = symmetricEigenvalues(rho);
        double s = 0.0;
        for (double e : eigs) s += xlogx(Math.max(e, 0.0));
        return -s;
    }

    // -----------------------------------------------------------------------
    // Protocol pure state
    // -----------------------------------------------------------------------

    /** Pure quantum state encoding a session-type state space. */
    public static final class ProtocolState {
        public final int[] basis;
        public final double[] amplitudes;
        public final double[][] density;

        public ProtocolState(int[] basis, double[] amplitudes, double[][] density) {
            this.basis = basis;
            this.amplitudes = amplitudes;
            this.density = density;
        }

        public int dimension() { return basis.length; }
    }

    private static Map<Integer, Double> reachabilityWeights(StateSpace ss) {
        List<Integer> order = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        List<Integer> queue = new ArrayList<>();
        queue.add(ss.top());
        while (!queue.isEmpty()) {
            List<Integer> nxt = new ArrayList<>();
            for (int s : queue) {
                if (!seen.add(s)) continue;
                order.add(s);
                for (var e : ss.enabled(s)) {
                    if (!seen.contains(e.getValue())) nxt.add(e.getValue());
                }
            }
            queue = nxt;
        }
        Map<Integer, Double> weights = new HashMap<>();
        for (int s : order) weights.put(s, 0.0);
        weights.put(ss.top(), 1.0);
        for (int s : order) {
            double ws = weights.getOrDefault(s, 0.0);
            for (var e : ss.enabled(s)) {
                int tgt = e.getValue();
                if (tgt == s) continue;
                weights.merge(tgt, ws, Double::sum);
            }
        }
        for (int s : order) {
            if (weights.get(s) <= 0.0) weights.put(s, 1.0);
        }
        return weights;
    }

    public static ProtocolState protocolState(StateSpace ss) {
        Map<Integer, Double> weights = reachabilityWeights(ss);

        // Decoherence: count selection sources, dampen successors by 1/sqrt(k)
        Map<Integer, Integer> selSources = new HashMap<>();
        for (Transition t : ss.transitions()) {
            if (t.kind() == TransitionKind.SELECTION) {
                selSources.merge(t.source(), 1, Integer::sum);
            }
        }
        for (var entry : selSources.entrySet()) {
            int src = entry.getKey();
            int k = Math.max(entry.getValue(), 1);
            for (Transition t : ss.transitions()) {
                if (t.source() == src && t.kind() == TransitionKind.SELECTION
                        && weights.containsKey(t.target())) {
                    weights.put(t.target(), weights.get(t.target()) / Math.sqrt(k));
                }
            }
        }

        TreeSet<Integer> sortedKeys = new TreeSet<>(weights.keySet());
        int n = sortedKeys.size();
        int[] basis = new int[n];
        double[] amps = new double[n];
        int i = 0;
        for (int s : sortedKeys) {
            basis[i] = s;
            amps[i] = Math.sqrt(Math.max(weights.get(s), 0.0));
            i++;
        }
        double sumSq = 0.0;
        for (double a : amps) sumSq += a * a;
        double norm = Math.sqrt(sumSq);
        if (norm <= 0) norm = 1.0;
        for (int j = 0; j < n; j++) amps[j] /= norm;
        double[][] density = outer(amps);
        return new ProtocolState(basis, amps, density);
    }

    public static double[][] densityMatrix(StateSpace ss) {
        return protocolState(ss).density;
    }

    public static double protocolEntropy(StateSpace ss) {
        return vonNeumannEntropy(densityMatrix(ss));
    }

    /** Returns (S_A, S_B, S_AB). */
    public static double[] reducedEntropyParallel(double[][] joint, int dimA, int dimB) {
        double[][] rhoA = partialTraceSecond(joint, dimA, dimB);
        double[][] rhoB = partialTraceFirst(joint, dimA, dimB);
        return new double[]{vonNeumannEntropy(rhoA), vonNeumannEntropy(rhoB), vonNeumannEntropy(joint)};
    }

    // -----------------------------------------------------------------------
    // MPST quantum mutual information
    // -----------------------------------------------------------------------

    public static final class MPSTQuantumResult {
        public final Map<String, Double> roleEntropies;
        public final double jointEntropy;
        public final double mutualInformation;
        public final String roleA;
        public final String roleB;
        public final int dimA;
        public final int dimB;

        public MPSTQuantumResult(Map<String, Double> roleEntropies, double jointEntropy,
                                 double mutualInformation, String roleA, String roleB,
                                 int dimA, int dimB) {
            this.roleEntropies = Map.copyOf(roleEntropies);
            this.jointEntropy = jointEntropy;
            this.mutualInformation = mutualInformation;
            this.roleA = roleA;
            this.roleB = roleB;
            this.dimA = dimA;
            this.dimB = dimB;
        }
    }

    public static MPSTQuantumResult mpstMutualInformation(GlobalType g, String roleA, String roleB) {
        SessionType sA = Projection.project(g, roleA);
        SessionType sB = Projection.project(g, roleB);
        StateSpace ssA = StateSpaceBuilder.build(sA);
        StateSpace ssB = StateSpaceBuilder.build(sB);
        double[][] rhoApure = densityMatrix(ssA);
        double[][] rhoBpure = densityMatrix(ssB);
        int dimA = rhoApure.length;
        int dimB = rhoBpure.length;

        // Count A-B messages in the global state space.
        StateSpace globalSs = GlobalTypeChecker.buildStateSpace(g);
        int abMessages = 0;
        for (Transition t : globalSs.transitions()) {
            String lbl = t.label();
            int arrow = lbl.indexOf("->");
            int colon = lbl.indexOf(':');
            if (arrow >= 0 && colon > arrow) {
                String who = lbl.substring(0, colon);
                int a = who.indexOf("->");
                if (a >= 0) {
                    String s = who.substring(0, a);
                    String r = who.substring(a + 2);
                    Set<String> pair = new HashSet<>();
                    pair.add(s); pair.add(r);
                    Set<String> tgt = new HashSet<>();
                    tgt.add(roleA); tgt.add(roleB);
                    if (pair.equals(tgt)) abMessages++;
                }
            }
        }

        double[][] rhoA;
        double[][] rhoB;
        double[][] rhoJoint;
        if (dimA == 0 || dimB == 0) {
            rhoA = rhoApure;
            rhoB = rhoBpure;
            rhoJoint = kron(rhoApure, rhoBpure);
        } else {
            int d = Math.min(dimA, dimB);
            double alpha = abMessages / (abMessages + 1.0);
            double[] pa = new double[dimA];
            double[] pb = new double[dimB];
            for (int i = 0; i < dimA; i++) pa[i] = alpha / dimA + (i == 0 ? (1.0 - alpha) : 0.0);
            for (int i = 0; i < dimB; i++) pb[i] = alpha / dimB + (i == 0 ? (1.0 - alpha) : 0.0);
            rhoA = new double[dimA][dimA];
            for (int i = 0; i < dimA; i++) rhoA[i][i] = pa[i];
            rhoB = new double[dimB][dimB];
            for (int i = 0; i < dimB; i++) rhoB[i][i] = pb[i];
            rhoJoint = new double[dimA * dimB][dimA * dimB];
            for (int i = 0; i < d; i++) {
                int idx = i * dimB + i;
                rhoJoint[idx][idx] += alpha / d;
            }
            rhoJoint[0][0] += (1.0 - alpha);
            double tr = 0.0;
            for (int i = 0; i < dimA * dimB; i++) tr += rhoJoint[i][i];
            if (tr > 0 && Math.abs(tr - 1.0) > 1e-12) {
                for (int i = 0; i < dimA * dimB; i++) rhoJoint[i][i] /= tr;
            }
        }

        double sa = vonNeumannEntropy(rhoA);
        double sb = vonNeumannEntropy(rhoB);
        double sab = vonNeumannEntropy(rhoJoint);
        double mi = sa + sb - sab;

        Map<String, Double> ents = new LinkedHashMap<>();
        for (String r : GlobalTypeChecker.roles(g)) {
            try {
                SessionType loc = Projection.project(g, r);
                StateSpace sr = StateSpaceBuilder.build(loc);
                ents.put(r, vonNeumannEntropy(densityMatrix(sr)));
            } catch (Exception ex) {
                ents.put(r, 0.0);
            }
        }
        return new MPSTQuantumResult(ents, sab, mi, roleA, roleB, dimA, dimB);
    }

    // -----------------------------------------------------------------------
    // Bidirectional morphisms
    // -----------------------------------------------------------------------

    public static final class HilbertEncoding {
        public final int[] basisLabels;
        public final double[][] density;
        public final int dimension;

        public HilbertEncoding(int[] basisLabels, double[][] density, int dimension) {
            this.basisLabels = basisLabels;
            this.density = density;
            this.dimension = dimension;
        }
    }

    public static HilbertEncoding phiToHilbert(StateSpace ss) {
        ProtocolState ps = protocolState(ss);
        return new HilbertEncoding(ps.basis, ps.density, ps.dimension());
    }

    public static final class ReconstructedLattice {
        public final int[] elements;
        public final Set<long[]> order; // unused; we expose Set<Pair>
        public final Set<String> orderKeys; // "i,j"

        public ReconstructedLattice(int[] elements, Set<String> orderKeys) {
            this.elements = elements;
            this.order = null;
            this.orderKeys = Set.copyOf(orderKeys);
        }

        public boolean contains(int a, int b) {
            return orderKeys.contains(a + "," + b);
        }
    }

    public static ReconstructedLattice psiFromHilbert(HilbertEncoding enc) {
        return psiFromHilbert(enc, 1e-9);
    }

    public static ReconstructedLattice psiFromHilbert(HilbertEncoding enc, double threshold) {
        int n = enc.dimension;
        double[][] rho = enc.density;
        Set<String> edges = new HashSet<>();
        for (int i = 0; i < n; i++) edges.add(i + "," + i);
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                if (Math.abs(rho[i][j]) > threshold) edges.add(i + "," + j);
            }
        // Transitive closure
        boolean changed = true;
        while (changed) {
            changed = false;
            Set<String> next = new HashSet<>(edges);
            for (String ab : edges) {
                int comma = ab.indexOf(',');
                int a = Integer.parseInt(ab.substring(0, comma));
                int b = Integer.parseInt(ab.substring(comma + 1));
                for (String cd : edges) {
                    int c2 = cd.indexOf(',');
                    int c = Integer.parseInt(cd.substring(0, c2));
                    int d = Integer.parseInt(cd.substring(c2 + 1));
                    if (b == c && next.add(a + "," + d)) {
                        changed = true;
                    }
                }
            }
            edges = next;
        }
        int[] els = new int[n];
        for (int i = 0; i < n; i++) els[i] = i;
        return new ReconstructedLattice(els, edges);
    }

    public static final class MorphismClassification {
        public final boolean phiInjective;
        public final boolean phiSurjective;
        public final boolean psiPreservesTop;
        public final boolean psiPreservesBottom;
        public final boolean roundTripLossless;
        public final String kind;

        public MorphismClassification(boolean phiInjective, boolean phiSurjective,
                                      boolean psiPreservesTop, boolean psiPreservesBottom,
                                      boolean roundTripLossless, String kind) {
            this.phiInjective = phiInjective;
            this.phiSurjective = phiSurjective;
            this.psiPreservesTop = psiPreservesTop;
            this.psiPreservesBottom = psiPreservesBottom;
            this.roundTripLossless = roundTripLossless;
            this.kind = kind;
        }
    }

    public static MorphismClassification classifyPair(StateSpace ss) {
        HilbertEncoding enc = phiToHilbert(ss);
        ReconstructedLattice rec = psiFromHilbert(enc);
        int n = enc.dimension;
        Set<Integer> distinct = new HashSet<>();
        for (int b : enc.basisLabels) distinct.add(b);
        boolean phiInj = distinct.size() == n;
        boolean phiSurj = true;
        boolean psiTop = rec.contains(0, 0);
        boolean psiBot = (n > 0) ? rec.contains(n - 1, n - 1) : true;
        boolean roundTrip = phiInj && phiSurj;
        String kind;
        if (roundTrip && psiTop && psiBot) kind = "isomorphism";
        else if (phiInj) kind = "embedding";
        else if (phiSurj) kind = "projection";
        else kind = "galois";
        return new MorphismClassification(phiInj, phiSurj, psiTop, psiBot, roundTrip, kind);
    }
}
