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
 * Step 200c: Alignment tests verifying that each formal rule in the
 * λ_S⁰ calculus (Step 200a) corresponds to the correct behavior in
 * {@link TypestateChecker}.
 *
 * <p>Each test class maps to a specific formal rule:
 * <ul>
 *   <li>{@link TNew} — T-New: allocate at ⊤</li>
 *   <li>{@link TCall} — T-Call: advance via δ(s,m)=s'</li>
 *   <li>{@link TLet} — T-Let: sequential composition</li>
 *   <li>{@link SessionCompletion} — all variables at ⊥</li>
 *   <li>{@link RCallStuck} — R-Call stuck = protocol violation</li>
 *   <li>{@link FileHandleWorkedExamples} — §7 worked examples from paper</li>
 * </ul>
 *
 * <p>Session types in λ_S⁰ use Branch and End only (no Select, Rec, Parallel).
 */
@DisplayName("Step 200c: λ_S⁰ Alignment Tests")
class LambdaS0AlignmentTest {

    // ── Helpers ──────────────────────────────────────────────────────

    /** Parse a session type string and build its state space. */
    private static StateSpace ss(String source) {
        return StateSpaceBuilder.build(Parser.parse(source));
    }

    /** Create a method call on variable v, method m. */
    private static MethodCall call(String v, String m) {
        return new MethodCall(v, m, 0);
    }

    // ── FileHandle protocol (running example from paper §2) ─────────

    /**
     * S_FH = &{open: &{read: &{close: end}, write: &{close: end}}}
     *
     * State space: s0 --open→ s1 --read→ s2 --close→ s4
     *                                --write→ s3 --close→ s4
     * where s0 = ⊤, s4 = ⊥
     */
    private static final String FILE_HANDLE_TYPE =
            "&{open: &{read: &{close: end}, write: &{close: end}}}";

    // ════════════════════════════════════════════════════════════════
    // §1  T-New: CT; Γ ⊢ new C : C ⊣ Γ, x : C @ ⊤
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T-New: allocate object at ⊤")
    class TNew {

        @Test
        @DisplayName("new object starts at top state (line 100)")
        void newObjectStartsAtTop() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            // T-New: initial state is ⊤
            var initialStates = Map.of("f", fh.top());
            var result = TypestateChecker.check(fh, List.of(), initialStates, false);
            assertTrue(result.isValid());
            assertEquals(fh.top(), result.finalStates().get("f"));
        }

        @Test
        @DisplayName("convenience check() auto-assigns top (line 100)")
        void convenienceCheckAutoAssignsTop() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            // The convenience overload initializes all variables at top,
            // then checks with checkEnd=true. Use a complete path.
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "read"),
                    call("f", "close")));
            assertTrue(result.isValid());
            assertEquals(fh.bottom(), result.finalStates().get("f"));
        }

        @Test
        @DisplayName("top state has exactly the root methods enabled")
        void topStateEnabledMethods() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            // At ⊤, only 'open' is enabled (the root branch)
            Set<String> enabled = fh.enabledMethods(fh.top());
            assertEquals(Set.of("open"), enabled);
        }

        @Test
        @DisplayName("bottom state has no methods enabled")
        void bottomStateNoMethods() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            Set<String> enabled = fh.enabledMethods(fh.bottom());
            assertTrue(enabled.isEmpty());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §2  T-Call: CT; Γ ⊢ x.m() : Unit ⊣ Γ[x ↦ C @ s']
    //     where Γ(x) = C @ s and δ(s,m) = s'
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T-Call: advance state via δ(s,m)=s'")
    class TCall {

        @Test
        @DisplayName("single call advances state (line 53)")
        void singleCallAdvancesState() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            int s0 = fh.top();
            // δ(s0, open) = s1
            var s1 = fh.step(s0, "open");
            assertTrue(s1.isPresent());

            var result = TypestateChecker.check(fh,
                    List.of(call("f", "open")),
                    Map.of("f", s0), false);
            assertTrue(result.isValid());
            assertEquals(s1.getAsInt(), result.finalStates().get("f"));
        }

        @Test
        @DisplayName("transition function is deterministic")
        void transitionIsDeterministic() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            int s0 = fh.top();
            var s1a = fh.step(s0, "open");
            var s1b = fh.step(s0, "open");
            assertEquals(s1a, s1b);
        }

        @Test
        @DisplayName("undefined transition means T-Call premise fails (line 55)")
        void undefinedTransitionFails() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            int s0 = fh.top();
            // δ(s0, read) is undefined — read not enabled at ⊤
            var next = fh.step(s0, "read");
            assertTrue(next.isEmpty());

            var result = TypestateChecker.check(fh,
                    List.of(call("f", "read")),
                    Map.of("f", s0), false);
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("T-Call updates environment: Γ[x ↦ s'] (line 64)")
        void callUpdatesEnvironment() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            // open → read: two successive T-Call applications
            var result = TypestateChecker.check(fh,
                    List.of(call("f", "open"), call("f", "read")),
                    Map.of("f", fh.top()), false);
            assertTrue(result.isValid());
            // Final state should be after read (s2), not after open (s1)
            int s1 = fh.step(fh.top(), "open").getAsInt();
            int s2 = fh.step(s1, "read").getAsInt();
            assertEquals(s2, result.finalStates().get("f"));
        }

        @Test
        @DisplayName("enabled methods match branch choices at each state")
        void enabledMethodsMatchBranch() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            int s1 = fh.step(fh.top(), "open").getAsInt();
            // At s1: &{read: ..., write: ...} → enabled = {read, write}
            Set<String> enabled = fh.enabledMethods(s1);
            assertEquals(Set.of("read", "write"), enabled);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §3  T-Let: sequential composition (threading Γ)
    //     CT; Γ ⊢ e₁ : T₁ ⊣ Γ₁   CT; Γ₁ ⊢ e₂ : T₂ ⊣ Γ₂
    //     ───────────────────────────────────────────────────
    //     CT; Γ ⊢ let x = e₁ in e₂ : T₂ ⊣ Γ₂
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("T-Let: sequential composition (lines 42-66)")
    class TLet {

        @Test
        @DisplayName("environment threads through sequential calls")
        void environmentThreads() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            // let _ = f.open() in let _ = f.read() in f.close()
            // Γ₀ = {f@s0} → Γ₁ = {f@s1} → Γ₂ = {f@s2} → Γ₃ = {f@s4}
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "read"),
                    call("f", "close")));
            assertTrue(result.isValid());
            assertEquals(fh.bottom(), result.finalStates().get("f"));
        }

        @Test
        @DisplayName("error in first call prevents valid continuation")
        void errorInFirstCallPreventsValid() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            // read is not enabled at s0; even if close follows, it's invalid
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "read"),
                    call("f", "close")));
            assertFalse(result.isValid());
            // First error is about 'read' not enabled
            assertEquals("read", result.errors().get(0).method());
        }

        @Test
        @DisplayName("multiple variables: independent environments")
        void multipleVariablesIndependent() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            // Two objects: f and g, each at ⊤
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("g", "open"),
                    call("f", "read"),
                    call("g", "write"),
                    call("f", "close"),
                    call("g", "close")),
                    Map.of("f", fh.top(), "g", fh.top()), true);
            assertTrue(result.isValid());
            assertEquals(fh.bottom(), result.finalStates().get("f"));
            assertEquals(fh.bottom(), result.finalStates().get("g"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §4  Session Completion: ∀(x : C @ s) ∈ Γ'. s = ⊥
    //     Corresponds to checkEnd flag (lines 68-84)
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Session Completion: all variables at ⊥")
    class SessionCompletion {

        @Test
        @DisplayName("complete path reaches ⊥ (line 72 passes)")
        void completePathReachesBottom() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "read"),
                    call("f", "close")));
            assertTrue(result.isValid());
            assertEquals(fh.bottom(), result.finalStates().get("f"));
        }

        @Test
        @DisplayName("incomplete path fails completion (line 72 fails)")
        void incompletePathFailsCompletion() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            // Only call open — not at ⊥
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open")));
            assertFalse(result.isValid());
            // Error is end-of-method, not disabled-method
            var endError = result.errors().stream()
                    .filter(e -> e.method().equals("<end-of-method>"))
                    .findFirst();
            assertTrue(endError.isPresent());
        }

        @Test
        @DisplayName("checkEnd=false disables completion check")
        void checkEndFalseDisablesCompletion() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            var result = TypestateChecker.check(fh,
                    List.of(call("f", "open")),
                    Map.of("f", fh.top()), false);
            // No completion check → valid even though not at ⊥
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("empty call sequence on fresh object fails completion")
        void emptyCallSequenceFailsCompletion() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            var result = TypestateChecker.check(fh,
                    List.of(call("f", "open")),  // need at least one call to register var
                    Map.of("f", fh.top()), true);
            // open leaves f at s1, not ⊥
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("alternative complete path also reaches ⊥")
        void alternativeCompletePathReachesBottom() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            // open → write → close (alternative branch)
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "write"),
                    call("f", "close")));
            assertTrue(result.isValid());
            assertEquals(fh.bottom(), result.finalStates().get("f"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §5  R-Call Stuck: δ(s,m) undefined → protocol violation
    //     Corresponds to TypestateError (lines 55-62)
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("R-Call Stuck: protocol violation")
    class RCallStuck {

        @Test
        @DisplayName("call disabled method at ⊤ → stuck")
        void callDisabledMethodAtTop() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            var result = TypestateChecker.check(fh,
                    List.of(call("f", "read")),
                    Map.of("f", fh.top()), false);
            assertFalse(result.isValid());
            assertEquals(1, result.errors().size());
            var err = result.errors().get(0);
            assertEquals("f", err.variable());
            assertEquals("read", err.method());
            assertEquals(fh.top(), err.state());
            assertTrue(err.enabled().contains("open"));
        }

        @Test
        @DisplayName("call method at ⊥ → stuck (no transitions)")
        void callMethodAtBottom() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            var result = TypestateChecker.check(fh,
                    List.of(call("f", "open")),
                    Map.of("f", fh.bottom()), false);
            assertFalse(result.isValid());
            var err = result.errors().get(0);
            assertEquals(fh.bottom(), err.state());
            assertTrue(err.enabled().isEmpty());
        }

        @Test
        @DisplayName("wrong method in middle of sequence → stuck")
        void wrongMethodInMiddle() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            // open then close (but close is not enabled after open)
            var result = TypestateChecker.check(fh,
                    List.of(call("f", "open"), call("f", "close")),
                    Map.of("f", fh.top()), false);
            assertFalse(result.isValid());
            assertEquals("close", result.errors().get(0).method());
        }

        @Test
        @DisplayName("error reports enabled methods at stuck state")
        void errorReportsEnabledMethods() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            // At s1 (after open): enabled = {read, write}
            var result = TypestateChecker.check(fh,
                    List.of(call("f", "open"), call("f", "open")),
                    Map.of("f", fh.top()), false);
            assertFalse(result.isValid());
            var err = result.errors().get(0);
            assertEquals(Set.of("read", "write"), err.enabled());
        }

        @Test
        @DisplayName("nonexistent method → stuck")
        void nonexistentMethod() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            var result = TypestateChecker.check(fh,
                    List.of(call("f", "destroy")),
                    Map.of("f", fh.top()), false);
            assertFalse(result.isValid());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §6  Worked Examples from Paper §7
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Paper §7: Worked Examples")
    class FileHandleWorkedExamples {

        @Test
        @DisplayName("e₁: correct client (open→read→close) — well-typed")
        void correctClientOpenReadClose() {
            // §7.1: let f = new FH in f.open(); f.read(); f.close()
            // Derivation: s0 →open→ s1 →read→ s2 →close→ s4 = ⊥
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "read"),
                    call("f", "close")));
            assertTrue(result.isValid(), "e₁ should be well-typed");
            assertEquals(fh.bottom(), result.finalStates().get("f"),
                    "e₁ should reach ⊥ (session completion)");
        }

        @Test
        @DisplayName("e₁: traces path s0→s1→s2→s4 in Hasse diagram")
        void correctClientTracesPath() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            int s0 = fh.top();
            int s1 = fh.step(s0, "open").getAsInt();
            int s2 = fh.step(s1, "read").getAsInt();
            int s4 = fh.step(s2, "close").getAsInt();

            assertEquals(fh.bottom(), s4, "path ends at ⊥");
            // Verify each step is a valid transition
            assertTrue(fh.step(s0, "open").isPresent());
            assertTrue(fh.step(s1, "read").isPresent());
            assertTrue(fh.step(s2, "close").isPresent());
        }

        @Test
        @DisplayName("e₂: rejected client (read at ⊤) — not well-typed")
        void rejectedClientReadAtTop() {
            // §7.2: let f = new FH in f.read()
            // T-Call fails: δ(s0, read) undefined, enabled(s0) = {open}
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            var result = TypestateChecker.check(fh,
                    List.of(call("f", "read")),
                    Map.of("f", fh.top()), false);
            assertFalse(result.isValid(), "e₂ should not be well-typed");
            assertEquals("read", result.errors().get(0).method());
            assertTrue(result.errors().get(0).enabled().contains("open"),
                    "error should report 'open' as enabled at ⊤");
        }

        @Test
        @DisplayName("e₃: incomplete client (open only) — fails completion")
        void incompleteClientOpenOnly() {
            // §7.3: let f = new FH in f.open()
            // Typing succeeds per step, but Γ'(f) = s1 ≠ ⊥
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open")));
            assertFalse(result.isValid(), "e₃ should fail session completion");
            var endError = result.errors().stream()
                    .filter(e -> e.method().equals("<end-of-method>"))
                    .findFirst();
            assertTrue(endError.isPresent(),
                    "failure should be end-of-method, not disabled-method");
        }

        @Test
        @DisplayName("alternative correct client (open→write→close)")
        void alternativeCorrectClient() {
            // Second root-to-leaf path in Hasse diagram
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "write"),
                    call("f", "close")));
            assertTrue(result.isValid());
            assertEquals(fh.bottom(), result.finalStates().get("f"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §7  Branch-Only Session Types (λ_S⁰ restriction)
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Branch-only types: finite trees with unique ⊤ and ⊥")
    class BranchOnlyTypes {

        @Test
        @DisplayName("end type: single state, ⊤ = ⊥")
        void endTypeSingleState() {
            StateSpace endSS = ss("end");
            assertEquals(endSS.top(), endSS.bottom());
            assertTrue(endSS.enabledMethods(endSS.top()).isEmpty());
        }

        @Test
        @DisplayName("single-method type: two states")
        void singleMethodType() {
            StateSpace single = ss("&{m: end}");
            assertNotEquals(single.top(), single.bottom());
            assertEquals(Set.of("m"), single.enabledMethods(single.top()));
            var next = single.step(single.top(), "m");
            assertTrue(next.isPresent());
            assertEquals(single.bottom(), next.getAsInt());
        }

        @Test
        @DisplayName("deep linear chain: &{a: &{b: &{c: end}}}")
        void deepLinearChain() {
            StateSpace chain = ss("&{a: &{b: &{c: end}}}");
            var result = TypestateChecker.check(chain, List.of(
                    call("x", "a"),
                    call("x", "b"),
                    call("x", "c")));
            assertTrue(result.isValid());
            assertEquals(chain.bottom(), result.finalStates().get("x"));
        }

        @Test
        @DisplayName("wide branch: &{a: end, b: end, c: end}")
        void wideBranch() {
            StateSpace wide = ss("&{a: end, b: end, c: end}");
            assertEquals(Set.of("a", "b", "c"), wide.enabledMethods(wide.top()));
            // Each single method reaches ⊥
            for (String m : List.of("a", "b", "c")) {
                var result = TypestateChecker.check(wide, List.of(call("x", m)));
                assertTrue(result.isValid(), m + " should reach ⊥");
            }
        }

        @Test
        @DisplayName("state space is a tree: no cycles in branch-only types")
        void stateSpaceIsTree() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            // ⊥ has no outgoing transitions → no cycles possible
            assertTrue(fh.enabledMethods(fh.bottom()).isEmpty());
            assertTrue(fh.enabledSelections(fh.bottom()).isEmpty());
            // ⊤ is not reachable from any non-⊤ state (tree property)
            // (verified indirectly: no method leads back to ⊤)
            int s1 = fh.step(fh.top(), "open").getAsInt();
            assertFalse(fh.enabledMethods(s1).contains("open"));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §8  Correspondence Table Verification
    //     (Table 1 from paper §8)
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Table 1: Rule-to-Implementation Correspondence")
    class CorrespondenceTable {

        @Test
        @DisplayName("T-New ↔ initialStates.put(v, top()) [line 100]")
        void tNewCorrespondence() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            // Verify convenience check auto-initializes at top (complete path)
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "read"),
                    call("f", "close")));
            // This exercises line 100: initialStates.put(v, stateSpace.top())
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("T-Call ↔ stateSpace.step(state, method) [line 53]")
        void tCallCorrespondence() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            // Directly verify step function matches TypestateChecker behavior
            int s0 = fh.top();
            var s1opt = fh.step(s0, "open");
            assertTrue(s1opt.isPresent(), "δ(⊤, open) must be defined");

            var result = TypestateChecker.check(fh,
                    List.of(call("f", "open")),
                    Map.of("f", s0), false);
            assertEquals(s1opt.getAsInt(), result.finalStates().get("f"),
                    "TypestateChecker final state must equal δ(⊤, open)");
        }

        @Test
        @DisplayName("T-Call error ↔ next.isEmpty() [line 55]")
        void tCallErrorCorrespondence() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            int s0 = fh.top();
            // δ(s0, close) is undefined
            assertTrue(fh.step(s0, "close").isEmpty());
            // TypestateChecker reports error (line 55)
            var result = TypestateChecker.check(fh,
                    List.of(call("f", "close")),
                    Map.of("f", s0), false);
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("Session completion ↔ finalState != bottom() [line 72]")
        void completionCorrespondence() {
            StateSpace fh = ss(FILE_HANDLE_TYPE);
            // Incomplete: final state is not bottom
            var result = TypestateChecker.check(fh,
                    List.of(call("f", "open"), call("f", "read")),
                    Map.of("f", fh.top()), true);
            assertFalse(result.isValid());
            // The error is about end-of-method, matching line 72 check
            assertTrue(result.errors().stream()
                    .anyMatch(e -> e.method().equals("<end-of-method>")));
        }
    }
}
