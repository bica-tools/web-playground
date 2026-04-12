package com.bica.reborn.protocol_engineering;

import com.bica.reborn.algebraic.eigenvalues.EigenvalueComputer;
import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.protocol_mechanics.ProtocolMechanicsChecker;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Protocol engineering toolkit using Lagrangian mechanics (Step 60r).
 *
 * <p>Unified framework for DESCRIBE &rarr; BUILD &rarr; VERIFY &rarr; MONITOR.
 *
 * <p>Faithful Java port of {@code reticulate.protocol_engineering}.
 */
public final class ProtocolEngineeringChecker {

    private ProtocolEngineeringChecker() {}

    // =========================================================================
    // Result types
    // =========================================================================

    /** Energy profile of a single protocol state. */
    public record StateProfile(
            int stateId,
            String label,
            double kinetic,
            double potential,
            double lagrangian,
            double hamiltonian,
            int momentumVal,
            double gravity,
            boolean isHotspot,
            boolean isBottleneck,
            boolean isDeadlock) {}

    /** Design quality score based on mechanical principles. */
    public record DesignScore(
            double efficiency,
            double smoothness,
            double robustness,
            boolean termination,
            double balance,
            double overall,
            List<String> suggestions) {}

    /** Result of mechanical verification. */
    public record VerificationResult(
            boolean allPassed,
            List<Check> checks) {

        public record Check(String name, boolean passed, String message) {}
    }

    // =========================================================================
    // 1. DESCRIBE: Energy landscape
    // =========================================================================

    /** Generate energy profile for every state in the protocol. */
    public static List<StateProfile> describeProtocol(StateSpace ss) {
        Map<Integer, Double> T = LagrangianUtils.kineticEnergy(ss);
        Map<Integer, Double> V = LagrangianUtils.potentialEnergy(ss);
        Map<Integer, Double> L = LagrangianUtils.lagrangianField(ss);
        Map<Integer, Double> H = LagrangianUtils.hamiltonianField(ss);
        Map<Integer, Integer> p = LagrangianUtils.momentum(ss);
        Map<Integer, Double> g = LagrangianUtils.gravitationalField(ss);

        double meanT = T.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        List<StateProfile> profiles = new ArrayList<>();
        List<Integer> sorted = new ArrayList<>(ss.states());
        Collections.sort(sorted);

        for (int s : sorted) {
            String label = ss.labels().getOrDefault(s, String.valueOf(s));
            boolean isHot = T.get(s) > meanT * 1.5;
            boolean isBottle = p.get(s) == 1 && V.get(s) > 1.0;
            boolean isDead = T.get(s) == 0.0 && s != ss.bottom();

            profiles.add(new StateProfile(
                    s, label, T.get(s), V.get(s), L.get(s), H.get(s),
                    p.get(s), g.get(s), isHot, isBottle, isDead));
        }
        return profiles;
    }

    // =========================================================================
    // 2. BUILD: Design rules
    // =========================================================================

    /** Evaluate protocol design quality using mechanical principles. */
    public static DesignScore evaluateDesign(StateSpace ss) {
        // Efficiency: inverse of total action
        var lap = LagrangianUtils.leastActionPath(ss);
        List<Integer> path = lap.getKey();
        double action = lap.getValue();
        int n = ss.states().size();
        double efficiency = 1.0 / (1.0 + Math.abs(action) / Math.max(n, 1));

        // Smoothness
        double smooth = ProtocolMechanicsChecker.smoothness(ss);

        // Robustness (Fiedler)
        double fiedler = EigenvalueComputer.fiedlerValue(ss);
        double robustness = Math.min(1.0, fiedler / 2.0);

        // Termination: bottom reachable from all states
        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);
        boolean terminates = true;
        for (int s : ss.states()) {
            if (!reach.get(s).contains(ss.bottom())) {
                terminates = false;
                break;
            }
        }

        // Energy balance along least-action path
        Map<Integer, Double> H = LagrangianUtils.hamiltonianField(ss);
        double balance;
        if (path.size() >= 2) {
            double maxH = Double.NEGATIVE_INFINITY, minH = Double.POSITIVE_INFINITY;
            for (int s : path) {
                double h = H.getOrDefault(s, 0.0);
                maxH = Math.max(maxH, h);
                minH = Math.min(minH, h);
            }
            balance = 1.0 / (1.0 + (maxH - minH));
        } else {
            balance = 1.0;
        }

        // Overall score
        double overall = (efficiency + smooth + robustness + balance) / 4.0;
        if (!terminates) overall *= 0.5;

        // Suggestions
        List<String> suggestions = new ArrayList<>();
        if (efficiency < 0.3) {
            suggestions.add("High action: consider removing unnecessary branching");
        }
        if (smooth < 0.3) {
            suggestions.add("Low smoothness: add intermediate states to reduce sharp transitions");
        }
        if (robustness < 0.2) {
            suggestions.add("Low robustness: add parallel paths for redundancy");
        }
        if (!terminates) {
            suggestions.add("CRITICAL: not all states can reach termination");
        }
        if (balance < 0.3) {
            suggestions.add("Energy imbalance: some path segments are much more complex than others");
        }

        // Check for deadlocks
        Map<Integer, Double> T = LagrangianUtils.kineticEnergy(ss);
        List<Integer> deadlocks = new ArrayList<>();
        for (int s : ss.states()) {
            if (T.get(s) == 0.0 && s != ss.bottom()) deadlocks.add(s);
        }
        if (!deadlocks.isEmpty()) {
            suggestions.add("DEADLOCK: states " + deadlocks + " have no outgoing transitions");
        }

        return new DesignScore(efficiency, smooth, robustness, terminates,
                balance, overall, List.copyOf(suggestions));
    }

    // =========================================================================
    // 3. VERIFY: Conservation law checking
    // =========================================================================

    /** Verify protocol correctness using conservation laws. */
    public static VerificationResult verifyProtocol(StateSpace ss) {
        List<VerificationResult.Check> checks = new ArrayList<>();

        Map<Integer, Double> T = LagrangianUtils.kineticEnergy(ss);
        Map<Integer, Double> V = LagrangianUtils.potentialEnergy(ss);
        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);

        // Check 1: No deadlocks
        List<Integer> deadlocks = new ArrayList<>();
        for (int s : ss.states()) {
            if (T.get(s) == 0 && s != ss.bottom()) deadlocks.add(s);
        }
        checks.add(new VerificationResult.Check(
                "no_deadlock", deadlocks.isEmpty(),
                deadlocks.isEmpty() ? "No deadlocks" : "Deadlock states: " + deadlocks));

        // Check 2: Termination
        List<Integer> unreachable = new ArrayList<>();
        for (int s : ss.states()) {
            if (!reach.get(s).contains(ss.bottom())) unreachable.add(s);
        }
        checks.add(new VerificationResult.Check(
                "termination", unreachable.isEmpty(),
                unreachable.isEmpty() ? "All paths terminate" : "Cannot reach end from: " + unreachable));

        // Check 3: Potential monotonically decreasing along transitions
        int violations = 0;
        for (var t : ss.transitions()) {
            if (V.get(t.target()) > V.get(t.source())) violations++;
        }
        checks.add(new VerificationResult.Check(
                "potential_decreasing", violations == 0,
                violations == 0 ? "Potential decreases along all transitions"
                        : "Potential increases in " + violations + " transitions"));

        // Check 4: No isolated states
        Set<Integer> fromTop = reach.get(ss.top());
        List<Integer> isolated = new ArrayList<>();
        for (int s : ss.states()) {
            if (!fromTop.contains(s)) isolated.add(s);
        }
        checks.add(new VerificationResult.Check(
                "no_isolated", isolated.isEmpty(),
                isolated.isEmpty() ? "All states reachable from top" : "Isolated states: " + isolated));

        // Check 5: Recursion termination
        var circs = LagrangianUtils.analyzeCircular(ss);
        long stuckLoops = circs.stream().filter(c -> c.escapeTransitions() == 0).count();
        checks.add(new VerificationResult.Check(
                "recursive_escape", stuckLoops == 0,
                stuckLoops == 0 ? "All recursive loops have escape transitions"
                        : stuckLoops + " loops with no escape"));

        boolean allPassed = checks.stream().allMatch(VerificationResult.Check::passed);
        return new VerificationResult(allPassed, List.copyOf(checks));
    }

    // =========================================================================
    // 4. MONITOR: Runtime energy tracking
    // =========================================================================

    /** Runtime monitor that tracks protocol execution using energy. */
    public static class RuntimeMonitor {
        private final StateSpace ss;
        private final Map<Integer, Double> T;
        private final Map<Integer, Double> V;
        private final Map<Integer, Double> H;
        private int currentState;
        private final List<Map.Entry<Integer, String>> history = new ArrayList<>();
        private final List<String> anomalies = new ArrayList<>();
        private final Map<Integer, Integer> loopCounter = new HashMap<>();

        public RuntimeMonitor(StateSpace ss) {
            this.ss = ss;
            this.T = LagrangianUtils.kineticEnergy(ss);
            this.V = LagrangianUtils.potentialEnergy(ss);
            this.H = LagrangianUtils.hamiltonianField(ss);
            this.currentState = ss.top();
        }

        /** Execute one protocol step and return any anomalies detected. */
        public List<String> step(String label) {
            List<String> stepAnomalies = new ArrayList<>();

            // Find transition
            Integer nextState = null;
            for (var t : ss.transitions()) {
                if (t.source() == currentState && t.label().equals(label)) {
                    nextState = t.target();
                    break;
                }
            }

            if (nextState == null) {
                stepAnomalies.add("INVALID_TRANSITION: '" + label + "' not available at state " + currentState);
                anomalies.addAll(stepAnomalies);
                return stepAnomalies;
            }

            // Check potential decrease
            if (V.get(nextState) > V.get(currentState)) {
                stepAnomalies.add(String.format("REGRESSION: V increased from %.1f to %.1f",
                        V.get(currentState), V.get(nextState)));
            }

            // Check for approaching deadlock
            if (T.getOrDefault(nextState, 0.0) == 0 && nextState != ss.bottom()) {
                stepAnomalies.add("DEADLOCK_RISK: state " + nextState + " has no outgoing transitions");
            }

            // Track loop iterations
            int count = loopCounter.merge(nextState, 1, Integer::sum);
            if (count > 100) {
                stepAnomalies.add("INFINITE_LOOP_RISK: state " + nextState + " visited " + count + " times");
            }

            // Update state
            history.add(Map.entry(currentState, label));
            currentState = nextState;
            anomalies.addAll(stepAnomalies);

            return stepAnomalies;
        }

        public int currentState() { return currentState; }
        public double currentEnergy() { return H.getOrDefault(currentState, 0.0); }
        public double currentPotential() { return V.getOrDefault(currentState, 0.0); }
        public boolean isTerminated() { return currentState == ss.bottom(); }
        public List<String> allAnomalies() { return List.copyOf(anomalies); }
        public List<Map.Entry<Integer, String>> history() { return List.copyOf(history); }

        /** Return the Hamiltonian value at each step of the execution. */
        public List<Double> energyTrace() {
            List<Double> trace = new ArrayList<>();
            trace.add(H.getOrDefault(ss.top(), 0.0));
            int cur = ss.top();
            for (var entry : history) {
                int state = entry.getKey();
                String label = entry.getValue();
                for (var t : ss.transitions()) {
                    if (t.source() == state && t.label().equals(label)) {
                        cur = t.target();
                        break;
                    }
                }
                trace.add(H.getOrDefault(cur, 0.0));
            }
            return trace;
        }
    }
}
