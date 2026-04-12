package com.bica.reborn.grothendieck;

import com.bica.reborn.grothendieck.GrothendieckChecker.GrothendieckGroup;
import com.bica.reborn.grothendieck.GrothendieckChecker.GrothendieckResult;
import com.bica.reborn.grothendieck.GrothendieckChecker.LatticeSig;
import com.bica.reborn.grothendieck.GrothendieckChecker.PsiResult;
import com.bica.reborn.grothendieck.GrothendieckChecker.VirtualLattice;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Java port of {@code reticulate/tests/test_grothendieck.py}. */
class GrothendieckCheckerTest {

    private static StateSpace ss(String src) {
        return StateSpaceBuilder.build(Parser.parse(src));
    }

    // -----------------------------------------------------------------------
    // Signatures
    // -----------------------------------------------------------------------

    @Test
    void signatureDeterministic() {
        LatticeSig s1 = GrothendieckChecker.latticeSignature(ss("&{a: end, b: end}"));
        LatticeSig s2 = GrothendieckChecker.latticeSignature(ss("&{a: end, b: end}"));
        assertEquals(s1, s2);
    }

    @Test
    void signatureDistinguishesDifferentShapes() {
        LatticeSig a = GrothendieckChecker.latticeSignature(ss("end"));
        LatticeSig b = GrothendieckChecker.latticeSignature(ss("&{a: end}"));
        LatticeSig c = GrothendieckChecker.latticeSignature(ss("&{a: end, b: end}"));
        assertNotEquals(a, b);
        assertNotEquals(b, c);
        assertNotEquals(a, c);
    }

    @Test
    void signatureRenamingInvariant() {
        LatticeSig s1 = GrothendieckChecker.latticeSignature(ss("&{foo: end, bar: end}"));
        LatticeSig s2 = GrothendieckChecker.latticeSignature(ss("&{x: end, y: end}"));
        assertEquals(s1, s2);
    }

    @Test
    void signatureEndIsSingleton() {
        LatticeSig sig = GrothendieckChecker.latticeSignature(ss("end"));
        assertEquals(1, sig.size());
        assertEquals(0, sig.height());
    }

    // -----------------------------------------------------------------------
    // VirtualLattice algebra
    // -----------------------------------------------------------------------

    @Test
    void zeroVirtualLattice() {
        VirtualLattice z = VirtualLattice.zero();
        assertTrue(z.isZero());
        assertEquals(z, z.add(z));
    }

    @Test
    void generatorRoundTrip() {
        LatticeSig sig = GrothendieckChecker.latticeSignature(ss("&{a: end}"));
        VirtualLattice g = VirtualLattice.generator(sig);
        assertFalse(g.isZero());
        assertEquals(1, g.posCounter().get(sig));
        assertEquals(1, g.posCounter().size());
    }

    @Test
    void additionCommutative() {
        LatticeSig s1 = GrothendieckChecker.latticeSignature(ss("end"));
        LatticeSig s2 = GrothendieckChecker.latticeSignature(ss("&{a: end}"));
        VirtualLattice g1 = VirtualLattice.generator(s1);
        VirtualLattice g2 = VirtualLattice.generator(s2);
        assertEquals(g1.add(g2), g2.add(g1));
    }

    @Test
    void additionAssociative() {
        LatticeSig s1 = GrothendieckChecker.latticeSignature(ss("end"));
        LatticeSig s2 = GrothendieckChecker.latticeSignature(ss("&{a: end}"));
        LatticeSig s3 = GrothendieckChecker.latticeSignature(ss("&{a: end, b: end}"));
        VirtualLattice g1 = VirtualLattice.generator(s1);
        VirtualLattice g2 = VirtualLattice.generator(s2);
        VirtualLattice g3 = VirtualLattice.generator(s3);
        assertEquals(g1.add(g2).add(g3), g1.add(g2.add(g3)));
    }

    @Test
    void inverseLaw() {
        LatticeSig sig = GrothendieckChecker.latticeSignature(ss("&{a: end, b: end}"));
        VirtualLattice g = VirtualLattice.generator(sig);
        assertTrue(g.add(g.negate()).isZero());
        assertTrue(g.subtract(g).isZero());
    }

    @Test
    void cancellation() {
        LatticeSig s1 = GrothendieckChecker.latticeSignature(ss("end"));
        LatticeSig s2 = GrothendieckChecker.latticeSignature(ss("&{a: end}"));
        VirtualLattice g1 = VirtualLattice.generator(s1);
        VirtualLattice g2 = VirtualLattice.generator(s2);
        VirtualLattice v = g1.add(g2).subtract(g2);
        assertEquals(g1, v);
    }

    @Test
    void totalSizeSigned() {
        LatticeSig s1 = GrothendieckChecker.latticeSignature(ss("&{a: end, b: end}"));
        LatticeSig s2 = GrothendieckChecker.latticeSignature(ss("end"));
        VirtualLattice g1 = VirtualLattice.generator(s1);
        VirtualLattice g2 = VirtualLattice.generator(s2);
        assertEquals(s1.size() - s2.size(), g1.subtract(g2).totalSize());
    }

    @Test
    void doubleNegation() {
        LatticeSig sig = GrothendieckChecker.latticeSignature(ss("&{a: end}"));
        VirtualLattice g = VirtualLattice.generator(sig);
        assertEquals(g, g.negate().negate());
    }

    // -----------------------------------------------------------------------
    // Grothendieck group
    // -----------------------------------------------------------------------

    @Test
    void groupRegisterAndPhi() {
        GrothendieckGroup G = new GrothendieckGroup("sum");
        StateSpace s = ss("&{a: end, b: end}");
        LatticeSig sig = G.register(s);
        assertTrue(G.generators().contains(sig));
        VirtualLattice g = G.phi(s);
        assertEquals(VirtualLattice.generator(sig), g);
    }

    @Test
    void psiOfPhiIsIdentityOnGenerators() {
        GrothendieckGroup G = new GrothendieckGroup();
        StateSpace s = ss("&{a: end}");
        VirtualLattice v = G.phi(s);
        PsiResult pr = G.psi(v);
        assertEquals(1, pr.positiveTotal());
        assertEquals(0, pr.negativeTotal());
    }

    @Test
    void psiOfDifference() {
        GrothendieckGroup G = new GrothendieckGroup();
        StateSpace a = ss("&{a: end}");
        StateSpace b = ss("&{a: end, b: end}");
        VirtualLattice v = G.virtualDifference(a, b);
        PsiResult pr = G.psi(v);
        assertEquals(1, pr.positiveTotal());
        assertEquals(1, pr.negativeTotal());
    }

    @Test
    void sameSpaceDifferenceIsZero() {
        GrothendieckGroup G = new GrothendieckGroup();
        StateSpace a = ss("&{a: end, b: end}");
        StateSpace b = ss("&{x: end, y: end}");
        assertTrue(G.virtualDifference(a, b).isZero());
    }

    @Test
    void combineSignaturesSum() {
        GrothendieckGroup G = new GrothendieckGroup("sum");
        LatticeSig s1 = GrothendieckChecker.latticeSignature(ss("&{a: end}"));
        LatticeSig s2 = GrothendieckChecker.latticeSignature(ss("&{a: end}"));
        LatticeSig comb = G.combineSignatures(s1, s2);
        List<Integer> expected = new ArrayList<>();
        for (int x : s1.rankProfile()) expected.add(2 * x);
        assertEquals(expected, comb.rankProfile());
    }

    @Test
    void combineSignaturesProductConvolves() {
        GrothendieckGroup G = new GrothendieckGroup("product");
        LatticeSig s1 = GrothendieckChecker.latticeSignature(ss("&{a: end}"));
        LatticeSig s2 = GrothendieckChecker.latticeSignature(ss("&{a: end}"));
        LatticeSig comb = G.combineSignatures(s1, s2);
        assertEquals(List.of(1, 2, 1), comb.rankProfile());
    }

    @Test
    void combineSignaturesUnknownOp() {
        GrothendieckGroup G = new GrothendieckGroup("banana");
        LatticeSig s1 = GrothendieckChecker.latticeSignature(ss("end"));
        assertThrows(IllegalArgumentException.class, () -> G.combineSignatures(s1, s1));
    }

    // -----------------------------------------------------------------------
    // buildK0
    // -----------------------------------------------------------------------

    @Test
    void buildK0Basic() {
        List<StateSpace> spaces = List.of(
                ss("end"), ss("&{a: end}"), ss("&{a: end, b: end}"));
        GrothendieckResult r = GrothendieckChecker.buildK0(spaces);
        assertEquals(3, r.numGenerators());
        assertTrue(r.isFreeAbelian());
        assertEquals(2 * spaces.size(), r.relationsVerified());
    }

    @Test
    void buildK0DeduplicatesIsomorphic() {
        List<StateSpace> spaces = List.of(
                ss("&{a: end, b: end}"), ss("&{x: end, y: end}"));
        GrothendieckResult r = GrothendieckChecker.buildK0(spaces);
        assertEquals(1, r.numGenerators());
    }

    @Test
    void buildK0ProductOperation() {
        List<StateSpace> spaces = List.of(ss("&{a: end}"), ss("&{a: end, b: end}"));
        GrothendieckResult r = GrothendieckChecker.buildK0(spaces, "product");
        assertEquals("product", r.operation());
        assertEquals(2, r.numGenerators());
    }

    @Test
    void buildK0Empty() {
        GrothendieckResult r = GrothendieckChecker.buildK0(Collections.emptyList());
        assertEquals(0, r.numGenerators());
        assertTrue(r.isFreeAbelian());
    }

    // -----------------------------------------------------------------------
    // Morphism laws
    // -----------------------------------------------------------------------

    @Test
    void phiIsomorphismInvariant() {
        GrothendieckGroup G = new GrothendieckGroup();
        VirtualLattice a = G.phi(ss("&{a: end, b: end}"));
        VirtualLattice b = G.phi(ss("&{u: end, v: end}"));
        assertEquals(a, b);
    }

    @Test
    void virtualDifferenceAntisymmetric() {
        GrothendieckGroup G = new GrothendieckGroup();
        StateSpace a = ss("&{a: end}");
        StateSpace b = ss("&{a: end, b: end}");
        assertEquals(G.virtualDifference(a, b), G.virtualDifference(b, a).negate());
    }

    @Test
    void reducedFormUnique() {
        LatticeSig s1 = GrothendieckChecker.latticeSignature(ss("&{a: end}"));
        LatticeSig s2 = GrothendieckChecker.latticeSignature(ss("&{a: end, b: end}"));
        VirtualLattice g1 = VirtualLattice.generator(s1);
        VirtualLattice g2 = VirtualLattice.generator(s2);
        VirtualLattice v1 = g1.add(g2).subtract(g2);
        assertEquals(g1, v1);
    }

    @Test
    void phiMonoidMorphismSumAtSignatureLevel() {
        GrothendieckGroup G = new GrothendieckGroup("sum");
        LatticeSig sig1 = GrothendieckChecker.latticeSignature(ss("&{a: end}"));
        LatticeSig sig2 = GrothendieckChecker.latticeSignature(ss("&{a: end, b: end}"));
        LatticeSig combined = G.combineSignatures(sig1, sig2);
        assertEquals(sig1.size() + sig2.size(), combined.size());
    }

    @Test
    void k0HasAbelianAdditionOnBenchmarks() {
        GrothendieckGroup G = new GrothendieckGroup();
        List<String> sources = List.of(
                "end",
                "&{a: end}",
                "+{x: end, y: end}",
                "&{a: end, b: +{x: end}}");
        List<VirtualLattice> virtuals = new ArrayList<>();
        for (String s : sources) virtuals.add(G.phi(ss(s)));
        VirtualLattice total = VirtualLattice.zero();
        for (VirtualLattice v : virtuals) total = total.add(v);
        VirtualLattice total2 = VirtualLattice.zero();
        List<VirtualLattice> reversed = new ArrayList<>(virtuals);
        Collections.reverse(reversed);
        for (VirtualLattice v : reversed) total2 = total2.add(v);
        assertEquals(total, total2);
        for (VirtualLattice v : virtuals) total = total.subtract(v);
        assertTrue(total.isZero());
    }

    @Test
    void signatureDegreeMultisetPresent() {
        LatticeSig sig = GrothendieckChecker.latticeSignature(ss("&{a: end, b: end}"));
        assertNotNull(sig.degreeMultiset());
        for (int[] p : sig.degreeMultiset()) {
            assertEquals(2, p.length);
        }
    }
}
