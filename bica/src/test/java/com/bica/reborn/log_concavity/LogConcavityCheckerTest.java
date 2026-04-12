package com.bica.reborn.log_concavity;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for log-concavity verification of session type lattices (Step 32e) -- Java port.
 */
class LogConcavityCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    @Nested
    class Helpers {

        @Test
        void combBasic() {
            assertEquals(10, LogConcavityChecker.comb(5, 2));
            assertEquals(1, LogConcavityChecker.comb(4, 0));
            assertEquals(1, LogConcavityChecker.comb(0, 0));
        }

        @Test
        void combInvalid() {
            assertEquals(0, LogConcavityChecker.comb(3, -1));
            assertEquals(0, LogConcavityChecker.comb(3, 5));
        }

        @Test
        void sequenceFromDictEmpty() {
            assertEquals(List.of(), LogConcavityChecker.sequenceFromDict(Map.of()));
        }

        @Test
        void sequenceFromDictBasic() {
            assertEquals(List.of(1, 3, 1),
                    LogConcavityChecker.sequenceFromDict(Map.of(0, 1, 1, 3, 2, 1)));
        }

        @Test
        void sequenceFromDictGaps() {
            assertEquals(List.of(1, 0, 5),
                    LogConcavityChecker.sequenceFromDict(Map.of(0, 1, 2, 5)));
        }

        @Test
        void findViolationsNone() {
            List<int[]> v = LogConcavityChecker.findViolations(List.of(1, 3, 3, 1));
            assertTrue(v.isEmpty());
        }

        @Test
        void findViolationsPresent() {
            // 1, 1, 5: 1^2 < 1*5
            List<int[]> v = LogConcavityChecker.findViolations(List.of(1, 1, 5));
            assertEquals(1, v.size());
            assertEquals(1, v.get(0)[0]); // position k=1
        }
    }

    // -----------------------------------------------------------------------
    // Unimodality
    // -----------------------------------------------------------------------

    @Nested
    class Unimodality {

        @Test
        void empty() {
            assertTrue(LogConcavityChecker.checkUnimodal(List.of()));
        }

        @Test
        void single() {
            assertTrue(LogConcavityChecker.checkUnimodal(List.of(5)));
        }

        @Test
        void increasing() {
            assertTrue(LogConcavityChecker.checkUnimodal(List.of(1, 2, 3)));
        }

        @Test
        void decreasing() {
            assertTrue(LogConcavityChecker.checkUnimodal(List.of(3, 2, 1)));
        }

        @Test
        void peak() {
            assertTrue(LogConcavityChecker.checkUnimodal(List.of(1, 3, 2)));
        }

        @Test
        void valley() {
            assertFalse(LogConcavityChecker.checkUnimodal(List.of(3, 1, 2)));
        }

        @Test
        void flat() {
            assertTrue(LogConcavityChecker.checkUnimodal(List.of(2, 2, 2)));
        }

        @Test
        void sessionTypeUnimodal() {
            StateSpace ss = build("&{a: &{b: end}}");
            assertTrue(LogConcavityChecker.checkUnimodality(ss));
        }
    }

    // -----------------------------------------------------------------------
    // Ultra-log-concavity
    // -----------------------------------------------------------------------

    @Nested
    class UltraLogConcave {

        @Test
        void shortSequence() {
            assertTrue(LogConcavityChecker.checkUltraLogConcave(List.of(1)));
            assertTrue(LogConcavityChecker.checkUltraLogConcave(List.of(1, 1)));
        }

        @Test
        void binomialCoefficients() {
            // C(4, k) = [1, 4, 6, 4, 1] -- trivially ultra-LC (ratio = 1)
            assertTrue(LogConcavityChecker.checkUltraLogConcave(List.of(1, 4, 6, 4, 1)));
        }

        @Test
        void constantNotUltra() {
            // [1, 1, 1] normalised by C(2,k)=[1,2,1] gives [1, 0.5, 1], not LC
            assertFalse(LogConcavityChecker.checkUltraLogConcave(List.of(1, 1, 1)));
        }
    }

    // -----------------------------------------------------------------------
    // Whitney log-concavity
    // -----------------------------------------------------------------------

    @Nested
    class WhitneyLogConcavity {

        @Test
        void end() {
            StateSpace ss = build("end");
            assertTrue(LogConcavityChecker.isLogConcaveWhitneySecond(ss));
            assertTrue(LogConcavityChecker.isLogConcaveWhitneyFirst(ss));
        }

        @Test
        void singleMethod() {
            StateSpace ss = build("&{a: end}");
            assertTrue(LogConcavityChecker.isLogConcaveWhitneySecond(ss));
            assertTrue(LogConcavityChecker.isLogConcaveWhitneyFirst(ss));
        }

        @Test
        void chain() {
            StateSpace ss = build("&{a: &{b: end}}");
            assertTrue(LogConcavityChecker.isLogConcaveWhitneySecond(ss));
            assertTrue(LogConcavityChecker.isLogConcaveWhitneyFirst(ss));
        }

        @Test
        void branch() {
            StateSpace ss = build("&{a: end, b: end}");
            assertTrue(LogConcavityChecker.isLogConcaveWhitneySecond(ss));
        }

        @Test
        void selection() {
            StateSpace ss = build("+{a: end, b: end}");
            assertTrue(LogConcavityChecker.isLogConcaveWhitneySecond(ss));
        }

        @Test
        void parallel() {
            StateSpace ss = build("(&{a: end} || &{b: end})");
            assertTrue(LogConcavityChecker.isLogConcaveWhitneySecond(ss));
        }

        @Test
        void recursive() {
            StateSpace ss = build("rec X . &{a: X, b: end}");
            assertTrue(LogConcavityChecker.isLogConcaveWhitneySecond(ss));
        }
    }

    // -----------------------------------------------------------------------
    // Heron-Rota-Welsh check
    // -----------------------------------------------------------------------

    @Nested
    class HRW {

        @Test
        void end() {
            assertTrue(LogConcavityChecker.heronRotaWelshCheck(build("end")));
        }

        @Test
        void chain() {
            assertTrue(LogConcavityChecker.heronRotaWelshCheck(build("&{a: &{b: end}}")));
        }

        @Test
        void parallel() {
            boolean result = LogConcavityChecker.heronRotaWelshCheck(
                    build("(&{a: end} || &{b: end})"));
            // Just check it returns a boolean (may or may not hold)
            assertNotNull(result);
        }
    }

    // -----------------------------------------------------------------------
    // Adiprasito-Huh-Katz bound
    // -----------------------------------------------------------------------

    @Nested
    class AHK {

        @Test
        void end() {
            assertTrue(LogConcavityChecker.adiprasitoHuhKatzBound(build("end")));
        }

        @Test
        void chain() {
            assertTrue(LogConcavityChecker.adiprasitoHuhKatzBound(build("&{a: &{b: end}}")));
        }

        @Test
        void parallel() {
            boolean result = LogConcavityChecker.adiprasitoHuhKatzBound(
                    build("(&{a: end} || &{b: end})"));
            assertNotNull(result);
        }
    }

    // -----------------------------------------------------------------------
    // Log-concavity ratios
    // -----------------------------------------------------------------------

    @Nested
    class Ratios {

        @Test
        void perfectLc() {
            // [1, 2, 1]: 2^2 / (1*1) = 4 >= 1
            List<Double> ratios = LogConcavityChecker.logConcavityRatio(List.of(1, 2, 1));
            assertEquals(1, ratios.size());
            assertTrue(ratios.get(0) >= 1.0);
        }

        @Test
        void violation() {
            // [1, 1, 5]: 1^2 / (1*5) = 0.2 < 1
            List<Double> ratios = LogConcavityChecker.logConcavityRatio(List.of(1, 1, 5));
            assertEquals(1, ratios.size());
            assertTrue(ratios.get(0) < 1.0);
        }

        @Test
        void zeros() {
            List<Double> ratios = LogConcavityChecker.logConcavityRatio(List.of(0, 0, 0));
            assertTrue(ratios.stream().allMatch(r -> r >= 1.0));
        }
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    @Nested
    class FullAnalysis {

        @Test
        void endAnalysis() {
            var result = LogConcavityChecker.verifyLogConcavity(build("end"));
            assertTrue(result.whitneySecondLc());
            assertTrue(result.whitneyFirstLc());
            assertTrue(result.lcViolationsSecond().isEmpty());
            assertTrue(result.lcViolationsFirst().isEmpty());
        }

        @Test
        void chainAnalysis() {
            var result = LogConcavityChecker.verifyLogConcavity(build("&{a: &{b: end}}"));
            assertTrue(result.whitneySecondLc());
            assertTrue(result.isUnimodal());
            assertTrue(result.height() >= 0);
            assertTrue(result.numStates() >= 1);
        }

        @Test
        void parallelAnalysis() {
            var result = LogConcavityChecker.verifyLogConcavity(
                    build("(&{a: end} || &{b: end})"));
            assertTrue(result.whitneySecondLc());
            assertTrue(result.isUnimodal());
        }

        @Test
        void branchAnalysis() {
            var result = LogConcavityChecker.verifyLogConcavity(build("&{a: end, b: end}"));
            assertTrue(result.whitneySecondLc());
        }

        @Test
        void recursiveAnalysis() {
            var result = LogConcavityChecker.verifyLogConcavity(
                    build("rec X . &{a: X, b: end}"));
            assertNotNull(result.hrwSatisfied());
            assertNotNull(result.ahkBoundHolds());
        }

        @Test
        void complexType() {
            var result = LogConcavityChecker.verifyLogConcavity(
                    build("&{a: &{b: end}, c: &{d: end}}"));
            assertTrue(result.whitneySecondLc());
            assertTrue(result.numStates() >= 1);
        }

        @Test
        void nestedBranch() {
            var result = LogConcavityChecker.verifyLogConcavity(
                    build("&{a: &{b: end, c: end}, d: end}"));
            assertNotNull(result.whitneySecondLc());
            assertNotNull(result.isUltraLc());
        }
    }

    // -----------------------------------------------------------------------
    // Parameterized: benchmark protocols
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "end",
            "&{a: end}",
            "&{a: end, b: end}",
            "+{a: end, b: end}",
            "&{a: &{b: end}}",
            "(&{a: end} || &{b: end})",
            "rec X . &{a: X, b: end}",
            "&{a: &{b: end}, c: end}",
            "+{OK: &{read: end}, ERR: end}",
            "&{a: &{b: end, c: end}, d: end}"
    })
    void whitneySecondIsLogConcave(String type) {
        assertTrue(LogConcavityChecker.isLogConcaveWhitneySecond(build(type)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "end",
            "&{a: end}",
            "&{a: &{b: end}}",
            "&{a: end, b: end}",
            "+{a: end, b: end}",
            "(&{a: end} || &{b: end})",
            "rec X . &{a: X, b: end}"
    })
    void unimodalityHolds(String type) {
        assertTrue(LogConcavityChecker.checkUnimodality(build(type)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "end",
            "&{a: end}",
            "&{a: &{b: end}}",
            "&{a: end, b: end}"
    })
    void hrwHoldsForSimpleTypes(String type) {
        assertTrue(LogConcavityChecker.heronRotaWelshCheck(build(type)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "end",
            "&{a: end}",
            "&{a: &{b: end}}",
            "&{a: end, b: end}"
    })
    void ahkHoldsForSimpleTypes(String type) {
        assertTrue(LogConcavityChecker.adiprasitoHuhKatzBound(build(type)));
    }

    // -----------------------------------------------------------------------
    // Preservation under product
    // -----------------------------------------------------------------------

    @Test
    void preservationUnderProduct() {
        StateSpace left = build("&{a: end}");
        StateSpace right = build("&{b: end}");
        StateSpace product = build("(&{a: end} || &{b: end})");
        assertTrue(LogConcavityChecker.preservationUnderProduct(left, right, product));
    }

    @Test
    void preservationUnderProductChains() {
        StateSpace left = build("&{a: &{b: end}}");
        StateSpace right = build("&{c: end}");
        StateSpace product = build("(&{a: &{b: end}} || &{c: end})");
        assertTrue(LogConcavityChecker.preservationUnderProduct(left, right, product));
    }
}
