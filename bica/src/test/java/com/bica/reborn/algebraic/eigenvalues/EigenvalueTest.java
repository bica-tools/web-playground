package com.bica.reborn.algebraic.eigenvalues;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for eigenvalue computation on session type lattices.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Adjacency eigenvalues of chains match cos(k*pi/(n+1)) formula</li>
 *   <li>Laplacian smallest eigenvalue is 0 (connected graph)</li>
 *   <li>Spectral radius is non-negative</li>
 *   <li>Spectral gap is non-negative</li>
 *   <li>Fiedler value > 0 for connected graphs</li>
 *   <li>SpectralFingerprint consistency</li>
 * </ul>
 */
class EigenvalueTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Adjacency eigenvalues
    // -----------------------------------------------------------------------

    @Test
    void adjacencyEigenvaluesEnd() {
        var ss = build("end");
        double[] eigs = EigenvalueComputer.adjacencyEigenvalues(ss);
        assertEquals(1, eigs.length);
        assertEquals(0.0, eigs[0], 1e-10);
    }

    @Test
    void adjacencyEigenvaluesChain2() {
        // 2-node chain (path graph P_2): eigenvalues are +1, -1
        var ss = build("&{a: end}");
        double[] eigs = EigenvalueComputer.adjacencyEigenvalues(ss);
        assertEquals(2, eigs.length);
        // P_2 eigenvalues: 2*cos(k*pi/3) for k=1,2 -> 1, -1
        assertEquals(-1.0, eigs[0], 0.1);
        assertEquals(1.0, eigs[1], 0.1);
    }

    @Test
    void adjacencyEigenvaluesChain3MatchFormula() {
        // 3-node chain (path graph P_3): eigenvalues are 2*cos(k*pi/4) for k=1,2,3
        // = sqrt(2), 0, -sqrt(2)
        var ss = build("&{a: &{b: end}}");
        double[] eigs = EigenvalueComputer.adjacencyEigenvalues(ss);
        assertEquals(3, eigs.length);
        double[] expected = new double[3];
        for (int k = 1; k <= 3; k++) {
            expected[k - 1] = 2 * Math.cos(k * Math.PI / 4);
        }
        Arrays.sort(expected);
        for (int i = 0; i < 3; i++) {
            assertEquals(expected[i], eigs[i], 0.15,
                    "Eigenvalue " + i + " should match cos formula");
        }
    }

    @Test
    void adjacencyEigenvaluesSorted() {
        var ss = build("&{a: end, b: end}");
        double[] eigs = EigenvalueComputer.adjacencyEigenvalues(ss);
        for (int i = 1; i < eigs.length; i++) {
            assertTrue(eigs[i] >= eigs[i - 1] - 1e-10, "Eigenvalues should be sorted");
        }
    }

    @Test
    void adjacencyEigenvaluesSymmetricSpectrum() {
        // For bipartite graphs (all Hasse diagrams of graded posets are bipartite),
        // eigenvalues should be symmetric around 0
        var ss = build("&{a: end}");
        double[] eigs = EigenvalueComputer.adjacencyEigenvalues(ss);
        for (int i = 0; i < eigs.length; i++) {
            double lambda = eigs[i];
            double neg = eigs[eigs.length - 1 - i];
            assertEquals(-lambda, neg, 0.1,
                    "Eigenvalues should be symmetric around 0 for bipartite graph");
        }
    }

    // -----------------------------------------------------------------------
    // Laplacian eigenvalues
    // -----------------------------------------------------------------------

    @Test
    void laplacianSmallestIsZero() {
        var ss = build("&{a: end, b: end}");
        double[] eigs = EigenvalueComputer.laplacianEigenvalues(ss);
        assertTrue(eigs.length > 0);
        assertEquals(0.0, eigs[0], 1e-8, "Smallest Laplacian eigenvalue must be 0");
    }

    @Test
    void laplacianAllNonNegative() {
        var ss = build("&{a: &{b: end, c: end}, d: end}");
        double[] eigs = EigenvalueComputer.laplacianEigenvalues(ss);
        for (double e : eigs) {
            assertTrue(e >= -1e-8, "Laplacian eigenvalues must be non-negative");
        }
    }

    @Test
    void laplacianEnd() {
        var ss = build("end");
        double[] eigs = EigenvalueComputer.laplacianEigenvalues(ss);
        assertEquals(1, eigs.length);
        assertEquals(0.0, eigs[0], 1e-10);
    }

    @Test
    void laplacianSumEqualsTwiceEdges() {
        // Trace of Laplacian = sum of eigenvalues = sum of degrees = 2 * |E|
        var ss = build("&{a: &{b: end, c: end}, d: end}");
        double[] eigs = EigenvalueComputer.laplacianEigenvalues(ss);
        double sum = 0;
        for (double e : eigs) sum += e;
        int edges = EigenvalueComputer.edgeCount(ss);
        assertEquals(2.0 * edges, sum, 0.5,
                "Sum of Laplacian eigenvalues should equal 2 * edge count");
    }

    // -----------------------------------------------------------------------
    // Spectral radius
    // -----------------------------------------------------------------------

    @Test
    void spectralRadiusEnd() {
        assertEquals(0.0, EigenvalueComputer.spectralRadius(build("end")), 1e-10);
    }

    @Test
    void spectralRadiusNonNegative() {
        var ss = build("&{a: end, b: end}");
        assertTrue(EigenvalueComputer.spectralRadius(ss) >= 0.0);
    }

    @Test
    void spectralRadiusChain2() {
        var ss = build("&{a: end}");
        assertEquals(1.0, EigenvalueComputer.spectralRadius(ss), 0.1);
    }

    // -----------------------------------------------------------------------
    // Spectral gap
    // -----------------------------------------------------------------------

    @Test
    void spectralGapEndIsZero() {
        assertEquals(0.0, EigenvalueComputer.spectralGap(build("end")), 1e-10);
    }

    @Test
    void spectralGapNonNegative() {
        var ss = build("&{a: end, b: end}");
        assertTrue(EigenvalueComputer.spectralGap(ss) >= -1e-10);
    }

    @Test
    void spectralGapChain2() {
        var ss = build("&{a: end}");
        // P_2: eigenvalues -1, 1 -> gap = 1 - (-1) = 2
        assertEquals(2.0, EigenvalueComputer.spectralGap(ss), 0.1);
    }

    // -----------------------------------------------------------------------
    // Energy
    // -----------------------------------------------------------------------

    @Test
    void energyEndIsZero() {
        assertEquals(0.0, EigenvalueComputer.energy(build("end")), 1e-10);
    }

    @Test
    void energyChain2() {
        var ss = build("&{a: end}");
        // |1| + |-1| = 2
        assertEquals(2.0, EigenvalueComputer.energy(ss), 0.1);
    }

    @Test
    void energyNonNegative() {
        var ss = build("&{a: &{b: end, c: end}, d: end}");
        assertTrue(EigenvalueComputer.energy(ss) >= 0.0);
    }

    // -----------------------------------------------------------------------
    // Fiedler value
    // -----------------------------------------------------------------------

    @Test
    void fiedlerEndIsZero() {
        assertEquals(0.0, EigenvalueComputer.fiedlerValue(build("end")), 1e-10);
    }

    @Test
    void fiedlerConnectedIsPositive() {
        var ss = build("&{a: end}");
        assertTrue(EigenvalueComputer.fiedlerValue(ss) > 0.0,
                "Connected Hasse diagram should have positive Fiedler value");
    }

    @Test
    void fiedlerNonNegative() {
        var ss = build("&{a: end, b: end}");
        assertTrue(EigenvalueComputer.fiedlerValue(ss) >= 0.0);
    }

    // -----------------------------------------------------------------------
    // Edge count and average degree
    // -----------------------------------------------------------------------

    @Test
    void edgeCountEnd() {
        assertEquals(0, EigenvalueComputer.edgeCount(build("end")));
    }

    @Test
    void edgeCountChain() {
        var ss = build("&{a: end}");
        assertEquals(1, EigenvalueComputer.edgeCount(ss));
    }

    @Test
    void averageDegreeEnd() {
        assertEquals(0.0, EigenvalueComputer.averageDegree(build("end")), 1e-10);
    }

    @Test
    void averageDegreeChain() {
        var ss = build("&{a: end}");
        // 2 nodes, 1 edge, each has degree 1 -> avg = 1.0
        assertEquals(1.0, EigenvalueComputer.averageDegree(ss), 1e-10);
    }

    // -----------------------------------------------------------------------
    // SpectralFingerprint
    // -----------------------------------------------------------------------

    @Test
    void fingerprintEnd() {
        var fp = EigenvalueComputer.compute(build("end"));
        assertEquals(1, fp.numStates());
        assertEquals(0, fp.numEdges());
        assertEquals(0.0, fp.spectralRadius(), 1e-10);
    }

    @Test
    void fingerprintConnected() {
        var fp = EigenvalueComputer.compute(build("&{a: end}"));
        assertTrue(fp.isConnected());
    }

    @Test
    void fingerprintEigenvalueCountMatchesStates() {
        var fp = EigenvalueComputer.compute(build("&{a: end, b: end}"));
        assertEquals(fp.numStates(), fp.adjacencyEigenvalues().size());
        assertEquals(fp.numStates(), fp.laplacianEigenvalues().size());
    }

    @Test
    void fingerprintNested() {
        var fp = EigenvalueComputer.compute(build("&{a: &{b: end, c: end}, d: end}"));
        assertTrue(fp.numStates() >= 2);
        assertTrue(fp.energy() >= 0);
        assertTrue(fp.spectralRadius() >= 0);
    }

    @Test
    void fingerprintRecursive() {
        var fp = EigenvalueComputer.compute(
                build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"));
        assertTrue(fp.numStates() > 1);
        assertNotNull(fp.adjacencyEigenvalues());
    }
}
