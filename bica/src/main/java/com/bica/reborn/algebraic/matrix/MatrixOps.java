package com.bica.reborn.algebraic.matrix;

/**
 * Basic matrix operations for double-valued matrices.
 *
 * <p>Provides multiplication, inversion (Gaussian elimination with partial pivoting),
 * determinant, trace, and transpose. These are used by the algebraic analysis
 * pipeline to verify matrix identities (e.g., zeta * mobius = identity).
 */
public final class MatrixOps {

    private MatrixOps() {}

    /**
     * Matrix multiplication: C = A * B.
     *
     * @param a m x p matrix
     * @param b p x n matrix
     * @return m x n result matrix
     * @throws IllegalArgumentException if inner dimensions do not match
     */
    public static double[][] multiply(double[][] a, double[][] b) {
        int m = a.length;
        if (m == 0) return new double[0][0];
        int p = a[0].length;
        if (b.length != p) {
            throw new IllegalArgumentException(
                    "Inner dimensions do not match: " + p + " vs " + b.length);
        }
        int n = (p > 0) ? b[0].length : 0;

        double[][] c = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                double sum = 0.0;
                for (int k = 0; k < p; k++) {
                    sum += a[i][k] * b[k][j];
                }
                c[i][j] = sum;
            }
        }
        return c;
    }

    /**
     * Matrix inversion via Gaussian elimination with partial pivoting.
     *
     * @param m n x n square matrix
     * @return the inverse matrix
     * @throws IllegalArgumentException if the matrix is not square or is singular
     */
    public static double[][] invert(double[][] m) {
        int n = m.length;
        if (n == 0) return new double[0][0];
        for (double[] row : m) {
            if (row.length != n) {
                throw new IllegalArgumentException("Matrix must be square");
            }
        }

        // Augmented matrix [M | I]
        double[][] aug = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(m[i], 0, aug[i], 0, n);
            aug[i][n + i] = 1.0;
        }

        // Forward elimination with partial pivoting
        for (int col = 0; col < n; col++) {
            // Find pivot
            int pivotRow = col;
            double pivotVal = Math.abs(aug[col][col]);
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(aug[row][col]) > pivotVal) {
                    pivotVal = Math.abs(aug[row][col]);
                    pivotRow = row;
                }
            }
            if (pivotVal < 1e-14) {
                throw new IllegalArgumentException("Matrix is singular (or nearly singular)");
            }

            // Swap rows
            if (pivotRow != col) {
                double[] tmp = aug[col];
                aug[col] = aug[pivotRow];
                aug[pivotRow] = tmp;
            }

            // Scale pivot row
            double scale = aug[col][col];
            for (int j = 0; j < 2 * n; j++) {
                aug[col][j] /= scale;
            }

            // Eliminate column
            for (int row = 0; row < n; row++) {
                if (row == col) continue;
                double factor = aug[row][col];
                for (int j = 0; j < 2 * n; j++) {
                    aug[row][j] -= factor * aug[col][j];
                }
            }
        }

        // Extract inverse
        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(aug[i], n, inv[i], 0, n);
        }
        return inv;
    }

    /**
     * Compute the determinant of a square matrix via LU decomposition
     * with partial pivoting.
     *
     * @param m n x n square matrix
     * @return the determinant
     * @throws IllegalArgumentException if the matrix is not square
     */
    public static double determinant(double[][] m) {
        int n = m.length;
        if (n == 0) return 1.0;
        for (double[] row : m) {
            if (row.length != n) {
                throw new IllegalArgumentException("Matrix must be square");
            }
        }
        if (n == 1) return m[0][0];
        if (n == 2) return m[0][0] * m[1][1] - m[0][1] * m[1][0];

        // Copy to avoid mutation
        double[][] a = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(m[i], 0, a[i], 0, n);
        }

        double det = 1.0;
        for (int col = 0; col < n; col++) {
            // Partial pivoting
            int pivotRow = col;
            double pivotVal = Math.abs(a[col][col]);
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(a[row][col]) > pivotVal) {
                    pivotVal = Math.abs(a[row][col]);
                    pivotRow = row;
                }
            }
            if (pivotVal < 1e-14) return 0.0;

            if (pivotRow != col) {
                double[] tmp = a[col];
                a[col] = a[pivotRow];
                a[pivotRow] = tmp;
                det = -det;
            }

            det *= a[col][col];

            for (int row = col + 1; row < n; row++) {
                double factor = a[row][col] / a[col][col];
                for (int j = col + 1; j < n; j++) {
                    a[row][j] -= factor * a[col][j];
                }
            }
        }
        return det;
    }

    /**
     * Compute the trace (sum of diagonal elements).
     *
     * @param m n x n square matrix
     * @return trace value
     */
    public static double trace(double[][] m) {
        double sum = 0.0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i][i];
        }
        return sum;
    }

    /**
     * Compute the transpose of a matrix.
     *
     * @param m m x n matrix
     * @return n x m transposed matrix
     */
    public static double[][] transpose(double[][] m) {
        int rows = m.length;
        if (rows == 0) return new double[0][0];
        int cols = m[0].length;
        double[][] t = new double[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                t[j][i] = m[i][j];
            }
        }
        return t;
    }
}
