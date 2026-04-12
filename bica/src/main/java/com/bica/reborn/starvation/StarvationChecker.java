package com.bica.reborn.starvation;

import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.TransitionKind;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Starvation detection via spectral analysis for session types (Step 80d).
 *
 * <p>Starvation occurs when a transition (or set of transitions) is enabled in
 * some state but never taken under fair scheduling. More precisely, a transition
 * is <em>starved</em> if it exists in the state space but no path from top to
 * bottom uses it.
 *
 * <p>Public API:
 * <ul>
 *   <li>{@link #detectStarvation(StateSpace)} -- find starved transitions</li>
 *   <li>{@link #perRoleSpectralGap(StateSpace)} -- spectral gap per label</li>
 *   <li>{@link #fairnessScore(StateSpace)} -- min/max transition coverage ratio</li>
 *   <li>{@link #pendularFairness(StateSpace)} -- pendular balance fairness</li>
 *   <li>{@link #isStarvationFree(StateSpace)} -- true iff all transitions reachable</li>
 *   <li>{@link #analyzeStarvation(StateSpace)} -- comprehensive StarvationResult</li>
 * </ul>
 */
public final class StarvationChecker {

    private StarvationChecker() {}

    // -----------------------------------------------------------------------
    // Result records
    // -----------------------------------------------------------------------

    /**
     * A single starved transition.
     *
     * @param source source state
     * @param label  transition label
     * @param target target state
     * @param reason why this transition is starved
     */
    public record StarvedTransition(int source, String label, int target, String reason) {
        public StarvedTransition {
            Objects.requireNonNull(label, "label must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    /**
     * Spectral gap for a single label (role).
     *
     * @param label          the transition label
     * @param count          number of transitions with this label
     * @param reachableCount number of those on top-to-bottom paths
     * @param spectralGap    spectral gap for this label's sub-matrix
     * @param isStarved      true if some transitions with this label are unreachable
     */
    public record RoleSpectralGap(String label, int count, int reachableCount,
                                  double spectralGap, boolean isStarved) {
        public RoleSpectralGap {
            Objects.requireNonNull(label, "label must not be null");
        }
    }

    /**
     * Comprehensive starvation analysis result.
     *
     * @param isStarvationFree     true iff all transitions are on top-bottom paths
     * @param starvedTransitions   details of each starved transition
     * @param numStarved           number of starved transitions
     * @param numTotalTransitions  total transitions
     * @param roleSpectralGaps     spectral gap per label
     * @param fairness             fairness score (0 = maximally unfair, 1 = perfectly fair)
     * @param pendularBalance      pendular fairness measure
     * @param reachableStates      states on paths from top to bottom
     * @param unreachableStates    states not on any top-to-bottom path
     * @param coverageRatio        fraction of transitions that are reachable
     * @param summary              human-readable summary
     */
    public record StarvationResult(
            boolean isStarvationFree,
            List<StarvedTransition> starvedTransitions,
            int numStarved,
            int numTotalTransitions,
            List<RoleSpectralGap> roleSpectralGaps,
            double fairness,
            double pendularBalance,
            Set<Integer> reachableStates,
            Set<Integer> unreachableStates,
            double coverageRatio,
            String summary) {
        public StarvationResult {
            Objects.requireNonNull(starvedTransitions, "starvedTransitions must not be null");
            Objects.requireNonNull(roleSpectralGaps, "roleSpectralGaps must not be null");
            Objects.requireNonNull(reachableStates, "reachableStates must not be null");
            Objects.requireNonNull(unreachableStates, "unreachableStates must not be null");
            Objects.requireNonNull(summary, "summary must not be null");
            starvedTransitions = List.copyOf(starvedTransitions);
            roleSpectralGaps = List.copyOf(roleSpectralGaps);
            reachableStates = Set.copyOf(reachableStates);
            unreachableStates = Set.copyOf(unreachableStates);
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /** States reachable from {@code start} via forward edges. */
    private static Set<Integer> reachableFrom(StateSpace ss, int start) {
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            int s = queue.poll();
            for (var t : ss.transitionsFrom(s)) {
                if (visited.add(t.target())) {
                    queue.add(t.target());
                }
            }
        }
        return visited;
    }

    /** States that can reach {@code target} via reverse edges. */
    private static Set<Integer> reverseReachable(StateSpace ss, int target) {
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

    /**
     * Transitions that lie on at least one top-to-bottom path.
     * A transition (s, l, t) is on a top-bottom path if s is reachable from top
     * AND t can reach bottom.
     */
    private static Set<TransitionKey> transitionsOnPaths(
            StateSpace ss, Set<Integer> fromTop, Set<Integer> toBottom) {
        Set<TransitionKey> onPath = new HashSet<>();
        for (var t : ss.transitions()) {
            if (fromTop.contains(t.source()) && toBottom.contains(t.target())) {
                onPath.add(new TransitionKey(t.source(), t.label(), t.target()));
            }
        }
        return onPath;
    }

    /** Internal key for identifying a transition by (source, label, target). */
    private record TransitionKey(int source, String label, int target) {}

    /**
     * Estimate transition coverage under uniform random walk from top.
     * Uses power method on transition matrix to compute expected visit
     * frequency for each transition.
     */
    private static Map<TransitionKey, Double> randomWalkTransitionCoverage(
            StateSpace ss, int numSteps) {
        if (ss.states().isEmpty() || ss.transitions().isEmpty()) {
            return Map.of();
        }

        List<Integer> stateList = ss.states().stream().sorted().collect(Collectors.toList());
        int n = stateList.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(stateList.get(i), i);

        // Build adjacency
        Map<Integer, List<Transition>> adj = new HashMap<>();
        for (int s : stateList) adj.put(s, new ArrayList<>());
        for (var t : ss.transitions()) {
            adj.computeIfAbsent(t.source(), k -> new ArrayList<>()).add(t);
        }

        // Start from top
        double[] prob = new double[n];
        prob[idx.getOrDefault(ss.top(), 0)] = 1.0;

        Map<TransitionKey, Double> transFreq = new HashMap<>();

        for (int step = 0; step < numSteps; step++) {
            double[] newProb = new double[n];
            for (int s : stateList) {
                int si = idx.get(s);
                if (prob[si] < 1e-15) continue;
                List<Transition> neighbors = adj.getOrDefault(s, List.of());
                if (neighbors.isEmpty()) {
                    // Absorbing state
                    newProb[si] += prob[si];
                    continue;
                }
                double pEach = prob[si] / neighbors.size();
                for (var t : neighbors) {
                    int ti = idx.get(t.target());
                    newProb[ti] += pEach;
                    TransitionKey key = new TransitionKey(t.source(), t.label(), t.target());
                    transFreq.merge(key, pEach, Double::sum);
                }
            }
            prob = newProb;
        }

        return transFreq;
    }

    /**
     * Compute spectral gap for transitions with a specific label.
     * Builds a sub-transition matrix restricted to the given label and computes
     * 1 - |lambda_2| where lambda_2 is the second-largest eigenvalue magnitude.
     */
    private static double computeSpectralGapForLabel(StateSpace ss, String label) {
        Set<Integer> statesInvolved = new HashSet<>();
        for (var t : ss.transitions()) {
            if (t.label().equals(label)) {
                statesInvolved.add(t.source());
                statesInvolved.add(t.target());
            }
        }

        if (statesInvolved.size() <= 1) return 1.0;

        List<Integer> stateList = statesInvolved.stream().sorted().collect(Collectors.toList());
        int n = stateList.size();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(stateList.get(i), i);

        // Build symmetric adjacency matrix
        double[][] A = new double[n][n];
        for (var t : ss.transitions()) {
            if (t.label().equals(label) && statesInvolved.contains(t.source())
                    && statesInvolved.contains(t.target())) {
                A[idx.get(t.source())][idx.get(t.target())] = 1.0;
                A[idx.get(t.target())][idx.get(t.source())] = 1.0; // symmetrize
            }
        }

        // Degree and transition matrix P = D^{-1} A
        double[][] P = new double[n][n];
        for (int i = 0; i < n; i++) {
            double d = 0;
            for (int j = 0; j < n; j++) d += A[i][j];
            if (d > 0) {
                for (int j = 0; j < n; j++) P[i][j] = A[i][j] / d;
            } else {
                P[i][i] = 1.0;
            }
        }

        // QR iteration for eigenvalues
        double[] eigs = smallEigenvalues(P, n, 100);
        if (eigs.length < 2) return 1.0;

        double lambda2 = Math.abs(eigs[1]);
        return Math.max(0.0, Math.min(1.0, 1.0 - lambda2));
    }

    /**
     * Compute eigenvalues of a small matrix via QR iteration.
     * Returns eigenvalues sorted descending by magnitude.
     */
    private static double[] smallEigenvalues(double[][] M, int n, int maxIter) {
        if (n == 0) return new double[0];
        if (n == 1) return new double[]{M[0][0]};

        // Copy matrix
        double[][] A = new double[n][n];
        for (int i = 0; i < n; i++) System.arraycopy(M[i], 0, A[i], 0, n);

        for (int iter = 0; iter < maxIter; iter++) {
            // QR decomposition (Gram-Schmidt)
            double[][] Q = new double[n][n];
            double[][] R = new double[n][n];

            for (int j = 0; j < n; j++) {
                double[] v = new double[n];
                for (int i = 0; i < n; i++) v[i] = A[i][j];

                for (int k = 0; k < j; k++) {
                    double dot = 0;
                    for (int i = 0; i < n; i++) dot += Q[i][k] * v[i];
                    R[k][j] = dot;
                    for (int i = 0; i < n; i++) v[i] -= dot * Q[i][k];
                }

                double norm = 0;
                for (double x : v) norm += x * x;
                norm = Math.sqrt(norm);

                if (norm < 1e-15) {
                    R[j][j] = 0.0;
                } else {
                    R[j][j] = norm;
                    for (int i = 0; i < n; i++) Q[i][j] = v[i] / norm;
                }
            }

            // A = R * Q
            double[][] newA = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    for (int k = 0; k < n; k++) {
                        newA[i][j] += R[i][k] * Q[k][j];
                    }
                }
            }
            A = newA;

            // Check convergence
            double off = 0;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < i; j++) {
                    off += A[i][j] * A[i][j];
                }
            }
            if (off < 1e-20) break;
        }

        double[] eigs = new double[n];
        for (int i = 0; i < n; i++) eigs[i] = A[i][i];
        // Sort descending by magnitude
        Double[] boxed = new Double[n];
        for (int i = 0; i < n; i++) boxed[i] = eigs[i];
        Arrays.sort(boxed, (a, b) -> Double.compare(Math.abs(b), Math.abs(a)));
        double[] result = new double[n];
        for (int i = 0; i < n; i++) result[i] = boxed[i];
        return result;
    }

    /**
     * Compute pendular balance: ratio of branch to select transitions.
     * A perfectly balanced protocol has equal branch and select transitions.
     * Returns a value in [0, 1] where 1 = perfectly balanced.
     */
    private static double pendularBalance(StateSpace ss) {
        int branchCount = 0;
        int selectCount = 0;

        for (var t : ss.transitions()) {
            if (t.kind() == TransitionKind.SELECTION) {
                selectCount++;
            } else {
                branchCount++;
            }
        }

        int total = branchCount + selectCount;
        if (total == 0) return 1.0;

        int max = Math.max(branchCount, selectCount);
        int min = Math.min(branchCount, selectCount);
        return max > 0 ? (double) min / max : 1.0;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Find transitions that are not on any top-to-bottom path.
     *
     * <p>A transition is starved if it exists in the state space but no
     * complete execution (top -> ... -> bottom) uses it.
     *
     * @param ss the state space to check
     * @return list of starved transitions
     */
    public static List<StarvedTransition> detectStarvation(StateSpace ss) {
        Set<Integer> fromTop = reachableFrom(ss, ss.top());
        Set<Integer> toBottom = reverseReachable(ss, ss.bottom());
        Set<TransitionKey> onPath = transitionsOnPaths(ss, fromTop, toBottom);

        List<StarvedTransition> starved = new ArrayList<>();
        for (var t : ss.transitions()) {
            TransitionKey key = new TransitionKey(t.source(), t.label(), t.target());
            if (!onPath.contains(key)) {
                String reason;
                if (!fromTop.contains(t.source())) {
                    reason = "source state " + t.source() + " not reachable from top";
                } else if (!toBottom.contains(t.target())) {
                    reason = "target state " + t.target() + " cannot reach bottom";
                } else {
                    reason = "transition not on any top-to-bottom path";
                }
                starved.add(new StarvedTransition(t.source(), t.label(), t.target(), reason));
            }
        }

        return List.copyOf(starved);
    }

    /**
     * Compute spectral gap per transition label.
     *
     * <p>Groups transitions by label and computes the spectral gap of the
     * sub-transition-matrix for each label. Low spectral gap indicates
     * poor connectivity for that label (potential starvation).
     *
     * @param ss the state space to check
     * @return map from label to RoleSpectralGap
     */
    public static Map<String, RoleSpectralGap> perRoleSpectralGap(StateSpace ss) {
        Set<Integer> fromTop = reachableFrom(ss, ss.top());
        Set<Integer> toBottom = reverseReachable(ss, ss.bottom());
        Set<TransitionKey> onPath = transitionsOnPaths(ss, fromTop, toBottom);

        // Group transitions by label
        Map<String, List<Transition>> byLabel = new TreeMap<>();
        for (var t : ss.transitions()) {
            byLabel.computeIfAbsent(t.label(), k -> new ArrayList<>()).add(t);
        }

        Map<String, RoleSpectralGap> result = new LinkedHashMap<>();
        for (var entry : byLabel.entrySet()) {
            String label = entry.getKey();
            List<Transition> transitions = entry.getValue();
            int count = transitions.size();
            int reachableCount = 0;
            for (var t : transitions) {
                if (onPath.contains(new TransitionKey(t.source(), t.label(), t.target()))) {
                    reachableCount++;
                }
            }
            double gap = computeSpectralGapForLabel(ss, label);
            boolean isStarved = reachableCount < count;
            result.put(label, new RoleSpectralGap(label, count, reachableCount, gap, isStarved));
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * Compute fairness score: min/max transition coverage ratio.
     *
     * <p>Under uniform random walk from top, measures how evenly transitions
     * are used. Returns ratio of minimum to maximum expected transition frequency.
     *
     * @param ss the state space to check
     * @return double in [0, 1]; 1 = perfectly fair, 0 = maximally unfair
     */
    public static double fairnessScore(StateSpace ss) {
        if (ss.transitions().isEmpty()) return 1.0;

        Map<TransitionKey, Double> coverage = randomWalkTransitionCoverage(ss, 500);
        if (coverage.isEmpty()) return 0.0;

        double maxFreq = coverage.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double minFreq = coverage.values().stream().mapToDouble(Double::doubleValue).min().orElse(0.0);

        if (maxFreq < 1e-15) return 1.0;

        // Account for transitions not in coverage (frequency 0)
        Set<TransitionKey> allTransitions = new HashSet<>();
        for (var t : ss.transitions()) {
            allTransitions.add(new TransitionKey(t.source(), t.label(), t.target()));
        }
        if (coverage.size() < allTransitions.size()) {
            minFreq = 0.0;
        }

        return maxFreq > 0 ? minFreq / maxFreq : 1.0;
    }

    /**
     * Pendular balance as a fairness measure.
     *
     * <p>Measures the ratio between branch and selection transitions.
     * Perfectly pendular protocols (alternating +/&amp;) have balance close to 1.0.
     *
     * @param ss the state space to check
     * @return double in [0, 1]; 1 = perfectly balanced
     */
    public static double pendularFairness(StateSpace ss) {
        return pendularBalance(ss);
    }

    /**
     * Return true iff all transitions are on some top-to-bottom path.
     *
     * @param ss the state space to check
     * @return true if starvation-free
     */
    public static boolean isStarvationFree(StateSpace ss) {
        return detectStarvation(ss).isEmpty();
    }

    /**
     * Comprehensive starvation analysis.
     *
     * <p>Combines transition reachability, spectral gap per label, fairness
     * scoring, and pendular balance.
     *
     * @param ss the state space to check
     * @return StarvationResult with complete analysis
     */
    public static StarvationResult analyzeStarvation(StateSpace ss) {
        Set<Integer> fromTop = reachableFrom(ss, ss.top());
        Set<Integer> toBottom = reverseReachable(ss, ss.bottom());

        // States on top-bottom paths
        Set<Integer> reachable = new HashSet<>(fromTop);
        reachable.retainAll(toBottom);
        Set<Integer> unreachable = new HashSet<>(ss.states());
        unreachable.removeAll(reachable);

        // Starved transitions
        List<StarvedTransition> starved = detectStarvation(ss);

        // Spectral gaps
        Map<String, RoleSpectralGap> gaps = perRoleSpectralGap(ss);

        // Fairness
        double fair = fairnessScore(ss);

        // Pendular balance
        double pendular = pendularFairness(ss);

        // Coverage ratio
        int total = ss.transitions().size();
        double coverage = total > 0 ? (double) (total - starved.size()) / total : 1.0;

        boolean isFree = starved.isEmpty();

        String summary;
        if (isFree) {
            summary = "Starvation-free: all transitions are on top-to-bottom paths.";
        } else {
            Set<String> starvedLabels = starved.stream()
                    .map(StarvedTransition::label)
                    .collect(Collectors.toCollection(TreeSet::new));
            summary = String.format(
                    "STARVATION DETECTED: %d of %d transitions are starved. " +
                    "Affected labels: %s. Fairness score: %.3f. %d unreachable states.",
                    starved.size(), total,
                    String.join(", ", starvedLabels),
                    fair, unreachable.size());
        }

        return new StarvationResult(
                isFree,
                starved,
                starved.size(),
                total,
                List.copyOf(gaps.values()),
                fair,
                pendular,
                reachable,
                unreachable,
                coverage,
                summary);
    }
}
