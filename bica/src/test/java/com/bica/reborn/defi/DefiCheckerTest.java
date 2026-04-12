package com.bica.reborn.defi;

import com.bica.reborn.defi.DefiChecker.DeFiAnalysis;
import com.bica.reborn.defi.DefiChecker.DeFiScenario;
import com.bica.reborn.defi.DefiChecker.PhiRule;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java port of {@code reticulate/tests/test_defi.py}. Cross-validation:
 * same scenarios, same expected results.
 */
class DefiCheckerTest {

    // -----------------------------------------------------------------------
    // Scenario construction
    // -----------------------------------------------------------------------

    @Test
    void allScenariosConstruct() {
        for (var factory : DefiChecker.SCENARIOS) {
            DeFiScenario sc = factory.get();
            assertNotNull(sc);
            assertFalse(sc.name().isEmpty());
            assertFalse(sc.description().isEmpty());
            assertFalse(sc.sessionTypeString().isEmpty());
            assertFalse(sc.phiRules().isEmpty());
            assertFalse(sc.auditActions().isEmpty());
        }
    }

    @Test
    void scenarioNamesUnique() {
        Set<String> names = new HashSet<>();
        for (var f : DefiChecker.SCENARIOS) names.add(f.get().name());
        assertEquals(DefiChecker.SCENARIOS.size(), names.size());
    }

    @Test
    void uniswapSwapParses() {
        Parser.parse(DefiChecker.uniswapSwap().sessionTypeString());
    }

    @Test
    void aaveParses() {
        Parser.parse(DefiChecker.aaveLendBorrow().sessionTypeString());
    }

    @Test
    void flashLoanParses() {
        Parser.parse(DefiChecker.flashLoan().sessionTypeString());
    }

    @Test
    void vulnerableWithdrawParses() {
        Parser.parse(DefiChecker.vulnerableWithdraw().sessionTypeString());
    }

    @Test
    void priceOracleParses() {
        Parser.parse(DefiChecker.priceOracleConsumer().sessionTypeString());
    }

    // -----------------------------------------------------------------------
    // Lattice and analysis
    // -----------------------------------------------------------------------

    @Test
    void analyseScenarioReturnsAnalysis() {
        DeFiAnalysis a = DefiChecker.analyseScenario(DefiChecker.uniswapSwap());
        assertNotNull(a);
        assertFalse(a.stateSpace().states().isEmpty());
        assertNotNull(a.lattice());
    }

    @Test
    void allScenariosAreLattices() {
        for (DeFiAnalysis a : DefiChecker.analyseAll()) {
            assertTrue(a.isWellFormed(),
                    a.scenario().name() + " failed lattice check");
        }
    }

    @Test
    void analyseAllReturnsFive() {
        assertEquals(5, DefiChecker.analyseAll().size());
    }

    @Test
    void stateSpaceNonempty() {
        for (DeFiAnalysis a : DefiChecker.analyseAll()) {
            assertTrue(a.stateSpace().states().size() >= 2);
            assertTrue(a.stateSpace().transitions().size() >= 1);
        }
    }

    // -----------------------------------------------------------------------
    // phi / psi morphism pair
    // -----------------------------------------------------------------------

    @Test
    void phiLabelBasic() {
        List<PhiRule> rules = List.of(
                new PhiRule("external_call", "Reentrancy"),
                new PhiRule("settle", "Safe"));
        assertEquals("Reentrancy", DefiChecker.phiLabel("external_call", rules));
        assertEquals("Safe", DefiChecker.phiLabel("settle", rules));
        assertEquals("Safe", DefiChecker.phiLabel("unknown", rules));
    }

    @Test
    void phiLabelPrefixMatching() {
        List<PhiRule> rules = List.of(new PhiRule("external_call", "Reentrancy"));
        assertEquals("Reentrancy", DefiChecker.phiLabel("external_call2", rules));
    }

    @Test
    void phiStateWorstSeverity() {
        DeFiScenario sc = DefiChecker.vulnerableWithdraw();
        DeFiAnalysis a = DefiChecker.analyseScenario(sc);
        Set<String> patterns = new HashSet<>();
        for (int s : a.stateSpace().states()) {
            patterns.add(DefiChecker.phiState(a.stateSpace(), s, sc.phiRules()));
        }
        assertTrue(patterns.contains("Reentrancy"));
    }

    @Test
    void psiIsInverseImageOfPhi() {
        DeFiScenario sc = DefiChecker.uniswapSwap();
        DeFiAnalysis a = DefiChecker.analyseScenario(sc);
        for (String pattern : DefiChecker.EXPLOIT_PATTERNS) {
            Set<Integer> pre = DefiChecker.psiPattern(a.stateSpace(), pattern, sc.phiRules());
            for (int s : pre) {
                assertEquals(pattern, DefiChecker.phiState(a.stateSpace(), s, sc.phiRules()));
            }
        }
    }

    @Test
    void roundtripCoverageIsOne() {
        for (DeFiAnalysis a : DefiChecker.analyseAll()) {
            assertEquals(1.0, a.roundtripCoverage());
        }
    }

    @Test
    void patternHistogramSumsToStateCount() {
        for (DeFiAnalysis a : DefiChecker.analyseAll()) {
            int total = 0;
            for (int v : a.patternHistogram().values()) total += v;
            assertEquals(a.stateSpace().states().size(), total);
        }
    }

    @Test
    void histogramContainsOnlyKnownPatterns() {
        for (DeFiAnalysis a : DefiChecker.analyseAll()) {
            for (String p : a.patternHistogram().keySet()) {
                assertTrue(DefiChecker.EXPLOIT_PATTERNS.contains(p));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Exploit detection
    // -----------------------------------------------------------------------

    @Test
    void vulnerableWithdrawDetectsReentrancy() {
        DeFiScenario sc = DefiChecker.vulnerableWithdraw();
        DeFiAnalysis a = DefiChecker.analyseScenario(sc);
        assertFalse(DefiChecker.detectReentrancy(a.stateSpace(), sc.phiRules()).isEmpty());
    }

    @Test
    void uniswapDetectsSandwich() {
        DeFiScenario sc = DefiChecker.uniswapSwap();
        DeFiAnalysis a = DefiChecker.analyseScenario(sc);
        assertFalse(DefiChecker.detectSandwich(a.stateSpace(), sc.phiRules()).isEmpty());
    }

    @Test
    void aaveDetectsOracleManipulation() {
        DeFiScenario sc = DefiChecker.aaveLendBorrow();
        DeFiAnalysis a = DefiChecker.analyseScenario(sc);
        assertFalse(DefiChecker.detectOracleManipulation(a.stateSpace(), sc.phiRules()).isEmpty());
    }

    @Test
    void flashLoanDetectsFlashWindow() {
        DeFiScenario sc = DefiChecker.flashLoan();
        DeFiAnalysis a = DefiChecker.analyseScenario(sc);
        assertFalse(DefiChecker.detectFlashLoanWindow(a.stateSpace(), sc.phiRules()).isEmpty());
    }

    @Test
    void safeScenariosHaveNoReentrancy() {
        DeFiScenario sc = DefiChecker.uniswapSwap();
        DeFiAnalysis a = DefiChecker.analyseScenario(sc);
        assertTrue(DefiChecker.detectReentrancy(a.stateSpace(), sc.phiRules()).isEmpty());
    }

    @Test
    void priceOracleDetectsMultiplePatterns() {
        DeFiScenario sc = DefiChecker.priceOracleConsumer();
        DeFiAnalysis a = DefiChecker.analyseScenario(sc);
        Map<String, Integer> hist = a.patternHistogram();
        long nonsafe = hist.entrySet().stream()
                .filter(e -> !e.getKey().equals("Safe") && e.getValue() > 0)
                .count();
        assertTrue(nonsafe >= 2);
    }

    // -----------------------------------------------------------------------
    // Loss exposure economics
    // -----------------------------------------------------------------------

    @Test
    void lossExposurePositiveForVulnerable() {
        DeFiAnalysis a = DefiChecker.analyseScenario(DefiChecker.vulnerableWithdraw());
        assertTrue(a.totalLossExposureUsd() > 0);
    }

    @Test
    void patternLossesDefinedForAllPatterns() {
        for (String p : DefiChecker.EXPLOIT_PATTERNS) {
            assertTrue(DefiChecker.PATTERN_LOSSES_USD.containsKey(p));
            assertTrue(DefiChecker.PATTERN_LOSSES_USD.get(p) >= 0);
        }
    }

    @Test
    void totalExposureAtLeastOneBillion() {
        double total = 0.0;
        for (DeFiAnalysis a : DefiChecker.analyseAll()) {
            total += a.totalLossExposureUsd();
        }
        assertTrue(total >= 1_000_000_000.0);
    }

    // -----------------------------------------------------------------------
    // Morphism classification
    // -----------------------------------------------------------------------

    @Test
    void classifyMorphismPairMentionsGalois() {
        String s = DefiChecker.classifyMorphismPair();
        assertTrue(s.contains("Galois") || s.toLowerCase().contains("galois"));
    }

    // -----------------------------------------------------------------------
    // Reporting
    // -----------------------------------------------------------------------

    @Test
    void formatAnalysisContainsFields() {
        DeFiAnalysis a = DefiChecker.analyseScenario(DefiChecker.uniswapSwap());
        String report = DefiChecker.formatAnalysis(a);
        assertTrue(report.contains("UniswapSwap"));
        assertTrue(report.contains("Lattice"));
        assertTrue(report.contains("Pattern histogram"));
    }

    @Test
    void formatSummaryListsAllScenarios() {
        List<DeFiAnalysis> analyses = DefiChecker.analyseAll();
        String summary = DefiChecker.formatSummary(analyses);
        for (DeFiAnalysis a : analyses) {
            assertTrue(summary.contains(a.scenario().name()));
        }
        assertTrue(summary.contains("Total modelled exposure"));
    }

    // -----------------------------------------------------------------------
    // Determinism
    // -----------------------------------------------------------------------

    @Test
    void analysisDeterministic() {
        DeFiAnalysis a1 = DefiChecker.analyseScenario(DefiChecker.uniswapSwap());
        DeFiAnalysis a2 = DefiChecker.analyseScenario(DefiChecker.uniswapSwap());
        assertEquals(a1.patternHistogram(), a2.patternHistogram());
        assertEquals(a1.roundtripCoverage(), a2.roundtripCoverage());
        assertEquals(a1.totalLossExposureUsd(), a2.totalLossExposureUsd());
    }

    @Test
    void everyScenarioHasAuditActionsForUsedPatterns() {
        for (var factory : DefiChecker.SCENARIOS) {
            DeFiScenario sc = factory.get();
            DeFiAnalysis a = DefiChecker.analyseScenario(sc);
            for (var e : a.patternHistogram().entrySet()) {
                if (e.getValue() > 0 && !e.getKey().equals("Safe")) {
                    assertTrue(sc.auditActions().containsKey(e.getKey()),
                            sc.name() + " missing audit action for " + e.getKey());
                }
            }
        }
    }
}
