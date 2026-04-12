package com.bica.reborn.defi;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * DeFi cross-contract interaction verification (Step 74) -- Java port of
 * {@code reticulate/reticulate/defi.py}.
 *
 * <p>Models decentralised-finance (DeFi) protocols as binary session types and
 * analyses them with BICA's lattice machinery. Focus is on cross-contract
 * interactions: reentrancy, sandwich, flash-loan, oracle-manipulation. Five
 * canonical scenarios are provided. For each, a phi/psi morphism pair
 * classifies states by exploit pattern and produces audit actions.
 *
 * <p>Pure Java: no Web3 dependency. Everything is symbolic.
 */
public final class DefiChecker {

    private DefiChecker() {}

    // -----------------------------------------------------------------------
    // Exploit pattern taxonomy
    // -----------------------------------------------------------------------

    /** Canonical exploit-pattern labels used by the phi/psi morphism pair. */
    public static final List<String> EXPLOIT_PATTERNS = List.of(
            "Safe",
            "Reentrancy",
            "Sandwich",
            "FlashLoan",
            "OracleManip",
            "Unchecked");

    /** Human-readable description of each exploit pattern. */
    public static final Map<String, String> PATTERN_DESCRIPTIONS;

    /** Real-world losses (USD, approximate) per pattern class. */
    public static final Map<String, Double> PATTERN_LOSSES_USD;

    /** Severity ordering (higher = worse) used by phi state classification. */
    private static final Map<String, Integer> SEVERITY;

    static {
        Map<String, String> d = new LinkedHashMap<>();
        d.put("Safe", "No externally observable vulnerability at this protocol point.");
        d.put("Reentrancy", "State not yet updated before an external call; a recursive "
                + "re-entry can drain funds (DAO 2016, Cream 2021).");
        d.put("Sandwich", "Price-sensitive AMM step exposed to front/back-run MEV.");
        d.put("FlashLoan", "Inside an atomic flash-loan window where invariants may "
                + "temporarily be violated.");
        d.put("OracleManip", "Reads a price oracle whose reported value can be "
                + "manipulated within a block (bZx 2020).");
        d.put("Unchecked", "Return value of an external call is not inspected.");
        PATTERN_DESCRIPTIONS = Map.copyOf(d);

        Map<String, Double> l = new LinkedHashMap<>();
        l.put("Safe", 0.0);
        l.put("Reentrancy", 320_000_000.0);
        l.put("Sandwich", 1_380_000_000.0);
        l.put("FlashLoan", 850_000_000.0);
        l.put("OracleManip", 720_000_000.0);
        l.put("Unchecked", 30_000_000.0);
        PATTERN_LOSSES_USD = Map.copyOf(l);

        Map<String, Integer> s = new LinkedHashMap<>();
        s.put("Safe", 0);
        s.put("Unchecked", 1);
        s.put("Sandwich", 2);
        s.put("OracleManip", 3);
        s.put("FlashLoan", 4);
        s.put("Reentrancy", 5);
        SEVERITY = Map.copyOf(s);
    }

    // -----------------------------------------------------------------------
    // Records
    // -----------------------------------------------------------------------

    /** A (prefix, pattern) phi rule. */
    public record PhiRule(String prefix, String pattern) {
        public PhiRule {
            Objects.requireNonNull(prefix, "prefix must not be null");
            Objects.requireNonNull(pattern, "pattern must not be null");
        }
    }

    /** A DeFi interaction scenario as a binary session type. */
    public record DeFiScenario(
            String name,
            String description,
            String sessionTypeString,
            List<PhiRule> phiRules,
            Map<String, String> auditActions) {

        public DeFiScenario {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(description, "description must not be null");
            Objects.requireNonNull(sessionTypeString, "sessionTypeString must not be null");
            Objects.requireNonNull(phiRules, "phiRules must not be null");
            Objects.requireNonNull(auditActions, "auditActions must not be null");
            phiRules = List.copyOf(phiRules);
            auditActions = Map.copyOf(auditActions);
        }
    }

    /** Result of analysing a single scenario. */
    public record DeFiAnalysis(
            DeFiScenario scenario,
            StateSpace stateSpace,
            LatticeResult lattice,
            Map<String, Integer> patternHistogram,
            double roundtripCoverage,
            double totalLossExposureUsd,
            boolean isWellFormed) {

        public DeFiAnalysis {
            Objects.requireNonNull(scenario, "scenario must not be null");
            Objects.requireNonNull(stateSpace, "stateSpace must not be null");
            Objects.requireNonNull(lattice, "lattice must not be null");
            Objects.requireNonNull(patternHistogram, "patternHistogram must not be null");
            patternHistogram = Map.copyOf(patternHistogram);
        }
    }

    // -----------------------------------------------------------------------
    // Scenario factories
    // -----------------------------------------------------------------------

    public static DeFiScenario uniswapSwap() {
        return new DeFiScenario(
                "UniswapSwap",
                "Trader sends tokenIn to a pair contract, pair computes the "
                        + "output via the x*y=k invariant, transfers tokenOut back, "
                        + "and settles.  The amountOut calculation is the sandwich "
                        + "attack surface.",
                "&{approve: &{swap_quote: "
                        + "+{quote_ok: &{swap_execute: &{settle: end}}, "
                        + "  quote_bad: end}}}",
                List.of(
                        new PhiRule("swap_quote", "Sandwich"),
                        new PhiRule("swap_execute", "Sandwich"),
                        new PhiRule("settle", "Safe"),
                        new PhiRule("approve", "Safe"),
                        new PhiRule("quote_ok", "Safe"),
                        new PhiRule("quote_bad", "Safe")),
                Map.of(
                        "Sandwich", "Require minAmountOut slippage bound; monitor "
                                + "block-level price delta.",
                        "Safe", "Standard transfer/approval checks."));
    }

    public static DeFiScenario aaveLendBorrow() {
        return new DeFiScenario(
                "AaveLendBorrow",
                "User deposits collateral, borrows against it, accrues "
                        + "interest, repays, and finally withdraws.  The borrow and "
                        + "repay steps depend on an oracle price for health-factor "
                        + "computation.",
                "&{deposit: &{oracle_price: &{borrow: "
                        + "+{borrow_ok: &{accrue: &{oracle_price2: &{repay: "
                        + "&{withdraw: end}}}}, borrow_rejected: end}}}}",
                List.of(
                        new PhiRule("oracle_price", "OracleManip"),
                        new PhiRule("borrow", "OracleManip"),
                        new PhiRule("deposit", "Safe"),
                        new PhiRule("repay", "Safe"),
                        new PhiRule("withdraw", "Safe"),
                        new PhiRule("accrue", "Safe"),
                        new PhiRule("borrow_ok", "Safe"),
                        new PhiRule("borrow_rejected", "Safe")),
                Map.of(
                        "OracleManip", "Use time-weighted average price (TWAP) or "
                                + "Chainlink aggregator; bound single-block deviation.",
                        "Safe", "ERC-20 balance and allowance invariants."));
    }

    public static DeFiScenario flashLoan() {
        return new DeFiScenario(
                "FlashLoan",
                "Borrower receives N tokens, invokes arbitrary callback "
                        + "logic that must return N + fee before the transaction "
                        + "ends.  Every state between borrow and repay is inside the "
                        + "flash-loan window.",
                "&{flash_borrow: &{callback_enter: &{callback_body: "
                        + "&{callback_exit: &{flash_repay: end}}}}}",
                List.of(
                        new PhiRule("flash_borrow", "FlashLoan"),
                        new PhiRule("callback_enter", "FlashLoan"),
                        new PhiRule("callback_body", "FlashLoan"),
                        new PhiRule("callback_exit", "FlashLoan"),
                        new PhiRule("flash_repay", "Safe")),
                Map.of(
                        "FlashLoan", "Check global invariants AT repay time, not "
                                + "during callback; disallow reentrant flash loans "
                                + "from the same caller.",
                        "Safe", "Reconcile balances and emit event."));
    }

    public static DeFiScenario vulnerableWithdraw() {
        return new DeFiScenario(
                "VulnerableWithdraw",
                "withdraw() sends ETH to the caller BEFORE zeroing the "
                        + "internal balance, allowing a malicious fallback to "
                        + "re-enter withdraw() and drain the contract.  This is the "
                        + "canonical Checks-Effects-Interactions violation.",
                "&{check_balance: &{external_call: "
                        + "+{reenter: &{external_call2: &{update_state_late: end}}, "
                        + "  no_reenter: &{update_state_late: end}}}}",
                List.of(
                        new PhiRule("external_call", "Reentrancy"),
                        new PhiRule("reenter", "Reentrancy"),
                        new PhiRule("update_state_late", "Unchecked"),
                        new PhiRule("check_balance", "Safe"),
                        new PhiRule("no_reenter", "Safe")),
                Map.of(
                        "Reentrancy", "Enforce Checks-Effects-Interactions: move "
                                + "state update BEFORE external call; add "
                                + "ReentrancyGuard (OpenZeppelin).",
                        "Unchecked", "Verify return value of external call; revert "
                                + "on failure.",
                        "Safe", "Read-only guard."));
    }

    public static DeFiScenario priceOracleConsumer() {
        return new DeFiScenario(
                "PriceOracleConsumer",
                "A liquidation bot reads spot price from an AMM, compares "
                        + "against a threshold, and liquidates under-collateralised "
                        + "positions.  An attacker can sandwich the oracle read to "
                        + "trigger fake liquidations.",
                "&{read_oracle: &{compare_threshold: "
                        + "+{liquidate_yes: &{external_call: &{settle: end}}, "
                        + "  liquidate_no: end}}}",
                List.of(
                        new PhiRule("read_oracle", "OracleManip"),
                        new PhiRule("compare_threshold", "OracleManip"),
                        new PhiRule("liquidate_yes", "Sandwich"),
                        new PhiRule("external_call", "Reentrancy"),
                        new PhiRule("settle", "Safe"),
                        new PhiRule("liquidate_no", "Safe")),
                Map.of(
                        "OracleManip", "Use Chainlink or Uniswap v3 TWAP; reject "
                                + "readings whose block-height delta < 2.",
                        "Sandwich", "Mint MEV-protected transactions via Flashbots.",
                        "Reentrancy", "Apply ReentrancyGuard to the settle path.",
                        "Safe", "Emit event and exit."));
    }

    /** All canonical scenario factories, in order. */
    public static final List<Supplier<DeFiScenario>> SCENARIOS = List.of(
            DefiChecker::uniswapSwap,
            DefiChecker::aaveLendBorrow,
            DefiChecker::flashLoan,
            DefiChecker::vulnerableWithdraw,
            DefiChecker::priceOracleConsumer);

    // -----------------------------------------------------------------------
    // Phi / psi morphism pair
    // -----------------------------------------------------------------------

    /** Forward morphism on a transition label: prefix-rule lookup, default Safe. */
    public static String phiLabel(String label, List<PhiRule> rules) {
        for (PhiRule r : rules) {
            if (label.startsWith(r.prefix())) {
                return r.pattern();
            }
        }
        return "Safe";
    }

    /**
     * Exploit pattern at a state = worst pattern over outgoing transition labels.
     * If a state has no outgoing transitions, returns Safe.
     */
    public static String phiState(StateSpace ss, int state, List<PhiRule> rules) {
        String worst = "Safe";
        for (var t : ss.transitions()) {
            if (t.source() != state) continue;
            String pat = phiLabel(t.label(), rules);
            if (SEVERITY.get(pat) > SEVERITY.get(worst)) {
                worst = pat;
            }
        }
        return worst;
    }

    /** Reverse morphism: pattern -> set of states classified as that pattern. */
    public static Set<Integer> psiPattern(StateSpace ss, String pattern, List<PhiRule> rules) {
        Set<Integer> out = new TreeSet<>();
        for (int s : ss.states()) {
            if (phiState(ss, s, rules).equals(pattern)) {
                out.add(s);
            }
        }
        return out;
    }

    /** Fraction of states recovered by psi(phi(s)) = set containing s. */
    public static double roundtripCoverage(StateSpace ss, List<PhiRule> rules) {
        if (ss.states().isEmpty()) return 1.0;
        int recovered = 0;
        for (int s : ss.states()) {
            String pat = phiState(ss, s, rules);
            if (psiPattern(ss, pat, rules).contains(s)) {
                recovered++;
            }
        }
        return (double) recovered / ss.states().size();
    }

    /** Histogram of states per exploit pattern (all known patterns as keys). */
    public static Map<String, Integer> patternHistogram(StateSpace ss, List<PhiRule> rules) {
        Map<String, Integer> hist = new LinkedHashMap<>();
        for (String p : EXPLOIT_PATTERNS) hist.put(p, 0);
        for (int s : ss.states()) {
            String p = phiState(ss, s, rules);
            hist.merge(p, 1, Integer::sum);
        }
        return hist;
    }

    /** Classify the (phi, psi) pair in the morphism hierarchy. */
    public static String classifyMorphismPair() {
        return "Galois insertion: phi (monotone, surjective onto used patterns) "
                + "has upper adjoint psi (preimage), with phi o psi = id on the "
                + "image.  Equivalently: (phi, psi) is a retraction of the state "
                + "lattice onto the exploit-pattern lattice ordered by severity.";
    }

    // -----------------------------------------------------------------------
    // Analysis entry points
    // -----------------------------------------------------------------------

    /** Parse, build state space, check lattice, compute morphism metrics. */
    public static DeFiAnalysis analyseScenario(DeFiScenario scenario) {
        SessionType ast = Parser.parse(scenario.sessionTypeString());
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lattice = LatticeChecker.checkLattice(ss);
        Map<String, Integer> hist = patternHistogram(ss, scenario.phiRules());
        double cov = roundtripCoverage(ss, scenario.phiRules());
        double exposure = 0.0;
        for (var e : hist.entrySet()) {
            if (e.getValue() > 0) {
                exposure += PATTERN_LOSSES_USD.get(e.getKey());
            }
        }
        return new DeFiAnalysis(
                scenario, ss, lattice, hist, cov, exposure, lattice.isLattice());
    }

    /** Analyse every canonical scenario. */
    public static List<DeFiAnalysis> analyseAll() {
        List<DeFiAnalysis> out = new ArrayList<>();
        for (Supplier<DeFiScenario> f : SCENARIOS) {
            out.add(analyseScenario(f.get()));
        }
        return out;
    }

    // -----------------------------------------------------------------------
    // Exploit detection
    // -----------------------------------------------------------------------

    private static List<Integer> detect(StateSpace ss, List<PhiRule> rules, String target) {
        List<Integer> out = new ArrayList<>();
        for (int s : ss.states()) {
            if (phiState(ss, s, rules).equals(target)) out.add(s);
        }
        out.sort(Integer::compareTo);
        return out;
    }

    public static List<Integer> detectReentrancy(StateSpace ss, List<PhiRule> rules) {
        return detect(ss, rules, "Reentrancy");
    }

    public static List<Integer> detectSandwich(StateSpace ss, List<PhiRule> rules) {
        return detect(ss, rules, "Sandwich");
    }

    public static List<Integer> detectOracleManipulation(StateSpace ss, List<PhiRule> rules) {
        return detect(ss, rules, "OracleManip");
    }

    public static List<Integer> detectFlashLoanWindow(StateSpace ss, List<PhiRule> rules) {
        return detect(ss, rules, "FlashLoan");
    }

    // -----------------------------------------------------------------------
    // Reporting
    // -----------------------------------------------------------------------

    /** Human-readable report for a single scenario. */
    public static String formatAnalysis(DeFiAnalysis analysis) {
        DeFiScenario sc = analysis.scenario();
        List<String> lines = new ArrayList<>();
        lines.add("=== " + sc.name() + " ===");
        lines.add(sc.description());
        lines.add("States: " + analysis.stateSpace().states().size());
        lines.add("Transitions: " + analysis.stateSpace().transitions().size());
        lines.add("Lattice: " + analysis.lattice().isLattice());
        lines.add(String.format(Locale.ROOT,
                "Roundtrip coverage phi/psi: %.2f", analysis.roundtripCoverage()));
        lines.add(String.format(Locale.ROOT,
                "Loss exposure (USD): %,.0f", analysis.totalLossExposureUsd()));
        lines.add("Pattern histogram:");
        for (var e : analysis.patternHistogram().entrySet()) {
            if (e.getValue() > 0) {
                lines.add("  " + e.getKey() + ": " + e.getValue());
            }
        }
        lines.add("Audit actions:");
        for (var e : sc.auditActions().entrySet()) {
            lines.add("  [" + e.getKey() + "] " + e.getValue());
        }
        return String.join("\n", lines);
    }

    /** Summary table across all scenarios. */
    public static String formatSummary(List<DeFiAnalysis> analyses) {
        List<String> lines = new ArrayList<>();
        lines.add("DeFi cross-contract verification summary");
        lines.add("=".repeat(48));
        lines.add(String.format(Locale.ROOT, "%-24s%-10s%-8s%16s",
                "Scenario", "Lattice", "Cov", "USD exposure"));
        double total = 0.0;
        for (DeFiAnalysis a : analyses) {
            lines.add(String.format(Locale.ROOT, "%-24s%-10s%-8.2f%,16.0f",
                    a.scenario().name(),
                    a.isWellFormed() ? "yes" : "no",
                    a.roundtripCoverage(),
                    a.totalLossExposureUsd()));
            total += a.totalLossExposureUsd();
        }
        lines.add("-".repeat(48));
        lines.add(String.format(Locale.ROOT, "Total modelled exposure: USD %,.0f", total));
        return String.join("\n", lines);
    }
}
