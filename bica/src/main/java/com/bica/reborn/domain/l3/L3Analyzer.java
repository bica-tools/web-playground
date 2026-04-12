package com.bica.reborn.domain.l3;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.lattice.DistributivityChecker;
import com.bica.reborn.lattice.DistributivityResult;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.lattice.Valuations;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Analyzes session type protocols at different fidelity levels (L1, L2, L3).
 *
 * <p>Ported from {@code reticulate/reticulate/l3_protocols.py}.
 *
 * <p>Scientific question (Step 97d): Does L3 (correct) modeling give
 * predominantly distributive lattices, matching the benchmark hypothesis?
 */
public final class L3Analyzer {

    private L3Analyzer() {}

    /**
     * Analyze a single protocol string at the given fidelity level.
     *
     * @param name    Protocol name.
     * @param stStr   Session type string.
     * @param level   Fidelity level label ("L1", "L2", or "L3").
     * @return Analysis result.
     */
    public static L3Result analyze(String name, String stStr, String level) {
        SessionType ast = Parser.parse(stStr);
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);

        if (!lr.isLattice()) {
            return new L3Result(name, level, stStr,
                    ss.states().size(), ss.transitions().size(),
                    false, false, false, false, false,
                    "not_lattice", false, false);
        }

        DistributivityResult dr = DistributivityChecker.checkDistributive(ss);
        Valuations.ValuationAnalysis va = Valuations.analyzeValuations(ss);

        return new L3Result(name, level, stStr,
                ss.states().size(), ss.transitions().size(),
                true, dr.isDistributive(), dr.isModular(),
                dr.hasM3(), dr.hasN5(), dr.classification(),
                va.isGraded(), va.rankIsValuation());
    }

    /**
     * Analyze all protocols in the catalog at L3 fidelity.
     *
     * @return List of L3 analysis results.
     */
    public static List<L3Result> analyzeAllL3() {
        List<L3Result> results = new ArrayList<>();
        for (var entry : L3Protocols.CATALOG.entrySet()) {
            results.add(analyze(entry.getKey(), entry.getValue().l3(), "L3"));
        }
        return results;
    }

    /**
     * Analyze all protocols at both L1 and L3 fidelity, for comparison.
     *
     * @return List of results, two per protocol (L1 then L3).
     */
    public static List<L3Result> analyzeAllLevels() {
        List<L3Result> results = new ArrayList<>();
        for (var entry : L3Protocols.CATALOG.entrySet()) {
            results.add(analyze(entry.getKey(), entry.getValue().l1(), "L1"));
            results.add(analyze(entry.getKey(), entry.getValue().l3(), "L3"));
        }
        return results;
    }

    /**
     * Format a report of L3 analysis results.
     *
     * @param results Analysis results to report.
     * @return Formatted multi-line report string.
     */
    public static String formatReport(List<L3Result> results) {
        var sb = new StringBuilder();
        sb.append("=".repeat(90)).append('\n');
        sb.append("  L3 PROTOCOL FIDELITY ANALYSIS\n");
        sb.append("=".repeat(90)).append('\n');

        int total = results.size();
        long lattice = results.stream().filter(L3Result::isLattice).count();
        long dist = results.stream().filter(L3Result::isDistributive).count();
        long mod = results.stream().filter(L3Result::isModular).count();

        sb.append(String.format("  Protocols: %d%n", total));
        sb.append(String.format("  Lattice: %d/%d (%.0f%%)%n", lattice, total, 100.0 * lattice / total));
        sb.append(String.format("  Distributive: %d/%d (%.0f%%)%n", dist, total, 100.0 * dist / total));
        sb.append(String.format("  Modular: %d/%d (%.0f%%)%n", mod, total, 100.0 * mod / total));
        sb.append('\n');

        sb.append(String.format("  %-26s %-4s %3s %3s %-14s %3s %3s %7s %6s%n",
                "Protocol", "Lv", "St", "Tr", "Class", "M3", "N5", "Graded", "RkVal"));
        sb.append("  ").append("-".repeat(78)).append('\n');

        for (L3Result r : results) {
            String m3 = r.hasM3() ? "Y" : "-";
            String n5 = r.hasN5() ? "Y" : "-";
            String g = r.isGraded() ? "YES" : "NO";
            String rv = r.rankIsValuation() ? "YES" : "NO";
            sb.append(String.format("  %-26s %-4s %3d %3d %-14s %3s %3s %7s %6s%n",
                    r.name(), r.level(), r.numStates(), r.numTransitions(),
                    r.classification(), m3, n5, g, rv));
        }

        sb.append("=".repeat(90)).append('\n');
        return sb.toString();
    }

    /**
     * Format a side-by-side L1 vs L3 comparison report.
     *
     * @param results Results from {@link #analyzeAllLevels()}.
     * @return Formatted comparison report.
     */
    public static String formatComparison(List<L3Result> results) {
        var sb = new StringBuilder();
        sb.append("=".repeat(90)).append('\n');
        sb.append("  L1 vs L3 FIDELITY COMPARISON\n");
        sb.append("=".repeat(90)).append('\n');

        // Group by protocol name
        var byName = new java.util.LinkedHashMap<String, Map<String, L3Result>>();
        for (L3Result r : results) {
            byName.computeIfAbsent(r.name(), k -> new java.util.LinkedHashMap<>())
                    .put(r.level(), r);
        }

        sb.append(String.format("  %-24s %-14s %-14s %-20s%n",
                "Protocol", "L1 Class", "L3 Class", "L1→L3 Change"));
        sb.append("  ").append("-".repeat(70)).append('\n');

        long l1Dist = 0, l3Dist = 0, l1Mod = 0, l3Mod = 0, l1Count = 0, l3Count = 0;

        for (var entry : byName.entrySet()) {
            L3Result l1 = entry.getValue().get("L1");
            L3Result l3 = entry.getValue().get("L3");
            if (l1 != null && l3 != null) {
                String change = l1.classification().equals(l3.classification())
                        ? "same"
                        : l1.classification() + " → " + l3.classification();
                sb.append(String.format("  %-24s %-14s %-14s %-20s%n",
                        entry.getKey(), l1.classification(), l3.classification(), change));
                if (l1.isDistributive()) l1Dist++;
                if (l3.isDistributive()) l3Dist++;
                if (l1.isModular()) l1Mod++;
                if (l3.isModular()) l3Mod++;
                l1Count++;
                l3Count++;
            }
        }

        sb.append('\n');
        sb.append(String.format("  L1: %d/%d distributive, %d/%d modular%n", l1Dist, l1Count, l1Mod, l1Count));
        sb.append(String.format("  L3: %d/%d distributive, %d/%d modular%n", l3Dist, l3Count, l3Mod, l3Count));
        sb.append("=".repeat(90)).append('\n');
        return sb.toString();
    }
}
