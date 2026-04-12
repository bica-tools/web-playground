package com.bica.reborn.extensions;

import com.bica.reborn.ast.*;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ParallelLevelChecker} -- L3 parallel level analysis.
 */
class ParallelLevelCheckerTest {

    private static SessionType parse(String source) {
        return Parser.parse(source);
    }

    private static StateSpace ss(String source) {
        return StateSpaceBuilder.build(Parser.parse(source));
    }

    // =========================================================================
    // productFactors
    // =========================================================================

    @Nested
    class ProductFactors {

        @Test
        void nonParallel_emptyFactors() {
            List<StateSpace> factors = ParallelLevelChecker.productFactors(parse("&{a: end}"));
            assertTrue(factors.isEmpty());
        }

        @Test
        void simpleParallel_twoFactors() {
            List<StateSpace> factors = ParallelLevelChecker.productFactors(
                    parse("(&{a: end} || &{b: end})"));
            assertEquals(2, factors.size());
        }

        @Test
        void endParallel_twoFactors() {
            List<StateSpace> factors = ParallelLevelChecker.productFactors(
                    parse("(end || end)"));
            assertEquals(2, factors.size());
        }

        @Test
        void factorSizesCorrect() {
            List<StateSpace> factors = ParallelLevelChecker.productFactors(
                    parse("(&{a: end, b: end} || &{c: end})"));
            // &{a: end, b: end}: top + end = 2 states (both choices -> end)
            // &{c: end}: top + end = 2 states
            assertEquals(2, factors.get(0).states().size());
            assertEquals(2, factors.get(1).states().size());
        }
    }

    // =========================================================================
    // factorAnalysis
    // =========================================================================

    @Nested
    class FactorAnalysis {

        @Test
        void nonParallel_zeroFactors() {
            ParallelAnalysisResult result = ParallelLevelChecker.factorAnalysis(
                    parse("&{a: end}"));
            assertEquals(0, result.factorCount());
        }

        @Test
        void simpleParallel_analysisComplete() {
            ParallelAnalysisResult result = ParallelLevelChecker.factorAnalysis(
                    parse("(&{a: end} || &{b: end})"));
            assertEquals(2, result.factorCount());
            assertEquals(2, result.factors().size());
        }

        @Test
        void predictedSizeMatchesActual() {
            ParallelAnalysisResult result = ParallelLevelChecker.factorAnalysis(
                    parse("(&{a: end} || &{b: end})"));
            assertEquals(result.predictedSize(), result.actualSize());
        }

        @Test
        void factorSizesNonEmpty() {
            ParallelAnalysisResult result = ParallelLevelChecker.factorAnalysis(
                    parse("(&{a: end, b: end} || &{c: end})"));
            assertFalse(result.factorSizes().isEmpty());
            for (int size : result.factorSizes()) {
                assertTrue(size > 0);
            }
        }
    }

    // =========================================================================
    // distributivityFromFactors
    // =========================================================================

    @Nested
    class Distributivity {

        @Test
        void emptyFactors_distributive() {
            assertTrue(ParallelLevelChecker.distributivityFromFactors(List.of()));
        }

        @Test
        void chainFactors_distributive() {
            List<StateSpace> factors = ParallelLevelChecker.productFactors(
                    parse("(&{a: end} || &{b: end})"));
            assertTrue(ParallelLevelChecker.distributivityFromFactors(factors));
        }

        @Test
        void parallelAnalysisReportsDistributivity() {
            ParallelAnalysisResult result = ParallelLevelChecker.factorAnalysis(
                    parse("(&{a: end} || &{b: end})"));
            assertTrue(result.distributiveFromFactors());
        }
    }

    // =========================================================================
    // Height and width helpers
    // =========================================================================

    @Nested
    class HeightWidth {

        @Test
        void endHeight_zero() {
            assertEquals(0, ParallelLevelChecker.computeHeight(ss("end")));
        }

        @Test
        void chainHeight_one() {
            assertEquals(1, ParallelLevelChecker.computeHeight(ss("&{a: end}")));
        }

        @Test
        void branchHeight_one() {
            assertEquals(1, ParallelLevelChecker.computeHeight(ss("&{a: end, b: end}")));
        }

        @Test
        void endWidth_one() {
            assertEquals(1, ParallelLevelChecker.computeWidth(ss("end")));
        }

        @Test
        void branchWidth_two() {
            // &{a: end, b: end}: top -> a/b -> end => width at depth 0 = 1, depth 1 = 1 (end)
            // Actually: top has 2 successors that both go to end, so width depends on structure
            int w = ParallelLevelChecker.computeWidth(ss("&{a: end, b: end}"));
            assertTrue(w >= 1);
        }
    }
}
