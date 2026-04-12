package com.bica.reborn.modularity;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.parser.Parser;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the T2b disciplined-fragment modularity checker.
 *
 * <p>Java mirror of
 * {@code reticulate/tests/test_modularity.py::TestIsDisciplined}
 * and {@code ::TestCheckModularityAst}. Per
 * {@code feedback_java_python_parity} these tests exist to guarantee
 * that the Java {@link ModularityChecker} returns the same verdict,
 * certificate, and classification as the Python
 * {@code check_modularity_ast} for identical inputs.
 */
class ModularityCheckerTest {

    private static SessionType parse(String s) {
        return Parser.parse(s);
    }

    // =========================================================================
    // isDisciplined — T2b fragment predicate
    // =========================================================================

    @Nested
    class IsDisciplined {

        @Test
        void endIsDisciplined() {
            assertTrue(ModularityChecker.isDisciplined(parse("end")));
        }

        @Test
        void singleChainIsDisciplined() {
            // m . end → Chain("m", End)
            assertTrue(ModularityChecker.isDisciplined(parse("a . end")));
        }

        @Test
        void nestedChainIsDisciplined() {
            assertTrue(ModularityChecker.isDisciplined(parse("a . b . c . end")));
        }

        @Test
        void parallelOfChainsIsDisciplined() {
            assertTrue(ModularityChecker.isDisciplined(
                    parse("(a . end || b . end)")));
        }

        @Test
        void parallelOfParallelsIsDisciplined() {
            assertTrue(ModularityChecker.isDisciplined(
                    parse("((a . end || b . end) || (c . end || d . end))")));
        }

        @Test
        void sequenceAfterParallelIsDisciplined() {
            // (a . end || b . end) . close . end
            // parses with Sequence on the left-Parallel shape.
            assertTrue(ModularityChecker.isDisciplined(
                    parse("(a . end || b . end) . close . end")));
        }

        @Test
        void branchIsNotDisciplined() {
            assertFalse(ModularityChecker.isDisciplined(
                    parse("&{a: end, b: end}")));
        }

        @Test
        void selectIsNotDisciplined() {
            assertFalse(ModularityChecker.isDisciplined(
                    parse("+{OK: end, ERR: end}")));
        }

        @Test
        void recIsNotDisciplined() {
            assertFalse(ModularityChecker.isDisciplined(
                    parse("rec X . (X || end)")));
        }

        @Test
        void chainOfBranchIsNotDisciplined() {
            // Chain embedding a Branch escapes the fragment at the branch.
            assertFalse(ModularityChecker.isDisciplined(
                    parse("a . &{b: end, c: end}")));
        }

        @Test
        void sequenceWithNonParallelLeftIsNotDisciplined() {
            // Disciplined Sequence requires the left arm to be Parallel.
            assertFalse(ModularityChecker.isDisciplined(
                    parse("&{a: end} . close . end")));
        }
    }

    // =========================================================================
    // check — full verdict with certificate
    // =========================================================================

    @Nested
    class CheckVerdict {

        @Test
        void endUsesSyntacticCertificate() {
            ModularityResult r = ModularityChecker.check(parse("end"));
            assertTrue(r.isModular());
            assertEquals(ModularityResult.CERT_SYNTACTIC, r.certificate());
        }

        @Test
        void chainUsesSyntacticCertificate() {
            ModularityResult r = ModularityChecker.check(parse("a . b . end"));
            assertTrue(r.isModular());
            assertEquals(ModularityResult.CERT_SYNTACTIC, r.certificate());
        }

        @Test
        void parallelUsesSyntacticCertificate() {
            ModularityResult r = ModularityChecker.check(
                    parse("(a . end || b . end)"));
            assertTrue(r.isModular());
            assertEquals(ModularityResult.CERT_SYNTACTIC, r.certificate());
        }

        @Test
        void sequenceAfterParallelUsesSyntacticCertificate() {
            ModularityResult r = ModularityChecker.check(
                    parse("(a . end || b . end) . close . end"));
            assertTrue(r.isModular());
            assertEquals(ModularityResult.CERT_SYNTACTIC, r.certificate());
        }

        @Test
        void branchFallsBackToSemanticCertificate() {
            // &{a: end, b: end} is outside the disciplined fragment, but
            // its state space IS a distributive lattice — the semantic
            // search still emits is_modular=true.
            ModularityResult r = ModularityChecker.check(
                    parse("&{a: end, b: end}"));
            assertEquals(ModularityResult.CERT_SEMANTIC, r.certificate());
        }

        @Test
        void n5WitnessIsNotModularSemantic() {
            // Canonical N5 counterexample from the modularity campaign
            // director session 2026-04-10.
            ModularityResult r = ModularityChecker.check(
                    parse("&{a: &{x: &{y: end}}, b: &{z: end}}"));
            assertEquals(ModularityResult.CERT_SEMANTIC, r.certificate());
            assertFalse(r.isModular());
        }

        @Test
        void resultCarriesClassification() {
            ModularityResult r = ModularityChecker.check(parse("end"));
            assertNotNull(r.classification());
        }

        @Test
        void resultCarriesNumStates() {
            ModularityResult r = ModularityChecker.check(parse("a . b . end"));
            assertTrue(r.numStates() >= 3);
        }
    }
}
