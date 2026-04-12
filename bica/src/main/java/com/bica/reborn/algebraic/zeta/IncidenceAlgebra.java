package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.statespace.StateSpace;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Incidence algebra of session type lattices (port of Python
 * {@code reticulate.incidence_algebra}, Step 32g).
 *
 * <p>The incidence algebra {@code I(P)} of a finite poset {@code P} is the
 * algebra of all functions {@code f : {(x,y) : x ≥ y} → ℝ} with multiplication
 * given by convolution:
 * <pre>
 *     (f * g)(x, y) = Σ_{x ≥ z ≥ y} f(x, z) · g(z, y)
 * </pre>
 *
 * <p>Key elements:
 * <ul>
 *   <li><b>zeta</b>: {@code ζ(x, y) = 1} for all {@code x ≥ y}</li>
 *   <li><b>Möbius</b>: {@code μ = ζ⁻¹}</li>
 *   <li><b>delta</b>: {@code δ(x, y) = 1} iff {@code x = y}</li>
 *   <li><b>rank</b>: {@code ρ(x, y) = rank(x) − rank(y)}</li>
 * </ul>
 *
 * <p>Elements are represented as {@code Map<Zeta.Pair, Double>} and are sparse
 * (zero entries are omitted). All computations handle cycles via SCC quotient.
 *
 * <p>This is a faithful port of the Python module with the same semantics and
 * numerical tolerances (1e-9 / 1e-12).
 */
public final class IncidenceAlgebra {

    private static final double TOL = 1e-9;
    private static final double ZERO_TOL = 1e-12;

    private IncidenceAlgebra() {}

    // =======================================================================
    // Result types
    // =======================================================================

    /** Complete incidence algebra analysis. */
    public record IncidenceAlgebraResult(
            int numStates,
            int numIntervals,
            int dimension,
            int zetaNonzero,
            int mobiusNonzero,
            boolean convolutionIdentityVerified,
            boolean mobiusInvolutionVerified,
            boolean isCommutative,
            Integer nilpotentIndex,
            List<String> multiplicativeFunctions) {}

    /** Result of a convolution {@code f * g}. */
    public record ConvolutionResult(
            Map<Zeta.Pair, Double> result,
            int nonzeroCount,
            double maxAbsValue) {}

    // =======================================================================
    // Incidence function construction
    // =======================================================================

    /**
     * Create an incidence function on the poset. If {@code func} is {@code null},
     * returns the zero function. Otherwise {@code func.apply(x, y)} is called
     * for each comparable pair {@code x ≥ y}.
     */
    public static Map<Zeta.Pair, Double> incidenceFunction(
            StateSpace ss, BiFunction<Integer, Integer, Double> func) {
        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);
        Map<Zeta.Pair, Double> result = new HashMap<>();
        if (func == null) return result;
        for (int x : ss.states()) {
            for (int y : ss.states()) {
                if (reach.get(x).contains(y)) {
                    double val = func.apply(x, y);
                    if (val != 0.0) {
                        result.put(new Zeta.Pair(x, y), val);
                    }
                }
            }
        }
        return result;
    }

    /** The zeta function {@code ζ(x, y) = 1} for all {@code x ≥ y}. */
    public static Map<Zeta.Pair, Double> zetaElement(StateSpace ss) {
        Map<Zeta.Pair, Integer> z = Zeta.zetaFunction(ss);
        Map<Zeta.Pair, Double> result = new HashMap<>();
        for (var e : z.entrySet()) result.put(e.getKey(), (double) e.getValue());
        return result;
    }

    /** The Möbius function {@code μ(x, y)} — inverse of zeta. */
    public static Map<Zeta.Pair, Double> mobiusElement(StateSpace ss) {
        Map<Zeta.Pair, Integer> mu = Zeta.mobiusFunction(ss);
        Map<Zeta.Pair, Double> result = new HashMap<>();
        for (var e : mu.entrySet()) result.put(e.getKey(), (double) e.getValue());
        return result;
    }

    /** The delta (identity) function {@code δ(x, y) = 1} iff {@code x = y} (modulo SCCs). */
    public static Map<Zeta.Pair, Double> deltaElement(StateSpace ss) {
        Map<Zeta.Pair, Integer> d = Zeta.deltaFunction(ss);
        Map<Zeta.Pair, Double> result = new HashMap<>();
        for (var e : d.entrySet()) result.put(e.getKey(), (double) e.getValue());
        return result;
    }

    /** The rank-difference function {@code ρ(x, y) = rank(x) − rank(y)}. */
    public static Map<Zeta.Pair, Double> rankElement(StateSpace ss) {
        Map<Integer, Integer> rank = Zeta.computeRank(ss);
        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);
        Map<Zeta.Pair, Double> result = new HashMap<>();
        for (int x : ss.states()) {
            for (int y : ss.states()) {
                if (reach.get(x).contains(y)) {
                    double val = rank.getOrDefault(x, 0) - rank.getOrDefault(y, 0);
                    if (val != 0.0) {
                        result.put(new Zeta.Pair(x, y), val);
                    }
                }
            }
        }
        return result;
    }

    /** Constant function {@code f(x, y) = c} for all {@code x ≥ y}. */
    public static Map<Zeta.Pair, Double> constantElement(StateSpace ss, double c) {
        Map<Zeta.Pair, Double> result = new HashMap<>();
        if (c == 0.0) return result;
        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);
        for (int x : ss.states()) {
            for (int y : ss.states()) {
                if (reach.get(x).contains(y)) {
                    result.put(new Zeta.Pair(x, y), c);
                }
            }
        }
        return result;
    }

    /** Indicator function: {@code f(x,y) = 1} for {@code (x,y) ∈ pairs}, else {@code 0}. */
    public static Map<Zeta.Pair, Double> indicatorElement(StateSpace ss, Set<Zeta.Pair> pairs) {
        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);
        Set<Integer> states = ss.states();
        Map<Zeta.Pair, Double> result = new HashMap<>();
        for (Zeta.Pair p : pairs) {
            if (states.contains(p.x()) && states.contains(p.y())
                    && reach.get(p.x()).contains(p.y())) {
                result.put(p, 1.0);
            }
        }
        return result;
    }

    // =======================================================================
    // Algebra operations
    // =======================================================================

    /**
     * Convolution in the incidence algebra:
     * {@code (f * g)(x, y) = Σ_{x ≥ z ≥ y} f(x, z) · g(z, y)}.
     *
     * <p>SCC-aware: states in the same SCC are treated as equivalent.
     */
    public static Map<Zeta.Pair, Double> convolve(
            Map<Zeta.Pair, Double> f, Map<Zeta.Pair, Double> g, StateSpace ss) {
        Zeta.SccResult sccR = Zeta.computeSccs(ss);
        Map<Integer, Integer> sccMap = sccR.sccMap();
        Map<Integer, Set<Integer>> sccMembers = sccR.sccMembers();
        List<Integer> reps = new ArrayList<>(sccMembers.keySet());
        Collections.sort(reps);

        // Quotient adjacency + reachability
        Map<Integer, Set<Integer>> qAdj = new HashMap<>();
        for (int r : reps) qAdj.put(r, new HashSet<>());
        for (var t : ss.transitions()) {
            int sr = sccMap.get(t.source());
            int tr = sccMap.get(t.target());
            if (sr != tr) qAdj.get(sr).add(tr);
        }
        Map<Integer, Set<Integer>> qReach = new HashMap<>();
        for (int r : reps) {
            Set<Integer> visited = new HashSet<>();
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(r);
            while (!stack.isEmpty()) {
                int u = stack.pop();
                if (!visited.add(u)) continue;
                for (int v : qAdj.get(u)) stack.push(v);
            }
            qReach.put(r, visited);
        }

        Map<Zeta.Pair, Double> result = new HashMap<>();
        for (int x : ss.states()) {
            int xr = sccMap.get(x);
            for (int y : ss.states()) {
                int yr = sccMap.get(y);
                if (!qReach.get(xr).contains(yr)) continue;
                double total = 0.0;
                for (int zr : reps) {
                    if (qReach.get(xr).contains(zr) && qReach.get(zr).contains(yr)) {
                        int z = Collections.min(sccMembers.get(zr));
                        double fval = f.getOrDefault(new Zeta.Pair(x, z), 0.0);
                        double gval = g.getOrDefault(new Zeta.Pair(z, y), 0.0);
                        total += fval * gval;
                    }
                }
                if (Math.abs(total) > ZERO_TOL) {
                    result.put(new Zeta.Pair(x, y), total);
                }
            }
        }
        return result;
    }

    /** Pointwise addition. */
    public static Map<Zeta.Pair, Double> add(
            Map<Zeta.Pair, Double> f, Map<Zeta.Pair, Double> g) {
        Map<Zeta.Pair, Double> result = new HashMap<>();
        Set<Zeta.Pair> keys = new HashSet<>(f.keySet());
        keys.addAll(g.keySet());
        for (Zeta.Pair k : keys) {
            double val = f.getOrDefault(k, 0.0) + g.getOrDefault(k, 0.0);
            if (Math.abs(val) > ZERO_TOL) result.put(k, val);
        }
        return result;
    }

    /** Scalar multiplication {@code c · f}. */
    public static Map<Zeta.Pair, Double> scalarMul(double c, Map<Zeta.Pair, Double> f) {
        Map<Zeta.Pair, Double> result = new HashMap<>();
        if (Math.abs(c) < ZERO_TOL) return result;
        for (var e : f.entrySet()) {
            double v = c * e.getValue();
            if (Math.abs(v) > ZERO_TOL) result.put(e.getKey(), v);
        }
        return result;
    }

    /** Pointwise subtraction {@code f − g}. */
    public static Map<Zeta.Pair, Double> subtract(
            Map<Zeta.Pair, Double> f, Map<Zeta.Pair, Double> g) {
        return add(f, scalarMul(-1.0, g));
    }

    /**
     * Compute {@code f^n} (n-fold convolution).
     * {@code f^0 = δ}, {@code f^1 = f}, {@code f^n = f * f^{n-1}}.
     */
    public static Map<Zeta.Pair, Double> power(
            Map<Zeta.Pair, Double> f, int n, StateSpace ss) {
        if (n < 0) throw new IllegalArgumentException("Negative powers not supported directly");
        if (n == 0) return deltaElement(ss);
        Map<Zeta.Pair, Double> result = deltaElement(ss);
        Map<Zeta.Pair, Double> base = new HashMap<>(f);
        int exp = n;
        while (exp > 0) {
            if ((exp & 1) == 1) result = convolve(result, base, ss);
            base = convolve(base, base, ss);
            exp >>= 1;
        }
        return result;
    }

    // =======================================================================
    // Verification and properties
    // =======================================================================

    /** Verify {@code ζ * μ = δ} (Möbius inversion in algebraic form). */
    public static boolean verifyConvolutionIdentity(StateSpace ss) {
        Map<Zeta.Pair, Double> zeta = zetaElement(ss);
        Map<Zeta.Pair, Double> mu = mobiusElement(ss);
        Map<Zeta.Pair, Double> delta = deltaElement(ss);
        Map<Zeta.Pair, Double> product = convolve(zeta, mu, ss);

        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);
        for (int x : ss.states()) {
            for (int y : ss.states()) {
                if (reach.get(x).contains(y)) {
                    double expected = delta.getOrDefault(new Zeta.Pair(x, y), 0.0);
                    double actual = product.getOrDefault(new Zeta.Pair(x, y), 0.0);
                    if (Math.abs(expected - actual) > TOL) return false;
                }
            }
        }
        return true;
    }

    /** Verify {@code μ * ζ = δ} (right inverse check). */
    public static boolean verifyMobiusInvolution(StateSpace ss) {
        Map<Zeta.Pair, Double> zeta = zetaElement(ss);
        Map<Zeta.Pair, Double> mu = mobiusElement(ss);
        Map<Zeta.Pair, Double> delta = deltaElement(ss);
        Map<Zeta.Pair, Double> product = convolve(mu, zeta, ss);

        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);
        for (int x : ss.states()) {
            for (int y : ss.states()) {
                if (reach.get(x).contains(y)) {
                    double expected = delta.getOrDefault(new Zeta.Pair(x, y), 0.0);
                    double actual = product.getOrDefault(new Zeta.Pair(x, y), 0.0);
                    if (Math.abs(expected - actual) > TOL) return false;
                }
            }
        }
        return true;
    }

    /** Check if {@code f} is idempotent: {@code f * f = f}. */
    public static boolean isMultiplicative(Map<Zeta.Pair, Double> f, StateSpace ss) {
        Map<Zeta.Pair, Double> f2 = convolve(f, f, ss);
        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);
        for (int x : ss.states()) {
            for (int y : ss.states()) {
                if (reach.get(x).contains(y)) {
                    double a = f.getOrDefault(new Zeta.Pair(x, y), 0.0);
                    double b = f2.getOrDefault(new Zeta.Pair(x, y), 0.0);
                    if (Math.abs(a - b) > TOL) return false;
                }
            }
        }
        return true;
    }

    /** Check if {@code f * g = g * f}. */
    public static boolean isCommutativePair(
            Map<Zeta.Pair, Double> f, Map<Zeta.Pair, Double> g, StateSpace ss) {
        Map<Zeta.Pair, Double> fg = convolve(f, g, ss);
        Map<Zeta.Pair, Double> gf = convolve(g, f, ss);
        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);
        for (int x : ss.states()) {
            for (int y : ss.states()) {
                if (reach.get(x).contains(y)) {
                    double a = fg.getOrDefault(new Zeta.Pair(x, y), 0.0);
                    double b = gf.getOrDefault(new Zeta.Pair(x, y), 0.0);
                    if (Math.abs(a - b) > TOL) return false;
                }
            }
        }
        return true;
    }

    /**
     * Find the nilpotency index of {@code η = ζ − δ}. Returns {@code null}
     * if not nilpotent within {@code n + 2} steps.
     */
    public static Integer nilpotentIndex(StateSpace ss) {
        Map<Zeta.Pair, Double> zeta = zetaElement(ss);
        Map<Zeta.Pair, Double> delta = deltaElement(ss);
        Map<Zeta.Pair, Double> eta = subtract(zeta, delta);

        int n = ss.states().size();
        Map<Zeta.Pair, Double> current = new HashMap<>(eta);

        for (int k = 1; k <= n + 1; k++) {
            if (isZero(current)) return k;
            current = convolve(current, eta, ss);
        }
        if (isZero(current)) return n + 2;
        return null;
    }

    private static boolean isZero(Map<Zeta.Pair, Double> f) {
        for (double v : f.values()) {
            if (Math.abs(v) >= TOL) return false;
        }
        return true;
    }

    /**
     * Compute the inverse of {@code f} in the incidence algebra, if it exists.
     * {@code f} is invertible iff {@code f(x, x) ≠ 0} for all {@code x}.
     * Returns {@code null} if not invertible.
     */
    public static Map<Zeta.Pair, Double> inverse(Map<Zeta.Pair, Double> f, StateSpace ss) {
        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);

        // Check diagonal
        for (int x : ss.states()) {
            if (Math.abs(f.getOrDefault(new Zeta.Pair(x, x), 0.0)) < ZERO_TOL) {
                return null;
            }
        }
        Map<Integer, Integer> rank = Zeta.computeRank(ss);
        Map<Zeta.Pair, Double> inv = new HashMap<>();

        // Diagonal
        for (int x : ss.states()) {
            inv.put(new Zeta.Pair(x, x), 1.0 / f.get(new Zeta.Pair(x, x)));
        }

        // Off-diagonal pairs, sorted by increasing rank difference
        List<Zeta.Pair> pairs = new ArrayList<>();
        for (int x : ss.states()) {
            for (int y : ss.states()) {
                if (x != y && reach.get(x).contains(y)) {
                    pairs.add(new Zeta.Pair(x, y));
                }
            }
        }
        pairs.sort(Comparator.comparingInt(
                p -> rank.getOrDefault(p.x(), 0) - rank.getOrDefault(p.y(), 0)));

        for (Zeta.Pair p : pairs) {
            int x = p.x();
            int y = p.y();
            double total = 0.0;
            for (int z : ss.states()) {
                if (z != x && reach.get(x).contains(z) && reach.get(z).contains(y)) {
                    double fz = f.getOrDefault(new Zeta.Pair(x, z), 0.0);
                    double iv = inv.getOrDefault(new Zeta.Pair(z, y), 0.0);
                    total += fz * iv;
                }
            }
            inv.put(p, -total / f.get(new Zeta.Pair(x, x)));
        }
        return inv;
    }

    // =======================================================================
    // Full analysis
    // =======================================================================

    /** Complete incidence algebra analysis. */
    public static IncidenceAlgebraResult analyzeIncidenceAlgebra(StateSpace ss) {
        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);

        int numIntervals = 0;
        for (int x : ss.states()) {
            for (int y : ss.states()) {
                if (reach.get(x).contains(y)) numIntervals++;
            }
        }

        Map<Zeta.Pair, Double> zeta = zetaElement(ss);
        Map<Zeta.Pair, Double> mu = mobiusElement(ss);

        int zetaNz = 0;
        for (double v : zeta.values()) if (Math.abs(v) > ZERO_TOL) zetaNz++;
        int muNz = 0;
        for (double v : mu.values()) if (Math.abs(v) > ZERO_TOL) muNz++;

        boolean convId = verifyConvolutionIdentity(ss);
        boolean muInv = verifyMobiusInvolution(ss);
        boolean commutative = isCommutativePair(zeta, mu, ss);
        Integer nilp = nilpotentIndex(ss);

        List<String> multFuncs = new ArrayList<>();
        Map<Zeta.Pair, Double> delta = deltaElement(ss);
        if (isMultiplicative(delta, ss)) multFuncs.add("delta");

        return new IncidenceAlgebraResult(
                ss.states().size(),
                numIntervals,
                numIntervals,
                zetaNz,
                muNz,
                convId,
                muInv,
                commutative,
                nilp,
                multFuncs);
    }
}
