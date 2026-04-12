package com.bica.reborn.quantum_walk;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Quantum walks on session type lattices (Step 31c) -- Java port of
 * {@code reticulate/reticulate/quantum_walk.py}.
 *
 * <p>Discrete-time quantum walk on the Hasse diagram of a session type
 * lattice.  The walk evolves a probability amplitude vector using the
 * adjacency matrix as the coin operator:
 * <ul>
 *   <li>Quantum walk evolution: U = e^{iAt} for continuous-time</li>
 *   <li>Transition probability: |&lt;j|U|i&gt;|^2 at time t</li>
 *   <li>Mixing rate: how fast the quantum walk spreads</li>
 *   <li>Localization: probability of return to start</li>
 *   <li>Quantum hitting time: expected time to reach bottom from top</li>
 * </ul>
 */
public final class QuantumWalkChecker {

    private QuantumWalkChecker() {}

    // -----------------------------------------------------------------------
    // Result type
    // -----------------------------------------------------------------------

    /**
     * Quantum walk analysis results.
     *
     * @param numStates          number of states
     * @param evolutionMatrix    |U(t=1)|^2 transition probability matrix
     * @param returnProbability  probability of being at start after t=1
     * @param spread             standard deviation of probability distribution at t=1
     * @param topToBottomProb    probability of reaching bottom from top at t=1
     * @param mixingTimeEstimate estimated quantum mixing time
     */
    public record QuantumWalkResult(
            int numStates,
            double[][] evolutionMatrix,
            double returnProbability,
            double spread,
            double topToBottomProb,
            double mixingTimeEstimate) {}

    // -----------------------------------------------------------------------
    // Matrix primitives
    // -----------------------------------------------------------------------

    static double[][] matScale(double[][] A, double c) {
        int n = A.length;
        double[][] R = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                R[i][j] = c * A[i][j];
        return R;
    }

    static double[][] matAdd(double[][] A, double[][] B) {
        int n = A.length;
        double[][] R = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                R[i][j] = A[i][j] + B[i][j];
        return R;
    }

    static double[][] matMul(double[][] A, double[][] B) {
        int n = A.length;
        double[][] C = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                for (int k = 0; k < n; k++)
                    C[i][j] += A[i][k] * B[k][j];
        return C;
    }

    static double[][] matIdentity(int n) {
        double[][] I = new double[n][n];
        for (int i = 0; i < n; i++) I[i][i] = 1.0;
        return I;
    }

    // -----------------------------------------------------------------------
    // Adjacency matrix (undirected Hasse diagram)
    // -----------------------------------------------------------------------

    /**
     * Build the adjacency matrix of the undirected Hasse diagram.
     * Uses covering relation from Zeta (direct covers, not transitive closure).
     */
    public static double[][] adjacencyMatrix(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        int n = states.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(states.get(i), i);

        // Use covering relation for Hasse edges
        List<Zeta.Pair> covers = Zeta.coveringRelation(ss);
        double[][] A = new double[n][n];
        for (Zeta.Pair p : covers) {
            Integer ui = idx.get(p.x());
            Integer vi = idx.get(p.y());
            if (ui != null && vi != null) {
                A[ui][vi] = 1.0;
                A[vi][ui] = 1.0;
            }
        }
        return A;
    }

    /**
     * Jacobi eigensolver for real symmetric matrices.
     */
    public static double[] symmetricEigenvalues(double[][] A) {
        int n = A.length;
        if (n == 0) return new double[0];
        if (n == 1) return new double[]{A[0][0]};

        double[][] M = new double[n][n];
        for (int i = 0; i < n; i++) System.arraycopy(A[i], 0, M[i], 0, n);

        for (int iter = 0; iter < 200; iter++) {
            int p = 0, q = 1;
            double best = -1.0;
            for (int i = 0; i < n; i++)
                for (int j = i + 1; j < n; j++) {
                    double a = Math.abs(M[i][j]);
                    if (a > best) { best = a; p = i; q = j; }
                }
            if (best < 1e-12) break;
            double app = M[p][p], aqq = M[q][q], apq = M[p][q];
            if (Math.abs(apq) < 1e-12) break;
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
        Arrays.sort(eigs);
        return eigs;
    }

    // -----------------------------------------------------------------------
    // Quantum walk evolution
    // -----------------------------------------------------------------------

    /**
     * Compute quantum walk transition probabilities at time t.
     *
     * <p>U(t) = e^{iAt} where A is the adjacency matrix.
     * Returns P[i][j] = |&lt;j|U(t)|i&gt;|^2 = transition probability.
     *
     * <p>Since A is real symmetric, U(t) is unitary.
     * We compute cos(At) and sin(At) via Taylor series, then
     * P[i][j] = cos^2(At)[i][j] + sin^2(At)[i][j].
     */
    public static double[][] quantumEvolution(StateSpace ss, double t) {
        double[][] A = adjacencyMatrix(ss);
        int n = A.length;
        if (n == 0) return new double[0][0];
        if (n == 1) return new double[][]{{1.0}};

        double[][] At = matScale(A, t);

        // cos(At) = I - (At)^2/2! + (At)^4/4! - ...
        // sin(At) = At - (At)^3/3! + (At)^5/5! - ...
        double[][] cosMat = matIdentity(n);
        double[][] sinMat = new double[n][n];
        double[][] power = matIdentity(n);
        double factorial = 1.0;

        for (int k = 1; k < 20; k++) {
            power = matMul(power, At);
            factorial *= k;
            switch (k % 4) {
                case 0 -> cosMat = matAdd(cosMat, matScale(power, 1.0 / factorial));
                case 1 -> sinMat = matAdd(sinMat, matScale(power, 1.0 / factorial));
                case 2 -> cosMat = matAdd(cosMat, matScale(power, -1.0 / factorial));
                case 3 -> sinMat = matAdd(sinMat, matScale(power, -1.0 / factorial));
            }
        }

        // P[i][j] = cos^2[i][j] + sin^2[i][j]
        double[][] P = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                P[i][j] = cosMat[i][j] * cosMat[i][j] + sinMat[i][j] * sinMat[i][j];
        return P;
    }

    /**
     * Probability of returning to the initial state (top) at time t.
     */
    public static double returnProbability(StateSpace ss, double t) {
        double[][] P = quantumEvolution(ss, t);
        List<Integer> states = Zeta.stateList(ss);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < states.size(); i++) idx.put(states.get(i), i);
        int topI = idx.get(ss.top());
        return P[topI][topI];
    }

    /**
     * Probability of reaching bottom from top at time t.
     */
    public static double topToBottomProbability(StateSpace ss, double t) {
        double[][] P = quantumEvolution(ss, t);
        List<Integer> states = Zeta.stateList(ss);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < states.size(); i++) idx.put(states.get(i), i);
        int topI = idx.get(ss.top());
        int botI = idx.get(ss.bottom());
        return P[topI][botI];
    }

    /**
     * Standard deviation of probability distribution at time t.
     * Measures how spread out the quantum walk is from starting position.
     */
    public static double quantumSpread(StateSpace ss, double t) {
        double[][] P = quantumEvolution(ss, t);
        List<Integer> states = Zeta.stateList(ss);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < states.size(); i++) idx.put(states.get(i), i);
        int topI = idx.get(ss.top());
        int n = states.size();

        double[] probs = new double[n];
        for (int j = 0; j < n; j++) probs[j] = P[topI][j];

        double meanPos = 0.0;
        for (int j = 0; j < n; j++) meanPos += j * probs[j];

        double variance = 0.0;
        for (int j = 0; j < n; j++) variance += (j - meanPos) * (j - meanPos) * probs[j];

        return Math.sqrt(Math.max(0.0, variance));
    }

    /**
     * Estimate quantum mixing time from spectral gap.
     *
     * <p>Quantum walks mix in O(1/Delta) time where Delta is the spectral gap.
     */
    public static double quantumMixingEstimate(StateSpace ss) {
        double[][] A = adjacencyMatrix(ss);
        double[] eigs = symmetricEigenvalues(A);
        int n = eigs.length;
        if (n < 2) return 0.0;
        // eigs sorted ascending; largest is eigs[n-1]
        double gap = (eigs[n - 1] != eigs[n - 2])
                ? eigs[n - 1] - eigs[n - 2]
                : 1.0;
        return 1.0 / Math.max(gap, 1e-10);
    }

    // -----------------------------------------------------------------------
    // Main analysis
    // -----------------------------------------------------------------------

    /**
     * Complete quantum walk analysis of a state space.
     */
    public static QuantumWalkResult analyzeQuantumWalk(StateSpace ss) {
        double[][] P = quantumEvolution(ss, 1.0);
        double ret = returnProbability(ss, 1.0);
        double spread = quantumSpread(ss, 1.0);
        double ttb = topToBottomProbability(ss, 1.0);
        double mixing = quantumMixingEstimate(ss);

        return new QuantumWalkResult(
                ss.states().size(),
                P,
                ret,
                spread,
                ttb,
                mixing);
    }
}
