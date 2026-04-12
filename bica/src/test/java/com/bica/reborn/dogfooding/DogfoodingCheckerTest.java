package com.bica.reborn.dogfooding;

import com.bica.reborn.dogfooding.DogfoodingChecker.CheckerProtocol;
import com.bica.reborn.dogfooding.DogfoodingChecker.DogfoodResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.statespace.TransitionKind;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Java port of {@code reticulate/tests/test_dogfooding.py}. */
class DogfoodingCheckerTest {

    private static CheckerProtocol parserProtocol() {
        return DogfoodingChecker.CHECKER_PROTOCOLS.stream()
                .filter(p -> p.moduleName().equals("parser"))
                .findFirst()
                .orElseThrow();
    }

    // ---------------------------------------------------------------------
    // Registry
    // ---------------------------------------------------------------------

    @Test
    void registryHasFourCheckers() {
        assertEquals(4, DogfoodingChecker.CHECKER_PROTOCOLS.size());
        Set<String> names = new HashSet<>();
        for (CheckerProtocol p : DogfoodingChecker.CHECKER_PROTOCOLS) {
            names.add(p.moduleName());
        }
        assertEquals(Set.of("parser", "statespace", "lattice", "subtyping"), names);
    }

    @Test
    void everyProtocolHasSessionTypeString() {
        for (CheckerProtocol p : DogfoodingChecker.CHECKER_PROTOCOLS) {
            assertNotNull(p.sessionType());
            assertFalse(p.sessionType().isEmpty());
            assertNotNull(p.entryPoint());
            assertFalse(p.entryPoint().isEmpty());
            assertTrue(p.phases().size() >= 2);
        }
    }

    @Test
    void everyProtocolParses() {
        for (CheckerProtocol p : DogfoodingChecker.CHECKER_PROTOCOLS) {
            assertNotNull(Parser.parse(p.sessionType()));
        }
    }

    @Test
    void everyProtocolBuildsStatespace() {
        for (CheckerProtocol p : DogfoodingChecker.CHECKER_PROTOCOLS) {
            StateSpace ss = StateSpaceBuilder.build(Parser.parse(p.sessionType()));
            assertTrue(ss.states().size() >= 2);
        }
    }

    // ---------------------------------------------------------------------
    // Call-graph poset
    // ---------------------------------------------------------------------

    @Test
    void callGraphReachabilityChain() {
        List<String> phases = List.of("a", "b", "c");
        List<String[]> edges = List.of(
                new String[] {"a", "b"},
                new String[] {"b", "c"});
        Map<String, Set<String>> reach = DogfoodingChecker.callGraphReachability(phases, edges);
        assertEquals(Set.of("a", "b", "c"), reach.get("a"));
        assertEquals(Set.of("b", "c"), reach.get("b"));
        assertEquals(Set.of("c"), reach.get("c"));
    }

    @Test
    void callGraphReachabilityEmptyEdges() {
        Map<String, Set<String>> reach = DogfoodingChecker.callGraphReachability(
                List.of("a", "b"), List.of());
        assertEquals(Set.of("a"), reach.get("a"));
        assertEquals(Set.of("b"), reach.get("b"));
    }

    @Test
    void callGraphLeqReflexive() {
        var leq = DogfoodingChecker.callGraphLeq(
                List.of("a", "b"), List.<String[]>of(new String[] {"a", "b"}));
        assertTrue(leq.test("a", "a"));
        assertTrue(leq.test("b", "b"));
    }

    @Test
    void callGraphLeqTransitive() {
        var leq = DogfoodingChecker.callGraphLeq(
                List.of("a", "b", "c"),
                List.of(new String[] {"a", "b"}, new String[] {"b", "c"}));
        assertTrue(leq.test("a", "c"));
        assertFalse(leq.test("c", "a"));
    }

    @Test
    void callGraphLeqIncomparable() {
        var leq = DogfoodingChecker.callGraphLeq(
                List.of("a", "b", "c"),
                List.of(new String[] {"a", "b"}, new String[] {"a", "c"}));
        assertFalse(leq.test("b", "c"));
        assertFalse(leq.test("c", "b"));
    }

    // ---------------------------------------------------------------------
    // phi and psi construction
    // ---------------------------------------------------------------------

    @Test
    void buildPhiAssignsEveryPhase() {
        CheckerProtocol proto = parserProtocol();
        StateSpace ss = StateSpaceBuilder.build(Parser.parse(proto.sessionType()));
        Map<String, Integer> phi = DogfoodingChecker.buildPhi(proto, ss);
        for (String phase : proto.phases()) {
            assertTrue(phi.containsKey(phase), "phase missing: " + phase);
        }
    }

    @Test
    void buildPhiStartsAtTop() {
        CheckerProtocol proto = parserProtocol();
        StateSpace ss = StateSpaceBuilder.build(Parser.parse(proto.sessionType()));
        Map<String, Integer> phi = DogfoodingChecker.buildPhi(proto, ss);
        assertEquals(ss.top(), (int) phi.get(proto.phases().get(0)));
    }

    @Test
    void buildPsiNonempty() {
        CheckerProtocol proto = parserProtocol();
        StateSpace ss = StateSpaceBuilder.build(Parser.parse(proto.sessionType()));
        Map<String, Integer> phi = DogfoodingChecker.buildPhi(proto, ss);
        Map<Integer, String> psi = DogfoodingChecker.buildPsi(proto, ss, phi);
        assertTrue(psi.size() >= 1);
    }

    @Test
    void phiOrderPreserving() {
        CheckerProtocol proto = parserProtocol();
        StateSpace ss = StateSpaceBuilder.build(Parser.parse(proto.sessionType()));
        Map<String, Integer> phi = DogfoodingChecker.buildPhi(proto, ss);
        assertTrue(DogfoodingChecker.checkPhiOrderPreserving(proto, ss, phi));
    }

    @Test
    void psiOrderPreserving() {
        CheckerProtocol proto = parserProtocol();
        StateSpace ss = StateSpaceBuilder.build(Parser.parse(proto.sessionType()));
        Map<String, Integer> phi = DogfoodingChecker.buildPhi(proto, ss);
        Map<Integer, String> psi = DogfoodingChecker.buildPsi(proto, ss, phi);
        assertTrue(DogfoodingChecker.checkPsiOrderPreserving(proto, ss, psi));
    }

    // ---------------------------------------------------------------------
    // Full dogfood driver
    // ---------------------------------------------------------------------

    @Test
    void dogfoodCheckerReturnsResult() {
        CheckerProtocol proto = DogfoodingChecker.CHECKER_PROTOCOLS.get(0);
        DogfoodResult r = DogfoodingChecker.dogfoodChecker(proto);
        assertNotNull(r);
        assertSame(proto, r.protocol());
    }

    @Test
    void dogfoodAllLength() {
        List<DogfoodResult> results = DogfoodingChecker.dogfoodAll();
        assertEquals(DogfoodingChecker.CHECKER_PROTOCOLS.size(), results.size());
    }

    @Test
    void dogfoodAllEveryCheckerIsLattice() {
        for (DogfoodResult r : DogfoodingChecker.dogfoodAll()) {
            assertTrue(r.latticeResult().isLattice(),
                    r.protocol().moduleName() + " is not a lattice");
        }
    }

    @Test
    void dogfoodAllEveryCheckerBidirectional() {
        for (DogfoodResult r : DogfoodingChecker.dogfoodAll()) {
            assertTrue(r.bidirectional(),
                    r.protocol().moduleName() + " fails bidirectional morphism");
        }
    }

    @Test
    void dogfoodSummaryTotals() {
        Map<String, Integer> s = DogfoodingChecker.dogfoodSummary();
        int n = DogfoodingChecker.CHECKER_PROTOCOLS.size();
        assertEquals(n, (int) s.get("total"));
        assertEquals(n, (int) s.get("lattices"));
        assertEquals(n, (int) s.get("bidirectional"));
        assertEquals(n, (int) s.get("phi_ok"));
        assertEquals(n, (int) s.get("psi_ok"));
    }

    @Test
    void parserProtocolHasSelection() {
        CheckerProtocol proto = parserProtocol();
        StateSpace ss = StateSpaceBuilder.build(Parser.parse(proto.sessionType()));
        long selections = ss.transitions().stream()
                .filter(t -> t.kind() == TransitionKind.SELECTION)
                .count();
        assertTrue(selections >= 1);
    }

    @Test
    void latticeProtocolHasCounterexampleBranch() {
        CheckerProtocol proto = DogfoodingChecker.CHECKER_PROTOCOLS.stream()
                .filter(p -> p.moduleName().equals("lattice"))
                .findFirst().orElseThrow();
        assertTrue(proto.sessionType().contains("counterexample"));
    }

    @Test
    void subtypingProtocolTwoPhases() {
        CheckerProtocol proto = DogfoodingChecker.CHECKER_PROTOCOLS.stream()
                .filter(p -> p.moduleName().equals("subtyping"))
                .findFirst().orElseThrow();
        assertEquals(2, proto.phases().size());
    }

    @Test
    void statespaceProtocolLinear() {
        CheckerProtocol proto = DogfoodingChecker.CHECKER_PROTOCOLS.stream()
                .filter(p -> p.moduleName().equals("statespace"))
                .findFirst().orElseThrow();
        StateSpace ss = StateSpaceBuilder.build(Parser.parse(proto.sessionType()));
        assertEquals(3, ss.transitions().size());
    }

    @Test
    void customProtocolDogfoodable() {
        CheckerProtocol custom = new CheckerProtocol(
                "example",
                "run",
                List.of("init", "work", "done"),
                "&{init: &{work: &{done: end}}}",
                List.of(new String[] {"init", "work"},
                        new String[] {"work", "done"}));
        DogfoodResult r = DogfoodingChecker.dogfoodChecker(custom);
        assertTrue(r.latticeResult().isLattice());
        assertTrue(r.bidirectional());
    }

    @Test
    void phiMonotoneOnCallGraphChain() {
        CheckerProtocol proto = parserProtocol();
        StateSpace ss = StateSpaceBuilder.build(Parser.parse(proto.sessionType()));
        Map<String, Integer> phi = DogfoodingChecker.buildPhi(proto, ss);
        assertTrue(phi.containsKey("tokenize"));
        assertTrue(phi.containsKey("parse_ast"));
    }

    @Test
    void dogfoodResultFieldsConsistent() {
        DogfoodResult r = DogfoodingChecker.dogfoodChecker(
                DogfoodingChecker.CHECKER_PROTOCOLS.get(0));
        assertEquals(r.bidirectional(), r.phiValid() && r.psiValid());
    }
}
