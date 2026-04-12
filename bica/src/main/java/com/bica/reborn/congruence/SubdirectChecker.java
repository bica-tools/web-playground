package com.bica.reborn.congruence;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.statespace.StateSpace;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Subdirect decomposition of session type lattices (Step 363d).
 *
 * <p>Implements Birkhoff's Subdirect Representation Theorem: every lattice is
 * isomorphic to a subdirect product of subdirectly irreducible (SI) lattices.
 * The factors are quotients L/θ for completely meet-irreducible congruences θ
 * in Con(L).
 *
 * <p>A lattice is <b>subdirectly irreducible</b> iff Con(L) has a unique atom
 * (smallest non-trivial congruence). Simple lattices are always SI.
 *
 * <p>Operationally, decomposing L(S) into SI factors identifies the minimal
 * independent protocol concerns.
 */
public final class SubdirectChecker {

    private SubdirectChecker() {}

    // -----------------------------------------------------------------------
    // Result records
    // -----------------------------------------------------------------------

    /**
     * A single factor in a subdirect decomposition.
     *
     * @param congruence    The completely meet-irreducible congruence θ.
     * @param quotient      The quotient lattice L/θ.
     * @param quotientSize  Number of states in the quotient.
     * @param isSimpleFactor True iff the quotient is a simple lattice.
     */
    public record SubdirectFactor(
            Congruence congruence,
            StateSpace quotient,
            int quotientSize,
            boolean isSimpleFactor) {

        public SubdirectFactor {
            Objects.requireNonNull(congruence, "congruence must not be null");
            Objects.requireNonNull(quotient, "quotient must not be null");
        }
    }

    /**
     * Complete subdirect decomposition analysis.
     *
     * @param isSubdirectlyIrreducible True iff Con(L) has a unique atom.
     * @param isSimple                 True iff Con(L) = {⊥, ⊤}.
     * @param numFactors               Number of decomposition factors.
     * @param factors                  The factors themselves.
     * @param isDirectProduct          True iff |L| = ∏|factor|.
     * @param conLatticeSize           |Con(L)|.
     * @param numMeetIrreducibles      Number of completely meet-irreducible congruences.
     * @param originalSize             |L|.
     */
    public record SubdirectAnalysis(
            boolean isSubdirectlyIrreducible,
            boolean isSimple,
            int numFactors,
            List<SubdirectFactor> factors,
            boolean isDirectProduct,
            int conLatticeSize,
            int numMeetIrreducibles,
            int originalSize) {

        public SubdirectAnalysis {
            Objects.requireNonNull(factors, "factors must not be null");
            factors = List.copyOf(factors);
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /** Atoms (minimal non-bottom elements) of Con(L), as indices. */
    private static List<Integer> atomsOfCon(CongruenceChecker.CongruenceLatticeResult conLat) {
        int n = conLat.congruences().size();
        if (n <= 1) return List.of();

        int bot = conLat.bottom();
        var ord = conLat.ordering();

        List<Integer> aboveBot = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (i != bot && ord.contains(List.of(bot, i))) aboveBot.add(i);
        }

        List<Integer> atoms = new ArrayList<>();
        for (int i : aboveBot) {
            boolean isAtom = true;
            for (int j : aboveBot) {
                if (j != i && ord.contains(List.of(bot, j)) && ord.contains(List.of(j, i))) {
                    isAtom = false;
                    break;
                }
            }
            if (isAtom) atoms.add(i);
        }
        return atoms;
    }

    /** Completely meet-irreducible congruence indices: those with exactly one upper cover. */
    private static List<Integer> meetIrreduciblesOfCon(
            CongruenceChecker.CongruenceLatticeResult conLat) {
        int n = conLat.congruences().size();
        if (n <= 1) return List.of();

        var ord = conLat.ordering();
        List<Integer> meetIrr = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            List<Integer> aboveI = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if (j != i && ord.contains(List.of(i, j))) aboveI.add(j);
            }
            if (aboveI.isEmpty()) continue; // top

            List<Integer> covers = new ArrayList<>();
            for (int j : aboveI) {
                boolean isCover = true;
                for (int k : aboveI) {
                    if (k != j && ord.contains(List.of(i, k)) && ord.contains(List.of(k, j))) {
                        isCover = false;
                        break;
                    }
                }
                if (isCover) covers.add(j);
            }
            if (covers.size() == 1) meetIrr.add(i);
        }
        return meetIrr;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Check if the lattice is subdirectly irreducible.
     * SI iff Con(L) has a unique atom. Simple lattices are always SI.
     */
    public static boolean isSubdirectlyIrreducible(StateSpace ss) {
        return isSubdirectlyIrreducible(ss, null);
    }

    public static boolean isSubdirectlyIrreducible(StateSpace ss, LatticeResult lr) {
        LatticeResult result = (lr == null) ? LatticeChecker.checkLattice(ss) : lr;
        if (!result.isLattice()) return false;
        if (CongruenceChecker.isSimple(ss)) return true;

        var conLat = CongruenceChecker.congruenceLattice(ss);
        return atomsOfCon(conLat).size() == 1;
    }

    /**
     * Compute the subdirect decomposition factors.
     *
     * <p>Returns the quotients L/θ for each completely meet-irreducible
     * congruence θ in Con(L). By Birkhoff's theorem, L embeds as a subdirect
     * product of these factors.
     */
    public static List<SubdirectFactor> subdirectFactors(StateSpace ss) {
        return subdirectFactors(ss, null);
    }

    public static List<SubdirectFactor> subdirectFactors(StateSpace ss, LatticeResult lr) {
        LatticeResult result = (lr == null) ? LatticeChecker.checkLattice(ss) : lr;
        if (!result.isLattice()) return List.of();

        var conLat = CongruenceChecker.congruenceLattice(ss);
        List<Integer> miIndices = meetIrreduciblesOfCon(conLat);

        if (miIndices.isEmpty()) {
            Congruence dummy = conLat.congruences().isEmpty()
                    ? new Congruence(List.of(), 0, List.of())
                    : conLat.congruences().get(conLat.bottom());
            return List.of(new SubdirectFactor(dummy, ss, ss.states().size(), true));
        }

        List<SubdirectFactor> factors = new ArrayList<>();
        for (int idx : miIndices) {
            Congruence theta = conLat.congruences().get(idx);
            if (theta.isTrivialBottom()) continue;
            StateSpace q = CongruenceChecker.quotientLattice(ss, theta);
            factors.add(new SubdirectFactor(
                    theta, q, q.states().size(), CongruenceChecker.isSimple(q)));
        }

        if (factors.isEmpty()) {
            Congruence dummy = conLat.congruences().isEmpty()
                    ? new Congruence(List.of(), 0, List.of())
                    : conLat.congruences().get(conLat.bottom());
            return List.of(new SubdirectFactor(
                    dummy, ss, ss.states().size(), CongruenceChecker.isSimple(ss)));
        }

        return factors;
    }

    private static boolean checkDirectProduct(StateSpace ss, List<SubdirectFactor> factors) {
        if (factors.size() <= 1) return true;
        long productSize = 1;
        for (var f : factors) productSize *= f.quotientSize();
        return productSize == ss.states().size();
    }

    /** Perform complete subdirect decomposition analysis. */
    public static SubdirectAnalysis analyzeSubdirect(StateSpace ss) {
        return analyzeSubdirect(ss, null);
    }

    public static SubdirectAnalysis analyzeSubdirect(StateSpace ss, LatticeResult lr) {
        LatticeResult result = (lr == null) ? LatticeChecker.checkLattice(ss) : lr;
        if (!result.isLattice()) {
            return new SubdirectAnalysis(
                    false, false, 0, List.of(), false, 0, 0, ss.states().size());
        }

        boolean simple = CongruenceChecker.isSimple(ss);
        boolean si = isSubdirectlyIrreducible(ss, result);
        var conLat = CongruenceChecker.congruenceLattice(ss);
        List<Integer> miIndices = meetIrreduciblesOfCon(conLat);
        List<SubdirectFactor> facts = subdirectFactors(ss, result);
        boolean isDirect = checkDirectProduct(ss, facts);

        return new SubdirectAnalysis(
                si, simple, facts.size(), facts, isDirect,
                conLat.congruences().size(), miIndices.size(), ss.states().size());
    }
}
