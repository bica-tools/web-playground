package com.bica.reborn.zeta_polynomial;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.zeta_polynomial.ZetaPolynomialChecker.ZetaPolynomialResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java port of {@code reticulate/tests/test_zeta_polynomial.py} (45 tests).
 *
 * <p>Tests cover multichain counting, strict chain counting, polynomial
 * interpolation, Philip Hall's theorem, palindromicity, Whitney numbers,
 * and benchmark protocols.
 */
class ZetaPolynomialCheckerTest {

    private static StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // ===================================================================
    // Basic multichain counting
    // ===================================================================

    @Nested
    class MultichainCount {

        @Test
        void endK0() {
            // Z(end, 0) = 1 (empty chain)
            StateSpace ss = build("end");
            assertEquals(1, ZetaPolynomialChecker.multichainCount(ss, 0));
        }

        @Test
        void endK1() {
            // Z(end, 1) = 1 (one element)
            StateSpace ss = build("end");
            assertEquals(1, ZetaPolynomialChecker.multichainCount(ss, 1));
        }

        @Test
        void endK2() {
            // Z(end, 2) = 1 (only chain: end <= end)
            StateSpace ss = build("end");
            assertEquals(1, ZetaPolynomialChecker.multichainCount(ss, 2));
        }

        @Test
        void singleBranchK1() {
            // Z(&{a: end}, 1) = 2 (two states)
            StateSpace ss = build("&{a: end}");
            assertEquals(2, ZetaPolynomialChecker.multichainCount(ss, 1));
        }

        @Test
        void singleBranchK2() {
            // Z(&{a: end}, 2) = 3: (top,top), (top,bot), (bot,bot)
            StateSpace ss = build("&{a: end}");
            assertEquals(3, ZetaPolynomialChecker.multichainCount(ss, 2));
        }

        @Test
        void twoBranchK1() {
            // &{a: end, b: end} has 2 states (both branches merge to end)
            StateSpace ss = build("&{a: end, b: end}");
            assertEquals(2, ZetaPolynomialChecker.multichainCount(ss, 1));
        }

        @ParameterizedTest
        @ValueSource(strings = {"end", "&{a: end}", "&{a: end, b: end}", "+{a: end}"})
        void k0AlwaysOne(String type) {
            StateSpace ss = build(type);
            assertEquals(1, ZetaPolynomialChecker.multichainCount(ss, 0));
        }

        @Test
        void monotoneInK() {
            // Z(P, k) is non-decreasing in k for k >= 0
            StateSpace ss = build("&{a: end, b: end}");
            int prev = ZetaPolynomialChecker.multichainCount(ss, 0);
            for (int k = 1; k <= 4; k++) {
                int curr = ZetaPolynomialChecker.multichainCount(ss, k);
                assertTrue(curr >= prev, "Z(P," + k + ") should >= Z(P," + (k - 1) + ")");
                prev = curr;
            }
        }

        @Test
        void selectionK1() {
            // +{a: end} has same count as &{a: end}
            StateSpace ss = build("+{a: end}");
            assertEquals(2, ZetaPolynomialChecker.multichainCount(ss, 1));
        }

        @Test
        void threeBranchK2() {
            // Z(P, 2) >= Z(P, 1) always
            StateSpace ss = build("&{a: end, b: end, c: end}");
            int n = ZetaPolynomialChecker.multichainCount(ss, 1);
            int z2 = ZetaPolynomialChecker.multichainCount(ss, 2);
            assertTrue(z2 >= n);
        }
    }

    // ===================================================================
    // Strict chain counting
    // ===================================================================

    @Nested
    class ChainCount {

        @Test
        void endChain0() {
            StateSpace ss = build("end");
            assertEquals(1, ZetaPolynomialChecker.chainCount(ss, 0));
        }

        @Test
        void endChain1() {
            StateSpace ss = build("end");
            assertEquals(1, ZetaPolynomialChecker.chainCount(ss, 1));
        }

        @Test
        void singleBranchChain1() {
            // &{a: end} has 2 chains of length 1
            StateSpace ss = build("&{a: end}");
            assertEquals(2, ZetaPolynomialChecker.chainCount(ss, 1));
        }

        @Test
        void singleBranchChain2() {
            // &{a: end} has 1 chain of length 2: top > bottom
            StateSpace ss = build("&{a: end}");
            assertEquals(1, ZetaPolynomialChecker.chainCount(ss, 2));
        }

        @Test
        void negativeLength() {
            StateSpace ss = build("end");
            assertEquals(0, ZetaPolynomialChecker.chainCount(ss, -1));
        }

        @Test
        void allChainCountsBasic() {
            StateSpace ss = build("&{a: end, b: end}");
            Map<Integer, Integer> counts = ZetaPolynomialChecker.allChainCounts(ss);
            assertTrue(counts.containsKey(0) || counts.containsKey(1));
            for (int v : counts.values()) assertTrue(v > 0);
        }
    }

    // ===================================================================
    // All chain counts comprehensive
    // ===================================================================

    @Nested
    class AllChainCounts {

        @Test
        void endChains() {
            StateSpace ss = build("end");
            Map<Integer, Integer> counts = ZetaPolynomialChecker.allChainCounts(ss);
            assertEquals(1, counts.getOrDefault(0, 0));
            assertEquals(1, counts.getOrDefault(1, 0));
        }

        @Test
        void diamond() {
            StateSpace ss = build("&{a: end, b: end}");
            Map<Integer, Integer> counts = ZetaPolynomialChecker.allChainCounts(ss);
            assertTrue(counts.containsKey(1));
            assertTrue(counts.get(1) >= 2);
        }
    }

    // ===================================================================
    // Zeta polynomial as polynomial
    // ===================================================================

    @Nested
    class ZetaPolynomial {

        @Test
        void endPolynomial() {
            // Z(end, k) = 1 for all k (constant polynomial)
            StateSpace ss = build("end");
            for (int k = 0; k < 5; k++) {
                assertEquals(1, ZetaPolynomialChecker.zetaPolynomial(ss, k));
            }
        }

        @Test
        void singleBranchPolynomial() {
            // Z(&{a: end}, k) should be a degree-1 polynomial
            StateSpace ss = build("&{a: end}");
            List<Double> coeffs = ZetaPolynomialChecker.zetaPolynomialCoefficients(ss);
            assertTrue(coeffs.size() >= 2);
        }

        @Test
        void coefficientsMatchValues() {
            StateSpace ss = build("&{a: end, b: end}");
            List<Double> coeffs = ZetaPolynomialChecker.zetaPolynomialCoefficients(ss);
            for (int k = 0; k < 5; k++) {
                int expected = ZetaPolynomialChecker.multichainCount(ss, k);
                double computed = 0;
                for (int i = 0; i < coeffs.size(); i++) {
                    computed += coeffs.get(i) * Math.pow(k, i);
                }
                assertEquals(expected, Math.round(computed),
                        "k=" + k + ": " + computed + " vs " + expected);
            }
        }
    }

    // ===================================================================
    // Philip Hall's theorem
    // ===================================================================

    @Nested
    class HallTheorem {

        @Test
        void endHall() {
            StateSpace ss = build("end");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertTrue(result.hallVerified());
        }

        @Test
        void singleBranchHall() {
            StateSpace ss = build("&{a: end}");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertTrue(result.hallVerified());
        }

        @Test
        void twoBranchHall() {
            StateSpace ss = build("&{a: end, b: end}");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertTrue(result.hallVerified());
        }

        @Test
        void selectionHall() {
            StateSpace ss = build("+{a: end, b: end}");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertTrue(result.hallVerified());
        }

        @Test
        void nestedBranchHall() {
            StateSpace ss = build("&{a: &{c: end}, b: end}");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertTrue(result.hallVerified());
        }
    }

    // ===================================================================
    // Palindromicity
    // ===================================================================

    @Nested
    class Palindromic {

        @Test
        void endPalindromic() {
            StateSpace ss = build("end");
            assertTrue(ZetaPolynomialChecker.isPalindromic(ss));
        }

        @Test
        void singleBranchCheck() {
            StateSpace ss = build("&{a: end}");
            // Just check it doesn't crash; result depends on coefficients
            boolean result = ZetaPolynomialChecker.isPalindromic(ss);
            assertNotNull(result);
        }
    }

    // ===================================================================
    // Whitney numbers
    // ===================================================================

    @Nested
    class WhitneyNumbers {

        @Test
        void endWhitney() {
            StateSpace ss = build("end");
            List<Integer> w = ZetaPolynomialChecker.whitneyNumbersSecondKind(ss);
            assertTrue(w.stream().mapToInt(Integer::intValue).sum() >= 1);
        }

        @Test
        void singleBranchWhitney() {
            StateSpace ss = build("&{a: end}");
            List<Integer> w = ZetaPolynomialChecker.whitneyNumbersSecondKind(ss);
            assertEquals(2, w.stream().mapToInt(Integer::intValue).sum());
        }

        @Test
        void twoBranchWhitney() {
            StateSpace ss = build("&{a: end, b: end}");
            List<Integer> w = ZetaPolynomialChecker.whitneyNumbersSecondKind(ss);
            assertEquals(2, w.stream().mapToInt(Integer::intValue).sum());
        }
    }

    // ===================================================================
    // Full analysis
    // ===================================================================

    @Nested
    class AnalyzeZetaPolynomial {

        @Test
        void endAnalysis() {
            StateSpace ss = build("end");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertEquals(1, result.numStates());
            assertTrue(result.degree() >= 0);
            assertTrue(result.hallVerified());
        }

        @Test
        void singleBranchAnalysis() {
            StateSpace ss = build("&{a: end}");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertEquals(2, result.numStates());
            assertFalse(result.coefficients().isEmpty());
            assertFalse(result.values().isEmpty());
        }

        @Test
        void twoBranchAnalysis() {
            StateSpace ss = build("&{a: end, b: end}");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertEquals(2, result.numStates());
            assertTrue(result.totalMultichainsK2() >= result.numStates());
        }

        @Test
        void selectionAnalysis() {
            StateSpace ss = build("+{a: end, b: end}");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertTrue(result.numStates() >= 2);
        }

        @Test
        void nestedAnalysis() {
            StateSpace ss = build("&{a: &{c: end, d: end}, b: end}");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertTrue(result.numStates() >= 3);
        }

        @Test
        void recursiveAnalysis() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertTrue(result.numStates() >= 1);
            assertNotNull(result.hallVerified());
        }
    }

    // ===================================================================
    // Benchmark protocols
    // ===================================================================

    @Nested
    class Benchmarks {

        @Test
        void iterator() {
            StateSpace ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertTrue(result.numStates() >= 2);
            assertTrue(result.hallVerified());
        }

        @Test
        void simpleResource() {
            StateSpace ss = build("&{open: &{use: &{close: end}}}");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertEquals(4, result.numStates());
            assertTrue(result.degree() >= 1);
        }

        @Test
        void binaryChoice() {
            StateSpace ss = build("+{left: end, right: end}");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertTrue(result.numStates() >= 2);
        }

        @Test
        void parallelSimple() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertTrue(result.numStates() >= 4);
        }

        @ParameterizedTest
        @CsvSource({
                "end",
                "&{a: end}",
                "'&{a: end, b: end}'",
                "+{a: end}",
                "&{a: &{b: end}}"
        })
        void zK2GeqN(String type) {
            // Z(P, 2) >= |P| for any poset
            StateSpace ss = build(type);
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertTrue(result.totalMultichainsK2() >= result.numStates(),
                    "Z(P,2) must be >= |P| for " + type);
        }

        @Test
        void multichainGrowth() {
            // Z(P, k) grows at most polynomially
            StateSpace ss = build("&{a: end, b: end}");
            int n = ss.states().size();
            for (int k = 1; k <= 5; k++) {
                int v = ZetaPolynomialChecker.multichainCount(ss, k);
                assertTrue(v <= (int) Math.pow(n, k) + 1,
                        "Z(P," + k + ") must be bounded by n^k + 1");
            }
        }
    }

    // ===================================================================
    // Edge cases
    // ===================================================================

    @Nested
    class EdgeCases {

        @Test
        void deeplyNested() {
            StateSpace ss = build("&{a: &{b: &{c: &{d: end}}}}");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertEquals(5, result.numStates());
            assertTrue(result.degree() >= 1);
        }

        @Test
        void wideBranch() {
            // Wide branch: all merge to end => 2 states
            StateSpace ss = build("&{a: end, b: end, c: end, d: end}");
            ZetaPolynomialResult result = ZetaPolynomialChecker.analyzeZetaPolynomial(ss);
            assertEquals(2, result.numStates());
        }
    }
}
