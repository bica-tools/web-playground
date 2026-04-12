package com.bica.reborn.ect_fingerprint;

import com.bica.reborn.ect_fingerprint.EctFingerprintChecker.ECTComparison;
import com.bica.reborn.ect_fingerprint.EctFingerprintChecker.ECTFingerprint;
import com.bica.reborn.ect_fingerprint.EctFingerprintChecker.MorphismClassification;
import com.bica.reborn.ect_fingerprint.EctFingerprintChecker.PsiInvariants;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java port of {@code reticulate/tests/test_ect_fingerprint.py} (Step 32i).
 */
class EctFingerprintCheckerTest {

    private static StateSpace ss(String t) {
        return StateSpaceBuilder.build(Parser.parse(t));
    }

    // ---- Core fingerprint properties ----

    @Test
    void endFingerprint() {
        ECTFingerprint fp = EctFingerprintChecker.computeEct(ss("end"));
        assertNotNull(fp);
        assertEquals(0, fp.rankMax());
        assertEquals(List.of(1), fp.chiSequence());
    }

    @Test
    void singleBranchFingerprint() {
        ECTFingerprint fp = EctFingerprintChecker.computeEct(ss("&{a: end}"));
        assertTrue(fp.rankMax() >= 1);
        assertEquals(fp.rankMax() + 1, fp.chiSequence().size());
        assertEquals(1, fp.chiSequence().get(0));
    }

    @Test
    void branchTwoMethods() {
        ECTFingerprint fp = EctFingerprintChecker.computeEct(ss("&{a: end, b: end}"));
        assertTrue(fp.rankMax() >= 1);
        assertEquals(1, fp.chiSequence().get(0));
    }

    @Test
    void chiSequenceLengthMatchesRank() {
        ECTFingerprint fp = EctFingerprintChecker.computeEct(ss("&{a: &{b: &{c: end}}}"));
        assertEquals(fp.rankMax() + 1, fp.chiSequence().size());
    }

    @Test
    void fVectorsNonemptyWhenStatesPresent() {
        ECTFingerprint fp = EctFingerprintChecker.computeEct(ss("&{a: &{b: end}}"));
        for (int i = 0; i < fp.fVectors().size(); i++) {
            if (fp.levelSizes().get(i) > 0) {
                assertTrue(fp.fVectors().get(i).size() >= 1);
            }
        }
    }

    @Test
    void levelSizesMonotone() {
        ECTFingerprint fp = EctFingerprintChecker.computeEct(ss("&{a: &{b: &{c: end}}}"));
        for (int i = 1; i < fp.levelSizes().size(); i++) {
            assertTrue(fp.levelSizes().get(i) >= fp.levelSizes().get(i - 1));
        }
    }

    @Test
    void selectionFingerprint() {
        ECTFingerprint fp = EctFingerprintChecker.computeEct(ss("+{a: end, b: end}"));
        assertTrue(fp.rankMax() >= 1);
        assertTrue(fp.chiSequence().get(0) >= 1);
    }

    @Test
    void parallelFingerprint() {
        ECTFingerprint fp = EctFingerprintChecker.computeEct(ss("(&{a: end} || &{b: end})"));
        assertTrue(fp.rankMax() >= 1);
        assertTrue(fp.chiSequence().size() >= 2);
    }

    @Test
    void deeperProtocol() {
        ECTFingerprint fp = EctFingerprintChecker.computeEct(ss("&{a: &{b: &{c: &{d: end}}}}"));
        assertEquals(fp.rankMax() + 1, fp.chiSequence().size());
        assertTrue(fp.rankMax() >= 3);
    }

    // ---- sublevelStates ----

    @Test
    void sublevelAtZero() {
        StateSpace s = ss("&{a: &{b: end}}");
        List<Integer> sub0 = EctFingerprintChecker.sublevelStates(s, 0);
        assertEquals(1, sub0.size());
    }

    @Test
    void sublevelGrows() {
        StateSpace s = ss("&{a: &{b: &{c: end}}}");
        List<Integer> s0 = EctFingerprintChecker.sublevelStates(s, 0);
        List<Integer> s1 = EctFingerprintChecker.sublevelStates(s, 1);
        List<Integer> s2 = EctFingerprintChecker.sublevelStates(s, 2);
        assertTrue(s0.size() <= s1.size());
        assertTrue(s1.size() <= s2.size());
    }

    @Test
    void sublevelAtMaxIsFull() {
        StateSpace s = ss("&{a: end, b: end}");
        Map<Integer, Integer> ranks = EctFingerprintChecker.computeRank(s);
        int K = ranks.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<Integer> full = EctFingerprintChecker.sublevelStates(s, K);
        assertTrue(full.size() >= 1);
    }

    // ---- compareEct ----

    @Test
    void compareEqual() {
        ECTFingerprint fp = EctFingerprintChecker.computeEct(ss("&{a: end}"));
        ECTComparison cmp = EctFingerprintChecker.compareEct(fp, fp);
        assertTrue(cmp.equal());
        assertEquals(0, cmp.l1Distance());
        assertEquals(-1, cmp.firstDivergence());
    }

    @Test
    void compareDifferent() {
        ECTFingerprint a = EctFingerprintChecker.computeEct(ss("&{a: end}"));
        ECTFingerprint b = EctFingerprintChecker.computeEct(ss("&{a: &{b: &{c: &{d: end}}}}"));
        ECTComparison cmp = EctFingerprintChecker.compareEct(a, b);
        assertFalse(cmp.equal());
        assertTrue(cmp.l1Distance() >= 0);
    }

    @Test
    void distanceSymmetric() {
        ECTFingerprint a = EctFingerprintChecker.computeEct(ss("&{a: end}"));
        ECTFingerprint b = EctFingerprintChecker.computeEct(ss("&{a: &{b: end}}"));
        assertEquals(a.distance(b), b.distance(a));
    }

    @Test
    void distanceZeroIffEqual() {
        ECTFingerprint a = EctFingerprintChecker.computeEct(ss("&{a: &{b: end}}"));
        ECTFingerprint b = EctFingerprintChecker.computeEct(ss("&{a: &{b: end}}"));
        assertEquals(0, a.distance(b));
    }

    @Test
    void firstDivergenceIndex() {
        ECTFingerprint a = EctFingerprintChecker.computeEct(ss("&{a: end}"));
        ECTFingerprint b = EctFingerprintChecker.computeEct(ss("&{a: &{b: end}}"));
        ECTComparison cmp = EctFingerprintChecker.compareEct(a, b);
        if (!cmp.equal()) {
            assertTrue(cmp.firstDivergence() >= 0);
        }
    }

    // ---- psiRecoverInvariants ----

    @Test
    void psiBasic() {
        ECTFingerprint fp = EctFingerprintChecker.computeEct(ss("&{a: &{b: end}}"));
        PsiInvariants inv = EctFingerprintChecker.psiRecoverInvariants(fp);
        assertEquals(fp.rankMax(), inv.rank());
    }

    @Test
    void psiEnd() {
        ECTFingerprint fp = EctFingerprintChecker.computeEct(ss("end"));
        PsiInvariants inv = EctFingerprintChecker.psiRecoverInvariants(fp);
        assertEquals(0, inv.rank());
        assertEquals(1, inv.topChi());
    }

    @Test
    void psiRankMatches() {
        for (String s : new String[]{"&{a: end}", "&{a: &{b: end}}", "&{a: &{b: &{c: end}}}"}) {
            ECTFingerprint fp = EctFingerprintChecker.computeEct(ss(s));
            assertEquals(fp.rankMax(), EctFingerprintChecker.psiRecoverInvariants(fp).rank());
        }
    }

    // ---- classifyEctMorphism ----

    @Test
    void classifyEmpty() {
        MorphismClassification cls = EctFingerprintChecker.classifyEctMorphism(List.of());
        assertNotNull(cls);
        assertTrue(cls.phiSurjectiveOntoImage());
    }

    @Test
    void classifySamples() {
        List<StateSpace> samples = List.of(
                ss("end"),
                ss("&{a: end}"),
                ss("&{a: &{b: end}}"),
                ss("(&{a: end} || &{b: end})"));
        MorphismClassification cls = EctFingerprintChecker.classifyEctMorphism(samples);
        assertTrue(List.of("galois", "projection", "injection").contains(cls.classification()));
        assertTrue(cls.psiRightAdjoint());
    }

    @Test
    void classifySingleSample() {
        MorphismClassification cls = EctFingerprintChecker.classifyEctMorphism(List.of(ss("&{a: end}")));
        assertTrue(cls.galoisConnection() || cls.phiOrderPreserving());
    }

    // ---- Fingerprint invariants ----

    @Test
    void fingerprintHashable() {
        ECTFingerprint fp = EctFingerprintChecker.computeEct(ss("&{a: end}"));
        assertDoesNotThrow(fp::hashCode);
    }

    @Test
    void fingerprintEquality() {
        ECTFingerprint a = EctFingerprintChecker.computeEct(ss("&{a: end}"));
        ECTFingerprint b = EctFingerprintChecker.computeEct(ss("&{a: end}"));
        assertEquals(a, b);
    }

    @Test
    void fingerprintLen() {
        ECTFingerprint fp = EctFingerprintChecker.computeEct(ss("&{a: &{b: end}}"));
        assertEquals(fp.chiSequence().size(), fp.length());
    }

    @Test
    void recursiveProtocolFingerprint() {
        ECTFingerprint fp = EctFingerprintChecker.computeEct(ss("rec X . &{loop: X, stop: end}"));
        assertTrue(fp.chiSequence().size() >= 1);
    }

    @Test
    void largeParallel() {
        ECTFingerprint fp = EctFingerprintChecker.computeEct(ss("(&{a: &{b: end}} || &{c: &{d: end}})"));
        assertTrue(fp.rankMax() >= 2);
    }

    @Test
    void nontrivialL1Distance() {
        ECTFingerprint a = EctFingerprintChecker.computeEct(ss("&{a: end}"));
        ECTFingerprint b = EctFingerprintChecker.computeEct(ss("&{a: &{b: &{c: &{d: &{e: end}}}}}"));
        assertTrue(a.distance(b) > 0);
    }
}
