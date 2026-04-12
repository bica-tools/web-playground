package com.bica.reborn.spectral_sync;

import com.bica.reborn.algebraic.AlgebraicChecker;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Spectral synchronization analysis for multiparty session types (Step 31d).
 *
 * <p>Applies spectral methods to analyze synchronization patterns in
 * multiparty session types. The global type's state space is decomposed
 * spectrally to identify synchronization bottlenecks and communication
 * patterns between roles.
 *
 * <ul>
 *   <li><b>Role coupling matrix</b>: spectral analysis of inter-role communication</li>
 *   <li><b>Synchronization spectrum</b>: eigenvalues of the role interaction graph</li>
 *   <li><b>Role clustering</b>: spectral clustering of roles by communication pattern</li>
 *   <li><b>Bottleneck roles</b>: roles with lowest algebraic connectivity</li>
 * </ul>
 *
 * <p>Java port of Python {@code spectral_sync.py}.
 */
public final class SpectralSyncChecker {

    private SpectralSyncChecker() {}

    // -----------------------------------------------------------------------
    // Role extraction
    // -----------------------------------------------------------------------

    /**
     * Extract role names from transition labels.
     *
     * <p>For global types: labels are "sender-&gt;receiver:method".
     * For local types: labels are just method names (single role).
     */
    public static List<String> extractRoles(StateSpace ss) {
        Set<String> roles = new TreeSet<>();
        for (var t : ss.transitions()) {
            String label = t.label();
            if (label.contains("->")) {
                String[] parts = label.split("->");
                roles.add(parts[0].trim());
                String[] recvMethod = parts[1].split(":");
                if (recvMethod.length > 0) {
                    roles.add(recvMethod[0].trim());
                }
            } else {
                roles.add(label);
            }
        }
        return new ArrayList<>(roles);
    }

    // -----------------------------------------------------------------------
    // Role coupling matrix
    // -----------------------------------------------------------------------

    /**
     * Compute role x role coupling matrix.
     *
     * <p>M[i][j] = number of transitions involving both role i and role j.
     * For global types with "sender-&gt;receiver:method" labels.
     * For local types, each label is its own "role" (method name).
     *
     * @return a pair of (matrix, roles list)
     */
    public static CouplingResult roleCouplingMatrix(StateSpace ss) {
        List<String> roles = extractRoles(ss);
        int n = roles.size();
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) {
            idx.put(roles.get(i), i);
        }

        int[][] M = new int[n][n];

        for (var t : ss.transitions()) {
            String label = t.label();
            if (label.contains("->")) {
                String[] parts = label.split("->");
                String sender = parts[0].trim();
                String[] recvMethod = parts[1].split(":");
                String receiver = recvMethod.length > 0 ? recvMethod[0].trim() : "";
                if (idx.containsKey(sender) && idx.containsKey(receiver)) {
                    int si = idx.get(sender);
                    int ri = idx.get(receiver);
                    M[si][ri]++;
                    M[ri][si]++;  // Symmetric
                }
            } else {
                // Local type: self-interaction
                if (idx.containsKey(label)) {
                    int li = idx.get(label);
                    M[li][li]++;
                }
            }
        }

        // Convert to List<List<Integer>>
        List<List<Integer>> matrix = new ArrayList<>();
        for (int[] row : M) {
            List<Integer> r = new ArrayList<>();
            for (int v : row) {
                r.add(v);
            }
            matrix.add(r);
        }
        return new CouplingResult(matrix, roles);
    }

    /** Result of role coupling matrix computation. */
    public record CouplingResult(List<List<Integer>> matrix, List<String> roles) {}

    // -----------------------------------------------------------------------
    // Spectral analysis of coupling
    // -----------------------------------------------------------------------

    /**
     * Eigenvalues of the role coupling matrix.
     */
    public static List<Double> syncSpectrum(StateSpace ss) {
        CouplingResult cr = roleCouplingMatrix(ss);
        int n = cr.matrix().size();
        if (n == 0) return List.of();
        if (n == 1) return List.of((double) cr.matrix().get(0).get(0));

        // Convert to double[][]
        double[][] mat = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                mat[i][j] = cr.matrix().get(i).get(j);
            }
        }
        double[] eigs = AlgebraicChecker.computeEigenvalues(mat);
        List<Double> result = new ArrayList<>();
        for (double e : eigs) {
            result.add(e);
        }
        return result;
    }

    /**
     * Maximum coupling between any two distinct roles.
     */
    public static int maxCoupling(StateSpace ss) {
        CouplingResult cr = roleCouplingMatrix(ss);
        List<List<Integer>> M = cr.matrix();
        int n = M.size();
        int best = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                best = Math.max(best, M.get(i).get(j));
            }
        }
        return best;
    }

    /**
     * Minimum total coupling of any role (least connected role).
     */
    public static int isolationScore(StateSpace ss) {
        CouplingResult cr = roleCouplingMatrix(ss);
        List<List<Integer>> M = cr.matrix();
        if (M.isEmpty()) return 0;
        int min = Integer.MAX_VALUE;
        for (List<Integer> row : M) {
            int sum = 0;
            for (int v : row) {
                sum += v;
            }
            min = Math.min(min, sum);
        }
        return min;
    }

    // -----------------------------------------------------------------------
    // Main analysis
    // -----------------------------------------------------------------------

    /**
     * Complete spectral synchronization analysis.
     */
    public static SyncResult analyze(StateSpace ss) {
        CouplingResult cr = roleCouplingMatrix(ss);
        List<Double> spec = syncSpectrum(ss);
        int mc = maxCoupling(ss);
        int iso = isolationScore(ss);

        return new SyncResult(
                ss.states().size(),
                cr.roles().size(),
                cr.matrix(),
                cr.roles(),
                spec,
                mc,
                iso);
    }
}
