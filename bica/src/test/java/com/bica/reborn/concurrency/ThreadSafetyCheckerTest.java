package com.bica.reborn.concurrency;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.parser.Parser;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.bica.reborn.concurrency.ConcurrencyLevel.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ThreadSafetyChecker}.
 */
class ThreadSafetyCheckerTest {

    // -- helpers ---------------------------------------------------------------

    private static ThreadSafetyResult check(String source, Map<String, ConcurrencyLevel> levels) {
        return ThreadSafetyChecker.check(Parser.parse(source), MethodClassifier.fromMap(levels));
    }

    // =========================================================================
    // extractMethods — method extraction from AST
    // =========================================================================

    @Nested
    class ExtractMethods {

        @Test
        void endHasNoMethods() {
            assertEquals(Set.of(), ThreadSafetyChecker.extractMethods(new End()));
        }

        @Test
        void varHasNoMethods() {
            assertEquals(Set.of(), ThreadSafetyChecker.extractMethods(new Var("X")));
        }

        @Test
        void singleBranch() {
            assertEquals(Set.of("read"),
                    ThreadSafetyChecker.extractMethods(Parser.parse("read . end")));
        }

        @Test
        void multipleBranches() {
            assertEquals(Set.of("a", "b", "c"),
                    ThreadSafetyChecker.extractMethods(Parser.parse("&{a: end, b: end, c: end}")));
        }

        @Test
        void select() {
            assertEquals(Set.of(),
                    ThreadSafetyChecker.extractMethods(Parser.parse("+{OK: end, ERR: end}")));
        }

        @Test
        void parallel() {
            assertEquals(Set.of("read", "write"),
                    ThreadSafetyChecker.extractMethods(
                            Parser.parse("(read . end || write . end)")));
        }

        @Test
        void recursion() {
            assertEquals(Set.of("hasNext", "next"),
                    ThreadSafetyChecker.extractMethods(
                            Parser.parse("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}")));
        }

        @Test
        void sequence() {
            assertEquals(Set.of("a", "b"),
                    ThreadSafetyChecker.extractMethods(Parser.parse("a . b . end")));
        }

        @Test
        void nestedAll() {
            var methods = ThreadSafetyChecker.extractMethods(
                    Parser.parse("&{open: +{OK: (&{read: end} || &{write: end}) . &{close: end}, ERR: end}}"));
            assertEquals(Set.of("open", "read", "write", "close"), methods);
        }

        @Test
        void selectionLabelsExcludedFromParallelMethods() {
            // Selection labels (ACCEPT, REJECT) are enum return values, not methods
            // Only actual callable methods should be extracted
            var type = Parser.parse(
                    "(send . +{ACCEPT: end, REJECT: end} || recv . end)");
            var methods = ThreadSafetyChecker.extractMethods(type);
            assertEquals(Set.of("send", "recv"), methods);
            assertFalse(methods.contains("ACCEPT"));
            assertFalse(methods.contains("REJECT"));
        }

        @Test
        void nullThrows() {
            assertThrows(NullPointerException.class,
                    () -> ThreadSafetyChecker.extractMethods(null));
        }
    }

    // =========================================================================
    // Safe parallel — all pairs compatible
    // =========================================================================

    @Nested
    class SafeParallel {

        @Test
        void bothReadOnly() {
            var r = check("(read . end || peek . end)",
                    Map.of("read", READ_ONLY, "peek", READ_ONLY));
            assertTrue(r.isSafe());
            assertTrue(r.violations().isEmpty());
        }

        @Test
        void readOnlyAndSync() {
            var r = check("(read . end || sync . end)",
                    Map.of("read", READ_ONLY, "sync", SYNC));
            assertTrue(r.isSafe());
        }

        @Test
        void bothSync() {
            var r = check("(lock . end || lock2 . end)",
                    Map.of("lock", SYNC, "lock2", SYNC));
            assertTrue(r.isSafe());
        }

        @Test
        void bothShared() {
            var r = check("(a . end || b . end)",
                    Map.of("a", SHARED, "b", SHARED));
            assertTrue(r.isSafe());
        }

        @Test
        void sharedAndReadOnly() {
            var r = check("(a . end || b . end)",
                    Map.of("a", SHARED, "b", READ_ONLY));
            assertTrue(r.isSafe());
        }

        @Test
        void sharedAndSync() {
            var r = check("(a . end || b . end)",
                    Map.of("a", SHARED, "b", SYNC));
            assertTrue(r.isSafe());
        }

        @Test
        void noParallel() {
            var r = check("a . b . end", Map.of("a", EXCLUSIVE, "b", EXCLUSIVE));
            assertTrue(r.isSafe());
            assertTrue(r.violations().isEmpty());
        }

        @Test
        void trustBoundariesTracked() {
            var r = check("(read . end || sync . end)",
                    Map.of("read", READ_ONLY, "sync", SYNC));
            // READ_ONLY is not in trust boundaries (not SHARED/SYNC in the plan's sense)
            // Actually, per the implementation, SHARED and SYNC are trust boundaries
            assertTrue(r.trustBoundaries().contains("sync"));
        }
    }

    // =========================================================================
    // Unsafe parallel — incompatible pairs
    // =========================================================================

    @Nested
    class UnsafeParallel {

        @Test
        void exclusiveAndReadOnly() {
            var r = check("(write . end || read . end)",
                    Map.of("write", EXCLUSIVE, "read", READ_ONLY));
            assertFalse(r.isSafe());
            assertEquals(1, r.violations().size());
            var v = r.violations().get(0);
            assertEquals(EXCLUSIVE, v.level1());
            assertEquals(READ_ONLY, v.level2());
        }

        @Test
        void exclusiveAndSync() {
            var r = check("(write . end || lock . end)",
                    Map.of("write", EXCLUSIVE, "lock", SYNC));
            assertFalse(r.isSafe());
        }

        @Test
        void exclusiveAndShared() {
            var r = check("(write . end || shared . end)",
                    Map.of("write", EXCLUSIVE, "shared", SHARED));
            assertFalse(r.isSafe());
        }

        @Test
        void bothExclusive() {
            var r = check("(a . end || b . end)",
                    Map.of("a", EXCLUSIVE, "b", EXCLUSIVE));
            assertFalse(r.isSafe());
        }

        @Test
        void multipleBranchMethods() {
            // Left has read+write, right has peek — write×peek is unsafe
            var r = check("(&{read: end, write: end} || peek . end)",
                    Map.of("read", READ_ONLY, "write", EXCLUSIVE, "peek", READ_ONLY));
            assertFalse(r.isSafe());
            // At least one violation involving write
            assertTrue(r.violations().stream().anyMatch(
                    v -> v.method1().equals("write") || v.method2().equals("write")));
        }

        @Test
        void multipleViolations() {
            // Both exclusive methods on both sides
            var r = check("(&{a: end, b: end} || &{c: end, d: end})",
                    Map.of("a", EXCLUSIVE, "b", EXCLUSIVE, "c", EXCLUSIVE, "d", EXCLUSIVE));
            assertFalse(r.isSafe());
            assertEquals(4, r.violations().size()); // a×c, a×d, b×c, b×d
        }
    }

    // =========================================================================
    // Unknown methods — default to EXCLUSIVE
    // =========================================================================

    @Nested
    class UnknownMethods {

        @Test
        void unknownDefaultsToExclusive() {
            // "write" is not in the map → defaults to EXCLUSIVE
            var r = check("(write . end || read . end)",
                    Map.of("read", READ_ONLY));
            assertFalse(r.isSafe());
        }

        @Test
        void unknownClassificationRecorded() {
            var r = check("(write . end || read . end)",
                    Map.of("read", READ_ONLY));
            assertTrue(r.classifications().stream().anyMatch(
                    mc -> mc.methodName().equals("write")
                            && mc.level() == EXCLUSIVE
                            && mc.source().equals("default")));
        }

        @Test
        void allUnknownInParallel() {
            var r = check("(a . end || b . end)", Map.of());
            assertFalse(r.isSafe());
        }

        @Test
        void unknownWithoutParallelIsSafe() {
            var r = check("a . b . end", Map.of());
            assertTrue(r.isSafe());
        }
    }

    // =========================================================================
    // Classifications tracking
    // =========================================================================

    @Nested
    class Classifications {

        @Test
        void allMethodsClassified() {
            var r = check("(read . end || write . end)",
                    Map.of("read", READ_ONLY, "write", EXCLUSIVE));
            assertEquals(2, r.classifications().size());
        }

        @Test
        void classificationSourceFromMap() {
            var r = check("read . end", Map.of("read", READ_ONLY));
            var mc = r.classifications().stream()
                    .filter(c -> c.methodName().equals("read"))
                    .findFirst()
                    .orElseThrow();
            assertEquals("map", mc.source());
        }

        @Test
        void classificationSourceDefault() {
            var r = check("read . end", Map.of());
            var mc = r.classifications().stream()
                    .filter(c -> c.methodName().equals("read"))
                    .findFirst()
                    .orElseThrow();
            assertEquals("default", mc.source());
            assertEquals(EXCLUSIVE, mc.level());
        }
    }

    // =========================================================================
    // Recursive types with parallel
    // =========================================================================

    @Nested
    class RecursiveParallel {

        @Test
        void recursiveWithSafeParallel() {
            // rec X . &{go: (read . end || peek . end), loop: X}
            var r = check("rec X . &{go: (read . end || peek . end), loop: X}",
                    Map.of("read", READ_ONLY, "peek", READ_ONLY, "go", SHARED, "loop", SHARED));
            assertTrue(r.isSafe());
        }

        @Test
        void recursiveWithUnsafeParallel() {
            var r = check("rec X . &{go: (read . end || write . end), loop: X}",
                    Map.of("read", READ_ONLY, "write", EXCLUSIVE, "go", SHARED, "loop", SHARED));
            assertFalse(r.isSafe());
        }
    }

    // =========================================================================
    // Spec examples and benchmark protocols
    // =========================================================================

    @Nested
    class SpecExamples {

        @Test
        void fileObjectReadWrite() {
            // open . +{OK: (read . end || write . end) . close . end, ERR: end}
            // read and write execute concurrently — safe only if both non-exclusive
            var r = check(
                    "&{open: +{OK: (&{read: end} || &{write: end}) . &{close: end}, ERR: end}}",
                    Map.of("open", EXCLUSIVE,
                            "read", SYNC, "write", SYNC, "close", EXCLUSIVE));
            assertTrue(r.isSafe(), "SYNC read + SYNC write should be safe");
        }

        @Test
        void fileObjectReadWriteUnsafe() {
            var r = check(
                    "&{open: +{OK: (&{read: end} || &{write: end}) . &{close: end}, ERR: end}}",
                    Map.of("open", EXCLUSIVE,
                            "read", READ_ONLY, "write", EXCLUSIVE, "close", EXCLUSIVE));
            assertFalse(r.isSafe(), "READ_ONLY read + EXCLUSIVE write should be unsafe");
        }

        @Test
        void twoBuyer() {
            // lookup . getPrice . (proposeA . end || proposeB . +{ACCEPT: pay . end, REJECT: end})
            var r = check(
                    "&{lookup: &{getPrice: (&{proposeA: end} || &{proposeB: +{ACCEPT: &{pay: end}, REJECT: end}})}}",
                    Map.of("lookup", EXCLUSIVE, "getPrice", READ_ONLY,
                            "proposeA", SYNC, "proposeB", SYNC, "pay", SYNC));
            assertTrue(r.isSafe());
        }

        @Test
        void mcpProtocol() {
            // initialize . (rec X . &{callTool: +{RESULT: X, ERROR: X}, listTools: X, shutdown: end}
            //            || rec Y . +{NOTIFICATION: Y, DONE: end})
            var allSync = Map.of(
                    "initialize", EXCLUSIVE, "callTool", SYNC,
                    "listTools", SYNC, "shutdown", SYNC);
            var r = check(
                    "&{initialize: (rec X . &{callTool: +{RESULT: X, ERROR: X}, listTools: X, shutdown: end} || rec Y . +{NOTIFICATION: Y, DONE: end})}",
                    allSync);
            assertTrue(r.isSafe());
        }

        @Test
        void websocket() {
            // connect . +{OPEN: (rec X . &{send: X, ping: X, closeSend: end}
            //                  || rec Y . +{MESSAGE: Y, PONG: Y, CLOSE: end}), REFUSED: end}
            var levels = Map.of(
                    "connect", EXCLUSIVE,
                    "send", SYNC,
                    "ping", SYNC,
                    "closeSend", SYNC);
            var r = check(
                    "&{connect: +{OPEN: (rec X . &{send: X, ping: X, closeSend: end} || rec Y . +{MESSAGE: Y, PONG: Y, CLOSE: end}), REFUSED: end}}",
                    levels);
            assertTrue(r.isSafe());
        }
    }

    // =========================================================================
    // Benchmark protocols with parallel
    // =========================================================================

    @Nested
    class Benchmarks {

        private static final String[] PARALLEL_BENCHMARKS = {
            // #5 Two-buyer
            "&{lookup: &{getPrice: (&{proposeA: end} || &{proposeB: +{ACCEPT: &{pay: end}, REJECT: end}})}}",
            // #6 MCP
            "&{initialize: (rec X . &{callTool: +{RESULT: X, ERROR: X}, listTools: X, shutdown: end} || rec Y . +{NOTIFICATION: Y, DONE: end})}",
            // #8 File object
            "&{open: +{OK: (rec X . &{read: X, doneRead: end} || rec Y . &{write: Y, doneWrite: end}) . &{close: end}, ERR: end}}",
            // #11 WebSocket
            "&{connect: +{OPEN: (rec X . &{send: X, ping: X, closeSend: end} || rec Y . +{MESSAGE: Y, PONG: Y, CLOSE: end}), REFUSED: end}}",
            // #15 Reticulate pipeline
            "&{input: &{parse: +{OK: &{buildStateSpace: +{OK: (&{checkLattice: &{checkTermination: &{checkWFPar: end}}} || &{renderDiagram: end}) . &{returnResult: end}, ERROR: end}}, ERROR: end}}}",
            // #16 CI/CD pipeline
            "&{trigger: &{checkout: &{setup: (&{lint: end} || &{test: end}) . +{PASS: &{deploy: +{OK: end, FAIL: &{rollback: end}}}, FAIL: end}}}}",
            // #19 MQTT Client
            "&{connect: +{CONNACK: (rec X . &{publish: +{PUBACK: X, TIMEOUT: X}, disconnect: end} || rec Y . +{MESSAGE: Y, SUBACK: Y, DONE: end}) . end, REFUSED: end}}",
            // #21 Connection Pool
            "&{init: (rec X . &{acquire: &{use: &{release: X}}, drain: end} || rec Y . &{healthCheck: +{HEALTHY: Y, UNHEALTHY: Y}, shutdown: end})}",
            // #22 gRPC BiDi Stream
            "&{open: (rec X . &{send: X, halfClose: end} || rec Y . +{RESPONSE: Y, TRAILER: end})}",
            // #24 Kafka Consumer
            "&{subscribe: (rec X . &{poll: +{RECORDS: &{process: &{commit: X}}, EMPTY: X}, pause: end} || rec Y . +{REBALANCE: Y, REVOKED: end})}",
            // #26 Saga Orchestrator
            "&{begin: (&{step1: +{OK: end, FAIL: &{compensate1: end}}} || &{step2: +{OK: end, FAIL: &{compensate2: end}}}) . +{ALL_OK: &{commit: end}, PARTIAL: &{rollback: end}}}",
            // #28 Leader Replication
            "&{electLeader: rec X . &{write: (&{replicate1: +{ACK: end, NACK: end}} || &{replicate2: +{ACK: end, NACK: end}}) . +{QUORUM: &{apply: X}, NO_QUORUM: X}, stepDown: end}}",
        };

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11})
        void parallelBenchmarksSafeWithSync(int idx) {
            // Mark all methods as SYNC → should be safe
            var type = Parser.parse(PARALLEL_BENCHMARKS[idx]);
            var methods = ThreadSafetyChecker.extractMethods(type);
            var levels = new java.util.HashMap<String, ConcurrencyLevel>();
            for (String m : methods) levels.put(m, SYNC);
            var classifier = MethodClassifier.fromMap(levels);
            var r = ThreadSafetyChecker.check(type, classifier);
            assertTrue(r.isSafe(),
                    "Benchmark #" + idx + " should be safe with all SYNC, "
                            + "violations: " + r.violations());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 2, 4, 5, 7, 10, 11})
        void parallelBenchmarksUnsafeWithExclusive(int idx) {
            // Benchmarks with callable methods on BOTH parallel branches
            // Mark all methods as EXCLUSIVE → should be unsafe
            var type = Parser.parse(PARALLEL_BENCHMARKS[idx]);
            var methods = ThreadSafetyChecker.extractMethods(type);
            var levels = new java.util.HashMap<String, ConcurrencyLevel>();
            for (String m : methods) levels.put(m, EXCLUSIVE);
            var classifier = MethodClassifier.fromMap(levels);
            var r = ThreadSafetyChecker.check(type, classifier);
            assertFalse(r.isSafe(),
                    "Benchmark #" + idx + " should be unsafe with all EXCLUSIVE");
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 3, 6, 8, 9})
        void parallelBenchmarksSafeWhenOneBranchIsPureSelection(int idx) {
            // These have one parallel branch with only selections (no callable methods)
            // Even with all EXCLUSIVE, no cross-branch pairs exist → safe
            var type = Parser.parse(PARALLEL_BENCHMARKS[idx]);
            var methods = ThreadSafetyChecker.extractMethods(type);
            var levels = new java.util.HashMap<String, ConcurrencyLevel>();
            for (String m : methods) levels.put(m, EXCLUSIVE);
            var classifier = MethodClassifier.fromMap(levels);
            var r = ThreadSafetyChecker.check(type, classifier);
            assertTrue(r.isSafe(),
                    "Benchmark #" + idx + " should be safe — one branch is pure selection");
        }
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    @Nested
    class EdgeCases {

        @Test
        void nullType() {
            assertThrows(NullPointerException.class,
                    () -> ThreadSafetyChecker.check(null, MethodClassifier.fromMap(Map.of())));
        }

        @Test
        void nullClassifier() {
            assertThrows(NullPointerException.class,
                    () -> ThreadSafetyChecker.check(new End(), null));
        }

        @Test
        void endAlone() {
            var r = ThreadSafetyChecker.check(new End(), MethodClassifier.fromMap(Map.of()));
            assertTrue(r.isSafe());
            assertTrue(r.violations().isEmpty());
            assertTrue(r.classifications().isEmpty());
        }

        @Test
        void sameMethodBothBranches() {
            // Same method name in both branches of parallel
            // (read . end || read . end) — read appears in both
            var r = check("(read . end || read . end)",
                    Map.of("read", READ_ONLY));
            assertTrue(r.isSafe(), "READ_ONLY with itself should be safe");
        }

        @Test
        void sameMethodBothBranchesExclusive() {
            var r = check("(write . end || write . end)",
                    Map.of("write", EXCLUSIVE));
            assertFalse(r.isSafe(), "EXCLUSIVE with itself should be unsafe");
        }

        @Test
        void nestedParallelBothChecked() {
            // Nested parallel (inner parallel inside sequence after outer parallel)
            // Note: this is not WF-Par valid, but the thread safety checker still works
            var node = new Sequence(
                    new Parallel(
                            new Branch(List.of(new Choice("a", new End()))),
                            new Branch(List.of(new Choice("b", new End())))),
                    new Parallel(
                            new Branch(List.of(new Choice("c", new End()))),
                            new Branch(List.of(new Choice("d", new End())))));
            var r = ThreadSafetyChecker.check(node,
                    MethodClassifier.fromMap(Map.of(
                            "a", EXCLUSIVE, "b", READ_ONLY,
                            "c", READ_ONLY, "d", READ_ONLY)));
            assertFalse(r.isSafe()); // a×b is unsafe
            assertEquals(1, r.violations().size());
        }
    }
}
