package com.bica.reborn.order_polynomial;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Order polynomial for session type lattices (Step 30ab) -- Java port of
 * {@code reticulate/reticulate/order_polynomial.py}.
 *
 * <p>The order polynomial Omega(P, t) of a finite poset P counts the number
 * of order-preserving maps f: P -> {1, 2, ..., t}, i.e., maps satisfying
 * x <= y implies f(x) <= f(y).
 *
 * <p>The strict order polynomial counts strictly order-preserving maps
 * (x < y implies f(x) < f(y)).
 *
 * <p>All methods are static. Results use Java records.
 */
public final class OrderPolynomialChecker {

    private OrderPolynomialChecker() {}

    // =======================================================================
    // Result type
    // =======================================================================

    /** Complete order polynomial analysis. */
    public record OrderPolynomialResult(
            int numStates,
            List<Integer> values,
            List<Integer> strictValues,
            List<Double> coefficients,
            List<Double> strictCoefficients,
            int omegaAtMinusOne,
            int strictAt1,
            int numLinearExtensions,
            boolean isProductFormulaValid,
            int height) {

        public OrderPolynomialResult {
            values = List.copyOf(values);
            strictValues = List.copyOf(strictValues);
            coefficients = List.copyOf(coefficients);
            strictCoefficients = List.copyOf(strictCoefficients);
        }
    }

    // =======================================================================
    // Internal: build DAG (SCC quotient)
    // =======================================================================

    /** DAG representation: list of SCC representative states + successor map. */
    record Dag(List<Integer> states, Map<Integer, Set<Integer>> succ) {}

    static Dag buildDag(StateSpace ss) {
        Zeta.SccResult sccResult = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccResult.sccMap();
        Map<Integer, Set<Integer>> sccMembers = sccResult.sccMembers();

        List<Integer> reps = new ArrayList<>(sccMembers.keySet());
        Collections.sort(reps);

        Map<Integer, Set<Integer>> succ = new LinkedHashMap<>();
        for (int r : reps) {
            succ.put(r, new LinkedHashSet<>());
        }

        for (var tr : ss.transitions()) {
            int rSrc = sccMap.get(tr.source());
            int rTgt = sccMap.get(tr.target());
            if (rSrc != rTgt) {
                succ.get(rSrc).add(rTgt);
            }
        }

        return new Dag(reps, succ);
    }

    // =======================================================================
    // Core: counting order-preserving maps via backtracking
    // =======================================================================

    /**
     * Count order-preserving maps from the poset to {1, ..., t}.
     *
     * @param states DAG states (SCC reps)
     * @param succ   successor map (s -> t means s can reach t, f(s) >= f(t))
     * @param t      target set size
     * @param strict if true, count strictly order-preserving maps
     * @return number of valid maps
     */
    static int countOrderPreserving(List<Integer> states, Map<Integer, Set<Integer>> succ,
                                     int t, boolean strict) {
        int n = states.size();
        if (t <= 0) {
            return n == 0 ? 1 : 0;
        }
        if (n == 0) {
            return 1;
        }

        // Build direct ordering constraints from succ
        // succ[s] contains states that s reaches, so f(s) >= f(t) for t in succ[s]
        Map<Integer, Set<Integer>> mustBeLeq = new HashMap<>(); // f(s) >= f(t) for t in mustBeLeq[s]
        Map<Integer, Set<Integer>> mustBeGeq = new HashMap<>(); // f(s) <= f(t) for t in mustBeGeq[s]
        Set<Integer> stateSet = new HashSet<>(states);
        for (int s : states) {
            mustBeLeq.put(s, new HashSet<>());
            mustBeGeq.put(s, new HashSet<>());
        }
        for (int s : states) {
            for (int s2 : succ.getOrDefault(s, Set.of())) {
                if (stateSet.contains(s2)) {
                    mustBeLeq.get(s).add(s2);
                    mustBeGeq.get(s2).add(s);
                }
            }
        }

        // Topological sort via Kahn's algorithm
        Map<Integer, Integer> inCount = new HashMap<>();
        for (int s : states) inCount.put(s, 0);
        for (int s : states) {
            for (int s2 : succ.getOrDefault(s, Set.of())) {
                if (inCount.containsKey(s2)) {
                    inCount.merge(s2, 1, Integer::sum);
                }
            }
        }

        List<Integer> topo = new ArrayList<>();
        PriorityQueue<Integer> queue = new PriorityQueue<>();
        for (int s : states) {
            if (inCount.get(s) == 0) queue.add(s);
        }
        while (!queue.isEmpty()) {
            int s = queue.poll();
            topo.add(s);
            for (int s2 : succ.getOrDefault(s, Set.of())) {
                if (inCount.containsKey(s2)) {
                    int newVal = inCount.get(s2) - 1;
                    inCount.put(s2, newVal);
                    if (newVal == 0) queue.add(s2);
                }
            }
        }

        if (topo.size() != n) {
            // Fallback for cycles (shouldn't happen after SCC quotient)
            topo = new ArrayList<>(states);
        }

        // Backtracking
        Map<Integer, Integer> assignment = new HashMap<>();
        int[] count = {0};
        List<Integer> topoFinal = topo;

        backtrack(0, n, topoFinal, mustBeLeq, mustBeGeq, assignment, count, t, strict);
        return count[0];
    }

    private static void backtrack(int idx, int n, List<Integer> topo,
                                   Map<Integer, Set<Integer>> mustBeLeq,
                                   Map<Integer, Set<Integer>> mustBeGeq,
                                   Map<Integer, Integer> assignment,
                                   int[] count, int t, boolean strict) {
        if (idx == n) {
            count[0]++;
            return;
        }

        int s = topo.get(idx);
        int lo = 1;
        int hi = t;

        for (int s2 : mustBeLeq.get(s)) {
            if (assignment.containsKey(s2)) {
                int bound = strict ? assignment.get(s2) + 1 : assignment.get(s2);
                lo = Math.max(lo, bound);
            }
        }
        for (int s2 : mustBeGeq.get(s)) {
            if (assignment.containsKey(s2)) {
                int bound = strict ? assignment.get(s2) - 1 : assignment.get(s2);
                hi = Math.min(hi, bound);
            }
        }

        for (int v = lo; v <= hi; v++) {
            assignment.put(s, v);
            backtrack(idx + 1, n, topo, mustBeLeq, mustBeGeq, assignment, count, t, strict);
        }
        assignment.remove(s);
    }

    // =======================================================================
    // Public API: values
    // =======================================================================

    /**
     * Evaluate Omega(P, t) for t = 0, 1, ..., maxT.
     */
    public static List<Integer> orderPolynomialValues(StateSpace ss, int maxT) {
        Dag dag = buildDag(ss);
        List<Integer> values = new ArrayList<>();
        for (int t = 0; t <= maxT; t++) {
            values.add(countOrderPreserving(dag.states(), dag.succ(), t, false));
        }
        return values;
    }

    /**
     * Evaluate strict Omega(P, t) for t = 0, 1, ..., maxT.
     */
    public static List<Integer> strictOrderPolynomialValues(StateSpace ss, int maxT) {
        Dag dag = buildDag(ss);
        List<Integer> values = new ArrayList<>();
        for (int t = 0; t <= maxT; t++) {
            values.add(countOrderPreserving(dag.states(), dag.succ(), t, true));
        }
        return values;
    }

    // =======================================================================
    // Lagrange interpolation
    // =======================================================================

    /**
     * Lagrange interpolation: given points (x_i, y_i), return polynomial
     * coefficients [a_0, a_1, ..., a_n] where p(x) = sum a_k * x^k.
     */
    static List<Double> lagrangeInterpolate(List<int[]> points) {
        int n = points.size();
        double[] coeffs = new double[n];

        for (int i = 0; i < n; i++) {
            double xi = points.get(i)[0];
            double yi = points.get(i)[1];
            double numer = yi;
            double[] basis = new double[n];
            basis[0] = 1.0;

            for (int j = 0; j < n; j++) {
                if (j == i) continue;
                double xj = points.get(j)[0];
                double denom = xi - xj;
                numer /= denom;

                double[] newBasis = new double[n];
                for (int k = n - 1; k >= 0; k--) {
                    newBasis[k] += basis[k] * (-xj);
                    if (k + 1 < n) {
                        newBasis[k + 1] += basis[k];
                    }
                }
                basis = newBasis;
            }

            for (int k = 0; k < n; k++) {
                coeffs[k] += numer * basis[k];
            }
        }

        List<Double> result = new ArrayList<>();
        for (double c : coeffs) result.add(c);
        return result;
    }

    // =======================================================================
    // Public API: coefficients
    // =======================================================================

    /**
     * Interpolate Omega(P, t) as a polynomial. Returns [a_0, a_1, ..., a_n].
     */
    public static List<Double> orderPolynomialCoefficients(StateSpace ss) {
        Dag dag = buildDag(ss);
        int n = dag.states().size();
        List<int[]> points = new ArrayList<>();
        for (int t = 0; t <= n; t++) {
            int val = countOrderPreserving(dag.states(), dag.succ(), t, false);
            points.add(new int[]{t, val});
        }
        return lagrangeInterpolate(points);
    }

    /**
     * Interpolate strict Omega(P, t) as a polynomial. Returns [a_0, a_1, ..., a_n].
     */
    public static List<Double> strictOrderPolynomialCoefficients(StateSpace ss) {
        Dag dag = buildDag(ss);
        int n = dag.states().size();
        List<int[]> points = new ArrayList<>();
        for (int t = 0; t <= n; t++) {
            int val = countOrderPreserving(dag.states(), dag.succ(), t, true);
            points.add(new int[]{t, val});
        }
        return lagrangeInterpolate(points);
    }

    // =======================================================================
    // Polynomial evaluation
    // =======================================================================

    /** Evaluate polynomial at x using Horner's method. */
    static double evalPoly(List<Double> coeffs, double x) {
        double result = 0.0;
        for (int i = coeffs.size() - 1; i >= 0; i--) {
            result = result * x + coeffs.get(i);
        }
        return result;
    }

    // =======================================================================
    // Linear extensions
    // =======================================================================

    /**
     * Count the number of linear extensions of the poset.
     * A linear extension is a strictly order-preserving BIJECTION f: P -> {1, ..., |P|}.
     */
    public static int numLinearExtensions(StateSpace ss) {
        Dag dag = buildDag(ss);
        List<Integer> states = dag.states();
        Map<Integer, Set<Integer>> succ = dag.succ();
        int n = states.size();
        if (n == 0) return 1;

        // Build "must be above" constraints: if s reaches t, then f(s) > f(t)
        Map<Integer, Set<Integer>> mustBeAbove = new HashMap<>();
        for (int s : states) mustBeAbove.put(s, new HashSet<>());
        for (int s : states) {
            for (int t : succ.getOrDefault(s, Set.of())) {
                if (mustBeAbove.containsKey(t)) {
                    mustBeAbove.get(t).add(s); // f(s) > f(t), so s is "above" t
                }
            }
        }

        // Topological sort
        Map<Integer, Integer> inDeg = new HashMap<>();
        for (int s : states) inDeg.put(s, 0);
        for (int s : states) {
            for (int t : succ.getOrDefault(s, Set.of())) {
                if (inDeg.containsKey(t)) {
                    inDeg.merge(t, 1, Integer::sum);
                }
            }
        }
        List<Integer> topo = new ArrayList<>();
        PriorityQueue<Integer> queue = new PriorityQueue<>();
        for (int s : states) {
            if (inDeg.get(s) == 0) queue.add(s);
        }
        while (!queue.isEmpty()) {
            int s = queue.poll();
            topo.add(s);
            for (int t : succ.getOrDefault(s, Set.of())) {
                if (inDeg.containsKey(t)) {
                    int nv = inDeg.get(t) - 1;
                    inDeg.put(t, nv);
                    if (nv == 0) queue.add(t);
                }
            }
        }

        // Backtracking: assign values 1..n bijectively
        Set<Integer> used = new HashSet<>();
        Map<Integer, Integer> assignment = new HashMap<>();
        int[] count = {0};

        backtrackLinExt(0, n, topo, succ, mustBeAbove, assignment, used, count);
        return count[0];
    }

    private static void backtrackLinExt(int idx, int n, List<Integer> topo,
                                         Map<Integer, Set<Integer>> succ,
                                         Map<Integer, Set<Integer>> mustBeAbove,
                                         Map<Integer, Integer> assignment,
                                         Set<Integer> used, int[] count) {
        if (idx == n) {
            count[0]++;
            return;
        }
        int s = topo.get(idx);
        int lo = 1;
        int hi = n;

        for (int above : mustBeAbove.get(s)) {
            if (assignment.containsKey(above)) {
                hi = Math.min(hi, assignment.get(above) - 1);
            }
        }
        for (int below : succ.getOrDefault(s, Set.of())) {
            if (assignment.containsKey(below)) {
                lo = Math.max(lo, assignment.get(below) + 1);
            }
        }

        for (int v = lo; v <= hi; v++) {
            if (!used.contains(v)) {
                assignment.put(s, v);
                used.add(v);
                backtrackLinExt(idx + 1, n, topo, succ, mustBeAbove, assignment, used, count);
                assignment.remove(s);
                used.remove(v);
            }
        }
    }

    // =======================================================================
    // Full analysis
    // =======================================================================

    /**
     * Complete order polynomial analysis.
     *
     * @param ss   state space
     * @param maxT maximum t value (default 8)
     */
    public static OrderPolynomialResult analyze(StateSpace ss, int maxT) {
        Dag dag = buildDag(ss);
        int n = dag.states().size();

        // Compute values
        List<Integer> values = new ArrayList<>();
        List<Integer> strictValues = new ArrayList<>();
        for (int t = 0; t <= maxT; t++) {
            values.add(countOrderPreserving(dag.states(), dag.succ(), t, false));
            strictValues.add(countOrderPreserving(dag.states(), dag.succ(), t, true));
        }

        // Interpolate coefficients -- need n+1 points
        List<int[]> pointsOp = new ArrayList<>();
        List<int[]> pointsSp = new ArrayList<>();
        int needed = n + 1;
        for (int t = 0; t < Math.min(needed, maxT + 1); t++) {
            pointsOp.add(new int[]{t, values.get(t)});
            pointsSp.add(new int[]{t, strictValues.get(t)});
        }
        // If we need more points beyond maxT
        for (int t = pointsOp.size(); t < needed; t++) {
            pointsOp.add(new int[]{t, countOrderPreserving(dag.states(), dag.succ(), t, false)});
            pointsSp.add(new int[]{t, countOrderPreserving(dag.states(), dag.succ(), t, true)});
        }

        List<Double> coeffs = lagrangeInterpolate(pointsOp.subList(0, needed));
        List<Double> strictCoeffs = lagrangeInterpolate(pointsSp.subList(0, needed));

        // Omega(P, -1)
        int omegaMinus1 = (int) Math.round(evalPoly(coeffs, -1.0));

        // Strict at 1
        int strict1 = strictValues.size() > 1 ? strictValues.get(1) : 0;

        // Linear extensions
        int nLinExt = numLinearExtensions(ss);

        // Height
        Map<Integer, Integer> rank = Zeta.computeRank(ss);
        int height = 0;
        for (int v : rank.values()) {
            if (v > height) height = v;
        }

        return new OrderPolynomialResult(
                n, values, strictValues, coeffs, strictCoeffs,
                omegaMinus1, strict1, nLinExt, true, height);
    }

    /** Overload with default maxT = 8. */
    public static OrderPolynomialResult analyze(StateSpace ss) {
        return analyze(ss, 8);
    }
}
