package com.bica.reborn.prime_spectrum;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.prime_spectrum.PrimeSpectrumChecker.*;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PrimeSpectrumChecker (Step 363h).
 * Mirrors reticulate/tests/test_prime_spectrum.py.
 */
class PrimeSpectrumCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    @Nested
    class TestComputeSpectrum {

        @Test
        void chainSpectrum() {
            var r = PrimeSpectrumChecker.computeSpectrum(build("&{a: &{b: &{c: end}}}"));
            assertTrue(r.isLattice());
            assertEquals(4, r.numElements());
            assertEquals(3, r.numPrimeIdeals());
            assertTrue(r.dualityHolds());
            assertTrue(r.phiIsOrderPreserving());
            assertTrue(r.phiIsInjective());
        }

        @Test
        void twoChain() {
            var r = PrimeSpectrumChecker.computeSpectrum(build("&{m: end}"));
            assertTrue(r.isLattice());
            assertEquals(2, r.numElements());
            assertEquals(1, r.numPrimeIdeals());
            assertTrue(r.dualityHolds());
        }

        @Test
        void singleEnd() {
            var r = PrimeSpectrumChecker.computeSpectrum(build("end"));
            assertTrue(r.isLattice());
            assertEquals(1, r.numElements());
            assertEquals(0, r.numPrimeIdeals());
        }

        @Test
        void parallelDistributive() {
            var r = PrimeSpectrumChecker.computeSpectrum(
                    build("(&{a: end} || &{b: end})"));
            assertTrue(r.isLattice());
            assertEquals(2, r.numPrimeIdeals());
        }

        @Test
        void irreducibleChain() {
            var r = PrimeSpectrumChecker.computeSpectrum(build("&{a: &{b: end}}"));
            assertTrue(r.isIrreducible());
            assertEquals(1, r.irreducibleComponents().size());
        }

        @Test
        void componentsPartitionPrimes() {
            var r = PrimeSpectrumChecker.computeSpectrum(
                    build("(&{a: end} || &{b: end})"));
            Set<Integer> union = new HashSet<>();
            for (Set<Integer> c : r.irreducibleComponents()) union.addAll(c);
            Set<Integer> expected = new HashSet<>();
            for (int i = 0; i < r.numPrimeIdeals(); i++) expected.add(i);
            assertEquals(expected, union);
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    class TestIdeals {

        @Test
        void twoChainIdeals() {
            var ss = build("&{m: end}");
            var L = PrimeSpectrumChecker.buildLattice(ss);
            var ideals = PrimeSpectrumChecker.enumerateIdeals(L);
            assertEquals(2, ideals.size());
        }

        @Test
        void threeChainIdeals() {
            var L = PrimeSpectrumChecker.buildLattice(build("&{a: &{b: end}}"));
            var ideals = PrimeSpectrumChecker.enumerateIdeals(L);
            assertEquals(3, ideals.size());
        }

        @Test
        void idealsContainBottom() {
            var L = PrimeSpectrumChecker.buildLattice(build("&{a: &{b: end}}"));
            for (Set<Integer> I : PrimeSpectrumChecker.enumerateIdeals(L)) {
                assertTrue(I.contains(L.bottomIdx));
            }
        }

        @Test
        void singleElementOneIdeal() {
            var L = PrimeSpectrumChecker.buildLattice(build("end"));
            assertEquals(1, PrimeSpectrumChecker.enumerateIdeals(L).size());
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    class TestPrimeIdeals {

        @Test
        void chainHasNMinusOnePrimes() {
            Object[][] cases = {
                    {"&{m: end}", 1},
                    {"&{a: &{b: end}}", 2},
                    {"&{a: &{b: &{c: end}}}", 3},
            };
            for (var c : cases) {
                var L = PrimeSpectrumChecker.buildLattice(build((String) c[0]));
                var ideals = PrimeSpectrumChecker.enumerateIdeals(L);
                var primes = PrimeSpectrumChecker.enumeratePrimeIdeals(L, ideals);
                assertEquals(c[1], primes.size(), "case " + c[0]);
            }
        }

        @Test
        void primesAreProper() {
            var L = PrimeSpectrumChecker.buildLattice(build("&{a: &{b: end}}"));
            var primes = PrimeSpectrumChecker.enumeratePrimeIdeals(
                    L, PrimeSpectrumChecker.enumerateIdeals(L));
            for (PrimeIdeal p : primes) {
                assertNotEquals(L.size(), p.members().size());
                assertTrue(p.members().size() >= 1);
            }
        }

        @Test
        void chainPrimesArePrincipal() {
            var L = PrimeSpectrumChecker.buildLattice(build("&{a: &{b: end}}"));
            var primes = PrimeSpectrumChecker.enumeratePrimeIdeals(
                    L, PrimeSpectrumChecker.enumerateIdeals(L));
            for (PrimeIdeal p : primes) {
                assertTrue(p.isPrincipal());
                assertNotNull(p.generator());
            }
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    class TestTopology {

        @Test
        void basicOpenAtBottomIsEmpty() {
            var L = PrimeSpectrumChecker.buildLattice(build("&{a: &{b: end}}"));
            var primes = PrimeSpectrumChecker.enumeratePrimeIdeals(
                    L, PrimeSpectrumChecker.enumerateIdeals(L));
            var topo = PrimeSpectrumChecker.buildTopology(L, primes);
            assertEquals(Set.of(), topo.basicOpens().get(L.bottomIdx));
        }

        @Test
        void basicOpenAtTopIsFull() {
            var L = PrimeSpectrumChecker.buildLattice(build("&{a: &{b: end}}"));
            var primes = PrimeSpectrumChecker.enumeratePrimeIdeals(
                    L, PrimeSpectrumChecker.enumerateIdeals(L));
            var topo = PrimeSpectrumChecker.buildTopology(L, primes);
            Set<Integer> full = new HashSet<>();
            for (int i = 0; i < primes.size(); i++) full.add(i);
            assertEquals(full, topo.basicOpens().get(L.topIdx));
        }

        @Test
        void basicOpenIntersectionIsMeet() {
            var L = PrimeSpectrumChecker.buildLattice(build("&{a: &{b: end}}"));
            var primes = PrimeSpectrumChecker.enumeratePrimeIdeals(
                    L, PrimeSpectrumChecker.enumerateIdeals(L));
            var topo = PrimeSpectrumChecker.buildTopology(L, primes);
            for (int x = 0; x < L.size(); x++) {
                for (int y = 0; y < L.size(); y++) {
                    Set<Integer> lhs = new HashSet<>(topo.basicOpens().get(x));
                    lhs.retainAll(topo.basicOpens().get(y));
                    Set<Integer> rhs = topo.basicOpens().get(L.meet[x][y]);
                    assertEquals(rhs, lhs);
                }
            }
        }

        @Test
        void closedSetsAreComplements() {
            var L = PrimeSpectrumChecker.buildLattice(build("&{a: &{b: end}}"));
            var primes = PrimeSpectrumChecker.enumeratePrimeIdeals(
                    L, PrimeSpectrumChecker.enumerateIdeals(L));
            var topo = PrimeSpectrumChecker.buildTopology(L, primes);
            Set<Integer> full = new HashSet<>();
            for (int i = 0; i < primes.size(); i++) full.add(i);
            for (int a = 0; a < L.size(); a++) {
                Set<Integer> u = new HashSet<>(topo.basicOpens().get(a));
                u.addAll(topo.closedSets().get(a));
                assertEquals(full, u);
                Set<Integer> i = new HashSet<>(topo.basicOpens().get(a));
                i.retainAll(topo.closedSets().get(a));
                assertEquals(Set.of(), i);
            }
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    class TestMorphisms {

        @Test
        void phiOrderPreservingOnChain() {
            var r = PrimeSpectrumChecker.computeSpectrum(build("&{a: &{b: end}}"));
            assertTrue(r.phiIsOrderPreserving());
            assertTrue(r.psiIsWellDefined());
            assertTrue(r.dualityHolds());
        }

        @Test
        void phiInjectiveOnDistributive() {
            var r = PrimeSpectrumChecker.computeSpectrum(build("&{a: &{b: end}}"));
            assertTrue(r.phiIsInjective());
        }

        @Test
        void psiInvertsPhiRoundTripOnChain() {
            var ss = build("&{a: &{b: end}}");
            var L = PrimeSpectrumChecker.buildLattice(ss);
            var primes = PrimeSpectrumChecker.enumeratePrimeIdeals(
                    L, PrimeSpectrumChecker.enumerateIdeals(L));
            var topo = PrimeSpectrumChecker.buildTopology(L, primes);
            var phi = PrimeSpectrumChecker.phiMap(L, topo);
            var psi = PrimeSpectrumChecker.psiMap(L, primes);
            for (int i = 0; i < primes.size(); i++) {
                assertTrue(phi.get(psi.get(i)).contains(i));
            }
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    class TestAlgebraicLaws {

        @Test
        void meetIdentityHoldsAcrossBenchmarks() {
            String[] sources = {
                    "&{a: end}",
                    "&{a: &{b: end}}",
                    "&{a: &{b: &{c: end}}}",
                    "(&{a: end} || &{b: end})",
            };
            for (String src : sources) {
                var ss = build(src);
                var L = PrimeSpectrumChecker.buildLattice(ss);
                var primes = PrimeSpectrumChecker.enumeratePrimeIdeals(
                        L, PrimeSpectrumChecker.enumerateIdeals(L));
                var topo = PrimeSpectrumChecker.buildTopology(L, primes);
                for (int x = 0; x < L.size(); x++) {
                    for (int y = 0; y < L.size(); y++) {
                        Set<Integer> lhs = new HashSet<>(topo.basicOpens().get(x));
                        lhs.retainAll(topo.basicOpens().get(y));
                        Set<Integer> rhs = topo.basicOpens().get(L.meet[x][y]);
                        assertEquals(rhs, lhs, "src=" + src);
                    }
                }
            }
        }

        @Test
        void joinInclusionHolds() {
            String[] sources = {
                    "&{a: end}",
                    "&{a: &{b: end}}",
                    "(&{a: end} || &{b: end})",
            };
            for (String src : sources) {
                var ss = build(src);
                var L = PrimeSpectrumChecker.buildLattice(ss);
                var primes = PrimeSpectrumChecker.enumeratePrimeIdeals(
                        L, PrimeSpectrumChecker.enumerateIdeals(L));
                var topo = PrimeSpectrumChecker.buildTopology(L, primes);
                for (int x = 0; x < L.size(); x++) {
                    for (int y = 0; y < L.size(); y++) {
                        Set<Integer> rhs = new HashSet<>(topo.basicOpens().get(x));
                        rhs.addAll(topo.basicOpens().get(y));
                        Set<Integer> lhs = topo.basicOpens().get(L.join[x][y]);
                        assertTrue(lhs.containsAll(rhs), "src=" + src);
                    }
                }
            }
        }
    }
}
