package com.bica.reborn.lattice;

import com.bica.reborn.lattice.Dogfooding.CheckerProtocol;
import com.bica.reborn.lattice.Dogfooding.DogfoodResult;
import com.bica.reborn.lattice.Dogfooding.Edge;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.statespace.TransitionKind;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Dogfooding}.
 *
 * <p>Ported from {@code reticulate/tests/test_dogfooding.py} (Step 80i).
 */
class DogfoodingTest {

    private static CheckerProtocol parserProtocol() {
        return Dogfooding.CHECKER_PROTOCOLS.stream()
                .filter(p -> p.moduleName().equals("parser"))
                .findFirst().orElseThrow();
    }

    private static StateSpace ssOf(String s) {
        return StateSpaceBuilder.build(Parser.parse(s));
    }

    // ---------------------------------------------------------------------
    // Registry
    // ---------------------------------------------------------------------

    @Test
    void registryHasFourCheckers() {
        assertEquals(4, Dogfooding.CHECKER_PROTOCOLS.size());
        Set<String> names = Set.copyOf(
                Dogfooding.CHECKER_PROTOCOLS.stream().map(CheckerProtocol::moduleName).toList());
        assertEquals(Set.of("parser", "statespace", "lattice", "subtyping"), names);
    }

    @Test
    void everyProtocolHasSessionTypeString() {
        for (CheckerProtocol p : Dogfooding.CHECKER_PROTOCOLS) {
            assertNotNull(p.sessionType());
            assertFalse(p.sessionType().isEmpty());
            assertNotNull(p.entryPoint());
            assertFalse(p.entryPoint().isEmpty());
            assertTrue(p.phases().size() >= 2);
        }
    }

    @Test
    void everyProtocolParses() {
        for (CheckerProtocol p : Dogfooding.CHECKER_PROTOCOLS) {
            assertNotNull(Parser.parse(p.sessionType()));
        }
    }

    @Test
    void everyProtocolBuildsStatespace() {
        for (CheckerProtocol p : Dogfooding.CHECKER_PROTOCOLS) {
            StateSpace ss = ssOf(p.sessionType());
            assertTrue(ss.states().size() >= 2);
        }
    }

    // ---------------------------------------------------------------------
    // Call-graph poset
    // ---------------------------------------------------------------------

    @Test
    void callGraphReachabilityChain() {
        List<String> phases = List.of("a", "b", "c");
        List<Edge> edges = List.of(new Edge("a", "b"), new Edge("b", "c"));
        Map<String, Set<String>> reach = Dogfooding.callGraphReachability(phases, edges);
        assertEquals(Set.of("a", "b", "c"), reach.get("a"));
        assertEquals(Set.of("b", "c"), reach.get("b"));
        assertEquals(Set.of("c"), reach.get("c"));
    }

    @Test
    void callGraphReachabilityEmptyEdges() {
        Map<String, Set<String>> reach =
                Dogfooding.callGraphReachability(List.of("a", "b"), List.of());
        assertEquals(Set.of("a"), reach.get("a"));
        assertEquals(Set.of("b"), reach.get("b"));
    }

    @Test
    void callGraphLeqReflexive() {
        BiPredicate<String, String> leq =
                Dogfooding.callGraphLeq(List.of("a", "b"), List.of(new Edge("a", "b")));
        assertTrue(leq.test("a", "a"));
        assertTrue(leq.test("b", "b"));
    }

    @Test
    void callGraphLeqTransitive() {
        BiPredicate<String, String> leq = Dogfooding.callGraphLeq(
                List.of("a", "b", "c"),
                List.of(new Edge("a", "b"), new Edge("b", "c")));
        assertTrue(leq.test("a", "c"));
        assertFalse(leq.test("c", "a"));
    }

    @Test
    void callGraphLeqIncomparable() {
        BiPredicate<String, String> leq = Dogfooding.callGraphLeq(
                List.of("a", "b", "c"),
                List.of(new Edge("a", "b"), new Edge("a", "c")));
        assertFalse(leq.test("b", "c"));
        assertFalse(leq.test("c", "b"));
    }

    // ---------------------------------------------------------------------
    // phi and psi construction
    // ---------------------------------------------------------------------

    @Test
    void buildPhiAssignsEveryPhase() {
        CheckerProtocol p = parserProtocol();
        StateSpace ss = ssOf(p.sessionType());
        Map<String, Integer> phi = Dogfooding.buildPhi(p, ss);
        for (String phase : p.phases()) {
            assertTrue(phi.containsKey(phase), "phase missing: " + phase);
        }
    }

    @Test
    void buildPhiStartsAtTop() {
        CheckerProtocol p = parserProtocol();
        StateSpace ss = ssOf(p.sessionType());
        Map<String, Integer> phi = Dogfooding.buildPhi(p, ss);
        assertEquals(ss.top(), phi.get(p.phases().get(0)));
    }

    @Test
    void buildPsiNonempty() {
        CheckerProtocol p = parserProtocol();
        StateSpace ss = ssOf(p.sessionType());
        Map<String, Integer> phi = Dogfooding.buildPhi(p, ss);
        Map<Integer, String> psi = Dogfooding.buildPsi(p, ss, phi);
        assertTrue(psi.size() >= 1);
    }

    @Test
    void phiOrderPreserving() {
        CheckerProtocol p = parserProtocol();
        StateSpace ss = ssOf(p.sessionType());
        Map<String, Integer> phi = Dogfooding.buildPhi(p, ss);
        assertTrue(Dogfooding.checkPhiOrderPreserving(p, ss, phi));
    }

    @Test
    void psiOrderPreserving() {
        CheckerProtocol p = parserProtocol();
        StateSpace ss = ssOf(p.sessionType());
        Map<String, Integer> phi = Dogfooding.buildPhi(p, ss);
        Map<Integer, String> psi = Dogfooding.buildPsi(p, ss, phi);
        assertTrue(Dogfooding.checkPsiOrderPreserving(p, ss, psi));
    }

    // ---------------------------------------------------------------------
    // Full dogfood driver
    // ---------------------------------------------------------------------

    @Test
    void dogfoodCheckerReturnsResult() {
        CheckerProtocol proto = Dogfooding.CHECKER_PROTOCOLS.get(0);
        DogfoodResult r = Dogfooding.dogfoodChecker(proto);
        assertNotNull(r);
        assertSame(proto, r.protocol());
    }

    @Test
    void dogfoodAllLength() {
        List<DogfoodResult> results = Dogfooding.dogfoodAll();
        assertEquals(Dogfooding.CHECKER_PROTOCOLS.size(), results.size());
    }

    @Test
    void dogfoodAllEveryCheckerIsLattice() {
        for (DogfoodResult r : Dogfooding.dogfoodAll()) {
            assertTrue(r.latticeResult().isLattice(),
                    r.protocol().moduleName() + " is not a lattice");
        }
    }

    @Test
    void dogfoodAllEveryCheckerBidirectional() {
        for (DogfoodResult r : Dogfooding.dogfoodAll()) {
            assertTrue(r.bidirectional(),
                    r.protocol().moduleName() + " fails bidirectional morphism");
        }
    }

    @Test
    void dogfoodSummaryTotals() {
        Map<String, Integer> s = Dogfooding.dogfoodSummary();
        int n = Dogfooding.CHECKER_PROTOCOLS.size();
        assertEquals(n, s.get("total"));
        assertEquals(n, s.get("lattices"));
        assertEquals(n, s.get("bidirectional"));
        assertEquals(n, s.get("phi_ok"));
        assertEquals(n, s.get("psi_ok"));
    }

    @Test
    void parserProtocolHasSelection() {
        CheckerProtocol proto = parserProtocol();
        StateSpace ss = ssOf(proto.sessionType());
        long selectionCount = ss.transitions().stream()
                .filter(t -> t.kind() == TransitionKind.SELECTION)
                .count();
        assertTrue(selectionCount >= 1);
    }

    @Test
    void latticeProtocolHasCounterexampleBranch() {
        CheckerProtocol proto = Dogfooding.CHECKER_PROTOCOLS.stream()
                .filter(p -> p.moduleName().equals("lattice"))
                .findFirst().orElseThrow();
        assertTrue(proto.sessionType().contains("counterexample"));
    }

    @Test
    void subtypingProtocolTwoPhases() {
        CheckerProtocol proto = Dogfooding.CHECKER_PROTOCOLS.stream()
                .filter(p -> p.moduleName().equals("subtyping"))
                .findFirst().orElseThrow();
        assertEquals(2, proto.phases().size());
    }

    @Test
    void statespaceProtocolLinear() {
        CheckerProtocol proto = Dogfooding.CHECKER_PROTOCOLS.stream()
                .filter(p -> p.moduleName().equals("statespace"))
                .findFirst().orElseThrow();
        StateSpace ss = ssOf(proto.sessionType());
        assertEquals(3, ss.transitions().size());
    }

    @Test
    void customProtocolDogfoodable() {
        CheckerProtocol custom = new CheckerProtocol(
                "example",
                "run",
                List.of("init", "work", "done"),
                "&{init: &{work: &{done: end}}}",
                List.of(new Edge("init", "work"), new Edge("work", "done")));
        DogfoodResult r = Dogfooding.dogfoodChecker(custom);
        assertTrue(r.latticeResult().isLattice());
        assertTrue(r.bidirectional());
    }

    @Test
    void phiMonotoneOnCallGraphChain() {
        CheckerProtocol proto = parserProtocol();
        StateSpace ss = ssOf(proto.sessionType());
        Map<String, Integer> phi = Dogfooding.buildPhi(proto, ss);
        assertTrue(phi.containsKey("tokenize"));
        assertTrue(phi.containsKey("parse_ast"));
    }

    @Test
    void dogfoodResultFrozen() {
        // Java records are inherently immutable: verify DogfoodResult is a
        // final record with no setter methods.
        Class<?> cls = DogfoodResult.class;
        assertTrue(cls.isRecord());
        assertTrue(Modifier.isFinal(cls.getModifiers()));
        for (var method : cls.getDeclaredMethods()) {
            assertFalse(method.getName().startsWith("set"),
                    "unexpected setter: " + method.getName());
        }
    }
}
