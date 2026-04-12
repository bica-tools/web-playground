package com.bica.reborn.monoidal;

import com.bica.reborn.monoidal.MonoidalResult.*;
import com.bica.reborn.morphism.Morphism;
import com.bica.reborn.morphism.MorphismChecker;
import com.bica.reborn.statespace.ProductStateSpace;
import com.bica.reborn.statespace.StateSpace;

import java.util.*;

/**
 * Monoidal structure of session type composition.
 *
 * <p>Verifies that the category <b>SessLat</b> with parallel composition (product)
 * as the tensor and End as the unit forms a <b>symmetric monoidal category</b>.
 *
 * <p>Structural laws:
 * <ol>
 *   <li>Unit: L(S || end) ≅ L(S) ≅ L(end || S)</li>
 *   <li>Associativity: L((S1 || S2) || S3) ≅ L(S1 || (S2 || S3))</li>
 *   <li>Symmetry: L(S1 || S2) ≅ L(S2 || S1)</li>
 *   <li>Coherence: Mac Lane's pentagon and triangle diagrams commute</li>
 * </ol>
 */
public final class MonoidalChecker {

    private MonoidalChecker() {}

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Construct the trivial 1-state state space for End. */
    private static StateSpace endStateSpace() {
        return new StateSpace(
                Set.of(0), List.of(), 0, 0, Map.of(0, "end"));
    }

    /** Find a structural isomorphism mapping or null. */
    private static Map<Integer, Integer> structuralIso(StateSpace ss1, StateSpace ss2) {
        Morphism m = MorphismChecker.findIsomorphism(ss1, ss2);
        return m != null ? m.mapping() : null;
    }

    // =========================================================================
    // Unit check
    // =========================================================================

    /**
     * Verify that End is the monoidal unit for parallel composition.
     */
    public static UnitResult checkMonoidalUnit(StateSpace ss) {
        StateSpace end = endStateSpace();

        StateSpace leftProduct = ProductStateSpace.product(end, ss);
        Map<Integer, Integer> leftIso = structuralIso(leftProduct, ss);

        StateSpace rightProduct = ProductStateSpace.product(ss, end);
        Map<Integer, Integer> rightIso = structuralIso(rightProduct, ss);

        return new UnitResult(
                leftIso != null, rightIso != null, leftIso, rightIso);
    }

    // =========================================================================
    // Associativity check
    // =========================================================================

    /**
     * Verify associativity of parallel composition up to isomorphism.
     */
    public static AssociativityResult checkAssociativity(
            StateSpace ss1, StateSpace ss2, StateSpace ss3) {
        StateSpace leftInner = ProductStateSpace.product(ss1, ss2);
        StateSpace leftAssoc = ProductStateSpace.product(leftInner, ss3);

        StateSpace rightInner = ProductStateSpace.product(ss2, ss3);
        StateSpace rightAssoc = ProductStateSpace.product(ss1, rightInner);

        Map<Integer, Integer> iso = structuralIso(leftAssoc, rightAssoc);

        return new AssociativityResult(
                iso != null, iso,
                leftAssoc.states().size(),
                rightAssoc.states().size());
    }

    /**
     * Return the associator isomorphism.
     */
    public static Map<Integer, Integer> associator(
            StateSpace ss1, StateSpace ss2, StateSpace ss3) {
        var result = checkAssociativity(ss1, ss2, ss3);
        return result.iso();
    }

    // =========================================================================
    // Symmetry (braiding) check
    // =========================================================================

    /**
     * Verify symmetry (braiding) of parallel composition.
     */
    public static SymmetryResult checkSymmetry(StateSpace ss1, StateSpace ss2) {
        StateSpace prod12 = ProductStateSpace.product(ss1, ss2);
        StateSpace prod21 = ProductStateSpace.product(ss2, ss1);

        Map<Integer, Integer> iso1221 = structuralIso(prod12, prod21);

        boolean isInvolution = false;
        if (iso1221 != null) {
            Map<Integer, Integer> iso2112 = structuralIso(prod21, prod12);
            if (iso2112 != null) {
                isInvolution = true;
                for (int s : prod12.states()) {
                    int composed = iso2112.getOrDefault(iso1221.get(s), -1);
                    if (composed != s) {
                        isInvolution = false;
                        break;
                    }
                }
            }
        }

        return new SymmetryResult(iso1221 != null, iso1221, isInvolution);
    }

    // =========================================================================
    // Coherence check
    // =========================================================================

    /**
     * Verify Mac Lane coherence conditions.
     */
    public static CoherenceResult checkCoherence(
            StateSpace ss1, StateSpace ss2, StateSpace ss3) {
        boolean pentagon = checkPentagon(ss1, ss2, ss3, ss1);
        boolean triangle = checkTriangle(ss1, ss2);
        boolean hexagon = checkHexagon(ss1, ss2, ss3);

        String cx = null;
        if (!pentagon) cx = "Pentagon diagram does not commute";
        else if (!triangle) cx = "Triangle diagram does not commute";
        else if (!hexagon) cx = "Hexagon diagram does not commute";

        return new CoherenceResult(pentagon, triangle, hexagon, cx);
    }

    /**
     * Unit constraints: return the unit result.
     */
    public static UnitResult unitConstraints(StateSpace ss) {
        return checkMonoidalUnit(ss);
    }

    // =========================================================================
    // Full monoidal structure check
    // =========================================================================

    /**
     * Verify the full symmetric monoidal structure on SessLat.
     */
    public static MonoidalResult checkMonoidalStructure(
            StateSpace ss1, StateSpace ss2, StateSpace ss3) {
        UnitResult unit = checkMonoidalUnit(ss1);
        AssociativityResult assoc = checkAssociativity(ss1, ss2, ss3);
        SymmetryResult sym = checkSymmetry(ss1, ss2);
        CoherenceResult coh = checkCoherence(ss1, ss2, ss3);

        boolean isMonoidal = unit.leftUnit() && unit.rightUnit()
                && assoc.isAssociative()
                && sym.isSymmetric() && sym.isInvolution()
                && coh.pentagon() && coh.triangle() && coh.hexagon();

        return new MonoidalResult(unit, assoc, sym, coh, isMonoidal);
    }

    // =========================================================================
    // Internal: coherence diagram checks
    // =========================================================================

    private static boolean checkPentagon(
            StateSpace a, StateSpace b, StateSpace c, StateSpace d) {
        StateSpace ab = ProductStateSpace.product(a, b);
        StateSpace abC = ProductStateSpace.product(ab, c);
        StateSpace abCD = ProductStateSpace.product(abC, d);

        StateSpace cd = ProductStateSpace.product(c, d);
        StateSpace bcd = ProductStateSpace.product(b, cd);
        StateSpace aBcd = ProductStateSpace.product(a, bcd);

        Map<Integer, Integer> iso = structuralIso(abCD, aBcd);
        if (iso == null) return false;

        // Check intermediate path
        StateSpace abCd = ProductStateSpace.product(ab, cd);
        Map<Integer, Integer> isoMid1 = structuralIso(abCD, abCd);
        Map<Integer, Integer> isoMid2 = structuralIso(abCd, aBcd);

        if (isoMid1 == null || isoMid2 == null) return false;

        // Verify composition matches direct iso
        for (int s : abCD.states()) {
            int composed = isoMid2.getOrDefault(isoMid1.get(s), -1);
            if (composed != iso.get(s)) return false;
        }
        return true;
    }

    private static boolean checkTriangle(StateSpace a, StateSpace b) {
        StateSpace end = endStateSpace();

        StateSpace aEnd = ProductStateSpace.product(a, end);
        StateSpace aEndB = ProductStateSpace.product(aEnd, b);

        StateSpace endB = ProductStateSpace.product(end, b);
        StateSpace aEndB2 = ProductStateSpace.product(a, endB);

        StateSpace aB = ProductStateSpace.product(a, b);

        Map<Integer, Integer> isoPath1 = structuralIso(aEndB, aB);
        Map<Integer, Integer> isoAlpha = structuralIso(aEndB, aEndB2);
        Map<Integer, Integer> isoLambda = structuralIso(aEndB2, aB);

        if (isoPath1 == null || isoAlpha == null || isoLambda == null) return false;

        for (int s : aEndB.states()) {
            int composed = isoLambda.getOrDefault(isoAlpha.get(s), -1);
            if (composed != isoPath1.get(s)) return false;
        }
        return true;
    }

    private static boolean checkHexagon(
            StateSpace a, StateSpace b, StateSpace c) {
        StateSpace bc = ProductStateSpace.product(b, c);
        StateSpace aBc = ProductStateSpace.product(a, bc);
        StateSpace ba = ProductStateSpace.product(b, a);
        StateSpace baC = ProductStateSpace.product(ba, c);
        StateSpace ac = ProductStateSpace.product(a, c);
        StateSpace bAc = ProductStateSpace.product(b, ac);
        StateSpace ca = ProductStateSpace.product(c, a);
        StateSpace bCa = ProductStateSpace.product(b, ca);

        Map<Integer, Integer> iso1 = structuralIso(aBc, baC);
        if (iso1 == null) return false;
        Map<Integer, Integer> iso2 = structuralIso(baC, bAc);
        if (iso2 == null) return false;
        Map<Integer, Integer> iso3 = structuralIso(bAc, bCa);
        if (iso3 == null) return false;

        Map<Integer, Integer> direct = structuralIso(aBc, bCa);
        if (direct == null) return false;

        for (int s : aBc.states()) {
            int composed = iso3.getOrDefault(iso2.getOrDefault(iso1.get(s), -1), -1);
            if (composed != direct.get(s)) return false;
        }
        return true;
    }
}
