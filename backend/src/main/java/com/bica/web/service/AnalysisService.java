package com.bica.web.service;

import com.bica.reborn.ast.*;
import com.bica.reborn.cli.BicaCli;
import com.bica.reborn.concurrency.ConcurrencyLevel;
import com.bica.reborn.concurrency.MethodClassifier;
import com.bica.reborn.concurrency.ThreadSafetyChecker;
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
import com.bica.reborn.globaltype.*;
import com.bica.web.dto.AnalyzeResponse;
import com.bica.web.dto.BenchmarkDto;
import com.bica.web.dto.CoverageFrameDto;
import com.bica.web.dto.CoverageStoryboardResponse;
import com.bica.web.dto.GlobalAnalyzeResponse;
import com.bica.web.dto.TestGenResponse;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
        String toolUrl = "/tools/analyzer?type=" + URLEncoder.encode(def.typeString(), StandardCharsets.UTF_8);

        var enumConfig = new TestGenConfig(def.name(), null, "obj", 2, 100, ViolationStyle.CALL_ANYWAY);
        var enumResult = PathEnumerator.enumerate(ss, enumConfig);
        int numTests = enumResult.validPaths().size()
                + enumResult.violations().size()
                + enumResult.incompletePrefixes().size();

        boolean isRecursive = containsRec(ast);
        int depth = recDepth(ast);
        Set<String> methodSet = ThreadSafetyChecker.extractMethods(ast);
        List<String> methods = new ArrayList<>(methodSet);
        Collections.sort(methods);
        boolean threadSafe = computeThreadSafe(ast, methodSet);

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
                toolUrl,
                numTests,
                isRecursive,
                depth,
                methods.size(),
                methods,
                threadSafe,
                enumResult.validPaths().size(),
                enumResult.violations().size(),
                enumResult.incompletePrefixes().size()
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

        boolean usesParallel = containsParallel(ast);
        boolean isRecursive = containsRec(ast);
        int depth = recDepth(ast);
        Set<String> methodSet = ThreadSafetyChecker.extractMethods(ast);
        List<String> methods = new ArrayList<>(methodSet);
        Collections.sort(methods);
        boolean threadSafe = computeThreadSafe(ast, methodSet);

        var enumConfig = new TestGenConfig("Analysis", null, "obj", 2, 100, ViolationStyle.CALL_ANYWAY);
        var enumResult = PathEnumerator.enumerate(ss, enumConfig);
        int numTests = enumResult.validPaths().size()
                + enumResult.violations().size()
                + enumResult.incompletePrefixes().size();

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
                dotSource,
                usesParallel,
                isRecursive,
                depth,
                methods,
                methods.size(),
                threadSafe,
                numTests,
                enumResult.validPaths().size(),
                enumResult.violations().size(),
                enumResult.incompletePrefixes().size()
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

    public CoverageStoryboardResponse coverageStoryboard(String typeString) {
        SessionType ast = Parser.parse(typeString);
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult latticeResult = LatticeChecker.checkLattice(ss);

        var config = new TestGenConfig("Coverage", null, "obj", 2, 100, ViolationStyle.CALL_ANYWAY);
        var enumResult = PathEnumerator.enumerate(ss, config);

        var frames = new ArrayList<CoverageFrameDto>();
        int totalTransitions = ss.transitions().size();
        int totalStates = ss.states().size();

        // Valid paths
        for (int i = 0; i < enumResult.validPaths().size(); i++) {
            var path = enumResult.validPaths().get(i);
            var covered = collectCoveredTransitions(ss, path.steps());
            var coveredStates = collectCoveredStates(ss, path.steps());
            double transCov = totalTransitions > 0 ? (double) covered.size() / totalTransitions : 0;
            double stateCov = totalStates > 0 ? (double) coveredStates.size() / totalStates : 0;
            String dot = BicaCli.buildDotWithCoverage(ss, latticeResult, covered, coveredStates);
            String svg = renderSvg(dot);
            frames.add(new CoverageFrameDto(
                    "validPath_" + (i + 1), "valid", transCov, stateCov, svg));
        }

        // Violations
        for (int i = 0; i < enumResult.violations().size(); i++) {
            var viol = enumResult.violations().get(i);
            var covered = collectCoveredTransitions(ss, viol.prefixPath());
            var coveredStates = collectCoveredStates(ss, viol.prefixPath());
            double transCov = totalTransitions > 0 ? (double) covered.size() / totalTransitions : 0;
            double stateCov = totalStates > 0 ? (double) coveredStates.size() / totalStates : 0;
            String dot = BicaCli.buildDotWithCoverage(ss, latticeResult, covered, coveredStates);
            String svg = renderSvg(dot);
            frames.add(new CoverageFrameDto(
                    "violation_" + viol.disabledMethod() + "_at_" + viol.state(),
                    "violation", transCov, stateCov, svg));
        }

        // Incomplete prefixes
        for (int i = 0; i < enumResult.incompletePrefixes().size(); i++) {
            var prefix = enumResult.incompletePrefixes().get(i);
            var covered = collectCoveredTransitions(ss, prefix.steps());
            var coveredStates = collectCoveredStates(ss, prefix.steps());
            double transCov = totalTransitions > 0 ? (double) covered.size() / totalTransitions : 0;
            double stateCov = totalStates > 0 ? (double) coveredStates.size() / totalStates : 0;
            String dot = BicaCli.buildDotWithCoverage(ss, latticeResult, covered, coveredStates);
            String svg = renderSvg(dot);
            frames.add(new CoverageFrameDto(
                    "incomplete_" + (i + 1), "incomplete", transCov, stateCov, svg));
        }

        return new CoverageStoryboardResponse(totalTransitions, totalStates, frames);
    }

    private Set<StateSpace.Transition> collectCoveredTransitions(StateSpace ss,
                                                                   List<PathEnumerator.Step> steps) {
        var covered = new HashSet<StateSpace.Transition>();
        int current = ss.top();
        for (var step : steps) {
            for (var t : ss.transitions()) {
                if (t.source() == current && t.label().equals(step.label()) && t.target() == step.target()) {
                    covered.add(t);
                    break;
                }
            }
            current = step.target();
        }
        return covered;
    }

    private Set<Integer> collectCoveredStates(StateSpace ss, List<PathEnumerator.Step> steps) {
        var covered = new HashSet<Integer>();
        covered.add(ss.top());
        for (var step : steps) {
            covered.add(step.target());
        }
        return covered;
    }

    public List<BenchmarkDto> getBenchmarks() {
        return benchmarkCache;
    }

    private String renderSvg(String dotSource) {
        return Graphviz.fromString(dotSource).render(Format.SVG).toString();
    }

    // --- AST helper methods ---

    private static boolean containsParallel(SessionType type) {
        return switch (type) {
            case End e -> false;
            case Var v -> false;
            case Branch b -> b.choices().stream().anyMatch(c -> containsParallel(c.body()));
            case Select s -> s.choices().stream().anyMatch(c -> containsParallel(c.body()));
            case Parallel p -> true;
            case Rec r -> containsParallel(r.body());
            case Sequence seq -> containsParallel(seq.left()) || containsParallel(seq.right());
        };
    }

    private static boolean containsRec(SessionType type) {
        return switch (type) {
            case End e -> false;
            case Var v -> false;
            case Branch b -> b.choices().stream().anyMatch(c -> containsRec(c.body()));
            case Select s -> s.choices().stream().anyMatch(c -> containsRec(c.body()));
            case Parallel p -> containsRec(p.left()) || containsRec(p.right());
            case Rec r -> true;
            case Sequence seq -> containsRec(seq.left()) || containsRec(seq.right());
        };
    }

    private static int recDepth(SessionType type) {
        return switch (type) {
            case End e -> 0;
            case Var v -> 0;
            case Branch b -> b.choices().stream().mapToInt(c -> recDepth(c.body())).max().orElse(0);
            case Select s -> s.choices().stream().mapToInt(c -> recDepth(c.body())).max().orElse(0);
            case Parallel p -> Math.max(recDepth(p.left()), recDepth(p.right()));
            case Rec r -> 1 + recDepth(r.body());
            case Sequence seq -> Math.max(recDepth(seq.left()), recDepth(seq.right()));
        };
    }

    private static boolean computeThreadSafe(SessionType ast, Set<String> methodSet) {
        Map<String, ConcurrencyLevel> defaultLevels = new LinkedHashMap<>();
        for (String m : methodSet) {
            defaultLevels.put(m, ConcurrencyLevel.SHARED);
        }
        var tsResult = ThreadSafetyChecker.check(ast, MethodClassifier.fromMap(defaultLevels));
        return tsResult.isSafe();
    }

    // --- Global type analysis ---

    public GlobalAnalyzeResponse analyzeGlobal(String typeString) {
        GlobalType g = GlobalTypeParser.parse(typeString);
        String pretty = GlobalTypePrettyPrinter.pretty(g);
        Set<String> roles = GlobalTypeChecker.roles(g);
        StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
        LatticeResult latticeResult = LatticeChecker.checkLattice(ss);

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

        boolean usesParallel = containsGlobalParallel(g);
        boolean isRecursive = containsGlobalRec(g);

        // Project onto each role
        Map<String, GlobalAnalyzeResponse.ProjectionDto> projections = new LinkedHashMap<>();
        for (String role : new TreeSet<>(roles)) {
            try {
                SessionType localType = Projection.project(g, role);
                String localPretty = PrettyPrinter.pretty(localType);
                StateSpace localSs = StateSpaceBuilder.build(localType);
                LatticeResult localLr = LatticeChecker.checkLattice(localSs);

                String localDot = BicaCli.buildDot(localSs, localLr, role, true, true);
                String localSvg;
                try {
                    localSvg = renderSvg(localDot);
                } catch (Exception e) {
                    localSvg = "";
                }

                projections.put(role, new GlobalAnalyzeResponse.ProjectionDto(
                        role, localPretty,
                        localSs.states().size(), localSs.transitions().size(),
                        localLr.isLattice(), localSvg));
            } catch (ProjectionError e) {
                projections.put(role, new GlobalAnalyzeResponse.ProjectionDto(
                        role, "projection undefined: " + e.getMessage(),
                        0, 0, false, ""));
            }
        }

        return new GlobalAnalyzeResponse(
                pretty,
                ss.states().size(),
                ss.transitions().size(),
                latticeResult.numScc(),
                latticeResult.isLattice(),
                counterexample,
                new ArrayList<>(new TreeSet<>(roles)),
                roles.size(),
                usesParallel,
                isRecursive,
                svgHtml,
                dotSource,
                projections
        );
    }

    private static boolean containsGlobalParallel(GlobalType g) {
        return switch (g) {
            case GEnd e -> false;
            case GVar v -> false;
            case GMessage m -> m.choices().stream()
                    .anyMatch(c -> containsGlobalParallel(c.body()));
            case GParallel p -> true;
            case GRec r -> containsGlobalParallel(r.body());
        };
    }

    private static boolean containsGlobalRec(GlobalType g) {
        return switch (g) {
            case GEnd e -> false;
            case GVar v -> false;
            case GMessage m -> m.choices().stream()
                    .anyMatch(c -> containsGlobalRec(c.body()));
            case GParallel p -> containsGlobalRec(p.left()) || containsGlobalRec(p.right());
            case GRec r -> true;
        };
    }
}
