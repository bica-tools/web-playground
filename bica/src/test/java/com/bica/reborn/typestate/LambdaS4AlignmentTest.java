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
 * Step 200g: Alignment tests for λ_S⁴ (parallel composition).
 *
 * <p>λ_S⁴ extends λ_S³ with:
 * <ul>
 *   <li>Parallel constructor: (S₁ || S₂)</li>
 *   <li>Product state spaces: L(S₁ || S₂) = L(S₁) × L(S₂)</li>
 *   <li>Continuation after parallel: (S₁ || S₂) . S₃</li>
 *   <li>Concurrent access on disjoint protocol branches</li>
 * </ul>
 */
@DisplayName("Step 200g: λ_S⁴ Alignment Tests")
class LambdaS4AlignmentTest {

    // ── Helpers ──────────────────────────────────────────────────────

    /** Parse a session type string and build its state space. */
    private static StateSpace ss(String source) {
        return StateSpaceBuilder.build(Parser.parse(source));
    }

    /** Create a method call on variable v, method m. */
    private static MethodCall call(String v, String m) {
        return new MethodCall(v, m, 0);
    }

    // ── Protocol constants ───────────────────────────────────────────

    /** Simple 2×2 product: (&{a: end} || &{b: end}) */
    private static final String SIMPLE_PRODUCT = "(&{a: end} || &{b: end})";

    /** Concurrent FileHandle:
     *  &{open: (&{read: end} || &{write: end}) . &{close: end}} */
    private static final String CONCURRENT_FILE =
            "&{open: (&{read: end} || &{write: end}) . &{close: end}}";

    // ════════════════════════════════════════════════════════════════
    // §1  Product State Space
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Product state space: 2×2 diamond")
    class ProductStateSpace {

        @Test
        @DisplayName("simple product has exactly 4 states")
        void simpleProductHasFourStates() {
            StateSpace prod = ss(SIMPLE_PRODUCT);
            assertEquals(4, prod.states().size(),
                    "(&{a: end} || &{b: end}) should produce a 2×2 product with 4 states");
        }

        @Test
        @DisplayName("top has both a and b enabled")
        void topHasBothEnabled() {
            StateSpace prod = ss(SIMPLE_PRODUCT);
            Set<String> enabled = prod.enabledMethods(prod.top());
            assertEquals(Set.of("a", "b"), enabled,
                    "at ⊤=(⊤,⊤) both branches are available");
        }

        @Test
        @DisplayName("a-then-b reaches bottom")
        void aThenBReachesBottom() {
            StateSpace prod = ss(SIMPLE_PRODUCT);
            int s1 = prod.step(prod.top(), "a").getAsInt();
            int s2 = prod.step(s1, "b").getAsInt();
            assertEquals(prod.bottom(), s2,
                    "a→b must reach ⊥=(⊥,⊥)");
        }

        @Test
        @DisplayName("b-then-a reaches bottom (either order valid)")
        void bThenAReachesBottom() {
            StateSpace prod = ss(SIMPLE_PRODUCT);
            int s1 = prod.step(prod.top(), "b").getAsInt();
            int s2 = prod.step(s1, "a").getAsInt();
            assertEquals(prod.bottom(), s2,
                    "b→a must also reach ⊥=(⊥,⊥)");
        }

        @Test
        @DisplayName("intermediate states are distinct (diamond shape)")
        void intermediateStatesDistinct() {
            StateSpace prod = ss(SIMPLE_PRODUCT);
            int afterA = prod.step(prod.top(), "a").getAsInt();
            int afterB = prod.step(prod.top(), "b").getAsInt();
            assertNotEquals(afterA, afterB,
                    "after-a and after-b are distinct intermediate states");
            assertNotEquals(prod.top(), afterA);
            assertNotEquals(prod.bottom(), afterA);
        }

        @Test
        @DisplayName("bottom state has no enabled methods")
        void bottomHasNoMethods() {
            StateSpace prod = ss(SIMPLE_PRODUCT);
            assertTrue(prod.enabledMethods(prod.bottom()).isEmpty(),
                    "⊥=(⊥,⊥) has no transitions");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §2  Concurrent Access
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Concurrent access: read and write in either order")
    class ConcurrentAccess {

        @Test
        @DisplayName("open→read→write→close: valid path")
        void openReadWriteClose() {
            StateSpace fh = ss(CONCURRENT_FILE);
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "read"),
                    call("f", "write"),
                    call("f", "close")));
            assertTrue(result.isValid(),
                    "open→read→write→close must be a valid protocol trace");
        }

        @Test
        @DisplayName("open→write→read→close: valid path (reversed order)")
        void openWriteReadClose() {
            StateSpace fh = ss(CONCURRENT_FILE);
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "write"),
                    call("f", "read"),
                    call("f", "close")));
            assertTrue(result.isValid(),
                    "open→write→read→close must also be valid (parallel allows either order)");
        }

        @Test
        @DisplayName("both orderings reach the same close state")
        void bothOrderingsReachSameCloseState() {
            StateSpace fh = ss(CONCURRENT_FILE);
            int s0 = fh.top();
            int afterOpen = fh.step(s0, "open").getAsInt();

            // Path 1: read then write
            int rw1 = fh.step(afterOpen, "read").getAsInt();
            int rw2 = fh.step(rw1, "write").getAsInt();

            // Path 2: write then read
            int wr1 = fh.step(afterOpen, "write").getAsInt();
            int wr2 = fh.step(wr1, "read").getAsInt();

            assertEquals(rw2, wr2,
                    "both orderings must converge to the same product-bottom / close state");
        }

        @Test
        @DisplayName("after open, both read and write are enabled")
        void afterOpenBothEnabled() {
            StateSpace fh = ss(CONCURRENT_FILE);
            int afterOpen = fh.step(fh.top(), "open").getAsInt();
            Set<String> enabled = fh.enabledMethods(afterOpen);
            assertTrue(enabled.contains("read"), "read must be enabled after open");
            assertTrue(enabled.contains("write"), "write must be enabled after open");
        }

        @Test
        @DisplayName("close is not enabled in the parallel region")
        void closeNotEnabledInParallelRegion() {
            StateSpace fh = ss(CONCURRENT_FILE);
            int afterOpen = fh.step(fh.top(), "open").getAsInt();
            assertFalse(fh.enabledMethods(afterOpen).contains("close"),
                    "close must not be callable until both read and write are done");
        }

        @Test
        @DisplayName("close is enabled only after both read and write complete")
        void closeEnabledAfterBothComplete() {
            StateSpace fh = ss(CONCURRENT_FILE);
            int afterOpen = fh.step(fh.top(), "open").getAsInt();
            int afterRead = fh.step(afterOpen, "read").getAsInt();

            // After only read, close should not yet be available
            assertFalse(fh.enabledMethods(afterRead).contains("close"),
                    "close must not be enabled after only read (write still pending)");

            // After both read and write
            int afterBoth = fh.step(afterRead, "write").getAsInt();
            assertTrue(fh.enabledMethods(afterBoth).contains("close"),
                    "close must be enabled after both read and write complete");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §3  Conservative Extension
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Conservative extension: non-parallel types unchanged")
    class ConservativeExtension {

        @Test
        @DisplayName("branch-only FileHandle: sequential read/write, same as λ_S⁰")
        void branchOnlyUnchanged() {
            StateSpace fh = ss("&{open: &{read: &{close: end}, write: &{close: end}}}");
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"), call("f", "read"), call("f", "close")));
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("branch-only FileHandle: write path also valid")
        void branchOnlyWritePath() {
            StateSpace fh = ss("&{open: &{read: &{close: end}, write: &{close: end}}}");
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"), call("f", "write"), call("f", "close")));
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("selection type unchanged by parallel extension")
        void selectionUnchanged() {
            StateSpace sel = ss("&{connect: +{OK: &{use: end}, FAIL: end}}");
            var result = TypestateChecker.check(sel, List.of(
                    call("c", "connect"), call("c", "use")));
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("recursive type unchanged by parallel extension")
        void recursiveUnchanged() {
            StateSpace rec = ss("rec X . &{step: X, done: end}");
            var result = TypestateChecker.check(rec, List.of(
                    call("x", "step"), call("x", "step"), call("x", "done")));
            assertTrue(result.isValid());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // §4  Thread Safety
    // ════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Thread safety: disjoint states in the product")
    class ThreadSafety {

        @Test
        @DisplayName("read and write access disjoint intermediate states")
        void readWriteDisjointStates() {
            StateSpace prod = ss(SIMPLE_PRODUCT);
            int afterA = prod.step(prod.top(), "a").getAsInt();
            int afterB = prod.step(prod.top(), "b").getAsInt();

            // After a: only b is enabled (not a again)
            Set<String> enabledAfterA = prod.enabledMethods(afterA);
            assertTrue(enabledAfterA.contains("b"), "b must be enabled after a");
            assertFalse(enabledAfterA.contains("a"), "a must not be re-enabled after a");

            // After b: only a is enabled (not b again)
            Set<String> enabledAfterB = prod.enabledMethods(afterB);
            assertTrue(enabledAfterB.contains("a"), "a must be enabled after b");
            assertFalse(enabledAfterB.contains("b"), "b must not be re-enabled after b");
        }

        @Test
        @DisplayName("concurrent FileHandle: read-only path leaves write pending")
        void readOnlyPathLeavesWritePending() {
            StateSpace fh = ss(CONCURRENT_FILE);
            int afterOpen = fh.step(fh.top(), "open").getAsInt();
            int afterRead = fh.step(afterOpen, "read").getAsInt();

            // After read only, write is still available but read is not
            Set<String> enabled = fh.enabledMethods(afterRead);
            assertTrue(enabled.contains("write"),
                    "write must still be pending after only read");
            assertFalse(enabled.contains("read"),
                    "read must not be callable twice");
        }

        @Test
        @DisplayName("concurrent FileHandle: write-only path leaves read pending")
        void writeOnlyPathLeavesReadPending() {
            StateSpace fh = ss(CONCURRENT_FILE);
            int afterOpen = fh.step(fh.top(), "open").getAsInt();
            int afterWrite = fh.step(afterOpen, "write").getAsInt();

            Set<String> enabled = fh.enabledMethods(afterWrite);
            assertTrue(enabled.contains("read"),
                    "read must still be pending after only write");
            assertFalse(enabled.contains("write"),
                    "write must not be callable twice");
        }

        @Test
        @DisplayName("protocol violation: close before both branches complete")
        void closeBeforeBothComplete() {
            StateSpace fh = ss(CONCURRENT_FILE);
            // open→read→close (skip write) — should fail
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "read"),
                    call("f", "close")),
                    Map.of("f", fh.top()), false);
            assertFalse(result.isValid(),
                    "close before both branches complete must be a protocol violation");
        }

        @Test
        @DisplayName("protocol violation: read twice is not allowed")
        void readTwiceNotAllowed() {
            StateSpace fh = ss(CONCURRENT_FILE);
            var result = TypestateChecker.check(fh, List.of(
                    call("f", "open"),
                    call("f", "read"),
                    call("f", "read")),
                    Map.of("f", fh.top()), false);
            assertFalse(result.isValid(),
                    "calling read twice must be a protocol violation");
        }
    }
}
