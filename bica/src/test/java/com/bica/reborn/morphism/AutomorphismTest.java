package com.bica.reborn.morphism;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.morphism.Automorphism.AutomorphismGroup;
import com.bica.reborn.morphism.Automorphism.FindResult;
import com.bica.reborn.morphism.Automorphism.LatticeAutomorphism;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Automorphism}, ported from
 * {@code reticulate/tests/test_automorphism.py}.
 */
class AutomorphismTest {

    private record Built(StateSpace ss, LatticeResult lr) {}

    private static Built build(String src) {
        StateSpace ss = StateSpaceBuilder.build(Parser.parse(src));
        LatticeResult lr = LatticeChecker.checkLattice(ss);
        return new Built(ss, lr);
    }

    // ===== TestFindAutomorphisms =====

    @Test
    void endIdentity() {
        Built b = build("end");
        FindResult fr = Automorphism.findAutomorphisms(b.ss, b.lr);
        assertEquals(1, fr.automorphisms().size());
        assertTrue(fr.automorphisms().get(0).isIdentity());
    }

    @Test
    void twoChainIdentity() {
        Built b = build("&{a: end}");
        FindResult fr = Automorphism.findAutomorphisms(b.ss, b.lr);
        assertEquals(1, fr.automorphisms().size());
        assertTrue(fr.automorphisms().get(0).isIdentity());
    }

    @Test
    void symmetricBranchHasSymmetry() {
        Built b = build("&{a: end, b: end}");
        if (b.lr.isLattice()) {
            FindResult fr = Automorphism.findAutomorphisms(b.ss, b.lr);
            assertTrue(fr.automorphisms().size() >= 1);
        }
    }

    @Test
    void chain3IdentityOnly() {
        Built b = build("&{a: &{b: end}}");
        FindResult fr = Automorphism.findAutomorphisms(b.ss, b.lr);
        assertEquals(1, fr.automorphisms().size());
    }

    @Test
    void parallelHasSymmetry() {
        Built b = build("(&{a: end} || &{b: end})");
        if (b.lr.isLattice()) {
            FindResult fr = Automorphism.findAutomorphisms(b.ss, b.lr);
            // Boolean lattice 2^2 has Aut ≅ S₂
            assertTrue(fr.automorphisms().size() >= 2);
        }
    }

    @Test
    void identityAlwaysPresent() {
        for (String ts : new String[]{
                "end", "&{a: end}", "&{a: &{b: end}}", "&{a: end, b: end}"}) {
            Built b = build(ts);
            if (b.lr.isLattice()) {
                FindResult fr = Automorphism.findAutomorphisms(b.ss, b.lr);
                boolean any = fr.automorphisms().stream().anyMatch(LatticeAutomorphism::isIdentity);
                assertTrue(any, "No identity for " + ts);
            }
        }
    }

    @Test
    void nonLatticeEmpty() {
        Built b = build("&{a: end, b: rec X . &{a: X}}");
        if (!b.lr.isLattice()) {
            FindResult fr = Automorphism.findAutomorphisms(b.ss, b.lr);
            assertTrue(fr.automorphisms().isEmpty());
        }
    }

    @Test
    void automorphismHasCorrectFields() {
        Built b = build("&{a: end}");
        FindResult fr = Automorphism.findAutomorphisms(b.ss, b.lr);
        LatticeAutomorphism a = fr.automorphisms().get(0);
        assertNotNull(a.mapping());
        assertNotNull(a.fixedPoints());
        assertTrue(a.order() >= 1);
    }

    // ===== TestAutomorphismGroup =====

    @Test
    void returnsGroup() {
        Built b = build("&{a: end}");
        AutomorphismGroup g = Automorphism.automorphismGroup(b.ss, b.lr);
        assertNotNull(g);
    }

    @Test
    void trivialGroup() {
        Built b = build("end");
        AutomorphismGroup g = Automorphism.automorphismGroup(b.ss, b.lr);
        assertTrue(g.isTrivial());
        assertEquals(1, g.order());
    }

    @Test
    void chainTrivial() {
        for (String ts : new String[]{
                "&{a: end}", "&{a: &{b: end}}", "&{a: &{b: &{c: end}}}"}) {
            Built b = build(ts);
            AutomorphismGroup g = Automorphism.automorphismGroup(b.ss, b.lr);
            assertEquals(1, g.order(), "Expected trivial for " + ts);
            assertTrue(g.isTrivial());
        }
    }

    @Test
    void symmetricBranchOrderAtLeast1() {
        Built b = build("&{a: end, b: end}");
        if (b.lr.isLattice()) {
            AutomorphismGroup g = Automorphism.automorphismGroup(b.ss, b.lr);
            assertTrue(g.order() >= 1);
        }
    }

    @Test
    void parallelNontrivial() {
        Built b = build("(&{a: end} || &{b: end})");
        if (b.lr.isLattice()) {
            AutomorphismGroup g = Automorphism.automorphismGroup(b.ss, b.lr);
            assertTrue(g.order() >= 2);
        }
    }

    @Test
    void nonLatticeTrivial() {
        Built b = build("&{a: end, b: rec X . &{a: X}}");
        if (!b.lr.isLattice()) {
            AutomorphismGroup g = Automorphism.automorphismGroup(b.ss, b.lr);
            assertEquals(0, g.order());
            assertTrue(g.isTrivial());
        }
    }

    @Test
    void orbitPartitionCoversAll() {
        Built b = build("&{a: end, b: end}");
        if (b.lr.isLattice()) {
            AutomorphismGroup g = Automorphism.automorphismGroup(b.ss, b.lr);
            Set<Integer> all = new HashSet<>();
            for (var orbit : g.orbitPartition()) all.addAll(orbit);
            Set<Integer> nodesRep = b.lr.sccMap() != null && !b.lr.sccMap().isEmpty()
                    ? new HashSet<>(b.lr.sccMap().values())
                    : new HashSet<>(b.ss.states());
            assertEquals(nodesRep, all);
        }
    }

    @Test
    void fixedByAllIncludesTopBottom() {
        Built b = build("&{a: end, b: end}");
        if (b.lr.isLattice()) {
            AutomorphismGroup g = Automorphism.automorphismGroup(b.ss, b.lr);
            int topRep = b.lr.sccMap().getOrDefault(b.ss.top(), b.ss.top());
            int botRep = b.lr.sccMap().getOrDefault(b.ss.bottom(), b.ss.bottom());
            for (var a : g.automorphisms()) {
                assertEquals(topRep, a.apply(topRep));
                assertEquals(botRep, a.apply(botRep));
            }
        }
    }

    // ===== TestProperties =====

    @Test
    void orderDividesNFactorial() {
        Built b = build("&{a: end, b: end}");
        if (b.lr.isLattice()) {
            AutomorphismGroup g = Automorphism.automorphismGroup(b.ss, b.lr);
            int n = b.lr.sccMap() != null && !b.lr.sccMap().isEmpty()
                    ? new TreeSet<>(b.lr.sccMap().values()).size()
                    : b.ss.states().size();
            long fact = 1;
            for (int i = 2; i <= n; i++) fact *= i;
            assertEquals(0, fact % g.order());
        }
    }

    @Test
    void identityHasOrder1() {
        Built b = build("&{a: end}");
        FindResult fr = Automorphism.findAutomorphisms(b.ss, b.lr);
        for (var a : fr.automorphisms()) {
            if (a.isIdentity()) assertEquals(1, a.order());
        }
    }

    @Test
    void nonIdentityOrderAtLeast2() {
        Built b = build("(&{a: end} || &{b: end})");
        if (b.lr.isLattice()) {
            FindResult fr = Automorphism.findAutomorphisms(b.ss, b.lr);
            for (var a : fr.automorphisms()) {
                if (!a.isIdentity()) assertTrue(a.order() >= 2);
            }
        }
    }

    // ===== TestBenchmarks =====

    @ParameterizedTest(name = "benchmark[{0}]")
    @CsvSource({
            "end,                          end",
            "single,                       '&{a: end}'",
            "branch2,                      '&{a: end, b: end}'",
            "chain3,                       '&{a: &{b: end}}'",
            "select2,                      '+{ok: end, err: end}'",
            "parallel,                     '(&{a: end} || &{b: end})'"
    })
    void benchmark(String name, String typeStr) {
        Built b = build(typeStr);
        if (b.lr.isLattice()) {
            AutomorphismGroup g = Automorphism.automorphismGroup(b.ss, b.lr);
            assertTrue(g.order() >= 1);
            assertTrue(g.automorphisms().stream().anyMatch(LatticeAutomorphism::isIdentity));
        }
    }
}
