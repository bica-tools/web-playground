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
import com.bica.reborn.composition.CompositionChecker;
import com.bica.reborn.composition.CompositionResult;
import com.bica.reborn.composition.ComparisonResult;
import com.bica.reborn.composition.ParticipantPair;
import com.bica.reborn.composition.SynchronizedResult;
import com.bica.reborn.contextfree.ContextFreeChecker;
import com.bica.reborn.duality.DualityChecker;
import com.bica.reborn.globaltype.*;
import com.bica.reborn.recursion.RecursionChecker;
import com.bica.reborn.subtyping.SubtypingChecker;
import com.bica.reborn.duality.DualityResult;
import com.bica.web.dto.AnalyzeResponse;
import com.bica.web.dto.CompareResponse;
import com.bica.web.dto.CompositionRequest;
import com.bica.web.dto.CompositionResponse;
import com.bica.web.dto.BenchmarkDto;
import com.bica.web.dto.CoverageFrameDto;
import com.bica.web.dto.CoverageStoryboardResponse;
import com.bica.web.dto.DualResponse;
import com.bica.web.dto.GameDataResponse;
import com.bica.web.dto.GamePlaysResponse;
import com.bica.web.dto.GlobalAnalyzeResponse;
import com.bica.web.dto.SubtypeResponse;
import com.bica.web.dto.TestGenResponse;
import com.bica.web.dto.TraceResponse;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private List<BenchmarkDto> benchmarkCache = Collections.emptyList();

    // --- Benchmark definitions (mirroring bica/src/test/.../BenchmarkCoverageTest.java) ---

    private record BenchmarkDef(String name, String description, String typeString, boolean usesParallel, List<String> tags) {}

    private static final List<BenchmarkDef> BENCHMARK_DEFS = List.of(
            new BenchmarkDef("Java Iterator",
                    "The java.util.Iterator protocol: repeatedly call hasNext, then next if TRUE or stop if FALSE.",
                    "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}", false, List.of("java", "design-pattern")),
            new BenchmarkDef("File Object",
                    "A file handle: open, then repeatedly read until EOF, then close.",
                    "open . rec X . &{read: +{data: X, eof: close . end}}", false, List.of("java", "io")),
            new BenchmarkDef("SMTP",
                    "Simplified SMTP session: connect, EHLO, then loop sending mail or QUIT.",
                    "connect . ehlo . rec X . &{mail: rcpt . data . +{OK: X, ERR: X}, quit: end}", false, List.of("networking", "protocol")),
            new BenchmarkDef("HTTP Connection",
                    "A persistent HTTP connection: connect, then repeatedly send requests or close.",
                    "connect . rec X . &{request: +{OK200: readBody . X, ERR4xx: X, ERR5xx: X}, close: end}", false, List.of("networking", "protocol")),
            new BenchmarkDef("OAuth 2.0",
                    "OAuth 2.0 authorization code flow: request authorization, obtain token, then use/refresh/revoke.",
                    "requestAuth . +{GRANTED: getToken . +{TOKEN: rec X . &{useToken: X, refreshToken: +{OK: X, EXPIRED: end}, revoke: end}, ERROR: end}, DENIED: end}", false, List.of("security", "protocol")),
            new BenchmarkDef("Two-Buyer",
                    "The two-buyer protocol: lookup, get price, then two buyers concurrently propose.",
                    "lookup . getPrice . (proposeA . end || proposeB . +{ACCEPT: pay . end, REJECT: end})", true, List.of("multiparty", "classic")),
            new BenchmarkDef("MCP",
                    "AI agent Model Context Protocol: initialize, then concurrently handle tool calls and notifications.",
                    "initialize . (rec X . &{callTool: +{RESULT: X, ERROR: X}, listTools: X, shutdown: end} || rec Y . +{NOTIFICATION: Y, DONE: end})", true, List.of("ai", "protocol")),
            new BenchmarkDef("A2A",
                    "Google's Agent-to-Agent protocol: send a task, then poll for status with cancellation.",
                    "sendTask . rec X . +{WORKING: &{getStatus: X, cancel: end}, COMPLETED: getArtifact . end, FAILED: end}", false, List.of("ai", "protocol")),
            new BenchmarkDef("File Channel",
                    "A file channel with concurrent read/write streams: open, parallel read and write, then close.",
                    "open . +{OK: (rec X . &{read: X, doneRead: end} || rec Y . &{write: Y, doneWrite: end}) . close . end, ERR: end}", true, List.of("java", "io", "concurrency")),
            new BenchmarkDef("ATM",
                    "An ATM session: insert card, enter PIN, authenticate, then banking operations or eject.",
                    "insertCard . enterPIN . +{AUTH: rec X . &{checkBalance: X, withdraw: +{OK: X, INSUFFICIENT: X}, deposit: X, ejectCard: end}, REJECTED: ejectCard . end}", false, List.of("finance", "classic")),
            new BenchmarkDef("Reentrant Lock",
                    "A reentrant lock: repeatedly lock/unlock with optional condition variables, or close.",
                    "rec X . &{lock: &{unlock: X, newCondition: &{await: &{signal: &{unlock: X}}}}, close: end}", false, List.of("java", "concurrency")),
            new BenchmarkDef("WebSocket",
                    "A WebSocket connection: connect, then concurrently send/ping and receive messages.",
                    "connect . +{OPEN: (rec X . &{send: X, ping: X, closeSend: end} || rec Y . +{MESSAGE: Y, PONG: Y, CLOSE: end}), REFUSED: end}", true, List.of("networking", "protocol", "concurrency")),
            new BenchmarkDef("DB Transaction",
                    "A database transaction: begin, query/update, then commit or rollback.",
                    "begin . rec X . &{query: +{RESULT: X, ERROR: X}, update: +{ROWS: X, ERROR: X}, commit: end, rollback: end}", false, List.of("database", "classic")),
            new BenchmarkDef("Pub/Sub",
                    "A publish/subscribe client: connect, subscribe, poll for messages, or disconnect.",
                    "connect . rec X . &{subscribe: X, unsubscribe: X, poll: +{MSG: X, EMPTY: X}, disconnect: end}", false, List.of("messaging", "distributed")),
            new BenchmarkDef("DNS Resolver",
                    "A DNS resolver: repeatedly query with various outcomes, or close.",
                    "rec X . &{query: +{ANSWER: X, NXDOMAIN: X, SERVFAIL: X, TIMEOUT: &{retry: X, abandon: end}}, close: end}", false, List.of("networking", "protocol")),
            new BenchmarkDef("Reticulate Pipeline",
                    "The reticulate analysis pipeline as a session type. Verification and rendering run concurrently.",
                    "input . parse . +{OK: buildStateSpace . +{OK: (checkLattice . checkTermination . checkWFPar . end || renderDiagram . end) . returnResult . end, ERROR: end}, ERROR: end}", true, List.of("systems", "concurrency")),
            new BenchmarkDef("GitHub CI Workflow",
                    "A CI/CD pipeline: trigger, checkout, setup, parallel lint and test, then deploy or fail.",
                    "trigger . checkout . setup . (lint . end || test . end) . +{PASS: deploy . +{OK: end, FAIL: rollback . end}, FAIL: end}", true, List.of("devops", "systems", "concurrency")),
            new BenchmarkDef("TLS Handshake",
                    "A TLS 1.3 handshake: client hello with possible retry, then certificate or PSK.",
                    "clientHello . +{HELLO_RETRY: clientHello . serverHello . &{certificate: verify . changeCipher . end, psk: changeCipher . end}, SERVER_HELLO: &{certificate: verify . changeCipher . end, psk: changeCipher . end}}", false, List.of("security", "networking", "protocol")),
            new BenchmarkDef("Raft Leader Election",
                    "Raft consensus: followers timeout, request votes, become leader or step down.",
                    "rec X . +{TIMEOUT: requestVote . +{ELECTED: rec Y . &{appendEntries: +{ACK: Y, NACK: Y}, heartbeatTimeout: Y, stepDown: X}, REJECTED: X}, HEARTBEAT: X, SHUTDOWN: end}", false, List.of("distributed", "consensus")),
            new BenchmarkDef("MQTT Client",
                    "An MQTT client: connect, then concurrently publish and receive messages.",
                    "connect . +{CONNACK: (rec X . &{publish: +{PUBACK: X, TIMEOUT: X}, disconnect: end} || rec Y . +{MESSAGE: Y, SUBACK: Y, DONE: end}) . end, REFUSED: end}", true, List.of("messaging", "protocol", "concurrency")),
            new BenchmarkDef("Circuit Breaker",
                    "Circuit breaker pattern: closed state accepts calls, open state probes for recovery.",
                    "rec X . &{call: +{SUCCESS: X, FAILURE: +{TRIPPED: rec Y . &{probe: +{OK: X, FAIL: Y}, timeout: end}, OK: X}}, reset: end}", false, List.of("design-pattern", "distributed")),
            new BenchmarkDef("Connection Pool",
                    "A connection pool: concurrent acquire/use/release cycles and health checks.",
                    "init . (rec X . &{acquire: use . release . X, drain: end} || rec Y . &{healthCheck: +{HEALTHY: Y, UNHEALTHY: Y}, shutdown: end})", true, List.of("java", "concurrency", "database")),
            new BenchmarkDef("gRPC BiDi Stream",
                    "A gRPC bidirectional streaming RPC: concurrently send requests and receive responses.",
                    "open . (rec X . &{send: X, halfClose: end} || rec Y . +{RESPONSE: Y, TRAILER: end})", true, List.of("networking", "protocol", "concurrency")),
            new BenchmarkDef("Blockchain Tx",
                    "A blockchain transaction lifecycle: create, sign, broadcast, then poll for confirmation.",
                    "createTx . sign . broadcast . rec X . +{PENDING: X, CONFIRMED: &{getReceipt: end}, DROPPED: end, FAILED: end}", false, List.of("finance", "distributed")),
            new BenchmarkDef("Kafka Consumer",
                    "A Kafka consumer: subscribe, then concurrently poll/process and handle rebalance events.",
                    "subscribe . (rec X . &{poll: +{RECORDS: process . commit . X, EMPTY: X}, pause: end} || rec Y . +{REBALANCE: Y, REVOKED: end})", true, List.of("messaging", "distributed", "concurrency")),
            new BenchmarkDef("Rate Limiter",
                    "A rate limiter: try to acquire permits, wait or abort if throttled, or close.",
                    "rec X . &{tryAcquire: +{ALLOWED: X, THROTTLED: &{wait: X, abort: end}}, close: end}", false, List.of("design-pattern", "systems")),
            new BenchmarkDef("Saga Orchestrator",
                    "A saga orchestrator: two concurrent steps with compensation, then commit or rollback.",
                    "begin . (step1 . +{OK: end, FAIL: compensate1 . end} || step2 . +{OK: end, FAIL: compensate2 . end}) . +{ALL_OK: commit . end, PARTIAL: rollback . end}", true, List.of("distributed", "design-pattern", "concurrency")),
            new BenchmarkDef("Two-Phase Commit",
                    "Two-phase commit: prepare, then either all yes (commit) or any no (abort).",
                    "prepare . &{allYes: commit . +{ACK: end, TIMEOUT: abort . end}, anyNo: abort . end}", false, List.of("distributed", "consensus", "classic")),
            new BenchmarkDef("Leader Replication",
                    "Leader-based replication: write with parallel replication, check quorum, or step down.",
                    "electLeader . rec X . &{write: (replicate1 . +{ACK: end, NACK: end} || replicate2 . +{ACK: end, NACK: end}) . +{QUORUM: apply . X, NO_QUORUM: X}, stepDown: end}", true, List.of("distributed", "consensus", "concurrency")),
            new BenchmarkDef("Failover",
                    "A failover protocol: request, reconnect on failure, or close.",
                    "connect . rec X . &{request: +{OK: X, FAIL: reconnect . +{UP: X, DOWN: end}}, close: end}", false, List.of("distributed", "systems")),
            new BenchmarkDef("Enzyme (Michaelis-Menten)",
                    "Michaelis-Menten enzyme kinetics: bind substrate, catalyze or dissociate, or shut down.",
                    "rec X . &{bind_substrate: +{CATALYZE: release_product . X, DISSOCIATE: X}, shutdown: end}", false, List.of("biology", "biochemistry")),
            new BenchmarkDef("Enzyme (Competitive Inhibition)",
                    "Enzyme with competitive inhibition: bind substrate or inhibitor, or shut down.",
                    "rec X . &{bind_substrate: +{CATALYZE: release_product . X, DISSOCIATE: X}, bind_inhibitor: &{release_inhibitor: X}, shutdown: end}", false, List.of("biology", "biochemistry")),
            new BenchmarkDef("Ion Channel (Single)",
                    "Voltage-gated ion channel: depolarize, conduct ions, inactivate or close, or shut down.",
                    "rec X . &{depolarize: +{OPEN: conduct_ions . +{INACTIVATE: &{repolarize: X, permanent_inactivation: end}, CLOSE_DIRECT: X}, SUBTHRESHOLD: X}, shutdown: end}", false, List.of("biology", "neuroscience")),
            new BenchmarkDef("Ion Channel (Na+/K+ Parallel)",
                    "Parallel Na+/K+ ion channels during action potential with concurrent activation.",
                    "rec X . &{depolarize: (conduct_Na . +{INACTIVATE_Na: end, CLOSE_Na: end} || conduct_K . delayed_close_K . end) . &{repolarize: X, permanent_inactivation: end}, shutdown: end}", true, List.of("biology", "neuroscience", "concurrency")),
            new BenchmarkDef("Ki3 Onboarding",
                    "Ki3 SaaS tenant provisioning: parallel VPS/DNS, then database/proxy/monitoring, health checks, activation or rollback.",
                    "validateContract . +{APPROVED: (createVPS . +{PROVISIONED: end, FAILED: end} || configureDNS . +{PROPAGATED: end, FAILED: end}) . createKeycloakRealm . +{CREATED: (createSchema . seedData . end || configureProxy . requestSSL . end) . (setupMonitoring . createDashboards . end || configureBackup . end) . runHealthChecks . +{HEALTHY: notifyTenant . activateSubscription . end, UNHEALTHY: rollback . notifyOps . end}, FAILED: end}, REJECTED: end}", true, List.of("devops", "systems", "concurrency")),
            new BenchmarkDef("Ki3 Offboarding",
                    "Ki3 SaaS tenant teardown: three parallel phases removing services, middleware, infrastructure, then VPS deletion.",
                    "confirmCancellation . exportData . notifyTenant . (removeMonitoring . end || removeBackup . end) . (revokeSSL . end || deleteSchema . end) . (deleteKeycloakRealm . end || removeDNS . end) . deleteVPS . archiveAuditLog . closeSubscription . end", true, List.of("devops", "systems", "concurrency")),
            new BenchmarkDef("Carnot Cycle",
                    "Carnot thermodynamic engine: four-stroke cycle with optional stop.",
                    "rec X . &{isothermal_expand: adiabatic_expand . isothermal_compress . &{adiabatic_compress: X, stop: end}}", false, List.of("physics", "thermodynamics")),
            new BenchmarkDef("Quantum Measurement",
                    "Sequential spin measurements on a qubit: prepare, then choose measurement basis (X or Z).",
                    "prepare . &{measure_x: +{up_x: measure_z . end, down_x: measure_z . end}, measure_z: +{up_z: measure_x . end, down_z: measure_x . end}}", false, List.of("physics", "quantum")),
            new BenchmarkDef("Billiard Map",
                    "A 2D billiard table: periodic orbits as recursive reflections off four walls.",
                    "rec X . &{reflect_north: X, reflect_south: X, reflect_east: X, reflect_west: X, exit: end}", false, List.of("physics", "dynamical-systems")),
            new BenchmarkDef("Ki3 CI/CD Pipeline",
                    "Ki3 multi-tenant SaaS CI/CD: parallel lint, parallel test, parallel Docker build, security scan, staging, production.",
                    "(lintBackend . +{PASS: end, FAIL: end} || lintFrontend . +{PASS: end, FAIL: end}) . +{LINT_OK: (testBackend . +{PASS: end, FAIL: end} || testFrontend . +{PASS: end, FAIL: end}) . +{TESTS_OK: (buildBackend . +{PUSHED: end, FAIL: end} || buildFrontend . +{PUSHED: end, FAIL: end}) . +{IMAGES_OK: securityScan . +{CLEAN: deployStaging . +{HEALTHY: e2eSmoke . +{PASS: approveProduction . +{APPROVED: deployProduction . +{HEALTHY: end, UNHEALTHY: rollbackProduction . end}, REJECTED: end}, FAIL: rollbackStaging . end}, UNHEALTHY: rollbackStaging . end}, CRITICAL: end}, BUILD_FAIL: end}, TEST_FAIL: end}, LINT_FAIL: end}", true, List.of("devops", "systems", "concurrency")),
            new BenchmarkDef("Ribosome Translation",
                    "Ribosome translation: elongation loop (sense codon, match tRNA, peptide bond) until stop codon triggers release.",
                    "rec X . &{sense_codon: match_tRNA . peptide_bond . X, stop_codon: release . end}", false, List.of("biology", "molecular")),
            new BenchmarkDef("Polysome",
                    "Polysome: two ribosomes translating the same mRNA concurrently.",
                    "initiate . (rec X . &{sense_codon: match_tRNA . peptide_bond . X, stop_codon: release . end} || rec Y . &{sense_codon: match_tRNA . peptide_bond . Y, stop_codon: release . end}) . end", true, List.of("biology", "molecular", "concurrency")),
            new BenchmarkDef("Alternative Splicing",
                    "Alternative splicing: transcription produces pre-mRNA, spliceosome selects among three isoforms.",
                    "transcribe . splice . +{ISOFORM_A: translate . end, ISOFORM_B: translate . end, ISOFORM_C: translate . end}", false, List.of("biology", "molecular")),
            new BenchmarkDef("Lac Operon",
                    "Lac operon gene regulation: sense lactose, then transcribe/translate or repress.",
                    "rec X . &{sense_lactose: +{PRESENT: transcribe . translate . X, ABSENT: repress . X}, cell_death: end}", false, List.of("biology", "genetics")),
            new BenchmarkDef("DNA Replication Fork",
                    "DNA replication fork: leading strand synthesis in parallel with lagging strand (Okazaki fragments).",
                    "unwind . (rec X . &{synthesize_leading: X, complete_leading: end} || rec Y . &{synthesize_okazaki: ligate . Y, complete_lagging: end}) . end", true, List.of("biology", "molecular", "concurrency")),
            new BenchmarkDef("tRNA Charging",
                    "Aminoacyl-tRNA synthetase charging cycle: bind amino acid, bind tRNA, transfer, release, loop.",
                    "rec X . &{bind_amino_acid: bind_tRNA . transfer . release . X, shutdown: end}", false, List.of("biology", "biochemistry")),
            new BenchmarkDef("mRNA Lifecycle",
                    "Complete mRNA lifecycle: transcription, capping, splicing, export, translation, decay.",
                    "transcribe . cap . splice . export . translate . decay . end", false, List.of("biology", "molecular")),
            new BenchmarkDef("Protein Folding",
                    "Chaperone-assisted protein folding: fold attempt, then native/misfolded/aggregate outcomes.",
                    "rec X . &{fold_attempt: +{NATIVE: end, MISFOLDED: chaperone_bind . unfold . X, AGGREGATE: degrade . end}}", false, List.of("biology", "biochemistry")),
            new BenchmarkDef("Cell Cycle",
                    "Eukaryotic cell cycle with G1/G2 checkpoints, S phase, mitosis, repair, differentiation, apoptosis.",
                    "rec X . &{G1_checkpoint: +{PASS: S_phase . G2_checkpoint . +{PASS: mitosis . X, FAIL: repair . X}, FAIL: repair . X}, differentiate: end, apoptosis: end}", false, List.of("biology", "cell")),
            new BenchmarkDef("Signal Transduction PKA",
                    "PKA signal transduction: hormone binds receptor, activates G-protein, produces cAMP, PKA concurrently phosphorylates and opens ion channels.",
                    "hormone_bind . activate_G_protein . produce_cAMP . activate_PKA . (phosphorylate . end || open_ion_channel . end) . end", true, List.of("biology", "cell", "concurrency")),
            new BenchmarkDef("ER-Golgi Secretory",
                    "ER-Golgi secretory pathway: synthesize, fold, quality control, then sort or degrade.",
                    "synthesize . fold . +{NATIVE: ER_exit . golgi_sort . +{MEMBRANE: end, SECRETED: end, LYSOSOME: end}, MISFOLDED: ER_retain . degrade . end}", false, List.of("biology", "cell")),
            new BenchmarkDef("Glucose Regulation",
                    "Homeostatic glucose regulation: measure glucose, respond with glucagon/insulin, loop.",
                    "rec X . &{measure_glucose: +{LOW: release_glucagon . X, NORMAL: X, HIGH: release_insulin . X}, apoptosis: end}", false, List.of("biology", "cell")),
            new BenchmarkDef("Action Potential Full",
                    "Full action potential with parallel Na+/K+ channels, repolarization, and refractory period.",
                    "rec X . &{stimulus: +{THRESHOLD: (Na_activate . +{INACTIVATE: end, CLOSE: end} || K_activate . K_delayed_close . end) . repolarize . refractory . X, SUBTHRESHOLD: X}, shutdown: end}", true, List.of("biology", "neuroscience", "concurrency")),
            new BenchmarkDef("Apoptosis",
                    "Programmed cell death: intrinsic (mitochondrial) and extrinsic (death receptor) pathways converge on caspase-3.",
                    "receive_signal . +{INTRINSIC: mitochondrial_release . activate_caspase9 . activate_caspase3 . DNA_fragmentation . end, EXTRINSIC: activate_caspase8 . activate_caspase3 . DNA_fragmentation . end}", false, List.of("biology", "cell")),
            new BenchmarkDef("T-Cell Activation",
                    "T-cell immune response: antigen presentation, activation, concurrent cytokine release and proliferation.",
                    "antigen_present . activate . (release_cytokine . end || proliferate . end) . +{MEMORY: end, APOPTOSIS: end}", true, List.of("biology", "immunology", "concurrency")),
            new BenchmarkDef("Photosynthesis-Respiration",
                    "Plant cell energy metabolism: photosynthesis (light reactions || Calvin cycle) then respiration (glycolysis || Krebs cycle).",
                    "(light_reactions . produce_ATP . end || calvin_cycle . fix_carbon . end) . (glycolysis . pyruvate . end || krebs_cycle . electron_transport . end) . end", true, List.of("biology", "biochemistry", "concurrency"))
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
                enumResult.incompletePrefixes().size(),
                def.tags()
        );
    }

    @Cacheable(value = "analysis", key = "#typeString.hashCode()")
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

    @Cacheable(value = "testgen", key = "T(java.util.Objects).hash(#typeString, #className, #packageName, #maxRevisits)")
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

    @Cacheable(value = "coverage", key = "#typeString.hashCode()")
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

    /** Render a session type's Hasse diagram as SVG string for OG image previews. */
    public String renderOgSvg(String typeString) {
        SessionType ast = Parser.parse(typeString);
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        String dot = BicaCli.buildDot(ss, lr, null, true, true);
        return Graphviz.fromString(dot).width(800).render(Format.SVG).toString();
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

    // --- Type comparison ---

    @Cacheable(value = "compare", key = "T(java.util.Objects).hash(#type1Str, #type2Str)")
    public CompareResponse compareTypes(String type1Str, String type2Str) {
        SessionType ast1 = Parser.parse(type1Str);
        SessionType ast2 = Parser.parse(type2Str);
        String pretty1 = PrettyPrinter.pretty(ast1);
        String pretty2 = PrettyPrinter.pretty(ast2);

        // Subtyping
        boolean sub12 = SubtypingChecker.isSubtype(ast1, ast2);
        boolean sub21 = SubtypingChecker.isSubtype(ast2, ast1);
        String subtypingRelation;
        if (sub12 && sub21) subtypingRelation = "equivalent";
        else if (sub12) subtypingRelation = "type1 <: type2";
        else if (sub21) subtypingRelation = "type2 <: type1";
        else subtypingRelation = "unrelated";

        // Duality
        SessionType dual1 = DualityChecker.dual(ast1);
        SessionType dual2 = DualityChecker.dual(ast2);
        String dualStr1 = PrettyPrinter.pretty(dual1);
        String dualStr2 = PrettyPrinter.pretty(dual2);
        // Check if type1 and type2 are duals of each other (dual(type1) == type2)
        boolean areDuals = SubtypingChecker.isSubtype(dual1, ast2)
                && SubtypingChecker.isSubtype(ast2, dual1);

        // State spaces
        StateSpace ss1 = StateSpaceBuilder.build(ast1);
        StateSpace ss2 = StateSpaceBuilder.build(ast2);
        LatticeResult lr1 = LatticeChecker.checkLattice(ss1);
        LatticeResult lr2 = LatticeChecker.checkLattice(ss2);
        String dot1 = BicaCli.buildDot(ss1, lr1, null, true, true);
        String dot2 = BicaCli.buildDot(ss2, lr2, null, true, true);
        String svg1, svg2;
        try { svg1 = renderSvg(dot1); } catch (Exception e) { svg1 = ""; }
        try { svg2 = renderSvg(dot2); } catch (Exception e) { svg2 = ""; }

        // Chomsky
        var chomsky1 = ContextFreeChecker.classifyChomsky(ast1);
        var chomsky2 = ContextFreeChecker.classifyChomsky(ast2);

        // Recursion
        var rec1 = RecursionChecker.analyzeRecursion(ast1);
        var rec2 = RecursionChecker.analyzeRecursion(ast2);

        // Method diff
        Set<String> methods1 = ThreadSafetyChecker.extractMethods(ast1);
        Set<String> methods2 = ThreadSafetyChecker.extractMethods(ast2);
        var shared = new ArrayList<>(methods1);
        shared.retainAll(methods2);
        Collections.sort(shared);
        var unique1 = new ArrayList<>(methods1);
        unique1.removeAll(methods2);
        Collections.sort(unique1);
        var unique2 = new ArrayList<>(methods2);
        unique2.removeAll(methods1);
        Collections.sort(unique2);

        return new CompareResponse(
                pretty1, pretty2,
                sub12, sub21, subtypingRelation,
                dualStr1, dualStr2, areDuals,
                ss1.states().size(), ss1.transitions().size(), lr1.isLattice(), svg1,
                ss2.states().size(), ss2.transitions().size(), lr2.isLattice(), svg2,
                chomsky1.level(), chomsky2.level(),
                rec1.numRecBinders() > 0, rec1.isGuarded(), rec1.isContractive(), rec1.isTailRecursive(),
                rec2.numRecBinders() > 0, rec2.isGuarded(), rec2.isContractive(), rec2.isTailRecursive(),
                shared, unique1, unique2
        );
    }

    // --- Subtype check (MCP tool: subtype_check) ---

    @Cacheable(value = "subtype", key = "T(java.util.Objects).hash(#subtypeStr, #supertypeStr)")
    public SubtypeResponse checkSubtype(String subtypeStr, String supertypeStr) {
        SessionType astSub = Parser.parse(subtypeStr);
        SessionType astSup = Parser.parse(supertypeStr);
        String prettySub = PrettyPrinter.pretty(astSub);
        String prettySup = PrettyPrinter.pretty(astSup);

        boolean sub = SubtypingChecker.isSubtype(astSub, astSup);
        boolean reverse = SubtypingChecker.isSubtype(astSup, astSub);
        boolean equiv = sub && reverse;
        String relation;
        if (equiv) relation = "equivalent";
        else if (sub) relation = "subtype <: supertype";
        else if (reverse) relation = "supertype <: subtype (reversed)";
        else relation = "unrelated";

        StateSpace ssSub = StateSpaceBuilder.build(astSub);
        StateSpace ssSup = StateSpaceBuilder.build(astSup);
        LatticeResult lrSub = LatticeChecker.checkLattice(ssSub);
        LatticeResult lrSup = LatticeChecker.checkLattice(ssSup);
        String dotSub = BicaCli.buildDot(ssSub, lrSub, null, true, true);
        String dotSup = BicaCli.buildDot(ssSup, lrSup, null, true, true);
        String svgSub, svgSup;
        try { svgSub = renderSvg(dotSub); } catch (Exception e) { svgSub = ""; }
        try { svgSup = renderSvg(dotSup); } catch (Exception e) { svgSup = ""; }

        return new SubtypeResponse(
                prettySub, prettySup, sub, equiv, relation,
                ssSub.states().size(), ssSup.states().size(),
                svgSub, svgSup
        );
    }

    // --- Dual (MCP tool: dual) ---

    @Cacheable(value = "dual", key = "#typeStr.hashCode()")
    public DualResponse computeDual(String typeStr) {
        SessionType ast = Parser.parse(typeStr);
        String prettyOrig = PrettyPrinter.pretty(ast);

        DualityResult dr = DualityChecker.check(ast);
        SessionType dualAst = DualityChecker.dual(ast);
        String prettyDual = PrettyPrinter.pretty(dualAst);

        StateSpace ssOrig = StateSpaceBuilder.build(ast);
        StateSpace ssDual = StateSpaceBuilder.build(dualAst);
        LatticeResult lrOrig = LatticeChecker.checkLattice(ssOrig);
        LatticeResult lrDual = LatticeChecker.checkLattice(ssDual);
        String dotOrig = BicaCli.buildDot(ssOrig, lrOrig, null, true, true);
        String dotDual = BicaCli.buildDot(ssDual, lrDual, null, true, true);
        String svgOrig, svgDual;
        try { svgOrig = renderSvg(dotOrig); } catch (Exception e) { svgOrig = ""; }
        try { svgDual = renderSvg(dotDual); } catch (Exception e) { svgDual = ""; }

        return new DualResponse(
                prettyOrig, prettyDual,
                dr.isInvolution(), dr.isIsomorphic(), dr.selectionFlipped(),
                ssOrig.states().size(), ssDual.states().size(),
                svgOrig, svgDual
        );
    }

    // --- Trace validation (MCP tool: trace_validate) ---

    @Cacheable(value = "trace", key = "T(java.util.Objects).hash(#typeStr, #traceStr)")
    public TraceResponse validateTrace(String typeStr, String traceStr) {
        SessionType ast = Parser.parse(typeStr);
        String pretty = PrettyPrinter.pretty(ast);
        StateSpace ss = StateSpaceBuilder.build(ast);

        // Parse trace: comma or space separated
        String[] methods = traceStr.trim().split("[,\\s]+");

        var path = new ArrayList<Map<String, Object>>();
        int current = ss.top();
        boolean valid = true;
        String violationAt = "";

        for (String method : methods) {
            if (method.isBlank()) continue;
            boolean found = false;
            for (var t : ss.transitions()) {
                if (t.source() == current && t.label().equals(method)) {
                    path.add(Map.of("method", method, "from", t.source(), "to", t.target(), "valid", true));
                    current = t.target();
                    found = true;
                    break;
                }
            }
            if (!found) {
                var enabled = new ArrayList<String>();
                for (var t : ss.transitions()) {
                    if (t.source() == current) enabled.add(t.label());
                }
                path.add(Map.of("method", method, "from", current, "to", current, "valid", false,
                        "enabled", enabled));
                valid = false;
                violationAt = method;
                break;
            }
        }

        boolean atEnd = current == ss.bottom();
        // Methods enabled at the final state
        var enabledAtEnd = new ArrayList<String>();
        for (var t : ss.transitions()) {
            if (t.source() == current) enabledAtEnd.add(t.label());
        }

        return new TraceResponse(valid, valid && atEnd, current, path, violationAt, atEnd, enabledAtEnd, pretty);
    }

    // --- Composition ---

    public CompositionResponse compose(List<CompositionRequest.ParticipantEntry> participants,
                                        String globalType) {
        // Build participant entries for CompositionChecker
        var entries = new ArrayList<Map.Entry<String, SessionType>>();
        for (var p : participants) {
            SessionType ast = Parser.parse(p.typeString());
            entries.add(Map.entry(p.name(), ast));
        }

        // Synchronized composition (includes free product)
        SynchronizedResult syncResult = CompositionChecker.synchronizedCompose(entries);

        // Per-participant info
        var participantDtos = new ArrayList<CompositionResponse.ParticipantDto>();
        for (var p : participants) {
            StateSpace ss = syncResult.stateSpaces().get(p.name());
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            String dot = BicaCli.buildDot(ss, lr, p.name(), true, true);
            String svg;
            try { svg = renderSvg(dot); } catch (Exception e) { svg = ""; }
            participantDtos.add(new CompositionResponse.ParticipantDto(
                    p.name(),
                    PrettyPrinter.pretty(syncResult.participants().get(p.name())),
                    ss.states().size(),
                    ss.transitions().size(),
                    lr.isLattice(),
                    svg
            ));
        }

        // Free product SVG
        StateSpace freeSs = syncResult.freeProduct();
        LatticeResult freeLr = LatticeChecker.checkLattice(freeSs);
        String freeDot = BicaCli.buildDot(freeSs, freeLr, "Free Product", true, true);
        String freeSvg;
        try { freeSvg = renderSvg(freeDot); } catch (Exception e) { freeSvg = ""; }

        // Synced product SVG
        StateSpace syncSs = syncResult.synced();
        LatticeResult syncLr = LatticeChecker.checkLattice(syncSs);
        String syncDot = BicaCli.buildDot(syncSs, syncLr, "Synchronized Product", true, true);
        String syncSvg;
        try { syncSvg = renderSvg(syncDot); } catch (Exception e) { syncSvg = ""; }

        // Compatibility entries
        var compatEntries = new ArrayList<CompositionResponse.CompatibilityEntry>();
        for (var entry : syncResult.compatibility().entrySet()) {
            compatEntries.add(new CompositionResponse.CompatibilityEntry(
                    entry.getKey().first(), entry.getKey().second(), entry.getValue()));
        }

        // Shared labels entries
        var sharedLabelEntries = new ArrayList<CompositionResponse.SharedLabelsEntry>();
        for (var entry : syncResult.sharedLabels().entrySet()) {
            sharedLabelEntries.add(new CompositionResponse.SharedLabelsEntry(
                    entry.getKey().first(), entry.getKey().second(),
                    new ArrayList<>(entry.getValue())));
        }

        // Optional global comparison
        CompositionResponse.GlobalComparisonDto globalDto = null;
        if (globalType != null && !globalType.isBlank()) {
            Map<String, SessionType> participantMap = new LinkedHashMap<>();
            for (var e : entries) {
                participantMap.put(e.getKey(), e.getValue());
            }
            ComparisonResult cmp = CompositionChecker.compareWithGlobal(participantMap, globalType.trim());
            globalDto = new CompositionResponse.GlobalComparisonDto(
                    cmp.globalStates(),
                    cmp.globalIsLattice(),
                    cmp.embeddingExists(),
                    cmp.overApproximationRatio(),
                    cmp.roleTypeMatches()
            );
        }

        return new CompositionResponse(
                participants.size(),
                participantDtos,
                freeSs.states().size(),
                freeSs.transitions().size(),
                freeLr.isLattice(),
                freeSvg,
                syncSs.states().size(),
                syncSs.transitions().size(),
                syncLr.isLattice(),
                syncSvg,
                syncResult.reductionRatio(),
                compatEntries,
                sharedLabelEntries,
                globalDto
        );
    }

    // --- Global type analysis ---

    @Cacheable(value = "globalAnalysis", key = "#typeString.hashCode()")
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

    // --- Game data for Protocol Duel ---

    public GameDataResponse gameData(String typeString) {
        var ast = Parser.parse(typeString);
        var ss = StateSpaceBuilder.build(ast);
        var pretty = PrettyPrinter.pretty(ast);

        // Compute ranks via BFS from top
        Map<Integer, Integer> ranks = new LinkedHashMap<>();
        Queue<Integer> queue = new ArrayDeque<>();
        ranks.put(ss.top(), 0);
        queue.add(ss.top());

        Map<Integer, List<Integer>> adj = new LinkedHashMap<>();
        for (var t : ss.transitions()) {
            adj.computeIfAbsent(t.source(), k -> new ArrayList<>()).add(t.target());
        }

        while (!queue.isEmpty()) {
            int node = queue.poll();
            for (int neighbor : adj.getOrDefault(node, List.of())) {
                if (!ranks.containsKey(neighbor)) {
                    ranks.put(neighbor, ranks.get(node) + 1);
                    queue.add(neighbor);
                }
            }
        }

        int maxRank = ranks.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        // Group by rank
        Map<Integer, List<Integer>> byRank = new LinkedHashMap<>();
        for (var entry : ranks.entrySet()) {
            byRank.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }

        // Compute positions
        double boardWidth = 800;
        double ySpacing = 120;
        double yOffset = 80;

        Map<Integer, double[]> positions = new LinkedHashMap<>();
        for (int r = 0; r <= maxRank; r++) {
            var statesAtRank = byRank.getOrDefault(r, List.of());
            int n = statesAtRank.size();
            if (n == 0) continue;
            double xSpacing = boardWidth / (n + 1);
            for (int i = 0; i < n; i++) {
                int sid = statesAtRank.get(i);
                double x = xSpacing * (i + 1);
                double y = yOffset + r * ySpacing;
                positions.put(sid, new double[]{x, y});
            }
        }

        // Build nodes
        List<GameDataResponse.GameNode> nodes = new ArrayList<>();
        for (int sid : ss.states().stream().sorted().toList()) {
            double[] pos = positions.getOrDefault(sid, new double[]{400, 100});
            String label = ss.labels().getOrDefault(sid, String.valueOf(sid));
            String kind = classifyGameNode(ss, sid);
            nodes.add(new GameDataResponse.GameNode(sid, pos[0], pos[1], label, kind, sid == ss.top(), sid == ss.bottom()));
        }

        // Build edges
        List<GameDataResponse.GameEdge> edges = new ArrayList<>();
        for (var t : ss.transitions()) {
            boolean isSel = t.kind() == com.bica.reborn.statespace.TransitionKind.SELECTION;
            edges.add(new GameDataResponse.GameEdge(t.source(), t.target(), t.label(), isSel));
        }

        return new GameDataResponse(nodes, edges, ss.top(), ss.bottom(), ss.states().size(), ss.transitions().size(), pretty);
    }

    private String classifyGameNode(com.bica.reborn.statespace.StateSpace ss, int sid) {
        if (sid == ss.bottom()) return "end";
        if (sid == ss.top()) return "top";
        var outgoing = ss.transitionsFrom(sid);
        if (outgoing.isEmpty()) return "end";
        if (outgoing.stream().anyMatch(t -> t.kind() == com.bica.reborn.statespace.TransitionKind.SELECTION)) {
            return "select";
        }
        return "branch";
    }

    // --- Game plays: tests as game replays ---

    public GamePlaysResponse gamePlays(String typeString) {
        var ast = Parser.parse(typeString);
        var ss = StateSpaceBuilder.build(ast);

        var config = new TestGenConfig("Game", null, "obj", 2, 100, ViolationStyle.CALL_ANYWAY);
        var enumResult = PathEnumerator.enumerate(ss, config);

        int totalTrans = ss.transitions().size();
        int totalStates = ss.states().size();

        var plays = new ArrayList<GamePlaysResponse.GamePlay>();

        // Valid paths → winning games
        for (int i = 0; i < enumResult.validPaths().size(); i++) {
            var path = enumResult.validPaths().get(i);
            var gameSteps = toGameSteps(ss, path.steps());
            var covered = collectCoveredTransitions(ss, path.steps());
            var covStates = collectCoveredStates(ss, path.steps());
            double transCov = totalTrans > 0 ? (double) covered.size() / totalTrans : 0;
            double stateCov = totalStates > 0 ? (double) covStates.size() / totalStates : 0;

            String name = "Play " + (i + 1) + ": " + String.join(" \u2192 ", path.labels());
            plays.add(new GamePlaysResponse.GamePlay(
                    name, "valid", gameSteps, null, null, null, transCov, stateCov));
        }

        // Violations → illegal moves
        for (int i = 0; i < enumResult.violations().size(); i++) {
            var viol = enumResult.violations().get(i);
            var gameSteps = toGameSteps(ss, viol.prefixPath());
            var covered = collectCoveredTransitions(ss, viol.prefixPath());
            var covStates = collectCoveredStates(ss, viol.prefixPath());
            double transCov = totalTrans > 0 ? (double) covered.size() / totalTrans : 0;
            double stateCov = totalStates > 0 ? (double) covStates.size() / totalStates : 0;

            String prefix = viol.prefixLabels().isEmpty() ? "\u22a4" :
                    String.join(" \u2192 ", viol.prefixLabels());
            String name = "Violation: " + prefix + " \u2192 \u2718" + viol.disabledMethod();
            plays.add(new GamePlaysResponse.GamePlay(
                    name, "violation", gameSteps, viol.disabledMethod(),
                    new ArrayList<>(viol.enabledMethods()), null, transCov, stateCov));
        }

        // Incomplete prefixes → abandoned games
        for (int i = 0; i < enumResult.incompletePrefixes().size(); i++) {
            var prefix = enumResult.incompletePrefixes().get(i);
            var gameSteps = toGameSteps(ss, prefix.steps());
            var covered = collectCoveredTransitions(ss, prefix.steps());
            var covStates = collectCoveredStates(ss, prefix.steps());
            double transCov = totalTrans > 0 ? (double) covered.size() / totalTrans : 0;
            double stateCov = totalStates > 0 ? (double) covStates.size() / totalStates : 0;

            String labels = prefix.labels().isEmpty() ? "\u22a4" :
                    String.join(" \u2192 ", prefix.labels());
            String name = "Incomplete: " + labels + " \u2026";
            plays.add(new GamePlaysResponse.GamePlay(
                    name, "incomplete", gameSteps, null, null,
                    new ArrayList<>(prefix.remainingMethods()), transCov, stateCov));
        }

        // Build the board data for rendering
        var board = gameData(typeString);

        return new GamePlaysResponse(plays, totalTrans, totalStates, board);
    }

    private List<GamePlaysResponse.GameStep> toGameSteps(StateSpace ss,
                                                          List<PathEnumerator.Step> steps) {
        var result = new ArrayList<GamePlaysResponse.GameStep>();
        int current = ss.top();
        for (var step : steps) {
            boolean isSel = step.kind() == com.bica.reborn.statespace.TransitionKind.SELECTION;
            result.add(new GamePlaysResponse.GameStep(step.label(), current, step.target(), isSel));
            current = step.target();
        }
        return result;
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
