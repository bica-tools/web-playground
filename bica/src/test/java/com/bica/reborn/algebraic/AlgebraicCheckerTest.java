package com.bica.reborn.algebraic;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the algebraic invariants checker.
 *
 * <p>Verifies zeta/Mobius matrices, eigenvalue computation, Fiedler value,
 * tropical invariants, von Neumann entropy, and join/meet irreducibles.
 */
class AlgebraicCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Zeta matrix
    // -----------------------------------------------------------------------

    @Test
    void zetaMatrixEnd() {
        var ss = build("end");
        int[][] Z = AlgebraicChecker.computeZetaMatrix(ss);
        assertEquals(1, Z.length);
        assertEquals(1, Z[0][0]); // self-reachable
    }

    @Test
    void zetaMatrixSimpleBranch() {
        var ss = build("&{a: end}");
        int[][] Z = AlgebraicChecker.computeZetaMatrix(ss);
        // 2 states: top and end
        assertEquals(2, Z.length);
        // Diagonal should be 1
        assertEquals(1, Z[0][0]);
        assertEquals(1, Z[1][1]);
    }

    @Test
    void zetaMatrixReachability() {
        var ss = build("&{a: end, b: end}");
        int[][] Z = AlgebraicChecker.computeZetaMatrix(ss);
        // Top reaches all states
        int topRow = -1;
        for (int i = 0; i < Z.length; i++) {
            int sum = 0;
            for (int j = 0; j < Z[i].length; j++) sum += Z[i][j];
            if (sum == Z.length) { topRow = i; break; }
        }
        assertTrue(topRow >= 0, "Top should reach all states");
    }

    // -----------------------------------------------------------------------
    // Mobius value
    // -----------------------------------------------------------------------

    @Test
    void mobiusValueEnd() {
        var ss = build("end");
        assertEquals(1, AlgebraicChecker.computeMobiusValue(ss));
    }

    @Test
    void mobiusValueSimpleBranch() {
        var ss = build("&{a: end}");
        int mu = AlgebraicChecker.computeMobiusValue(ss);
        assertEquals(-1, mu); // chain of length 1
    }

    @Test
    void mobiusValueTwoBranches() {
        var ss = build("&{a: end, b: end}");
        int mu = AlgebraicChecker.computeMobiusValue(ss);
        // Two-element chain (top -> end with two edges): mu(top, end) = -1
        assertEquals(-1, mu);
    }

    // -----------------------------------------------------------------------
    // Rota polynomial
    // -----------------------------------------------------------------------

    @Test
    void rotaPolynomialEnd() {
        var ss = build("end");
        var poly = AlgebraicChecker.computeRotaPolynomial(ss);
        assertNotNull(poly);
        assertFalse(poly.isEmpty());
    }

    @Test
    void rotaPolynomialLeadingCoefficient() {
        var ss = build("&{a: end}");
        var poly = AlgebraicChecker.computeRotaPolynomial(ss);
        // Leading coefficient should be 1 (from top)
        assertEquals(1, poly.get(0));
    }

    // -----------------------------------------------------------------------
    // Adjacency matrix
    // -----------------------------------------------------------------------

    @Test
    void adjacencyMatrixEnd() {
        var ss = build("end");
        double[][] A = AlgebraicChecker.computeAdjacencyMatrix(ss);
        assertEquals(1, A.length);
        assertEquals(0.0, A[0][0]); // no self-loop
    }

    @Test
    void adjacencyMatrixSymmetric() {
        var ss = build("&{a: end, b: end}");
        double[][] A = AlgebraicChecker.computeAdjacencyMatrix(ss);
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < A.length; j++) {
                assertEquals(A[i][j], A[j][i], 1e-12,
                        "Adjacency matrix must be symmetric");
            }
        }
    }

    // -----------------------------------------------------------------------
    // Eigenvalues
    // -----------------------------------------------------------------------

    @Test
    void eigenvaluesIdentity() {
        double[][] I = {{1, 0}, {0, 1}};
        double[] eigs = AlgebraicChecker.computeEigenvalues(I);
        assertEquals(2, eigs.length);
        assertEquals(1.0, eigs[0], 1e-10);
        assertEquals(1.0, eigs[1], 1e-10);
    }

    @Test
    void eigenvaluesSymmetric2x2() {
        double[][] A = {{2, 1}, {1, 2}};
        double[] eigs = AlgebraicChecker.computeEigenvalues(A);
        assertEquals(2, eigs.length);
        assertEquals(1.0, eigs[0], 1e-10);
        assertEquals(3.0, eigs[1], 1e-10);
    }

    @Test
    void eigenvaluesEmpty() {
        double[][] A = {};
        double[] eigs = AlgebraicChecker.computeEigenvalues(A);
        assertEquals(0, eigs.length);
    }

    @Test
    void eigenvaluesSingle() {
        double[][] A = {{5.0}};
        double[] eigs = AlgebraicChecker.computeEigenvalues(A);
        assertEquals(1, eigs.length);
        assertEquals(5.0, eigs[0], 1e-10);
    }

    // -----------------------------------------------------------------------
    // Spectral radius
    // -----------------------------------------------------------------------

    @Test
    void spectralRadiusEnd() {
        var ss = build("end");
        assertEquals(0.0, AlgebraicChecker.spectralRadius(ss), 1e-10);
    }

    @Test
    void spectralRadiusNonNegative() {
        var ss = build("&{a: end, b: end}");
        assertTrue(AlgebraicChecker.spectralRadius(ss) >= 0.0);
    }

    // -----------------------------------------------------------------------
    // Fiedler value
    // -----------------------------------------------------------------------

    @Test
    void fiedlerValueEnd() {
        var ss = build("end");
        assertEquals(0.0, AlgebraicChecker.fiedlerValue(ss), 1e-10);
    }

    @Test
    void fiedlerValueNonNegative() {
        var ss = build("&{a: end, b: end}");
        assertTrue(AlgebraicChecker.fiedlerValue(ss) >= 0.0);
    }

    @Test
    void fiedlerValueConnected() {
        var ss = build("&{a: end}");
        // Chain of 2 -> connected -> Fiedler > 0
        assertTrue(AlgebraicChecker.fiedlerValue(ss) > 0.0);
    }

    // -----------------------------------------------------------------------
    // Tropical invariants
    // -----------------------------------------------------------------------

    @Test
    void tropicalDiameterEnd() {
        var ss = build("end");
        assertEquals(0, AlgebraicChecker.tropicalDiameter(ss));
    }

    @Test
    void tropicalDiameterChain() {
        var ss = build("&{a: end}");
        assertEquals(1, AlgebraicChecker.tropicalDiameter(ss));
    }

    @Test
    void tropicalDiameterBranch() {
        var ss = build("&{a: end, b: end}");
        // top -> a -> end or top -> b -> end: diameter = 1
        assertEquals(1, AlgebraicChecker.tropicalDiameter(ss));
    }

    @Test
    void tropicalEigenvalueAcyclic() {
        var ss = build("&{a: end, b: end}");
        assertEquals(0.0, AlgebraicChecker.tropicalEigenvalue(ss), 1e-10);
    }

    @Test
    void tropicalEigenvalueRecursive() {
        var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
        assertEquals(1.0, AlgebraicChecker.tropicalEigenvalue(ss), 1e-10);
    }

    // -----------------------------------------------------------------------
    // Von Neumann entropy
    // -----------------------------------------------------------------------

    @Test
    void vonNeumannEntropyEnd() {
        var ss = build("end");
        assertEquals(0.0, AlgebraicChecker.vonNeumannEntropy(ss), 1e-10);
    }

    @Test
    void vonNeumannEntropyNonNegative() {
        var ss = build("&{a: end, b: end}");
        assertTrue(AlgebraicChecker.vonNeumannEntropy(ss) >= 0.0);
    }

    // -----------------------------------------------------------------------
    // Join/meet irreducibles
    // -----------------------------------------------------------------------

    @Test
    void joinIrreduciblesEnd() {
        var ss = build("end");
        assertTrue(AlgebraicChecker.joinIrreducibles(ss).isEmpty());
    }

    @Test
    void meetIrreduciblesEnd() {
        var ss = build("end");
        assertTrue(AlgebraicChecker.meetIrreducibles(ss).isEmpty());
    }

    @Test
    void joinIrreduciblesChain() {
        // &{a: end} -> chain: top -> end; top has 1 lower cover -> join-irred (but not bottom)
        var ss = build("&{a: end}");
        // In a 2-chain, top has 1 lower cover and is not bottom -> join-irreducible
        assertFalse(AlgebraicChecker.joinIrreducibles(ss).isEmpty());
    }

    // -----------------------------------------------------------------------
    // Full invariants
    // -----------------------------------------------------------------------

    @Test
    void computeInvariantsEnd() {
        var ss = build("end");
        var inv = AlgebraicChecker.computeInvariants(ss);
        assertEquals(1, inv.numStates());
        assertEquals(1, inv.mobiusValue());
        assertEquals(0.0, inv.tropicalEigenvalue(), 1e-10);
    }

    @Test
    void computeInvariantsSimpleBranch() {
        var ss = build("&{a: end, b: end}");
        var inv = AlgebraicChecker.computeInvariants(ss);
        assertTrue(inv.numStates() > 1);
        assertNotNull(inv.rotaPolynomial());
        assertNotNull(inv.eigenvalues());
    }

    @Test
    void computeInvariantsRecursive() {
        var ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
        var inv = AlgebraicChecker.computeInvariants(ss);
        assertEquals(1.0, inv.tropicalEigenvalue(), 1e-10);
        assertTrue(inv.numStates() > 1);
    }

    @Test
    void computeInvariantsNested() {
        var ss = build("&{a: &{b: end, c: end}, d: end}");
        var inv = AlgebraicChecker.computeInvariants(ss);
        assertTrue(inv.numStates() >= 2, "Nested branch should have multiple states");
        assertTrue(inv.fiedlerValue() >= 0.0);
        assertTrue(inv.vonNeumannEntropy() >= 0.0);
    }
}
