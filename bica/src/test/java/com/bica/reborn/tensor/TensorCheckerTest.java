package com.bica.reborn.tensor;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TensorChecker} — tensor product of session type lattices.
 */
class TensorCheckerTest {

    private static StateSpace build(String input) {
        return StateSpaceBuilder.build(Parser.parse(input));
    }

    @Test
    void tensorOfTwoSimpleBranches() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");

        TensorResult r = TensorChecker.tensorProduct(
                Map.entry("A", ss1), Map.entry("B", ss2));

        assertTrue(r.isLattice());
        assertEquals(ss1.states().size() * ss2.states().size(),
                r.tensor().states().size());
    }

    @Test
    void tensorProductIsLattice() {
        StateSpace ss1 = build("&{a: end, b: end}");
        StateSpace ss2 = build("&{c: end}");

        TensorResult r = TensorChecker.tensorProduct(
                Map.entry("X", ss1), Map.entry("Y", ss2));

        assertTrue(LatticeChecker.checkLattice(r.tensor()).isLattice());
    }

    @Test
    void tensorLabelsAreQualified() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");

        TensorResult r = TensorChecker.tensorProduct(
                Map.entry("Obj1", ss1), Map.entry("Obj2", ss2));

        boolean hasQualified = r.tensor().transitions().stream()
                .anyMatch(t -> t.label().startsWith("Obj1.") || t.label().startsWith("Obj2."));
        assertTrue(hasQualified);
    }

    @Test
    void projectionsAreSurjective() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");

        TensorResult r = TensorChecker.tensorProduct(
                Map.entry("A", ss1), Map.entry("B", ss2));

        assertTrue(TensorChecker.checkBilinearity(r, "A"));
        assertTrue(TensorChecker.checkBilinearity(r, "B"));
    }

    @Test
    void tensorWithEnd() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace end = build("end");

        TensorResult r = TensorChecker.tensorProduct(
                Map.entry("A", ss1), Map.entry("E", end));

        // Tensor with End should have same size as ss1
        assertEquals(ss1.states().size() * end.states().size(),
                r.tensor().states().size());
    }

    @Test
    void tensorStateCounts() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end, c: end}");

        TensorResult r = TensorChecker.tensorProduct(
                Map.entry("A", ss1), Map.entry("B", ss2));

        assertEquals(2, r.stateCounts().get("A"));
        assertTrue(r.stateCounts().get("B") >= 2);
    }

    @Test
    void tensorDuplicateNameThrows() {
        StateSpace ss = build("&{a: end}");
        assertThrows(IllegalArgumentException.class,
                () -> TensorChecker.tensorProduct(
                        Map.entry("A", ss), Map.entry("A", ss)));
    }

    @Test
    void tensorLessThanTwoThrows() {
        StateSpace ss = build("&{a: end}");
        assertThrows(IllegalArgumentException.class,
                () -> TensorChecker.tensorProduct(Map.entry("A", ss)));
    }

    @Test
    void tensorOfThreeParticipants() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");
        StateSpace ss3 = build("&{c: end}");

        TensorResult r = TensorChecker.tensorProduct(
                Map.entry("A", ss1), Map.entry("B", ss2), Map.entry("C", ss3));

        assertEquals(8, r.tensor().states().size()); // 2 * 2 * 2
        assertTrue(r.isLattice());
    }

    @Test
    void tensorTopAndBottom() {
        StateSpace ss1 = build("&{a: end}");
        StateSpace ss2 = build("&{b: end}");

        TensorResult r = TensorChecker.tensorProduct(
                Map.entry("A", ss1), Map.entry("B", ss2));

        StateSpace tensor = r.tensor();
        // Top should have outgoing transitions, bottom should have none
        assertFalse(tensor.transitionsFrom(tensor.top()).isEmpty());
        assertTrue(tensor.transitionsFrom(tensor.bottom()).isEmpty());
    }

    @Test
    void tensorWithSelections() {
        StateSpace ss1 = build("+{OK: end, ERR: end}");
        StateSpace ss2 = build("&{a: end}");

        TensorResult r = TensorChecker.tensorProduct(
                Map.entry("S", ss1), Map.entry("B", ss2));

        assertTrue(r.isLattice());
        assertEquals(ss1.states().size() * ss2.states().size(),
                r.tensor().states().size());
    }
}
