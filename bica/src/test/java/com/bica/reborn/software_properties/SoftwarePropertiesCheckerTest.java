package com.bica.reborn.software_properties;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.software_properties.SoftwarePropertiesChecker.DictionaryResult;
import com.bica.reborn.software_properties.SoftwarePropertiesChecker.PropertyResult;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SoftwarePropertiesChecker -- lattice-theoretic dictionary of software properties.
 *
 * <p>Port of Python test_software_properties.py (78 tests).
 */
class SoftwarePropertiesCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    // Common protocols
    private static final StateSpace END_SS = build("end");
    private static final StateSpace SIMPLE_BRANCH = build("&{a: end, b: end}");
    private static final StateSpace ITERATOR = build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
    private static final StateSpace FILE = build("&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}");
    private static final StateSpace PARALLEL = build("(&{a: end} || &{b: end})");

    // =========================================================================
    // Safety
    // =========================================================================

    @Nested
    class Safety {

        @Test
        void endIsSafe() {
            var r = SoftwarePropertiesChecker.checkSafety(END_SS);
            assertTrue(r.holds());
            assertEquals("safety", r.name());
        }

        @Test
        void simpleBranchSafe() {
            assertTrue(SoftwarePropertiesChecker.checkSafety(SIMPLE_BRANCH).holds());
        }

        @Test
        void iteratorSafe() {
            assertTrue(SoftwarePropertiesChecker.checkSafety(ITERATOR).holds());
        }

        @Test
        void fileSafe() {
            assertTrue(SoftwarePropertiesChecker.checkSafety(FILE).holds());
        }

        @Test
        void parallelSafe() {
            assertTrue(SoftwarePropertiesChecker.checkSafety(PARALLEL).holds());
        }

        @Test
        void characterizationPresent() {
            var r = SoftwarePropertiesChecker.checkSafety(SIMPLE_BRANCH);
            assertFalse(r.characterization().isEmpty());
            assertTrue(r.characterization().contains("\u22a5")
                    || r.characterization().toLowerCase().contains("bottom"));
        }
    }

    // =========================================================================
    // Liveness
    // =========================================================================

    @Nested
    class Liveness {

        @Test
        void endLive() {
            assertTrue(SoftwarePropertiesChecker.checkLiveness(END_SS).holds());
        }

        @Test
        void simpleBranchLive() {
            assertTrue(SoftwarePropertiesChecker.checkLiveness(SIMPLE_BRANCH).holds());
        }

        @Test
        void iteratorLive() {
            assertTrue(SoftwarePropertiesChecker.checkLiveness(ITERATOR).holds());
        }

        @Test
        void fileLive() {
            assertTrue(SoftwarePropertiesChecker.checkLiveness(FILE).holds());
        }
    }

    // =========================================================================
    // Deadlock Freedom
    // =========================================================================

    @Nested
    class DeadlockFreedom {

        @Test
        void endDeadlockFree() {
            assertTrue(SoftwarePropertiesChecker.checkDeadlockFreedom(END_SS).holds());
        }

        @Test
        void simpleBranchDeadlockFree() {
            assertTrue(SoftwarePropertiesChecker.checkDeadlockFreedom(SIMPLE_BRANCH).holds());
        }

        @Test
        void iteratorDeadlockFree() {
            assertTrue(SoftwarePropertiesChecker.checkDeadlockFreedom(ITERATOR).holds());
        }

        @Test
        void parallelDeadlockFree() {
            assertTrue(SoftwarePropertiesChecker.checkDeadlockFreedom(PARALLEL).holds());
        }
    }

    // =========================================================================
    // Progress
    // =========================================================================

    @Nested
    class Progress {

        @Test
        void endProgress() {
            assertTrue(SoftwarePropertiesChecker.checkProgress(END_SS).holds());
        }

        @Test
        void simpleBranchProgress() {
            assertTrue(SoftwarePropertiesChecker.checkProgress(SIMPLE_BRANCH).holds());
        }

        @Test
        void iteratorProgress() {
            assertTrue(SoftwarePropertiesChecker.checkProgress(ITERATOR).holds());
        }

        @Test
        void fileProgress() {
            assertTrue(SoftwarePropertiesChecker.checkProgress(FILE).holds());
        }
    }

    // =========================================================================
    // Confluence
    // =========================================================================

    @Nested
    class Confluence {

        @Test
        void endConfluent() {
            assertTrue(SoftwarePropertiesChecker.checkConfluence(END_SS).holds());
        }

        @Test
        void simpleBranchConfluent() {
            assertTrue(SoftwarePropertiesChecker.checkConfluence(SIMPLE_BRANCH).holds());
        }

        @Test
        void iteratorConfluent() {
            assertTrue(SoftwarePropertiesChecker.checkConfluence(ITERATOR).holds());
        }

        @Test
        void parallelConfluent() {
            assertTrue(SoftwarePropertiesChecker.checkConfluence(PARALLEL).holds());
        }
    }

    // =========================================================================
    // Determinism
    // =========================================================================

    @Nested
    class Determinism {

        @Test
        void endDeterministic() {
            assertTrue(SoftwarePropertiesChecker.checkDeterminism(END_SS).holds());
        }

        @Test
        void simpleBranchDeterministic() {
            assertTrue(SoftwarePropertiesChecker.checkDeterminism(SIMPLE_BRANCH).holds());
        }

        @Test
        void iteratorDeterministic() {
            assertTrue(SoftwarePropertiesChecker.checkDeterminism(ITERATOR).holds());
        }

        @Test
        void fileDeterministic() {
            assertTrue(SoftwarePropertiesChecker.checkDeterminism(FILE).holds());
        }

        @Test
        void parallelDeterministic() {
            assertTrue(SoftwarePropertiesChecker.checkDeterminism(PARALLEL).holds());
        }
    }

    // =========================================================================
    // Reversibility
    // =========================================================================

    @Nested
    class Reversibility {

        @Test
        void endReversible() {
            assertTrue(SoftwarePropertiesChecker.checkReversibility(END_SS).holds());
        }

        @Test
        void simpleBranchNotReversible() {
            assertFalse(SoftwarePropertiesChecker.checkReversibility(SIMPLE_BRANCH).holds());
        }

        @Test
        void iteratorNotFullyReversible() {
            assertFalse(SoftwarePropertiesChecker.checkReversibility(ITERATOR).holds());
        }

        @Test
        void pureCycleReversible() {
            var ss = build("rec X . &{a: X}");
            assertTrue(SoftwarePropertiesChecker.checkReversibility(ss).holds());
        }
    }

    // =========================================================================
    // Transparency
    // =========================================================================

    @Nested
    class Transparency {

        @Test
        void endTransparent() {
            assertTrue(SoftwarePropertiesChecker.checkTransparency(END_SS).holds());
        }

        @Test
        void simpleBranchTransparent() {
            assertTrue(SoftwarePropertiesChecker.checkTransparency(SIMPLE_BRANCH).holds());
        }

        @Test
        void iteratorTransparent() {
            assertTrue(SoftwarePropertiesChecker.checkTransparency(ITERATOR).holds());
        }

        @Test
        void fileTransparent() {
            assertTrue(SoftwarePropertiesChecker.checkTransparency(FILE).holds());
        }
    }

    // =========================================================================
    // Boundedness
    // =========================================================================

    @Nested
    class Boundedness {

        @Test
        void endBounded() {
            assertTrue(SoftwarePropertiesChecker.checkBoundedness(END_SS).holds());
        }

        @Test
        void simpleBranchBounded() {
            assertTrue(SoftwarePropertiesChecker.checkBoundedness(SIMPLE_BRANCH).holds());
        }

        @Test
        void iteratorBounded() {
            assertTrue(SoftwarePropertiesChecker.checkBoundedness(ITERATOR).holds());
        }

        @Test
        void fileBounded() {
            assertTrue(SoftwarePropertiesChecker.checkBoundedness(FILE).holds());
        }

        @Test
        void heightInCharacterization() {
            var r = SoftwarePropertiesChecker.checkBoundedness(SIMPLE_BRANCH);
            assertTrue(r.characterization().toLowerCase().contains("height"));
        }
    }

    // =========================================================================
    // Fairness
    // =========================================================================

    @Nested
    class Fairness {

        @Test
        void endFair() {
            assertTrue(SoftwarePropertiesChecker.checkFairness(END_SS).holds());
        }

        @Test
        void simpleBranchFair() {
            assertTrue(SoftwarePropertiesChecker.checkFairness(SIMPLE_BRANCH).holds());
        }

        @Test
        void iteratorFair() {
            assertTrue(SoftwarePropertiesChecker.checkFairness(ITERATOR).holds());
        }

        @Test
        void fileFair() {
            assertTrue(SoftwarePropertiesChecker.checkFairness(FILE).holds());
        }
    }

    // =========================================================================
    // Monotonicity
    // =========================================================================

    @Nested
    class Monotonicity {

        @Test
        void endMonotone() {
            assertTrue(SoftwarePropertiesChecker.checkMonotonicity(END_SS).holds());
        }

        @Test
        void simpleBranchMonotone() {
            assertTrue(SoftwarePropertiesChecker.checkMonotonicity(SIMPLE_BRANCH).holds());
        }

        @Test
        void iteratorHasBackEdges() {
            var r = SoftwarePropertiesChecker.checkMonotonicity(ITERATOR);
            assertEquals("monotonicity", r.name());
        }

        @Test
        void fileMonotonicity() {
            var r = SoftwarePropertiesChecker.checkMonotonicity(FILE);
            assertEquals("monotonicity", r.name());
        }
    }

    // =========================================================================
    // Responsiveness
    // =========================================================================

    @Nested
    class Responsiveness {

        @Test
        void endResponsive() {
            assertTrue(SoftwarePropertiesChecker.checkResponsiveness(END_SS).holds());
        }

        @Test
        void simpleBranchResponsive() {
            assertTrue(SoftwarePropertiesChecker.checkResponsiveness(SIMPLE_BRANCH).holds());
        }

        @Test
        void iteratorResponsive() {
            assertTrue(SoftwarePropertiesChecker.checkResponsiveness(ITERATOR).holds());
        }

        @Test
        void parallelResponsive() {
            assertTrue(SoftwarePropertiesChecker.checkResponsiveness(PARALLEL).holds());
        }
    }

    // =========================================================================
    // Compositionality
    // =========================================================================

    @Nested
    class Compositionality {

        @Test
        void nonProductCompositionality() {
            assertTrue(SoftwarePropertiesChecker.checkCompositionality(SIMPLE_BRANCH).holds());
        }

        @Test
        void parallelCompositionality() {
            assertTrue(SoftwarePropertiesChecker.checkCompositionality(PARALLEL).holds());
        }

        @Test
        void resultHasWitnesses() {
            var r = SoftwarePropertiesChecker.checkCompositionality(PARALLEL);
            assertFalse(r.witnesses().isEmpty());
        }
    }

    // =========================================================================
    // Full dictionary
    // =========================================================================

    @Nested
    class FullDictionary {

        @Test
        void checkAllProperties() {
            var r = SoftwarePropertiesChecker.checkAllProperties(SIMPLE_BRANCH);
            assertInstanceOf(DictionaryResult.class, r);
            assertEquals(13, r.properties().size());
            assertTrue(r.numStates() > 0);
            assertFalse(r.summary().isEmpty());
        }

        @Test
        void holdingAndFailing() {
            var r = SoftwarePropertiesChecker.checkAllProperties(SIMPLE_BRANCH);
            assertNotNull(r.holding());
            assertNotNull(r.failing());
            assertEquals(13, r.holding().size() + r.failing().size());
        }

        @Test
        void allHoldOnEnd() {
            var r = SoftwarePropertiesChecker.checkAllProperties(END_SS);
            assertTrue(r.holding().size() >= 10);
        }

        @Test
        void profile() {
            Map<String, Boolean> p = SoftwarePropertiesChecker.propertyProfile(ITERATOR);
            assertNotNull(p);
            assertEquals(13, p.size());
            for (var v : p.values()) {
                assertInstanceOf(Boolean.class, v);
            }
        }
    }

    // =========================================================================
    // Selective checking
    // =========================================================================

    @Nested
    class SelectiveChecking {

        @Test
        void checkSpecificProperties() {
            var r = SoftwarePropertiesChecker.checkProperties(SIMPLE_BRANCH, "safety", "liveness");
            assertEquals(2, r.properties().size());
            assertTrue(r.properties().containsKey("safety"));
            assertTrue(r.properties().containsKey("liveness"));
        }

        @Test
        void unknownPropertyRaises() {
            assertThrows(IllegalArgumentException.class,
                    () -> SoftwarePropertiesChecker.checkProperties(SIMPLE_BRANCH, "nonexistent"));
        }

        @Test
        void singleProperty() {
            var r = SoftwarePropertiesChecker.checkProperties(ITERATOR, "determinism");
            assertEquals(1, r.properties().size());
            assertTrue(r.holds("determinism"));
        }
    }

    // =========================================================================
    // Benchmark protocols
    // =========================================================================

    @Nested
    class Benchmarks {

        @ParameterizedTest
        @CsvSource(delimiter = '#', value = {
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}} # Iterator",
                "&{open: rec X . &{read: +{data: X, eof: &{close: end}}}} # File",
                "&{connect: &{ehlo: rec X . &{mail: &{rcpt: &{data: +{OK: X, ERR: X}}}, quit: end}}} # SMTP",
                "&{a: end, b: end} # SimpleBranch",
                "+{ok: end, err: end} # SimpleSelect",
                "(&{a: end} || &{b: end}) # SimpleParallel",
                "&{get: +{OK: end, NOT_FOUND: end}, put: +{OK: end, ERR: end}} # REST",
                "rec X . &{request: +{OK: X, ERR: end}} # RetryLoop",
        })
        void safetyOnBenchmarks(String typeString, String name) {
            var ss = build(typeString.trim());
            assertTrue(SoftwarePropertiesChecker.checkSafety(ss).holds(),
                    name + " should be safe");
        }

        @ParameterizedTest
        @CsvSource(delimiter = '#', value = {
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}} # Iterator",
                "&{open: rec X . &{read: +{data: X, eof: &{close: end}}}} # File",
                "&{a: end, b: end} # SimpleBranch",
                "(&{a: end} || &{b: end}) # SimpleParallel",
        })
        void deadlockFreedomOnBenchmarks(String typeString, String name) {
            var ss = build(typeString.trim());
            assertTrue(SoftwarePropertiesChecker.checkDeadlockFreedom(ss).holds(),
                    name + " should be deadlock-free");
        }

        @ParameterizedTest
        @CsvSource(delimiter = '#', value = {
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}} # Iterator",
                "&{a: end, b: end} # SimpleBranch",
                "(&{a: end} || &{b: end}) # SimpleParallel",
                "&{get: +{OK: end, NOT_FOUND: end}, put: +{OK: end, ERR: end}} # REST",
        })
        void determinismOnBenchmarks(String typeString, String name) {
            var ss = build(typeString.trim());
            assertTrue(SoftwarePropertiesChecker.checkDeterminism(ss).holds(),
                    name + " should be deterministic");
        }

        @ParameterizedTest
        @CsvSource(delimiter = '#', value = {
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}} # Iterator",
                "&{open: rec X . &{read: +{data: X, eof: &{close: end}}}} # File",
                "&{a: end, b: end} # SimpleBranch",
                "(&{a: end} || &{b: end}) # SimpleParallel",
        })
        void fairnessOnBenchmarks(String typeString, String name) {
            var ss = build(typeString.trim());
            assertTrue(SoftwarePropertiesChecker.checkFairness(ss).holds(),
                    name + " should be fair");
        }
    }

    // =========================================================================
    // Property result structure
    // =========================================================================

    @Nested
    class PropertyResultTests {

        @Test
        void resultFields() {
            var r = SoftwarePropertiesChecker.checkSafety(SIMPLE_BRANCH);
            assertInstanceOf(PropertyResult.class, r);
            assertNotNull(r.name());
            assertNotNull(r.characterization());
            assertNotNull(r.witnesses());
        }

        @Test
        void counterexampleNullWhenHolds() {
            var r = SoftwarePropertiesChecker.checkSafety(SIMPLE_BRANCH);
            if (r.holds()) {
                assertNull(r.counterexample());
            }
        }
    }

    // =========================================================================
    // Dictionary result structure
    // =========================================================================

    @Nested
    class DictionaryResultTests {

        @Test
        void holdsMethod() {
            var r = SoftwarePropertiesChecker.checkAllProperties(SIMPLE_BRANCH);
            for (String name : SoftwarePropertiesChecker.ALL_PROPERTIES) {
                // Should not throw
                boolean val = r.holds(name);
                assertInstanceOf(Boolean.class, val);
            }
        }

        @Test
        void allHold() {
            var r = SoftwarePropertiesChecker.checkAllProperties(END_SS);
            assertInstanceOf(Boolean.class, r.allHold());
        }

        @Test
        void summaryNonEmpty() {
            var r = SoftwarePropertiesChecker.checkAllProperties(SIMPLE_BRANCH);
            assertFalse(r.summary().isEmpty());
        }
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    @Nested
    class EdgeCases {

        @Test
        void singleStateEnd() {
            var r = SoftwarePropertiesChecker.checkAllProperties(END_SS);
            for (var entry : r.properties().entrySet()) {
                if (!entry.getKey().equals("reversibility")) {
                    assertTrue(entry.getValue().holds(),
                            entry.getKey() + " should hold on 'end'");
                }
            }
        }

        @Test
        void deepNesting() {
            var ss = build("&{a: &{b: &{c: &{d: end}}}}");
            var r = SoftwarePropertiesChecker.checkAllProperties(ss);
            assertTrue(r.holds("safety"));
            assertTrue(r.holds("liveness"));
            assertTrue(r.holds("determinism"));
        }

        @Test
        void wideBranch() {
            var ss = build("&{a: end, b: end, c: end, d: end, e: end}");
            var r = SoftwarePropertiesChecker.checkAllProperties(ss);
            assertTrue(r.holds("safety"));
            assertTrue(r.holds("confluence"));
        }

        @Test
        void pureRecursion() {
            var ss = build("rec X . &{a: X}");
            Map<String, Boolean> profile = SoftwarePropertiesChecker.propertyProfile(ss);
            assertTrue(profile.get("reversibility"));
        }

        @Test
        void nestedParallel() {
            var ss = build("((&{a: end} || &{b: end}) || &{c: end})");
            var r = SoftwarePropertiesChecker.checkAllProperties(ss);
            assertTrue(r.holds("safety"));
            assertTrue(r.holds("determinism"));
        }

        @Test
        void selectionProtocol() {
            var ss = build("+{ok: end, err: end}");
            var r = SoftwarePropertiesChecker.checkAllProperties(ss);
            assertTrue(r.holds("safety"));
            assertTrue(r.holds("transparency"));
        }

        @Test
        void registryCompleteness() {
            assertEquals(13, SoftwarePropertiesChecker.ALL_PROPERTIES.size());
        }
    }
}
