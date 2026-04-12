package com.bica.reborn.species;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.species.SpeciesChecker.*;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for combinatorial species analysis (port of test_species.py).
 *
 * <p>Covers automorphism group, isomorphism types, type-generating series,
 * cycle index, species product/sum, exponential formula, symmetry factor,
 * and benchmark protocols.
 */
class SpeciesCheckerTest {

    private static StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // =====================================================================
    // Automorphism group
    // =====================================================================

    @Test
    void endAutomorphism() {
        var auts = SpeciesChecker.findAutomorphisms(build("end"));
        assertEquals(1, auts.size());
    }

    @Test
    void singleBranchAutomorphism() {
        var auts = SpeciesChecker.findAutomorphisms(build("&{a: end}"));
        assertEquals(1, auts.size());
    }

    @Test
    void automorphismIsValid() {
        StateSpace ss = build("&{a: end, b: end}");
        var auts = SpeciesChecker.findAutomorphisms(ss);
        for (var aut : auts) {
            assertEquals(ss.states(), aut.keySet());
            assertEquals(ss.states(), new HashSet<>(aut.values()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"end", "&{a: end}", "&{a: end, b: end}", "+{a: end}"})
    void identityAlwaysPresent(String type) {
        StateSpace ss = build(type);
        var auts = SpeciesChecker.findAutomorphisms(ss);
        Map<Integer, Integer> identity = new HashMap<>();
        for (int s : ss.states()) identity.put(s, s);
        assertTrue(auts.contains(identity));
    }

    @Test
    void symmetricBranchHasSwap() {
        var auts = SpeciesChecker.findAutomorphisms(build("&{a: end, b: end}"));
        assertTrue(auts.size() >= 1);
    }

    @Test
    void chainNoNontrivial() {
        var auts = SpeciesChecker.findAutomorphisms(build("&{a: &{b: end}}"));
        assertEquals(1, auts.size());
    }

    @Test
    void automorphismGroupSizeMatchesList() {
        StateSpace ss = build("&{a: end, b: end}");
        assertEquals(
                SpeciesChecker.findAutomorphisms(ss).size(),
                SpeciesChecker.automorphismGroupSize(ss));
    }

    // =====================================================================
    // Isomorphism types
    // =====================================================================

    @Test
    void endTypes() {
        var types = SpeciesChecker.isomorphismTypes(build("end"));
        assertTrue(types.size() >= 1);
    }

    @Test
    void singleBranchTypes() {
        var types = SpeciesChecker.isomorphismTypes(build("&{a: end}"));
        assertTrue(types.size() >= 2); // point + chain
    }

    @Test
    void typesAreSets() {
        var types = SpeciesChecker.isomorphismTypes(build("&{a: end, b: end}"));
        for (var t : types) {
            assertInstanceOf(Set.class, t);
        }
    }

    @Test
    void typesNonempty() {
        var types = SpeciesChecker.isomorphismTypes(build("&{a: end, b: end}"));
        for (var t : types) {
            assertFalse(t.isEmpty());
        }
    }

    @Test
    void moreStatesMoreTypes() {
        int tSmall = SpeciesChecker.isomorphismTypes(build("&{a: end}")).size();
        int tLarge = SpeciesChecker.isomorphismTypes(build("&{a: end, b: end}")).size();
        assertTrue(tLarge >= tSmall);
    }

    // =====================================================================
    // Type-generating series
    // =====================================================================

    @Test
    void endSeries() {
        var tgs = SpeciesChecker.typeGeneratingSeries(build("end"));
        assertEquals(1.0, tgs.get(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"end", "&{a: end}", "&{a: end, b: end}"})
    void seriesA0Is1(String type) {
        var tgs = SpeciesChecker.typeGeneratingSeries(build(type));
        assertEquals(1.0, tgs.get(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"end", "&{a: end}", "&{a: end, b: end}"})
    void seriesA1Is1(String type) {
        var tgs = SpeciesChecker.typeGeneratingSeries(build(type));
        if (tgs.size() > 1) {
            assertEquals(1.0, tgs.get(1));
        }
    }

    @Test
    void seriesNonnegative() {
        var tgs = SpeciesChecker.typeGeneratingSeries(build("&{a: end, b: end}"));
        for (double c : tgs) {
            assertTrue(c >= 0);
        }
    }

    @Test
    void seriesLength() {
        StateSpace ss = build("&{a: end, b: end}");
        var tgs = SpeciesChecker.typeGeneratingSeries(ss);
        assertTrue(tgs.size() <= ss.states().size() + 1);
    }

    // =====================================================================
    // Cycle index
    // =====================================================================

    @Test
    void endCycleIndex() {
        var ci = SpeciesChecker.cycleIndex(build("end"));
        assertTrue(ci.size() >= 1);
        double total = ci.stream().mapToDouble(CycleIndexTerm::coefficient).sum();
        assertEquals(1.0, total, 1e-9);
    }

    @ParameterizedTest
    @ValueSource(strings = {"end", "&{a: end}", "&{a: end, b: end}", "+{a: end}"})
    void coefficientsSumTo1(String type) {
        var ci = SpeciesChecker.cycleIndex(build(type));
        double total = ci.stream().mapToDouble(CycleIndexTerm::coefficient).sum();
        assertEquals(1.0, total, 1e-9);
    }

    @Test
    void partitionsValid() {
        StateSpace ss = build("&{a: end, b: end}");
        int n = ss.states().size();
        var ci = SpeciesChecker.cycleIndex(ss);
        for (var term : ci) {
            int partSum = term.partition().stream().mapToInt(Integer::intValue).sum();
            assertEquals(n, partSum);
        }
    }

    @Test
    void coefficientsPositive() {
        var ci = SpeciesChecker.cycleIndex(build("&{a: end}"));
        for (var term : ci) {
            assertTrue(term.coefficient() > 0);
        }
    }

    // =====================================================================
    // Species product
    // =====================================================================

    @Test
    void productStates() {
        var result = SpeciesChecker.speciesProduct(build("&{a: end}"), build("&{b: end}"));
        assertEquals(result.leftStates() * result.rightStates(), result.productStates());
    }

    @Test
    void productAutomorphismsUpperBound() {
        var result = SpeciesChecker.speciesProduct(build("&{a: end}"), build("&{b: end}"));
        assertEquals(
                result.leftAutomorphisms() * result.rightAutomorphisms(),
                result.productAutomorphismsUpper());
    }

    @Test
    void productSymmetric() {
        var r1 = SpeciesChecker.speciesProduct(build("&{a: end}"), build("&{b: end, c: end}"));
        var r2 = SpeciesChecker.speciesProduct(build("&{b: end, c: end}"), build("&{a: end}"));
        assertEquals(r1.productStates(), r2.productStates());
    }

    // =====================================================================
    // Species sum
    // =====================================================================

    @Test
    void sumStates() {
        var result = SpeciesChecker.speciesSum(build("&{a: end}"), build("&{b: end}"));
        assertEquals(result.leftStates() + result.rightStates(), result.sumStates());
    }

    // =====================================================================
    // Exponential formula
    // =====================================================================

    @Test
    void endExpTerms() {
        var terms = SpeciesChecker.exponentialFormulaTerms(build("end"));
        assertTrue(terms.size() >= 1);
    }

    @Test
    void termsLength() {
        StateSpace ss = build("&{a: end}");
        var tgs = SpeciesChecker.typeGeneratingSeries(ss);
        var terms = SpeciesChecker.exponentialFormulaTerms(ss);
        assertEquals(tgs.size(), terms.size());
    }

    // =====================================================================
    // Symmetry factor
    // =====================================================================

    @Test
    void endSymmetry() {
        assertEquals(1.0, SpeciesChecker.symmetryFactor(build("end")), 1e-9);
    }

    @Test
    void chainSymmetry() {
        StateSpace ss = build("&{a: &{b: end}}");
        int n = ss.states().size();
        double expected = 1.0 / factorial(n);
        assertEquals(expected, SpeciesChecker.symmetryFactor(ss), 1e-9);
    }

    @ParameterizedTest
    @ValueSource(strings = {"end", "&{a: end}", "&{a: end, b: end}"})
    void symmetryBounded(String type) {
        double sf = SpeciesChecker.symmetryFactor(build(type));
        assertTrue(sf > 0 && sf <= 1.0);
    }

    // =====================================================================
    // Full analysis
    // =====================================================================

    @Test
    void endAnalysis() {
        var result = SpeciesChecker.analyzeSpecies(build("end"));
        assertEquals(1, result.numStates());
        assertEquals(1, result.numAutomorphisms());
        assertEquals(1.0, result.symmetryFactor());
    }

    @Test
    void singleBranchAnalysis() {
        var result = SpeciesChecker.analyzeSpecies(build("&{a: end}"));
        assertEquals(2, result.numStates());
        assertTrue(result.numAutomorphisms() >= 1);
        assertTrue(result.typeGeneratingCoefficients().size() >= 1);
    }

    @Test
    void twoBranchAnalysis() {
        var result = SpeciesChecker.analyzeSpecies(build("&{a: end, b: end}"));
        assertEquals(2, result.numStates());
        assertTrue(result.isomorphismTypeCount() >= 1);
    }

    @Test
    void selectionAnalysis() {
        var result = SpeciesChecker.analyzeSpecies(build("+{a: end, b: end}"));
        assertTrue(result.numAutomorphisms() >= 1);
    }

    @Test
    void nestedAnalysis() {
        var result = SpeciesChecker.analyzeSpecies(build("&{a: &{c: end}, b: end}"));
        assertTrue(result.numStates() >= 3);
    }

    @Test
    void recursiveAnalysis() {
        var result = SpeciesChecker.analyzeSpecies(build("rec X . &{a: X, b: end}"));
        assertTrue(result.numStates() >= 1);
    }

    // =====================================================================
    // Benchmark protocols
    // =====================================================================

    @Test
    void iteratorProtocol() {
        var result = SpeciesChecker.analyzeSpecies(
                build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"));
        assertTrue(result.numStates() >= 2);
        assertTrue(result.numAutomorphisms() >= 1);
    }

    @Test
    void simpleResource() {
        var result = SpeciesChecker.analyzeSpecies(
                build("&{open: &{use: &{close: end}}}"));
        assertEquals(4, result.numStates());
        assertEquals(1, result.numAutomorphisms()); // chain, no symmetry
    }

    @Test
    void parallelSimple() {
        var result = SpeciesChecker.analyzeSpecies(
                build("(&{a: end} || &{b: end})"));
        assertTrue(result.numStates() >= 4);
    }

    @Test
    void binaryChoice() {
        var result = SpeciesChecker.analyzeSpecies(
                build("+{left: end, right: end}"));
        assertTrue(result.numStates() >= 2);
    }

    @Test
    void deepChain() {
        var result = SpeciesChecker.analyzeSpecies(
                build("&{a: &{b: &{c: &{d: end}}}}"));
        assertEquals(1, result.numAutomorphisms());
    }

    // =====================================================================
    // Partition from permutation (internal utility)
    // =====================================================================

    @Test
    void identityPartition() {
        // Identity on 3 elements: three fixed points -> [1, 1, 1]
        Map<Integer, Integer> id = Map.of(0, 0, 1, 1, 2, 2);
        var partition = SpeciesChecker.partitionFromPermutation(id);
        assertEquals(List.of(1, 1, 1), partition);
    }

    @Test
    void singleCyclePartition() {
        // (0 -> 1 -> 2 -> 0): one 3-cycle -> [3]
        Map<Integer, Integer> perm = Map.of(0, 1, 1, 2, 2, 0);
        var partition = SpeciesChecker.partitionFromPermutation(perm);
        assertEquals(List.of(3), partition);
    }

    @Test
    void transpositionPartition() {
        // (0 -> 1, 1 -> 0, 2 -> 2): [2, 1]
        Map<Integer, Integer> perm = Map.of(0, 1, 1, 0, 2, 2);
        var partition = SpeciesChecker.partitionFromPermutation(perm);
        assertEquals(List.of(2, 1), partition);
    }

    // =====================================================================
    // Additional parity tests
    // =====================================================================

    // Note: Java parser does not support &{} or +{} (empty branch/selection),
    // unlike the Python parser. Skipped for parity.

    // =====================================================================
    // Helpers
    // =====================================================================

    private static long factorial(int n) {
        long f = 1;
        for (int i = 2; i <= n; i++) f *= i;
        return f;
    }
}
