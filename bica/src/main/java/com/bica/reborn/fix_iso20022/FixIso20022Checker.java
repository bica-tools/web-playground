package com.bica.reborn.fix_iso20022;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.coverage.CoverageChecker;
import com.bica.reborn.coverage.CoverageResult;
import com.bica.reborn.lattice.DistributivityChecker;
import com.bica.reborn.lattice.DistributivityResult;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.testgen.PathEnumerator;
import com.bica.reborn.testgen.PathEnumerator.EnumerationResult;
import com.bica.reborn.testgen.TestGenConfig;
import com.bica.reborn.testgen.TestGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * FIX 4.4 and ISO 20022 financial protocol verification (Step 73).
 *
 * <p>Java port of {@code reticulate/reticulate/fix_iso20022.py} for BICA Reborn.
 * Models financial messaging protocols (FIX 4.4 order lifecycle and ISO 20022
 * payment initiation / clearing / cash reporting) as session types and runs the
 * full lattice + test-generation + compliance pipeline.
 */
public final class FixIso20022Checker {

    private FixIso20022Checker() {}

    // -----------------------------------------------------------------------
    // Records
    // -----------------------------------------------------------------------

    /** A named financial messaging protocol modelled as a session type. */
    public record FinancialProtocol(
            String name,
            String family,
            List<String> messages,
            List<String> transitions,
            String sessionTypeString,
            String description) {

        public FinancialProtocol {
            Objects.requireNonNull(name);
            Objects.requireNonNull(family);
            Objects.requireNonNull(sessionTypeString);
            Objects.requireNonNull(description);
            messages = List.copyOf(messages);
            transitions = List.copyOf(transitions);
        }
    }

    /** A compliance / audit action attached to a state-space point. */
    public record ComplianceAction(int stateId, String action, String rationale) {
        public ComplianceAction {
            Objects.requireNonNull(action);
            Objects.requireNonNull(rationale);
        }
    }

    /** Complete analysis result for a financial protocol. */
    public record FinancialAnalysisResult(
            FinancialProtocol protocol,
            SessionType ast,
            StateSpace stateSpace,
            LatticeResult latticeResult,
            DistributivityResult distributivity,
            EnumerationResult enumeration,
            String testSource,
            CoverageResult coverage,
            List<ComplianceAction> complianceActions,
            int numStates,
            int numTransitions,
            int numValidPaths,
            int numViolations,
            boolean isWellFormed) {

        public FinancialAnalysisResult {
            complianceActions = List.copyOf(complianceActions);
        }
    }

    // -----------------------------------------------------------------------
    // FIX 4.4 protocols
    // -----------------------------------------------------------------------

    public static FinancialProtocol newOrderSingleProtocol() {
        return new FinancialProtocol(
                "NewOrderSingle",
                "FIX",
                List.of("D", "8", "F", "9"),
                List.of(
                        "newOrderSingle: Tag 35=D submission",
                        "ack: Tag 35=8 ExecutionReport OrdStatus=0",
                        "reject: Tag 35=8 ExecType=8",
                        "fill: Tag 35=8 ExecType=F OrdStatus=2",
                        "partial: Tag 35=8 ExecType=F OrdStatus=1",
                        "cancelRequest: Tag 35=F",
                        "cancelled: Tag 35=8 ExecType=4",
                        "cancelReject: Tag 35=9"),
                "&{newOrderSingle: "
                        + "+{ACK: &{cancelRequest: +{CANCELLED: end, CANCELREJECT: end}}, "
                        + "REJECT: end, "
                        + "FILL: end, "
                        + "PARTIAL: &{cancelRequest: +{CANCELLED: end, CANCELREJECT: end}}}}",
                "FIX 4.4 NewOrderSingle lifecycle: submission, exchange "
                        + "response (ack/reject/fill/partial), optional cancel "
                        + "request with confirmation or reject.");
    }

    public static FinancialProtocol executionReportProtocol() {
        return new FinancialProtocol(
                "ExecutionReport",
                "FIX",
                List.of("8"),
                List.of(
                        "report: Receive ExecutionReport",
                        "PARTIAL: Partial fill",
                        "DONE: Final fill / done-for-day",
                        "EXPIRED: Order expired"),
                "&{report: +{PARTIAL: &{report: +{DONE: end, EXPIRED: end}}, "
                        + "DONE: end, EXPIRED: end}}",
                "FIX 4.4 ExecutionReport sequence: a working order "
                        + "receives a first report that is either PARTIAL, DONE, "
                        + "or EXPIRED; if PARTIAL, a second report terminates.");
    }

    public static FinancialProtocol orderCancelProtocol() {
        return new FinancialProtocol(
                "OrderCancelRequest",
                "FIX",
                List.of("F", "8", "9"),
                List.of(
                        "cancelRequest: Tag 35=F submission",
                        "CANCELLED: ExecutionReport ExecType=4",
                        "CANCELREJECT: OrderCancelReject Tag 35=9"),
                "&{cancelRequest: +{CANCELLED: end, CANCELREJECT: end}}",
                "FIX 4.4 order cancel request: submit cancel, receive "
                        + "either a CANCELLED ExecutionReport or an "
                        + "OrderCancelReject.");
    }

    // -----------------------------------------------------------------------
    // ISO 20022 protocols
    // -----------------------------------------------------------------------

    public static FinancialProtocol pain001Protocol() {
        return new FinancialProtocol(
                "pain001",
                "ISO20022",
                List.of("pain.001", "pain.002"),
                List.of(
                        "initiate: Submit pain.001",
                        "validate: Bank validation",
                        "ACCEPTED: pain.002 status=ACSP",
                        "REJECTED: pain.002 status=RJCT",
                        "clear: Hand off to pacs.008 clearing",
                        "SETTLED: Settlement confirmation",
                        "RETURNED: pacs.004 payment return"),
                "&{initiate: &{validate: +{ACCEPTED: &{clear: "
                        + "+{SETTLED: end, RETURNED: end}}, REJECTED: end}}}",
                "ISO 20022 pain.001 customer credit transfer: initiate, "
                        + "bank validation (accept or reject), clearing and "
                        + "settlement (or return).");
    }

    public static FinancialProtocol pacs008Protocol() {
        return new FinancialProtocol(
                "pacs008",
                "ISO20022",
                List.of("pacs.008", "pacs.002", "pacs.004"),
                List.of(
                        "send: pacs.008 interbank credit transfer",
                        "ackClear: pacs.002 ACSC (accepted settled clearing)",
                        "rejectClear: pacs.002 RJCT (rejected)",
                        "RETURNED: pacs.004 payment return",
                        "SETTLED: Settlement finalised"),
                "&{send: +{ACKCLEAR: +{SETTLED: end, RETURNED: end}, "
                        + "REJECTCLEAR: end}}",
                "ISO 20022 pacs.008 interbank clearing: send credit "
                        + "transfer, receive pacs.002 status (accept or reject), "
                        + "finalise or return.");
    }

    public static FinancialProtocol camt054Protocol() {
        return new FinancialProtocol(
                "camt054",
                "ISO20022",
                List.of("camt.054"),
                List.of(
                        "notify: camt.054 debit/credit notification",
                        "reconcile: Customer reconciles against ledger",
                        "MATCHED: Entry matched to open item",
                        "UNMATCHED: Entry kept in suspense"),
                "&{notify: &{reconcile: +{MATCHED: end, UNMATCHED: end}}}",
                "ISO 20022 camt.054 debit/credit notification: the bank "
                        + "notifies a customer of a movement; the customer "
                        + "reconciles against its ledger.");
    }

    public static FinancialProtocol pain008Protocol() {
        return new FinancialProtocol(
                "pain008",
                "ISO20022",
                List.of("pain.008", "pain.002", "pacs.003"),
                List.of(
                        "initiateDD: pain.008 direct debit initiation",
                        "validateDD: Creditor bank validation",
                        "ACCEPTED: pain.002 ACSP",
                        "REJECTED: pain.002 RJCT",
                        "collect: pacs.003 interbank collection",
                        "SETTLED: Funds settled",
                        "RETURNED: pacs.004 return (R-transaction)"),
                "&{initiateDD: &{validateDD: +{ACCEPTED: &{collect: "
                        + "+{SETTLED: end, RETURNED: end}}, REJECTED: end}}}",
                "ISO 20022 pain.008 direct debit: initiate, validate, "
                        + "collect and either settle or return (R-transaction).");
    }

    /** All pre-defined financial messaging protocols. */
    public static final List<FinancialProtocol> ALL_FINANCIAL_PROTOCOLS = List.of(
            newOrderSingleProtocol(),
            executionReportProtocol(),
            orderCancelProtocol(),
            pain001Protocol(),
            pacs008Protocol(),
            camt054Protocol(),
            pain008Protocol());

    // -----------------------------------------------------------------------
    // Compliance label sets
    // -----------------------------------------------------------------------

    private static final Set<String> TERMINAL_SETTLE_LABELS =
            Set.of("FILL", "SETTLED", "MATCHED");

    private static final Set<String> TERMINAL_REJECT_LABELS = Set.of(
            "REJECT", "REJECTED", "REJECTCLEAR", "RJCT",
            "CANCELREJECT", "UNMATCHED", "EXPIRED");

    private static final Set<String> TERMINAL_CANCEL_LABELS =
            Set.of("CANCELLED", "RETURNED");

    // -----------------------------------------------------------------------
    // Bidirectional morphism phi / psi
    // -----------------------------------------------------------------------

    public static ComplianceAction phiStateToAction(StateSpace ss, int stateId) {
        if (stateId == ss.top()) {
            return new ComplianceAction(
                    stateId,
                    "audit",
                    "Initial state: run pre-trade / KYC / sanctions "
                            + "checks on incoming message.");
        }

        List<String> incoming = new ArrayList<>();
        boolean hasOutgoing = false;
        for (var t : ss.transitions()) {
            if (t.target() == stateId) incoming.add(t.label());
            if (t.source() == stateId) hasOutgoing = true;
        }

        boolean isTerminal = stateId == ss.bottom() || !hasOutgoing;

        if (isTerminal) {
            for (String lbl : incoming) {
                if (TERMINAL_REJECT_LABELS.contains(lbl)) {
                    return new ComplianceAction(
                            stateId,
                            "block",
                            "Terminal reject state: record reject reason, "
                                    + "notify upstream, no settlement booking.");
                }
            }
            for (String lbl : incoming) {
                if (TERMINAL_SETTLE_LABELS.contains(lbl)) {
                    return new ComplianceAction(
                            stateId,
                            "settle",
                            "Terminal settle state: finalise books, archive "
                                    + "for regulatory retention (MiFID II RTS 22, "
                                    + "ISO 20022 retention).");
                }
            }
            for (String lbl : incoming) {
                if (TERMINAL_CANCEL_LABELS.contains(lbl)) {
                    return new ComplianceAction(
                            stateId,
                            "reconcile",
                            "Terminal cancel / return state: reconcile "
                                    + "pending items and release reservation.");
                }
            }
            return new ComplianceAction(
                    stateId,
                    "archive",
                    "Terminal state: archive audit trail.");
        }

        return new ComplianceAction(
                stateId,
                "monitor",
                "Intermediate state: monitor SLA timers and surveillance "
                        + "rules until next transition.");
    }

    public static List<ComplianceAction> phi(StateSpace ss) {
        List<Integer> sorted = new ArrayList<>(new TreeSet<>(ss.states()));
        List<ComplianceAction> out = new ArrayList<>(sorted.size());
        for (int s : sorted) out.add(phiStateToAction(ss, s));
        return out;
    }

    public static Set<Integer> psi(StateSpace ss, String action) {
        Set<Integer> result = new TreeSet<>();
        for (int s : ss.states()) {
            if (phiStateToAction(ss, s).action().equals(action)) result.add(s);
        }
        return result;
    }

    public static boolean isPhiPsiGalois(StateSpace ss) {
        Set<String> actionNames = new TreeSet<>();
        for (int s : ss.states()) actionNames.add(phiStateToAction(ss, s).action());

        for (int s : ss.states()) {
            String a = phiStateToAction(ss, s).action();
            if (!psi(ss, a).contains(s)) return false;
        }

        for (String a : actionNames) {
            Set<Integer> pre = psi(ss, a);
            if (pre.isEmpty()) continue;
            for (int s : pre) {
                if (!phiStateToAction(ss, s).action().equals(a)) return false;
            }
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Core analysis
    // -----------------------------------------------------------------------

    public static SessionType financialToSessionType(FinancialProtocol protocol) {
        return Parser.parse(protocol.sessionTypeString());
    }

    public static FinancialAnalysisResult verifyFinancialProtocol(FinancialProtocol protocol) {
        return verifyFinancialProtocol(protocol, null);
    }

    public static FinancialAnalysisResult verifyFinancialProtocol(
            FinancialProtocol protocol, TestGenConfig config) {
        if (config == null) {
            config = TestGenConfig.withPackage(
                    protocol.name() + "ConformanceTest",
                    "com." + protocol.family().toLowerCase(Locale.ROOT) + ".conformance");
        }

        SessionType ast = financialToSessionType(protocol);
        StateSpace ss = StateSpaceBuilder.build(ast);

        LatticeResult lr = LatticeChecker.checkLattice(ss);
        DistributivityResult dist = DistributivityChecker.checkDistributive(ss);

        EnumerationResult enumResult = PathEnumerator.enumerate(ss, config);
        String testSrc = TestGenerator.generate(ss, config, protocol.sessionTypeString());
        CoverageResult cov = CoverageChecker.computeCoverage(ss, enumResult);

        List<ComplianceAction> actions = phi(ss);

        return new FinancialAnalysisResult(
                protocol,
                ast,
                ss,
                lr,
                dist,
                enumResult,
                testSrc,
                cov,
                actions,
                ss.states().size(),
                ss.transitions().size(),
                enumResult.validPaths().size(),
                enumResult.violations().size(),
                lr.isLattice());
    }

    public static List<FinancialAnalysisResult> verifyAllFinancialProtocols() {
        List<FinancialAnalysisResult> out = new ArrayList<>();
        for (FinancialProtocol p : ALL_FINANCIAL_PROTOCOLS) {
            out.add(verifyFinancialProtocol(p));
        }
        return out;
    }

    // -----------------------------------------------------------------------
    // Report formatting
    // -----------------------------------------------------------------------

    public static String formatFinancialReport(FinancialAnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        FinancialProtocol p = result.protocol();
        String sep = "=".repeat(72);

        sb.append(sep).append('\n');
        sb.append("  FINANCIAL PROTOCOL REPORT: ")
                .append(p.family()).append(" / ").append(p.name()).append('\n');
        sb.append(sep).append('\n').append('\n');
        sb.append("  ").append(p.description()).append('\n').append('\n');

        sb.append("--- Messages ---\n");
        for (String m : p.messages()) sb.append("  - ").append(m).append('\n');
        sb.append('\n');

        sb.append("--- Session Type ---\n");
        sb.append("  ").append(p.sessionTypeString()).append('\n').append('\n');

        sb.append("--- State Space ---\n");
        sb.append(String.format("  States:      %d%n", result.numStates()));
        sb.append(String.format("  Transitions: %d%n", result.numTransitions()));
        sb.append(String.format("  Top:         %d%n", result.stateSpace().top()));
        sb.append(String.format("  Bottom:      %d%n", result.stateSpace().bottom()));
        sb.append('\n');

        String latticeStr = result.latticeResult().isLattice() ? "YES" : "NO";
        String distStr = result.distributivity().isDistributive() ? "yes" : "no";
        sb.append("--- Lattice Properties ---\n");
        sb.append("  Is lattice:     ").append(latticeStr).append('\n');
        sb.append("  Has top:        ").append(result.latticeResult().hasTop()).append('\n');
        sb.append("  Has bottom:     ").append(result.latticeResult().hasBottom()).append('\n');
        sb.append("  Distributive:   ").append(distStr).append('\n').append('\n');

        sb.append("--- Compliance Actions (phi) ---\n");
        for (ComplianceAction a : result.complianceActions()) {
            sb.append(String.format("  state %3d -> %-10s  %s%n",
                    a.stateId(), a.action(), a.rationale()));
        }
        sb.append('\n');

        sb.append("--- Conformance Tests ---\n");
        sb.append(String.format("  Valid paths:          %d%n", result.numValidPaths()));
        sb.append(String.format("  Violation points:     %d%n", result.numViolations()));
        sb.append(String.format(Locale.ROOT, "  Transition coverage:  %.1f%%%n",
                result.coverage().transitionCoverage() * 100.0));
        sb.append(String.format(Locale.ROOT, "  State coverage:       %.1f%%%n",
                result.coverage().stateCoverage() * 100.0));
        sb.append('\n');

        sb.append(sep).append('\n');
        if (result.isWellFormed()) {
            sb.append("  VERDICT: ").append(p.name()).append(" protocol is WELL-FORMED\n");
        } else {
            sb.append("  VERDICT: ").append(p.name()).append(" protocol has ISSUES\n");
        }
        sb.append(sep).append('\n');

        return sb.toString();
    }

    public static String formatFinancialSummary(List<FinancialAnalysisResult> results) {
        StringBuilder sb = new StringBuilder();
        String sep = "=".repeat(80);
        sb.append(sep).append('\n');
        sb.append("  FINANCIAL PROTOCOL VERIFICATION SUMMARY\n");
        sb.append(sep).append('\n').append('\n');

        sb.append(String.format("  %-20s %-10s %6s %6s %4s %5s %6s %6s%n",
                "Protocol", "Family", "States", "Trans", "Lat", "Dist",
                "Paths", "Viols"));
        sb.append("  ").append("-".repeat(70)).append('\n');

        for (FinancialAnalysisResult r : results) {
            String latt = r.isWellFormed() ? "YES" : "NO";
            String dist = r.distributivity().isDistributive() ? "yes" : "no";
            sb.append(String.format("  %-20s %-10s %6d %6d %4s %5s %6d %6d%n",
                    r.protocol().name(), r.protocol().family(),
                    r.numStates(), r.numTransitions(),
                    latt, dist,
                    r.numValidPaths(), r.numViolations()));
        }

        sb.append('\n');
        int total = results.size();
        long nLat = results.stream().filter(FinancialAnalysisResult::isWellFormed).count();
        sb.append(String.format("  %d/%d protocols form lattices.%n", nLat, total));
        sb.append(sep).append('\n');

        return sb.toString();
    }
}
