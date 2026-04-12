package com.bica.reborn.testgen;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the test generator on all 34 benchmarks and prints coverage statistics.
 */
class BenchmarkCoverageTest {

    private static final String[] NAMES = {
        "Iterator",        "File Reader",      "SMTP",            "HTTP",
        "OAuth 2.0",       "Two-Buyer",        "MCP Agent",       "A2A Agent",
        "Shared File",     "ATM",              "Reentrant Lock",  "WebSocket",
        "JDBC Tx",         "Pub/Sub",          "DNS Resolver",    "BICA Pipeline",
        "CI/CD Pipeline",  "TLS Handshake",    "Raft Leader",     "MQTT Client",
        "Circuit Breaker", "Connection Pool",  "gRPC BiDi",       "Blockchain Tx",
        "Kafka Consumer",  "Rate Limiter",     "Saga Orchestr.",  "Two-Phase Commit",
        "Leader Repl.",    "Failover",
        "Enzyme (M-M)",    "Enzyme (Inhib.)",  "Ion Channel",     "Ion Ch. (Na/K)",
    };

    private static final String[] BENCHMARKS = {
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
        "&{clientHello: +{HELLO_RETRY: &{clientHello: &{serverHello: &{certificate: &{verify: &{changeCipher: end}}, psk: &{changeCipher: end}}}}, SERVER_HELLO: &{certificate: &{verify: &{changeCipher: end}}, psk: &{changeCipher: end}}}}",
        "rec X . +{TIMEOUT: &{requestVote: +{ELECTED: rec Y . &{appendEntries: +{ACK: Y, NACK: Y}, heartbeatTimeout: Y, stepDown: X}, REJECTED: X}}, HEARTBEAT: X, SHUTDOWN: end}",
        "&{connect: +{CONNACK: (rec X . &{publish: +{PUBACK: X, TIMEOUT: X}, disconnect: end} || rec Y . +{MESSAGE: Y, SUBACK: Y, DONE: end}) . end, REFUSED: end}}",
        "rec X . &{call: +{SUCCESS: X, FAILURE: +{TRIPPED: rec Y . &{probe: +{OK: X, FAIL: Y}, timeout: end}, OK: X}}, reset: end}",
        "&{init: (rec X . &{acquire: &{use: &{release: X}}, drain: end} || rec Y . &{healthCheck: +{HEALTHY: Y, UNHEALTHY: Y}, shutdown: end})}",
        "&{open: (rec X . &{send: X, halfClose: end} || rec Y . +{RESPONSE: Y, TRAILER: end})}",
        "&{createTx: &{sign: &{broadcast: rec X . +{PENDING: X, CONFIRMED: &{getReceipt: end}, DROPPED: end, FAILED: end}}}}",
        "&{subscribe: (rec X . &{poll: +{RECORDS: &{process: &{commit: X}}, EMPTY: X}, pause: end} || rec Y . +{REBALANCE: Y, REVOKED: end})}",
        "rec X . &{tryAcquire: +{ALLOWED: X, THROTTLED: &{wait: X, abort: end}}, close: end}",
        "&{begin: (&{step1: +{OK: end, FAIL: &{compensate1: end}}} || &{step2: +{OK: end, FAIL: &{compensate2: end}}}) . +{ALL_OK: &{commit: end}, PARTIAL: &{rollback: end}}}",
        "&{prepare: &{allYes: &{commit: +{ACK: end, TIMEOUT: &{abort: end}}}, anyNo: &{abort: end}}}",
        "&{electLeader: rec X . &{write: (&{replicate1: +{ACK: end, NACK: end}} || &{replicate2: +{ACK: end, NACK: end}}) . +{QUORUM: &{apply: X}, NO_QUORUM: X}, stepDown: end}}",
        "&{connect: rec X . &{request: +{OK: X, FAIL: &{reconnect: +{UP: X, DOWN: end}}}, close: end}}",
        "rec X . &{bind_substrate: +{CATALYZE: &{release_product: X}, DISSOCIATE: X}, shutdown: end}",
        "rec X . &{bind_substrate: +{CATALYZE: &{release_product: X}, DISSOCIATE: X}, bind_inhibitor: &{release_inhibitor: X}, shutdown: end}",
        "rec X . &{depolarize: +{OPEN: &{conduct_ions: +{INACTIVATE: &{repolarize: X, permanent_inactivation: end}, CLOSE_DIRECT: X}}, SUBTHRESHOLD: X}, shutdown: end}",
        "rec X . &{depolarize: (&{conduct_Na: +{INACTIVATE_Na: end, CLOSE_Na: end}} || &{conduct_K: &{delayed_close_K: end}}) . &{repolarize: X, permanent_inactivation: end}, shutdown: end}",
    };

    @Test
    void printCoverageStats() {
        var config = new TestGenConfig("Obj", null, "obj", 2, 100, ViolationStyle.DISABLED_ANNOTATION);

        int totalPaths = 0, totalViolations = 0, totalIncomplete = 0, totalTests = 0;
        int truncatedCount = 0;

        System.out.println();
        System.out.printf("%-4s %-18s %6s %6s %6s %8s %8s %5s%n",
                "#", "Benchmark", "States", "Trans", "Paths", "Violat.", "Incompl.", "Trunc");
        System.out.println("-".repeat(75));

        for (int i = 0; i < BENCHMARKS.length; i++) {
            var ast = Parser.parse(BENCHMARKS[i]);
            var ss = StateSpaceBuilder.build(ast);
            var result = PathEnumerator.enumerate(ss, config);

            int paths = result.validPaths().size();
            int violations = result.violations().size();
            int incomplete = result.incompletePrefixes().size();
            int tests = paths + violations + incomplete;

            totalPaths += paths;
            totalViolations += violations;
            totalIncomplete += incomplete;
            totalTests += tests;
            if (result.truncated()) truncatedCount++;

            System.out.printf("%-4d %-18s %6d %6d %6d %8d %8d %5s%n",
                    i, NAMES[i],
                    ss.states().size(), ss.transitions().size(),
                    paths, violations, incomplete,
                    result.truncated() ? "YES" : "");
        }

        System.out.println("-".repeat(75));
        System.out.printf("%-4s %-18s %6s %6s %6d %8d %8d%n",
                "", "TOTAL", "", "", totalPaths, totalViolations, totalIncomplete);
        System.out.printf("%nTotal generated tests: %d  |  Truncated: %d / %d benchmarks%n",
                totalTests, truncatedCount, BENCHMARKS.length);
        System.out.printf("Average per benchmark: %.1f paths, %.1f violations, %.1f incomplete%n",
                totalPaths / (double) BENCHMARKS.length, totalViolations / (double) BENCHMARKS.length, totalIncomplete / (double) BENCHMARKS.length);

        // Verify all succeeded
        assertTrue(totalPaths > 0);
        assertTrue(totalTests > 0);
    }
}
