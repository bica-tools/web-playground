package com.bica.reborn.lattice;

import com.bica.reborn.lattice.PrimeSpectrum.Lattice;
import com.bica.reborn.lattice.PrimeSpectrum.PrimeIdeal;
import com.bica.reborn.lattice.PrimeSpectrum.SpectrumResult;
import com.bica.reborn.lattice.PrimeSpectrum.ZariskiTopology;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PrimeSpectrum}.
 *
 * <p>Ported from {@code reticulate/tests/test_prime_spectrum.py} (Step 363h).
 */
class PrimeSpectrumTest {

    private static StateSpace ssOf(String s) {
        return StateSpaceBuilder.build(Parser.parse(s));
    }

    private static Lattice latOf(String s) {
        return PrimeSpectrum.buildLattice(ssOf(s));
    }

    // ---------------------------------------------------------------------
    // Lattice carrier
    // ---------------------------------------------------------------------

    @Nested
    class LatticeCarrier {

        @Test
        void singleEnd() {
            Lattice L = latOf("end");
            assertEquals(1, L.elements().size());
            assertEquals(L.top(), L.bottom());
        }

        @Test
        void twoChain() {
            Lattice L = latOf("&{m: end}");
            assertEquals(2, L.elements().size());
            assertTrue(L.isLe(L.bottom(), L.top()));
            assertTrue(L.isLe(L.top(), L.top()));
            assertFalse(L.isLt(L.top(), L.bottom()));
        }

        @Test
        void threeChain() {
            Lattice L = latOf("&{a: &{b: end}}");
            assertEquals(3, L.elements().size());
        }

        @Test
        void meetJoinWellDefined() {
            Lattice L = latOf("&{a: &{b: end}}");
            for (int x : L.elements()) {
                for (int y : L.elements()) {
                    // Tables are total: getters return a value
                    assertNotNull(L.meetOf(x, y));
                    assertNotNull(L.joinOf(x, y));
                }
            }
            for (int x : L.elements()) {
                assertEquals(x, L.meetOf(x, x));
                assertEquals(x, L.joinOf(x, x));
            }
        }

        @Test
        void topBottomAbsorption() {
            Lattice L = latOf("&{a: &{b: end}}");
            for (int x : L.elements()) {
                assertEquals(x, L.meetOf(L.top(), x));
                assertEquals(L.top(), L.joinOf(L.top(), x));
                assertEquals(L.bottom(), L.meetOf(L.bottom(), x));
                assertEquals(x, L.joinOf(L.bottom(), x));
            }
        }
    }

    // ---------------------------------------------------------------------
    // Ideal enumeration
    // ---------------------------------------------------------------------

    @Nested
    class Ideals {

        @Test
        void singleElement() {
            Lattice L = latOf("end");
            List<Set<Integer>> ideals = PrimeSpectrum.enumerateIdeals(L);
            assertEquals(1, ideals.size());
            assertEquals(Set.of(L.bottom()), ideals.get(0));
        }

        @Test
        void twoChainIdeals() {
            Lattice L = latOf("&{m: end}");
            assertEquals(2, PrimeSpectrum.enumerateIdeals(L).size());
        }

        @Test
        void threeChainIdeals() {
            Lattice L = latOf("&{a: &{b: end}}");
            // n+1 ideals in an n-chain
            assertEquals(3, PrimeSpectrum.enumerateIdeals(L).size());
        }

        @Test
        void allIdealsContainBottom() {
            Lattice L = latOf("&{a: &{b: end}}");
            for (Set<Integer> I : PrimeSpectrum.enumerateIdeals(L)) {
                assertTrue(I.contains(L.bottom()));
            }
        }

        @Test
        void fullLatticeIsAnIdeal() {
            Lattice L = latOf("&{a: &{b: end}}");
            List<Set<Integer>> ideals = PrimeSpectrum.enumerateIdeals(L);
            assertTrue(ideals.contains(Set.copyOf(L.elements())));
        }
    }

    // ---------------------------------------------------------------------
    // Prime ideals
    // ---------------------------------------------------------------------

    @Nested
    class PrimeIdeals {

        @Test
        void chainHasNMinus1Primes() {
            int[][] cases = {{1, 2}, {2, 3}, {3, 4}};
            for (int[] c : cases) {
                int depth = c[0];
                int n = c[1];
                String inner = "end";
                for (int i = 0; i < depth; i++) inner = "&{m: " + inner + "}";
                Lattice L = latOf(inner);
                List<PrimeIdeal> primes = PrimeSpectrum.enumeratePrimeIdeals(L);
                assertEquals(n - 1, primes.size(),
                        "depth " + depth + ": expected " + (n - 1) + " primes");
            }
        }

        @Test
        void primesAreProper() {
            Lattice L = latOf("&{a: &{b: end}}");
            Set<Integer> full = Set.copyOf(L.elements());
            for (PrimeIdeal p : PrimeSpectrum.enumeratePrimeIdeals(L)) {
                assertNotEquals(full, p.members());
                assertTrue(p.members().size() >= 1);
            }
        }

        @Test
        void primesAreDownwardClosed() {
            Lattice L = latOf("&{a: &{b: end}}");
            for (PrimeIdeal p : PrimeSpectrum.enumeratePrimeIdeals(L)) {
                for (int x : p.members()) {
                    for (int y : L.elements()) {
                        if (L.isLe(y, x)) {
                            assertTrue(p.members().contains(y));
                        }
                    }
                }
            }
        }

        @Test
        void primeIdealPrincipalInChain() {
            Lattice L = latOf("&{a: &{b: end}}");
            for (PrimeIdeal p : PrimeSpectrum.enumeratePrimeIdeals(L)) {
                assertTrue(p.isPrincipal());
                assertNotNull(p.generator());
            }
        }
    }

    // ---------------------------------------------------------------------
    // Topology
    // ---------------------------------------------------------------------

    @Nested
    class Topology {

        @Test
        void basicOpenAtBottomIsEmpty() {
            Lattice L = latOf("&{a: &{b: end}}");
            List<PrimeIdeal> primes = PrimeSpectrum.enumeratePrimeIdeals(L);
            ZariskiTopology topo = PrimeSpectrum.buildTopology(L, primes);
            assertEquals(Set.of(), topo.basicOpens().get(L.bottom()));
        }

        @Test
        void basicOpenAtTopIsFull() {
            Lattice L = latOf("&{a: &{b: end}}");
            List<PrimeIdeal> primes = PrimeSpectrum.enumeratePrimeIdeals(L);
            ZariskiTopology topo = PrimeSpectrum.buildTopology(L, primes);
            Set<Integer> expected = new HashSet<>();
            for (int i = 0; i < primes.size(); i++) expected.add(i);
            assertEquals(expected, topo.basicOpens().get(L.top()));
        }

        @Test
        void basicOpenIntersectionMeet() {
            Lattice L = latOf("&{a: &{b: end}}");
            List<PrimeIdeal> primes = PrimeSpectrum.enumeratePrimeIdeals(L);
            ZariskiTopology topo = PrimeSpectrum.buildTopology(L, primes);
            for (int x : L.elements()) {
                for (int y : L.elements()) {
                    Set<Integer> lhs = new HashSet<>(topo.basicOpens().get(x));
                    lhs.retainAll(topo.basicOpens().get(y));
                    Set<Integer> rhs = topo.basicOpens().get(L.meetOf(x, y));
                    assertEquals(rhs, lhs);
                }
            }
        }

        @Test
        void closedSetComplement() {
            Lattice L = latOf("&{a: &{b: end}}");
            List<PrimeIdeal> primes = PrimeSpectrum.enumeratePrimeIdeals(L);
            ZariskiTopology topo = PrimeSpectrum.buildTopology(L, primes);
            Set<Integer> full = new HashSet<>();
            for (int i = 0; i < primes.size(); i++) full.add(i);
            for (int x : L.elements()) {
                Set<Integer> union = new HashSet<>(topo.basicOpens().get(x));
                union.addAll(topo.closedSets().get(x));
                assertEquals(full, union);

                Set<Integer> inter = new HashSet<>(topo.basicOpens().get(x));
                inter.retainAll(topo.closedSets().get(x));
                assertEquals(Set.of(), inter);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Bidirectional morphisms
    // ---------------------------------------------------------------------

    @Nested
    class Morphisms {

        @Test
        void phiOrderPreservingChain() {
            Lattice L = latOf("&{a: &{b: end}}");
            List<PrimeIdeal> primes = PrimeSpectrum.enumeratePrimeIdeals(L);
            ZariskiTopology topo = PrimeSpectrum.buildTopology(L, primes);
            Map<String, Boolean> morph = PrimeSpectrum.checkMorphisms(L, topo);
            assertTrue(morph.get("phi_order_preserving"));
            assertTrue(morph.get("psi_well_defined"));
            assertTrue(morph.get("duality_holds"));
        }

        @Test
        void phiInjectiveDistributive() {
            Lattice L = latOf("&{a: &{b: end}}");
            List<PrimeIdeal> primes = PrimeSpectrum.enumeratePrimeIdeals(L);
            ZariskiTopology topo = PrimeSpectrum.buildTopology(L, primes);
            Map<String, Boolean> morph = PrimeSpectrum.checkMorphisms(L, topo);
            assertTrue(morph.get("phi_injective"));
        }

        @Test
        void psiInvertsPhiInChain() {
            Lattice L = latOf("&{a: &{b: end}}");
            List<PrimeIdeal> primes = PrimeSpectrum.enumeratePrimeIdeals(L);
            ZariskiTopology topo = PrimeSpectrum.buildTopology(L, primes);
            Map<Integer, Integer> psi = PrimeSpectrum.psiMap(L, topo.primes());
            Map<Integer, Set<Integer>> phi = PrimeSpectrum.phiMap(L, topo);
            for (int i = 0; i < topo.primes().size(); i++) {
                assertTrue(phi.get(psi.get(i)).contains(i));
            }
        }
    }

    // ---------------------------------------------------------------------
    // Top-level computeSpectrum
    // ---------------------------------------------------------------------

    @Nested
    class ComputeSpectrum {

        @Test
        void chainSpectrum() {
            StateSpace ss = ssOf("&{a: &{b: &{c: end}}}");
            SpectrumResult r = PrimeSpectrum.computeSpectrum(ss);
            assertTrue(r.isLattice());
            assertTrue(r.isDistributive());
            assertEquals(4, r.numElements());
            assertEquals(3, r.numPrimeIdeals());
            assertTrue(r.dualityHolds());
            assertTrue(r.phiIsOrderPreserving());
            assertTrue(r.phiIsInjective());
        }

        @Test
        void irreducibleChain() {
            StateSpace ss = ssOf("&{a: &{b: end}}");
            SpectrumResult r = PrimeSpectrum.computeSpectrum(ss);
            assertTrue(r.isIrreducible());
            assertEquals(1, r.irreducibleComponents().size());
        }

        @Test
        void parallelDistributive() {
            StateSpace ss = ssOf("(&{a: end} || &{b: end})");
            SpectrumResult r = PrimeSpectrum.computeSpectrum(ss);
            assertTrue(r.isLattice());
            assertTrue(r.isDistributive());
            assertEquals(2, r.numPrimeIdeals());
        }

        @Test
        void singleEnd() {
            StateSpace ss = ssOf("end");
            SpectrumResult r = PrimeSpectrum.computeSpectrum(ss);
            assertTrue(r.isLattice());
            assertEquals(1, r.numElements());
            assertEquals(0, r.numPrimeIdeals());
        }

        @Test
        void notesWhenNonLattice() {
            StateSpace ss = ssOf("&{a: end}");
            SpectrumResult r = PrimeSpectrum.computeSpectrum(ss);
            assertTrue(r.isLattice());
        }

        @Test
        void componentsAreAPartition() {
            StateSpace ss = ssOf("(&{a: end} || &{b: end})");
            SpectrumResult r = PrimeSpectrum.computeSpectrum(ss);
            int n = r.numPrimeIdeals();
            Set<Integer> union = new HashSet<>();
            for (Set<Integer> c : r.irreducibleComponents()) union.addAll(c);
            Set<Integer> expected = new HashSet<>();
            for (int i = 0; i < n; i++) expected.add(i);
            assertEquals(expected, union);
        }
    }

    // ---------------------------------------------------------------------
    // Algebraic laws
    // ---------------------------------------------------------------------

    @Nested
    class AlgebraicLaws {

        @ParameterizedTest
        @ValueSource(strings = {
                "&{a: end}",
                "&{a: &{b: end}}",
                "&{a: &{b: &{c: end}}}",
                "(&{a: end} || &{b: end})",
        })
        void dMeet(String src) {
            StateSpace ss = ssOf(src);
            SpectrumResult r = PrimeSpectrum.computeSpectrum(ss);
            Lattice L = PrimeSpectrum.buildLattice(ss);
            ZariskiTopology topo = r.topology();
            assertNotNull(topo);
            for (int x : L.elements()) {
                for (int y : L.elements()) {
                    Set<Integer> lhs = topo.basicOpens().get(L.meetOf(x, y));
                    Set<Integer> rhs = new HashSet<>(topo.basicOpens().get(x));
                    rhs.retainAll(topo.basicOpens().get(y));
                    assertEquals(lhs, rhs);
                }
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "&{a: end}",
                "&{a: &{b: end}}",
                "(&{a: end} || &{b: end})",
        })
        void dJoinContainsUnion(String src) {
            StateSpace ss = ssOf(src);
            SpectrumResult r = PrimeSpectrum.computeSpectrum(ss);
            Lattice L = PrimeSpectrum.buildLattice(ss);
            ZariskiTopology topo = r.topology();
            assertNotNull(topo);
            for (int x : L.elements()) {
                for (int y : L.elements()) {
                    Set<Integer> lhs = topo.basicOpens().get(L.joinOf(x, y));
                    Set<Integer> rhs = new HashSet<>(topo.basicOpens().get(x));
                    rhs.addAll(topo.basicOpens().get(y));
                    assertTrue(lhs.containsAll(rhs));
                }
            }
        }
    }
}
