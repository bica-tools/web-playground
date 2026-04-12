package com.bica.reborn.order_polynomial;

import com.bica.reborn.order_polynomial.OrderPolynomialChecker.OrderPolynomialResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java port of {@code reticulate/tests/test_order_polynomial.py} (28 tests).
 */
class OrderPolynomialCheckerTest {

    private static StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // ===================================================================
    // Order polynomial values
    // ===================================================================

    @Nested
    class OrderPolynomialValues {

        @Test
        void endOmega() {
            // Single state: Omega(P, t) = t
            StateSpace ss = build("end");
            List<Integer> vals = OrderPolynomialChecker.orderPolynomialValues(ss, 5);
            assertEquals(0, vals.get(0));
            assertEquals(1, vals.get(1));
            assertEquals(2, vals.get(2));
            assertEquals(3, vals.get(3));
        }

        @Test
        void chain2Omega() {
            // Chain of 2: Omega(P, t) = t(t+1)/2 = C(t+1, 2)
            StateSpace ss = build("&{a: end}");
            List<Integer> vals = OrderPolynomialChecker.orderPolynomialValues(ss, 5);
            assertEquals(0, vals.get(0));
            assertEquals(1, vals.get(1));
            assertEquals(3, vals.get(2));
            assertEquals(6, vals.get(3));
        }

        @Test
        void chain3Omega() {
            // Chain of 3: Omega(P, t) = C(t+2, 3)
            StateSpace ss = build("&{a: &{b: end}}");
            List<Integer> vals = OrderPolynomialChecker.orderPolynomialValues(ss, 5);
            assertEquals(0, vals.get(0));
            assertEquals(1, vals.get(1));
            assertEquals(4, vals.get(2));
            assertEquals(10, vals.get(3));
        }

        @Test
        void omegaNonnegative() {
            StateSpace ss = build("&{a: end, b: end}");
            List<Integer> vals = OrderPolynomialChecker.orderPolynomialValues(ss, 5);
            for (int v : vals) {
                assertTrue(v >= 0, "Omega(P, t) must be non-negative");
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {"end", "&{a: end}", "&{a: end, b: end}"})
        void omegaZeroAtZero(String type) {
            StateSpace ss = build(type);
            List<Integer> vals = OrderPolynomialChecker.orderPolynomialValues(ss, 1);
            assertEquals(0, vals.get(0));
        }

        @Test
        void omegaMonotone() {
            StateSpace ss = build("&{a: &{b: end}}");
            List<Integer> vals = OrderPolynomialChecker.orderPolynomialValues(ss, 8);
            for (int i = 0; i < vals.size() - 1; i++) {
                assertTrue(vals.get(i) <= vals.get(i + 1),
                        "Omega(P, t) must be non-decreasing");
            }
        }
    }

    // ===================================================================
    // Strict order polynomial
    // ===================================================================

    @Nested
    class StrictOrderPolynomial {

        @Test
        void endStrict() {
            StateSpace ss = build("end");
            List<Integer> vals = OrderPolynomialChecker.strictOrderPolynomialValues(ss, 5);
            assertEquals(1, vals.get(1));
            assertEquals(2, vals.get(2));
        }

        @Test
        void chain2Strict() {
            // Omega_bar(P, t) = C(t, 2) = t(t-1)/2
            StateSpace ss = build("&{a: end}");
            List<Integer> vals = OrderPolynomialChecker.strictOrderPolynomialValues(ss, 5);
            assertEquals(0, vals.get(0));
            assertEquals(0, vals.get(1));
            assertEquals(1, vals.get(2));
            assertEquals(3, vals.get(3));
        }

        @Test
        void chain3Strict() {
            // Omega_bar(P, t) = C(t, 3)
            StateSpace ss = build("&{a: &{b: end}}");
            List<Integer> vals = OrderPolynomialChecker.strictOrderPolynomialValues(ss, 5);
            assertEquals(0, vals.get(0));
            assertEquals(0, vals.get(1));
            assertEquals(0, vals.get(2));
            assertEquals(1, vals.get(3));
            assertEquals(4, vals.get(4));
        }

        @Test
        void strictLeqNonstrict() {
            StateSpace ss = build("&{a: end, b: end}");
            List<Integer> vals = OrderPolynomialChecker.orderPolynomialValues(ss, 5);
            List<Integer> strict = OrderPolynomialChecker.strictOrderPolynomialValues(ss, 5);
            for (int i = 0; i < vals.size(); i++) {
                assertTrue(strict.get(i) <= vals.get(i),
                        "Strict must be <= non-strict at t=" + i);
            }
        }
    }

    // ===================================================================
    // Coefficients
    // ===================================================================

    @Nested
    class Coefficients {

        @Test
        void endCoefficients() {
            StateSpace ss = build("end");
            List<Double> coeffs = OrderPolynomialChecker.orderPolynomialCoefficients(ss);
            assertTrue(coeffs.size() >= 1);
        }

        @Test
        void coefficientsLength() {
            StateSpace ss = build("&{a: &{b: end}}");
            List<Double> coeffs = OrderPolynomialChecker.orderPolynomialCoefficients(ss);
            // 3 states -> degree 3 polynomial (n+1 = 4 coefficients)
            assertEquals(4, coeffs.size());
        }

        @Test
        void coefficientsEvaluate() {
            StateSpace ss = build("&{a: end, b: end}");
            List<Double> coeffs = OrderPolynomialChecker.orderPolynomialCoefficients(ss);
            List<Integer> vals = OrderPolynomialChecker.orderPolynomialValues(ss, 5);
            for (int t = 0; t <= 5; t++) {
                double polyVal = 0;
                for (int k = 0; k < coeffs.size(); k++) {
                    polyVal += coeffs.get(k) * Math.pow(t, k);
                }
                assertEquals((long) vals.get(t), Math.round(polyVal),
                        "Polynomial must match at t=" + t);
            }
        }
    }

    // ===================================================================
    // Linear extensions
    // ===================================================================

    @Nested
    class LinearExtensions {

        @ParameterizedTest
        @ValueSource(strings = {"end", "&{a: end}", "&{a: &{b: end}}"})
        void chainOneExtension(String type) {
            StateSpace ss = build(type);
            assertEquals(1, OrderPolynomialChecker.numLinearExtensions(ss));
        }

        @Test
        void parallelExtensions() {
            // 2x2 grid (diamond): 2 linear extensions
            StateSpace ss = build("(&{a: end} || &{b: end})");
            assertEquals(2, OrderPolynomialChecker.numLinearExtensions(ss));
        }

        @Test
        void diamondExtensions() {
            StateSpace ss = build("&{a: &{c: end}, b: &{c: end}}");
            int nExt = OrderPolynomialChecker.numLinearExtensions(ss);
            assertTrue(nExt >= 2, "Diamond must have at least 2 extensions");
        }

        @ParameterizedTest
        @CsvSource({
                "end",
                "&{a: end}",
                "'+{a: end, b: end}'",
                "'(&{a: end} || &{b: end})'"
        })
        void extensionsPositive(String type) {
            StateSpace ss = build(type);
            assertTrue(OrderPolynomialChecker.numLinearExtensions(ss) >= 1);
        }
    }

    // ===================================================================
    // Full analysis
    // ===================================================================

    @Nested
    class AnalyzeTests {

        @Test
        void endAnalysis() {
            StateSpace ss = build("end");
            OrderPolynomialResult result = OrderPolynomialChecker.analyze(ss);
            assertNotNull(result);
            assertEquals(1, result.numStates());
            assertTrue(result.numLinearExtensions() >= 1);
        }

        @Test
        void chainAnalysis() {
            StateSpace ss = build("&{a: &{b: end}}");
            OrderPolynomialResult result = OrderPolynomialChecker.analyze(ss);
            assertEquals(3, result.numStates());
            assertEquals(1, result.numLinearExtensions());
            assertTrue(result.height() >= 2);
        }

        @Test
        void parallelAnalysis() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            OrderPolynomialResult result = OrderPolynomialChecker.analyze(ss);
            assertEquals(4, result.numStates());
            assertEquals(2, result.numLinearExtensions());
        }

        @Test
        void branchAnalysis() {
            StateSpace ss = build("&{a: end, b: end}");
            OrderPolynomialResult result = OrderPolynomialChecker.analyze(ss);
            assertTrue(result.numStates() >= 2);
            assertTrue(result.numLinearExtensions() >= 1);
        }

        @Test
        void recursiveAnalysis() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            OrderPolynomialResult result = OrderPolynomialChecker.analyze(ss);
            assertTrue(result.numStates() >= 2);
            assertTrue(result.numLinearExtensions() >= 1);
        }

        @Test
        void selectionAnalysis() {
            StateSpace ss = build("+{a: end, b: end}");
            OrderPolynomialResult result = OrderPolynomialChecker.analyze(ss);
            assertTrue(result.numStates() >= 2);
        }

        @Test
        void valuesTuple() {
            StateSpace ss = build("&{a: end}");
            OrderPolynomialResult result = OrderPolynomialChecker.analyze(ss);
            assertNotNull(result.values());
            assertNotNull(result.strictValues());
        }

        @Test
        void resultConsistency() {
            StateSpace ss = build("&{a: end, b: end}");
            OrderPolynomialResult result = OrderPolynomialChecker.analyze(ss);
            assertEquals(9, result.values().size()); // maxT=8 -> 0..8
            assertEquals(0, result.values().get(0));
        }
    }

    // ===================================================================
    // Benchmark-like protocols
    // ===================================================================

    @Nested
    class Benchmarks {

        @Test
        void smtpLike() {
            StateSpace ss = build("&{ehlo: &{mail: &{data: end}}}");
            OrderPolynomialResult result = OrderPolynomialChecker.analyze(ss);
            assertEquals(1, result.numLinearExtensions()); // Chain
        }

        @Test
        void iteratorLike() {
            StateSpace ss = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            OrderPolynomialResult result = OrderPolynomialChecker.analyze(ss);
            assertTrue(result.numLinearExtensions() >= 1);
        }

        @Test
        void twoBuyerLike() {
            StateSpace ss = build("&{quote: &{accept: end, reject: end}}");
            OrderPolynomialResult result = OrderPolynomialChecker.analyze(ss);
            assertTrue(result.numLinearExtensions() >= 1);
        }
    }
}
