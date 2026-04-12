package com.bica.reborn.cli;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BicaCli}.
 *
 * <p>All tests call {@code BicaCli.run()} with captured output streams —
 * no subprocess needed.
 */
class BicaCliTest {

    // -- helpers ---------------------------------------------------------------

    private record Result(int exitCode, String out, String err) {}

    private static Result run(String... args) {
        var outBuf = new ByteArrayOutputStream();
        var errBuf = new ByteArrayOutputStream();
        int code = BicaCli.run(args,
                new PrintStream(outBuf, true, StandardCharsets.UTF_8),
                new PrintStream(errBuf, true, StandardCharsets.UTF_8));
        return new Result(code,
                outBuf.toString(StandardCharsets.UTF_8),
                errBuf.toString(StandardCharsets.UTF_8));
    }

    // =========================================================================
    // Default text output
    // =========================================================================

    @Nested
    class DefaultOutput {

        @Test
        void end() {
            var r = run("end");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("Session type: end"));
            assertTrue(r.out().contains("IS a lattice"));
        }

        @Test
        void chain() {
            var r = run("a . b . end");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("States: 3"));
            assertTrue(r.out().contains("IS a lattice"));
        }

        @Test
        void diamond() {
            var r = run("&{m: a.end, n: b.end}");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("IS a lattice"));
        }

        @Test
        void parallel() {
            var r = run("(a.end || b.end)");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("IS a lattice"));
        }

        @Test
        void recursive() {
            var r = run("rec X . &{a: X, b: end}");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("IS a lattice"));
        }

        @Test
        void showsSessionType() {
            var r = run("&{m: end, n: end}");
            assertTrue(r.out().contains("Session type:"));
            assertTrue(r.out().contains("&{m: end, n: end}"));
        }

        @Test
        void showsStates() {
            var r = run("a . end");
            assertTrue(r.out().contains("States:"));
        }

        @Test
        void showsTransitions() {
            var r = run("a . end");
            assertTrue(r.out().contains("Transitions:"));
        }

        @Test
        void showsSccs() {
            var r = run("a . b . end");
            assertTrue(r.out().contains("SCCs:"));
        }

        @Test
        void showsTermination() {
            var r = run("end");
            assertTrue(r.out().contains("Termination:"));
            assertTrue(r.out().contains("\u2713")); // checkmark
        }

        @Test
        void showsWfPar() {
            var r = run("end");
            assertTrue(r.out().contains("WF-Par:"));
        }
    }

    // =========================================================================
    // Error handling
    // =========================================================================

    @Nested
    class Errors {

        @Test
        void parseError() {
            var r = run("&{");
            assertEquals(1, r.exitCode());
            assertTrue(r.err().contains("Parse error"));
        }

        @Test
        void unboundVar() {
            var r = run("X");
            assertEquals(1, r.exitCode());
            assertTrue(r.err().toLowerCase().contains("unbound"));
        }

        @Test
        void emptyString() {
            var r = run("");
            assertEquals(1, r.exitCode());
        }

        @Test
        void invalidSyntax() {
            var r = run("&{m:}");
            assertEquals(1, r.exitCode());
        }
    }

    // =========================================================================
    // DOT output
    // =========================================================================

    @Nested
    class DotOutput {

        @Test
        void dotContainsDigraph() {
            var r = run("--dot", "a.end");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("digraph"));
            assertTrue(r.out().contains("rankdir=TB"));
        }

        @Test
        void dotNoLabels() {
            var r = run("--dot", "--no-labels", "a.end");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("digraph"));
        }

        @Test
        void dotNoEdgeLabels() {
            var r = run("--dot", "--no-edge-labels", "a.end");
            assertEquals(0, r.exitCode());
            // Edge lines should not have label= attributes
            for (String line : r.out().split("\n")) {
                if (line.contains("->")) {
                    assertFalse(line.contains("label="), "Edge should not have label: " + line);
                }
            }
        }

        @Test
        void dotWithTitle() {
            var r = run("--dot", "--title", "Test Title", "a.end");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("Test Title"));
        }

        @Test
        void dotDiamond() {
            var r = run("--dot", "&{m: a.end, n: b.end}");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("digraph"));
            assertTrue(r.out().contains("->"));
        }
    }

    // =========================================================================
    // Help and argument handling
    // =========================================================================

    @Nested
    class HelpAndArgs {

        @Test
        void helpReturnsZero() {
            var r = run("--help");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("Usage"));
        }

        @Test
        void unknownFlagReturnsOne() {
            var r = run("--unknown", "end");
            assertEquals(1, r.exitCode());
            assertTrue(r.err().contains("unknown option"));
        }

        @Test
        void missingTypeStringReturnsOne() {
            var r = run();
            assertEquals(1, r.exitCode());
            assertTrue(r.err().contains("missing"));
        }

        @Test
        void titleWithValue() {
            var r = run("--dot", "--title", "My Title", "a.end");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("My Title"));
        }
    }

    // =========================================================================
    // Test generation
    // =========================================================================

    @Nested
    class TestGenOutput {

        @Test
        void basicTestGen() {
            var r = run("--test-gen", "--class-name", "Obj", "a . end");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("class ObjProtocolTest"));
            assertTrue(r.out().contains("@Test"));
        }

        @Test
        void testGenWithPackage() {
            var r = run("--test-gen", "--class-name", "Obj", "--package", "com.example", "a . end");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("package com.example;"));
        }

        @Test
        void testGenWithVarName() {
            var r = run("--test-gen", "--class-name", "Obj", "--var-name", "sut", "a . end");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("Obj sut = new Obj()"));
        }

        @Test
        void testGenWithMaxRevisits() {
            var r = run("--test-gen", "--class-name", "Iter", "--max-revisits", "1",
                    "rec X . &{next: X, done: end}");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("Valid paths (2)"));
        }

        @Test
        void testGenContainsValidPaths() {
            var r = run("--test-gen", "--class-name", "FH",
                    "open . &{read: close . end, write: close . end}");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("validPath_open_read_close"));
            assertTrue(r.out().contains("validPath_open_write_close"));
        }

        @Test
        void testGenContainsViolations() {
            var r = run("--test-gen", "--class-name", "FH",
                    "open . &{read: close . end, write: close . end}");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("Violations"));
        }

        @Test
        void testGenContainsIncomplete() {
            var r = run("--test-gen", "--class-name", "FH",
                    "open . &{read: close . end, write: close . end}");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("Incomplete protocols"));
        }

        @Test
        void testGenShowsSessionType() {
            var r = run("--test-gen", "--class-name", "Obj", "a . b . end");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("Session type: a . b . end"));
        }

        @Test
        void testGenMissingClassNameFails() {
            var r = run("--test-gen", "a . end");
            assertEquals(1, r.exitCode());
            assertTrue(r.err().contains("--class-name is required"));
        }

        @Test
        void testGenAndDotMutuallyExclusive() {
            var r = run("--test-gen", "--dot", "--class-name", "Obj", "a . end");
            assertEquals(1, r.exitCode());
            assertTrue(r.err().contains("mutually exclusive"));
        }

        @Test
        void testGenClassNameMissingValue() {
            var r = run("--test-gen", "--class-name");
            assertEquals(1, r.exitCode());
            assertTrue(r.err().contains("--class-name requires a value"));
        }

        @Test
        void testGenPackageMissingValue() {
            var r = run("--test-gen", "--class-name", "X", "--package");
            assertEquals(1, r.exitCode());
            assertTrue(r.err().contains("--package requires a value"));
        }

        @Test
        void testGenVarNameMissingValue() {
            var r = run("--test-gen", "--class-name", "X", "--var-name");
            assertEquals(1, r.exitCode());
            assertTrue(r.err().contains("--var-name requires a value"));
        }

        @Test
        void testGenMaxRevisitsMissingValue() {
            var r = run("--test-gen", "--class-name", "X", "--max-revisits");
            assertEquals(1, r.exitCode());
            assertTrue(r.err().contains("--max-revisits requires a value"));
        }

        @Test
        void testGenMaxRevisitsNonInteger() {
            var r = run("--test-gen", "--class-name", "X", "--max-revisits", "abc", "end");
            assertEquals(1, r.exitCode());
            assertTrue(r.err().contains("--max-revisits requires an integer"));
        }

        @Test
        void testGenMaxRevisitsNegative() {
            var r = run("--test-gen", "--class-name", "X", "--max-revisits", "-1", "end");
            assertEquals(1, r.exitCode());
            assertTrue(r.err().contains("non-negative"));
        }

        @Test
        void helpShowsTestGenOption() {
            var r = run("--help");
            assertTrue(r.out().contains("--test-gen"));
            assertTrue(r.out().contains("--class-name"));
        }
    }

    // =========================================================================
    // Skip termination
    // =========================================================================

    @Nested
    class SkipTermination {

        @Test
        void skipTerminationFlag() {
            var r = run("--skip-termination", "rec X . X");
            assertEquals(0, r.exitCode(), "Should succeed with --skip-termination: " + r.err());
        }

        @Test
        void skipTerminationShowsWarning() {
            var r = run("--skip-termination", "rec X . X");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("skipped"),
                    "Output should mention skipped: " + r.out());
            assertTrue(r.out().contains("\u26a0"),
                    "Output should contain warning symbol: " + r.out());
        }

        @Test
        void withoutFlagStillShowsTerminationFailure() {
            var r = run("rec X . X");
            assertEquals(0, r.exitCode());
            assertTrue(r.out().contains("\u2717"),
                    "Should show X mark for termination failure: " + r.out());
            assertFalse(r.out().contains("skipped"),
                    "Should not say skipped: " + r.out());
        }
    }

    // =========================================================================
    // Benchmarks
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

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29})
        void benchmarkProducesValidOutput(int idx) {
            var r = run(ALL_BENCHMARKS[idx]);
            assertEquals(0, r.exitCode(), "Benchmark #" + idx + " should succeed");
            assertTrue(r.out().contains("Session type:"), "Benchmark #" + idx);
            assertTrue(r.out().contains("States:"), "Benchmark #" + idx);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29})
        void benchmarkIsLattice(int idx) {
            var r = run(ALL_BENCHMARKS[idx]);
            assertTrue(r.out().contains("IS a lattice"),
                    "Benchmark #" + idx + " should be a lattice");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29})
        void benchmarkShowsTermination(int idx) {
            var r = run(ALL_BENCHMARKS[idx]);
            assertTrue(r.out().contains("Termination:"),
                    "Benchmark #" + idx + " should show termination");
        }
    }
}
