package com.bica.reborn.algebraic.matrix;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MatrixBuilder and MatrixOps.
 */
class MatrixTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // MatrixBuilder: adjacency matrix
    // -----------------------------------------------------------------------

    @Test
    void adjacencyEnd() {
        var ss = build("end");
        double[][] A = MatrixBuilder.buildAdjacencyMatrix(ss);
        assertEquals(1, A.length);
        assertEquals(0.0, A[0][0]); // no self-loop on end
    }

    @Test
    void adjacencySimpleBranch() {
        var ss = build("&{a: end}");
        double[][] A = MatrixBuilder.buildAdjacencyMatrix(ss);
        assertEquals(2, A.length);
        // There should be one directed edge from top to bottom
        double sum = 0;
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++)
                sum += A[i][j];
        assertEquals(1.0, sum);
    }

    @Test
    void adjacencyTwoBranch() {
        var ss = build("&{a: end, b: end}");
        double[][] A = MatrixBuilder.buildAdjacencyMatrix(ss);
        // 2 states: top and end (both branches go to same end)
        assertEquals(2, A.length);
    }

    @Test
    void adjacencyChain() {
        var ss = build("&{a: &{b: end}}");
        double[][] A = MatrixBuilder.buildAdjacencyMatrix(ss);
        assertEquals(3, A.length);
        // Chain: top -> middle -> end = 2 edges
        double sum = 0;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                sum += A[i][j];
        assertEquals(2.0, sum);
    }

    // -----------------------------------------------------------------------
    // MatrixBuilder: zeta matrix
    // -----------------------------------------------------------------------

    @Test
    void zetaEnd() {
        var ss = build("end");
        double[][] Z = MatrixBuilder.buildZetaMatrix(ss);
        assertEquals(1, Z.length);
        assertEquals(1.0, Z[0][0]); // self-reachable
    }

    @Test
    void zetaSimpleBranch() {
        var ss = build("&{a: end}");
        double[][] Z = MatrixBuilder.buildZetaMatrix(ss);
        assertEquals(2, Z.length);
        assertEquals(1.0, Z[0][0]); // diagonal
        assertEquals(1.0, Z[1][1]);
        // top reaches end, but end does not reach top
        double total = 0;
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++)
                total += Z[i][j];
        // 2 diagonal + 1 off-diagonal = 3
        assertEquals(3.0, total);
    }

    @Test
    void zetaDiagonalIsOne() {
        var ss = build("&{a: &{b: end}}");
        double[][] Z = MatrixBuilder.buildZetaMatrix(ss);
        for (int i = 0; i < Z.length; i++) {
            assertEquals(1.0, Z[i][i], "Diagonal must be 1 (reflexivity)");
        }
    }

    @Test
    void zetaChainIsFullyReachable() {
        // Chain: top -> mid -> end -- top reaches all, mid reaches end
        var ss = build("&{a: &{b: end}}");
        double[][] Z = MatrixBuilder.buildZetaMatrix(ss);
        assertEquals(3, Z.length);
        // Total entries: 3 (diagonal) + 2 (top->mid, top->end) + 1 (mid->end) = 6
        double total = 0;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                total += Z[i][j];
        assertEquals(6.0, total);
    }

    // -----------------------------------------------------------------------
    // MatrixBuilder: Laplacian matrix
    // -----------------------------------------------------------------------

    @Test
    void laplacianEnd() {
        var ss = build("end");
        double[][] L = MatrixBuilder.buildLaplacianMatrix(ss);
        assertEquals(1, L.length);
        assertEquals(0.0, L[0][0]); // isolated node
    }

    @Test
    void laplacianRowSumsZero() {
        var ss = build("&{a: &{b: end}}");
        double[][] L = MatrixBuilder.buildLaplacianMatrix(ss);
        for (int i = 0; i < L.length; i++) {
            double rowSum = 0;
            for (int j = 0; j < L.length; j++) {
                rowSum += L[i][j];
            }
            assertEquals(0.0, rowSum, 1e-10, "Laplacian row sums must be 0");
        }
    }

    @Test
    void laplacianIsSymmetric() {
        var ss = build("&{a: end, b: end}");
        double[][] L = MatrixBuilder.buildLaplacianMatrix(ss);
        for (int i = 0; i < L.length; i++) {
            for (int j = 0; j < L.length; j++) {
                assertEquals(L[i][j], L[j][i], 1e-10, "Laplacian must be symmetric");
            }
        }
    }

    // -----------------------------------------------------------------------
    // MatrixBuilder: Hasse matrix
    // -----------------------------------------------------------------------

    @Test
    void hasseEnd() {
        var ss = build("end");
        double[][] H = MatrixBuilder.buildHasseMatrix(ss);
        assertEquals(1, H.length);
        assertEquals(0.0, H[0][0]);
    }

    @Test
    void hasseSimpleBranch() {
        var ss = build("&{a: end}");
        double[][] H = MatrixBuilder.buildHasseMatrix(ss);
        assertEquals(2, H.length);
        // One cover relation: top covers end
        double total = 0;
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++)
                total += H[i][j];
        assertEquals(1.0, total);
    }

    @Test
    void hasseChain() {
        var ss = build("&{a: &{b: end}}");
        double[][] H = MatrixBuilder.buildHasseMatrix(ss);
        assertEquals(3, H.length);
        // Two cover relations: top covers mid, mid covers end
        double total = 0;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                total += H[i][j];
        assertEquals(2.0, total);
    }

    // -----------------------------------------------------------------------
    // MatrixOps: multiply
    // -----------------------------------------------------------------------

    @Test
    void multiplyIdentity() {
        double[][] I = {{1, 0}, {0, 1}};
        double[][] A = {{3, 4}, {5, 6}};
        double[][] result = MatrixOps.multiply(I, A);
        assertEquals(3.0, result[0][0]);
        assertEquals(6.0, result[1][1]);
    }

    @Test
    void multiplyZero() {
        double[][] A = {{1, 2}, {3, 4}};
        double[][] Z = {{0, 0}, {0, 0}};
        double[][] result = MatrixOps.multiply(A, Z);
        assertEquals(0.0, result[0][0]);
        assertEquals(0.0, result[1][1]);
    }

    @Test
    void multiplyDimensionMismatch() {
        double[][] A = {{1, 2, 3}};
        double[][] B = {{1}, {2}};
        assertThrows(IllegalArgumentException.class, () -> MatrixOps.multiply(A, B));
    }

    // -----------------------------------------------------------------------
    // MatrixOps: invert
    // -----------------------------------------------------------------------

    @Test
    void invertIdentity() {
        double[][] I = {{1, 0}, {0, 1}};
        double[][] inv = MatrixOps.invert(I);
        assertEquals(1.0, inv[0][0], 1e-10);
        assertEquals(0.0, inv[0][1], 1e-10);
    }

    @Test
    void invertSimple() {
        double[][] A = {{2, 1}, {0, 1}};
        double[][] inv = MatrixOps.invert(A);
        // A * inv = I
        double[][] product = MatrixOps.multiply(A, inv);
        assertEquals(1.0, product[0][0], 1e-10);
        assertEquals(0.0, product[0][1], 1e-10);
        assertEquals(0.0, product[1][0], 1e-10);
        assertEquals(1.0, product[1][1], 1e-10);
    }

    @Test
    void invertSingularThrows() {
        double[][] singular = {{1, 2}, {2, 4}};
        assertThrows(IllegalArgumentException.class, () -> MatrixOps.invert(singular));
    }

    // -----------------------------------------------------------------------
    // MatrixOps: determinant
    // -----------------------------------------------------------------------

    @Test
    void determinantIdentity() {
        double[][] I = {{1, 0}, {0, 1}};
        assertEquals(1.0, MatrixOps.determinant(I), 1e-10);
    }

    @Test
    void determinantSingular() {
        double[][] s = {{1, 2}, {2, 4}};
        assertEquals(0.0, MatrixOps.determinant(s), 1e-10);
    }

    @Test
    void determinant3x3() {
        double[][] A = {{1, 2, 3}, {0, 1, 4}, {5, 6, 0}};
        // det = 1*(0-24) - 2*(0-20) + 3*(0-5) = -24 + 40 - 15 = 1
        assertEquals(1.0, MatrixOps.determinant(A), 1e-10);
    }

    @Test
    void determinant1x1() {
        double[][] A = {{7}};
        assertEquals(7.0, MatrixOps.determinant(A), 1e-10);
    }

    // -----------------------------------------------------------------------
    // MatrixOps: trace
    // -----------------------------------------------------------------------

    @Test
    void traceIdentity() {
        double[][] I = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        assertEquals(3.0, MatrixOps.trace(I), 1e-10);
    }

    @Test
    void traceZeta() {
        // Zeta matrix diagonal is always 1, so trace = n
        var ss = build("&{a: &{b: end}}");
        double[][] Z = MatrixBuilder.buildZetaMatrix(ss);
        assertEquals(3.0, MatrixOps.trace(Z), 1e-10);
    }

    // -----------------------------------------------------------------------
    // MatrixOps: transpose
    // -----------------------------------------------------------------------

    @Test
    void transposeSymmetric() {
        double[][] A = {{1, 2}, {2, 1}};
        double[][] T = MatrixOps.transpose(A);
        assertEquals(A[0][0], T[0][0]);
        assertEquals(A[0][1], T[1][0]);
    }

    @Test
    void transposeRectangular() {
        double[][] A = {{1, 2, 3}, {4, 5, 6}};
        double[][] T = MatrixOps.transpose(A);
        assertEquals(3, T.length);
        assertEquals(2, T[0].length);
        assertEquals(4.0, T[0][1]);
    }

    // -----------------------------------------------------------------------
    // Integration: zeta * mobius = identity
    // -----------------------------------------------------------------------

    @Test
    void zetaTimesMobiusIsIdentity() {
        var ss = build("&{a: &{b: end}}");
        double[][] Z = MatrixBuilder.buildZetaMatrix(ss);
        double[][] M = MatrixOps.invert(Z);
        double[][] product = MatrixOps.multiply(Z, M);
        int n = product.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double expected = (i == j) ? 1.0 : 0.0;
                assertEquals(expected, product[i][j], 1e-10,
                        "Z * M should be identity at [" + i + "][" + j + "]");
            }
        }
    }

    @Test
    void zetaTimesMobiusTwoBranch() {
        var ss = build("&{a: end, b: end}");
        double[][] Z = MatrixBuilder.buildZetaMatrix(ss);
        double[][] M = MatrixOps.invert(Z);
        double[][] product = MatrixOps.multiply(Z, M);
        int n = product.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double expected = (i == j) ? 1.0 : 0.0;
                assertEquals(expected, product[i][j], 1e-10);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Determinant of zeta matrix
    // -----------------------------------------------------------------------

    @Test
    void zetaDeterminantIsOne() {
        // Zeta matrix of a poset is upper triangular with 1s on diagonal, so det = 1
        var ss = build("&{a: end}");
        double[][] Z = MatrixBuilder.buildZetaMatrix(ss);
        assertEquals(1.0, MatrixOps.determinant(Z), 1e-10);
    }

    @Test
    void zetaDeterminantChain() {
        var ss = build("&{a: &{b: end}}");
        double[][] Z = MatrixBuilder.buildZetaMatrix(ss);
        assertEquals(1.0, MatrixOps.determinant(Z), 1e-10);
    }
}
