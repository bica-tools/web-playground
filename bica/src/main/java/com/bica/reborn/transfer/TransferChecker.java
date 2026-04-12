package com.bica.reborn.transfer;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Transfer matrix analysis for session type lattices (Step 30f).
 *
 * <p>The transfer matrix T encodes the covering relation of the Hasse diagram.
 * T^k[i,j] counts paths of length exactly k from state i to j.
 *
 * <p>Faithful Java port of {@code reticulate.transfer}.
 */
public final class TransferChecker {

    private TransferChecker() {}

    /** Complete transfer matrix analysis result. */
    public record TransferResult(
            int numStates,
            int[][] transferMatrix,
            List<Integer> states,
            int totalPaths,
            Map<Integer, Integer> pathLengthDistribution,
            double expectedPathLength,
            int height,
            boolean isAcyclic) {}

    /** Compute the directed transfer (covering) matrix. */
    public static int[][] transferMatrix(StateSpace ss, List<Integer> states) {
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);
        List<Zeta.Pair> covers = Zeta.coveringRelation(ss);

        int[][] T = new int[n][n];
        for (Zeta.Pair c : covers) {
            Integer xi = idx.get(c.x()), yi = idx.get(c.y());
            if (xi != null && yi != null) T[xi][yi] = 1;
        }
        return T;
    }

    /** Count directed paths from top to bottom by length. */
    public static Map<Integer, Integer> countPathsByLength(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int[][] T = transferMatrix(ss, states);
        int n = states.size();
        if (n == 0) return Map.of();

        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);
        int topI = idx.getOrDefault(ss.top(), 0);
        int botI = idx.getOrDefault(ss.bottom(), 0);

        if (topI == botI) return Map.of(0, 1);

        Map<Integer, Integer> counts = new LinkedHashMap<>();
        int[][] power = copyMatrix(T);

        for (int k = 1; k <= n; k++) {
            int val = power[topI][botI];
            if (val > 0) counts.put(k, val);
            if (isZero(power)) break;
            power = Zeta.matMul(power, T);
        }
        return counts;
    }

    /** Total number of directed paths from top to bottom. */
    public static int totalPathCount(StateSpace ss) {
        return countPathsByLength(ss).values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Expected (average) path length from top to bottom. */
    public static double expectedPathLength(StateSpace ss) {
        Map<Integer, Integer> counts = countPathsByLength(ss);
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) return 0.0;
        long weighted = counts.entrySet().stream()
                .mapToLong(e -> (long) e.getKey() * e.getValue()).sum();
        return (double) weighted / total;
    }

    /** Compute T^k via fast exponentiation. */
    public static int[][] transferPower(StateSpace ss, int k) {
        List<Integer> states = Zeta.stateList(ss);
        int[][] T = transferMatrix(ss, states);
        return Zeta.zetaPower(T, k);
    }

    /** Enumerate all directed paths from top to bottom (bounded). */
    public static List<List<Integer>> enumeratePaths(StateSpace ss, int maxPaths) {
        List<Integer> states = Zeta.stateList(ss);
        int[][] T = transferMatrix(ss, states);
        int n = states.size();

        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int i = 0; i < n; i++) {
            List<Integer> neighbors = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if (T[i][j] > 0) neighbors.add(states.get(j));
            }
            adj.put(states.get(i), neighbors);
        }

        List<List<Integer>> paths = new ArrayList<>();
        Deque<Integer> path = new ArrayDeque<>();
        path.add(ss.top());
        dfsEnum(ss.top(), ss.bottom(), adj, path, paths, maxPaths);
        return paths;
    }

    private static void dfsEnum(int current, int bottom,
                                 Map<Integer, List<Integer>> adj,
                                 Deque<Integer> path,
                                 List<List<Integer>> paths, int maxPaths) {
        if (paths.size() >= maxPaths) return;
        if (current == bottom) {
            paths.add(new ArrayList<>(path));
            return;
        }
        for (int nxt : adj.getOrDefault(current, List.of())) {
            path.addLast(nxt);
            dfsEnum(nxt, bottom, adj, path, paths, maxPaths);
            path.removeLast();
        }
    }

    /** Check if the covering matrix defines an acyclic graph. */
    public static boolean isTransferAcyclic(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int[][] T = transferMatrix(ss, states);
        int n = states.size();
        int[][] power = copyMatrix(T);
        for (int i = 0; i < n; i++) power = Zeta.matMul(power, T);
        return isZero(power);
    }

    /** Compute resolvent (I - zT)^{-1} or null if singular. */
    public static double[][] resolventAt(StateSpace ss, double z) {
        List<Integer> states = Zeta.stateList(ss);
        int[][] T = transferMatrix(ss, states);
        int n = states.size();

        double[][] aug = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                aug[i][j] = (i == j ? 1.0 : 0.0) - z * T[i][j];
            }
            aug[i][n + i] = 1.0;
        }

        for (int col = 0; col < n; col++) {
            int pivot = -1;
            for (int row = col; row < n; row++) {
                if (Math.abs(aug[row][col]) > 1e-12) { pivot = row; break; }
            }
            if (pivot == -1) return null;

            double[] tmp = aug[col]; aug[col] = aug[pivot]; aug[pivot] = tmp;

            double scale = aug[col][col];
            for (int j = 0; j < 2 * n; j++) aug[col][j] /= scale;

            for (int row = 0; row < n; row++) {
                if (row == col) continue;
                double factor = aug[row][col];
                for (int j = 0; j < 2 * n; j++) aug[row][j] -= factor * aug[col][j];
            }
        }

        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                inv[i][j] = aug[i][n + j];
        return inv;
    }

    /** Complete transfer matrix analysis. */
    public static TransferResult analyzeTransfer(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int[][] T = transferMatrix(ss, states);
        Map<Integer, Integer> counts = countPathsByLength(ss);
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        double avgLen = expectedPathLength(ss);
        int height = counts.isEmpty() ? 0 : Collections.max(counts.keySet());
        boolean acyclic = isTransferAcyclic(ss);

        return new TransferResult(states.size(), T, states, total, counts, avgLen, height, acyclic);
    }

    // Helpers
    private static int[][] copyMatrix(int[][] M) {
        int[][] C = new int[M.length][];
        for (int i = 0; i < M.length; i++) C[i] = M[i].clone();
        return C;
    }

    private static boolean isZero(int[][] M) {
        for (int[] row : M) for (int v : row) if (v != 0) return false;
        return true;
    }
}
