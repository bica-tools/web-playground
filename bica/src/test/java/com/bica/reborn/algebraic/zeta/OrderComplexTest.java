package com.bica.reborn.algebraic.zeta;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OrderComplex} — Java port of Python
 * {@code test_order_complex.py} (29 tests, Steps 30u-30x).
 */
class OrderComplexTest {

    private static StateSpace ss(String t) {
        return StateSpaceBuilder.build(Parser.parse(t));
    }

    // -----------------------------------------------------------------------
    // f-vector
    // -----------------------------------------------------------------------

    @Nested
    class FVector {
        @Test
        void end() {
            List<Integer> fv = OrderComplex.fVector(ss("end"));
            assertEquals(1, fv.get(0)); // f_{-1} = 1
        }

        @Test
        void singleBranch() {
            List<Integer> fv = OrderComplex.fVector(ss("&{a: end}"));
            assertEquals(1, fv.get(0));
        }

        @Test
        void chain3() {
            List<Integer> fv = OrderComplex.fVector(ss("&{a: &{b: end}}"));
            assertEquals(1, fv.get(0)); // f_{-1}
            assertEquals(1, fv.get(1)); // f_0: one vertex in open interval
        }

        @Test
        void fNeg1AlwaysOne() {
            for (String t : List.of("end", "&{a: end}", "&{a: &{b: end}}", "(&{a: end} || &{b: end})")) {
                assertEquals(1, OrderComplex.fVector(ss(t)).get(0), t);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Dimension
    // -----------------------------------------------------------------------

    @Nested
    class Dimension {
        @Test
        void end() {
            assertEquals(-1, OrderComplex.dimension(ss("end")));
        }

        @Test
        void singleBranch() {
            assertEquals(-1, OrderComplex.dimension(ss("&{a: end}")));
        }

        @Test
        void chain3() {
            assertEquals(0, OrderComplex.dimension(ss("&{a: &{b: end}}")));
        }

        @Test
        void chain4() {
            assertTrue(OrderComplex.dimension(ss("&{a: &{b: &{c: end}}}")) >= 0);
        }
    }

    // -----------------------------------------------------------------------
    // Euler
    // -----------------------------------------------------------------------

    @Nested
    class Euler {
        @Test
        void end() {
            assertEquals(0, OrderComplex.eulerCharacteristic(ss("end")));
        }

        @Test
        void chain3() {
            // Merely check it is an int (Python test asserts isinstance int).
            int chi = OrderComplex.eulerCharacteristic(ss("&{a: &{b: end}}"));
            // Cross-checked against Python: chi = 1
            assertEquals(1, chi);
        }

        @Test
        void reducedMatchesMobius() {
            // Python test only checks int-ness; we additionally cross-check
            // values computed from the reference implementation.
            assertEquals(-1, OrderComplex.reducedEulerCharacteristic(ss("&{a: end}")));
            assertEquals(0, OrderComplex.reducedEulerCharacteristic(ss("&{a: &{b: end}}")));
        }
    }

    // -----------------------------------------------------------------------
    // Maximal chains
    // -----------------------------------------------------------------------

    @Nested
    class MaximalChains {
        @Test
        void end() {
            int n = OrderComplex.numMaximalChains(ss("end"));
            assertTrue(n >= 0);
        }

        @Test
        void chain2() {
            assertEquals(1, OrderComplex.numMaximalChains(ss("&{a: end}")));
        }

        @Test
        void chain3() {
            assertEquals(1, OrderComplex.numMaximalChains(ss("&{a: &{b: end}}")));
        }

        @Test
        void twoBranches() {
            assertTrue(OrderComplex.numMaximalChains(ss("&{a: end, b: end}")) >= 1);
        }

        @Test
        void parallelMultiple() {
            assertTrue(OrderComplex.numMaximalChains(ss("(&{a: end} || &{b: end})")) >= 2);
        }
    }

    // -----------------------------------------------------------------------
    // Shellability
    // -----------------------------------------------------------------------

    @Nested
    class Shellability {
        @Test
        void chainShellable() {
            assertTrue(OrderComplex.isShellable(ss("&{a: end}")));
            assertTrue(OrderComplex.isShellable(ss("&{a: &{b: end}}")));
        }

        @Test
        void parallelShellable() {
            assertTrue(OrderComplex.isShellable(ss("(&{a: end} || &{b: end})")));
        }

        @Test
        void singleState() {
            assertTrue(OrderComplex.isShellable(ss("end")));
        }
    }

    // -----------------------------------------------------------------------
    // Morse
    // -----------------------------------------------------------------------

    @Nested
    class Morse {
        @Test
        void end() {
            assertTrue(OrderComplex.morseNumber(ss("end")) >= 0);
        }

        @Test
        void chain() {
            assertTrue(OrderComplex.morseNumber(ss("&{a: end}")) >= 0);
        }

        @Test
        void parallel() {
            assertTrue(OrderComplex.morseNumber(ss("(&{a: end} || &{b: end})")) >= 0);
        }
    }

    // -----------------------------------------------------------------------
    // h-vector
    // -----------------------------------------------------------------------

    @Nested
    class HVector {
        @Test
        void end() {
            assertEquals(List.of(1), OrderComplex.hVector(ss("end")));
        }

        @Test
        void chain() {
            List<Integer> hv = OrderComplex.hVector(ss("&{a: end}"));
            assertTrue(hv.size() >= 1);
            assertEquals(1, hv.get(0));
        }
    }

    // -----------------------------------------------------------------------
    // Analyze
    // -----------------------------------------------------------------------

    @Nested
    class Analyze {
        @Test
        void end() {
            var r = OrderComplex.analyzeOrderComplex(ss("end"));
            assertEquals(-1, r.dimension());
            assertEquals(1, r.fVector().get(0));
        }

        @Test
        void chain() {
            var r = OrderComplex.analyzeOrderComplex(ss("&{a: &{b: end}}"));
            assertEquals(1, r.numMaximalChains());
            assertTrue(r.isShellable());
        }

        @Test
        void parallel() {
            var r = OrderComplex.analyzeOrderComplex(ss("(&{a: end} || &{b: end})"));
            assertTrue(r.numMaximalChains() >= 2);
            assertTrue(r.isShellable());
        }
    }

    // -----------------------------------------------------------------------
    // Benchmarks
    // -----------------------------------------------------------------------

    @Nested
    class Benchmarks {
        private static final List<String> BENCHMARKS = List.of(
                "&{open: &{read: end, write: end}}",
                "&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}",
                "&{a: end, b: end}",
                "&{a: &{b: &{c: end}}}",
                "(&{a: end} || &{b: end})"
        );

        @Test
        void orderComplexRuns() {
            for (String typ : BENCHMARKS) {
                var r = OrderComplex.analyzeOrderComplex(ss(typ));
                assertEquals(1, r.fVector().get(0), typ);
                assertTrue(r.numMaximalChains() >= 1, typ);
                assertTrue(r.morseNumber() >= 0, typ);
            }
        }

        @Test
        void eulerInteger() {
            for (String typ : BENCHMARKS) {
                // Just make sure the call succeeds.
                OrderComplex.eulerCharacteristic(ss(typ));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Cross-validation against Python reference values
    // -----------------------------------------------------------------------

    @Nested
    class PythonCrossCheck {
        @Test
        void chain4FVector() {
            // &{a: &{b: &{c: end}}} → fv = [1, 2, 1], dim = 1
            var r = OrderComplex.analyzeOrderComplex(ss("&{a: &{b: &{c: end}}}"));
            assertEquals(List.of(1, 2, 1), r.fVector());
            assertEquals(1, r.dimension());
            assertEquals(1, r.eulerCharacteristic());
        }

        @Test
        void parallelDiamond() {
            // (&{a: end} || &{b: end}) → fv=[1,2], chi=2, chi~=1, nmax=2, morse=2
            var r = OrderComplex.analyzeOrderComplex(ss("(&{a: end} || &{b: end})"));
            assertEquals(List.of(1, 2), r.fVector());
            assertEquals(2, r.eulerCharacteristic());
            assertEquals(1, r.reducedEuler());
            assertEquals(2, r.numMaximalChains());
            assertEquals(2, r.morseNumber());
            assertEquals(List.of(1, 1), r.hVector());
        }

        @Test
        void fileObject() {
            // &{open: &{read: end, write: end}} → fv=[1,1], dim=0, chi=1
            var r = OrderComplex.analyzeOrderComplex(ss("&{open: &{read: end, write: end}}"));
            assertEquals(List.of(1, 1), r.fVector());
            assertEquals(0, r.dimension());
            assertEquals(1, r.eulerCharacteristic());
        }
    }
}
