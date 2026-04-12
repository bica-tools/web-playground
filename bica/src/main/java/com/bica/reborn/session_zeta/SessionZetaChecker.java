package com.bica.reborn.session_zeta;

import com.bica.reborn.algebraic.AlgebraicChecker;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Session Dirichlet zeta function and Euler product (Step 32l) -- Java port of
 * {@code reticulate/reticulate/session_zeta.py}.
 *
 * <p>For a session-type lattice L = L(S) with principal downsets down(x), the
 * session zeta function is the Dirichlet series
 * <pre>
 *     zeta_S(s) = sum_{x in L} 1 / n_x^s,    n_x = |down(x)|.
 * </pre>
 * The central theorem is the Euler product:
 * <pre>
 *     zeta_{S1 || S2}(s) = zeta_{S1}(s) * zeta_{S2}(s).
 * </pre>
 *
 * <p>All arithmetic is exact (BigInteger rationals via {@link Rational}).
 */
public final class SessionZetaChecker {

    private SessionZetaChecker() {}

    // -----------------------------------------------------------------------
    // Exact rational
    // -----------------------------------------------------------------------

    /** Minimal exact rational backed by BigInteger. */
    public static final class Rational {
        public final BigInteger num;
        public final BigInteger den; // > 0

        private Rational(BigInteger n, BigInteger d) {
            if (d.signum() == 0) throw new ArithmeticException("zero denominator");
            if (d.signum() < 0) { n = n.negate(); d = d.negate(); }
            BigInteger g = n.gcd(d);
            if (!g.equals(BigInteger.ONE)) { n = n.divide(g); d = d.divide(g); }
            this.num = n;
            this.den = d;
        }

        public static Rational of(long n) { return new Rational(BigInteger.valueOf(n), BigInteger.ONE); }
        public static Rational of(long n, long d) { return new Rational(BigInteger.valueOf(n), BigInteger.valueOf(d)); }
        public static Rational of(BigInteger n, BigInteger d) { return new Rational(n, d); }
        public static final Rational ZERO = Rational.of(0);

        public Rational add(Rational o) {
            return new Rational(num.multiply(o.den).add(o.num.multiply(den)), den.multiply(o.den));
        }
        public Rational multiply(Rational o) {
            return new Rational(num.multiply(o.num), den.multiply(o.den));
        }

        @Override public boolean equals(Object o) {
            if (!(o instanceof Rational r)) return false;
            return num.equals(r.num) && den.equals(r.den);
        }
        @Override public int hashCode() { return Objects.hash(num, den); }
        @Override public String toString() {
            return den.equals(BigInteger.ONE) ? num.toString() : num + "/" + den;
        }
    }

    // -----------------------------------------------------------------------
    // Downset sizes
    // -----------------------------------------------------------------------

    /** Sorted (non-decreasing) list of principal downset sizes |down(x)|. */
    public static List<Integer> downsetSizes(StateSpace ss) {
        Map<Integer, Set<Integer>> reach = AlgebraicChecker.reachability(ss);
        List<Integer> states = AlgebraicChecker.stateList(ss);
        List<Integer> sizes = new ArrayList<>(states.size());
        for (int s : states) sizes.add(reach.get(s).size());
        Collections.sort(sizes);
        return sizes;
    }

    // -----------------------------------------------------------------------
    // ZetaSeries: finite Dirichlet series with integer multiplicities
    // -----------------------------------------------------------------------

    /**
     * Finite Dirichlet series sum_{n} c_n / n^s, represented as a sorted map
     * n -> c_n. Multiplication is Dirichlet convolution with Kronecker keying:
     * keys multiply, multiplicities multiply and add.
     */
    public static final class ZetaSeries {
        // Sorted, non-zero multiplicities only.
        public final TreeMap<Integer, Integer> coeffs;

        private ZetaSeries(TreeMap<Integer, Integer> c) { this.coeffs = c; }

        public static ZetaSeries fromSizes(List<Integer> sizes) {
            TreeMap<Integer, Integer> d = new TreeMap<>();
            for (int n : sizes) d.merge(n, 1, Integer::sum);
            return new ZetaSeries(d);
        }

        public static ZetaSeries fromMap(Map<Integer, Integer> m) {
            TreeMap<Integer, Integer> d = new TreeMap<>();
            for (Map.Entry<Integer, Integer> e : m.entrySet()) {
                if (e.getValue() != 0) d.put(e.getKey(), e.getValue());
            }
            return new ZetaSeries(d);
        }

        public Map<Integer, Integer> asMap() {
            return new LinkedHashMap<>(coeffs);
        }

        public int totalMass() {
            int t = 0;
            for (int c : coeffs.values()) t += c;
            return t;
        }

        /** Evaluate zeta(s) exactly as a Rational for integer s (s >= 0 supported, s < 0 too). */
        public Rational evaluate(int s) {
            Rational total = Rational.ZERO;
            for (Map.Entry<Integer, Integer> e : coeffs.entrySet()) {
                int n = e.getKey();
                int c = e.getValue();
                BigInteger pow = BigInteger.valueOf(n).pow(Math.abs(s));
                Rational term = (s >= 0)
                        ? Rational.of(BigInteger.valueOf(c), pow)
                        : Rational.of(BigInteger.valueOf(c).multiply(pow), BigInteger.ONE);
                total = total.add(term);
            }
            return total;
        }

        public ZetaSeries multiply(ZetaSeries other) {
            TreeMap<Integer, Integer> out = new TreeMap<>();
            for (Map.Entry<Integer, Integer> a : coeffs.entrySet()) {
                for (Map.Entry<Integer, Integer> b : other.coeffs.entrySet()) {
                    long k = (long) a.getKey() * (long) b.getKey();
                    if (k > Integer.MAX_VALUE) throw new ArithmeticException("series key overflow");
                    out.merge((int) k, a.getValue() * b.getValue(), Integer::sum);
                }
            }
            return new ZetaSeries(out);
        }

        @Override public boolean equals(Object o) {
            if (!(o instanceof ZetaSeries z)) return false;
            return coeffs.equals(z.coeffs);
        }
        @Override public int hashCode() { return coeffs.hashCode(); }
        @Override public String toString() { return coeffs.toString(); }
    }

    // -----------------------------------------------------------------------
    // Session zeta
    // -----------------------------------------------------------------------

    public static ZetaSeries sessionZetaSeries(StateSpace ss) {
        return ZetaSeries.fromSizes(downsetSizes(ss));
    }

    public static Rational sessionZeta(StateSpace ss, int s) {
        return sessionZetaSeries(ss).evaluate(s);
    }

    // -----------------------------------------------------------------------
    // Euler product verification
    // -----------------------------------------------------------------------

    public record PointCheck(int s, Rational lhs, Rational rhs, boolean ok) {}

    public record EulerVerification(
            List<Integer> leftSizes,
            List<Integer> rightSizes,
            List<Integer> productSizes,
            boolean seriesEqual,
            List<PointCheck> pointChecks,
            boolean allPointsEqual) {
        public EulerVerification {
            leftSizes = List.copyOf(leftSizes);
            rightSizes = List.copyOf(rightSizes);
            productSizes = List.copyOf(productSizes);
            pointChecks = List.copyOf(pointChecks);
        }
    }

    public static EulerVerification verifyEulerProduct(
            StateSpace left, StateSpace right, StateSpace product) {
        return verifyEulerProduct(left, right, product, new int[]{1, 2, 3});
    }

    public static EulerVerification verifyEulerProduct(
            StateSpace left, StateSpace right, StateSpace product, int[] testValues) {
        ZetaSeries zL = sessionZetaSeries(left);
        ZetaSeries zR = sessionZetaSeries(right);
        ZetaSeries zP = sessionZetaSeries(product);
        ZetaSeries zLR = zL.multiply(zR);
        boolean seriesOk = zLR.equals(zP);

        List<PointCheck> pts = new ArrayList<>();
        boolean allOk = true;
        for (int s : testValues) {
            Rational lhs = zP.evaluate(s);
            Rational rhs = zLR.evaluate(s);
            boolean ok = lhs.equals(rhs);
            allOk &= ok;
            pts.add(new PointCheck(s, lhs, rhs, ok));
        }
        return new EulerVerification(
                downsetSizes(left), downsetSizes(right), downsetSizes(product),
                seriesOk, pts, allOk);
    }

    // -----------------------------------------------------------------------
    // Irreducibility / factorization
    // -----------------------------------------------------------------------

    public record EulerFactorization(
            ZetaSeries original,
            List<ZetaSeries> factors,
            ZetaSeries product,
            boolean matches) {
        public EulerFactorization {
            factors = List.copyOf(factors);
        }
        public boolean isTrivial() { return factors.size() <= 1; }
    }

    public static boolean isZetaIrreducible(StateSpace ss) {
        return seriesIrreducible(sessionZetaSeries(ss));
    }

    public static boolean seriesIrreducible(ZetaSeries Z) {
        int mass = Z.totalMass();
        if (mass <= 1) return true;
        for (int a = 2; a <= mass; a++) {
            if (mass % a != 0) continue;
            int b = mass / a;
            if (a > b) break;
            ZetaSeries[] split = trySplit(Z, a, b);
            if (split != null) return false;
        }
        return true;
    }

    /** Returns {A, B} with A*B == Z, |A| = massA, |B| = massB; null if none. */
    public static ZetaSeries[] trySplit(ZetaSeries Z, int massA, int massB) {
        Map<Integer, Integer> Zd = Z.asMap();
        if (Zd.isEmpty()) return null;
        int maxKey = 0;
        for (int k : Zd.keySet()) if (k > maxKey) maxKey = k;

        // Divisors of maxKey
        List<Integer> divisors = new ArrayList<>();
        for (int d = 1; d <= maxKey; d++) if (maxKey % d == 0) divisors.add(d);
        if (!divisors.contains(1)) return null;

        // Restrict to divisors that divide some key in Z
        List<Integer> candidateNs = new ArrayList<>();
        for (int d : divisors) {
            for (int k : Zd.keySet()) {
                if (k % d == 0) { candidateNs.add(d); break; }
            }
        }

        int maxSubSize = Math.min(candidateNs.size(), massA);
        for (int size = 1; size <= maxSubSize; size++) {
            for (List<Integer> combo : combinations(candidateNs, size)) {
                if (combo.get(0) != 1) continue;
                for (List<Integer> mult : compositions(massA, size)) {
                    TreeMap<Integer, Integer> Adict = new TreeMap<>();
                    for (int i = 0; i < size; i++) {
                        Adict.merge(combo.get(i), mult.get(i), Integer::sum);
                    }
                    ZetaSeries A = new ZetaSeries(Adict);
                    ZetaSeries B = quotientSeries(Z, A, massB);
                    if (B != null && A.multiply(B).equals(Z)
                            && A.totalMass() >= 2 && B.totalMass() >= 2) {
                        return new ZetaSeries[]{A, B};
                    }
                }
            }
        }
        return null;
    }

    public static List<List<Integer>> compositions(int total, int parts) {
        List<List<Integer>> out = new ArrayList<>();
        if (parts == 1) {
            if (total >= 1) out.add(List.of(total));
            return out;
        }
        for (int first = 1; first <= total - parts + 1; first++) {
            for (List<Integer> rest : compositions(total - first, parts - 1)) {
                List<Integer> next = new ArrayList<>(parts);
                next.add(first);
                next.addAll(rest);
                out.add(next);
            }
        }
        return out;
    }

    private static List<List<Integer>> combinations(List<Integer> items, int k) {
        List<List<Integer>> out = new ArrayList<>();
        combine(items, k, 0, new ArrayList<>(), out);
        return out;
    }

    private static void combine(List<Integer> items, int k, int start,
                                List<Integer> cur, List<List<Integer>> out) {
        if (cur.size() == k) { out.add(new ArrayList<>(cur)); return; }
        for (int i = start; i <= items.size() - (k - cur.size()); i++) {
            cur.add(items.get(i));
            combine(items, k, i + 1, cur, out);
            cur.remove(cur.size() - 1);
        }
    }

    /** Compute B such that A * B = Z, with totalMass(B) = expectedMass; null otherwise. */
    public static ZetaSeries quotientSeries(ZetaSeries Z, ZetaSeries A, int expectedMass) {
        TreeMap<Integer, Integer> remaining = new TreeMap<>(Z.coeffs);
        List<Map.Entry<Integer, Integer>> Alist = new ArrayList<>(A.coeffs.entrySet());
        if (Alist.isEmpty()) return null;
        Map.Entry<Integer, Integer> a1 = Alist.get(0);
        if (a1.getKey() != 1) return null;
        int a1mult = a1.getValue();
        TreeMap<Integer, Integer> Bdict = new TreeMap<>();

        while (!remaining.isEmpty()) {
            int kMin = remaining.firstKey();
            int rem = remaining.get(kMin);
            if (rem % a1mult != 0) return null;
            int bMult = rem / a1mult;
            Bdict.merge(kMin, bMult, Integer::sum);
            for (Map.Entry<Integer, Integer> ae : Alist) {
                long key = (long) ae.getKey() * (long) kMin;
                if (key > Integer.MAX_VALUE) return null;
                int ikey = (int) key;
                Integer cur = remaining.get(ikey);
                if (cur == null) return null;
                int next = cur - ae.getValue() * bMult;
                if (next < 0) return null;
                if (next == 0) remaining.remove(ikey);
                else remaining.put(ikey, next);
            }
        }
        int total = 0;
        for (int v : Bdict.values()) total += v;
        if (total != expectedMass) return null;
        return new ZetaSeries(Bdict);
    }

    public static EulerFactorization eulerFactor(StateSpace ss) {
        ZetaSeries Z = sessionZetaSeries(ss);
        List<ZetaSeries> factors = factorSeries(Z);
        ZetaSeries product = factors.get(0);
        for (int i = 1; i < factors.size(); i++) product = product.multiply(factors.get(i));
        return new EulerFactorization(Z, factors, product, product.equals(Z));
    }

    private static List<ZetaSeries> factorSeries(ZetaSeries Z) {
        List<ZetaSeries> out = new ArrayList<>();
        if (Z.totalMass() <= 1 || seriesIrreducible(Z)) {
            out.add(Z);
            return out;
        }
        int mass = Z.totalMass();
        for (int a = 2; a <= mass; a++) {
            if (mass % a != 0) continue;
            int b = mass / a;
            if (a > b) break;
            ZetaSeries[] split = trySplit(Z, a, b);
            if (split != null) {
                out.addAll(factorSeries(split[0]));
                out.addAll(factorSeries(split[1]));
                return out;
            }
        }
        out.add(Z);
        return out;
    }

    // -----------------------------------------------------------------------
    // Convenience
    // -----------------------------------------------------------------------

    public static EulerFactorization eulerFactor(String sessionTypeString) {
        SessionType ast = Parser.parse(sessionTypeString);
        StateSpace ss = StateSpaceBuilder.build(ast);
        return eulerFactor(ss);
    }
}
