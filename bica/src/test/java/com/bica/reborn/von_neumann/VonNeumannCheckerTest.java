package com.bica.reborn.von_neumann;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.von_neumann.VonNeumannChecker.VonNeumannResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java port of {@code reticulate/tests/test_von_neumann.py} (Step 30r).
 */
class VonNeumannCheckerTest {

    private static StateSpace ss(String t) {
        return StateSpaceBuilder.build(Parser.parse(t));
    }

    // -----------------------------------------------------------------------
    // Trivial graphs
    // -----------------------------------------------------------------------

    @Test
    void endTypeEntropyZero() {
        assertEquals(0.0, VonNeumannChecker.vonNeumannEntropy(ss("end")), 1e-10);
    }

    @Test
    void endTypeMaxEntropyZero() {
        assertEquals(0.0, VonNeumannChecker.maxEntropy(ss("end")), 1e-10);
    }

    @Test
    void endTypeNormalizedZero() {
        assertEquals(0.0, VonNeumannChecker.normalizedEntropy(ss("end")), 1e-10);
    }

    @Test
    void endTypeEffectiveDimensionOne() {
        assertEquals(1.0, VonNeumannChecker.effectiveDimension(ss("end")), 1e-10);
    }

    @Test
    void endDensityMatrix() {
        double[][] rho = VonNeumannChecker.densityMatrix(ss("end"));
        assertEquals(1, rho.length);
        assertEquals(1.0, rho[0][0], 1e-10);
    }

    @Test
    void endDensityEigenvalues() {
        double[] eigs = VonNeumannChecker.densityEigenvalues(ss("end"));
        assertEquals(1, eigs.length);
        assertEquals(1.0, eigs[0], 1e-10);
    }

    // -----------------------------------------------------------------------
    // Density matrix properties
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
        "&{a: end}",
        "&{a: end, b: end}",
        "(&{a: end} || &{b: end})"
    })
    void densityMatrixTraceOne(String type) {
        double[][] rho = VonNeumannChecker.densityMatrix(ss(type));
        assertEquals(1.0, VonNeumannChecker.trace(rho), 1e-10);
    }

    @Test
    void eigenvaluesSumToOne() {
        double[] eigs = VonNeumannChecker.densityEigenvalues(ss("&{a: end, b: end}"));
        double sum = 0;
        for (double e : eigs) sum += e;
        assertEquals(1.0, sum, 1e-10);
    }

    @Test
    void eigenvaluesNonNegative() {
        double[] eigs = VonNeumannChecker.densityEigenvalues(ss("&{a: &{c: end}, b: end}"));
        for (double e : eigs) assertTrue(e >= -1e-10, "eigenvalue " + e + " is negative");
    }

    @Test
    void densityMatrixSymmetric() {
        double[][] rho = VonNeumannChecker.densityMatrix(ss("&{a: end, b: end}"));
        int n = rho.length;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                assertEquals(rho[i][j], rho[j][i], 1e-10);
    }

    @Test
    void eigenvaluesSumOneDeeper() {
        double[] eigs = VonNeumannChecker.densityEigenvalues(ss("&{a: &{c: end, d: end}, b: end}"));
        double sum = 0;
        for (double e : eigs) sum += e;
        assertEquals(1.0, sum, 1e-10);
    }

    // -----------------------------------------------------------------------
    // Laplacian properties
    // -----------------------------------------------------------------------

    @Test
    void laplacianRowSumZero() {
        double[][] L = VonNeumannChecker.laplacianMatrix(ss("&{a: end, b: end}"));
        for (double[] row : L) {
            double sum = 0;
            for (double v : row) sum += v;
            assertEquals(0.0, sum, 1e-10);
        }
    }

    @Test
    void laplacianSymmetric() {
        double[][] L = VonNeumannChecker.laplacianMatrix(ss("&{a: &{c: end}, b: end}"));
        int n = L.length;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                assertEquals(L[i][j], L[j][i], 1e-10);
    }

    @Test
    void laplacianPsd() {
        double[][] L = VonNeumannChecker.laplacianMatrix(ss("&{a: end, b: end}"));
        double[] eigs = VonNeumannChecker.symmetricEigenvalues(L);
        for (double e : eigs) assertTrue(e >= -1e-10);
    }

    @Test
    void laplacianSmallestEigenvalueZero() {
        double[][] L = VonNeumannChecker.laplacianMatrix(ss("&{a: end, b: end}"));
        double[] eigs = VonNeumannChecker.symmetricEigenvalues(L);
        double min = Double.MAX_VALUE;
        for (double e : eigs) if (e < min) min = e;
        assertEquals(0.0, min, 1e-8);
    }

    // -----------------------------------------------------------------------
    // Entropy bounds
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
        "&{a: end}",
        "&{a: end, b: end}",
        "(&{a: end} || &{b: end})"
    })
    void entropyNonNegative(String type) {
        assertTrue(VonNeumannChecker.vonNeumannEntropy(ss(type)) >= -1e-10);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "&{a: end, b: end}",
        "&{a: &{c: end, d: end}, b: end}",
        "(&{a: end} || &{b: end})"
    })
    void entropyAtMostLogN(String type) {
        StateSpace s = ss(type);
        int n = s.states().size();
        assertTrue(VonNeumannChecker.vonNeumannEntropy(s) <= Math.log(n) + 1e-10);
    }

    // -----------------------------------------------------------------------
    // Renyi entropy inequalities
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
        "&{a: end, b: end}",
        "&{a: &{c: end, d: end}, b: end}",
        "(&{a: end} || &{b: end})"
    })
    void renyiOrdering(String type) {
        StateSpace s = ss(type);
        double sVn = VonNeumannChecker.vonNeumannEntropy(s);
        double s2 = VonNeumannChecker.renyiEntropy2(s);
        double sInf = VonNeumannChecker.renyiEntropyInf(s);
        assertTrue(sInf <= s2 + 1e-10, "S_inf > S_2");
        assertTrue(s2 <= sVn + 1e-10, "S_2 > S_vn");
    }

    @Test
    void renyiAlphaInvalid() {
        StateSpace s = ss("&{a: end}");
        assertThrows(IllegalArgumentException.class, () -> VonNeumannChecker.renyiEntropy(s, 0.0));
        assertThrows(IllegalArgumentException.class, () -> VonNeumannChecker.renyiEntropy(s, -1.0));
    }

    @Test
    void renyiAlphaNearOne() {
        StateSpace s = ss("&{a: end, b: end}");
        double sVn = VonNeumannChecker.vonNeumannEntropy(s);
        double sR = VonNeumannChecker.renyiEntropy(s, 0.9999999);
        assertEquals(sVn, sR, 1e-3);
    }

    @Test
    void renyi2NonNegative() {
        assertTrue(VonNeumannChecker.renyiEntropy2(ss("&{a: end, b: end}")) >= -1e-10);
    }

    @Test
    void renyiInfNonNegative() {
        assertTrue(VonNeumannChecker.renyiEntropyInf(ss("&{a: end, b: end}")) >= -1e-10);
    }

    // -----------------------------------------------------------------------
    // Effective dimension
    // -----------------------------------------------------------------------

    @Test
    void effectiveDimensionAtLeastOne() {
        assertTrue(VonNeumannChecker.effectiveDimension(ss("&{a: end, b: end}")) >= 1.0 - 1e-10);
    }

    @Test
    void effectiveDimensionAtMostN() {
        StateSpace s = ss("&{a: end, b: end}");
        int n = s.states().size();
        assertTrue(VonNeumannChecker.effectiveDimension(s) <= n + 1e-10);
    }

    @Test
    void effectiveDimensionOneForEnd() {
        assertEquals(1.0, VonNeumannChecker.effectiveDimension(ss("end")), 1e-10);
    }

    @Test
    void effectiveDimensionParallel() {
        assertTrue(VonNeumannChecker.effectiveDimension(ss("(&{a: end} || &{b: end})")) >= 1.0);
    }

    // -----------------------------------------------------------------------
    // Relative entropy
    // -----------------------------------------------------------------------

    @Test
    void sameTypeRelativeEntropyZero() {
        StateSpace s = ss("&{a: end, b: end}");
        double re = VonNeumannChecker.relativeEntropy(s, s);
        assertFalse(Double.isNaN(re));
        assertEquals(0.0, re, 1e-8);
    }

    @Test
    void differentDimensionReturnsNaN() {
        assertTrue(Double.isNaN(VonNeumannChecker.relativeEntropy(ss("end"), ss("&{a: end, b: end}"))));
    }

    @Test
    void relativeEntropyNonNegative() {
        StateSpace s1 = ss("&{a: end, b: end}");
        StateSpace s2 = ss("&{a: &{c: end}, b: end}");
        double re = VonNeumannChecker.relativeEntropy(s1, s2);
        if (!Double.isNaN(re)) assertTrue(re >= -1e-10);
    }

    @Test
    void endSelfRelativeEntropyZero() {
        StateSpace s = ss("end");
        double re = VonNeumannChecker.relativeEntropy(s, s);
        assertFalse(Double.isNaN(re));
        assertEquals(0.0, re, 1e-8);
    }

    // -----------------------------------------------------------------------
    // Mutual information
    // -----------------------------------------------------------------------

    @Test
    void mutualInformationNonNegative() {
        assertTrue(VonNeumannChecker.mutualInformation(ss("&{a: end, b: end}")) >= -1e-10);
    }

    @Test
    void mutualInformationZeroForEnd() {
        assertEquals(0.0, VonNeumannChecker.mutualInformation(ss("end")), 1e-10);
    }

    @Test
    void miPlusEntropyEqualsLogN() {
        StateSpace s = ss("&{a: end, b: end}");
        int n = s.states().size();
        double mi = VonNeumannChecker.mutualInformation(s);
        double ent = VonNeumannChecker.vonNeumannEntropy(s);
        assertEquals(Math.log(n), mi + ent, 1e-10);
    }

    // -----------------------------------------------------------------------
    // Entropy rate
    // -----------------------------------------------------------------------

    @Test
    void entropyRateEnd() {
        assertEquals(0.0, VonNeumannChecker.entropyRate(ss("end")), 1e-10);
    }

    @Test
    void entropyRateNonNegative() {
        assertTrue(VonNeumannChecker.entropyRate(ss("&{a: end, b: end}")) >= 0.0);
    }

    @Test
    void entropyRateEqualsSOverN() {
        StateSpace s = ss("&{a: end, b: end}");
        int n = s.states().size();
        assertEquals(VonNeumannChecker.vonNeumannEntropy(s) / n,
                     VonNeumannChecker.entropyRate(s), 1e-10);
    }

    // -----------------------------------------------------------------------
    // xlogx helper
    // -----------------------------------------------------------------------

    @Test
    void xlogxZero() {
        assertEquals(0.0, VonNeumannChecker.xlogx(0.0));
    }

    @Test
    void xlogxOne() {
        assertEquals(0.0, VonNeumannChecker.xlogx(1.0), 1e-15);
    }

    @Test
    void xlogxHalf() {
        assertEquals(-0.5 * Math.log(2), VonNeumannChecker.xlogx(0.5), 1e-10);
    }

    @Test
    void xlogxSmallPositive() {
        assertEquals(0.0, VonNeumannChecker.xlogx(1e-20));
    }

    // -----------------------------------------------------------------------
    // Protocol comparison
    // -----------------------------------------------------------------------

    @Test
    void singleBranchHasEntropy() {
        assertTrue(VonNeumannChecker.vonNeumannEntropy(ss("&{a: end}")) >= 0.0);
    }

    @Test
    void twoBranchEntropyNonNegative() {
        assertTrue(VonNeumannChecker.vonNeumannEntropy(ss("&{a: end}")) >= 0.0);
        assertTrue(VonNeumannChecker.vonNeumannEntropy(ss("&{a: end, b: end}")) >= 0.0);
    }

    // -----------------------------------------------------------------------
    // Recursive types
    // -----------------------------------------------------------------------

    @Test
    void recursiveEntropyPositive() {
        assertTrue(VonNeumannChecker.vonNeumannEntropy(ss("rec X . &{a: X, b: end}")) >= 0.0);
    }

    @Test
    void recursiveDensityTraceOne() {
        double[][] rho = VonNeumannChecker.densityMatrix(ss("rec X . &{a: X, b: end}"));
        assertEquals(1.0, VonNeumannChecker.trace(rho), 1e-10);
    }

    @Test
    void recursiveEigenvaluesSumOne() {
        double[] eigs = VonNeumannChecker.densityEigenvalues(ss("rec X . &{a: X, b: end}"));
        double sum = 0;
        for (double e : eigs) sum += e;
        assertEquals(1.0, sum, 1e-10);
    }

    // -----------------------------------------------------------------------
    // Full analysis integration
    // -----------------------------------------------------------------------

    @Test
    void endAnalysis() {
        VonNeumannResult r = VonNeumannChecker.analyze(ss("end"));
        assertEquals(0.0, r.entropy(), 1e-10);
        assertEquals(0.0, r.maxEntropy(), 1e-10);
        assertEquals(1.0, r.effectiveDimension(), 1e-10);
        assertFalse(r.isMaximallyMixed());
    }

    @Test
    void branchAnalysis() {
        StateSpace s = ss("&{a: end, b: end}");
        VonNeumannResult r = VonNeumannChecker.analyze(s);
        assertTrue(r.entropy() >= 0.0);
        assertTrue(r.maxEntropy() > 0.0);
        assertTrue(r.normalizedEntropy() >= 0.0 && r.normalizedEntropy() <= 1.0 + 1e-10);
        assertTrue(r.effectiveDimension() >= 1.0);
        assertEquals(s.states().size(), r.densityEigenvalues().length);
        double sum = 0;
        for (double e : r.densityEigenvalues()) sum += e;
        assertEquals(1.0, sum, 1e-10);
    }

    @Test
    void parallelAnalysis() {
        VonNeumannResult r = VonNeumannChecker.analyze(ss("(&{a: end} || &{b: end})"));
        assertTrue(r.entropy() >= 0.0);
        assertTrue(r.renyiEntropyInf() <= r.renyiEntropy2() + 1e-10);
        assertTrue(r.renyiEntropy2() <= r.entropy() + 1e-10);
    }

    @Test
    void deeperProtocol() {
        StateSpace s = ss("&{a: &{c: end, d: end}, b: end}");
        VonNeumannResult r = VonNeumannChecker.analyze(s);
        assertTrue(r.entropy() > 0.0);
        assertTrue(r.entropy() <= r.maxEntropy() + 1e-10);
        assertTrue(r.effectiveDimension() >= 1.0);
        assertTrue(r.effectiveDimension() <= s.states().size() + 1e-10);
    }

    // -----------------------------------------------------------------------
    // Benchmark protocols
    // -----------------------------------------------------------------------

    @Test
    void iteratorEntropy() {
        VonNeumannResult r = VonNeumannChecker.analyze(
            ss("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"));
        assertTrue(r.entropy() >= 0.0);
        assertTrue(r.entropy() <= r.maxEntropy() + 1e-10);
    }

    @Test
    void twoBranchSelection() {
        VonNeumannResult r = VonNeumannChecker.analyze(ss("+{OK: end, ERROR: end}"));
        assertTrue(r.entropy() >= 0.0);
        double sum = 0;
        for (double e : r.densityEigenvalues()) sum += e;
        assertEquals(1.0, sum, 1e-10);
    }

    @Test
    void nestedBranchSelection() {
        VonNeumannResult r = VonNeumannChecker.analyze(
            ss("&{open: +{OK: &{read: end}, ERROR: end}}"));
        assertTrue(r.entropy() > 0.0);
        assertTrue(r.effectiveDimension() >= 1.0);
    }
}
