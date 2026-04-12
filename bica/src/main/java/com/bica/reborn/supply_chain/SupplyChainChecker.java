package com.bica.reborn.supply_chain;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.globaltype.GlobalType;
import com.bica.reborn.globaltype.GlobalTypeChecker;
import com.bica.reborn.globaltype.GlobalTypeParser;
import com.bica.reborn.globaltype.Projection;
import com.bica.reborn.globaltype.ProjectionError;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.statespace.StateSpace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Supply-chain protocols as multiparty session types (Step 67) -- Java port
 * of {@code reticulate/reticulate/supply_chain.py}.
 *
 * <p>Models a five-tier supply chain (Supplier, Manufacturer, Distributor,
 * Retailer, Customer) plus auxiliary roles (Shipper, PaymentGW, Bank,
 * ReturnsDesk, QualityInspector) as multiparty global types and analyses
 * each scenario with BICA's lattice / projection machinery.
 *
 * <p>Five canonical scenarios:
 * <ol>
 *   <li>Order fulfilment (full happy path)</li>
 *   <li>Reverse-logistics return</li>
 *   <li>Payment clearing through gateway and bank</li>
 *   <li>Quality inspection with accept/reject branch</li>
 *   <li>Recursive replenishment loop</li>
 * </ol>
 *
 * <p>For each scenario the checker builds the global state space, runs the
 * lattice check, projects onto every role, and computes a bidirectional
 * morphism pair (phi: edges -&gt; phases, psi: phases -&gt; edges).
 */
public final class SupplyChainChecker {

    private SupplyChainChecker() {}

    // -----------------------------------------------------------------------
    // Roles and phases
    // -----------------------------------------------------------------------

    /** Canonical five-tier role ordering. */
    public static final List<String> SUPPLY_CHAIN_ROLES = List.of(
            "Supplier",
            "Manufacturer",
            "Distributor",
            "Retailer",
            "Customer");

    /** Abstract phases of a supply-chain session -- the codomain of phi. */
    public static final List<String> SUPPLY_CHAIN_PHASES = List.of(
            "Sourcing",
            "Production",
            "Distribution",
            "Retail",
            "Delivery",
            "Aftermarket");

    /** Map (sender, receiver) role-edge to its abstract phase. */
    private static final Map<RoleEdge, String> PHASE_OF_EDGE;

    static {
        Map<RoleEdge, String> m = new LinkedHashMap<>();
        m.put(new RoleEdge("Supplier", "Manufacturer"), "Sourcing");
        m.put(new RoleEdge("Manufacturer", "Supplier"), "Sourcing");
        m.put(new RoleEdge("Manufacturer", "Distributor"), "Production");
        m.put(new RoleEdge("Distributor", "Manufacturer"), "Production");
        m.put(new RoleEdge("Distributor", "Retailer"), "Distribution");
        m.put(new RoleEdge("Retailer", "Distributor"), "Distribution");
        m.put(new RoleEdge("Retailer", "Customer"), "Retail");
        m.put(new RoleEdge("Customer", "Retailer"), "Retail");
        m.put(new RoleEdge("Shipper", "Customer"), "Delivery");
        m.put(new RoleEdge("Customer", "Shipper"), "Delivery");
        m.put(new RoleEdge("Retailer", "Shipper"), "Delivery");
        m.put(new RoleEdge("Shipper", "Retailer"), "Delivery");
        m.put(new RoleEdge("Customer", "PaymentGW"), "Retail");
        m.put(new RoleEdge("PaymentGW", "Customer"), "Retail");
        m.put(new RoleEdge("Retailer", "PaymentGW"), "Retail");
        m.put(new RoleEdge("PaymentGW", "Retailer"), "Retail");
        m.put(new RoleEdge("PaymentGW", "Bank"), "Retail");
        m.put(new RoleEdge("Bank", "PaymentGW"), "Retail");
        m.put(new RoleEdge("Customer", "ReturnsDesk"), "Aftermarket");
        m.put(new RoleEdge("ReturnsDesk", "Customer"), "Aftermarket");
        m.put(new RoleEdge("ReturnsDesk", "Retailer"), "Aftermarket");
        m.put(new RoleEdge("Retailer", "ReturnsDesk"), "Aftermarket");
        m.put(new RoleEdge("Manufacturer", "QualityInspector"), "Production");
        m.put(new RoleEdge("QualityInspector", "Manufacturer"), "Production");
        m.put(new RoleEdge("QualityInspector", "Distributor"), "Production");
        PHASE_OF_EDGE = Map.copyOf(m);
    }

    /** Sender / receiver pair used as a phase-table key. */
    public record RoleEdge(String sender, String receiver) {
        public RoleEdge {
            Objects.requireNonNull(sender, "sender must not be null");
            Objects.requireNonNull(receiver, "receiver must not be null");
        }
    }

    /** Abstract phase for a role-to-role interaction edge. */
    public static String phaseOfEdge(String sender, String receiver) {
        RoleEdge key = new RoleEdge(sender, receiver);
        String direct = PHASE_OF_EDGE.get(key);
        if (direct != null) return direct;
        // Fallback heuristic: phase of any known edge that mentions either role.
        for (Map.Entry<RoleEdge, String> e : PHASE_OF_EDGE.entrySet()) {
            RoleEdge k = e.getKey();
            if (k.sender().equals(sender) || k.receiver().equals(sender)
                    || k.sender().equals(receiver) || k.receiver().equals(receiver)) {
                return e.getValue();
            }
        }
        return "Sourcing";
    }

    // -----------------------------------------------------------------------
    // Records
    // -----------------------------------------------------------------------

    /** A named supply-chain scenario expressed as a multiparty global type. */
    public record SupplyChainScenario(
            String name,
            String description,
            String globalTypeString,
            Set<String> expectedRoles,
            List<String> complianceNotes) {

        public SupplyChainScenario {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(description, "description must not be null");
            Objects.requireNonNull(globalTypeString, "globalTypeString must not be null");
            Objects.requireNonNull(expectedRoles, "expectedRoles must not be null");
            Objects.requireNonNull(complianceNotes, "complianceNotes must not be null");
            expectedRoles = Set.copyOf(expectedRoles);
            complianceNotes = List.copyOf(complianceNotes);
        }
    }

    /** Complete analysis of a supply-chain scenario. */
    public record SupplyChainAnalysis(
            SupplyChainScenario scenario,
            GlobalType globalType,
            StateSpace stateSpace,
            LatticeResult latticeResult,
            Map<String, SessionType> projections,
            int numStates,
            int numTransitions,
            int numRoles,
            Map<String, Integer> phaseHistogram,
            boolean isWellFormed) {

        public SupplyChainAnalysis {
            Objects.requireNonNull(scenario, "scenario must not be null");
            Objects.requireNonNull(globalType, "globalType must not be null");
            Objects.requireNonNull(stateSpace, "stateSpace must not be null");
            Objects.requireNonNull(latticeResult, "latticeResult must not be null");
            Objects.requireNonNull(projections, "projections must not be null");
            Objects.requireNonNull(phaseHistogram, "phaseHistogram must not be null");
            projections = Map.copyOf(projections);
            phaseHistogram = Map.copyOf(phaseHistogram);
        }
    }

    // -----------------------------------------------------------------------
    // Scenario factories
    // -----------------------------------------------------------------------

    public static SupplyChainScenario orderFulfilmentScenario() {
        return new SupplyChainScenario(
                "OrderFulfilment",
                "Customer places order with Retailer; Retailer pulls stock "
                        + "from Distributor; Distributor requests a production run "
                        + "from Manufacturer; Manufacturer sources raw material from "
                        + "Supplier; goods propagate back down the chain until "
                        + "Customer receives the product.",
                "Customer -> Retailer : {placeOrder: "
                        + "Retailer -> Distributor : {requestStock: "
                        + "Distributor -> Manufacturer : {requestProduction: "
                        + "Manufacturer -> Supplier : {requestMaterial: "
                        + "Supplier -> Manufacturer : {deliverMaterial: "
                        + "Manufacturer -> Distributor : {deliverGoods: "
                        + "Distributor -> Retailer : {shipStock: "
                        + "Retailer -> Customer : {deliverOrder: end}}}}}}}}",
                new LinkedHashSet<>(SUPPLY_CHAIN_ROLES),
                List.of(
                        "ISO 28000: each tier transition is a control point.",
                        "Monitoring: observability must capture every role hop.",
                        "Safety: ordering of requestMaterial before deliverMaterial "
                                + "is enforced by the protocol."));
    }

    public static SupplyChainScenario returnScenario() {
        return new SupplyChainScenario(
                "Return",
                "Customer initiates a return to a ReturnsDesk role; the desk "
                        + "collects the item, forwards it upstream to Retailer and "
                        + "then to Distributor. Distributor either restocks or "
                        + "escalates to Manufacturer for refurbishment.",
                "Customer -> ReturnsDesk : {requestReturn: "
                        + "ReturnsDesk -> Customer : {approved: "
                        + "Customer -> ReturnsDesk : {ship: "
                        + "ReturnsDesk -> Retailer : {forward: "
                        + "Retailer -> Distributor : {returnStock: "
                        + "Distributor -> Manufacturer : {refurbish: "
                        + "Manufacturer -> Distributor : {refurbished: end}}}}}}}",
                new LinkedHashSet<>(List.of(
                        "Customer", "ReturnsDesk", "Retailer",
                        "Distributor", "Manufacturer")),
                List.of(
                        "Consumer protection: explicit approval step audits the return.",
                        "Safety: defective items must not re-enter retail stock "
                                + "without refurbishment.",
                        "Traceability: every hop is a GS1 EPCIS event."));
    }

    public static SupplyChainScenario paymentScenario() {
        return new SupplyChainScenario(
                "Payment",
                "Customer pays Retailer through a PaymentGW; the gateway "
                        + "authorises with Bank, then confirms or declines back to "
                        + "the Retailer, who in turn notifies the Customer.",
                "Customer -> Retailer : {checkout: "
                        + "Retailer -> PaymentGW : {charge: "
                        + "PaymentGW -> Bank : {authorise: "
                        + "Bank -> PaymentGW : {approved: "
                        + "PaymentGW -> Retailer : {success: "
                        + "Retailer -> Customer : {receipt: end}}, "
                        + "declined: "
                        + "PaymentGW -> Retailer : {failure: "
                        + "Retailer -> Customer : {reject: end}}}}}}",
                new LinkedHashSet<>(List.of(
                        "Customer", "Retailer", "PaymentGW", "Bank")),
                List.of(
                        "PCI-DSS: merchant never sees raw card data.",
                        "Safety: declined path must not release goods.",
                        "Audit: every role transition is logged."));
    }

    public static SupplyChainScenario qualityScenario() {
        return new SupplyChainScenario(
                "QualityInspection",
                "Manufacturer submits a batch to an independent "
                        + "QualityInspector. If the inspector accepts, the batch "
                        + "ships to Distributor; if rejected, Manufacturer must "
                        + "rework and the cycle restarts.",
                "Manufacturer -> QualityInspector : {submit: "
                        + "QualityInspector -> Manufacturer : {accept: "
                        + "Manufacturer -> Distributor : {ship: end}, "
                        + "reject: "
                        + "Manufacturer -> Distributor : {scrap: end}}}",
                new LinkedHashSet<>(List.of(
                        "Manufacturer", "QualityInspector", "Distributor")),
                List.of(
                        "ISO 9001: quality gate is mandatory before distribution.",
                        "Safety: rejected batches cannot bypass inspector.",
                        "Testing: every production run exercises at least one "
                                + "accept and one reject path."));
    }

    public static SupplyChainScenario replenishmentScenario() {
        return new SupplyChainScenario(
                "Replenishment",
                "Manufacturer and Supplier repeatedly negotiate raw-material "
                        + "replenishment: Manufacturer issues a forecast; Supplier "
                        + "confirms and delivers, or declines (Manufacturer retries).",
                "rec X . Manufacturer -> Supplier : {forecast: "
                        + "Supplier -> Manufacturer : {confirm: "
                        + "Manufacturer -> Supplier : {purchase: "
                        + "Supplier -> Manufacturer : {deliver: end}}, "
                        + "decline: X}}",
                new LinkedHashSet<>(List.of("Manufacturer", "Supplier")),
                List.of(
                        "Just-in-time manufacturing requires bounded loop depth.",
                        "Monitoring: the recursion variable marks an SLA boundary.",
                        "Safety: every decline must be paired with a retry."));
    }

    /** All canonical scenario factories, in order. */
    public static final List<Supplier<SupplyChainScenario>> SCENARIOS = List.of(
            SupplyChainChecker::orderFulfilmentScenario,
            SupplyChainChecker::returnScenario,
            SupplyChainChecker::paymentScenario,
            SupplyChainChecker::qualityScenario,
            SupplyChainChecker::replenishmentScenario);

    /** All canonical scenarios materialised. */
    public static List<SupplyChainScenario> allScenarios() {
        List<SupplyChainScenario> out = new ArrayList<>();
        for (Supplier<SupplyChainScenario> f : SCENARIOS) {
            out.add(f.get());
        }
        return out;
    }

    // -----------------------------------------------------------------------
    // Analysis pipeline
    // -----------------------------------------------------------------------

    /** Run the full lattice / projection pipeline on a single scenario. */
    public static SupplyChainAnalysis analyseScenario(SupplyChainScenario scenario) {
        GlobalType g = GlobalTypeParser.parse(scenario.globalTypeString());
        StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
        LatticeResult lr = LatticeChecker.checkLattice(ss);

        Map<String, SessionType> projs = new TreeMap<>();
        for (String role : new TreeSet<>(GlobalTypeChecker.roles(g))) {
            try {
                projs.put(role, Projection.project(g, role));
            } catch (ProjectionError e) {
                // role not fully projectable -- skip silently, mirroring Python.
            }
        }

        Map<String, Integer> hist = phaseHistogram(ss);
        return new SupplyChainAnalysis(
                scenario,
                g,
                ss,
                lr,
                projs,
                ss.states().size(),
                ss.transitions().size(),
                GlobalTypeChecker.roles(g).size(),
                hist,
                lr.isLattice());
    }

    /** Analyse every canonical scenario. */
    public static List<SupplyChainAnalysis> analyseAll() {
        List<SupplyChainAnalysis> out = new ArrayList<>();
        for (SupplyChainScenario s : allScenarios()) {
            out.add(analyseScenario(s));
        }
        return out;
    }

    // -----------------------------------------------------------------------
    // Bidirectional morphism pair (phi, psi)
    // -----------------------------------------------------------------------

    /** Parsed components of a role-annotated transition label. */
    private record ParsedLabel(String sender, String receiver, String method) {}

    private static ParsedLabel parseLabel(String label) {
        if (!label.contains("->") || !label.contains(":")) return null;
        int arrow = label.indexOf("->");
        String left = label.substring(0, arrow);
        String rest = label.substring(arrow + 2);
        int colon = rest.indexOf(':');
        if (colon < 0) return null;
        String receiver = rest.substring(0, colon);
        String method = rest.substring(colon + 1);
        if (method.contains("{")) return null; // multi-method aggregate label
        return new ParsedLabel(left.trim(), receiver.trim(), method.trim());
    }

    /** phi: a transition label maps to its abstract phase. */
    public static String phiEdge(String label) {
        ParsedLabel parsed = parseLabel(label);
        if (parsed == null) return "Sourcing";
        return phaseOfEdge(parsed.sender(), parsed.receiver());
    }

    /** psi: a phase maps back to the set of role-edges it covers. */
    public static Set<RoleEdge> psiPhase(String phase) {
        Set<RoleEdge> out = new LinkedHashSet<>();
        for (Map.Entry<RoleEdge, String> e : PHASE_OF_EDGE.entrySet()) {
            if (e.getValue().equals(phase)) out.add(e.getKey());
        }
        return out;
    }

    /** Histogram of transitions per abstract phase -- the witness of phi. */
    public static Map<String, Integer> phaseHistogram(StateSpace ss) {
        Map<String, Integer> hist = new LinkedHashMap<>();
        for (String p : SUPPLY_CHAIN_PHASES) hist.put(p, 0);
        for (StateSpace.Transition t : ss.transitions()) {
            String phase = phiEdge(t.label());
            hist.merge(phase, 1, Integer::sum);
        }
        return hist;
    }

    /** Fraction of transitions whose edge lies in psi(phi(edge)). */
    public static double roundTripCoverage(StateSpace ss) {
        int total = 0;
        int covered = 0;
        for (StateSpace.Transition t : ss.transitions()) {
            ParsedLabel parsed = parseLabel(t.label());
            if (parsed == null) continue;
            total++;
            String phase = phaseOfEdge(parsed.sender(), parsed.receiver());
            if (psiPhase(phase).contains(new RoleEdge(parsed.sender(), parsed.receiver()))) {
                covered++;
            }
        }
        if (total == 0) return 1.0;
        return (double) covered / total;
    }

    /** Classify the (phi, psi) pair in the morphism hierarchy. */
    public static String classifyMorphismPair() {
        return "galois-insertion";
    }

    // -----------------------------------------------------------------------
    // Reporting
    // -----------------------------------------------------------------------

    /** Human-readable report for a single analysis. */
    public static String formatAnalysis(SupplyChainAnalysis analysis) {
        SupplyChainScenario s = analysis.scenario();
        List<String> lines = new ArrayList<>();
        String bar = "=".repeat(70);
        lines.add(bar);
        lines.add("  SUPPLY CHAIN SCENARIO: " + s.name());
        lines.add(bar);
        lines.add("");
        lines.add("  " + s.description());
        lines.add("");
        lines.add("  Roles:        " + new TreeSet<>(s.expectedRoles()));
        lines.add("  States:       " + analysis.numStates());
        lines.add("  Transitions:  " + analysis.numTransitions());
        lines.add("  Lattice:      " + analysis.isWellFormed());
        lines.add("");
        lines.add("  Phase histogram:");
        for (Map.Entry<String, Integer> e : analysis.phaseHistogram().entrySet()) {
            if (e.getValue() > 0) {
                lines.add(String.format(Locale.ROOT, "    %-14s %d",
                        e.getKey(), e.getValue()));
            }
        }
        lines.add("");
        lines.add("  Compliance notes:");
        for (String note : s.complianceNotes()) {
            lines.add("    - " + note);
        }
        lines.add(bar);
        return String.join("\n", lines);
    }

    /** Compact summary table for multiple analyses. */
    public static String formatSummary(List<SupplyChainAnalysis> analyses) {
        List<String> lines = new ArrayList<>();
        String bar = "=".repeat(72);
        lines.add(bar);
        lines.add("  SUPPLY CHAIN VERIFICATION SUMMARY");
        lines.add(bar);
        lines.add(String.format(Locale.ROOT,
                "  %-18s %5s %7s %6s %8s",
                "Scenario", "Roles", "States", "Trans", "Lattice"));
        lines.add("  " + "-".repeat(56));
        for (SupplyChainAnalysis a : analyses) {
            String latt = a.isWellFormed() ? "YES" : "NO";
            lines.add(String.format(Locale.ROOT,
                    "  %-18s %5d %7d %6d %8s",
                    a.scenario().name(),
                    a.numRoles(),
                    a.numStates(),
                    a.numTransitions(),
                    latt));
        }
        lines.add(bar);
        return String.join("\n", lines);
    }
}
