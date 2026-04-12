package com.bica.reborn.transfer;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for transfer matrix analysis (Step 30f).
 * Mirrors the 30 Python tests in test_transfer.py.
 */
class TransferCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    @Nested
    class TransferMatrixTests {

        @Test
        void endIsZero() {
            List<Integer> states = Zeta.stateList(build("end"));
            int[][] T = TransferChecker.transferMatrix(build("end"), states);
            assertEquals(1, T.length);
            assertEquals(0, T[0][0]);
        }

        @Test
        void singleBranchOneEdge() {
            StateSpace ss = build("&{a: end}");
            List<Integer> states = Zeta.stateList(ss);
            int[][] T = TransferChecker.transferMatrix(ss, states);
            assertEquals(2, T.length);
            int total = 0;
            for (int[] row : T) for (int v : row) total += v;
            assertEquals(1, total);
        }

        @Test
        void chain3TwoEdges() {
            StateSpace ss = build("&{a: &{b: end}}");
            List<Integer> states = Zeta.stateList(ss);
            int[][] T = TransferChecker.transferMatrix(ss, states);
            assertEquals(3, T.length);
            int total = 0;
            for (int[] row : T) for (int v : row) total += v;
            assertEquals(2, total);
        }

        @ParameterizedTest
        @CsvSource({"&{a: end}", "&{a: &{b: end}}", "(&{a: end} || &{b: end})"})
        void diagonalZero(String type) {
            StateSpace ss = build(type);
            List<Integer> states = Zeta.stateList(ss);
            int[][] T = TransferChecker.transferMatrix(ss, states);
            for (int i = 0; i < T.length; i++) assertEquals(0, T[i][i]);
        }
    }

    @Nested
    class PathCountingTests {

        @Test
        void endPaths() {
            Map<Integer, Integer> counts = TransferChecker.countPathsByLength(build("end"));
            assertEquals(1, counts.getOrDefault(0, 0));
        }

        @Test
        void singleBranchOnePath() {
            assertEquals(1, TransferChecker.countPathsByLength(build("&{a: end}")).getOrDefault(1, 0));
        }

        @Test
        void chain3OnePath() {
            assertEquals(1, TransferChecker.countPathsByLength(build("&{a: &{b: end}}")).getOrDefault(2, 0));
        }

        @Test
        void twoBranches() {
            int total = TransferChecker.countPathsByLength(build("&{a: end, b: end}"))
                    .values().stream().mapToInt(Integer::intValue).sum();
            assertTrue(total >= 1);
        }

        @Test
        void parallelMultiplePaths() {
            int total = TransferChecker.countPathsByLength(build("(&{a: end} || &{b: end})"))
                    .values().stream().mapToInt(Integer::intValue).sum();
            assertTrue(total >= 2);
        }

        @Test
        void totalPathCount() {
            assertEquals(1, TransferChecker.totalPathCount(build("&{a: end}")));
        }

        @Test
        void expectedLength() {
            assertEquals(1.0, TransferChecker.expectedPathLength(build("&{a: end}")), 0.001);
        }
    }

    @Nested
    class TransferPowerTests {

        @Test
        void power0IsIdentity() {
            int[][] T = TransferChecker.transferPower(build("&{a: end}"), 0);
            assertEquals(1, T[0][0]);
            assertEquals(1, T[1][1]);
        }

        @Test
        void power1IsT() {
            StateSpace ss = build("&{a: end}");
            List<Integer> states = Zeta.stateList(ss);
            int[][] T = TransferChecker.transferMatrix(ss, states);
            int[][] T1 = TransferChecker.transferPower(ss, 1);
            assertArrayEquals(T, T1);
        }

        @Test
        void powerExceedsHeightIsZero() {
            int[][] T3 = TransferChecker.transferPower(build("&{a: end}"), 3);
            for (int[] row : T3) for (int v : row) assertEquals(0, v);
        }
    }

    @Nested
    class PathEnumerationTests {

        @Test
        void endPath() {
            var paths = TransferChecker.enumeratePaths(build("end"), 1000);
            assertEquals(1, paths.size());
            assertEquals(1, paths.get(0).size());
        }

        @Test
        void singleBranchPath() {
            StateSpace ss = build("&{a: end}");
            var paths = TransferChecker.enumeratePaths(ss, 1000);
            assertEquals(1, paths.size());
            assertEquals(ss.top(), (int) paths.get(0).get(0));
            assertEquals(ss.bottom(), (int) paths.get(0).get(paths.get(0).size() - 1));
        }

        @Test
        void parallelMultiple() {
            assertTrue(TransferChecker.enumeratePaths(build("(&{a: end} || &{b: end})"), 1000).size() >= 2);
        }
    }

    @Nested
    class ResolventTests {

        @Test
        void identityAtZero() {
            double[][] R = TransferChecker.resolventAt(build("&{a: end}"), 0.0);
            assertNotNull(R);
            for (int i = 0; i < R.length; i++)
                for (int j = 0; j < R.length; j++)
                    assertEquals(i == j ? 1.0 : 0.0, R[i][j], 1e-10);
        }

        @ParameterizedTest
        @CsvSource({"&{a: end}", "&{a: &{b: end}}"})
        void resolventExists(String type) {
            assertNotNull(TransferChecker.resolventAt(build(type), 0.5));
        }
    }

    @Nested
    class AcyclicityTests {

        @Test
        void chainAcyclic() { assertTrue(TransferChecker.isTransferAcyclic(build("&{a: end}"))); }

        @Test
        void chain3Acyclic() { assertTrue(TransferChecker.isTransferAcyclic(build("&{a: &{b: end}}"))); }

        @Test
        void parallelAcyclic() { assertTrue(TransferChecker.isTransferAcyclic(build("(&{a: end} || &{b: end})"))); }
    }

    @Nested
    class AnalyzeTests {

        @Test
        void end() {
            var r = TransferChecker.analyzeTransfer(build("end"));
            assertEquals(1, r.numStates());
            assertEquals(1, r.totalPaths());
            assertEquals(0, r.height());
        }

        @Test
        void branch() {
            var r = TransferChecker.analyzeTransfer(build("&{a: end}"));
            assertEquals(2, r.numStates());
            assertEquals(1, r.totalPaths());
            assertEquals(1, r.height());
            assertTrue(r.isAcyclic());
        }

        @Test
        void parallel() {
            var r = TransferChecker.analyzeTransfer(build("(&{a: end} || &{b: end})"));
            assertTrue(r.totalPaths() >= 2);
            assertTrue(r.height() >= 2);
            assertTrue(r.isAcyclic());
        }
    }

    @Nested
    class BenchmarkTests {

        @ParameterizedTest
        @CsvSource({
            "'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}', Java Iterator",
            "'&{open: &{read: end, write: end}}', File Object",
            "'&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}', SMTP",
            "'&{a: end, b: end}', Two-choice",
            "'&{a: &{b: &{c: end}}}', Nested",
            "'(&{a: end} || &{b: end})', Parallel"
        })
        void transferProperties(String type, String name) {
            StateSpace ss = build(type);
            var r = TransferChecker.analyzeTransfer(ss);
            assertEquals(ss.states().size(), r.numStates());
            assertTrue(r.totalPaths() >= 0);
            assertTrue(r.height() >= 0);
            assertTrue(r.expectedPathLength() >= 0);
        }
    }
}
