package com.bica.reborn.enumerate;

import com.bica.reborn.ast.*;
import com.bica.reborn.parser.PrettyPrinter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for exhaustive session type enumeration and universality checking.
 */
class TypeEnumeratorTest {

    // -----------------------------------------------------------------------
    // Enumeration basics
    // -----------------------------------------------------------------------

    @Nested
    class EnumerationTests {

        @Test
        void depth0YieldsOnlyEnd() {
            var config = EnumerationConfig.defaults().withMaxDepth(0);
            List<SessionType> types = TypeEnumerator.enumerate(config);
            assertEquals(1, types.size());
            assertInstanceOf(End.class, types.getFirst());
        }

        @Test
        void depth1IncludesBranches() {
            var config = EnumerationConfig.defaults()
                    .withMaxDepth(1)
                    .withLabels(List.of("a"))
                    .withIncludeSelection(false)
                    .withIncludeParallel(false)
                    .withIncludeRecursion(false);
            List<SessionType> types = TypeEnumerator.enumerate(config);
            // end, &{a: end}
            assertTrue(types.size() >= 2);
            assertTrue(types.stream().anyMatch(t -> t instanceof Branch));
        }

        @Test
        void depth1IncludesSelections() {
            var config = EnumerationConfig.defaults()
                    .withMaxDepth(1)
                    .withLabels(List.of("a"))
                    .withIncludeParallel(false)
                    .withIncludeRecursion(false);
            List<SessionType> types = TypeEnumerator.enumerate(config);
            assertTrue(types.stream().anyMatch(t -> t instanceof Select));
        }

        @Test
        void depth1IncludesParallel() {
            var config = EnumerationConfig.defaults()
                    .withMaxDepth(1)
                    .withLabels(List.of("a"))
                    .withIncludeRecursion(false)
                    .withIncludeSelection(false);
            List<SessionType> types = TypeEnumerator.enumerate(config);
            assertTrue(types.stream().anyMatch(t -> t instanceof Parallel));
        }

        @Test
        void depth2IncludesRecursion() {
            // Recursion needs depth 2: rec X . &{a: X} requires depth 1 for the branch body
            var config = EnumerationConfig.defaults()
                    .withMaxDepth(2)
                    .withLabels(List.of("a"))
                    .withIncludeSelection(false)
                    .withIncludeParallel(false);
            List<SessionType> types = TypeEnumerator.enumerate(config);
            assertTrue(types.stream().anyMatch(t -> t instanceof Rec));
        }

        @Test
        void recursionRequiresVarUsage() {
            var config = EnumerationConfig.defaults()
                    .withMaxDepth(1)
                    .withLabels(List.of("a"))
                    .withIncludeSelection(false)
                    .withIncludeParallel(false);
            List<SessionType> types = TypeEnumerator.enumerate(config);
            for (SessionType t : types) {
                if (t instanceof Rec r) {
                    // Body should not be bare Var or End without var usage
                    assertNotEquals(new Var(r.var()), r.body());
                }
            }
        }

        @Test
        void multipleLabelsProduceMoreTypes() {
            var config1 = EnumerationConfig.defaults()
                    .withMaxDepth(1).withLabels(List.of("a"))
                    .withIncludeSelection(false).withIncludeParallel(false)
                    .withIncludeRecursion(false);
            var config2 = EnumerationConfig.defaults()
                    .withMaxDepth(1).withLabels(List.of("a", "b"))
                    .withIncludeSelection(false).withIncludeParallel(false)
                    .withIncludeRecursion(false);
            assertTrue(TypeEnumerator.enumerate(config2).size()
                    > TypeEnumerator.enumerate(config1).size());
        }

        @Test
        void depth2ProducesMoreThanDepth1() {
            var config1 = EnumerationConfig.defaults()
                    .withMaxDepth(1).withLabels(List.of("a"))
                    .withIncludeSelection(false).withIncludeParallel(false)
                    .withIncludeRecursion(false);
            var config2 = config1.withMaxDepth(2);
            assertTrue(TypeEnumerator.enumerate(config2).size()
                    > TypeEnumerator.enumerate(config1).size());
        }

        @Test
        void disablingAllFeaturesAtDepth1YieldsOnlyBranches() {
            var config = EnumerationConfig.defaults()
                    .withMaxDepth(1).withLabels(List.of("a"))
                    .withIncludeSelection(false).withIncludeParallel(false)
                    .withIncludeRecursion(false);
            List<SessionType> types = TypeEnumerator.enumerate(config);
            for (SessionType t : types) {
                assertTrue(t instanceof End || t instanceof Branch,
                        "Expected End or Branch, got: " + t.getClass().getSimpleName());
            }
        }

        @Test
        void enumerationIsFinite() {
            var config = EnumerationConfig.defaults()
                    .withMaxDepth(2).withLabels(List.of("a", "b"));
            List<SessionType> types = TypeEnumerator.enumerate(config);
            assertTrue(types.size() > 10);
            assertTrue(types.size() < 100_000);
        }
    }

    // -----------------------------------------------------------------------
    // Universality checking
    // -----------------------------------------------------------------------

    @Nested
    class UniversalityTests {

        @Test
        void depth0IsUniversal() {
            var config = EnumerationConfig.defaults().withMaxDepth(0);
            UniversalityResult result = TypeEnumerator.checkUniversality(config);
            assertTrue(result.isUniversal());
            assertEquals(1, result.totalTypes()); // just end
        }

        @Test
        void depth1BranchOnlyIsUniversal() {
            var config = EnumerationConfig.defaults()
                    .withMaxDepth(1).withLabels(List.of("a", "b"))
                    .withIncludeSelection(false).withIncludeParallel(false)
                    .withIncludeRecursion(false);
            UniversalityResult result = TypeEnumerator.checkUniversality(config);
            assertTrue(result.isUniversal());
            assertTrue(result.totalTypes() > 1);
        }

        @Test
        void depth1WithSelectionIsUniversal() {
            var config = EnumerationConfig.defaults()
                    .withMaxDepth(1).withLabels(List.of("a", "b"))
                    .withIncludeParallel(false).withIncludeRecursion(false);
            UniversalityResult result = TypeEnumerator.checkUniversality(config);
            assertTrue(result.isUniversal());
        }

        @Test
        void depth1WithAllFeaturesIsUniversal() {
            var config = EnumerationConfig.defaults()
                    .withMaxDepth(1).withLabels(List.of("a", "b"));
            UniversalityResult result = TypeEnumerator.checkUniversality(config);
            assertTrue(result.isUniversal());
        }

        @Test
        void depth2SmallIsUniversal() {
            var config = EnumerationConfig.defaults()
                    .withMaxDepth(2).withLabels(List.of("a"))
                    .withMaxBranchWidth(1);
            UniversalityResult result = TypeEnumerator.checkUniversality(config);
            assertTrue(result.isUniversal());
        }

        @Test
        void depth2TwoLabelsIsUniversal() {
            var config = EnumerationConfig.defaults()
                    .withMaxDepth(2).withLabels(List.of("a", "b"))
                    .withIncludeParallel(false);
            UniversalityResult result = TypeEnumerator.checkUniversality(config);
            assertTrue(result.isUniversal());
            assertTrue(result.totalTypes() > 10);
        }

        @Test
        void resultHasCorrectConfig() {
            var config = EnumerationConfig.defaults().withMaxDepth(0);
            UniversalityResult result = TypeEnumerator.checkUniversality(config);
            assertEquals(config, result.config());
        }

        @Test
        void totalEqualsLatticesPlusCounterexamples() {
            var config = EnumerationConfig.defaults()
                    .withMaxDepth(1).withLabels(List.of("a"));
            UniversalityResult result = TypeEnumerator.checkUniversality(config);
            assertEquals(result.totalTypes(),
                    result.totalLattices() + result.counterexamples().size());
        }
    }

    // -----------------------------------------------------------------------
    // Config
    // -----------------------------------------------------------------------

    @Nested
    class ConfigTests {

        @Test
        void defaultConfig() {
            var config = EnumerationConfig.defaults();
            assertEquals(2, config.maxDepth());
            assertEquals(List.of("a", "b"), config.labels());
            assertEquals(2, config.maxBranchWidth());
            assertTrue(config.includeParallel());
            assertTrue(config.includeRecursion());
            assertTrue(config.includeSelection());
            assertTrue(config.requireTerminating());
        }

        @Test
        void withMaxDepth() {
            var config = EnumerationConfig.defaults().withMaxDepth(5);
            assertEquals(5, config.maxDepth());
        }

        @Test
        void withLabels() {
            var config = EnumerationConfig.defaults()
                    .withLabels(List.of("x", "y", "z"));
            assertEquals(List.of("x", "y", "z"), config.labels());
        }

        @Test
        void labelsAreImmutable() {
            var config = EnumerationConfig.defaults();
            assertThrows(UnsupportedOperationException.class,
                    () -> config.labels().add("z"));
        }
    }
}
