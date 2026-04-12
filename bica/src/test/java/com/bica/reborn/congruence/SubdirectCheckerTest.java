package com.bica.reborn.congruence;

import com.bica.reborn.congruence.SubdirectChecker.SubdirectAnalysis;
import com.bica.reborn.congruence.SubdirectChecker.SubdirectFactor;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for subdirect decomposition of session type lattices (Step 363d).
 * Mirrors all 22 Python tests from test_subdirect.py.
 */
class SubdirectCheckerTest {

    private record Built(StateSpace ss, LatticeResult lr) {}

    private Built build(String type) {
        StateSpace ss = StateSpaceBuilder.build(Parser.parse(type));
        return new Built(ss, LatticeChecker.checkLattice(ss));
    }

    // -----------------------------------------------------------------------
    // is_subdirectly_irreducible
    // -----------------------------------------------------------------------

    @Nested
    class SubdirectlyIrreducible {

        @Test
        void trivialSi() {
            var b = build("end");
            assertTrue(b.lr().isLattice());
            assertTrue(SubdirectChecker.isSubdirectlyIrreducible(b.ss(), b.lr()));
        }

        @Test
        void twoChainSi() {
            var b = build("&{a: end}");
            assertTrue(b.lr().isLattice());
            assertTrue(SubdirectChecker.isSubdirectlyIrreducible(b.ss(), b.lr()));
        }

        @Test
        void threeChainNotSi() {
            var b = build("&{a: &{b: end}}");
            assertTrue(b.lr().isLattice());
            assertFalse(SubdirectChecker.isSubdirectlyIrreducible(b.ss(), b.lr()));
        }

        @Test
        void nonLatticeNotSi() {
            var b = build("&{a: end, b: rec X . &{a: X}}");
            if (!b.lr().isLattice()) {
                assertFalse(SubdirectChecker.isSubdirectlyIrreducible(b.ss(), b.lr()));
            }
        }

        @Test
        void simpleImpliesSi() {
            var b = build("&{a: end}");
            assertTrue(b.lr().isLattice());
            if (CongruenceChecker.isSimple(b.ss())) {
                assertTrue(SubdirectChecker.isSubdirectlyIrreducible(b.ss(), b.lr()));
            }
        }
    }

    // -----------------------------------------------------------------------
    // subdirect_factors
    // -----------------------------------------------------------------------

    @Nested
    class Factors {

        @Test
        void trivialOneFactor() {
            var b = build("end");
            var factors = SubdirectChecker.subdirectFactors(b.ss(), b.lr());
            assertTrue(factors.size() >= 1);
        }

        @Test
        void twoChainOneFactor() {
            var b = build("&{a: end}");
            var factors = SubdirectChecker.subdirectFactors(b.ss(), b.lr());
            assertTrue(factors.size() >= 1);
        }

        @Test
        void factorsAreSubdirectFactor() {
            var b = build("&{a: end, b: end}");
            if (b.lr().isLattice()) {
                var factors = SubdirectChecker.subdirectFactors(b.ss(), b.lr());
                for (var f : factors) {
                    assertNotNull(f);
                    assertInstanceOf(SubdirectFactor.class, f);
                }
            }
        }

        @Test
        void factorQuotientIsStateSpace() {
            var b = build("&{a: &{b: end}}");
            var factors = SubdirectChecker.subdirectFactors(b.ss(), b.lr());
            for (var f : factors) {
                assertNotNull(f.quotient().states());
                assertNotNull(f.quotient().transitions());
                assertTrue(f.quotientSize() > 0);
            }
        }

        @Test
        void siLatticeGivesItself() {
            var b = build("&{a: end}");
            if (SubdirectChecker.isSubdirectlyIrreducible(b.ss(), b.lr())) {
                var factors = SubdirectChecker.subdirectFactors(b.ss(), b.lr());
                assertEquals(1, factors.size());
            }
        }

        @Test
        void nonLatticeEmpty() {
            var b = build("&{a: end, b: rec X . &{a: X}}");
            if (!b.lr().isLattice()) {
                var factors = SubdirectChecker.subdirectFactors(b.ss(), b.lr());
                assertTrue(factors.isEmpty());
            }
        }
    }

    // -----------------------------------------------------------------------
    // analyze_subdirect
    // -----------------------------------------------------------------------

    @Nested
    class Analyze {

        @Test
        void returnsAnalysis() {
            var b = build("&{a: end}");
            var result = SubdirectChecker.analyzeSubdirect(b.ss(), b.lr());
            assertInstanceOf(SubdirectAnalysis.class, result);
        }

        @Test
        void trivialAnalysis() {
            var b = build("end");
            var result = SubdirectChecker.analyzeSubdirect(b.ss(), b.lr());
            assertTrue(result.isSimple());
            assertTrue(result.isSubdirectlyIrreducible());
            assertTrue(result.numFactors() >= 1);
        }

        @Test
        void twoChainAnalysis() {
            var b = build("&{a: end}");
            var result = SubdirectChecker.analyzeSubdirect(b.ss(), b.lr());
            assertTrue(result.isSimple());
            assertTrue(result.isSubdirectlyIrreducible());
            assertEquals(2, result.originalSize());
        }

        @Test
        void threeChainAnalysis() {
            var b = build("&{a: &{b: end}}");
            var result = SubdirectChecker.analyzeSubdirect(b.ss(), b.lr());
            assertFalse(result.isSubdirectlyIrreducible());
            assertEquals(2, result.numFactors());
            assertEquals(3, result.originalSize());
        }

        @Test
        void nonLatticeAnalysis() {
            var b = build("&{a: end, b: rec X . &{a: X}}");
            if (!b.lr().isLattice()) {
                var result = SubdirectChecker.analyzeSubdirect(b.ss(), b.lr());
                assertFalse(result.isSubdirectlyIrreducible());
                assertEquals(0, result.numFactors());
            }
        }

        @Test
        void parallelAnalysis() {
            var b = build("(&{a: end} || &{b: end})");
            if (b.lr().isLattice()) {
                var result = SubdirectChecker.analyzeSubdirect(b.ss(), b.lr());
                assertEquals(b.ss().states().size(), result.originalSize());
                assertTrue(result.conLatticeSize() > 0);
            }
        }

        @Test
        void branchAnalysis() {
            var b = build("&{a: end, b: end}");
            if (b.lr().isLattice()) {
                var result = SubdirectChecker.analyzeSubdirect(b.ss(), b.lr());
                assertTrue(result.numFactors() >= 1);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Properties
    // -----------------------------------------------------------------------

    @Nested
    class Properties {

        @ParameterizedTest
        @CsvSource({"end", "&{a: end}"})
        void simpleImpliesSi(String type) {
            var b = build(type);
            var result = SubdirectChecker.analyzeSubdirect(b.ss(), b.lr());
            if (result.isSimple()) {
                assertTrue(result.isSubdirectlyIrreducible());
            }
        }

        @Test
        void siImpliesAtLeastOneFactor() {
            var b = build("&{a: &{b: end}}");
            var result = SubdirectChecker.analyzeSubdirect(b.ss(), b.lr());
            if (result.isSubdirectlyIrreducible()) {
                assertTrue(result.numFactors() >= 1);
            }
        }

        @Test
        void factorsNonemptyForLattice() {
            String[] types = {
                    "end",
                    "&{a: end}",
                    "&{a: &{b: end}}",
                    "&{a: end, b: end}"
            };
            for (String type : types) {
                var b = build(type);
                if (b.lr().isLattice()) {
                    var factors = SubdirectChecker.subdirectFactors(b.ss(), b.lr());
                    assertFalse(factors.isEmpty(), "No factors for " + type);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Benchmarks
    // -----------------------------------------------------------------------

    @Nested
    class Benchmarks {

        @Test
        void benchmark() {
            String[] types = {
                    "end",
                    "&{a: end}",
                    "&{a: end, b: end}",
                    "&{a: &{b: end}}",
                    "&{a: &{b: &{c: end}}}",
                    "+{ok: end, err: end}",
                    "(&{a: end} || &{b: end})"
            };
            for (String type : types) {
                var b = build(type);
                if (b.lr().isLattice()) {
                    var result = SubdirectChecker.analyzeSubdirect(b.ss(), b.lr());
                    assertNotNull(result);
                    assertTrue(result.numFactors() >= 1, "no factors for " + type);
                    if (result.isSimple()) {
                        assertTrue(result.isSubdirectlyIrreducible());
                    }
                }
            }
        }
    }
}
