package com.bica.reborn.characteristic;

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
 * Tests for Rota characteristic polynomial (Step 30c) -- Java port.
 */
class CharacteristicCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Polynomial arithmetic
    // -----------------------------------------------------------------------

    @Nested
    class PolyArithmetic {

        @Test
        void evaluateConstant() {
            assertEquals(5, CharacteristicChecker.polyEvaluate(List.of(5), 3));
        }

        @Test
        void evaluateLinear() {
            // 2t + 1 at t=3 -> 7
            assertEquals(7, CharacteristicChecker.polyEvaluate(List.of(2, 1), 3));
        }

        @Test
        void evaluateQuadratic() {
            // t^2 - 1 at t=2 -> 3
            assertEquals(3, CharacteristicChecker.polyEvaluate(List.of(1, 0, -1), 2));
        }

        @Test
        void multiplyConstants() {
            assertEquals(List.of(12), CharacteristicChecker.polyMultiply(List.of(3), List.of(4)));
        }

        @Test
        void multiplyLinear() {
            // (t - 1)(t + 1) = t^2 - 1
            assertEquals(List.of(1, 0, -1),
                    CharacteristicChecker.polyMultiply(List.of(1, -1), List.of(1, 1)));
        }

        @Test
        void multiplyQuadratic() {
            // (t - 1)^2 = t^2 - 2t + 1
            assertEquals(List.of(1, -2, 1),
                    CharacteristicChecker.polyMultiply(List.of(1, -1), List.of(1, -1)));
        }

        @Test
        void derivativeConstant() {
            assertEquals(List.of(0), CharacteristicChecker.polyDerivative(List.of(5)));
        }

        @Test
        void derivativeLinear() {
            // d/dt(3t + 2) = 3
            assertEquals(List.of(3), CharacteristicChecker.polyDerivative(List.of(3, 2)));
        }

        @Test
        void derivativeQuadratic() {
            // d/dt(t^2 - 2t + 1) = 2t - 2
            assertEquals(List.of(2, -2), CharacteristicChecker.polyDerivative(List.of(1, -2, 1)));
        }

        @Test
        void toStringBasic() {
            assertEquals("t - 1", CharacteristicChecker.polyToString(List.of(1, -1)));
            assertEquals("t^2 - 1", CharacteristicChecker.polyToString(List.of(1, 0, -1)));
            assertEquals("0", CharacteristicChecker.polyToString(List.of(0)));
        }
    }

    // -----------------------------------------------------------------------
    // Characteristic polynomial
    // -----------------------------------------------------------------------

    @Nested
    class CharacteristicPoly {

        @Test
        void endPoly() {
            var ss = build("end");
            var coeffs = CharacteristicChecker.characteristicPolynomial(ss);
            assertEquals(List.of(1), coeffs);
        }

        @Test
        void singleBranch() {
            // &{a: end}: chain of 2, chi(t) = t - 1
            var ss = build("&{a: end}");
            var coeffs = CharacteristicChecker.characteristicPolynomial(ss);
            assertEquals(List.of(1, -1), coeffs);
        }

        @Test
        void chainLength2EvalAt0() {
            var ss = build("&{a: &{b: end}}");
            var coeffs = CharacteristicChecker.characteristicPolynomial(ss);
            assertEquals(0, CharacteristicChecker.polyEvaluate(coeffs, 0));
        }

        @ParameterizedTest
        @CsvSource({
                "&{a: end}",
                "&{a: &{b: end}}",
                "(&{a: end} || &{b: end})"
        })
        void evalAt0EqualsMobiusValue(String type) {
            var ss = build(type);
            var coeffs = CharacteristicChecker.characteristicPolynomial(ss);
            var mu = Zeta.mobiusFunction(ss);
            int mobiusVal = mu.getOrDefault(new Zeta.Pair(ss.top(), ss.bottom()), 0);
            assertEquals(mobiusVal, (int) CharacteristicChecker.polyEvaluate(coeffs, 0));
        }

        @ParameterizedTest
        @CsvSource({
                "&{a: end}",
                "&{a: &{b: end}}",
                "(&{a: end} || &{b: end})"
        })
        void leadingCoefficientIsOne(String type) {
            var ss = build(type);
            var coeffs = CharacteristicChecker.characteristicPolynomial(ss);
            assertEquals(1, coeffs.get(0));
        }

        @Test
        void parallelPolynomial() {
            var ss = build("(&{a: end} || &{b: end})");
            var coeffs = CharacteristicChecker.characteristicPolynomial(ss);
            // Product of two (t-1): (t-1)^2 = t^2 - 2t + 1
            assertEquals(List.of(1, -2, 1), coeffs);
        }
    }

    // -----------------------------------------------------------------------
    // Whitney numbers
    // -----------------------------------------------------------------------

    @Nested
    class WhitneyNumbers {

        @Test
        void chainWhitneyFirst() {
            var ss = build("&{a: end}");
            var w = CharacteristicChecker.whitneyNumbersFirst(ss);
            assertEquals(1, w.getOrDefault(0, 0));
            assertEquals(-1, w.getOrDefault(1, 0));
        }

        @Test
        void chainWhitneySecond() {
            var ss = build("&{a: &{b: end}}");
            var W = CharacteristicChecker.whitneyNumbersSecond(ss);
            assertEquals(1, W.getOrDefault(0, 0));
            assertEquals(1, W.getOrDefault(1, 0));
            assertEquals(1, W.getOrDefault(2, 0));
        }

        @Test
        void parallelWhitneySecond() {
            var ss = build("(&{a: end} || &{b: end})");
            var W = CharacteristicChecker.whitneyNumbersSecond(ss);
            assertEquals(2, W.getOrDefault(1, 0));
        }
    }

    // -----------------------------------------------------------------------
    // Log-concavity
    // -----------------------------------------------------------------------

    @Nested
    class LogConcavity {

        @Test
        void constantSequence() {
            assertTrue(CharacteristicChecker.checkLogConcave(List.of(1, 1, 1)));
        }

        @Test
        void unimodal() {
            assertTrue(CharacteristicChecker.checkLogConcave(List.of(1, 2, 1)));
        }

        @Test
        void geometric() {
            assertTrue(CharacteristicChecker.checkLogConcave(List.of(1, 2, 4)));
        }

        @Test
        void notLogConcave() {
            assertFalse(CharacteristicChecker.checkLogConcave(List.of(1, 0, 1)));
        }

        @Test
        void singleElement() {
            assertTrue(CharacteristicChecker.checkLogConcave(List.of(5)));
        }

        @Test
        void twoElements() {
            assertTrue(CharacteristicChecker.checkLogConcave(List.of(3, 7)));
        }

        @ParameterizedTest
        @CsvSource({
                "&{a: end}",
                "&{a: &{b: end}}",
                "(&{a: end} || &{b: end})"
        })
        void benchmarkLogConcavity(String type) {
            var ss = build(type);
            assertTrue(CharacteristicChecker.isWhitneyLogConcave(ss));
        }
    }

    // -----------------------------------------------------------------------
    // Factorization
    // -----------------------------------------------------------------------

    @Nested
    class Factorization {

        @Test
        void parallelFactors() {
            var ss1 = build("&{a: end}");
            var ss2 = build("&{b: end}");
            var ssProd = build("(&{a: end} || &{b: end})");
            assertTrue(CharacteristicChecker.verifyFactorization(ss1, ss2, ssProd));
        }

        @Test
        void parallelChainFactors() {
            var ss1 = build("&{a: end}");
            var ss2 = build("&{b: &{c: end}}");
            var ssProd = build("(&{a: end} || &{b: &{c: end}})");
            assertTrue(CharacteristicChecker.verifyFactorization(ss1, ss2, ssProd));
        }

        @Test
        void parallelBothChains() {
            var ss1 = build("&{a: &{b: end}}");
            var ss2 = build("&{c: end}");
            var ssProd = build("(&{a: &{b: end}} || &{c: end})");
            assertTrue(CharacteristicChecker.verifyFactorization(ss1, ss2, ssProd));
        }
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    @Nested
    class Analyze {

        @Test
        void endAnalysis() {
            var r = CharacteristicChecker.analyzeCharacteristic(build("end"));
            assertEquals(0, r.degree());
            assertEquals(List.of(1), r.coefficients());
            assertEquals(1, r.evalAt0());
        }

        @Test
        void branchAnalysis() {
            var r = CharacteristicChecker.analyzeCharacteristic(build("&{a: end}"));
            assertEquals(1, r.degree());
            assertEquals(-1, r.evalAt0());
            assertEquals(0, r.evalAt1());
        }

        @Test
        void parallelAnalysis() {
            var r = CharacteristicChecker.analyzeCharacteristic(build("(&{a: end} || &{b: end})"));
            assertEquals(2, r.degree());
            assertEquals(1, r.evalAt0());
            assertEquals(0, r.evalAt1());
            assertTrue(r.isLogConcave());
        }
    }

    // -----------------------------------------------------------------------
    // Benchmark tests
    // -----------------------------------------------------------------------

    @Nested
    class Benchmarks {

        @ParameterizedTest
        @CsvSource({
                "'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}'",
                "'&{open: &{read: end, write: end}}'",
                "'&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}'",
                "'&{a: end, b: end}'",
                "'&{a: &{b: &{c: end}}}'",
                "'(&{a: end} || &{b: end})'"
        })
        void polynomialProperties(String type) {
            var ss = build(type);
            var r = CharacteristicChecker.analyzeCharacteristic(ss);
            assertEquals(r.degree(), r.height());
            assertEquals(1, r.coefficients().get(0)); // monic
            var mu = Zeta.mobiusFunction(ss);
            int mobiusVal = mu.getOrDefault(new Zeta.Pair(ss.top(), ss.bottom()), 0);
            assertEquals(mobiusVal, r.evalAt0());
        }

        @ParameterizedTest
        @CsvSource({
                "'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}'",
                "'&{open: &{read: end, write: end}}'",
                "'&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}'",
                "'&{a: end, b: end}'",
                "'&{a: &{b: &{c: end}}}'",
                "'(&{a: end} || &{b: end})'"
        })
        void logConcavity(String type) {
            var ss = build(type);
            assertTrue(CharacteristicChecker.isWhitneyLogConcave(ss));
        }

        @ParameterizedTest
        @CsvSource({
                "'rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}'",
                "'&{open: &{read: end, write: end}}'",
                "'&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}'",
                "'&{a: end, b: end}'",
                "'&{a: &{b: &{c: end}}}'",
                "'(&{a: end} || &{b: end})'"
        })
        void whitneySum(String type) {
            var ss = build(type);
            var W = CharacteristicChecker.whitneyNumbersSecond(ss);
            var r = CharacteristicChecker.analyzeCharacteristic(ss);
            assertEquals(r.numStates(), W.values().stream().mapToInt(Integer::intValue).sum());
        }
    }
}
