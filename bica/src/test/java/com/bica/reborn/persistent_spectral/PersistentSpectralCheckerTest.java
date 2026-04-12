package com.bica.reborn.persistent_spectral;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.persistent_spectral.PersistentSpectralChecker.Filtration;
import com.bica.reborn.persistent_spectral.PersistentSpectralChecker.PersistentSpectralAnalysis;
import com.bica.reborn.persistent_spectral.PersistentSpectralChecker.PersistentSpectralFingerprint;
import com.bica.reborn.persistent_spectral.PersistentSpectralChecker.SpectralActions;
import com.bica.reborn.persistent_spectral.PersistentSpectralChecker.SpectralPersistencePair;
import com.bica.reborn.persistent_spectral.PersistentSpectralChecker.SpectralSnapshot;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Java port of {@code reticulate/tests/test_persistent_spectral.py} (Step 31k). */
class PersistentSpectralCheckerTest {

    private static StateSpace ss(String s) {
        return StateSpaceBuilder.build(Parser.parse(s));
    }

    private static final String TRIVIAL = "end";
    private static final String LINEAR = "&{a: &{b: &{c: end}}}";
    private static final String BRANCH = "&{a: end, b: end}";
    private static final String PARALLEL = "(&{a: end} || &{b: end})";
    private static final String NESTED = "&{a: &{b: end, c: end}, d: end}";

    // ----- Basic snapshots -----

    @Test
    void snapshotsTrivial() {
        List<SpectralSnapshot> snaps = PersistentSpectralChecker.persistentLaplacianSpectra(ss(TRIVIAL));
        assertTrue(snaps.size() >= 1);
        SpectralSnapshot last = snaps.get(snaps.size() - 1);
        assertEquals(1, last.numVertices);
        assertArrayEquals(new double[]{0.0}, last.eigenvalues, 1e-12);
    }

    @Test
    void snapshotsLinearGrows() {
        List<SpectralSnapshot> snaps = PersistentSpectralChecker.persistentLaplacianSpectra(ss(LINEAR));
        for (int i = 1; i < snaps.size(); i++) {
            assertTrue(snaps.get(i).numVertices >= snaps.get(i - 1).numVertices);
            assertTrue(snaps.get(i).numEdges >= snaps.get(i - 1).numEdges);
        }
    }

    @Test
    void snapshotsBranchFinalConnected() {
        List<SpectralSnapshot> snaps = PersistentSpectralChecker.persistentLaplacianSpectra(ss(BRANCH));
        SpectralSnapshot last = snaps.get(snaps.size() - 1);
        assertTrue(last.numVertices >= 2);
    }

    @Test
    void snapshotsLevelsSorted() {
        List<SpectralSnapshot> snaps = PersistentSpectralChecker.persistentLaplacianSpectra(ss(NESTED));
        for (int i = 1; i < snaps.size(); i++) {
            assertTrue(snaps.get(i).level >= snaps.get(i - 1).level);
        }
    }

    @Test
    void snapshotsAllEigenvaluesRealNonNegative() {
        for (String src : List.of(TRIVIAL, LINEAR, BRANCH, PARALLEL, NESTED)) {
            for (SpectralSnapshot snap : PersistentSpectralChecker.persistentLaplacianSpectra(ss(src))) {
                for (double e : snap.eigenvalues) assertTrue(e > -1e-6);
            }
        }
    }

    @Test
    void zeroMultiplicityMatchesComponentsFinal() {
        List<SpectralSnapshot> snaps = PersistentSpectralChecker.persistentLaplacianSpectra(ss(LINEAR));
        assertEquals(1, snaps.get(snaps.size() - 1).zeroMultiplicity());
    }

    @Test
    void snapshotFiedlerProperty() {
        SpectralSnapshot snap = new SpectralSnapshot(0.0, 2, 1, new double[]{0.0, 2.0});
        assertEquals(2.0, snap.fiedler());
        assertEquals(2.0, snap.spectralRadius());
        assertEquals(1, snap.zeroMultiplicity());
    }

    @Test
    void snapshotFiedlerEmpty() {
        SpectralSnapshot snap = new SpectralSnapshot(0.0, 0, 0, new double[]{});
        assertEquals(0.0, snap.fiedler());
        assertEquals(0.0, snap.spectralRadius());
        assertEquals(0, snap.zeroMultiplicity());
    }

    // ----- Filtration kinds -----

    @Test
    void reverseRankFiltration() {
        List<SpectralSnapshot> snaps = PersistentSpectralChecker.persistentLaplacianSpectra(
                ss(LINEAR), Filtration.REVERSE_RANK);
        assertTrue(snaps.size() >= 1);
        assertEquals(ss(LINEAR).states().size(), snaps.get(snaps.size() - 1).numVertices);
    }

    @Test
    void unknownFiltrationRaises() {
        assertThrows(IllegalArgumentException.class,
                () -> PersistentSpectralChecker.filtrationFromString("bogus"));
    }

    // ----- Fiedler trace -----

    @Test
    void fiedlerTraceLengthMatchesSnapshots() {
        List<SpectralSnapshot> snaps = PersistentSpectralChecker.persistentLaplacianSpectra(ss(BRANCH));
        List<double[]> trace = PersistentSpectralChecker.persistentFiedlerTrace(ss(BRANCH));
        assertEquals(snaps.size(), trace.size());
    }

    @Test
    void fiedlerTraceTuples() {
        List<double[]> trace = PersistentSpectralChecker.persistentFiedlerTrace(ss(NESTED));
        for (double[] t : trace) assertEquals(2, t.length);
    }

    // ----- Persistence diagram -----

    @Test
    void persistenceDiagramNonempty() {
        List<SpectralPersistencePair> pairs = PersistentSpectralChecker.spectralPersistenceDiagram(ss(NESTED));
        assertTrue(pairs.size() >= 1);
    }

    @Test
    void persistenceDiagramIndicesUnique() {
        List<SpectralPersistencePair> pairs = PersistentSpectralChecker.spectralPersistenceDiagram(ss(NESTED));
        Set<Integer> idxs = new HashSet<>();
        for (SpectralPersistencePair p : pairs) idxs.add(p.index);
        assertEquals(pairs.size(), idxs.size());
    }

    @Test
    void persistencePairProperty() {
        SpectralPersistencePair p = new SpectralPersistencePair(1, 0.0, 2.0, 0.0, 1.0);
        assertEquals(2.0, p.persistence());
        SpectralPersistencePair pInf = new SpectralPersistencePair(0, 0.0,
                Double.POSITIVE_INFINITY, 0.0, Double.NaN);
        assertTrue(Double.isInfinite(pInf.persistence()));
    }

    @Test
    void persistenceTrivial() {
        List<SpectralPersistencePair> pairs = PersistentSpectralChecker.spectralPersistenceDiagram(ss(TRIVIAL));
        assertEquals(1, pairs.size());
        assertEquals(0, pairs.get(0).index);
    }

    // ----- Fingerprint -----

    @Test
    void fingerprintStructure() {
        PersistentSpectralFingerprint fp = PersistentSpectralChecker.persistentSpectralFingerprint(ss(NESTED));
        assertEquals(fp.numLevels, fp.fiedlerTrace.length);
        assertEquals(fp.numLevels, fp.radiusTrace.length);
        assertEquals(fp.numLevels, fp.zeroMultTrace.length);
        assertEquals(fp.numLevels, fp.energyTrace.length);
    }

    @Test
    void fingerprintAsVectorLength() {
        PersistentSpectralFingerprint fp = PersistentSpectralChecker.persistentSpectralFingerprint(ss(NESTED));
        assertEquals(4 * fp.numLevels + 2, fp.asVector().length);
    }

    @Test
    void fingerprintMaxFiedlerConsistent() {
        PersistentSpectralFingerprint fp = PersistentSpectralChecker.persistentSpectralFingerprint(ss(BRANCH));
        if (fp.fiedlerTrace.length > 0) {
            double m = fp.fiedlerTrace[0];
            for (double v : fp.fiedlerTrace) if (v > m) m = v;
            assertEquals(m, fp.maxFiedler);
        }
    }

    @Test
    void fingerprintEmptyStateSpaceSafe() {
        PersistentSpectralFingerprint fp = PersistentSpectralChecker.persistentSpectralFingerprint(ss(TRIVIAL));
        assertTrue(fp.numLevels >= 1);
    }

    @Test
    void fingerprintDistinguishesDifferentProtocols() {
        PersistentSpectralFingerprint a = PersistentSpectralChecker.persistentSpectralFingerprint(ss(LINEAR));
        PersistentSpectralFingerprint b = PersistentSpectralChecker.persistentSpectralFingerprint(ss(BRANCH));
        assertTrue(PersistentSpectralChecker.fingerprintDistance(a, b) > 0.0);
    }

    @Test
    void fingerprintIdentityZeroDistance() {
        PersistentSpectralFingerprint fp = PersistentSpectralChecker.persistentSpectralFingerprint(ss(NESTED));
        assertEquals(0.0, PersistentSpectralChecker.fingerprintDistance(fp, fp));
    }

    @Test
    void fingerprintDistanceSymmetric() {
        PersistentSpectralFingerprint a = PersistentSpectralChecker.persistentSpectralFingerprint(ss(LINEAR));
        PersistentSpectralFingerprint b = PersistentSpectralChecker.persistentSpectralFingerprint(ss(NESTED));
        double dab = PersistentSpectralChecker.fingerprintDistance(a, b);
        double dba = PersistentSpectralChecker.fingerprintDistance(b, a);
        assertTrue(Math.abs(dab - dba) < 1e-12);
    }

    @Test
    void fingerprintDistanceNonNegative() {
        String[][] pairs = {{LINEAR, BRANCH}, {NESTED, PARALLEL}, {TRIVIAL, LINEAR}};
        for (String[] p : pairs) {
            PersistentSpectralFingerprint a = PersistentSpectralChecker.persistentSpectralFingerprint(ss(p[0]));
            PersistentSpectralFingerprint b = PersistentSpectralChecker.persistentSpectralFingerprint(ss(p[1]));
            assertTrue(PersistentSpectralChecker.fingerprintDistance(a, b) >= 0.0);
        }
    }

    // ----- Bidirectional morphisms -----

    @Test
    void phiIsFingerprint() {
        PersistentSpectralFingerprint fp = PersistentSpectralChecker.phiLatticeToSpectrum(ss(NESTED));
        assertNotNull(fp);
    }

    @Test
    void psiOutputsActions() {
        PersistentSpectralFingerprint fp = PersistentSpectralChecker.phiLatticeToSpectrum(ss(NESTED));
        SpectralActions actions = PersistentSpectralChecker.psiSpectrumToAction(fp);
        assertNotNull(actions);
        assertTrue(actions.stabilityScore >= 0.0 && actions.stabilityScore <= 1.0);
        assertTrue(actions.asMap().containsKey("bottleneck"));
        assertTrue(actions.asMap().containsKey("complexity_alert"));
        assertTrue(actions.asMap().containsKey("fragile_levels"));
        assertTrue(actions.asMap().containsKey("monitoring_level"));
        assertTrue(actions.asMap().containsKey("stability_score"));
    }

    @Test
    void psiHighThresholdTriggersBottleneck() {
        PersistentSpectralFingerprint fp = PersistentSpectralChecker.phiLatticeToSpectrum(ss(LINEAR));
        SpectralActions actions = PersistentSpectralChecker.psiSpectrumToAction(fp, 10.0, 2.0);
        boolean allZero = true;
        for (double f : fp.fiedlerTrace) if (f != 0.0) { allZero = false; break; }
        assertTrue(actions.bottleneck || allZero);
    }

    @Test
    void psiLowThresholdQuiet() {
        PersistentSpectralFingerprint fp = PersistentSpectralChecker.phiLatticeToSpectrum(ss(BRANCH));
        SpectralActions actions = PersistentSpectralChecker.psiSpectrumToAction(fp, 0.0, 2.0);
        assertFalse(actions.bottleneck);
    }

    @Test
    void phiDeterministic() {
        PersistentSpectralFingerprint a = PersistentSpectralChecker.phiLatticeToSpectrum(ss(NESTED));
        PersistentSpectralFingerprint b = PersistentSpectralChecker.phiLatticeToSpectrum(ss(NESTED));
        assertEquals(a, b);
    }

    @Test
    void bidirectionalRoundtripRuns() {
        for (String src : List.of(TRIVIAL, LINEAR, BRANCH, PARALLEL, NESTED)) {
            PersistentSpectralFingerprint fp = PersistentSpectralChecker.phiLatticeToSpectrum(ss(src));
            SpectralActions actions = PersistentSpectralChecker.psiSpectrumToAction(fp);
            assertNotNull(actions);
        }
    }

    // ----- Full analysis -----

    @Test
    void analysisRuns() {
        PersistentSpectralAnalysis a = PersistentSpectralChecker.analyzePersistentSpectrum(ss(NESTED));
        assertEquals(ss(NESTED).states().size(), a.numStates);
        assertTrue(a.snapshots.size() >= 1);
    }

    @Test
    void analysisFingerprintMatches() {
        StateSpace sp = ss(LINEAR);
        PersistentSpectralAnalysis a = PersistentSpectralChecker.analyzePersistentSpectrum(sp);
        PersistentSpectralFingerprint fp = PersistentSpectralChecker.persistentSpectralFingerprint(sp);
        assertEquals(a.fingerprint, fp);
    }

    @Test
    void analysisMultipleProtocols() {
        for (String src : List.of(TRIVIAL, LINEAR, BRANCH, PARALLEL, NESTED)) {
            PersistentSpectralAnalysis a = PersistentSpectralChecker.analyzePersistentSpectrum(ss(src));
            assertEquals(ss(src).states().size(), a.numStates);
        }
    }

    // ----- Compositional properties -----

    @Test
    void parallelHasMoreLevelsThanComponent() {
        PersistentSpectralFingerprint single = PersistentSpectralChecker.persistentSpectralFingerprint(ss("&{a: end}"));
        PersistentSpectralFingerprint par = PersistentSpectralChecker.persistentSpectralFingerprint(ss(PARALLEL));
        assertTrue(par.numLevels >= single.numLevels);
    }

    @Test
    void monotoneVertexCount() {
        for (String src : List.of(LINEAR, BRANCH, NESTED, PARALLEL)) {
            List<SpectralSnapshot> snaps = PersistentSpectralChecker.persistentLaplacianSpectra(ss(src));
            for (int i = 1; i < snaps.size(); i++) {
                assertTrue(snaps.get(i).numVertices >= snaps.get(i - 1).numVertices);
            }
        }
    }
}
