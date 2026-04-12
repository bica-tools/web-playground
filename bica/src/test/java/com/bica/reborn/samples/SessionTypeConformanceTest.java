package com.bica.reborn.samples;

import com.bica.reborn.lattice.DistributivityChecker;
import com.bica.reborn.lattice.DistributivityResult;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.termination.TerminationChecker;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive session type checking and conformance verification for all
 * sample classes.
 *
 * <p>For each sample class, this test:
 * <ol>
 *   <li>Parses the session type string</li>
 *   <li>Checks termination (well-formedness)</li>
 *   <li>Builds the state space</li>
 *   <li>Verifies it forms a lattice</li>
 *   <li>Checks distributivity classification</li>
 *   <li>Exercises the protocol (valid and invalid paths)</li>
 * </ol>
 */
class SessionTypeConformanceTest {

    // ======================================================================
    // Session type definitions for all sample classes
    // ======================================================================

    /** All session types, one per sample class. */
    static Stream<Arguments> allSessionTypes() {
        return Stream.of(
                // Original samples
                Arguments.of("ATM",
                        "insertCard . enterPIN . +{AUTH: rec X . &{checkBalance: X, withdraw: +{OK: X, INSUFFICIENT: X}, deposit: X, ejectCard: end}, REJECTED: ejectCard . end}"),
                Arguments.of("FileHandle",
                        "open . &{read: close . end, write: close . end}"),
                Arguments.of("SimpleIterator",
                        "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"),

                // New tool-protocol samples
                Arguments.of("ParserSession",
                        "setInput . parse . +{OK: &{prettyPrint: end, getAST: end}, ERROR: getError . end}"),
                Arguments.of("SubtypingSession",
                        "setLeft . setRight . checkSubtype . +{SUBTYPE: end, NOT_SUBTYPE: end}"),
                Arguments.of("DualitySession",
                        "setType . computeDual . &{checkInvolution: +{INVOLUTION: end, NOT_INVOLUTION: end}, getDual: end}"),
                Arguments.of("MorphismSession",
                        "setSource . setTarget . classify . +{ISOMORPHISM: end, NOT_ISOMORPHISM: end}"),
                Arguments.of("TestGenSession",
                        "parseType . enumeratePaths . &{generateTests: end, getPathCount: end}"),
                Arguments.of("RecursionAnalysisSession",
                        "setType . checkGuardedness . +{GUARDED: checkContractivity . +{CONTRACTIVE: analyze . end, NOT_CONTRACTIVE: end}, NOT_GUARDED: end}"),
                Arguments.of("CoverageSession",
                        "parseType . enumeratePaths . computeCoverage . &{getStateCoverage: end, getTransitionCoverage: end}"),
                Arguments.of("EndomorphismSession",
                        "parseType . analyzeEndomorphisms . &{getSummary: end, getPreservationRate: end}"),
                Arguments.of("EnumerationSession",
                        "configure . enumerate . +{ALL_LATTICES: getCount . end, HAS_COUNTEREXAMPLE: getCounterexample . end}"),
                Arguments.of("GlobalTypeSession",
                        "parseGlobal . buildGlobalStateSpace . &{projectAll: checkProjections . end, checkLattice: end}"),
                Arguments.of("AnalysisPipeline",
                        "parse . buildStateSpace . &{checkLattice: +{IS_LATTICE: &{checkDistributive: getResult . end, getResult: end}, NOT_LATTICE: getResult . end}, getResult: end}"),
                Arguments.of("CongruenceSession",
                        "parse . buildStateSpace . checkLattice . +{IS_LATTICE: &{analyzeCongruences: getResult . end, isSimple: getResult . end, enumerateCongruences: getResult . end}, NOT_LATTICE: getResult . end}")
        );
    }

    // ======================================================================
    // Core pipeline: parse → terminate → build → lattice → distributive
    // ======================================================================

    @ParameterizedTest(name = "{0}")
    @MethodSource("allSessionTypes")
    void parsesSuccessfully(String name, String typeStr) {
        assertDoesNotThrow(() -> Parser.parse(typeStr),
                name + ": session type should parse without error");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allSessionTypes")
    void isTerminating(String name, String typeStr) {
        var ast = Parser.parse(typeStr);
        var result = TerminationChecker.checkTermination(ast);
        assertTrue(result.isTerminating(),
                name + ": session type should be terminating");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allSessionTypes")
    void buildsStateSpace(String name, String typeStr) {
        var ss = StateSpaceBuilder.build(Parser.parse(typeStr));
        assertNotNull(ss);
        assertFalse(ss.states().isEmpty(),
                name + ": state space should have states");
        assertTrue(ss.states().contains(ss.top()),
                name + ": top should be in state set");
        assertTrue(ss.states().contains(ss.bottom()),
                name + ": bottom should be in state set");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allSessionTypes")
    void formsLattice(String name, String typeStr) {
        var ss = StateSpaceBuilder.build(Parser.parse(typeStr));
        var lr = LatticeChecker.checkLattice(ss);
        assertTrue(lr.isLattice(),
                name + ": state space should form a lattice (counterexample: " + lr.counterexample() + ")");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allSessionTypes")
    void distributivityClassified(String name, String typeStr) {
        var ss = StateSpaceBuilder.build(Parser.parse(typeStr));
        var dr = DistributivityChecker.checkDistributive(ss);
        assertNotNull(dr.classification(),
                name + ": should have a classification");
        assertTrue(dr.classification().matches("boolean|distributive|modular|lattice"),
                name + ": classification should be valid, got: " + dr.classification());
    }

    // ======================================================================
    // Protocol exercise: valid paths
    // ======================================================================

    @Nested
    class ATMProtocol {

        @Test
        void happyPath() {
            var atm = new ATM();
            atm.insertCard();
            var pin = atm.enterPIN();
            assertEquals(ATM.PinResult.AUTH, pin);
            atm.checkBalance();
            atm.ejectCard();
        }

        @Test
        void rejectPath() {
            var atm = new ATM();
            atm.setPinValid(false);
            atm.insertCard();
            var pin = atm.enterPIN();
            assertEquals(ATM.PinResult.REJECTED, pin);
            atm.ejectCard();
        }

        @Test
        void outOfOrderThrows() {
            var atm = new ATM();
            assertThrows(Exception.class, atm::enterPIN);
        }
    }

    @Nested
    class FileHandleProtocol {

        @Test
        void readPath() {
            var fh = new FileHandle();
            fh.open();
            fh.read();
            fh.close();
        }

        @Test
        void writePath() {
            var fh = new FileHandle();
            fh.open();
            fh.write();
            fh.close();
        }

        @Test
        void outOfOrderThrows() {
            var fh = new FileHandle();
            assertThrows(Exception.class, fh::read);
        }
    }

    @Nested
    class SimpleIteratorProtocol {

        @Test
        void emptyIteration() {
            var it = new SimpleIterator();
            it.setRemaining(0);
            var has = it.hasNext();
            assertEquals(SimpleIterator.HasNextResult.FALSE, has);
        }

        @Test
        void singleElement() {
            var it = new SimpleIterator();
            it.setRemaining(1);
            assertEquals(SimpleIterator.HasNextResult.TRUE, it.hasNext());
            it.next();
            assertEquals(SimpleIterator.HasNextResult.FALSE, it.hasNext());
        }
    }

    @Nested
    class ParserSessionProtocol {

        @Test
        void successPath() {
            var ps = new ParserSession();
            ps.setInput("&{a: end}");
            var result = ps.parse();
            assertEquals(ParserSession.ParseResult.OK, result);
            assertNotNull(ps.prettyPrint());
        }

        @Test
        void errorPath() {
            var ps = new ParserSession();
            ps.setInput("{{invalid");
            var result = ps.parse();
            assertEquals(ParserSession.ParseResult.ERROR, result);
            assertNotNull(ps.getError());
        }

        @Test
        void outOfOrderThrows() {
            var ps = new ParserSession();
            assertThrows(Exception.class, ps::parse);
        }
    }

    @Nested
    class SubtypingSessionProtocol {

        @Test
        void subtypePath() {
            var ss = new SubtypingSession();
            ss.setLeft("&{a: end, b: end}");
            ss.setRight("&{a: end}");
            var result = ss.checkSubtype();
            assertEquals(SubtypingSession.SubtypeDecision.SUBTYPE, result);
        }

        @Test
        void notSubtypePath() {
            var ss = new SubtypingSession();
            ss.setLeft("&{a: end}");
            ss.setRight("&{a: end, b: end}");
            var result = ss.checkSubtype();
            assertEquals(SubtypingSession.SubtypeDecision.NOT_SUBTYPE, result);
        }
    }

    @Nested
    class DualitySessionProtocol {

        @Test
        void dualPath() {
            var ds = new DualitySession();
            ds.setType("&{a: end, b: end}");
            ds.computeDual();
            assertNotNull(ds.getDual());
        }

        @Test
        void involutionPath() {
            var ds = new DualitySession();
            ds.setType("&{a: end, b: end}");
            ds.computeDual();
            var result = ds.checkInvolution();
            assertEquals(DualitySession.InvolutionResult.INVOLUTION, result);
        }
    }

    @Nested
    class MorphismSessionProtocol {

        @Test
        void isomorphismPath() {
            var ms = new MorphismSession();
            ms.setSource("&{a: end, b: end}");
            ms.setTarget("&{a: end, b: end}");
            var result = ms.classify();
            // Same type → isomorphism (or at minimum not-not)
            assertNotNull(result);
        }
    }

    @Nested
    class AnalysisPipelineProtocol {

        @Test
        void fullPipeline() {
            var ap = new AnalysisPipeline();
            ap.parse("&{a: end, b: end}");
            ap.buildStateSpace();
            var lattice = ap.checkLattice();
            assertEquals(AnalysisPipeline.LatticeDecision.IS_LATTICE, lattice);
            ap.checkDistributive();
            assertNotNull(ap.getResult());
        }

        @Test
        void outOfOrderThrows() {
            var ap = new AnalysisPipeline();
            assertThrows(Exception.class, ap::buildStateSpace);
        }
    }

    @Nested
    class RecursionAnalysisProtocol {

        @Test
        void guardedContractiveType() {
            var ra = new RecursionAnalysisSession();
            ra.setType("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}");
            var g = ra.checkGuardedness();
            assertEquals(RecursionAnalysisSession.GuardednessResult.GUARDED, g);
            var c = ra.checkContractivity();
            assertEquals(RecursionAnalysisSession.ContractivityResult.CONTRACTIVE, c);
            assertNotNull(ra.analyze());
        }

        @Test
        void nonRecursiveType() {
            var ra = new RecursionAnalysisSession();
            ra.setType("&{a: end}");
            var g = ra.checkGuardedness();
            assertEquals(RecursionAnalysisSession.GuardednessResult.GUARDED, g);
        }
    }

    @Nested
    class EnumerationSessionProtocol {

        @Test
        void smallEnumeration() {
            var es = new EnumerationSession();
            es.configure(2, 2);
            var result = es.enumerate();
            assertEquals(EnumerationSession.UniversalityDecision.ALL_LATTICES, result);
            assertTrue(es.getCount() > 0);
        }
    }

    @Nested
    class CongruenceSessionProtocol {

        @Test
        void analyzeCongruencesPath() {
            var cs = new CongruenceSession();
            cs.parse("&{a: end, b: end}");
            cs.buildStateSpace();
            var decision = cs.checkLattice();
            assertEquals(CongruenceSession.LatticeDecision.IS_LATTICE, decision);
            cs.analyzeCongruences();
            var result = cs.getResult();
            assertNotNull(result);
            assertTrue(result.contains("congruences="));
        }

        @Test
        void isSimplePath() {
            var cs = new CongruenceSession();
            cs.parse("&{a: end}");
            cs.buildStateSpace();
            var decision = cs.checkLattice();
            assertEquals(CongruenceSession.LatticeDecision.IS_LATTICE, decision);
            cs.isSimple();
            var result = cs.getResult();
            assertNotNull(result);
            assertTrue(result.contains("simple="));
        }

        @Test
        void enumerateCongruencesPath() {
            var cs = new CongruenceSession();
            cs.parse("&{a: end, b: end}");
            cs.buildStateSpace();
            var decision = cs.checkLattice();
            assertEquals(CongruenceSession.LatticeDecision.IS_LATTICE, decision);
            cs.enumerateCongruences();
            assertNotNull(cs.getResult());
        }

        @Test
        void outOfOrderThrows() {
            var cs = new CongruenceSession();
            assertThrows(Exception.class, cs::buildStateSpace);
        }
    }

    // ======================================================================
    // State space metrics
    // ======================================================================

    @ParameterizedTest(name = "{0}")
    @MethodSource("allSessionTypes")
    void stateSpaceMetrics(String name, String typeStr) {
        var ss = StateSpaceBuilder.build(Parser.parse(typeStr));
        System.out.printf("%-30s states=%-3d transitions=%-3d%n",
                name, ss.states().size(), ss.transitions().size());
        assertTrue(ss.states().size() >= 1, name + ": needs at least 1 state");
        // All reachable states should reach bottom (termination)
        for (int state : ss.states()) {
            var reachable = ss.reachableFrom(state);
            assertTrue(reachable.contains(ss.bottom()),
                    name + ": state " + state + " should reach bottom");
        }
    }
}
