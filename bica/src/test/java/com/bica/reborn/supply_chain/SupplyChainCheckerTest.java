package com.bica.reborn.supply_chain;

import com.bica.reborn.supply_chain.SupplyChainChecker.RoleEdge;
import com.bica.reborn.supply_chain.SupplyChainChecker.SupplyChainAnalysis;
import com.bica.reborn.supply_chain.SupplyChainChecker.SupplyChainScenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 mirror of {@code reticulate/tests/test_supply_chain_module.py}.
 */
class SupplyChainCheckerTest {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    @Test
    void fiveCanonicalRoles() {
        assertEquals(
                List.of("Supplier", "Manufacturer", "Distributor", "Retailer", "Customer"),
                SupplyChainChecker.SUPPLY_CHAIN_ROLES);
    }

    @Test
    void sixPhases() {
        assertEquals(6, SupplyChainChecker.SUPPLY_CHAIN_PHASES.size());
        assertTrue(SupplyChainChecker.SUPPLY_CHAIN_PHASES.contains("Sourcing"));
        assertTrue(SupplyChainChecker.SUPPLY_CHAIN_PHASES.contains("Aftermarket"));
    }

    @Test
    void allScenariosDefined() {
        List<SupplyChainScenario> all = SupplyChainChecker.allScenarios();
        assertEquals(5, all.size());
        Set<String> names = new java.util.HashSet<>();
        for (SupplyChainScenario s : all) names.add(s.name());
        assertEquals(
                Set.of("OrderFulfilment", "Return", "Payment",
                        "QualityInspection", "Replenishment"),
                names);
    }

    // -----------------------------------------------------------------------
    // Scenario shape
    // -----------------------------------------------------------------------

    @Test
    void orderFulfilmentHasFiveRoles() {
        SupplyChainScenario s = SupplyChainChecker.orderFulfilmentScenario();
        assertEquals(
                Set.copyOf(SupplyChainChecker.SUPPLY_CHAIN_ROLES),
                s.expectedRoles());
        assertTrue(s.globalTypeString().contains("placeOrder"));
    }

    @Test
    void returnScenarioHasReturnsDesk() {
        SupplyChainScenario s = SupplyChainChecker.returnScenario();
        assertTrue(s.expectedRoles().contains("ReturnsDesk"));
    }

    @Test
    void paymentScenarioHasBank() {
        SupplyChainScenario s = SupplyChainChecker.paymentScenario();
        assertTrue(s.expectedRoles().contains("Bank"));
        assertTrue(s.expectedRoles().contains("PaymentGW"));
    }

    @Test
    void qualityScenarioHasInspector() {
        SupplyChainScenario s = SupplyChainChecker.qualityScenario();
        assertTrue(s.expectedRoles().contains("QualityInspector"));
    }

    @Test
    void replenishmentIsRecursive() {
        SupplyChainScenario s = SupplyChainChecker.replenishmentScenario();
        assertTrue(s.globalTypeString().contains("rec"));
    }

    @Test
    void complianceNotesPresent() {
        for (SupplyChainScenario s : SupplyChainChecker.allScenarios()) {
            assertTrue(s.complianceNotes().size() >= 2,
                    "scenario " + s.name() + " must have >=2 compliance notes");
        }
    }

    // -----------------------------------------------------------------------
    // Analysis pipeline
    // -----------------------------------------------------------------------

    static Stream<SupplyChainScenario> scenarios() {
        return SupplyChainChecker.allScenarios().stream();
    }

    @ParameterizedTest
    @MethodSource("scenarios")
    void analyseEveryScenario(SupplyChainScenario scenario) {
        SupplyChainAnalysis a = SupplyChainChecker.analyseScenario(scenario);
        assertTrue(a.numStates() > 0);
        assertTrue(a.numTransitions() > 0);
        assertTrue(a.numRoles() >= 2);
    }

    @Test
    void orderFulfilmentIsLattice() {
        SupplyChainAnalysis a = SupplyChainChecker.analyseScenario(
                SupplyChainChecker.orderFulfilmentScenario());
        assertTrue(a.isWellFormed());
        assertEquals(5, a.numRoles());
    }

    @Test
    void paymentScenarioTransitions() {
        SupplyChainAnalysis a = SupplyChainChecker.analyseScenario(
                SupplyChainChecker.paymentScenario());
        assertTrue(a.numTransitions() >= 6);
    }

    @Test
    void qualityScenarioHasAcceptAndReject() {
        SupplyChainAnalysis a = SupplyChainChecker.analyseScenario(
                SupplyChainChecker.qualityScenario());
        boolean hasReject = false;
        boolean hasAccept = false;
        for (var t : a.stateSpace().transitions()) {
            if (t.label().contains("reject")) hasReject = true;
            if (t.label().contains("accept")) hasAccept = true;
        }
        assertTrue(hasReject);
        assertTrue(hasAccept);
    }

    @Test
    void replenishmentHasFiniteStateSpace() {
        SupplyChainAnalysis a = SupplyChainChecker.analyseScenario(
                SupplyChainChecker.replenishmentScenario());
        assertTrue(a.numStates() > 0);
        assertTrue(a.numTransitions() > 0);
    }

    @Test
    void orderFulfilmentProjectionsCoverAllRoles() {
        SupplyChainAnalysis a = SupplyChainChecker.analyseScenario(
                SupplyChainChecker.orderFulfilmentScenario());
        for (String role : SupplyChainChecker.SUPPLY_CHAIN_ROLES) {
            assertTrue(a.projections().containsKey(role),
                    "missing projection for " + role);
        }
    }

    @Test
    void analyseAllReturnsFive() {
        assertEquals(5, SupplyChainChecker.analyseAll().size());
    }

    // -----------------------------------------------------------------------
    // Bidirectional morphisms phi, psi
    // -----------------------------------------------------------------------

    @Test
    void phaseOfEdgeKnown() {
        assertEquals("Sourcing",
                SupplyChainChecker.phaseOfEdge("Supplier", "Manufacturer"));
        assertEquals("Retail",
                SupplyChainChecker.phaseOfEdge("Retailer", "Customer"));
        assertEquals("Aftermarket",
                SupplyChainChecker.phaseOfEdge("Customer", "ReturnsDesk"));
    }

    @Test
    void phiEdgeParsesLabel() {
        assertEquals("Sourcing",
                SupplyChainChecker.phiEdge("Supplier->Manufacturer:deliverMaterial"));
        assertEquals("Retail",
                SupplyChainChecker.phiEdge("Retailer->Customer:deliverOrder"));
    }

    @Test
    void phiEdgeUnknownFallsBackToValidPhase() {
        String phase = SupplyChainChecker.phiEdge("Foo->Bar:baz");
        assertTrue(SupplyChainChecker.SUPPLY_CHAIN_PHASES.contains(phase));
    }

    @Test
    void psiPhaseInvertsPhi() {
        Set<RoleEdge> retail = SupplyChainChecker.psiPhase("Retail");
        assertTrue(retail.contains(new RoleEdge("Retailer", "Customer")));
        Set<RoleEdge> sourcing = SupplyChainChecker.psiPhase("Sourcing");
        assertTrue(sourcing.contains(new RoleEdge("Supplier", "Manufacturer")));
    }

    @Test
    void roundTripCoverageIsOneForOrderFulfilment() {
        SupplyChainAnalysis a = SupplyChainChecker.analyseScenario(
                SupplyChainChecker.orderFulfilmentScenario());
        assertEquals(1.0, SupplyChainChecker.roundTripCoverage(a.stateSpace()), 1e-9);
    }

    @ParameterizedTest
    @MethodSource("scenarios")
    void roundTripCoverageIsOneForAllScenarios(SupplyChainScenario scenario) {
        SupplyChainAnalysis a = SupplyChainChecker.analyseScenario(scenario);
        assertEquals(1.0, SupplyChainChecker.roundTripCoverage(a.stateSpace()), 1e-9);
    }

    @Test
    void phaseHistogramPositive() {
        SupplyChainAnalysis a = SupplyChainChecker.analyseScenario(
                SupplyChainChecker.orderFulfilmentScenario());
        int total = 0;
        for (int v : a.phaseHistogram().values()) total += v;
        assertTrue(total > 0);
    }

    @Test
    void classifyMorphismPair() {
        assertEquals("galois-insertion", SupplyChainChecker.classifyMorphismPair());
    }

    // -----------------------------------------------------------------------
    // Reporting
    // -----------------------------------------------------------------------

    @Test
    void formatAnalysisContainsNameAndLattice() {
        SupplyChainAnalysis a = SupplyChainChecker.analyseScenario(
                SupplyChainChecker.orderFulfilmentScenario());
        String text = SupplyChainChecker.formatAnalysis(a);
        assertNotNull(text);
        assertTrue(text.contains("OrderFulfilment"));
        assertTrue(text.contains("Lattice"));
    }

    @Test
    void formatSummaryListsAllScenarios() {
        List<SupplyChainAnalysis> results = SupplyChainChecker.analyseAll();
        String text = SupplyChainChecker.formatSummary(results);
        for (SupplyChainScenario s : SupplyChainChecker.allScenarios()) {
            assertTrue(text.contains(s.name()),
                    "summary missing " + s.name());
        }
    }
}
