package com.bica.reborn.quantum_info;

import com.bica.reborn.globaltype.GlobalType;
import com.bica.reborn.globaltype.GlobalTypeParser;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.quantum_info.QuantumInfoChecker.HilbertEncoding;
import com.bica.reborn.quantum_info.QuantumInfoChecker.MPSTQuantumResult;
import com.bica.reborn.quantum_info.QuantumInfoChecker.MorphismClassification;
import com.bica.reborn.quantum_info.QuantumInfoChecker.ProtocolState;
import com.bica.reborn.quantum_info.QuantumInfoChecker.ReconstructedLattice;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java port of {@code reticulate/tests/test_quantum_info.py} (Step 31l).
 */
class QuantumInfoCheckerTest {

    private static StateSpace ss(String t) {
        return StateSpaceBuilder.build(Parser.parse(t));
    }

    private static GlobalType g(String t) {
        return GlobalTypeParser.parse(t);
    }

    // -------------------- Matrix primitives --------------------

    @Test
    void zerosAndIdentity() {
        double[][] z = QuantumInfoChecker.zeros(3);
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                assertEquals(0.0, z[i][j]);
        double[][] I = QuantumInfoChecker.identity(3);
        assertEquals(1.0, I[0][0]);
        assertEquals(0.0, I[0][1]);
    }

    @Test
    void outerProductRankOne() {
        double[][] rho = QuantumInfoChecker.outer(new double[]{1.0, 0.0});
        assertEquals(1.0, rho[0][0]);
        assertEquals(0.0, rho[0][1]);
        assertEquals(0.0, rho[1][0]);
        assertEquals(0.0, rho[1][1]);
    }

    @Test
    void kron2x2() {
        double[][] A = {{1.0, 0.0}, {0.0, 1.0}};
        double[][] B = {{2.0, 3.0}, {4.0, 5.0}};
        double[][] R = QuantumInfoChecker.kron(A, B);
        assertEquals(4, R.length);
        assertEquals(4, R[0].length);
        assertEquals(2.0, R[0][0]);
        assertEquals(5.0, R[3][3]);
        assertEquals(2.0, R[2][2]);
    }

    // -------------------- Eigensolver / entropy --------------------

    @Test
    void symmetricEigenvaluesDiagonal() {
        double[] eigs = QuantumInfoChecker.symmetricEigenvalues(new double[][]{{2.0, 0.0}, {0.0, 3.0}});
        java.util.Arrays.sort(eigs);
        assertEquals(2.0, eigs[0], 1e-6);
        assertEquals(3.0, eigs[1], 1e-6);
    }

    @Test
    void symmetricEigenvaluesKnown2x2() {
        double[] eigs = QuantumInfoChecker.symmetricEigenvalues(new double[][]{{2.0, 1.0}, {1.0, 2.0}});
        java.util.Arrays.sort(eigs);
        assertEquals(1.0, eigs[0], 1e-6);
        assertEquals(3.0, eigs[1], 1e-6);
    }

    @Test
    void entropyPureStateZero() {
        double[][] rho = {{1.0, 0.0}, {0.0, 0.0}};
        assertEquals(0.0, QuantumInfoChecker.vonNeumannEntropy(rho), 1e-9);
    }

    @Test
    void entropyMaximallyMixed2d() {
        double[][] rho = {{0.5, 0.0}, {0.0, 0.5}};
        assertEquals(Math.log(2), QuantumInfoChecker.vonNeumannEntropy(rho), 1e-6);
    }

    @Test
    void entropyMaximallyMixed4d() {
        double[][] rho = new double[4][4];
        for (int i = 0; i < 4; i++) rho[i][i] = 0.25;
        assertEquals(Math.log(4), QuantumInfoChecker.vonNeumannEntropy(rho), 1e-6);
    }

    // -------------------- Protocol state --------------------

    @Test
    void protocolStateEndIsSingleBasis() {
        ProtocolState ps = QuantumInfoChecker.protocolState(ss("end"));
        assertEquals(1, ps.dimension());
        assertEquals(1.0, ps.amplitudes[0], 1e-9);
        assertEquals(1.0, ps.density[0][0], 1e-9);
    }

    @Test
    void protocolStateLinearBranch() {
        ProtocolState ps = QuantumInfoChecker.protocolState(ss("&{a: end}"));
        assertTrue(ps.dimension() >= 2);
        double sumSq = 0.0;
        for (double a : ps.amplitudes) sumSq += a * a;
        assertEquals(1.0, sumSq, 1e-9);
    }

    @Test
    void protocolStateBranchBinary() {
        ProtocolState ps = QuantumInfoChecker.protocolState(ss("&{a: end, b: end}"));
        double sumSq = 0.0;
        for (double a : ps.amplitudes) sumSq += a * a;
        assertEquals(1.0, sumSq, 1e-9);
    }

    @Test
    void protocolEntropyPureStateZero() {
        assertEquals(0.0, QuantumInfoChecker.protocolEntropy(ss("&{a: end, b: end}")), 1e-6);
    }

    @Test
    void densityMatrixIsHermitianAndTraceOne() {
        double[][] rho = QuantumInfoChecker.densityMatrix(ss("&{a: +{ok: end, err: end}}"));
        int n = rho.length;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                assertEquals(rho[j][i], rho[i][j], 1e-9);
        double tr = 0.0;
        for (int i = 0; i < n; i++) tr += rho[i][i];
        assertEquals(1.0, tr, 1e-6);
    }

    // -------------------- Tensor / partial trace --------------------

    @Test
    void tensorDensityDimensions() {
        double[][] rhoA = {{1.0, 0.0}, {0.0, 0.0}};
        double[][] rhoB = {{0.5, 0.0}, {0.0, 0.5}};
        double[][] t = QuantumInfoChecker.tensorDensity(rhoA, rhoB);
        assertEquals(4, t.length);
    }

    @Test
    void partialTraceRecoversSubsystem() {
        double[][] rhoA = {{0.7, 0.0}, {0.0, 0.3}};
        double[][] rhoB = {{0.5, 0.0}, {0.0, 0.5}};
        double[][] joint = QuantumInfoChecker.kron(rhoA, rhoB);
        double[][] aBack = QuantumInfoChecker.partialTraceSecond(joint, 2, 2);
        double[][] bBack = QuantumInfoChecker.partialTraceFirst(joint, 2, 2);
        assertEquals(0.7, aBack[0][0], 1e-9);
        assertEquals(0.3, aBack[1][1], 1e-9);
        assertEquals(0.5, bBack[0][0], 1e-9);
    }

    @Test
    void reducedEntropyProductIsSum() {
        double[][] rhoA = {{0.5, 0.0}, {0.0, 0.5}};
        double[][] rhoB = {{0.5, 0.0}, {0.0, 0.5}};
        double[][] joint = QuantumInfoChecker.kron(rhoA, rhoB);
        double[] r = QuantumInfoChecker.reducedEntropyParallel(joint, 2, 2);
        // S_AB == S_A + S_B
        assertEquals(r[0] + r[1], r[2], 1e-6);
    }

    @Test
    void parallelCompositionTensorProperty() {
        double[][] rho = QuantumInfoChecker.densityMatrix(ss("(end || end)"));
        double tr = 0.0;
        for (int i = 0; i < rho.length; i++) tr += rho[i][i];
        assertEquals(1.0, tr, 1e-6);
    }

    // -------------------- MPST mutual information --------------------

    @Test
    void mpstMutualInformationTwoRolePing() {
        MPSTQuantumResult res = QuantumInfoChecker.mpstMutualInformation(g("A->B:{ping: end}"), "A", "B");
        assertTrue(res.mutualInformation > 0.0);
        assertEquals("A", res.roleA);
        assertEquals("B", res.roleB);
    }

    @Test
    void mpstMutualInformationNoCommNonNegative() {
        MPSTQuantumResult res = QuantumInfoChecker.mpstMutualInformation(g("A->B:{m: end}"), "A", "B");
        assertTrue(res.mutualInformation >= -1e-9);
    }

    @Test
    void mpstResultContainsAllRoleEntropies() {
        MPSTQuantumResult res = QuantumInfoChecker.mpstMutualInformation(
                g("A->B:{m: B->A:{n: end}}"), "A", "B");
        assertTrue(res.roleEntropies.containsKey("A"));
        assertTrue(res.roleEntropies.containsKey("B"));
    }

    // -------------------- Bidirectional morphisms --------------------

    @Test
    void phiToHilbertBasisMatchesStates() {
        HilbertEncoding enc = QuantumInfoChecker.phiToHilbert(ss("&{a: end}"));
        assertEquals(enc.basisLabels.length, enc.dimension);
        assertTrue(enc.dimension >= 1);
    }

    @Test
    void psiFromHilbertTransitive() {
        HilbertEncoding enc = new HilbertEncoding(
                new int[]{0, 1, 2},
                new double[][]{{0.5, 0.3, 0.0}, {0.3, 0.3, 0.2}, {0.0, 0.2, 0.2}},
                3);
        ReconstructedLattice rec = QuantumInfoChecker.psiFromHilbert(enc, 0.1);
        assertTrue(rec.contains(0, 2));
        assertTrue(rec.contains(0, 0));
    }

    @Test
    void classifyPairReturnsKnownKind() {
        MorphismClassification cls = QuantumInfoChecker.classifyPair(ss("&{a: end, b: end}"));
        assertTrue(java.util.Set.of("isomorphism", "embedding", "projection", "galois").contains(cls.kind));
        assertTrue(cls.phiInjective);
    }

    @Test
    void classifyPairEndIsIsomorphism() {
        MorphismClassification cls = QuantumInfoChecker.classifyPair(ss("end"));
        assertEquals("isomorphism", cls.kind);
    }

    // -------------------- Predictive checks across benchmarks --------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "end",
            "&{a: end}",
            "&{a: end, b: end}",
            "+{ok: end, err: end}",
            "&{a: +{ok: end, err: end}}",
            "(end || end)"
    })
    void densityMatrixValidAcrossTypes(String src) {
        double[][] rho = QuantumInfoChecker.densityMatrix(ss(src));
        int n = rho.length;
        double tr = 0.0;
        for (int i = 0; i < n; i++) tr += rho[i][i];
        assertEquals(1.0, tr, 1e-6);
        double[] eigs = QuantumInfoChecker.symmetricEigenvalues(rho);
        for (double e : eigs) assertTrue(e >= -1e-6);
    }

    @Test
    void entropyMonotoneUnderSize() {
        StateSpace small = ss("&{a: end}");
        StateSpace large = ss("&{a: end, b: end, c: end}");
        assertTrue(QuantumInfoChecker.densityMatrix(small).length
                <= QuantumInfoChecker.densityMatrix(large).length);
    }

    @Test
    void mutualInformationScalesWithMessages() {
        MPSTQuantumResult r1 = QuantumInfoChecker.mpstMutualInformation(g("A->B:{m: end}"), "A", "B");
        MPSTQuantumResult r2 = QuantumInfoChecker.mpstMutualInformation(
                g("A->B:{m: A->B:{n: end}}"), "A", "B");
        assertTrue(r2.mutualInformation >= r1.mutualInformation - 1e-6);
    }
}
