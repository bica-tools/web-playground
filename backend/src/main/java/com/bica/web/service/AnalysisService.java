package com.bica.web.service;

import com.bica.reborn.ast.Parallel;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.cli.BicaCli;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.ParseError;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.termination.TerminationChecker;
import com.bica.reborn.termination.TerminationResult;
import com.bica.reborn.termination.WFParallelResult;
import com.bica.reborn.testgen.PathEnumerator;
import com.bica.reborn.testgen.TestGenConfig;
import com.bica.reborn.testgen.TestGenerator;
import com.bica.reborn.testgen.ViolationStyle;
import com.bica.web.dto.AnalyzeResponse;
import com.bica.web.dto.BenchmarkDto;
import com.bica.web.dto.TestGenResponse;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private List<BenchmarkDto> benchmarkCache = Collections.emptyList();

    // --- Benchmark definitions (mirroring bica/src/test/.../BenchmarkCoverageTest.java) ---

    private record BenchmarkDef(String name, String description, String typeString, boolean usesParallel) {}

    private static final List<BenchmarkDef> BENCHMARK_DEFS = List.of(
            new BenchmarkDef("Java Iterator",
                    "The java.util.Iterator protocol: repeatedly call hasNext, then next if TRUE or stop if FALSE.",
                    "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}", false),
            new BenchmarkDef("File Object",
                    "A file handle: open, then repeatedly read until EOF, then close.",
                    "open . rec X . &{read: +{data: X, eof: close . end}}", false),
            new BenchmarkDef("SMTP",
                    "Simplified SMTP session: connect, EHLO, then loop sending mail or QUIT.",
                    "connect . ehlo . rec X . &{mail: rcpt . data . +{OK: X, ERR: X}, quit: end}", false),
            new BenchmarkDef("HTTP Connection",
                    "A persistent HTTP connection: connect, then repeatedly send requests or close.",
                    "connect . rec X . &{request: +{OK200: readBody . X, ERR4xx: X, ERR5xx: X}, close: end}", false),
            new BenchmarkDef("OAuth 2.0",
                    "OAuth 2.0 authorization code flow: request authorization, obtain token, then use/refresh/revoke.",
                    "requestAuth . +{GRANTED: getToken . +{TOKEN: rec X . &{useToken: X, refreshToken: +{OK: X, EXPIRED: end}, revoke: end}, ERROR: end}, DENIED: end}", false),
            new BenchmarkDef("Two-Buyer",
                    "The two-buyer protocol: lookup, get price, then two buyers concurrently propose.",
                    "lookup . getPrice . (proposeA . end || proposeB . +{ACCEPT: pay . end, REJECT: end})", true),
            new BenchmarkDef("MCP",
                    "AI agent Model Context Protocol: initialize, then concurrently handle tool calls and notifications.",
                    "initialize . (rec X . &{callTool: +{RESULT: X, ERROR: X}, listTools: X, shutdown: end} || rec Y . +{NOTIFICATION: Y, DONE: end})", true),
            new BenchmarkDef("A2A",
                    "Google's Agent-to-Agent protocol: send a task, then poll for status with cancellation.",
                    "sendTask . rec X . +{WORKING: &{getStatus: X, cancel: end}, COMPLETED: getArtifact . end, FAILED: end}", false),
            new BenchmarkDef("File Channel",
                    "A file channel with concurrent read/write streams: open, parallel read and write, then close.",
                    "open . +{OK: (rec X . &{read: X, doneRead: end} || rec Y . &{write: Y, doneWrite: end}) . close . end, ERR: end}", true),
            new BenchmarkDef("ATM",
                    "An ATM session: insert card, enter PIN, authenticate, then banking operations or eject.",
                    "insertCard . enterPIN . +{AUTH: rec X . &{checkBalance: X, withdraw: +{OK: X, INSUFFICIENT: X}, deposit: X, ejectCard: end}, REJECTED: ejectCard . end}", false),
            new BenchmarkDef("Reentrant Lock",
                    "A reentrant lock: repeatedly lock/unlock with optional condition variables, or close.",
                    "rec X . &{lock: &{unlock: X, newCondition: &{await: &{signal: &{unlock: X}}}}, close: end}", false),
            new BenchmarkDef("WebSocket",
                    "A WebSocket connection: connect, then concurrently send/ping and receive messages.",
                    "connect . +{OPEN: (rec X . &{send: X, ping: X, closeSend: end} || rec Y . +{MESSAGE: Y, PONG: Y, CLOSE: end}), REFUSED: end}", true),
            new BenchmarkDef("DB Transaction",
                    "A database transaction: begin, query/update, then commit or rollback.",
                    "begin . rec X . &{query: +{RESULT: X, ERROR: X}, update: +{ROWS: X, ERROR: X}, commit: end, rollback: end}", false),
            new BenchmarkDef("Pub/Sub",
                    "A publish/subscribe client: connect, subscribe, poll for messages, or disconnect.",
                    "connect . rec X . &{subscribe: X, unsubscribe: X, poll: +{MSG: X, EMPTY: X}, disconnect: end}", false),
            new BenchmarkDef("DNS Resolver",
                    "A DNS resolver: repeatedly query with various outcomes, or close.",
                    "rec X . &{query: +{ANSWER: X, NXDOMAIN: X, SERVFAIL: X, TIMEOUT: &{retry: X, abandon: end}}, close: end}", false),
            new BenchmarkDef("Reticulate Pipeline",
                    "The reticulate analysis pipeline as a session type. Verification and rendering run concurrently.",
                    "input . parse . +{OK: buildStateSpace . +{OK: (checkLattice . checkTermination . checkWFPar . end || renderDiagram . end) . returnResult . end, ERROR: end}, ERROR: end}", true),
            new BenchmarkDef("GitHub CI Workflow",
                    "A CI/CD pipeline: trigger, checkout, setup, parallel lint and test, then deploy or fail.",
                    "trigger . checkout . setup . (lint . end || test . end) . +{PASS: deploy . +{OK: end, FAIL: rollback . end}, FAIL: end}", true),
            new BenchmarkDef("TLS Handshake",
                    "A TLS 1.3 handshake: client hello with possible retry, then certificate or PSK.",
                    "clientHello . +{HELLO_RETRY: clientHello . serverHello . &{certificate: verify . changeCipher . end, psk: changeCipher . end}, SERVER_HELLO: &{certificate: verify . changeCipher . end, psk: changeCipher . end}}", false),
            new BenchmarkDef("Raft Leader Election",
                    "Raft consensus: followers timeout, request votes, become leader or step down.",
                    "rec X . +{TIMEOUT: requestVote . +{ELECTED: rec Y . &{appendEntries: +{ACK: Y, NACK: Y}, heartbeatTimeout: Y, stepDown: X}, REJECTED: X}, HEARTBEAT: X, SHUTDOWN: end}", false),
            new BenchmarkDef("MQTT Client",
                    "An MQTT client: connect, then concurrently publish and receive messages.",
                    "connect . +{CONNACK: (rec X . &{publish: +{PUBACK: X, TIMEOUT: X}, disconnect: end} || rec Y . +{MESSAGE: Y, SUBACK: Y, DONE: end}) . end, REFUSED: end}", true),
            new BenchmarkDef("Circuit Breaker",
                    "Circuit breaker pattern: closed state accepts calls, open state probes for recovery.",
                    "rec X . &{call: +{SUCCESS: X, FAILURE: +{TRIPPED: rec Y . &{probe: +{OK: X, FAIL: Y}, timeout: end}, OK: X}}, reset: end}", false),
            new BenchmarkDef("Connection Pool",
                    "A connection pool: concurrent acquire/use/release cycles and health checks.",
                    "init . (rec X . &{acquire: use . release . X, drain: end} || rec Y . &{healthCheck: +{HEALTHY: Y, UNHEALTHY: Y}, shutdown: end})", true),
            new BenchmarkDef("gRPC BiDi Stream",
                    "A gRPC bidirectional streaming RPC: concurrently send requests and receive responses.",
                    "open . (rec X . &{send: X, halfClose: end} || rec Y . +{RESPONSE: Y, TRAILER: end})", true),
            new BenchmarkDef("Blockchain Tx",
                    "A blockchain transaction lifecycle: create, sign, broadcast, then poll for confirmation.",
                    "createTx . sign . broadcast . rec X . +{PENDING: X, CONFIRMED: &{getReceipt: end}, DROPPED: end, FAILED: end}", false),
            new BenchmarkDef("Kafka Consumer",
                    "A Kafka consumer: subscribe, then concurrently poll/process and handle rebalance events.",
                    "subscribe . (rec X . &{poll: +{RECORDS: process . commit . X, EMPTY: X}, pause: end} || rec Y . +{REBALANCE: Y, REVOKED: end})", true),
            new BenchmarkDef("Rate Limiter",
                    "A rate limiter: try to acquire permits, wait or abort if throttled, or close.",
                    "rec X . &{tryAcquire: +{ALLOWED: X, THROTTLED: &{wait: X, abort: end}}, close: end}", false),
            new BenchmarkDef("Saga Orchestrator",
                    "A saga orchestrator: two concurrent steps with compensation, then commit or rollback.",
                    "begin . (step1 . +{OK: end, FAIL: compensate1 . end} || step2 . +{OK: end, FAIL: compensate2 . end}) . +{ALL_OK: commit . end, PARTIAL: rollback . end}", true),
            new BenchmarkDef("Two-Phase Commit",
                    "Two-phase commit: prepare, then either all yes (commit) or any no (abort).",
                    "prepare . &{allYes: commit . +{ACK: end, TIMEOUT: abort . end}, anyNo: abort . end}", false),
            new BenchmarkDef("Leader Replication",
                    "Leader-based replication: write with parallel replication, check quorum, or step down.",
                    "electLeader . rec X . &{write: (replicate1 . +{ACK: end, NACK: end} || replicate2 . +{ACK: end, NACK: end}) . +{QUORUM: apply . X, NO_QUORUM: X}, stepDown: end}", true),
            new BenchmarkDef("Failover",
                    "A failover protocol: request, reconnect on failure, or close.",
                    "connect . rec X . &{request: +{OK: X, FAIL: reconnect . +{UP: X, DOWN: end}}, close: end}", false),
            new BenchmarkDef("Enzyme (Michaelis-Menten)",
                    "Michaelis-Menten enzyme kinetics: bind substrate, catalyze or dissociate, or shut down.",
                    "rec X . &{bind_substrate: +{CATALYZE: release_product . X, DISSOCIATE: X}, shutdown: end}", false),
            new BenchmarkDef("Enzyme (Competitive Inhibition)",
                    "Enzyme with competitive inhibition: bind substrate or inhibitor, or shut down.",
                    "rec X . &{bind_substrate: +{CATALYZE: release_product . X, DISSOCIATE: X}, bind_inhibitor: &{release_inhibitor: X}, shutdown: end}", false),
            new BenchmarkDef("Ion Channel (Single)",
                    "Voltage-gated ion channel: depolarize, conduct ions, inactivate or close, or shut down.",
                    "rec X . &{depolarize: +{OPEN: conduct_ions . +{INACTIVATE: &{repolarize: X, permanent_inactivation: end}, CLOSE_DIRECT: X}, SUBTHRESHOLD: X}, shutdown: end}", false),
            new BenchmarkDef("Ion Channel (Na+/K+ Parallel)",
                    "Parallel Na+/K+ ion channels during action potential with concurrent activation.",
                    "rec X . &{depolarize: (conduct_Na . +{INACTIVATE_Na: end, CLOSE_Na: end} || conduct_K . delayed_close_K . end) . &{repolarize: X, permanent_inactivation: end}, shutdown: end}", true)
    );

    @PostConstruct
    void initBenchmarkCache() {
        log.info("Pre-rendering {} benchmarks...", BENCHMARK_DEFS.size());
        var entries = new ArrayList<BenchmarkDto>();
        for (var def : BENCHMARK_DEFS) {
            try {
                var dto = analyzeBenchmark(def);
                entries.add(dto);
            } catch (Exception e) {
                log.warn("Failed to pre-render benchmark {}: {}", def.name(), e.getMessage());
            }
        }
        benchmarkCache = Collections.unmodifiableList(entries);
        log.info("Cached {} benchmark results.", benchmarkCache.size());
    }

    private BenchmarkDto analyzeBenchmark(BenchmarkDef def) {
        SessionType ast = Parser.parse(def.typeString());
        String pretty = PrettyPrinter.pretty(ast);
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult latticeResult = LatticeChecker.checkLattice(ss);
        String dotSource = BicaCli.buildDot(ss, latticeResult, null, true, true);
        String svgHtml = renderSvg(dotSource);

        String heroDotSource = BicaCli.buildHeroDot(ss);
        String heroSvgHtml;
        try {
            heroSvgHtml = renderSvg(heroDotSource);
        } catch (Exception e) {
            heroSvgHtml = "";
        }

        String toolUrl = "/tools/analyzer?type=" + URLEncoder.encode(def.typeString(), StandardCharsets.UTF_8);

        var enumConfig = new TestGenConfig(def.name(), null, "obj", 2, 100, ViolationStyle.CALL_ANYWAY);
        var enumResult = PathEnumerator.enumerate(ss, enumConfig);
        int numTests = enumResult.validPaths().size()
                + enumResult.violations().size()
                + enumResult.incompletePrefixes().size();

        return new BenchmarkDto(
                def.name(),
                def.description(),
                def.typeString(),
                pretty,
                ss.states().size(),
                ss.transitions().size(),
                latticeResult.numScc(),
                latticeResult.isLattice(),
                def.usesParallel(),
                svgHtml,
                heroSvgHtml,
                toolUrl,
                numTests
        );
    }

    public AnalyzeResponse analyze(String typeString) {
        SessionType ast = Parser.parse(typeString);
        String pretty = PrettyPrinter.pretty(ast);
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult latticeResult = LatticeChecker.checkLattice(ss);
        TerminationResult termResult = TerminationChecker.checkTermination(ast);
        WFParallelResult wfResult = TerminationChecker.checkWfParallel(ast);

        String dotSource = BicaCli.buildDot(ss, latticeResult, null, true, true);
        String svgHtml;
        try {
            svgHtml = renderSvg(dotSource);
        } catch (Exception e) {
            svgHtml = "";
        }

        String counterexample = null;
        if (latticeResult.counterexample() != null) {
            var cx = latticeResult.counterexample();
            String kind = "no_meet".equals(cx.kind()) ? "no meet" : "no join";
            counterexample = "States " + cx.a() + " and " + cx.b() + " have " + kind;
        }

        return new AnalyzeResponse(
                pretty,
                ss.states().size(),
                ss.transitions().size(),
                latticeResult.numScc(),
                latticeResult.isLattice(),
                counterexample,
                termResult.isTerminating(),
                wfResult.isWellFormed(),
                svgHtml,
                dotSource
        );
    }

    public TestGenResponse generateTests(String typeString, String className,
                                          String packageName, Integer maxRevisits) {
        SessionType ast = Parser.parse(typeString);
        String pretty = PrettyPrinter.pretty(ast);
        StateSpace ss = StateSpaceBuilder.build(ast);

        var config = new TestGenConfig(
                className,
                packageName,
                "obj",
                maxRevisits != null ? maxRevisits : 2,
                100,
                ViolationStyle.CALL_ANYWAY
        );

        String testSource = TestGenerator.generate(ss, config, pretty);
        return new TestGenResponse(testSource);
    }

    public List<BenchmarkDto> getBenchmarks() {
        return benchmarkCache;
    }

    private String renderSvg(String dotSource) {
        return Graphviz.fromString(dotSource).render(Format.SVG).toString();
    }
}
