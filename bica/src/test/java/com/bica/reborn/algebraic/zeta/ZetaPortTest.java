package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.algebraic.zeta.Zeta.IntervalInfo;
import com.bica.reborn.algebraic.zeta.Zeta.Pair;
import com.bica.reborn.algebraic.zeta.Zeta.ZetaCompositionResult;
import com.bica.reborn.algebraic.zeta.Zeta.ZetaResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full 1:1 port of reticulate/tests/test_zeta.py.
 * Tests the Java port in {@link Zeta}.
 */
class ZetaPortTest {

    private static StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // ----- Zeta matrix -----

    @Test
    void endType() {
        int[][] Z = Zeta.zetaMatrix(build("end"));
        assertEquals(1, Z.length);
        assertEquals(1, Z[0][0]);
    }

    @Test
    void singleBranch() {
        int[][] Z = Zeta.zetaMatrix(build("&{a: end}"));
        assertEquals(2, Z.length);
        assertEquals(1, Z[0][0]);
        assertEquals(1, Z[1][1]);
        int total = 0;
        for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) total += Z[i][j];
        assertEquals(3, total);
    }

    @Test
    void twoBranch() {
        int[][] Z = Zeta.zetaMatrix(build("&{a: end, b: end}"));
        assertEquals(2, Z.length);
        assertEquals(1, Z[0][0]);
        assertEquals(1, Z[1][1]);
    }

    @Test
    void diamond() {
        int[][] Z = Zeta.zetaMatrix(build("&{a: &{c: end}, b: &{c: end}}"));
        assertTrue(Z.length >= 3);
    }

    @Test
    void reflexivity() {
        int[][] Z = Zeta.zetaMatrix(build("&{a: &{b: end}, c: end}"));
        for (int i = 0; i < Z.length; i++) assertEquals(1, Z[i][i]);
    }

    @Test
    void transitivity() {
        int[][] Z = Zeta.zetaMatrix(build("&{a: &{b: end}}"));
        int n = Z.length;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                for (int k = 0; k < n; k++)
                    if (Z[i][j] == 1 && Z[j][k] == 1) assertEquals(1, Z[i][k]);
    }

    @Test
    void zetaTrace() {
        int[][] Z = Zeta.zetaMatrix(build("&{a: &{b: end}, c: end}"));
        assertEquals(Z.length, Zeta.zetaTrace(Z));
    }

    @Test
    void selectionType() {
        int[][] Z = Zeta.zetaMatrix(build("+{a: end, b: end}"));
        assertTrue(Z.length >= 2);
        for (int i = 0; i < Z.length; i++) assertEquals(1, Z[i][i]);
    }

    // ----- Rank -----

    @Test
    void endRank() {
        StateSpace ss = build("end");
        assertEquals(0, Zeta.computeRank(ss).get(ss.bottom()));
    }

    @Test
    void chainRank() {
        StateSpace ss = build("&{a: end}");
        Map<Integer, Integer> r = Zeta.computeRank(ss);
        assertEquals(0, r.get(ss.bottom()));
        assertTrue(r.get(ss.top()) >= 1);
    }

    @Test
    void longerChainRank() {
        StateSpace ss = build("&{a: &{b: end}}");
        Map<Integer, Integer> r = Zeta.computeRank(ss);
        assertEquals(0, r.get(ss.bottom()));
        assertEquals(2, r.get(ss.top()));
    }

    @Test
    void heightMatchesTopRank() {
        StateSpace ss = build("&{a: &{b: &{c: end}}}");
        int h = Zeta.computeHeight(ss);
        assertEquals(h, Zeta.computeRank(ss).get(ss.top()));
        assertEquals(3, h);
    }

    // ----- Width -----

    @Test
    void endWidth() { assertEquals(1, Zeta.computeWidth(build("end"))); }

    @Test
    void chainWidth() { assertEquals(1, Zeta.computeWidth(build("&{a: end}"))); }

    @Test
    void parallelWidth() {
        assertTrue(Zeta.computeWidth(build("(&{a: end} || &{b: end})")) >= 2);
    }

    // ----- Height -----

    @Test void endHeight() { assertEquals(0, Zeta.computeHeight(build("end"))); }
    @Test void branchHeight() { assertEquals(1, Zeta.computeHeight(build("&{a: end}"))); }
    @Test void nestedHeight() { assertEquals(2, Zeta.computeHeight(build("&{a: &{b: end}}"))); }
    @Test void deepChainHeight() { assertEquals(3, Zeta.computeHeight(build("&{a: &{b: &{c: end}}}"))); }

    // ----- Graded -----

    @Test void chainGraded() { assertTrue(Zeta.checkGraded(build("&{a: &{b: end}}"))); }
    @Test void singleStateGraded() { assertTrue(Zeta.checkGraded(build("end"))); }
    @Test void branchGraded() { assertTrue(Zeta.checkGraded(build("&{a: end, b: end}"))); }

    // ----- Intervals -----

    @Test
    void fullInterval() {
        StateSpace ss = build("&{a: end}");
        Set<Integer> iv = Zeta.computeInterval(ss, ss.top(), ss.bottom());
        assertTrue(iv.contains(ss.top()));
        assertTrue(iv.contains(ss.bottom()));
        assertEquals(ss.states().size(), iv.size());
    }

    @Test
    void selfInterval() {
        StateSpace ss = build("&{a: &{b: end}}");
        for (int s : ss.states()) {
            assertEquals(Set.of(s), Zeta.computeInterval(ss, s, s));
        }
    }

    @Test
    void incomparableEmpty() {
        StateSpace ss = build("(&{a: end} || &{b: end})");
        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);
        for (int x : ss.states()) {
            for (int y : ss.states()) {
                if (x != y && !reach.get(x).contains(y) && !reach.get(y).contains(x)) {
                    assertEquals(0, Zeta.computeInterval(ss, x, y).size());
                }
            }
        }
    }

    @Test
    void intervalInfoChains() {
        StateSpace ss = build("&{a: end}");
        IntervalInfo info = Zeta.intervalInfo(ss, ss.top(), ss.bottom());
        assertEquals(2, info.size());
        assertTrue(info.chains() >= 1);
    }

    @Test
    void enumerateIntervalsBasic() {
        StateSpace ss = build("&{a: &{b: end}}");
        List<IntervalInfo> all = Zeta.enumerateIntervals(ss);
        assertFalse(all.isEmpty());
        long full = all.stream().filter(i -> i.top() == ss.top() && i.bottom() == ss.bottom()).count();
        assertEquals(1, full);
    }

    // ----- Chain counts -----

    @Test
    void singleChain() {
        Map<Integer, Integer> c = Zeta.chainCounts(build("&{a: end}"));
        assertTrue(c.containsKey(1));
        assertEquals(1, c.get(1));
    }

    @Test
    void twoStepChain() {
        Map<Integer, Integer> c = Zeta.chainCounts(build("&{a: &{b: end}}"));
        assertTrue(c.containsKey(2));
    }

    @Test
    void branchChains() {
        Map<Integer, Integer> c = Zeta.chainCounts(build("&{a: end, b: end}"));
        assertFalse(c.isEmpty());
    }

    // ----- Density -----

    @Test
    void singleStateDensity() {
        assertEquals(1.0, Zeta.orderDensity(Zeta.zetaMatrix(build("end"))), 1e-12);
    }

    @Test
    void chainDensity() {
        double d = Zeta.orderDensity(Zeta.zetaMatrix(build("&{a: end}")));
        assertTrue(d > 0.0 && d <= 1.0);
    }

    @Test
    void comparablePairsConsistent() {
        int[][] Z = Zeta.zetaMatrix(build("&{a: &{b: end}}"));
        int cp = Zeta.countComparablePairs(Z);
        int total = 0;
        for (int i = 0; i < Z.length; i++) for (int j = 0; j < Z.length; j++) total += Z[i][j];
        assertEquals(total - Z.length, cp);
    }

    // ----- Kronecker -----

    @Test
    void identityKronecker() {
        int[][] C = Zeta.kroneckerProduct(new int[][]{{1}}, new int[][]{{1}});
        assertArrayEquals(new int[]{1}, C[0]);
    }

    @Test
    void twoByTwoKronecker() {
        int[][] A = {{1, 1}, {0, 1}};
        int[][] B = {{1, 1}, {0, 1}};
        int[][] C = Zeta.kroneckerProduct(A, B);
        assertEquals(4, C.length);
        assertEquals(1, C[0][0]);
        assertEquals(1, C[0][3]);
    }

    @Test
    void parallelCompositionDim() {
        StateSpace s1 = build("&{a: end}");
        StateSpace s2 = build("&{b: end}");
        StateSpace sp = build("(&{a: end} || &{b: end})");
        ZetaCompositionResult r = Zeta.verifyKroneckerComposition(s1, s2, sp);
        assertEquals(r.leftStates() * r.rightStates(), r.productStates());
    }

    @Test
    void densityMultiplicative() {
        int[][] Z1 = Zeta.zetaMatrix(build("&{a: end}"));
        int[][] Z2 = Zeta.zetaMatrix(build("&{b: end}"));
        double d1 = Zeta.orderDensity(Z1);
        double d2 = Zeta.orderDensity(Z2);
        double dp = Zeta.orderDensity(Zeta.kroneckerProduct(Z1, Z2));
        assertEquals(d1 * d2, dp, 1e-10);
    }

    // ----- Row/col sums -----

    @Test
    void topRowSum() {
        StateSpace ss = build("&{a: end}");
        int[][] Z = Zeta.zetaMatrix(ss);
        assertEquals(ss.states().size(), Zeta.zetaRowSums(Z, Zeta.stateList(ss)).get(ss.top()));
    }

    @Test
    void bottomColSum() {
        StateSpace ss = build("&{a: end}");
        int[][] Z = Zeta.zetaMatrix(ss);
        assertEquals(ss.states().size(), Zeta.zetaColSums(Z, Zeta.stateList(ss)).get(ss.bottom()));
    }

    @Test
    void bottomRowSumIsOne() {
        StateSpace ss = build("&{a: end}");
        int[][] Z = Zeta.zetaMatrix(ss);
        assertEquals(1, Zeta.zetaRowSums(Z, Zeta.stateList(ss)).get(ss.bottom()));
    }

    // ----- Incidence algebra -----

    @Test
    void zetaFunctionReflexive() {
        StateSpace ss = build("&{a: end}");
        Map<Pair, Integer> zf = Zeta.zetaFunction(ss);
        for (int s : ss.states()) assertEquals(1, zf.getOrDefault(new Pair(s, s), 0));
    }

    @Test
    void deltaFunctionAcyclic() {
        StateSpace ss = build("&{a: end}");
        Map<Pair, Integer> df = Zeta.deltaFunction(ss);
        for (int s : ss.states()) assertEquals(1, df.get(new Pair(s, s)));
        for (int s : ss.states())
            for (int t : ss.states())
                if (s != t) assertFalse(df.containsKey(new Pair(s, t)));
    }

    @Test
    void mobiusInversionSmall() {
        checkInversion(build("&{a: end}"));
    }

    @Test
    void mobiusInversionLarger() {
        checkInversion(build("&{a: &{b: end}, c: end}"));
    }

    private void checkInversion(StateSpace ss) {
        Map<Pair, Integer> zf = Zeta.zetaFunction(ss);
        Map<Pair, Integer> mf = Zeta.mobiusFunction(ss);
        Map<Pair, Integer> prod = Zeta.convolve(zf, mf, ss);
        Map<Pair, Integer> df = Zeta.deltaFunction(ss);
        for (int s : ss.states()) {
            for (int t : ss.states()) {
                int expected = df.getOrDefault(new Pair(s, t), 0);
                int actual = prod.getOrDefault(new Pair(s, t), 0);
                assertEquals(expected, actual, "(z*m)(" + s + "," + t + ")");
            }
        }
    }

    // ----- Zeta power -----

    @Test
    void identityPower() {
        int[][] Z = {{1, 1}, {0, 1}};
        int[][] Z0 = Zeta.zetaPower(Z, 0);
        assertArrayEquals(new int[]{1, 0}, Z0[0]);
        assertArrayEquals(new int[]{0, 1}, Z0[1]);
    }

    @Test
    void firstPower() {
        int[][] Z = {{1, 1}, {0, 1}};
        int[][] Z1 = Zeta.zetaPower(Z, 1);
        assertArrayEquals(Z[0], Z1[0]);
        assertArrayEquals(Z[1], Z1[1]);
    }

    @Test
    void idempotentSupport() {
        int[][] Z = Zeta.zetaMatrix(build("&{a: end}"));
        int[][] Z2 = Zeta.zetaPower(Z, 2);
        for (int i = 0; i < Z.length; i++)
            for (int j = 0; j < Z.length; j++)
                if (Z[i][j] > 0) assertTrue(Z2[i][j] > 0);
    }

    // ----- Upper triangular -----

    @Test
    void chainUpperTriangular() {
        StateSpace ss = build("&{a: &{b: end}}");
        int[][] Z = Zeta.zetaMatrix(ss);
        assertTrue(Zeta.isUpperTriangular(Z, Zeta.stateList(ss), Zeta.computeRank(ss)));
    }

    // ----- analyzeZeta -----

    @Test
    void endAnalysis() {
        ZetaResult r = Zeta.analyzeZeta(build("end"));
        assertEquals(1, r.numStates());
        assertEquals(0, r.height());
        assertEquals(1, r.width());
        assertEquals(1.0, r.density(), 1e-12);
        assertTrue(r.isGraded());
    }

    @Test
    void simpleBranchAnalysis() {
        ZetaResult r = Zeta.analyzeZeta(build("&{a: end}"));
        assertEquals(2, r.numStates());
        assertEquals(1, r.height());
        assertEquals(1, r.width());
        assertTrue(r.isGraded());
        assertEquals(1, r.numComparablePairs());
    }

    @Test
    void nestedBranchAnalysis() {
        ZetaResult r = Zeta.analyzeZeta(build("&{a: &{b: end}}"));
        assertEquals(3, r.numStates());
        assertEquals(2, r.height());
        assertTrue(r.isGraded());
    }

    @Test
    void parallelAnalysis() {
        ZetaResult r = Zeta.analyzeZeta(build("(&{a: end} || &{b: end})"));
        assertTrue(r.numStates() >= 4);
        assertTrue(r.width() >= 2);
        assertTrue(r.height() >= 2);
    }

    @Test
    void selectionAnalysis() {
        ZetaResult r = Zeta.analyzeZeta(build("+{a: end, b: end}"));
        assertTrue(r.numStates() >= 2);
        assertTrue(r.isGraded());
    }

    @Test
    void recursiveAnalysis() {
        ZetaResult r = Zeta.analyzeZeta(build("rec X . &{a: X, b: end}"));
        assertTrue(r.numStates() >= 2);
        assertTrue(r.height() >= 1);
    }

    // ----- Benchmarks -----

    static Stream<Arguments> benchmarks() {
        return Stream.of(
                Arguments.of("Java Iterator", "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"),
                Arguments.of("File Object", "&{open: &{read: end, write: end}}"),
                Arguments.of("Simple SMTP", "&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}"),
                Arguments.of("Two-choice", "&{a: end, b: end}"),
                Arguments.of("Nested", "&{a: &{b: &{c: end}}}"),
                Arguments.of("Parallel simple", "(&{a: end} || &{b: end})")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("benchmarks")
    void benchmarkZetaProperties(String name, String type) {
        StateSpace ss = build(type);
        ZetaResult r = Zeta.analyzeZeta(ss);
        assertEquals(ss.states().size(), r.numStates());
        assertTrue(r.height() >= 0);
        assertTrue(r.width() >= 1);
        assertTrue(r.density() > 0.0 && r.density() <= 1.0);
        assertEquals(r.numStates(), r.zeta().length);
        for (int[] row : r.zeta()) assertEquals(r.numStates(), row.length);
        for (int i = 0; i < r.numStates(); i++) assertEquals(1, r.zeta()[i][i]);
        assertEquals(r.numStates(), Zeta.zetaTrace(r.zeta()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("benchmarks")
    void benchmarkMobiusInversion(String name, String type) {
        checkInversion(build(type));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("benchmarks")
    void benchmarkRankConsistent(String name, String type) {
        StateSpace ss = build(type);
        Map<Integer, Integer> rank = Zeta.computeRank(ss);
        Map<Integer, Set<Integer>> reach = Zeta.reachability(ss);
        for (int x : ss.states()) {
            for (int y : ss.states()) {
                if (x != y && reach.get(x).contains(y)) {
                    assertTrue(rank.get(x) >= rank.get(y),
                            name + ": rank(" + x + ")=" + rank.get(x)
                                    + " < rank(" + y + ")=" + rank.get(y));
                }
            }
        }
    }
}
