package com.bica.reborn.modularity;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.parser.Parser;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java mirror of
 * {@code reticulate/tests/test_modularity_fixtures.py}.
 *
 * <p>Pins the three Birkhoff-hierarchy reference fixtures under
 * {@code bica/src/test/resources/modularity/} to their expected
 * verdicts from {@link ModularityChecker#check}. Landed per
 * {@code docs/observatory/campaigns/modularity/charter.md} §6
 * item (14).
 *
 * <p>The fixture payload strings in this directory stay
 * character-identical to the Python copies under
 * {@code reticulate/tests/benchmarks/modularity/}. Drift between
 * the two copies is caught by the Python
 * {@code TestJavaParity::test_python_and_java_fixtures_agree} test.
 */
class ModularityFixtureTest {

    private static SessionType parseFixture(String name) {
        return Parser.parse(ModularityFixtures.load(name));
    }

    // =========================================================================
    // Loader sanity
    // =========================================================================

    @Nested
    class Loader {

        @Test
        void loadsN5Canonical() {
            assertEquals(
                    "&{a: &{x: &{y: end}}, b: &{z: end}}",
                    ModularityFixtures.load("n5_canonical"));
        }

        @Test
        void loadsM3Canonical() {
            assertEquals(
                    "&{a: &{x: end}, b: &{y: end}, c: &{z: end}}",
                    ModularityFixtures.load("m3_canonical"));
        }

        @Test
        void loadsDistributiveReference() {
            assertEquals(
                    "(a . b . end || c . d . end)",
                    ModularityFixtures.load("distributive_reference"));
        }

        @Test
        void missingFixtureThrows() {
            assertThrows(
                    RuntimeException.class,
                    () -> ModularityFixtures.load("does_not_exist"));
        }
    }

    // =========================================================================
    // N5 canonical (pentagon) — NEVER modular
    // =========================================================================

    @Nested
    class N5Canonical {

        @Test
        void isParseable() {
            assertNotNull(parseFixture("n5_canonical"));
        }

        @Test
        void isNotDisciplined() {
            assertFalse(ModularityChecker.isDisciplined(
                    parseFixture("n5_canonical")));
        }

        @Test
        void isNotModular() {
            ModularityResult r = ModularityChecker.check(
                    parseFixture("n5_canonical"));
            assertFalse(r.isModular());
            assertFalse(r.isDistributive());
        }

        @Test
        void certificateIsSemantic() {
            ModularityResult r = ModularityChecker.check(
                    parseFixture("n5_canonical"));
            assertEquals(ModularityResult.CERT_SEMANTIC, r.certificate());
        }
    }

    // =========================================================================
    // M3 canonical (diamond) — modular but NOT distributive
    // =========================================================================

    @Nested
    class M3Canonical {

        @Test
        void isParseable() {
            assertNotNull(parseFixture("m3_canonical"));
        }

        @Test
        void isNotDisciplined() {
            assertFalse(ModularityChecker.isDisciplined(
                    parseFixture("m3_canonical")));
        }

        @Test
        void isLattice() {
            ModularityResult r = ModularityChecker.check(
                    parseFixture("m3_canonical"));
            assertTrue(r.isLattice());
        }

        @Test
        void isNotDistributive() {
            ModularityResult r = ModularityChecker.check(
                    parseFixture("m3_canonical"));
            assertFalse(r.isDistributive());
        }

        @Test
        void certificateIsSemantic() {
            ModularityResult r = ModularityChecker.check(
                    parseFixture("m3_canonical"));
            assertEquals(ModularityResult.CERT_SEMANTIC, r.certificate());
        }
    }

    // =========================================================================
    // Distributive reference — fast-path modular
    // =========================================================================

    @Nested
    class DistributiveReference {

        @Test
        void isParseable() {
            assertNotNull(parseFixture("distributive_reference"));
        }

        @Test
        void isDisciplined() {
            assertTrue(ModularityChecker.isDisciplined(
                    parseFixture("distributive_reference")));
        }

        @Test
        void isModular() {
            ModularityResult r = ModularityChecker.check(
                    parseFixture("distributive_reference"));
            assertTrue(r.isModular());
        }

        @Test
        void certificateIsSyntactic() {
            ModularityResult r = ModularityChecker.check(
                    parseFixture("distributive_reference"));
            assertEquals(ModularityResult.CERT_SYNTACTIC, r.certificate());
        }
    }
}
