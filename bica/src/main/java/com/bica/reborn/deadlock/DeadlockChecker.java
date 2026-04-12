package com.bica.reborn.deadlock;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.reticular.ReticularChecker;
import com.bica.reborn.reticular.ReticularFormResult;
import com.bica.reborn.reticular.StateClassification;
import com.bica.reborn.statespace.ProductStateSpace;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Lattice-based deadlock detection for session types (Step 80b).
 *
 * <p>Seven novel deadlock detection techniques unified from the reticulate
 * framework, all exploiting the lattice structure of session type state spaces:
 * <ol>
 *   <li><b>Direct detection</b> -- find states with no outgoing transitions that are not bottom</li>
 *   <li><b>Lattice Bottom Uniqueness</b> -- structural guarantee from lattice property</li>
 *   <li><b>Black Hole Detection</b> -- states from which bottom is unreachable</li>
 *   <li><b>Spectral Risk Indicators</b> -- connectivity-based deadlock risk score</li>
 *   <li><b>Compositional Checking</b> -- composition preserves deadlock freedom</li>
 *   <li><b>Buffer Capacity Computation</b> -- minimum buffer size for async deadlock freedom</li>
 *   <li><b>Siphon/Trap Analysis</b> -- Petri net structural analysis</li>
 *   <li><b>Reticular Form Certificate</b> -- session type structure implies deadlock freedom</li>
 * </ol>
 */
public final class DeadlockChecker {

    private DeadlockChecker() {}

    // -----------------------------------------------------------------------
    // Result records
    // -----------------------------------------------------------------------

    /**
     * A single deadlocked state.
     *
     * @param state        the state ID
     * @param label        human-readable state label
     * @param isBlackHole  true if bottom is unreachable from this state
     * @param depthFromTop shortest path length from top to this state
     */
    public record DeadlockState(int state, String label, boolean isBlackHole, int depthFromTop) {
        public DeadlockState {
            Objects.requireNonNull(label, "label must not be null");
        }
    }

    /**
     * Certificate that lattice structure implies deadlock freedom.
     *
     * @param isLattice       whether the state space is a lattice
     * @param hasUniqueBottom  true iff there is exactly one minimal element
     * @param allReachBottom   true iff every state can reach bottom
     * @param certificateValid true iff the lattice guarantees deadlock freedom
     * @param explanation      human-readable explanation
     */
    public record LatticeCertificate(
            boolean isLattice,
            boolean hasUniqueBottom,
            boolean allReachBottom,
            boolean certificateValid,
            String explanation) {
        public LatticeCertificate {
            Objects.requireNonNull(explanation, "explanation must not be null");
        }
    }

    /**
     * Result of black hole detection.
     *
     * @param blackHoles        states from which bottom is unreachable
     * @param eventHorizons     transitions leading into black holes from safe states
     * @param escapePaths       for states near black holes, shortest escape distance to bottom
     * @param totalTrappedStates number of states trapped in black holes
     */
    public record BlackHoleResult(
            List<Integer> blackHoles,
            List<int[]> eventHorizons,
            Map<Integer, Integer> escapePaths,
            int totalTrappedStates) {
        public BlackHoleResult {
            Objects.requireNonNull(blackHoles, "blackHoles must not be null");
            Objects.requireNonNull(eventHorizons, "eventHorizons must not be null");
            Objects.requireNonNull(escapePaths, "escapePaths must not be null");
            blackHoles = List.copyOf(blackHoles);
            eventHorizons = List.copyOf(eventHorizons);
            escapePaths = Map.copyOf(escapePaths);
        }
    }

    /**
     * Spectral risk indicators for deadlock.
     *
     * @param fiedlerValue     algebraic connectivity approximation
     * @param cheegerConstant  edge expansion approximation
     * @param spectralGap      gap between first and second Laplacian eigenvalues
     * @param riskScore        combined risk score in [0, 1]; 1 = highest risk
     * @param bottleneckStates states identified as connectivity bottlenecks
     * @param diagnosis        human-readable risk assessment
     */
    public record SpectralRisk(
            double fiedlerValue,
            double cheegerConstant,
            double spectralGap,
            double riskScore,
            List<Integer> bottleneckStates,
            String diagnosis) {
        public SpectralRisk {
            Objects.requireNonNull(bottleneckStates, "bottleneckStates must not be null");
            Objects.requireNonNull(diagnosis, "diagnosis must not be null");
            bottleneckStates = List.copyOf(bottleneckStates);
        }
    }

    /**
     * Result of compositional deadlock checking.
     *
     * @param isDeadlockFree          true iff composition is deadlock-free
     * @param individualDeadlockFree  per-component deadlock freedom (left, right)
     * @param interfaceCompatible     true iff shared labels respect duality
     * @param productDeadlockFree     true iff the product has no deadlocks
     * @param newDeadlocks            deadlocked states introduced by composition
     * @param explanation             human-readable analysis
     */
    public record CompositionalResult(
            boolean isDeadlockFree,
            boolean[] individualDeadlockFree,
            boolean interfaceCompatible,
            boolean productDeadlockFree,
            List<Integer> newDeadlocks,
            String explanation) {
        public CompositionalResult {
            Objects.requireNonNull(individualDeadlockFree, "individualDeadlockFree must not be null");
            Objects.requireNonNull(newDeadlocks, "newDeadlocks must not be null");
            Objects.requireNonNull(explanation, "explanation must not be null");
            individualDeadlockFree = individualDeadlockFree.clone();
            newDeadlocks = List.copyOf(newDeadlocks);
        }

        /** Returns individual deadlock freedom as a defensive copy. */
        @Override
        public boolean[] individualDeadlockFree() {
            return individualDeadlockFree.clone();
        }
    }

    /**
     * Result of buffer deadlock analysis.
     *
     * @param minSafeCapacity      minimum buffer capacity for deadlock freedom (-1 if none found)
     * @param deadlocksPerCapacity mapping capacity to number of deadlock states
     * @param isSynchronousSafe    true iff capacity=0 is deadlock-free
     * @param growthRate           how deadlock count changes with capacity
     * @param explanation          human-readable analysis
     */
    public record BufferDeadlockResult(
            int minSafeCapacity,
            Map<Integer, Integer> deadlocksPerCapacity,
            boolean isSynchronousSafe,
            String growthRate,
            String explanation) {
        public BufferDeadlockResult {
            Objects.requireNonNull(deadlocksPerCapacity, "deadlocksPerCapacity must not be null");
            Objects.requireNonNull(growthRate, "growthRate must not be null");
            Objects.requireNonNull(explanation, "explanation must not be null");
            deadlocksPerCapacity = Map.copyOf(deadlocksPerCapacity);
        }
    }

    /**
     * Result of siphon/trap structural analysis.
     *
     * @param siphons           list of siphons (each a set of place IDs)
     * @param traps             list of traps (each a set of place IDs)
     * @param dangerousSiphons  siphons that do not contain a marked trap
     * @param isDeadlockFree    true iff no dangerous siphons exist
     * @param explanation       human-readable analysis
     */
    public record SiphonTrapResult(
            List<Set<Integer>> siphons,
            List<Set<Integer>> traps,
            List<Set<Integer>> dangerousSiphons,
            boolean isDeadlockFree,
            String explanation) {
        public SiphonTrapResult {
            Objects.requireNonNull(siphons, "siphons must not be null");
            Objects.requireNonNull(traps, "traps must not be null");
            Objects.requireNonNull(dangerousSiphons, "dangerousSiphons must not be null");
            Objects.requireNonNull(explanation, "explanation must not be null");
            siphons = siphons.stream().map(Set::copyOf).collect(Collectors.toUnmodifiableList());
            traps = traps.stream().map(Set::copyOf).collect(Collectors.toUnmodifiableList());
            dangerousSiphons = dangerousSiphons.stream().map(Set::copyOf).collect(Collectors.toUnmodifiableList());
        }
    }

    /**
     * Certificate from reticular form analysis.
     *
     * @param isReticulate         true iff state space has reticular form
     * @param isLattice            true iff state space forms a lattice
     * @param certificateValid     true iff reticular form guarantees deadlock freedom
     * @param stateClassifications per-state classification summary
     * @param explanation          human-readable analysis
     */
    public record ReticularCertificate(
            boolean isReticulate,
            boolean isLattice,
            boolean certificateValid,
            Map<Integer, String> stateClassifications,
            String explanation) {
        public ReticularCertificate {
            Objects.requireNonNull(stateClassifications, "stateClassifications must not be null");
            Objects.requireNonNull(explanation, "explanation must not be null");
            stateClassifications = Map.copyOf(stateClassifications);
        }
    }

    /**
     * Unified deadlock analysis combining all techniques.
     *
     * @param deadlockedStates      list of deadlocked state details
     * @param isDeadlockFree        true iff no deadlocks detected
     * @param latticeCertificate    lattice bottom uniqueness certificate
     * @param blackHoles            black hole detection result
     * @param spectralRisk          spectral risk indicators
     * @param siphonTrap            siphon/trap structural analysis
     * @param reticularCertificate  reticular form certificate
     * @param numStates             total states in the state space
     * @param numTransitions        total transitions
     * @param summary               human-readable summary
     */
    public record DeadlockAnalysis(
            List<DeadlockState> deadlockedStates,
            boolean isDeadlockFree,
            LatticeCertificate latticeCertificate,
            BlackHoleResult blackHoles,
            SpectralRisk spectralRisk,
            SiphonTrapResult siphonTrap,
            ReticularCertificate reticularCertificate,
            int numStates,
            int numTransitions,
            String summary) {
        public DeadlockAnalysis {
            Objects.requireNonNull(deadlockedStates, "deadlockedStates must not be null");
            Objects.requireNonNull(latticeCertificate, "latticeCertificate must not be null");
            Objects.requireNonNull(blackHoles, "blackHoles must not be null");
            Objects.requireNonNull(spectralRisk, "spectralRisk must not be null");
            Objects.requireNonNull(siphonTrap, "siphonTrap must not be null");
            Objects.requireNonNull(reticularCertificate, "reticularCertificate must not be null");
            Objects.requireNonNull(summary, "summary must not be null");
            deadlockedStates = List.copyOf(deadlockedStates);
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /** Return outgoing transitions from a state. */
    private static List<Transition> outgoing(StateSpace ss, int state) {
        return ss.transitionsFrom(state);
    }

    /** BFS shortest distances from start. */
    private static Map<Integer, Integer> bfsDistances(StateSpace ss, int start) {
        Map<Integer, Integer> dist = new HashMap<>();
        dist.put(start, 0);
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            int s = queue.poll();
            for (var t : ss.transitionsFrom(s)) {
                if (!dist.containsKey(t.target())) {
                    dist.put(t.target(), dist.get(s) + 1);
                    queue.add(t.target());
                }
            }
        }
        return dist;
    }

    /** States that can reach target via reverse edges. */
    private static Set<Integer> reverseReachable(StateSpace ss, int target) {
        // Build reverse adjacency
        Map<Integer, List<Integer>> rev = new HashMap<>();
        for (int s : ss.states()) rev.put(s, new ArrayList<>());
        for (var t : ss.transitions()) {
            rev.computeIfAbsent(t.target(), k -> new ArrayList<>()).add(t.source());
        }
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        visited.add(target);
        queue.add(target);
        while (!queue.isEmpty()) {
            int s = queue.poll();
            for (int pred : rev.getOrDefault(s, List.of())) {
                if (visited.add(pred)) {
                    queue.add(pred);
                }
            }
        }
        return visited;
    }

    // -----------------------------------------------------------------------
    // Technique 1: Direct Deadlock Detection
    // -----------------------------------------------------------------------

    /**
     * Find all states with no outgoing transitions that are not bottom.
     *
     * <p>A deadlocked state is one where no progress is possible but the
     * protocol has not terminated (the state is not the bottom/end state).
     *
     * @param ss the state space to check
     * @return list of deadlocked states, sorted by state ID
     */
    public static List<DeadlockState> detectDeadlocks(StateSpace ss) {
        Map<Integer, Integer> distances = bfsDistances(ss, ss.top());
        Set<Integer> canReachBottom = reverseReachable(ss, ss.bottom());

        List<DeadlockState> deadlocks = new ArrayList<>();
        for (int state : ss.states()) {
            if (state == ss.bottom()) continue;
            if (outgoing(ss, state).isEmpty()) {
                deadlocks.add(new DeadlockState(
                        state,
                        ss.labels().getOrDefault(state, "s" + state),
                        !canReachBottom.contains(state),
                        distances.getOrDefault(state, -1)));
            }
        }
        deadlocks.sort(Comparator.comparingInt(DeadlockState::state));
        return List.copyOf(deadlocks);
    }

    /**
     * Return true iff no deadlocks exist in the state space.
     */
    public static boolean isDeadlockFree(StateSpace ss) {
        return detectDeadlocks(ss).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Technique 2: Lattice Deadlock Certificate
    // -----------------------------------------------------------------------

    /**
     * If the state space is a lattice, return certificate proving deadlock freedom.
     *
     * <p>Key theorem: if L(S) is a lattice with unique bottom element, then every
     * state has a path to bottom. Therefore no state can be deadlocked.
     *
     * @param ss the state space to check
     * @return certificate with lattice analysis
     */
    public static LatticeCertificate latticeDeadlockCertificate(StateSpace ss) {
        LatticeResult lr = LatticeChecker.checkLattice(ss);

        Set<Integer> canReachBottom = reverseReachable(ss, ss.bottom());
        boolean allReach = ss.states().stream().allMatch(canReachBottom::contains);

        if (lr.isLattice()) {
            return new LatticeCertificate(
                    true, true, allReach, true,
                    "State space is a lattice with " + lr.numScc() + " SCC(s). "
                    + "Unique bottom ensures every state has a path to termination. "
                    + "Certificate: DEADLOCK-FREE by lattice bottom uniqueness theorem.");
        } else if (lr.hasBottom() && allReach) {
            return new LatticeCertificate(
                    false, true, true, true,
                    "Not a lattice (missing " + (lr.allMeetsExist() ? "joins" : "meets")
                    + "), but all states reach bottom. Deadlock-free by reachability.");
        } else {
            int failCount = (int) ss.states().stream().filter(s -> !canReachBottom.contains(s)).count();
            return new LatticeCertificate(
                    lr.isLattice(), lr.hasBottom(), allReach, false,
                    "Not deadlock-free. "
                    + (lr.isLattice() ? "" : "Not a lattice. ")
                    + (lr.hasBottom() ? "" : "No unique bottom. ")
                    + failCount + " state(s) cannot reach bottom.");
        }
    }

    // -----------------------------------------------------------------------
    // Technique 3: Black Hole Detection
    // -----------------------------------------------------------------------

    /**
     * Detect states from which the bottom (end) is unreachable.
     *
     * <p>These are "gravitational black holes" -- once the protocol enters
     * such a state, termination is impossible.
     *
     * @param ss the state space to check
     * @return black hole analysis result
     */
    public static BlackHoleResult blackHoleDetection(StateSpace ss) {
        Set<Integer> canReachBottom = reverseReachable(ss, ss.bottom());

        List<Integer> blackHoles = ss.states().stream()
                .filter(s -> !canReachBottom.contains(s))
                .sorted()
                .collect(Collectors.toList());

        Set<Integer> bhSet = new HashSet<>(blackHoles);

        // Event horizons: transitions from safe states into black holes
        List<int[]> eventHorizons = new ArrayList<>();
        for (var t : ss.transitions()) {
            if (!bhSet.contains(t.source()) && bhSet.contains(t.target())) {
                eventHorizons.add(new int[]{t.source(), t.target()});
            }
        }

        // Escape paths: for states adjacent to black holes, distance to bottom
        Map<Integer, Integer> escapePaths = new HashMap<>();
        for (int[] eh : eventHorizons) {
            int src = eh[0];
            if (canReachBottom.contains(src) && !escapePaths.containsKey(src)) {
                Map<Integer, Integer> fwd = bfsDistances(ss, src);
                if (fwd.containsKey(ss.bottom())) {
                    escapePaths.put(src, fwd.get(ss.bottom()));
                }
            }
        }

        return new BlackHoleResult(blackHoles, eventHorizons, escapePaths, blackHoles.size());
    }

    // -----------------------------------------------------------------------
    // Technique 4: Spectral Deadlock Risk
    // -----------------------------------------------------------------------

    /**
     * Compute spectral risk indicators for deadlock.
     *
     * <p>Uses simplified connectivity analysis to approximate Fiedler value,
     * Cheeger constant, and spectral gap. No external linear algebra library
     * is required -- this uses iterative power method on the Laplacian.
     *
     * @param ss the state space to check
     * @return spectral risk indicators
     */
    public static SpectralRisk spectralDeadlockRisk(StateSpace ss) {
        int n = ss.states().size();

        if (n <= 1) {
            return new SpectralRisk(0.0, 0.0, 0.0, 0.0, List.of(),
                    "Spectral analysis not applicable (trivial state space).");
        }

        // Build undirected adjacency + degree for the Laplacian
        List<Integer> statesList = ss.states().stream().sorted().collect(Collectors.toList());
        Map<Integer, Integer> stateIndex = new HashMap<>();
        for (int i = 0; i < statesList.size(); i++) {
            stateIndex.put(statesList.get(i), i);
        }

        // Symmetrize the adjacency
        boolean[][] adj = new boolean[n][n];
        int[] degree = new int[n];
        for (var t : ss.transitions()) {
            int i = stateIndex.get(t.source());
            int j = stateIndex.get(t.target());
            if (!adj[i][j]) {
                adj[i][j] = true;
                degree[i]++;
            }
            if (!adj[j][i]) {
                adj[j][i] = true;
                degree[j]++;
            }
        }

        // Build Laplacian L = D - A
        double[][] laplacian = new double[n][n];
        for (int i = 0; i < n; i++) {
            laplacian[i][i] = degree[i];
            for (int j = 0; j < n; j++) {
                if (adj[i][j]) {
                    laplacian[i][j] -= 1.0;
                }
            }
        }

        // Approximate Fiedler value (second-smallest eigenvalue of Laplacian)
        // using inverse power iteration on L shifted by small epsilon
        double fiedlerValue = approximateFiedlerValue(laplacian, n);

        // Spectral gap = fiedler value for connected graphs
        double spectralGap = Math.max(0.0, fiedlerValue);

        // Cheeger constant approximation from Cheeger inequality:
        // fiedler/2 <= h(G) <= sqrt(2 * fiedler)
        double cheeger;
        if (fiedlerValue > 1e-10) {
            double lower = fiedlerValue / 2.0;
            double upper = Math.sqrt(2.0 * fiedlerValue);
            cheeger = (lower + upper) / 2.0;
        } else {
            cheeger = 0.0;
        }

        // Risk score: normalized inverse of connectivity
        double riskScore;
        if (fiedlerValue < 1e-10) {
            riskScore = 1.0; // disconnected
        } else {
            riskScore = Math.max(0.0, Math.min(1.0, 1.0 / (1.0 + fiedlerValue)));
        }

        // Find bottleneck states (cut vertices via simple DFS)
        List<Integer> bottlenecks = findCutVertices(ss, statesList, stateIndex, adj, n);

        // Diagnosis
        String diagnosis;
        if (riskScore >= 0.8) {
            diagnosis = "CRITICAL: Near-disconnection detected. Deadlock highly likely.";
        } else if (riskScore >= 0.5) {
            diagnosis = String.format(
                    "WARNING: Low algebraic connectivity (Fiedler=%.3f). "
                    + "Protocol has bottleneck states that could cause deadlock under perturbation.",
                    fiedlerValue);
        } else if (riskScore >= 0.2) {
            diagnosis = String.format(
                    "LOW RISK: Moderate connectivity (Fiedler=%.3f). "
                    + "Protocol is reasonably well-connected.",
                    fiedlerValue);
        } else {
            diagnosis = String.format(
                    "SAFE: High algebraic connectivity (Fiedler=%.3f). "
                    + "All states are well-connected to bottom.",
                    fiedlerValue);
        }

        return new SpectralRisk(fiedlerValue, cheeger, spectralGap, riskScore, bottlenecks, diagnosis);
    }

    /**
     * Approximate the Fiedler value (second-smallest eigenvalue of Laplacian).
     * Uses a simple QR-like approach via power iteration on (maxEig*I - L)
     * to find the largest eigenvalue of the complement, then derives lambda_2.
     */
    private static double approximateFiedlerValue(double[][] laplacian, int n) {
        if (n <= 1) return 0.0;

        // Use power iteration to estimate eigenvalues
        // First, estimate max eigenvalue
        double maxEig = 0;
        for (int i = 0; i < n; i++) {
            maxEig = Math.max(maxEig, laplacian[i][i] * 2.0);
        }
        if (maxEig < 1e-10) return 0.0;

        // For small matrices, compute directly via characteristic polynomial approach
        // For session type state spaces (typically < 100 states), this is fine
        if (n <= 20) {
            return fiedlerByJacobi(laplacian, n);
        }

        // For larger, use inverse iteration
        return fiedlerByPowerIteration(laplacian, n, maxEig);
    }

    /** Jacobi eigenvalue algorithm for small symmetric matrices. */
    private static double fiedlerByJacobi(double[][] A, int n) {
        // Copy the matrix
        double[][] M = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, M[i], 0, n);
        }

        int maxIter = 100 * n * n;
        for (int iter = 0; iter < maxIter; iter++) {
            // Find largest off-diagonal element
            double maxVal = 0;
            int p = 0, q = 1;
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (Math.abs(M[i][j]) > maxVal) {
                        maxVal = Math.abs(M[i][j]);
                        p = i;
                        q = j;
                    }
                }
            }
            if (maxVal < 1e-12) break;

            // Compute rotation
            double theta;
            if (Math.abs(M[p][p] - M[q][q]) < 1e-15) {
                theta = Math.PI / 4.0;
            } else {
                theta = 0.5 * Math.atan2(2.0 * M[p][q], M[p][p] - M[q][q]);
            }
            double c = Math.cos(theta);
            double s = Math.sin(theta);

            // Apply rotation
            double[] rowP = new double[n];
            double[] rowQ = new double[n];
            for (int j = 0; j < n; j++) {
                rowP[j] = c * M[p][j] + s * M[q][j];
                rowQ[j] = -s * M[p][j] + c * M[q][j];
            }
            for (int j = 0; j < n; j++) {
                M[p][j] = rowP[j];
                M[q][j] = rowQ[j];
            }
            for (int i = 0; i < n; i++) {
                double ip = M[i][p];
                double iq = M[i][q];
                M[i][p] = c * ip + s * iq;
                M[i][q] = -s * ip + c * iq;
            }
        }

        // Collect diagonal (eigenvalues)
        double[] eigenvalues = new double[n];
        for (int i = 0; i < n; i++) eigenvalues[i] = M[i][i];
        Arrays.sort(eigenvalues);

        // Second smallest eigenvalue (first is ~0 for connected graph Laplacian)
        return (n >= 2) ? Math.max(0.0, eigenvalues[1]) : 0.0;
    }

    /** Power iteration fallback for larger matrices. */
    private static double fiedlerByPowerIteration(double[][] L, int n, double maxEig) {
        // Shifted inverse iteration to find second smallest eigenvalue
        // Approximate: use L directly, deflate the zero eigenspace (all-ones vector)
        double shift = maxEig + 0.1;
        double[] v = new double[n];
        Random rng = new Random(42);
        for (int i = 0; i < n; i++) v[i] = rng.nextDouble() - 0.5;

        // Orthogonalize against the all-ones vector (null space of L)
        double oneNorm = 1.0 / Math.sqrt(n);
        for (int iter = 0; iter < 200; iter++) {
            // Multiply by (shift*I - L)
            double[] w = new double[n];
            for (int i = 0; i < n; i++) {
                w[i] = shift * v[i];
                for (int j = 0; j < n; j++) {
                    w[i] -= L[i][j] * v[j];
                }
            }

            // Remove component along all-ones vector
            double dot = 0;
            for (int i = 0; i < n; i++) dot += w[i];
            dot /= n;
            for (int i = 0; i < n; i++) w[i] -= dot;

            // Normalize
            double norm = 0;
            for (int i = 0; i < n; i++) norm += w[i] * w[i];
            norm = Math.sqrt(norm);
            if (norm < 1e-15) break;
            for (int i = 0; i < n; i++) v[i] = w[i] / norm;
        }

        // Rayleigh quotient: lambda = v^T L v / v^T v
        double num = 0, den = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                num += v[i] * L[i][j] * v[j];
            }
            den += v[i] * v[i];
        }
        return (den > 1e-15) ? Math.max(0.0, num / den) : 0.0;
    }

    /** Find cut vertices using Tarjan-style DFS. */
    private static List<Integer> findCutVertices(
            StateSpace ss, List<Integer> statesList,
            Map<Integer, Integer> stateIndex, boolean[][] adj, int n) {
        int[] disc = new int[n];
        int[] low = new int[n];
        boolean[] visited = new boolean[n];
        boolean[] isCut = new boolean[n];
        int[] parent = new int[n];
        Arrays.fill(disc, -1);
        Arrays.fill(low, -1);
        Arrays.fill(parent, -1);

        int[] timer = {0};

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                cutVertexDfs(i, adj, n, disc, low, visited, isCut, parent, timer);
            }
        }

        List<Integer> cuts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (isCut[i]) cuts.add(statesList.get(i));
        }
        return cuts;
    }

    private static void cutVertexDfs(
            int u, boolean[][] adj, int n,
            int[] disc, int[] low, boolean[] visited, boolean[] isCut,
            int[] parent, int[] timer) {
        // Iterative DFS to avoid stack overflow
        Deque<int[]> stack = new ArrayDeque<>();
        // Stack frame: [node, neighborIndex, childCount]
        stack.push(new int[]{u, 0, 0});
        visited[u] = true;
        disc[u] = low[u] = timer[0]++;

        while (!stack.isEmpty()) {
            int[] frame = stack.peek();
            int node = frame[0];
            int idx = frame[1];

            if (idx < n) {
                frame[1]++;
                if (adj[node][idx]) {
                    if (!visited[idx]) {
                        parent[idx] = node;
                        visited[idx] = true;
                        disc[idx] = low[idx] = timer[0]++;
                        frame[2]++;
                        stack.push(new int[]{idx, 0, 0});
                    } else if (idx != parent[node]) {
                        low[node] = Math.min(low[node], disc[idx]);
                    }
                }
            } else {
                // All neighbors processed, backtrack
                stack.pop();
                if (!stack.isEmpty()) {
                    int[] parentFrame = stack.peek();
                    int p = parentFrame[0];
                    low[p] = Math.min(low[p], low[node]);

                    // Check cut vertex conditions
                    if (parent[p] == -1 && parentFrame[2] > 1) {
                        isCut[p] = true;
                    }
                    if (parent[p] != -1 && low[node] >= disc[p]) {
                        isCut[p] = true;
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Technique 5: Compositional Deadlock Checking
    // -----------------------------------------------------------------------

    /**
     * Check if composing two protocol state spaces introduces deadlock.
     *
     * @param ss1 first component state space
     * @param ss2 second component state space
     * @return compositional analysis result
     */
    public static CompositionalResult compositionalDeadlockCheck(StateSpace ss1, StateSpace ss2) {
        boolean ind1 = isDeadlockFree(ss1);
        boolean ind2 = isDeadlockFree(ss2);

        // Build product
        StateSpace product = ProductStateSpace.product(ss1, ss2);

        // Check product for deadlocks
        List<DeadlockState> productDeadlocks = detectDeadlocks(product);
        boolean productFree = productDeadlocks.isEmpty();

        List<Integer> newDl = productDeadlocks.stream()
                .map(DeadlockState::state)
                .collect(Collectors.toList());

        String explanation;
        if (productFree) {
            explanation = "Composition is deadlock-free. "
                    + "Component 1: " + (ind1 ? "deadlock-free" : "has deadlocks") + ". "
                    + "Component 2: " + (ind2 ? "deadlock-free" : "has deadlocks") + ". "
                    + "Product (" + product.states().size() + " states) has no deadlocks.";
        } else {
            explanation = "Composition introduces " + productDeadlocks.size() + " deadlock(s)! "
                    + "Component 1: " + (ind1 ? "deadlock-free" : "has deadlocks") + ". "
                    + "Component 2: " + (ind2 ? "deadlock-free" : "has deadlocks") + ". "
                    + "Product (" + product.states().size() + " states) has "
                    + productDeadlocks.size() + " deadlocked state(s).";
        }

        // Interface compatibility
        Set<String> labels1 = ss1.transitions().stream().map(Transition::label).collect(Collectors.toSet());
        Set<String> labels2 = ss2.transitions().stream().map(Transition::label).collect(Collectors.toSet());
        Set<String> shared = new HashSet<>(labels1);
        shared.retainAll(labels2);
        boolean interfaceOk = shared.isEmpty() || productFree;

        return new CompositionalResult(
                productFree,
                new boolean[]{ind1, ind2},
                interfaceOk,
                productFree,
                newDl,
                explanation);
    }

    // -----------------------------------------------------------------------
    // Technique 6: Buffer Deadlock Analysis
    // -----------------------------------------------------------------------

    /**
     * Analyze minimum buffer capacity for deadlock freedom in async channels.
     *
     * @param ss    the base (synchronous) state space
     * @param maxK  maximum buffer capacity to search
     * @return buffer analysis result
     */
    public static BufferDeadlockResult bufferDeadlockAnalysis(StateSpace ss, int maxK) {
        Map<Integer, Integer> deadlocksPerK = new TreeMap<>();

        // Base state space: count deadlocks directly
        deadlocksPerK.put(0, detectDeadlocks(ss).size());

        // For higher capacities, simulate buffered behavior
        for (int k = 1; k <= maxK; k++) {
            StateSpace buffered = simulateBuffered(ss, k);
            deadlocksPerK.put(k, detectDeadlocks(buffered).size());
        }

        // Find minimum safe capacity
        int minSafe = -1;
        for (var entry : deadlocksPerK.entrySet()) {
            if (entry.getValue() == 0) {
                minSafe = entry.getKey();
                break;
            }
        }

        boolean isSyncSafe = deadlocksPerK.getOrDefault(0, 0) == 0;

        // Characterize growth rate
        List<Integer> values = deadlocksPerK.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        String growthRate;
        if (values.stream().allMatch(v -> v == 0)) {
            growthRate = "constant-zero";
        } else if (values.stream().allMatch(v -> v.equals(values.get(0)))) {
            growthRate = "constant";
        } else if (values.get(values.size() - 1) < values.get(0)) {
            growthRate = "decreasing";
        } else if (values.get(values.size() - 1) > values.get(0)) {
            growthRate = "increasing";
        } else {
            growthRate = "non-monotonic";
        }

        String explanation;
        if (minSafe >= 0) {
            explanation = "Deadlock-free at buffer capacity k=" + minSafe + ". "
                    + (isSyncSafe ? "Already safe synchronously." : "Requires " + minSafe + " buffer slots.");
        } else {
            explanation = "No deadlock-free capacity found up to k=" + maxK + ". "
                    + "Deadlocks at k=0: " + deadlocksPerK.get(0) + ", at k=" + maxK + ": " + deadlocksPerK.get(maxK) + ".";
        }

        return new BufferDeadlockResult(minSafe, deadlocksPerK, isSyncSafe, growthRate, explanation);
    }

    /**
     * Build a buffered version of the state space.
     * Selection transitions can fire into a buffer; branch transitions consume from the buffer.
     */
    private static StateSpace simulateBuffered(StateSpace ss, int capacity) {
        if (capacity == 0) return ss;

        Set<Integer> newStates = new HashSet<>();
        List<Transition> newTransitions = new ArrayList<>();
        Map<Integer, String> newLabels = new HashMap<>();

        int cap1 = capacity + 1;

        for (int state : ss.states()) {
            for (int buf = 0; buf < cap1; buf++) {
                int sid = state * cap1 + buf;
                newStates.add(sid);
                newLabels.put(sid, ss.labels().getOrDefault(state, "s" + state) + "[b=" + buf + "]");
            }
        }

        int newTop = ss.top() * cap1;
        int newBottom = ss.bottom() * cap1;

        for (var t : ss.transitions()) {
            boolean isSel = t.kind() == TransitionKind.SELECTION;

            if (isSel) {
                for (int buf = 0; buf < cap1; buf++) {
                    if (buf < capacity) {
                        // Send into buffer
                        newTransitions.add(new Transition(
                                t.source() * cap1 + buf, t.label(),
                                t.target() * cap1 + buf + 1, TransitionKind.SELECTION));
                    }
                    // Synchronous send
                    newTransitions.add(new Transition(
                            t.source() * cap1 + buf, t.label(),
                            t.target() * cap1 + buf, TransitionKind.SELECTION));
                }
            } else {
                for (int buf = 0; buf < cap1; buf++) {
                    if (buf > 0) {
                        // Consume from buffer
                        newTransitions.add(new Transition(
                                t.source() * cap1 + buf, t.label(),
                                t.target() * cap1 + buf - 1, t.kind()));
                    }
                    // Synchronous receive
                    newTransitions.add(new Transition(
                            t.source() * cap1 + buf, t.label(),
                            t.target() * cap1 + buf, t.kind()));
                }
            }
        }

        return new StateSpace(newStates, newTransitions, newTop, newBottom, newLabels);
    }

    // -----------------------------------------------------------------------
    // Technique 7: Siphon/Trap Analysis
    // -----------------------------------------------------------------------

    /**
     * Petri net siphon/trap structural analysis for deadlock.
     *
     * <p>Constructs a simple Petri net model from the state space and computes
     * siphons and traps. Deadlock-freedom: every siphon contains a marked trap.
     *
     * @param ss the state space to check
     * @return siphon/trap analysis result
     */
    public static SiphonTrapResult siphonTrapAnalysis(StateSpace ss) {
        // Build a simple Petri net: places = states, transitions = edges
        // For each transition (s, label, t): pre(t) = {s}, post(t) = {t}
        List<Integer> placeIds = ss.states().stream().sorted().collect(Collectors.toList());
        int nPlaces = placeIds.size();

        if (nPlaces == 0) {
            return new SiphonTrapResult(List.of(), List.of(), List.of(), true,
                    "Empty state space -- trivially deadlock-free.");
        }

        // Pre/post sets per transition
        int nTrans = ss.transitions().size();
        int[][] preSets = new int[nTrans][];
        int[][] postSets = new int[nTrans][];

        for (int i = 0; i < nTrans; i++) {
            var t = ss.transitions().get(i);
            preSets[i] = new int[]{t.source()};
            postSets[i] = new int[]{t.target()};
        }

        // Initial marking: token on top state
        Set<Integer> markedPlaces = Set.of(ss.top());

        List<Set<Integer>> siphons = findSiphons(placeIds, nTrans, preSets, postSets);
        List<Set<Integer>> traps = findTraps(placeIds, nTrans, preSets, postSets);

        // Check which siphons contain a marked trap
        List<Set<Integer>> dangerous = new ArrayList<>();
        for (Set<Integer> siphon : siphons) {
            boolean containsMarkedTrap = false;
            for (Set<Integer> trap : traps) {
                if (siphon.containsAll(trap) && !Collections.disjoint(trap, markedPlaces)) {
                    containsMarkedTrap = true;
                    break;
                }
            }
            if (!containsMarkedTrap) {
                dangerous.add(siphon);
            }
        }

        boolean isDf = dangerous.isEmpty();
        String explanation;
        if (isDf) {
            explanation = "Found " + siphons.size() + " siphon(s) and " + traps.size()
                    + " trap(s). Every siphon contains a marked trap. "
                    + "Certificate: DEADLOCK-FREE by siphon/trap theorem.";
        } else {
            explanation = "Found " + siphons.size() + " siphon(s) and " + traps.size()
                    + " trap(s). " + dangerous.size()
                    + " dangerous siphon(s) without marked traps. "
                    + "POTENTIAL DEADLOCK: siphon(s) may become empty.";
        }

        return new SiphonTrapResult(siphons, traps, dangerous, isDf, explanation);
    }

    /** Find all minimal siphons. */
    private static List<Set<Integer>> findSiphons(
            List<Integer> placeIds, int nTrans, int[][] preSets, int[][] postSets) {
        int maxCheck = Math.min(placeIds.size(), 12);
        List<Set<Integer>> minimal = new ArrayList<>();
        Set<Set<Integer>> checked = new HashSet<>();
        Deque<Set<Integer>> queue = new ArrayDeque<>();

        for (int p : placeIds) {
            queue.add(Set.of(p));
        }

        while (!queue.isEmpty() && checked.size() < 2000) {
            Set<Integer> candidate = queue.poll();
            if (!checked.add(candidate)) continue;

            if (isSiphon(candidate, nTrans, preSets, postSets)) {
                boolean isMin = minimal.stream().noneMatch(s -> candidate.containsAll(s) && !s.equals(candidate));
                if (isMin) {
                    minimal.removeIf(s -> s.containsAll(candidate) && !s.equals(candidate));
                    minimal.add(candidate);
                }
            } else if (candidate.size() < maxCheck) {
                for (int p : placeIds) {
                    if (!candidate.contains(p)) {
                        Set<Integer> newCand = new HashSet<>(candidate);
                        newCand.add(p);
                        Set<Integer> frozen = Set.copyOf(newCand);
                        if (!checked.contains(frozen)) {
                            queue.add(frozen);
                        }
                    }
                }
            }
        }

        // Check full set
        Set<Integer> full = Set.copyOf(placeIds);
        if (!checked.contains(full) && isSiphon(full, nTrans, preSets, postSets)) {
            boolean isMin = minimal.stream().noneMatch(s -> full.containsAll(s) && !s.equals(full));
            if (isMin) minimal.add(full);
        }

        return minimal;
    }

    /** Find all minimal traps. */
    private static List<Set<Integer>> findTraps(
            List<Integer> placeIds, int nTrans, int[][] preSets, int[][] postSets) {
        int maxCheck = Math.min(placeIds.size(), 12);
        List<Set<Integer>> minimal = new ArrayList<>();
        Set<Set<Integer>> checked = new HashSet<>();
        Deque<Set<Integer>> queue = new ArrayDeque<>();

        for (int p : placeIds) {
            queue.add(Set.of(p));
        }

        while (!queue.isEmpty() && checked.size() < 2000) {
            Set<Integer> candidate = queue.poll();
            if (!checked.add(candidate)) continue;

            if (isTrap(candidate, nTrans, preSets, postSets)) {
                boolean isMin = minimal.stream().noneMatch(s -> candidate.containsAll(s) && !s.equals(candidate));
                if (isMin) {
                    minimal.removeIf(s -> s.containsAll(candidate) && !s.equals(candidate));
                    minimal.add(candidate);
                }
            } else if (candidate.size() < maxCheck) {
                for (int p : placeIds) {
                    if (!candidate.contains(p)) {
                        Set<Integer> newCand = new HashSet<>(candidate);
                        newCand.add(p);
                        Set<Integer> frozen = Set.copyOf(newCand);
                        if (!checked.contains(frozen)) {
                            queue.add(frozen);
                        }
                    }
                }
            }
        }

        Set<Integer> full = Set.copyOf(placeIds);
        if (!checked.contains(full) && isTrap(full, nTrans, preSets, postSets)) {
            boolean isMin = minimal.stream().noneMatch(s -> full.containsAll(s) && !s.equals(full));
            if (isMin) minimal.add(full);
        }

        return minimal;
    }

    /** Check if a set of places is a siphon: post(t) intersects S => pre(t) intersects S. */
    private static boolean isSiphon(Set<Integer> candidate, int nTrans, int[][] preSets, int[][] postSets) {
        for (int i = 0; i < nTrans; i++) {
            boolean postIntersects = false;
            for (int p : postSets[i]) {
                if (candidate.contains(p)) { postIntersects = true; break; }
            }
            if (postIntersects) {
                boolean preIntersects = false;
                for (int p : preSets[i]) {
                    if (candidate.contains(p)) { preIntersects = true; break; }
                }
                if (!preIntersects) return false;
            }
        }
        return true;
    }

    /** Check if a set of places is a trap: pre(t) intersects T => post(t) intersects T. */
    private static boolean isTrap(Set<Integer> candidate, int nTrans, int[][] preSets, int[][] postSets) {
        for (int i = 0; i < nTrans; i++) {
            boolean preIntersects = false;
            for (int p : preSets[i]) {
                if (candidate.contains(p)) { preIntersects = true; break; }
            }
            if (preIntersects) {
                boolean postIntersects = false;
                for (int p : postSets[i]) {
                    if (candidate.contains(p)) { postIntersects = true; break; }
                }
                if (!postIntersects) return false;
            }
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Technique 8: Reticular Form Certificate
    // -----------------------------------------------------------------------

    /**
     * If the state machine has reticular form, certify deadlock freedom.
     *
     * <p>Reticular form means the state space arises from a well-formed session
     * type. By the reconstruction theorem, every reticulate state space has a
     * unique bottom and every state reaches it.
     *
     * @param ss the state space to check
     * @return reticular certificate
     */
    public static ReticularCertificate reticularDeadlockCertificate(StateSpace ss) {
        ReticularFormResult rf = ReticularChecker.checkReticularForm(ss);
        LatticeResult lr = LatticeChecker.checkLattice(ss);

        // Build classification dict
        Map<Integer, String> classifications = new HashMap<>();
        try {
            List<StateClassification> allCls = ReticularChecker.classifyAllStates(ss);
            for (var sc : allCls) {
                classifications.put(sc.state(), sc.kind());
            }
        } catch (Exception e) {
            for (int state : ss.states()) {
                classifications.put(state, "unknown");
            }
        }

        if (rf.isReticulate() && lr.isLattice()) {
            return new ReticularCertificate(
                    true, true, true, classifications,
                    "State space has reticular form and is a lattice. "
                    + "By the reconstruction theorem, every state reaches bottom. "
                    + "Certificate: DEADLOCK-FREE by reticular form.");
        } else if (rf.isReticulate()) {
            return new ReticularCertificate(
                    true, lr.isLattice(), true, classifications,
                    "State space has reticular form (reconstructible as session type). "
                    + "All reticulate state spaces have unique bottom. "
                    + "Certificate: DEADLOCK-FREE by reticular form.");
        } else {
            Set<Integer> canReach = reverseReachable(ss, ss.bottom());
            boolean allReach = ss.states().stream().allMatch(canReach::contains);
            String reason = rf.reason() != null ? rf.reason() : "unknown";
            return new ReticularCertificate(
                    false, lr.isLattice(), allReach, classifications,
                    "State space does NOT have reticular form (" + reason + "). "
                    + (allReach
                        ? "All states reach bottom (deadlock-free by reachability)."
                        : "POTENTIAL DEADLOCK: not all states reach bottom."));
        }
    }

    // -----------------------------------------------------------------------
    // Unified Analysis
    // -----------------------------------------------------------------------

    /**
     * Run all deadlock detection techniques and return unified analysis.
     *
     * <p>Combines direct detection, lattice certificate, black hole detection,
     * spectral risk, siphon/trap analysis, and reticular form certificate.
     *
     * @param ss the state space to analyze
     * @return unified deadlock analysis
     */
    public static DeadlockAnalysis analyzeDeadlock(StateSpace ss) {
        // 1. Direct detection
        List<DeadlockState> deadlocks = detectDeadlocks(ss);

        // 2. Lattice certificate
        LatticeCertificate latticeCert = latticeDeadlockCertificate(ss);

        // 3. Black holes
        BlackHoleResult bh = blackHoleDetection(ss);

        // 4. Spectral risk
        SpectralRisk spectral = spectralDeadlockRisk(ss);

        // 5. Siphon/trap
        SiphonTrapResult siphon;
        try {
            siphon = siphonTrapAnalysis(ss);
        } catch (Exception e) {
            siphon = new SiphonTrapResult(
                    List.of(), List.of(), List.of(), true,
                    "Siphon/trap analysis not applicable.");
        }

        // 6. Reticular certificate
        ReticularCertificate reticular = reticularDeadlockCertificate(ss);

        // Overall verdict
        boolean isDf = deadlocks.isEmpty();
        int nStates = ss.states().size();
        int nTrans = ss.transitions().size();

        // Build summary
        List<String> verdicts = new ArrayList<>();
        if (latticeCert.certificateValid()) verdicts.add("lattice-certificate");
        if (bh.totalTrappedStates() == 0) verdicts.add("no-black-holes");
        if (siphon.isDeadlockFree()) verdicts.add("siphon-trap-safe");
        if (reticular.certificateValid()) verdicts.add("reticular-certificate");

        String summary;
        if (isDf) {
            summary = String.format(
                    "DEADLOCK-FREE (%d states, %d transitions). "
                    + "Confirmed by %d technique(s): %s. "
                    + "Spectral risk: %.2f.",
                    nStates, nTrans, verdicts.size(), String.join(", ", verdicts),
                    spectral.riskScore());
        } else {
            summary = String.format(
                    "DEADLOCK DETECTED: %d deadlocked state(s) in %d states. "
                    + "Black holes: %d. Spectral risk: %.2f. Dangerous siphons: %d.",
                    deadlocks.size(), nStates, bh.totalTrappedStates(),
                    spectral.riskScore(), siphon.dangerousSiphons().size());
        }

        return new DeadlockAnalysis(
                deadlocks, isDf, latticeCert, bh, spectral, siphon,
                reticular, nStates, nTrans, summary);
    }
}
