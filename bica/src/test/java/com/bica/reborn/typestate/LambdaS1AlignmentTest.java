package com.bica.reborn.typestate;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step 200d: Alignment tests verifying that the λ_S¹ extension
 * (method return values) is orthogonal to session-type checking
 * in {@link TypestateChecker}.
 *
 * <p>λ_S¹ extends λ_S⁰ with:
 * <ul>
 *   <li>Base types (Int, Double, String, Bool)</li>
 *   <li>Method-signature table msig(C, m) → τ</li>
 *   <li>T-Const rule (constants, environment unchanged)</li>
 *   <li>Modified T-Call (returns τ instead of Unit)</li>
 * </ul>
 *
 * <p>The Orthogonality Theorem states that return types never affect
 * the session-typed environment Γ — TypestateChecker correctly ignores
 * Java return types entirely.
 *
 * <p>Test classes:
 * <ul>
 *   <li>{@link TCallRet} — T-Call with return types: state transitions identical</li>
 *   <li>{@link ConservativeExtension} — all λ_S⁰ tests pass unchanged</li>
 *   <li>{@link OrthogonalityOfReturns} — return values don't affect session completion</li>
 *   <li>{@link ATMWorkedExample} — checkBalance returns double, protocol unaffected</li>
 * </ul>
 *
 * <p>Session types use Branch and End only (λ_S⁰/λ_S¹ restriction).
 */
@DisplayName("Step 200d: λ_S¹ Alignment Tests")
class LambdaS1AlignmentTest {

    // ── Helpers ──────────────────────────────────────────────────────

    /** Parse a session type string and build its state space. */
    private static StateSpace ss(String source) {
        return StateSpaceBuilder.build(Parser.parse(source));
    }

    /** Create a method call on variable v, method m. */
    private static MethodCall call(String v, String m) {
        return new MethodCall(v, m, 0);
    }

    // ── Protocols ────────────────────────────────────────────────────

    /**
     * S_FH = &{open: &{read: &{close: end}, write: &{close: end}}}
     *
     * In λ_S¹: msig(FH, read) = String, all others = Unit.
     * The state space is identical to λ_S⁰.
     */
    private static final String FILE_HANDLE_TYPE =
            "&{open: &{read: &{close: end}, write: &{close: end}}}";

    /**
     * Simplified ATM (Branch-only, no selections):
     * S_ATM = &{insertCard: &{enterPIN: &{checkBalance: &{ejectCard: end}}}}
     *
     * In λ_S¹: msig(ATM, checkBalance) = Double, all others = Unit.
     */
    private static final String ATM_TYPE =
            "&{insertCard: &{enterPIN: &{checkBalance: &{ejectCard: end}}}}";

    // ════════════════════════════════════════════════════════════════
    // §1  T-Call with Return Types
    //     T-Call: Γ(x)=C@s, δ(s,m)=s', msig(C,m)=τ → x.m() : τ
    //     Key: state update Γ[x↦C@s'] is identical regardless of τ
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T-Call with return types: state transitions identical")
    class TCallRet {

        @Test
        @DisplayName("read() returning String: state advances same as Unit (line 53)")
        void readReturningStringStateAdvances() {
            // In λ_S⁰, read returns Unit; in λ_S¹, read returns String.
            // The state transition is identical: δ(s1, read) = s2.
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            int s0 = fh.top();
            int s1 = fh.step(s0, "open").getAsInt();
            int s2 = fh.step(s1, "read").getAsInt();

            // TypestateChecker produces the same final state regardless of return type
            var result = TypestateChecker.check(fh,
                    List.of(call("f", "open"), call("f", "read")),
                    Map.of("f", fh.top()), false);
            assertTrue(result.isValid());
            assertEquals(s2, result.finalStates().get("f"),
                    "State after read() must be s2 regardless of return type");
        }

        @Test
        @DisplayName("checkBalance() returning Double: same state as Unit-returning method")
        void checkBalanceReturningDoubleStateAdvances() {
            StateSpace atm = ss(ATM_TYPE);
            int s0 = atm.top();
            int s1 = atm.step(s0, "insertCard").getAsInt();
            int s2 = atm.step(s1, "enterPIN").getAsInt();
            int s3 = atm.step(s2, "checkBalance").getAsInt();

            var result = TypestateChecker.check(atm,
                    List.of(call("a", "insertCard"),
                            call("a", "enterPIN"),
                            call("a", "checkBalance")),
                    Map.of("a", atm.top()), false);
            assertTrue(result.isValid());
            assertEquals(s3, result.finalStates().get("a"),
                    "State after checkBalance() must be s3 regardless of Double return");
        }

        @Test
        @DisplayName("return type does not affect which methods are enabled")
        void returnTypeDoesNotAffectEnabled() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            int s1 = fh.step(fh.top(), "open").getAsInt();
            int s2_read = fh.step(s1, "read").getAsInt();
            int s2_write = fh.step(s1, "write").getAsInt();

            // After read (returns String in λ_S¹): only close is enabled
            assertEquals(Set.of("close"), fh.enabledMethods(s2_read));
            // After write (returns Unit): only close is enabled
            assertEquals(Set.of("close"), fh.enabledMethods(s2_write));
            // Enabled methods depend on state, not on what the method returned
        }

        @Test
        @DisplayName("protocol violation independent of return type")
        void protocolViolationIndependentOfReturn() {
            // read() returns String in λ_S¹, but calling read at ⊤ is still a violation
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            var result = TypestateChecker.check(fh,
                    List.of(call("f", "read")),
                    Map.of("f", fh.top()), false);
            assertFalse(result.isValid(),
                    "read at ⊤ is still a protocol violation regardless of return type");
        }

        @Test
        @DisplayName("multiple return-type methods in sequence: states thread correctly")
        void multipleReturnTypeMethods() {
            // read returns String, close returns Unit, open returns Unit
            // State threading: s0→s1→s2→s4=⊥
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),    // Unit
                    call("f", "read"),    // String in λ_S¹
                    call("f", "close"))); // Unit
            assertTrue(result.isValid());
            assertEquals(fh.bottom(), result.finalStates().get("f"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §2  Conservative Extension
    //     Every λ_S⁰ derivation is valid in λ_S¹ (Theorem 5.1)
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Conservative Extension: all λ_S⁰ tests pass unchanged")
    class ConservativeExtension {

        @Test
        @DisplayName("λ_S⁰ e₁ (open→read→close) still valid in λ_S¹")
        void lambdaS0Example1StillValid() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "read"),
                    call("f", "close")));
            assertTrue(result.isValid(), "λ_S⁰ example e₁ must remain valid");
        }

        @Test
        @DisplayName("λ_S⁰ e₂ (read at ⊤) still invalid in λ_S¹")
        void lambdaS0Example2StillInvalid() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            var result = TypestateChecker.check(fh,
                    List.of(call("f", "read")),
                    Map.of("f", fh.top()), false);
            assertFalse(result.isValid(), "λ_S⁰ example e₂ must remain invalid");
        }

        @Test
        @DisplayName("λ_S⁰ e₃ (open only, incomplete) still fails completion")
        void lambdaS0Example3StillFailsCompletion() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open")));
            assertFalse(result.isValid(), "λ_S⁰ example e₃ must still fail completion");
        }

        @Test
        @DisplayName("session types unchanged: same state space, same transitions")
        void sessionTypesUnchanged() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            // State space structure is identical between λ_S⁰ and λ_S¹
            assertNotEquals(fh.top(), fh.bottom());
            assertEquals(Set.of("open"), fh.enabledMethods(fh.top()));
            assertTrue(fh.enabledMethods(fh.bottom()).isEmpty());
        }

        @Test
        @DisplayName("multiple variables: independent environments preserved")
        void multipleVariablesPreserved() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("g", "open"),
                    call("f", "read"),
                    call("g", "write"),
                    call("f", "close"),
                    call("g", "close")),
                    Map.of("f", fh.top(), "g", fh.top()), true);
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("end type: single state, ⊤ = ⊥ (unchanged)")
        void endTypeUnchanged() {
            StateSpace endSS = ss("end");
            assertEquals(endSS.top(), endSS.bottom());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §3  Orthogonality of Returns (Theorem 4.1)
    //     Return values don't affect session completion
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Orthogonality: return values don't affect session completion")
    class OrthogonalityOfReturns {

        @Test
        @DisplayName("binding return value doesn't change Γ (Theorem 4.1(i))")
        void bindingReturnDoesNotChangeGamma() {
            // let content = f.read() in ...
            // In λ_S¹, content : String, but content is NOT in Γ (session env)
            // So Γ is the same as if read returned Unit
            StateSpace fh = ss(FILE_HANDLE_TYPE);

            // With read in the middle (returns String in λ_S¹):
            var result1 = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "read"),
                    call("f", "close")));

            // Alternative path (write, returns Unit):
            var result2 = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "write"),
                    call("f", "close")));

            // Both reach ⊥ — session completion is the same
            assertTrue(result1.isValid());
            assertTrue(result2.isValid());
            assertEquals(fh.bottom(), result1.finalStates().get("f"));
            assertEquals(fh.bottom(), result2.finalStates().get("f"));
        }

        @Test
        @DisplayName("session-completion predicate independent of msig (Theorem 4.1(ii))")
        void completionIndependentOfMsig() {
            // Session completion checks: ∀(x:C@s) ∈ Γ'. s = ⊥
            // This predicate never mentions msig or return types
            StateSpace fh = ss(FILE_HANDLE_TYPE);

            // Incomplete path — fails regardless of return types
            var incomplete = TypestateChecker.check(fh,
                    List.of(call("f", "open"), call("f", "read")),
                    Map.of("f", fh.top()), true);
            assertFalse(incomplete.isValid(),
                    "Incomplete path fails completion regardless of read's return type");
        }

        @Test
        @DisplayName("TypestateChecker.check ignores Java return types (Corollary 4.2)")
        void checkerIgnoresReturnTypes() {
            // TypestateChecker tracks Map<String, Integer> (variable → state)
            // It never inspects Java return types
            // This is formalized by the Orthogonality Theorem
            StateSpace atm = ss(ATM_TYPE);
            var result = TypestateChecker.check(atm, List.of(
                    call("a", "insertCard"),
                    call("a", "enterPIN"),
                    call("a", "checkBalance"),  // Java: returns double
                    call("a", "ejectCard")));
            assertTrue(result.isValid());
            assertEquals(atm.bottom(), result.finalStates().get("a"),
                    "ATM reaches ⊥ regardless of checkBalance's double return");
        }

        @Test
        @DisplayName("base-type variable not in session environment")
        void baseTypeNotInSessionEnv() {
            // In λ_S¹: let content = f.read() in ...
            // Variable 'content' has type String (base type)
            // It is NOT tracked in the session-typed environment Γ
            // TypestateChecker only tracks variables that appear in method calls
            StateSpace fh = ss(FILE_HANDLE_TYPE);

            // Only 'f' is session-tracked, even though content is bound
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "read"),
                    call("f", "close")));
            assertTrue(result.isValid());
            // Only 'f' appears in final states
            assertTrue(result.finalStates().containsKey("f"));
            assertEquals(1, result.finalStates().size(),
                    "Only session-typed variable 'f' should be tracked");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §4  ATM Worked Example
    //     checkBalance returns double, protocol unaffected
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ATM Worked Example: checkBalance → Double")
    class ATMWorkedExample {

        @Test
        @DisplayName("ATM complete path: insertCard→enterPIN→checkBalance→ejectCard")
        void atmCompletePath() {
            StateSpace atm = ss(ATM_TYPE);
            var result = TypestateChecker.check(atm, List.of(
                    call("a", "insertCard"),
                    call("a", "enterPIN"),
                    call("a", "checkBalance"),
                    call("a", "ejectCard")));
            assertTrue(result.isValid());
            assertEquals(atm.bottom(), result.finalStates().get("a"));
        }

        @Test
        @DisplayName("ATM state space: linear chain s0→s1→s2→s3→s4=⊥")
        void atmStateSpaceLinearChain() {
            StateSpace atm = ss(ATM_TYPE);
            int s0 = atm.top();
            int s1 = atm.step(s0, "insertCard").getAsInt();
            int s2 = atm.step(s1, "enterPIN").getAsInt();
            int s3 = atm.step(s2, "checkBalance").getAsInt();
            int s4 = atm.step(s3, "ejectCard").getAsInt();

            assertEquals(atm.bottom(), s4, "Path ends at ⊥");
            // Each state has exactly one enabled method
            assertEquals(Set.of("insertCard"), atm.enabledMethods(s0));
            assertEquals(Set.of("enterPIN"), atm.enabledMethods(s1));
            assertEquals(Set.of("checkBalance"), atm.enabledMethods(s2));
            assertEquals(Set.of("ejectCard"), atm.enabledMethods(s3));
            assertTrue(atm.enabledMethods(s4).isEmpty());
        }

        @Test
        @DisplayName("ATM checkBalance at wrong state → stuck (return type irrelevant)")
        void atmCheckBalanceAtWrongState() {
            StateSpace atm = ss(ATM_TYPE);
            // checkBalance before enterPIN → protocol violation
            var result = TypestateChecker.check(atm,
                    List.of(call("a", "insertCard"),
                            call("a", "checkBalance")),
                    Map.of("a", atm.top()), false);
            assertFalse(result.isValid(),
                    "checkBalance at s1 is a violation regardless of its Double return");
        }

        @Test
        @DisplayName("ATM incomplete (no ejectCard) fails completion")
        void atmIncompleteFailsCompletion() {
            StateSpace atm = ss(ATM_TYPE);
            var result = TypestateChecker.check(atm, List.of(
                    call("a", "insertCard"),
                    call("a", "enterPIN"),
                    call("a", "checkBalance")));
            assertFalse(result.isValid(),
                    "Missing ejectCard → not at ⊥ → completion fails");
        }

        @Test
        @DisplayName("ATM: ejectCard directly after enterPIN → stuck")
        void atmEjectCardAfterEnterPin() {
            StateSpace atm = ss(ATM_TYPE);
            var result = TypestateChecker.check(atm,
                    List.of(call("a", "insertCard"),
                            call("a", "enterPIN"),
                            call("a", "ejectCard")),
                    Map.of("a", atm.top()), false);
            assertFalse(result.isValid(),
                    "ejectCard not enabled at s2 (only checkBalance is)");
        }

        @Test
        @DisplayName("FileHandle read → String vs ATM checkBalance → Double: same checker")
        void fileHandleAndAtmSameChecker() {
            // Both use the same TypestateChecker, which ignores return types
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            StateSpace atm = ss(ATM_TYPE);

            var fhResult = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "read"),
                    call("f", "close")));

            var atmResult = TypestateChecker.check(atm, List.of(
                    call("a", "insertCard"),
                    call("a", "enterPIN"),
                    call("a", "checkBalance"),
                    call("a", "ejectCard")));

            assertTrue(fhResult.isValid());
            assertTrue(atmResult.isValid());
            assertEquals(fh.bottom(), fhResult.finalStates().get("f"));
            assertEquals(atm.bottom(), atmResult.finalStates().get("a"));
        }
    }
}
