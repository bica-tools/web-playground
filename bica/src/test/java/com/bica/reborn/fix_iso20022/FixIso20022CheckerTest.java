package com.bica.reborn.fix_iso20022;

import com.bica.reborn.fix_iso20022.FixIso20022Checker.ComplianceAction;
import com.bica.reborn.fix_iso20022.FixIso20022Checker.FinancialAnalysisResult;
import com.bica.reborn.fix_iso20022.FixIso20022Checker.FinancialProtocol;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FIX 4.4 / ISO 20022 financial protocol verification (Step 73).
 * Java port mirroring {@code reticulate/tests/test_fix_iso20022.py}.
 */
class FixIso20022CheckerTest {

    private static final List<Supplier<FinancialProtocol>> FIX_PROTOCOLS = List.of(
            FixIso20022Checker::newOrderSingleProtocol,
            FixIso20022Checker::executionReportProtocol,
            FixIso20022Checker::orderCancelProtocol);

    private static final List<Supplier<FinancialProtocol>> ISO_PROTOCOLS = List.of(
            FixIso20022Checker::pain001Protocol,
            FixIso20022Checker::pacs008Protocol,
            FixIso20022Checker::camt054Protocol,
            FixIso20022Checker::pain008Protocol);

    private static final List<Supplier<FinancialProtocol>> ALL_CTORS =
            Stream.concat(FIX_PROTOCOLS.stream(), ISO_PROTOCOLS.stream()).toList();

    private static StateSpace ssOf(Supplier<FinancialProtocol> ctor) {
        return StateSpaceBuilder.build(
                FixIso20022Checker.financialToSessionType(ctor.get()));
    }

    // ----- Protocol construction --------------------------------------------

    @Test
    void allCtorsProduceProtocols() {
        for (var ctor : ALL_CTORS) {
            var p = ctor.get();
            assertNotNull(p);
            assertFalse(p.name().isEmpty());
            assertTrue(Set.of("FIX", "ISO20022").contains(p.family()));
            assertFalse(p.sessionTypeString().isEmpty());
            assertFalse(p.description().isEmpty());
            assertFalse(p.messages().isEmpty());
            assertFalse(p.transitions().isEmpty());
        }
    }

    @Test
    void fixFamily() {
        for (var ctor : FIX_PROTOCOLS) assertEquals("FIX", ctor.get().family());
    }

    @Test
    void isoFamily() {
        for (var ctor : ISO_PROTOCOLS) assertEquals("ISO20022", ctor.get().family());
    }

    @Test
    void registryLength() {
        assertEquals(7, FixIso20022Checker.ALL_FINANCIAL_PROTOCOLS.size());
    }

    @Test
    void registryHasAllProtocols() {
        Set<String> names = new HashSet<>();
        for (var p : FixIso20022Checker.ALL_FINANCIAL_PROTOCOLS) names.add(p.name());
        assertTrue(names.contains("NewOrderSingle"));
        assertTrue(names.contains("pain001"));
        assertTrue(names.contains("pacs008"));
        assertTrue(names.contains("camt054"));
    }

    // ----- Parsing + state space --------------------------------------------

    @Test
    void allParse() {
        for (var ctor : ALL_CTORS) {
            assertNotNull(FixIso20022Checker.financialToSessionType(ctor.get()));
        }
    }

    @Test
    void allStateSpacesNonempty() {
        for (var ctor : ALL_CTORS) {
            StateSpace ss = ssOf(ctor);
            assertTrue(ss.states().size() >= 2);
            assertTrue(ss.transitions().size() >= 1);
        }
    }

    // ----- Lattice properties -----------------------------------------------

    @Test
    void allAreLattices() {
        for (var ctor : ALL_CTORS) {
            StateSpace ss = ssOf(ctor);
            LatticeResult lr = LatticeChecker.checkLattice(ss);
            assertTrue(lr.isLattice(),
                    "protocol does not form a lattice: " + lr.counterexample());
        }
    }

    @Test
    void allHaveTopAndBottom() {
        for (var ctor : ALL_CTORS) {
            LatticeResult lr = LatticeChecker.checkLattice(ssOf(ctor));
            assertTrue(lr.hasTop());
            assertTrue(lr.hasBottom());
        }
    }

    // ----- Morphisms --------------------------------------------------------

    private static final Set<String> ALLOWED_ACTIONS = Set.of(
            "audit", "monitor", "block", "settle", "reconcile", "archive");

    @Test
    void phiReturnsActionPerState() {
        for (var ctor : ALL_CTORS) {
            StateSpace ss = ssOf(ctor);
            List<ComplianceAction> actions = FixIso20022Checker.phi(ss);
            assertEquals(ss.states().size(), actions.size());
            for (var a : actions) {
                assertTrue(ALLOWED_ACTIONS.contains(a.action()), "bad action: " + a.action());
            }
        }
    }

    @Test
    void topIsAudit() {
        for (var ctor : ALL_CTORS) {
            StateSpace ss = ssOf(ctor);
            assertEquals("audit",
                    FixIso20022Checker.phiStateToAction(ss, ss.top()).action());
        }
    }

    @Test
    void psiReturnsPreimage() {
        for (var ctor : ALL_CTORS) {
            StateSpace ss = ssOf(ctor);
            for (int s : ss.states()) {
                String a = FixIso20022Checker.phiStateToAction(ss, s).action();
                assertTrue(FixIso20022Checker.psi(ss, a).contains(s));
            }
        }
    }

    @Test
    void galoisConnection() {
        for (var ctor : ALL_CTORS) {
            assertTrue(FixIso20022Checker.isPhiPsiGalois(ssOf(ctor)));
        }
    }

    @Test
    void newOrderSingleHasTerminalClassification() {
        StateSpace ss = ssOf(FixIso20022Checker::newOrderSingleProtocol);
        assertEquals("block",
                FixIso20022Checker.phiStateToAction(ss, ss.bottom()).action());
    }

    @Test
    void newOrderSingleHasBlock() {
        StateSpace ss = ssOf(FixIso20022Checker::newOrderSingleProtocol);
        assertTrue(FixIso20022Checker.psi(ss, "block").size() >= 1);
    }

    @Test
    void pacs008TerminalBlock() {
        StateSpace ss = ssOf(FixIso20022Checker::pacs008Protocol);
        assertEquals("block",
                FixIso20022Checker.phiStateToAction(ss, ss.bottom()).action());
    }

    @Test
    void pain001BlockOnRejected() {
        StateSpace ss = ssOf(FixIso20022Checker::pain001Protocol);
        assertTrue(FixIso20022Checker.psi(ss, "block").size() >= 1);
    }

    @Test
    void camt054BlockOnUnmatched() {
        StateSpace ss = ssOf(FixIso20022Checker::camt054Protocol);
        assertTrue(FixIso20022Checker.psi(ss, "block").size() >= 1);
    }

    // ----- Verify pipeline --------------------------------------------------

    @Test
    void verifyReturnsResult() {
        for (var ctor : ALL_CTORS) {
            FinancialAnalysisResult r = FixIso20022Checker.verifyFinancialProtocol(ctor.get());
            assertNotNull(r);
            assertTrue(r.isWellFormed());
            assertTrue(r.numStates() >= 2);
            assertTrue(r.numTransitions() >= 1);
            assertTrue(r.numValidPaths() >= 1);
            assertFalse(r.testSource().isEmpty());
            assertFalse(r.complianceActions().isEmpty());
        }
    }

    @Test
    void verifyAll() {
        List<FinancialAnalysisResult> rs = FixIso20022Checker.verifyAllFinancialProtocols();
        assertEquals(7, rs.size());
        for (var r : rs) assertTrue(r.isWellFormed());
    }

    @Test
    void formatReport() {
        var r = FixIso20022Checker.verifyFinancialProtocol(
                FixIso20022Checker.newOrderSingleProtocol());
        String out = FixIso20022Checker.formatFinancialReport(r);
        assertTrue(out.contains("NewOrderSingle"));
        assertTrue(out.contains("VERDICT"));
        assertTrue(out.contains("WELL-FORMED"));
        assertTrue(out.contains("phi"));
    }

    @Test
    void formatSummary() {
        var rs = FixIso20022Checker.verifyAllFinancialProtocols();
        String out = FixIso20022Checker.formatFinancialSummary(rs);
        assertTrue(out.contains("NewOrderSingle"));
        assertTrue(out.contains("pain001"));
        assertTrue(out.contains("7/7"));
    }

    @Test
    void distributivityRecorded() {
        var r = FixIso20022Checker.verifyFinancialProtocol(
                FixIso20022Checker.pacs008Protocol());
        assertTrue(r.distributivity().isDistributive());
    }
}
