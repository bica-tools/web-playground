package com.bica.reborn.algebraic.tropical;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static com.bica.reborn.algebraic.tropical.Tropical.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Tropical}: port of Python {@code tests/test_tropical.py}.
 */
class TropicalTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    private Map<Integer, Integer> indexMap(StateSpace ss) {
        List<Integer> states = Zeta.stateList(ss);
        java.util.HashMap<Integer, Integer> idx = new java.util.HashMap<>();
        for (int i = 0; i < states.size(); i++) idx.put(states.get(i), i);
        return idx;
    }

    // -----------------------------------------------------------------------
    // Primitive cross-checks
    // -----------------------------------------------------------------------

    @Test
    void tropicalAddIsMax() {
        assertEquals(3.0, tropicalAdd(2.0, 3.0));
        assertEquals(5.0, tropicalAdd(5.0, NEG_INF));
    }

    @Test
    void tropicalMulIsPlus() {
        assertEquals(5.0, tropicalMul(2.0, 3.0));
        assertEquals(NEG_INF, tropicalMul(2.0, NEG_INF));
        assertEquals(NEG_INF, tropicalMul(NEG_INF, 7.0));
    }

    // -----------------------------------------------------------------------
    // Distance matrix (Step 30k)
    // -----------------------------------------------------------------------

    @Test
    void distanceEnd() {
        double[][] D = tropicalDistance(build("end"));
        assertEquals(1, D.length);
        assertEquals(0.0, D[0][0]);
    }

    @Test
    void distanceSingleEdge() {
        StateSpace ss = build("&{a: end}");
        double[][] D = tropicalDistance(ss);
        var idx = indexMap(ss);
        assertEquals(1.0, D[idx.get(ss.top())][idx.get(ss.bottom())]);
    }

    @Test
    void distanceChain() {
        StateSpace ss = build("&{a: &{b: end}}");
        double[][] D = tropicalDistance(ss);
        var idx = indexMap(ss);
        assertEquals(2.0, D[idx.get(ss.top())][idx.get(ss.bottom())]);
    }

    @Test
    void distanceDiagonalZero() {
        StateSpace ss = build("&{a: &{b: end}}");
        double[][] D = tropicalDistance(ss);
        for (int i = 0; i < D.length; i++) assertEquals(0.0, D[i][i]);
    }

    @Test
    void distanceUnreachableIsInf() {
        StateSpace ss = build("&{a: end}");
        double[][] D = tropicalDistance(ss);
        var idx = indexMap(ss);
        assertEquals(INF, D[idx.get(ss.bottom())][idx.get(ss.top())]);
    }

    // -----------------------------------------------------------------------
    // Graph metrics
    // -----------------------------------------------------------------------

    @Test
    void endDiameter() {
        assertEquals(0, diameter(build("end")));
    }

    @Test
    void chainDiameter() {
        assertEquals(1, diameter(build("&{a: end}")));
        assertEquals(2, diameter(build("&{a: &{b: end}}")));
    }

    @ParameterizedTest
    @CsvSource({
            "'&{a: end}'",
            "'&{a: &{b: end}}'",
            "'(&{a: end} || &{b: end})'"
    })
    void radiusLeDiameter(String typ) {
        StateSpace ss = build(typ);
        assertTrue(radius(ss) <= diameter(ss));
    }

    @ParameterizedTest
    @CsvSource({
            "end",
            "'&{a: end}'",
            "'&{a: &{b: end}}'"
    })
    void centerNonempty(String typ) {
        assertFalse(center(build(typ)).isEmpty());
    }

    @Test
    void centerStatesValid() {
        StateSpace ss = build("&{a: &{b: end}}");
        for (int s : center(ss)) {
            assertTrue(ss.states().contains(s));
        }
    }

    // -----------------------------------------------------------------------
    // Tropical eigenvalue (Step 30l)
    // -----------------------------------------------------------------------

    @Test
    void acyclicEigenvalueZero() {
        assertEquals(0.0, tropicalEigenvalue(build("end")));
        assertEquals(0.0, tropicalEigenvalue(build("&{a: end}")));
        assertEquals(0.0, tropicalEigenvalue(build("&{a: &{b: end}}")));
    }

    @Test
    void recursivePositiveEigenvalue() {
        double te = tropicalEigenvalue(build("rec X . &{a: X, b: end}"));
        assertTrue(te >= 0.0);
    }

    @Test
    void parallelAcyclic() {
        assertEquals(0.0, tropicalEigenvalue(build("(&{a: end} || &{b: end})")));
    }

    // -----------------------------------------------------------------------
    // Tropical determinant (Step 30m)
    // -----------------------------------------------------------------------

    @Test
    void determinantEnd() {
        assertEquals(0.0, tropicalDeterminant(build("end")));
    }

    @Test
    void determinantSingleEdgeNonNegative() {
        assertTrue(tropicalDeterminant(build("&{a: end}")) >= 0);
    }

    @Test
    void determinantChainNonNegative() {
        assertTrue(tropicalDeterminant(build("&{a: &{b: end}}")) >= 0);
    }

    // -----------------------------------------------------------------------
    // Longest path (Step 30n)
    // -----------------------------------------------------------------------

    @Test
    void longestPathEnd() {
        assertEquals(0, longestPathTopBottom(build("end")));
    }

    @Test
    void longestPathChain1() {
        assertEquals(1, longestPathTopBottom(build("&{a: end}")));
    }

    @Test
    void longestPathChain2() {
        assertEquals(2, longestPathTopBottom(build("&{a: &{b: end}}")));
    }

    @Test
    void longestPathChain3() {
        assertEquals(3, longestPathTopBottom(build("&{a: &{b: &{c: end}}}")));
    }

    @Test
    void parallelLongest() {
        assertTrue(longestPathTopBottom(build("(&{a: end} || &{b: end})")) >= 2);
    }

    @ParameterizedTest
    @CsvSource({
            "'&{a: end}'",
            "'&{a: &{b: end}}'",
            "'(&{a: end} || &{b: end})'"
    })
    void shortestLeLongest(String typ) {
        StateSpace ss = build(typ);
        assertTrue(shortestPathTopBottom(ss) <= longestPathTopBottom(ss));
    }

    // -----------------------------------------------------------------------
    // analyzeTropical
    // -----------------------------------------------------------------------

    @Test
    void analyzeEnd() {
        TropicalResult r = analyzeTropical(build("end"));
        assertEquals(1, r.numStates());
        assertEquals(0, r.diameter());
        assertEquals(0.0, r.tropicalEigenvalue());
    }

    @Test
    void analyzeChain() {
        TropicalResult r = analyzeTropical(build("&{a: &{b: end}}"));
        assertEquals(2, r.diameter());
        assertEquals(2, r.longestPathTopBottom());
        assertEquals(2, r.shortestPathTopBottom());
    }

    @Test
    void analyzeParallel() {
        TropicalResult r = analyzeTropical(build("(&{a: end} || &{b: end})"));
        assertTrue(r.diameter() >= 2);
        assertTrue(r.longestPathTopBottom() >= 2);
    }

    // -----------------------------------------------------------------------
    // Benchmarks
    // -----------------------------------------------------------------------

    private static final String[][] BENCHMARKS = {
            {"Java Iterator", "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"},
            {"File Object", "&{open: &{read: end, write: end}}"},
            {"Simple SMTP", "&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}"},
            {"Two-choice", "&{a: end, b: end}"},
            {"Nested", "&{a: &{b: &{c: end}}}"},
            {"Parallel", "(&{a: end} || &{b: end})"}
    };

    @Test
    void benchmarkTropicalProperties() {
        for (String[] bm : BENCHMARKS) {
            StateSpace ss = build(bm[1]);
            TropicalResult r = analyzeTropical(ss);
            assertEquals(ss.states().size(), r.numStates(), bm[0]);
            assertTrue(r.diameter() >= 0, bm[0]);
            assertTrue(r.radius() >= 0, bm[0]);
            assertTrue(r.radius() <= r.diameter(), bm[0]);
            assertFalse(r.center().isEmpty(), bm[0]);
        }
    }

    @Test
    void benchmarkPathLengths() {
        for (String[] bm : BENCHMARKS) {
            StateSpace ss = build(bm[1]);
            int s = shortestPathTopBottom(ss);
            int l = longestPathTopBottom(ss);
            assertTrue(s <= l, bm[0] + ": shortest=" + s + " > longest=" + l);
        }
    }

    @Test
    void benchmarkDistanceReflexive() {
        for (String[] bm : BENCHMARKS) {
            StateSpace ss = build(bm[1]);
            double[][] D = tropicalDistance(ss);
            for (int i = 0; i < D.length; i++) assertEquals(0.0, D[i][i], bm[0]);
        }
    }

    // -----------------------------------------------------------------------
    // Matrix operations (extra coverage beyond the Python test file)
    // -----------------------------------------------------------------------

    @Test
    void matrixPowerIdentityWhenKZero() {
        double[][] A = {{1.0, 2.0}, {0.0, 3.0}};
        double[][] I = tropicalMatrixPower(A, 0);
        assertEquals(0.0, I[0][0]);
        assertEquals(NEG_INF, I[0][1]);
        assertEquals(NEG_INF, I[1][0]);
        assertEquals(0.0, I[1][1]);
    }

    @Test
    void matrixMulCrossCheck() {
        // max-plus: C[i][j] = max_k(A[i][k] + B[k][j])
        double[][] A = {{1.0, 2.0}, {3.0, 4.0}};
        double[][] B = {{5.0, 6.0}, {7.0, 8.0}};
        double[][] C = tropicalMatrixMul(A, B);
        assertEquals(9.0, C[0][0]);   // max(1+5, 2+7)=9
        assertEquals(10.0, C[0][1]);  // max(1+6, 2+8)=10
        assertEquals(11.0, C[1][0]);  // max(3+5, 4+7)=11
        assertEquals(12.0, C[1][1]);  // max(3+6, 4+8)=12
    }
}
