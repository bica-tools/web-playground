package com.bica.reborn.mapk_pi3k;

import com.bica.reborn.mapk_pi3k.MapkPi3kChecker.BottleneckReport;
import com.bica.reborn.mapk_pi3k.MapkPi3kChecker.DrugTargetImpact;
import com.bica.reborn.mapk_pi3k.MapkPi3kChecker.PathwayState;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Java port of {@code reticulate/tests/test_mapk_pi3k.py}. */
final class MapkPi3kCheckerTest {

    // -------------------------------------------------------------------
    // PathwayState
    // -------------------------------------------------------------------

    @Test
    void pathwayStateOrderReflexive() {
        PathwayState s = new PathwayState(List.of("A", "B"), List.of(true, false));
        assertTrue(s.leq(s));
    }

    @Test
    void pathwayStateMeetJoin() {
        List<String> roles = List.of("A", "B", "C");
        PathwayState s = new PathwayState(roles, List.of(true, false, true));
        PathwayState t = new PathwayState(roles, List.of(false, false, true));
        assertEquals(new PathwayState(roles, List.of(false, false, true)), s.meet(t));
        assertEquals(new PathwayState(roles, List.of(true, false, true)), s.join(t));
    }

    @Test
    void pathwayStateJoinOrder() {
        List<String> roles = List.of("A");
        PathwayState bot = new PathwayState(roles, List.of(false));
        PathwayState top = new PathwayState(roles, List.of(true));
        assertTrue(bot.leq(top));
        assertFalse(top.leq(bot));
    }

    @Test
    void pathwayStateLengthMismatch() {
        assertThrows(IllegalArgumentException.class,
                () -> new PathwayState(List.of("A", "B"), List.of(true)));
    }

    @Test
    void pathwayStateCrossRoleCompare() {
        PathwayState a = new PathwayState(List.of("A"), List.of(true));
        PathwayState b = new PathwayState(List.of("B"), List.of(true));
        assertThrows(IllegalArgumentException.class, () -> a.leq(b));
    }

    @Test
    void phosphorylatedSet() {
        PathwayState s = new PathwayState(
                List.of("A", "B", "C"), List.of(true, false, true));
        assertEquals(new TreeSet<>(Set.of("A", "C")), new TreeSet<>(s.phosphorylated()));
    }

    // -------------------------------------------------------------------
    // Canonical cascades parse and build
    // -------------------------------------------------------------------

    @Test
    void mapkCascadeParses() {
        assertNotNull(Parser.parse(MapkPi3kChecker.MAPK_CASCADE));
    }

    @Test
    void pi3kAxisParses() {
        assertNotNull(Parser.parse(MapkPi3kChecker.PI3K_AKT_AXIS));
    }

    @Test
    void crosstalkParses() {
        assertNotNull(Parser.parse(MapkPi3kChecker.CROSSTALK));
    }

    @Test
    void mapkStateSpaceNonempty() {
        StateSpace ss = MapkPi3kChecker.mapkStateSpace();
        assertTrue(ss.states().size() > 4);
        Set<String> labels = new java.util.HashSet<>();
        for (Transition t : ss.transitions()) labels.add(t.label());
        for (String r : MapkPi3kChecker.MAPK_ROLES) {
            assertTrue(labels.contains("phosphorylate" + r),
                    "missing phosphorylate" + r);
        }
    }

    @Test
    void pi3kStateSpaceNonempty() {
        StateSpace ss = MapkPi3kChecker.pi3kStateSpace();
        Set<String> labels = new java.util.HashSet<>();
        for (Transition t : ss.transitions()) labels.add(t.label());
        for (String r : MapkPi3kChecker.PI3K_ROLES) {
            assertTrue(labels.contains("phosphorylate" + r),
                    "missing phosphorylate" + r);
        }
    }

    @Test
    void crosstalkAtLeastAsBigAsMapk() {
        StateSpace cross = MapkPi3kChecker.crosstalkStateSpace();
        StateSpace mapk = MapkPi3kChecker.mapkStateSpace();
        assertTrue(cross.states().size() >= mapk.states().size());
    }

    // -------------------------------------------------------------------
    // phi / psi morphisms
    // -------------------------------------------------------------------

    @Test
    void phiMapCoversAllStates() {
        StateSpace ss = MapkPi3kChecker.mapkStateSpace();
        Map<Integer, PathwayState> phi =
                MapkPi3kChecker.phiMap(ss, MapkPi3kChecker.MAPK_ROLES);
        assertEquals(new TreeSet<>(ss.states()), new TreeSet<>(phi.keySet()));
    }

    @Test
    void phiTopIsEmptyVector() {
        StateSpace ss = MapkPi3kChecker.mapkStateSpace();
        Map<Integer, PathwayState> phi =
                MapkPi3kChecker.phiMap(ss, MapkPi3kChecker.MAPK_ROLES);
        PathwayState topVec = phi.get(ss.top());
        assertTrue(topVec.phosphorylated().isEmpty());
    }

    @Test
    void psiMapCoversBottomVector() {
        StateSpace ss = MapkPi3kChecker.mapkStateSpace();
        Map<PathwayState, Integer> psi =
                MapkPi3kChecker.psiMap(ss, MapkPi3kChecker.MAPK_ROLES);
        PathwayState bot = new PathwayState(
                MapkPi3kChecker.MAPK_ROLES, List.of(false, false, false, false));
        assertTrue(psi.containsKey(bot));
    }

    @Test
    void psiOfFullVectorDefinedForPi3k() {
        StateSpace ss = MapkPi3kChecker.pi3kStateSpace();
        Map<PathwayState, Integer> psi =
                MapkPi3kChecker.psiMap(ss, MapkPi3kChecker.PI3K_ROLES);
        PathwayState bot = new PathwayState(
                MapkPi3kChecker.PI3K_ROLES, List.of(false, false, false, false));
        assertTrue(psi.containsKey(bot));
    }

    @Test
    void galoisConnectionMapkExecutes() {
        StateSpace ss = MapkPi3kChecker.mapkStateSpace();
        // Only assert it runs without raising.
        MapkPi3kChecker.isGaloisConnectionPhiPsi(ss, MapkPi3kChecker.MAPK_ROLES);
    }

    @Test
    void galoisConnectionPi3kExecutes() {
        StateSpace ss = MapkPi3kChecker.pi3kStateSpace();
        MapkPi3kChecker.isGaloisConnectionPhiPsi(ss, MapkPi3kChecker.PI3K_ROLES);
    }

    // -------------------------------------------------------------------
    // Drug target impact
    // -------------------------------------------------------------------

    @Test
    void drugTargetImpactUnknownRaises() {
        StateSpace ss = MapkPi3kChecker.mapkStateSpace();
        assertThrows(IllegalArgumentException.class,
                () -> MapkPi3kChecker.drugTargetImpact(
                        ss, MapkPi3kChecker.MAPK_ROLES, "NotAProtein"));
    }

    @Test
    void drugTargetImpactRasIsBottleneck() {
        StateSpace ss = MapkPi3kChecker.mapkStateSpace();
        DrugTargetImpact impact = MapkPi3kChecker.drugTargetImpact(
                ss, MapkPi3kChecker.MAPK_ROLES, "Ras");
        assertEquals("Ras", impact.target());
        assertTrue(impact.reachableBefore() >= impact.reachableAfter());
        assertTrue(impact.efficacy() >= 0.5);
    }

    @Test
    void drugTargetRasDominatesErk() {
        StateSpace ss = MapkPi3kChecker.mapkStateSpace();
        DrugTargetImpact ras = MapkPi3kChecker.drugTargetImpact(
                ss, MapkPi3kChecker.MAPK_ROLES, "Ras");
        DrugTargetImpact erk = MapkPi3kChecker.drugTargetImpact(
                ss, MapkPi3kChecker.MAPK_ROLES, "ERK");
        assertTrue(ras.efficacy() >= erk.efficacy());
    }

    @Test
    void computeBottleneckReturnsRasForMapk() {
        StateSpace ss = MapkPi3kChecker.mapkStateSpace();
        BottleneckReport report = MapkPi3kChecker.computeBottleneck(
                ss, MapkPi3kChecker.MAPK_ROLES);
        assertEquals("Ras", report.target());
        assertTrue(report.efficacy() > 0.0);
        assertEquals(MapkPi3kChecker.MAPK_ROLES.size(), report.ranked().size());
    }

    @Test
    void computeBottleneckReturnsRtkForPi3k() {
        StateSpace ss = MapkPi3kChecker.pi3kStateSpace();
        BottleneckReport report = MapkPi3kChecker.computeBottleneck(
                ss, MapkPi3kChecker.PI3K_ROLES);
        assertEquals("RTK", report.target());
    }

    @Test
    void rankedIsMonotoneNonIncreasing() {
        StateSpace ss = MapkPi3kChecker.pi3kStateSpace();
        BottleneckReport report = MapkPi3kChecker.computeBottleneck(
                ss, MapkPi3kChecker.PI3K_ROLES);
        List<Map.Entry<String, Double>> ranked = report.ranked();
        for (int i = 0; i + 1 < ranked.size(); i++) {
            assertTrue(ranked.get(i).getValue() >= ranked.get(i + 1).getValue());
        }
    }

    // -------------------------------------------------------------------
    // Pathway states
    // -------------------------------------------------------------------

    @Test
    void pathwayStatesLengthMatchesStates() {
        StateSpace ss = MapkPi3kChecker.mapkStateSpace();
        List<PathwayState> ps = MapkPi3kChecker.pathwayStates(
                ss, MapkPi3kChecker.MAPK_ROLES);
        assertTrue(ps.size() >= ss.states().size());
    }

    @Test
    void pathwayStatesIncludeEmptyVector() {
        StateSpace ss = MapkPi3kChecker.mapkStateSpace();
        List<PathwayState> ps = MapkPi3kChecker.pathwayStates(
                ss, MapkPi3kChecker.MAPK_ROLES);
        boolean anyEmpty = false;
        for (PathwayState p : ps) {
            if (p.phosphorylated().isEmpty()) { anyEmpty = true; break; }
        }
        assertTrue(anyEmpty);
    }

    @Test
    void phiMonotoneOnPhosphorylateEdges() {
        StateSpace ss = MapkPi3kChecker.pi3kStateSpace();
        Map<Integer, PathwayState> phi =
                MapkPi3kChecker.phiMap(ss, MapkPi3kChecker.PI3K_ROLES);
        for (Transition t : ss.transitions()) {
            if (t.label().startsWith("phosphorylate") && t.source() != t.target()) {
                assertTrue(phi.get(t.source()).leq(phi.get(t.target())),
                        "phi not monotone on edge " + t);
            }
        }
    }
}
