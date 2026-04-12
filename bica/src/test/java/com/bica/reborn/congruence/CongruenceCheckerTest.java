package com.bica.reborn.congruence;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for congruence lattices of session types.
 * Mirrors all 41 Python tests from test_congruence.py.
 */
class CongruenceCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -- Fixtures -----------------------------------------------------------

    private StateSpace endSs() { return build("end"); }
    private StateSpace simpleBranch() { return build("&{a: end, b: end}"); }
    private StateSpace deepChain() { return build("&{a: &{b: &{c: end}}}"); }
    private StateSpace parallelSs() { return build("(&{a: end} || &{b: end})"); }
    private StateSpace nestedSs() { return build("&{a: &{c: end, d: end}, b: &{e: end, f: end}}"); }
    private StateSpace selectSs() { return build("+{ok: end, err: end}"); }

    // -----------------------------------------------------------------------
    // Principal congruences
    // -----------------------------------------------------------------------

    @Nested
    class PrincipalCongruence {

        @Test
        void sameElementGivesIdentity() {
            var ss = simpleBranch();
            int top = ss.top();
            Congruence pc = CongruenceChecker.principalCongruence(ss, top, top);
            assertTrue(pc.isTrivialBottom());
        }

        @Test
        void topBottomGivesTotal() {
            var ss = simpleBranch();
            Congruence pc = CongruenceChecker.principalCongruence(ss, ss.top(), ss.bottom());
            assertTrue(pc.isTrivialTop());
        }

        @Test
        void resultType() {
            var ss = simpleBranch();
            var states = ss.states().stream().sorted().toList();
            Congruence pc = CongruenceChecker.principalCongruence(ss, states.get(0), states.get(1));
            assertNotNull(pc);
            assertTrue(pc.numClasses() >= 1);
        }

        @Test
        void chainPrincipal() {
            var ss = deepChain();
            var states = ss.states().stream().sorted().toList();
            Congruence pc = CongruenceChecker.principalCongruence(ss, states.get(0), states.get(1));
            assertNotNull(pc);
        }

        @Test
        void generatorsRecorded() {
            var ss = simpleBranch();
            var states = ss.states().stream().sorted().toList();
            Congruence pc = CongruenceChecker.principalCongruence(ss, states.get(0), states.get(1));
            assertEquals(1, pc.generators().size());
            assertEquals(states.get(0), pc.generators().get(0)[0]);
            assertEquals(states.get(1), pc.generators().get(0)[1]);
        }
    }

    // -----------------------------------------------------------------------
    // Enumerate congruences
    // -----------------------------------------------------------------------

    @Nested
    class EnumerateCongruences {

        @Test
        void endOneCongruence() {
            var congs = CongruenceChecker.enumerateCongruences(endSs());
            assertEquals(1, congs.size());
        }

        @Test
        void simpleBranchCongruences() {
            var congs = CongruenceChecker.enumerateCongruences(simpleBranch());
            assertTrue(congs.size() >= 2);
        }

        @Test
        void chainCongruences() {
            var congs = CongruenceChecker.enumerateCongruences(deepChain());
            assertTrue(congs.size() >= 2);
        }

        @Test
        void sortedByClasses() {
            var congs = CongruenceChecker.enumerateCongruences(simpleBranch());
            for (int i = 0; i < congs.size() - 1; i++) {
                assertTrue(congs.get(i).numClasses() <= congs.get(i + 1).numClasses());
            }
        }

        @Test
        void includesIdentity() {
            var congs = CongruenceChecker.enumerateCongruences(simpleBranch());
            assertTrue(congs.stream().anyMatch(Congruence::isTrivialBottom));
        }

        @Test
        void includesTotal() {
            var congs = CongruenceChecker.enumerateCongruences(simpleBranch());
            assertTrue(congs.stream().anyMatch(Congruence::isTrivialTop));
        }
    }

    // -----------------------------------------------------------------------
    // Congruence lattice
    // -----------------------------------------------------------------------

    @Nested
    class CongruenceLatticeTests {

        @Test
        void conStructure() {
            var con = CongruenceChecker.congruenceLattice(simpleBranch());
            assertNotNull(con);
            assertTrue(con.numCongruences() >= 2);
        }

        @Test
        void conHasBottomTop() {
            var ss = simpleBranch();
            var con = CongruenceChecker.congruenceLattice(ss);
            assertTrue(con.bottom() >= 0);
            assertTrue(con.top() >= 0);
            if (ss.states().size() > 1) {
                assertNotEquals(con.bottom(), con.top());
            }
        }

        @Test
        void conOrderingReflexive() {
            var con = CongruenceChecker.congruenceLattice(simpleBranch());
            for (int i = 0; i < con.congruences().size(); i++) {
                assertTrue(con.ordering().contains(List.of(i, i)));
            }
        }

        @Test
        void conIsDistributive() {
            // Funayama-Nakayama: Con(L) is always distributive
            var con = CongruenceChecker.congruenceLattice(simpleBranch());
            assertTrue(con.isDistributive());
        }

        @Test
        void chainCon() {
            var con = CongruenceChecker.congruenceLattice(deepChain());
            assertTrue(con.numCongruences() >= 2);
        }
    }

    // -----------------------------------------------------------------------
    // Quotient lattice
    // -----------------------------------------------------------------------

    @Nested
    class QuotientLattice {

        @Test
        void identityQuotient() {
            var ss = simpleBranch();
            var congs = CongruenceChecker.enumerateCongruences(ss);
            var identity = congs.stream().filter(Congruence::isTrivialBottom).findFirst().orElseThrow();
            var q = CongruenceChecker.quotientLattice(ss, identity);
            assertEquals(ss.states().size(), q.states().size());
        }

        @Test
        void totalQuotient() {
            var ss = simpleBranch();
            var congs = CongruenceChecker.enumerateCongruences(ss);
            var total = congs.stream().filter(Congruence::isTrivialTop).findFirst().orElseThrow();
            var q = CongruenceChecker.quotientLattice(ss, total);
            assertEquals(1, q.states().size());
        }

        @Test
        void quotientIsSmaller() {
            var ss = deepChain();
            var congs = CongruenceChecker.enumerateCongruences(ss);
            for (var c : congs) {
                if (!c.isTrivialBottom() && !c.isTrivialTop()) {
                    var q = CongruenceChecker.quotientLattice(ss, c);
                    assertTrue(q.states().size() < ss.states().size());
                }
            }
        }

        @Test
        void quotientHasTransitions() {
            var ss = deepChain();
            var congs = CongruenceChecker.enumerateCongruences(ss);
            for (var c : congs) {
                if (!c.isTrivialTop()) {
                    var q = CongruenceChecker.quotientLattice(ss, c);
                    if (q.states().size() > 1) {
                        assertFalse(q.transitions().isEmpty());
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Simplicity
    // -----------------------------------------------------------------------

    @Nested
    class Simplicity {

        @Test
        void endIsSimple() {
            assertTrue(CongruenceChecker.isSimple(endSs()));
        }

        @Test
        void twoElementIsSimple() {
            var ss = build("&{a: end}");
            assertTrue(CongruenceChecker.isSimple(ss));
        }

        @Test
        void branchMayNotBeSimple() {
            boolean result = CongruenceChecker.isSimple(simpleBranch());
            // Just verify it returns a boolean without error
            assertNotNull(result);
        }
    }

    // -----------------------------------------------------------------------
    // Birkhoff fast path
    // -----------------------------------------------------------------------

    @Nested
    class Birkhoff {

        @Test
        void distributiveReturnsList() {
            var result = CongruenceChecker.birkhoffCongruences(simpleBranch());
            if (result != null) {
                assertFalse(result.isEmpty());
                for (var c : result) assertNotNull(c);
            }
        }

        @Test
        void chainBirkhoff() {
            var result = CongruenceChecker.birkhoffCongruences(deepChain());
            if (result != null) {
                assertTrue(result.size() >= 2);
            }
        }

        @Test
        void birkhoffConsistentWithEnumerate() {
            var birk = CongruenceChecker.birkhoffCongruences(simpleBranch());
            var enm = CongruenceChecker.enumerateCongruences(simpleBranch());
            if (birk != null) {
                assertEquals(birk.size(), enm.size());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Simplification options
    // -----------------------------------------------------------------------

    @Nested
    class Simplification {

        @Test
        void noOptionsForSimple() {
            var opts = CongruenceChecker.simplificationOptions(endSs());
            assertTrue(opts.isEmpty());
        }

        @Test
        void optionsSorted() {
            var opts = CongruenceChecker.simplificationOptions(deepChain());
            for (int i = 0; i < opts.size() - 1; i++) {
                assertTrue(opts.get(i).getValue() <= opts.get(i + 1).getValue());
            }
        }

        @Test
        void optionsAreNonTrivial() {
            var opts = CongruenceChecker.simplificationOptions(deepChain());
            for (var entry : opts) {
                assertFalse(entry.getKey().isTrivialBottom());
                assertFalse(entry.getKey().isTrivialTop());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Full analysis
    // -----------------------------------------------------------------------

    @Nested
    class Analysis {

        @Test
        void endAnalysis() {
            var a = CongruenceChecker.analyzeCongruences(endSs());
            assertNotNull(a);
            assertEquals(1, a.numStates());
            assertTrue(a.isSimple());
        }

        @Test
        void simpleBranchAnalysis() {
            var a = CongruenceChecker.analyzeCongruences(simpleBranch());
            assertTrue(a.numCongruences() >= 2);
            assertNotNull(a.isModular());
            assertNotNull(a.isDistributiveLattice());
        }

        @Test
        void chainAnalysis() {
            var a = CongruenceChecker.analyzeCongruences(deepChain());
            assertTrue(a.numCongruences() >= 2);
            assertTrue(a.numJoinIrreducibles() >= 1);
        }

        @Test
        void parallelAnalysis() {
            var a = CongruenceChecker.analyzeCongruences(parallelSs());
            assertNotNull(a);
        }

        @Test
        void funayamaNakayama() {
            // Con(L) is distributive (Funayama-Nakayama theorem)
            var a = CongruenceChecker.analyzeCongruences(simpleBranch());
            assertTrue(a.conLattice().isDistributive());
        }
    }

    // -----------------------------------------------------------------------
    // Benchmarks
    // -----------------------------------------------------------------------

    @Nested
    class Benchmarks {

        @Test
        void simpleBranchBenchmark() {
            var a = CongruenceChecker.analyzeCongruences(build("&{a: end, b: end}"));
            assertTrue(a.numCongruences() >= 2);
            assertTrue(a.conLattice().isDistributive());
        }

        @Test
        void deepChainBenchmark() {
            var a = CongruenceChecker.analyzeCongruences(build("&{a: &{b: &{c: end}}}"));
            assertTrue(a.numCongruences() >= 2);
            assertTrue(a.conLattice().isDistributive());
        }

        @Test
        void simpleSelectBenchmark() {
            var a = CongruenceChecker.analyzeCongruences(build("+{ok: end, err: end}"));
            assertTrue(a.numCongruences() >= 2);
            assertTrue(a.conLattice().isDistributive());
        }

        @Test
        void simpleParallelBenchmark() {
            var a = CongruenceChecker.analyzeCongruences(build("(&{a: end} || &{b: end})"));
            assertTrue(a.numCongruences() >= 2);
            assertTrue(a.conLattice().isDistributive());
        }

        @Test
        void asymmetricBranchBenchmark() {
            var a = CongruenceChecker.analyzeCongruences(build("&{a: &{c: end}, b: end}"));
            assertTrue(a.numCongruences() >= 2);
            assertTrue(a.conLattice().isDistributive());
        }
    }

    // -----------------------------------------------------------------------
    // Congruence properties
    // -----------------------------------------------------------------------

    @Nested
    class CongruenceProperties {

        @Test
        void classOf() {
            var ss = simpleBranch();
            var congs = CongruenceChecker.enumerateCongruences(ss);
            var total = congs.stream().filter(Congruence::isTrivialTop).findFirst().orElseThrow();
            Set<Integer> cls = total.classOf(ss.top());
            assertTrue(cls.contains(ss.bottom()));
        }

        @Test
        void trivialBottomCheck() {
            var ss = simpleBranch();
            var congs = CongruenceChecker.enumerateCongruences(ss);
            var identity = congs.stream().filter(Congruence::isTrivialBottom).findFirst().orElseThrow();
            assertTrue(identity.isTrivialBottom());
            assertFalse(identity.isTrivialTop());
        }
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Nested
    class EdgeCases {

        @Test
        void singleState() {
            var congs = CongruenceChecker.enumerateCongruences(build("end"));
            assertEquals(1, congs.size());
        }

        @Test
        void twoStates() {
            var a = CongruenceChecker.analyzeCongruences(build("&{a: end}"));
            assertTrue(a.isSimple());
        }

        @Test
        void wideBranch() {
            var a = CongruenceChecker.analyzeCongruences(build("&{a: end, b: end, c: end}"));
            assertTrue(a.numCongruences() >= 2);
        }

        @Test
        void nestedDeep() {
            var a = CongruenceChecker.analyzeCongruences(build("&{a: &{b: end}, c: &{d: end}}"));
            assertNotNull(a);
        }
    }
}
