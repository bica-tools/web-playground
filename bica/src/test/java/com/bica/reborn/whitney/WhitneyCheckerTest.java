package com.bica.reborn.whitney;

import com.bica.reborn.algebraic.zeta.Zeta;
import com.bica.reborn.characteristic.CharacteristicChecker;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Whitney number analysis (Step 30g) -- Java port.
 */
class WhitneyCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Rank profile
    // -----------------------------------------------------------------------

    @Nested
    class RankProfile {

        @Test
        void endProfile() {
            assertEquals(List.of(1), WhitneyChecker.rankProfile(build("end")));
        }

        @Test
        void chain2() {
            assertEquals(List.of(1, 1), WhitneyChecker.rankProfile(build("&{a: end}")));
        }

        @Test
        void chain3() {
            assertEquals(List.of(1, 1, 1), WhitneyChecker.rankProfile(build("&{a: &{b: end}}")));
        }

        @Test
        void parallel() {
            assertEquals(List.of(1, 2, 1), WhitneyChecker.rankProfile(build("(&{a: end} || &{b: end})")));
        }

        @ParameterizedTest
        @CsvSource({
                "&{a: end}",
                "&{a: &{b: end}}",
                "(&{a: end} || &{b: end})"
        })
        void sumEqualsTotal(String type) {
            var ss = build(type);
            var prof = WhitneyChecker.rankProfile(ss);
            var sccR = Zeta.computeSccs(ss);
            int nQuotient = new HashSet<>(sccR.sccMap().values()).size();
            assertEquals(nQuotient, prof.stream().mapToInt(Integer::intValue).sum());
        }
    }

    // -----------------------------------------------------------------------
    // Max rank level
    // -----------------------------------------------------------------------

    @Nested
    class MaxRankLevel {

        @Test
        void chain() {
            int[] mrl = WhitneyChecker.maxRankLevel(build("&{a: end}"));
            assertEquals(1, mrl[1]); // W = 1
        }

        @Test
        void parallel() {
            int[] mrl = WhitneyChecker.maxRankLevel(build("(&{a: end} || &{b: end})"));
            assertEquals(2, mrl[1]); // W = 2
            assertEquals(1, mrl[0]); // k = 1
        }
    }

    // -----------------------------------------------------------------------
    // Unimodality
    // -----------------------------------------------------------------------

    @Nested
    class Unimodality {

        @Test
        void empty() {
            assertTrue(WhitneyChecker.isUnimodal(List.of()));
        }

        @Test
        void single() {
            assertTrue(WhitneyChecker.isUnimodal(List.of(5)));
        }

        @Test
        void increasing() {
            assertTrue(WhitneyChecker.isUnimodal(List.of(1, 2, 3)));
        }

        @Test
        void decreasing() {
            assertTrue(WhitneyChecker.isUnimodal(List.of(3, 2, 1)));
        }

        @Test
        void peak() {
            assertTrue(WhitneyChecker.isUnimodal(List.of(1, 3, 2)));
        }

        @Test
        void valley() {
            assertFalse(WhitneyChecker.isUnimodal(List.of(3, 1, 2)));
        }

        @Test
        void constant() {
            assertTrue(WhitneyChecker.isUnimodal(List.of(2, 2, 2)));
        }

        @Test
        void chainUnimodal() {
            var prof = WhitneyChecker.rankProfile(build("&{a: &{b: end}}"));
            assertTrue(WhitneyChecker.isUnimodal(prof));
        }

        @Test
        void parallelUnimodal() {
            var prof = WhitneyChecker.rankProfile(build("(&{a: end} || &{b: end})"));
            assertTrue(WhitneyChecker.isUnimodal(prof));
        }
    }

    // -----------------------------------------------------------------------
    // Log-concavity
    // -----------------------------------------------------------------------

    @Nested
    class LogConcavity {

        @Test
        void chainLogConcave() {
            var prof = WhitneyChecker.rankProfile(build("&{a: &{b: end}}"));
            assertTrue(CharacteristicChecker.checkLogConcave(prof));
        }

        @Test
        void parallelLogConcave() {
            var prof = WhitneyChecker.rankProfile(build("(&{a: end} || &{b: end})"));
            assertTrue(CharacteristicChecker.checkLogConcave(prof));
        }
    }

    // -----------------------------------------------------------------------
    // Rank symmetry
    // -----------------------------------------------------------------------

    @Nested
    class RankSymmetry {

        @Test
        void chainSymmetric() {
            assertTrue(WhitneyChecker.isRankSymmetric(build("&{a: end}")));
            assertTrue(WhitneyChecker.isRankSymmetric(build("&{a: &{b: end}}")));
        }

        @Test
        void parallelSymmetric() {
            assertTrue(WhitneyChecker.isRankSymmetric(build("(&{a: end} || &{b: end})")));
        }
    }

    // -----------------------------------------------------------------------
    // Sperner property
    // -----------------------------------------------------------------------

    @Nested
    class Sperner {

        @Test
        void chainSperner() {
            assertTrue(WhitneyChecker.isSperner(build("&{a: end}")));
        }

        @Test
        void parallelSperner() {
            assertTrue(WhitneyChecker.isSperner(build("(&{a: end} || &{b: end})")));
        }

        @Test
        void spernerWidthChain() {
            assertEquals(1, WhitneyChecker.spernerWidth(build("&{a: end}")));
        }

        @Test
        void spernerWidthParallel() {
            assertEquals(2, WhitneyChecker.spernerWidth(build("(&{a: end} || &{b: end})")));
        }
    }

    // -----------------------------------------------------------------------
    // Convolution
    // -----------------------------------------------------------------------

    @Nested
    class Convolution {

        @Test
        void trivial() {
            assertEquals(List.of(1), WhitneyChecker.convolveProfiles(List.of(1), List.of(1)));
        }

        @Test
        void chainProduct() {
            assertEquals(List.of(1, 2, 1),
                    WhitneyChecker.convolveProfiles(List.of(1, 1), List.of(1, 1)));
        }

        @Test
        void parallelConvolution() {
            var ss1 = build("&{a: end}");
            var ss2 = build("&{b: end}");
            var ssProd = build("(&{a: end} || &{b: end})");
            assertTrue(WhitneyChecker.verifyProfileConvolution(ss1, ss2, ssProd));
        }
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    @Nested
    class Analyze {

        @Test
        void endAnalysis() {
            var r = WhitneyChecker.analyzeWhitney(build("end"));
            assertEquals(0, r.height());
            assertEquals(List.of(1), r.rankProfile());
            assertTrue(r.isUnimodal());
        }

        @Test
        void chainAnalysis() {
            var r = WhitneyChecker.analyzeWhitney(build("&{a: end}"));
            assertEquals(1, r.height());
            assertEquals(List.of(1, 1), r.rankProfile());
            assertTrue(r.isRankSymmetric());
        }

        @Test
        void parallelAnalysis() {
            var r = WhitneyChecker.analyzeWhitney(build("(&{a: end} || &{b: end})"));
            assertEquals(2, r.height());
            assertEquals(List.of(1, 2, 1), r.rankProfile());
            assertTrue(r.isUnimodal());
            assertTrue(r.isLogConcaveSecond());
            assertTrue(r.isRankSymmetric());
            assertTrue(r.isSperner());
        }
    }

    // -----------------------------------------------------------------------
    // Benchmark tests
    // -----------------------------------------------------------------------

    @Nested
    class Benchmarks {

        @ParameterizedTest
        @CsvSource({
                "'&{open: &{read: end, write: end}}'",
                "'&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}'",
                "'&{a: end, b: end}'",
                "'&{a: &{b: &{c: end}}}'",
                "'(&{a: end} || &{b: end})'"
        })
        void whitneyProperties(String type) {
            var ss = build(type);
            var r = WhitneyChecker.analyzeWhitney(ss);
            assertTrue(r.height() >= 0);
            assertEquals(r.height() + 1, r.rankProfile().size());
            assertEquals(r.totalElements(),
                    r.rankProfile().stream().mapToInt(Integer::intValue).sum());
            assertTrue(r.spernerWidth() >= 1);
        }

        @ParameterizedTest
        @CsvSource({
                "'&{open: &{read: end, write: end}}'",
                "'&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}'",
                "'&{a: end, b: end}'",
                "'&{a: &{b: &{c: end}}}'",
                "'(&{a: end} || &{b: end})'"
        })
        void unimodality(String type) {
            var ss = build(type);
            var prof = WhitneyChecker.rankProfile(ss);
            assertTrue(WhitneyChecker.isUnimodal(prof));
        }

        @ParameterizedTest
        @CsvSource({
                "'&{open: &{read: end, write: end}}'",
                "'&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}'",
                "'&{a: end, b: end}'",
                "'&{a: &{b: &{c: end}}}'",
                "'(&{a: end} || &{b: end})'"
        })
        void logConcavity(String type) {
            var ss = build(type);
            var prof = WhitneyChecker.rankProfile(ss);
            assertTrue(CharacteristicChecker.checkLogConcave(prof));
        }
    }
}
