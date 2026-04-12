package com.bica.reborn.termination;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.parser.Parser;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for termination checking and WF-Par well-formedness
 * ({@link TerminationChecker}).
 *
 * <p>Ported from {@code reticulate/tests/test_termination.py}.
 */
class TerminationCheckerTest {

    // -- helpers ---------------------------------------------------------------

    private static TerminationResult term(String source) {
        return TerminationChecker.checkTermination(Parser.parse(source));
    }

    private static WFParallelResult wf(String source) {
        return TerminationChecker.checkWfParallel(Parser.parse(source));
    }

    // =========================================================================
    // Terminating — non-recursive types (trivially terminating)
    // =========================================================================

    @Nested
    class TerminatingSimple {

        @Test
        void end() {
            assertTrue(TerminationChecker.isTerminating(Parser.parse("end")));
        }

        @Test
        void sequence() {
            assertTrue(TerminationChecker.isTerminating(Parser.parse("a . b . end")));
        }

        @Test
        void branch() {
            assertTrue(TerminationChecker.isTerminating(Parser.parse("&{m: end}")));
        }

        @Test
        void select() {
            assertTrue(TerminationChecker.isTerminating(Parser.parse("+{OK: end, ERR: end}")));
        }

        @Test
        void parallelSimple() {
            assertTrue(TerminationChecker.isTerminating(Parser.parse("(a . end || b . end)")));
        }

        @Test
        void branchMultiple() {
            assertTrue(TerminationChecker.isTerminating(
                    Parser.parse("&{a: end, b: end, c: end}")));
        }

        @Test
        void nestedBranchSelect() {
            assertTrue(TerminationChecker.isTerminating(
                    Parser.parse("&{m: +{OK: end, ERR: end}}")));
        }
    }

    // =========================================================================
    // Terminating — recursive types with exit paths
    // =========================================================================

    @Nested
    class TerminatingRecursion {

        @Test
        void recWithExit() {
            var r = term("rec X . &{read: X, done: end}");
            assertTrue(r.isTerminating());
            assertEquals(List.of(), r.nonTerminatingVars());
        }

        @Test
        void recSelectExit() {
            assertTrue(TerminationChecker.isTerminating(
                    Parser.parse("rec X . +{OK: X, DONE: end}")));
        }

        @Test
        void nestedRecBothTerminating() {
            var r = term("rec X . &{a: rec Y . &{b: Y, done: X}, done: end}");
            assertTrue(r.isTerminating());
        }

        @Test
        void recWithParallelExit() {
            assertTrue(TerminationChecker.isTerminating(
                    Parser.parse("rec X . &{go: (a . end || b . end), loop: X}")));
        }

        @Test
        void recSequenceExit() {
            assertTrue(TerminationChecker.isTerminating(
                    Parser.parse("rec X . &{step: X, done: a . b . end}")));
        }

        @Test
        void javaIterator() {
            assertTrue(TerminationChecker.isTerminating(
                    Parser.parse("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}")));
        }

        @Test
        void fileObject() {
            assertTrue(TerminationChecker.isTerminating(
                    Parser.parse("open . rec X . &{read: +{data: X, eof: close . end}}")));
        }
    }

    // =========================================================================
    // Non-terminating — recursive types without exit paths
    // =========================================================================

    @Nested
    class NonTerminating {

        @Test
        void trivialLoop() {
            var r = term("rec X . X");
            assertFalse(r.isTerminating());
            assertTrue(r.nonTerminatingVars().contains("X"));
        }

        @Test
        void allBranchesLoop() {
            var r = term("rec X . &{loop: X}");
            assertFalse(r.isTerminating());
            assertTrue(r.nonTerminatingVars().contains("X"));
        }

        @Test
        void multipleBranchesAllLoop() {
            var r = term("rec X . &{a: X, b: X}");
            assertFalse(r.isTerminating());
        }

        @Test
        void selectAllLoop() {
            var r = term("rec X . +{OK: X, ERR: X}");
            assertFalse(r.isTerminating());
        }

        @Test
        void nestedAllLoop() {
            var r = term("rec X . &{a: +{OK: X, ERR: X}}");
            assertFalse(r.isTerminating());
        }

        @Test
        void sequenceLeftLoops() {
            // Build AST directly because parser desugars X.end to Branch
            var node = new Rec("X", new Sequence(new Var("X"), new End()));
            var r = TerminationChecker.checkTermination(node);
            assertFalse(r.isTerminating());
            assertTrue(r.nonTerminatingVars().contains("X"));
        }
    }

    // =========================================================================
    // TerminationResult — field inspection
    // =========================================================================

    @Nested
    class TerminationResultFields {

        @Test
        void terminatingResultFields() {
            var r = TerminationChecker.checkTermination(Parser.parse("end"));
            assertTrue(r.isTerminating());
            assertEquals(List.of(), r.nonTerminatingVars());
            assertInstanceOf(TerminationResult.class, r);
        }

        @Test
        void nonTerminatingSingleVar() {
            var r = TerminationChecker.checkTermination(Parser.parse("rec X . X"));
            assertFalse(r.isTerminating());
            assertEquals(List.of("X"), r.nonTerminatingVars());
        }

        @Test
        void multipleNonTerminatingVars() {
            var r = TerminationChecker.checkTermination(
                    Parser.parse("rec X . &{a: rec Y . Y, b: X}"));
            assertTrue(r.nonTerminatingVars().contains("Y"));
            assertFalse(r.isTerminating());
        }

        @Test
        void twoIndependentNonTerminating() {
            var node = new Parallel(
                    new Rec("X", new Var("X")),
                    new Rec("Y", new Var("Y")));
            var r = TerminationChecker.checkTermination(node);
            assertFalse(r.isTerminating());
            assertTrue(r.nonTerminatingVars().contains("X"));
            assertTrue(r.nonTerminatingVars().contains("Y"));
        }

        @Test
        void frozenResult() {
            var r = TerminationChecker.checkTermination(Parser.parse("end"));
            assertThrows(UnsupportedOperationException.class,
                    () -> r.nonTerminatingVars().add("X"));
        }
    }

    // =========================================================================
    // WF-Par — well-formedness of parallel composition
    // =========================================================================

    @Nested
    class WFParallel {

        @Test
        void simpleOk() {
            var r = wf("(a . end || b . end)");
            assertTrue(r.isWellFormed());
            assertEquals(List.of(), r.errors());
        }

        @Test
        void recursiveOk() {
            var r = wf("(rec X . &{a: X, done: end} || rec Y . &{b: Y, done: end})");
            assertTrue(r.isWellFormed());
        }

        @Test
        void nonTerminatingLeft() {
            var r = wf("(rec X . X || end)");
            assertFalse(r.isWellFormed());
            assertTrue(r.errors().stream().anyMatch(e -> e.contains("non-terminating")));
        }

        @Test
        void nonTerminatingRight() {
            var r = wf("(end || rec Y . Y)");
            assertFalse(r.isWellFormed());
            assertTrue(r.errors().stream().anyMatch(e -> e.contains("non-terminating")));
        }

        @Test
        void crossBranchVars() {
            var node = new Parallel(new Var("X"), new Rec("X", new Var("X")));
            var r = TerminationChecker.checkWfParallel(node);
            assertFalse(r.isWellFormed());
            assertTrue(r.errors().stream().anyMatch(e -> e.contains("cross-branch")));
        }

        @Test
        void nestedParallelLeft() {
            var r = wf("( (a . end || b . end) || c . end )");
            assertFalse(r.isWellFormed());
            assertTrue(r.errors().stream().anyMatch(e -> e.contains("nested")));
        }

        @Test
        void nestedParallelRight() {
            var r = wf("( a . end || (b . end || c . end) )");
            assertFalse(r.isWellFormed());
            assertTrue(r.errors().stream().anyMatch(e -> e.contains("nested")));
        }

        @Test
        void noParallelIsWellFormed() {
            var r = wf("rec X . &{a: X, done: end}");
            assertTrue(r.isWellFormed());
            assertEquals(List.of(), r.errors());
        }

        @Test
        void wfResultFrozen() {
            var r = wf("end");
            assertThrows(UnsupportedOperationException.class,
                    () -> r.errors().add("X"));
        }
    }

    // =========================================================================
    // Benchmarks — all 17 benchmark protocols
    // =========================================================================

    @Nested
    class Benchmarks {

        private static final String[] ALL_BENCHMARKS = {
            "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
            "&{open: rec X . &{read: +{data: X, eof: &{close: end}}}}",
            "&{connect: &{ehlo: rec X . &{mail: &{rcpt: &{data: +{OK: X, ERR: X}}}, quit: end}}}",
            "&{connect: rec X . &{request: +{OK200: &{readBody: X}, ERR4xx: X, ERR5xx: X}, close: end}}",
            "&{requestAuth: +{GRANTED: &{getToken: +{TOKEN: rec X . &{useToken: X, refreshToken: +{OK: X, EXPIRED: end}, revoke: end}, ERROR: end}}, DENIED: end}}",
            "&{lookup: &{getPrice: (&{proposeA: end} || &{proposeB: +{ACCEPT: &{pay: end}, REJECT: end}})}}",
            "&{initialize: (rec X . &{callTool: +{RESULT: X, ERROR: X}, listTools: X, shutdown: end} || rec Y . +{NOTIFICATION: Y, DONE: end})}",
            "&{sendTask: rec X . +{WORKING: &{getStatus: X, cancel: end}, COMPLETED: &{getArtifact: end}, FAILED: end}}",
            "&{open: +{OK: (rec X . &{read: X, doneRead: end} || rec Y . &{write: Y, doneWrite: end}) . &{close: end}, ERR: end}}",
            "&{insertCard: &{enterPIN: +{AUTH: rec X . &{checkBalance: X, withdraw: +{OK: X, INSUFFICIENT: X}, deposit: X, ejectCard: end}, REJECTED: &{ejectCard: end}}}}",
            "rec X . &{lock: &{unlock: X, newCondition: &{await: &{signal: &{unlock: X}}}}, close: end}",
            "&{connect: +{OPEN: (rec X . &{send: X, ping: X, closeSend: end} || rec Y . +{MESSAGE: Y, PONG: Y, CLOSE: end}), REFUSED: end}}",
            "&{begin: rec X . &{query: +{RESULT: X, ERROR: X}, update: +{ROWS: X, ERROR: X}, commit: end, rollback: end}}",
            "&{connect: rec X . &{subscribe: X, unsubscribe: X, poll: +{MSG: X, EMPTY: X}, disconnect: end}}",
            "rec X . &{query: +{ANSWER: X, NXDOMAIN: X, SERVFAIL: X, TIMEOUT: &{retry: X, abandon: end}}, close: end}",
            "&{input: &{parse: +{OK: &{buildStateSpace: +{OK: (&{checkLattice: &{checkTermination: &{checkWFPar: end}}} || &{renderDiagram: end}) . &{returnResult: end}, ERROR: end}}, ERROR: end}}}",
            "&{trigger: &{checkout: &{setup: (&{lint: end} || &{test: end}) . +{PASS: &{deploy: +{OK: end, FAIL: &{rollback: end}}}, FAIL: end}}}}",
            // #17 TLS Handshake
            "&{clientHello: +{HELLO_RETRY: &{clientHello: &{serverHello: &{certificate: &{verify: &{changeCipher: end}}, psk: &{changeCipher: end}}}}, SERVER_HELLO: &{certificate: &{verify: &{changeCipher: end}}, psk: &{changeCipher: end}}}}",
            // #18 Raft Leader Election
            "rec X . +{TIMEOUT: &{requestVote: +{ELECTED: rec Y . &{appendEntries: +{ACK: Y, NACK: Y}, heartbeatTimeout: Y, stepDown: X}, REJECTED: X}}, HEARTBEAT: X, SHUTDOWN: end}",
            // #19 MQTT Client
            "&{connect: +{CONNACK: (rec X . &{publish: +{PUBACK: X, TIMEOUT: X}, disconnect: end} || rec Y . +{MESSAGE: Y, SUBACK: Y, DONE: end}) . end, REFUSED: end}}",
            // #20 Circuit Breaker
            "rec X . &{call: +{SUCCESS: X, FAILURE: +{TRIPPED: rec Y . &{probe: +{OK: X, FAIL: Y}, timeout: end}, OK: X}}, reset: end}",
            // #21 Connection Pool
            "&{init: (rec X . &{acquire: &{use: &{release: X}}, drain: end} || rec Y . &{healthCheck: +{HEALTHY: Y, UNHEALTHY: Y}, shutdown: end})}",
            // #22 gRPC BiDi Stream
            "&{open: (rec X . &{send: X, halfClose: end} || rec Y . +{RESPONSE: Y, TRAILER: end})}",
            // #23 Blockchain Tx
            "&{createTx: &{sign: &{broadcast: rec X . +{PENDING: X, CONFIRMED: &{getReceipt: end}, DROPPED: end, FAILED: end}}}}",
            // #24 Kafka Consumer
            "&{subscribe: (rec X . &{poll: +{RECORDS: &{process: &{commit: X}}, EMPTY: X}, pause: end} || rec Y . +{REBALANCE: Y, REVOKED: end})}",
            // #25 Rate Limiter
            "rec X . &{tryAcquire: +{ALLOWED: X, THROTTLED: &{wait: X, abort: end}}, close: end}",
            // #26 Saga Orchestrator
            "&{begin: (&{step1: +{OK: end, FAIL: &{compensate1: end}}} || &{step2: +{OK: end, FAIL: &{compensate2: end}}}) . +{ALL_OK: &{commit: end}, PARTIAL: &{rollback: end}}}",
            // #27 Two-Phase Commit
            "&{prepare: &{allYes: &{commit: +{ACK: end, TIMEOUT: &{abort: end}}}, anyNo: &{abort: end}}}",
            // #28 Leader Replication
            "&{electLeader: rec X . &{write: (&{replicate1: +{ACK: end, NACK: end}} || &{replicate2: +{ACK: end, NACK: end}}) . +{QUORUM: &{apply: X}, NO_QUORUM: X}, stepDown: end}}",
            // #29 Failover
            "&{connect: rec X . &{request: +{OK: X, FAIL: &{reconnect: +{UP: X, DOWN: end}}}, close: end}}",
        };

        private static final int[] PARALLEL_INDICES = {5, 6, 8, 11, 15, 16, 19, 21, 22, 24, 26, 28};

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29})
        void allBenchmarksTerminating(int idx) {
            var node = Parser.parse(ALL_BENCHMARKS[idx]);
            var r = TerminationChecker.checkTermination(node);
            assertTrue(r.isTerminating(),
                    "Benchmark #" + idx + " should be terminating, "
                            + "but non-terminating vars: " + r.nonTerminatingVars());
        }

        @ParameterizedTest
        @ValueSource(ints = {5, 6, 8, 11, 15, 16, 19, 21, 22, 24, 26, 28})
        void parallelBenchmarksWfPar(int idx) {
            var node = Parser.parse(ALL_BENCHMARKS[idx]);
            var r = TerminationChecker.checkWfParallel(node);
            assertTrue(r.isWellFormed(),
                    "Benchmark #" + idx + " should be WF-Par, but errors: " + r.errors());
        }
    }

    // =========================================================================
    // Helpers — internal helper function tests
    // =========================================================================

    @Nested
    class Helpers {

        // -- freeVars --

        @Test
        void freeVarsEnd() {
            assertEquals(Set.of(), TerminationChecker.freeVars(new End()));
        }

        @Test
        void freeVarsVar() {
            assertEquals(Set.of("X"), TerminationChecker.freeVars(new Var("X")));
        }

        @Test
        void freeVarsRecBinds() {
            assertEquals(Set.of(),
                    TerminationChecker.freeVars(new Rec("X", new Var("X"))));
        }

        @Test
        void freeVarsRecWithOther() {
            var node = new Rec("X", new Branch(List.of(
                    new Choice("a", new Var("X")),
                    new Choice("b", new Var("Y")))));
            assertEquals(Set.of("Y"), TerminationChecker.freeVars(node));
        }

        @Test
        void freeVarsParallel() {
            var node = new Parallel(new Var("A"), new Var("B"));
            assertEquals(Set.of("A", "B"), TerminationChecker.freeVars(node));
        }

        @Test
        void freeVarsSequence() {
            var node = new Sequence(new Var("X"), new End());
            assertEquals(Set.of("X"), TerminationChecker.freeVars(node));
        }

        // -- boundVars --

        @Test
        void boundVarsEnd() {
            assertEquals(Set.of(), TerminationChecker.boundVars(new End()));
        }

        @Test
        void boundVarsVar() {
            assertEquals(Set.of(), TerminationChecker.boundVars(new Var("X")));
        }

        @Test
        void boundVarsRec() {
            assertEquals(Set.of("X"),
                    TerminationChecker.boundVars(new Rec("X", new Var("X"))));
        }

        @Test
        void boundVarsNestedRec() {
            var node = new Rec("X", new Rec("Y", new Branch(List.of(
                    new Choice("a", new Var("X")),
                    new Choice("b", new Var("Y"))))));
            assertEquals(Set.of("X", "Y"), TerminationChecker.boundVars(node));
        }

        @Test
        void boundVarsParallel() {
            var node = new Parallel(
                    new Rec("X", new Var("X")),
                    new Rec("Y", new Var("Y")));
            assertEquals(Set.of("X", "Y"), TerminationChecker.boundVars(node));
        }

        // -- containsParallel --

        @Test
        void containsParallelEnd() {
            assertFalse(TerminationChecker.containsParallel(new End()));
        }

        @Test
        void containsParallelDirect() {
            assertTrue(TerminationChecker.containsParallel(
                    new Parallel(new End(), new End())));
        }

        @Test
        void containsParallelNestedInBranch() {
            var node = new Branch(List.of(
                    new Choice("m", new Parallel(new End(), new End()))));
            assertTrue(TerminationChecker.containsParallel(node));
        }

        @Test
        void containsParallelInRec() {
            var node = new Rec("X", new Parallel(new Var("X"), new End()));
            assertTrue(TerminationChecker.containsParallel(node));
        }

        @Test
        void noParallel() {
            var node = Parser.parse("rec X . &{a: X, done: end}");
            assertFalse(TerminationChecker.containsParallel(node));
        }

        // -- hasExitPath edge cases --

        @Test
        void exitPathEnd() {
            assertTrue(TerminationChecker.hasExitPath(new End(), "X"));
        }

        @Test
        void exitPathForbiddenVar() {
            assertFalse(TerminationChecker.hasExitPath(new Var("X"), "X"));
        }

        @Test
        void exitPathOtherVar() {
            assertTrue(TerminationChecker.hasExitPath(new Var("Y"), "X"));
        }

        @Test
        void exitPathThroughInnerRec() {
            var node = new Rec("Y", new Branch(List.of(
                    new Choice("b", new Var("Y")),
                    new Choice("done", new End()))));
            assertTrue(TerminationChecker.hasExitPath(node, "X"));
        }
    }
}
