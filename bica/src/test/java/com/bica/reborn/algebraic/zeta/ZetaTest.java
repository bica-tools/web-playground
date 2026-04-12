package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.algebraic.matrix.MatrixOps;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ZetaFunction: compute, mobiusInverse, chainCount, density.
 */
class ZetaTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // compute: zeta matrix
    // -----------------------------------------------------------------------

    @Test
    void computeEnd() {
        var ss = build("end");
        double[][] Z = ZetaFunction.compute(ss);
        assertEquals(1, Z.length);
        assertEquals(1.0, Z[0][0]);
    }

    @Test
    void computeSimpleBranch() {
        var ss = build("&{a: end}");
        double[][] Z = ZetaFunction.compute(ss);
        assertEquals(2, Z.length);
        // Diagonal = 1
        assertEquals(1.0, Z[0][0]);
        assertEquals(1.0, Z[1][1]);
    }

    @Test
    void computeChainDimension() {
        var ss = build("&{a: &{b: end}}");
        double[][] Z = ZetaFunction.compute(ss);
        assertEquals(3, Z.length);
    }

    @Test
    void computeReflexive() {
        var ss = build("&{a: end, b: end}");
        double[][] Z = ZetaFunction.compute(ss);
        for (int i = 0; i < Z.length; i++) {
            assertEquals(1.0, Z[i][i], "Zeta diagonal must be 1");
        }
    }

    @Test
    void computeChainTransitive() {
        // Chain: top -> mid -> end
        var ss = build("&{a: &{b: end}}");
        double[][] Z = ZetaFunction.compute(ss);
        // Total ones: 3 diagonal + 3 off-diagonal = 6 (full upper triangle)
        double total = 0;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                total += Z[i][j];
        assertEquals(6.0, total);
    }

    // -----------------------------------------------------------------------
    // mobiusInverse
    // -----------------------------------------------------------------------

    @Test
    void mobiusInverseIdentity() {
        double[][] I = {{1, 0}, {0, 1}};
        double[][] M = ZetaFunction.mobiusInverse(I);
        assertEquals(1.0, M[0][0], 1e-10);
        assertEquals(0.0, M[0][1], 1e-10);
    }

    @Test
    void mobiusInverseSimpleBranch() {
        var ss = build("&{a: end}");
        double[][] Z = ZetaFunction.compute(ss);
        double[][] M = ZetaFunction.mobiusInverse(Z);
        // Z * M = I
        double[][] product = MatrixOps.multiply(Z, M);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                double expected = (i == j) ? 1.0 : 0.0;
                assertEquals(expected, product[i][j], 1e-10);
            }
        }
    }

    @Test
    void mobiusInverseChain() {
        var ss = build("&{a: &{b: end}}");
        double[][] Z = ZetaFunction.compute(ss);
        double[][] M = ZetaFunction.mobiusInverse(Z);
        double[][] product = MatrixOps.multiply(Z, M);
        int n = Z.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double expected = (i == j) ? 1.0 : 0.0;
                assertEquals(expected, product[i][j], 1e-10,
                        "Z * M must be identity at [" + i + "][" + j + "]");
            }
        }
    }

    @Test
    void mobiusInverseTwoBranch() {
        var ss = build("&{a: end, b: end}");
        double[][] Z = ZetaFunction.compute(ss);
        double[][] M = ZetaFunction.mobiusInverse(Z);
        double[][] product = MatrixOps.multiply(Z, M);
        int n = Z.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double expected = (i == j) ? 1.0 : 0.0;
                assertEquals(expected, product[i][j], 1e-10);
            }
        }
    }

    @Test
    void mobiusInverseDeterminantOne() {
        // Mobius matrix of a poset also has determinant +/-1
        var ss = build("&{a: &{b: end}}");
        double[][] Z = ZetaFunction.compute(ss);
        double[][] M = ZetaFunction.mobiusInverse(Z);
        double det = MatrixOps.determinant(M);
        assertEquals(1.0, Math.abs(det), 1e-10);
    }

    // -----------------------------------------------------------------------
    // chainCount
    // -----------------------------------------------------------------------

    @Test
    void chainCountEnd() {
        var ss = build("end");
        // Chain of length 0 when top == bottom
        assertEquals(1, ZetaFunction.chainCount(ss, 0));
        assertEquals(0, ZetaFunction.chainCount(ss, 1));
    }

    @Test
    void chainCountSimpleBranch() {
        var ss = build("&{a: end}");
        // One chain of length 1: top -> end
        assertEquals(1, ZetaFunction.chainCount(ss, 1));
        assertEquals(0, ZetaFunction.chainCount(ss, 2));
    }

    @Test
    void chainCountChain() {
        var ss = build("&{a: &{b: end}}");
        // One chain of length 2: top -> mid -> end
        assertEquals(1, ZetaFunction.chainCount(ss, 2));
        assertEquals(0, ZetaFunction.chainCount(ss, 1)); // no direct cover from top to end
    }

    @Test
    void chainCountTwoBranch() {
        var ss = build("&{a: end, b: end}");
        // Both branches lead to same end state; Hasse has one cover relation top->end
        assertEquals(1, ZetaFunction.chainCount(ss, 1));
        assertEquals(0, ZetaFunction.chainCount(ss, 2));
    }

    @Test
    void chainCountNegativeLength() {
        var ss = build("&{a: end}");
        assertEquals(0, ZetaFunction.chainCount(ss, -1));
    }

    // -----------------------------------------------------------------------
    // density
    // -----------------------------------------------------------------------

    @Test
    void densityEnd() {
        var ss = build("end");
        // 1 state: density = 1/1 = 1.0
        assertEquals(1.0, ZetaFunction.density(ss), 1e-10);
    }

    @Test
    void densitySimpleBranch() {
        var ss = build("&{a: end}");
        // 2 states: 3 ones out of 4 = 0.75
        assertEquals(0.75, ZetaFunction.density(ss), 1e-10);
    }

    @Test
    void densityChain() {
        var ss = build("&{a: &{b: end}}");
        // 3 states: 6 ones out of 9 = 2/3
        assertEquals(6.0 / 9.0, ZetaFunction.density(ss), 1e-10);
    }

    @Test
    void densityTwoBranch() {
        var ss = build("&{a: end, b: end}");
        // 2 states (top, end): 3 ones out of 4 = 0.75
        assertEquals(0.75, ZetaFunction.density(ss), 1e-10);
    }

    @Test
    void densityBetweenZeroAndOne() {
        var ss = build("&{a: &{b: end}, c: end}");
        double d = ZetaFunction.density(ss);
        assertTrue(d > 0.0 && d <= 1.0, "Density must be in (0, 1]");
    }

    // -----------------------------------------------------------------------
    // Integration: zeta determinant
    // -----------------------------------------------------------------------

    @Test
    void zetaDeterminantAlwaysOne() {
        // For any acyclic poset, det(Z) = 1 since Z is upper triangular with 1s on diagonal
        String[] types = {"end", "&{a: end}", "&{a: &{b: end}}", "&{a: end, b: end}"};
        for (String type : types) {
            var ss = build(type);
            double[][] Z = ZetaFunction.compute(ss);
            double det = MatrixOps.determinant(Z);
            assertEquals(1.0, det, 1e-10, "det(Z) should be 1 for " + type);
        }
    }

    @Test
    void zetaTraceEqualsStateCount() {
        var ss = build("&{a: &{b: end}}");
        double[][] Z = ZetaFunction.compute(ss);
        assertEquals(3.0, MatrixOps.trace(Z), 1e-10);
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Test
    void mobiusInverseEmptyMatrix() {
        double[][] empty = new double[0][0];
        double[][] M = ZetaFunction.mobiusInverse(empty);
        assertEquals(0, M.length);
    }

    @Test
    void chainCountZeroLength() {
        var ss = build("&{a: end}");
        // Chain of length 0 exists only if top == bottom (not the case here)
        assertEquals(0, ZetaFunction.chainCount(ss, 0));
    }
}
