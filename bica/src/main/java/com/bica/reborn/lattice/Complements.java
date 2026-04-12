package com.bica.reborn.lattice;

import com.bica.reborn.lattice.DistributivityChecker.QuotientPoset;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Complement analysis for session type lattices.
 *
 * <p>Step 363c: Analyses the complement structure of lattices derived from
 * session type state spaces. Provides complement finding, complemented
 * lattice classification, Boolean algebra detection, and relative
 * complementation.
 *
 * <p>Key concepts:
 * <ul>
 *   <li>Complement: a' such that a ∧ a' = ⊥ and a ∨ a' = ⊤</li>
 *   <li>Complemented lattice: every element has at least one complement</li>
 *   <li>Uniquely complemented: every element has exactly one complement</li>
 *   <li>Relatively complemented: every element in every interval has a complement</li>
 *   <li>Boolean: complemented + distributive (≅ 2^n for finite)</li>
 * </ul>
 *
 * <p>Ported from {@code reticulate/reticulate/complements.py}.
 */
public final class Complements {

    private Complements() {}

    // -- Result records --------------------------------------------------------

    /** A pair {@code (element, complement)} in a lattice. */
    public record ComplementPair(int element, int complement) {}

    /** Complement information for a single element. */
    public record ComplementInfo(
            int element,
            List<Integer> complements,
            boolean hasComplement,
            boolean isUnique) {

        public ComplementInfo {
            complements = List.copyOf(complements);
        }
    }

    /** A relative complement within an interval {@code [lower, upper]}. */
    public record RelativeComplement(int element, int complement, int lower, int upper) {}

    /** Complete complement analysis of a lattice. */
    public record ComplementAnalysis(
            boolean isComplemented,
            boolean isUniquelyComplemented,
            boolean isRelativelyComplemented,
            boolean isBoolean,
            boolean isDistributive,
            Map<Integer, List<Integer>> complementMap,
            List<Integer> uncomplementedElements,
            int complementCount,
            int elementCount,
            Integer booleanRank) {

        public ComplementAnalysis {
            Map<Integer, List<Integer>> copy = new LinkedHashMap<>();
            for (var e : complementMap.entrySet()) {
                copy.put(e.getKey(), List.copyOf(e.getValue()));
            }
            complementMap = Collections.unmodifiableMap(copy);
            uncomplementedElements = List.copyOf(uncomplementedElements);
        }
    }

    // -- Internal helper view ---------------------------------------------------

    /** Snapshot of quotient operations keyed by representative original state IDs. */
    private static final class Ops {
        final List<Integer> nodes; // sorted rep IDs (original state IDs)
        final int top;
        final int bottom;
        final Map<Integer, QuotientPoset> unused = null; // placeholder kept only for clarity
        final QuotientPoset q;
        /** rep ID -> internal QuotientPoset node index. */
        final Map<Integer, Integer> repToNode;

        Ops(QuotientPoset q) {
            this.q = q;
            this.repToNode = new HashMap<>();
            for (int n : q.nodes()) {
                repToNode.put(q.rep().get(n), n);
            }
            List<Integer> reps = new ArrayList<>(repToNode.keySet());
            Collections.sort(reps);
            this.nodes = reps;
            this.top = q.rep().get(q.top());
            this.bottom = q.rep().get(q.bottom());
        }

        /** Map a rep ID (or any original state ID) to its rep ID via the scc map. */
        int toRep(int state, StateSpace ss) {
            // Look up in q.stateToNode, then back to rep.
            Integer node = q.stateToNode().get(state);
            if (node == null) return state;
            return q.rep().get(node);
        }

        Integer meetRep(int aRep, int bRep) {
            Integer aNode = repToNode.get(aRep);
            Integer bNode = repToNode.get(bRep);
            if (aNode == null || bNode == null) return null;
            Integer m = q.meet(aNode, bNode);
            return m == null ? null : q.rep().get(m);
        }

        Integer joinRep(int aRep, int bRep) {
            Integer aNode = repToNode.get(aRep);
            Integer bNode = repToNode.get(bRep);
            if (aNode == null || bNode == null) return null;
            Integer j = q.join(aNode, bNode);
            return j == null ? null : q.rep().get(j);
        }

        /** True iff upperRep >= lowerRep, i.e. upper reaches lower in the fwd DAG. */
        boolean ge(int upperRep, int lowerRep) {
            Integer uNode = repToNode.get(upperRep);
            Integer lNode = repToNode.get(lowerRep);
            if (uNode == null || lNode == null) return false;
            return q.fwdReach().get(uNode).contains(lNode);
        }
    }

    private static Ops buildOps(StateSpace ss) {
        return new Ops(QuotientPoset.build(ss));
    }

    // -- Public API -------------------------------------------------------------

    /**
     * Find all complements of an element in the lattice.
     *
     * <p>A complement {@code a'} of element {@code a} satisfies:
     * {@code a ∧ a' = ⊥} and {@code a ∨ a' = ⊤}.
     *
     * @return representative state IDs of the complements (possibly empty)
     */
    public static List<Integer> findComplements(StateSpace ss, int element) {
        return findComplements(ss, element, null);
    }

    public static List<Integer> findComplements(StateSpace ss, int element, LatticeResult lr) {
        if (lr == null) lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) return List.of();

        Ops ops = buildOps(ss);
        int elemRep = ops.toRep(element, ss);
        List<Integer> complements = new ArrayList<>();
        for (int b : ops.nodes) {
            Integer m = ops.meetRep(elemRep, b);
            Integer j = ops.joinRep(elemRep, b);
            if (m != null && m == ops.bottom && j != null && j == ops.top) {
                complements.add(b);
            }
        }
        return complements;
    }

    /** Get detailed complement information for a single element. */
    public static ComplementInfo complementInfo(StateSpace ss, int element) {
        return complementInfo(ss, element, null);
    }

    public static ComplementInfo complementInfo(StateSpace ss, int element, LatticeResult lr) {
        List<Integer> comps = findComplements(ss, element, lr);
        return new ComplementInfo(
                element,
                comps,
                !comps.isEmpty(),
                comps.size() == 1);
    }

    /**
     * Find relative complements of an element within an interval {@code [lower, upper]}.
     *
     * <p>A relative complement {@code c} of {@code a} in {@code [lower, upper]} satisfies:
     * {@code a ∧ c = lower} and {@code a ∨ c = upper}.
     */
    public static List<Integer> findRelativeComplement(
            StateSpace ss, int element, int lower, int upper) {
        return findRelativeComplement(ss, element, lower, upper, null);
    }

    public static List<Integer> findRelativeComplement(
            StateSpace ss, int element, int lower, int upper, LatticeResult lr) {
        if (lr == null) lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) return List.of();

        Ops ops = buildOps(ss);
        int elemRep = ops.toRep(element, ss);
        int lowerRep = ops.toRep(lower, ss);
        int upperRep = ops.toRep(upper, ss);

        List<Integer> interval = new ArrayList<>();
        for (int n : ops.nodes) {
            if (ops.ge(upperRep, n) && ops.ge(n, lowerRep)) {
                interval.add(n);
            }
        }
        if (!interval.contains(elemRep)) return List.of();

        List<Integer> complements = new ArrayList<>();
        for (int c : interval) {
            Integer m = ops.meetRep(elemRep, c);
            Integer j = ops.joinRep(elemRep, c);
            if (m != null && m == lowerRep && j != null && j == upperRep) {
                complements.add(c);
            }
        }
        return complements;
    }

    /** Check if the lattice is complemented (every element has a complement). */
    public static boolean isComplemented(StateSpace ss) {
        return isComplemented(ss, null);
    }

    public static boolean isComplemented(StateSpace ss, LatticeResult lr) {
        if (lr == null) lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) return false;

        Ops ops = buildOps(ss);
        for (int a : ops.nodes) {
            boolean found = false;
            for (int b : ops.nodes) {
                Integer m = ops.meetRep(a, b);
                Integer j = ops.joinRep(a, b);
                if (m != null && m == ops.bottom && j != null && j == ops.top) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    /** Check if every element has exactly one complement. */
    public static boolean isUniquelyComplemented(StateSpace ss) {
        return isUniquelyComplemented(ss, null);
    }

    public static boolean isUniquelyComplemented(StateSpace ss, LatticeResult lr) {
        if (lr == null) lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) return false;

        Ops ops = buildOps(ss);
        for (int a : ops.nodes) {
            int count = 0;
            for (int b : ops.nodes) {
                Integer m = ops.meetRep(a, b);
                Integer j = ops.joinRep(a, b);
                if (m != null && m == ops.bottom && j != null && j == ops.top) {
                    count++;
                }
            }
            if (count != 1) return false;
        }
        return true;
    }

    /**
     * Check whether the lattice is relatively complemented: every element in
     * every interval {@code [lower, upper]} has a complement within that interval.
     */
    public static boolean isRelativelyComplemented(StateSpace ss) {
        return isRelativelyComplemented(ss, null);
    }

    public static boolean isRelativelyComplemented(StateSpace ss, LatticeResult lr) {
        if (lr == null) lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) return false;

        Ops ops = buildOps(ss);
        for (int lower : ops.nodes) {
            for (int upper : ops.nodes) {
                if (!ops.ge(upper, lower)) continue;
                List<Integer> interval = new ArrayList<>();
                for (int n : ops.nodes) {
                    if (ops.ge(upper, n) && ops.ge(n, lower)) interval.add(n);
                }
                for (int elem : interval) {
                    boolean found = false;
                    for (int c : interval) {
                        Integer m = ops.meetRep(elem, c);
                        Integer j = ops.joinRep(elem, c);
                        if (m != null && m == lower && j != null && j == upper) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) return false;
                }
            }
        }
        return true;
    }

    /**
     * Check whether the lattice is Boolean (complemented + distributive).
     * A finite Boolean algebra is isomorphic to 2^n for some n.
     */
    public static boolean isBoolean(StateSpace ss) {
        return isBoolean(ss, null);
    }

    public static boolean isBoolean(StateSpace ss, LatticeResult lr) {
        if (lr == null) lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) return false;
        DistributivityResult dr = DistributivityChecker.checkDistributive(ss);
        if (!dr.isDistributive()) return false;
        return isComplemented(ss, lr);
    }

    /**
     * If the lattice is Boolean, return {@code n} where {@code L ≅ 2^n}, else {@code null}.
     */
    public static Integer booleanRank(StateSpace ss) {
        return booleanRank(ss, null);
    }

    public static Integer booleanRank(StateSpace ss, LatticeResult lr) {
        if (lr == null) lr = LatticeChecker.checkLattice(ss);
        if (!isBoolean(ss, lr)) return null;
        Ops ops = buildOps(ss);
        int n = ops.nodes.size();
        if (n <= 0) return null;
        int k = (int) (Math.log(n) / Math.log(2));
        // handle off-by-one from floating point
        while ((1 << (k + 1)) <= n) k++;
        while (k > 0 && (1 << k) > n) k--;
        if ((1 << k) == n) return k;
        return null;
    }

    /** Perform a complete complement analysis of the lattice. */
    public static ComplementAnalysis analyzeComplements(StateSpace ss) {
        return analyzeComplements(ss, null);
    }

    public static ComplementAnalysis analyzeComplements(StateSpace ss, LatticeResult lr) {
        if (lr == null) lr = LatticeChecker.checkLattice(ss);
        if (!lr.isLattice()) {
            return new ComplementAnalysis(
                    false, false, false, false, false,
                    Map.of(),
                    new ArrayList<>(ss.states()),
                    0,
                    ss.states().size(),
                    null);
        }

        Ops ops = buildOps(ss);
        Map<Integer, List<Integer>> compMap = new LinkedHashMap<>();
        List<Integer> uncomplemented = new ArrayList<>();
        int totalComplements = 0;

        for (int a : ops.nodes) {
            List<Integer> comps = new ArrayList<>();
            for (int b : ops.nodes) {
                Integer m = ops.meetRep(a, b);
                Integer j = ops.joinRep(a, b);
                if (m != null && m == ops.bottom && j != null && j == ops.top) {
                    comps.add(b);
                }
            }
            compMap.put(a, comps);
            totalComplements += comps.size();
            if (comps.isEmpty()) uncomplemented.add(a);
        }

        boolean complemented = uncomplemented.isEmpty();
        boolean uniquelyComp = complemented
                && compMap.values().stream().allMatch(v -> v.size() == 1);
        DistributivityResult dr = DistributivityChecker.checkDistributive(ss);
        boolean dist = dr.isDistributive();
        boolean bool = complemented && dist;

        Integer bRank = null;
        if (bool) {
            int n = ops.nodes.size();
            int k = n > 0 ? (int) (Math.log(n) / Math.log(2)) : 0;
            while ((1 << (k + 1)) <= n) k++;
            while (k > 0 && (1 << k) > n) k--;
            if ((1 << k) == n) bRank = k;
        }

        boolean relComp = complemented && isRelativelyComplemented(ss, lr);

        return new ComplementAnalysis(
                complemented,
                uniquelyComp,
                relComp,
                bool,
                dist,
                compMap,
                uncomplemented,
                totalComplements,
                ops.nodes.size(),
                bRank);
    }
}
