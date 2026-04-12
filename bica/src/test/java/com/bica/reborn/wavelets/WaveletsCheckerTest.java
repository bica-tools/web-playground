package com.bica.reborn.wavelets;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.wavelets.WaveletsChecker.ScaleDecomposition;
import com.bica.reborn.wavelets.WaveletsChecker.WaveletAnalysis;
import com.bica.reborn.wavelets.WaveletsChecker.WaveletCoefficient;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java port of {@code reticulate/tests/test_wavelets.py}.
 */
class WaveletsCheckerTest {

    private static StateSpace ss(String t) {
        return StateSpaceBuilder.build(Parser.parse(t));
    }

    private static Map<Integer, Double> rankSignal(StateSpace s) {
        return WaveletsChecker.defaultSignal(s);
    }

    private static StateSpace endSs() { return ss("end"); }
    private static StateSpace chain() { return ss("&{a: &{b: &{c: end}}}"); }
    private static StateSpace branch() { return ss("&{a: end, b: end}"); }
    private static StateSpace parallel() { return ss("(&{a: end} || &{b: end})"); }
    private static StateSpace nested() { return ss("&{a: &{c: end, d: end}, b: &{e: end, f: end}}"); }

    // ---------------------------------------------------------------------
    // Spectral wavelets
    // ---------------------------------------------------------------------

    @Test
    void spectralReturnsDecomposition() {
        StateSpace s = chain();
        ScaleDecomposition d = WaveletsChecker.spectralWavelets(s, rankSignal(s));
        assertNotNull(d);
        assertEquals("spectral", d.approach());
    }

    @Test
    void spectralHasScales() {
        StateSpace s = chain();
        ScaleDecomposition d = WaveletsChecker.spectralWavelets(s, rankSignal(s));
        assertTrue(d.nScales() >= 1);
    }

    @Test
    void spectralCoefficientsExist() {
        StateSpace s = chain();
        ScaleDecomposition d = WaveletsChecker.spectralWavelets(s, rankSignal(s));
        assertTrue(d.coefficients().size() >= 1);
    }

    @Test
    void spectralEnergySumsToOne() {
        StateSpace s = chain();
        ScaleDecomposition d = WaveletsChecker.spectralWavelets(s, rankSignal(s));
        if (!d.energyPerScale().isEmpty()) {
            double sum = 0.0;
            for (double e : d.energyPerScale()) sum += e;
            assertEquals(1.0, sum, 1e-6);
        }
    }

    @Test
    void spectralSingleState() {
        StateSpace s = endSs();
        ScaleDecomposition d = WaveletsChecker.spectralWavelets(s, rankSignal(s));
        assertEquals(0, d.nScales());
    }

    // ---------------------------------------------------------------------
    // Diffusion wavelets
    // ---------------------------------------------------------------------

    @Test
    void diffusionReturnsDecomposition() {
        StateSpace s = chain();
        ScaleDecomposition d = WaveletsChecker.diffusionWavelets(s, rankSignal(s));
        assertEquals("diffusion", d.approach());
    }

    @Test
    void diffusionPerfectReconstruction() {
        StateSpace s = chain();
        ScaleDecomposition d = WaveletsChecker.diffusionWavelets(s, rankSignal(s));
        assertTrue(WaveletsChecker.reconstructionError(d) < 0.01);
    }

    @Test
    void diffusionCoarseIsSmooth() {
        StateSpace s = chain();
        Map<Integer, Double> f = rankSignal(s);
        ScaleDecomposition d = WaveletsChecker.diffusionWavelets(s, f);
        double origMin = Double.POSITIVE_INFINITY, origMax = Double.NEGATIVE_INFINITY;
        for (double v : f.values()) { origMin = Math.min(origMin, v); origMax = Math.max(origMax, v); }
        double cMin = Double.POSITIVE_INFINITY, cMax = Double.NEGATIVE_INFINITY;
        for (double v : d.coarse().values()) { cMin = Math.min(cMin, v); cMax = Math.max(cMax, v); }
        assertTrue((cMax - cMin) <= (origMax - origMin) + 0.01);
    }

    @Test
    void diffusionBranch() {
        StateSpace s = branch();
        ScaleDecomposition d = WaveletsChecker.diffusionWavelets(s, rankSignal(s));
        assertTrue(d.nScales() >= 1);
    }

    @Test
    void diffusionSingleState() {
        StateSpace s = endSs();
        ScaleDecomposition d = WaveletsChecker.diffusionWavelets(s, rankSignal(s));
        assertEquals(0, d.nScales());
    }

    // ---------------------------------------------------------------------
    // Lattice wavelets
    // ---------------------------------------------------------------------

    @Test
    void latticeReturnsDecomposition() {
        StateSpace s = chain();
        ScaleDecomposition d = WaveletsChecker.latticeWavelets(s, rankSignal(s));
        assertEquals("lattice", d.approach());
    }

    @Test
    void latticePerfectReconstruction() {
        StateSpace s = chain();
        ScaleDecomposition d = WaveletsChecker.latticeWavelets(s, rankSignal(s));
        assertTrue(WaveletsChecker.reconstructionError(d) < 0.01);
    }

    @Test
    void latticeEnergyDistribution() {
        StateSpace s = chain();
        ScaleDecomposition d = WaveletsChecker.latticeWavelets(s, rankSignal(s));
        assertTrue(d.nScales() >= 2);
        if (d.energyPerScale().size() >= 2) {
            double max = 0.0;
            for (double e : d.energyPerScale()) max = Math.max(max, e);
            assertTrue(max < 1.0);
        }
    }

    @Test
    void latticeNestedBranch() {
        StateSpace s = nested();
        ScaleDecomposition d = WaveletsChecker.latticeWavelets(s, rankSignal(s));
        assertTrue(d.nScales() >= 1);
    }

    @Test
    void latticeParallel() {
        StateSpace s = parallel();
        ScaleDecomposition d = WaveletsChecker.latticeWavelets(s, rankSignal(s));
        assertNotNull(d);
    }

    // ---------------------------------------------------------------------
    // Reconstruction
    // ---------------------------------------------------------------------

    @Test
    void diffusionExactReconstruction() {
        StateSpace s = chain();
        Map<Integer, Double> f = rankSignal(s);
        ScaleDecomposition d = WaveletsChecker.diffusionWavelets(s, f);
        Map<Integer, Double> r = WaveletsChecker.reconstruct(d);
        for (int sId : s.states()) {
            assertTrue(Math.abs(r.getOrDefault(sId, 0.0) - f.getOrDefault(sId, 0.0)) < 0.01);
        }
    }

    @Test
    void latticeExactReconstruction() {
        StateSpace s = chain();
        Map<Integer, Double> f = rankSignal(s);
        ScaleDecomposition d = WaveletsChecker.latticeWavelets(s, f);
        Map<Integer, Double> r = WaveletsChecker.reconstruct(d);
        for (int sId : s.states()) {
            assertTrue(Math.abs(r.getOrDefault(sId, 0.0) - f.getOrDefault(sId, 0.0)) < 0.01);
        }
    }

    @Test
    void errorNonNegative() {
        StateSpace s = chain();
        Map<Integer, Double> f = rankSignal(s);
        for (String approach : List.of("spectral", "diffusion", "lattice")) {
            ScaleDecomposition d = WaveletsChecker.decompose(s, f, approach);
            assertTrue(WaveletsChecker.reconstructionError(d) >= 0);
        }
    }

    // ---------------------------------------------------------------------
    // Thresholding
    // ---------------------------------------------------------------------

    @Test
    void zeroThresholdPreserves() {
        StateSpace s = chain();
        ScaleDecomposition d = WaveletsChecker.latticeWavelets(s, rankSignal(s));
        ScaleDecomposition t = WaveletsChecker.thresholdCoefficients(d, 0.0);
        assertEquals(d.details().size(), t.details().size());
    }

    @Test
    void highThresholdZerosOut() {
        StateSpace s = chain();
        ScaleDecomposition d = WaveletsChecker.latticeWavelets(s, rankSignal(s));
        ScaleDecomposition t = WaveletsChecker.thresholdCoefficients(d, 1000.0);
        for (Map<Integer, Double> detail : t.details()) {
            for (double v : detail.values()) assertTrue(Math.abs(v) < 0.01);
        }
    }

    @Test
    void moderateThreshold() {
        StateSpace s = nested();
        ScaleDecomposition d = WaveletsChecker.latticeWavelets(s, rankSignal(s));
        ScaleDecomposition t = WaveletsChecker.thresholdCoefficients(d, 0.1);
        assertNotNull(t);
    }

    // ---------------------------------------------------------------------
    // Embedding
    // ---------------------------------------------------------------------

    @Test
    void embeddingReturnsDict() {
        StateSpace s = chain();
        Map<Integer, double[]> emb = WaveletsChecker.waveletEmbedding(s, rankSignal(s));
        assertEquals(s.states().size(), emb.size());
    }

    @Test
    void embeddingVectorsSameLength() {
        StateSpace s = chain();
        Map<Integer, double[]> emb = WaveletsChecker.waveletEmbedding(s, rankSignal(s), 3);
        int len = -1;
        for (double[] v : emb.values()) {
            if (len < 0) len = v.length;
            else assertEquals(len, v.length);
        }
    }

    @Test
    void branchEmbedding() {
        StateSpace s = branch();
        Map<Integer, double[]> emb = WaveletsChecker.waveletEmbedding(s, rankSignal(s));
        assertEquals(s.states().size(), emb.size());
    }

    // ---------------------------------------------------------------------
    // Anomaly localization
    // ---------------------------------------------------------------------

    @Test
    void noAnomaly() {
        StateSpace s = chain();
        Map<Integer, Double> f = rankSignal(s);
        List<WaveletCoefficient> anomalies = WaveletsChecker.anomalyLocalize(s, f, f);
        if (!anomalies.isEmpty()) {
            assertTrue(Math.abs(anomalies.get(0).value()) < 1e-10);
        }
    }

    @Test
    void detectsPerturbation() {
        StateSpace s = chain();
        Map<Integer, Double> f = rankSignal(s);
        Map<Integer, Double> perturbed = new HashMap<>(f);
        perturbed.put(s.top(), f.get(s.top()) + 10.0);
        List<WaveletCoefficient> anomalies = WaveletsChecker.anomalyLocalize(s, f, perturbed);
        boolean any = false;
        for (WaveletCoefficient a : anomalies) {
            if (a.state() == s.top() && Math.abs(a.value()) > 0.1) { any = true; break; }
        }
        assertTrue(any);
    }

    @Test
    void anomaliesSorted() {
        StateSpace s = chain();
        Map<Integer, Double> f = rankSignal(s);
        Map<Integer, Double> perturbed = new HashMap<>();
        for (var e : f.entrySet()) {
            perturbed.put(e.getKey(), e.getValue() + (e.getKey() == s.top() ? 1.0 : 0.0));
        }
        List<WaveletCoefficient> anomalies = WaveletsChecker.anomalyLocalize(s, f, perturbed);
        for (int i = 0; i < anomalies.size() - 1; i++) {
            assertTrue(Math.abs(anomalies.get(i).value()) >= Math.abs(anomalies.get(i + 1).value()));
        }
    }

    // ---------------------------------------------------------------------
    // Change impact
    // ---------------------------------------------------------------------

    @Test
    void noChange() {
        StateSpace s = chain();
        Map<Integer, Double> f = rankSignal(s);
        Map<Integer, Double> impact = WaveletsChecker.changeImpact(s, f, f);
        for (double e : impact.values()) assertTrue(e < 1e-10);
    }

    @Test
    void localChangeFineScale() {
        StateSpace s = chain();
        Map<Integer, Double> f = rankSignal(s);
        Map<Integer, Double> f2 = new HashMap<>(f);
        f2.put(s.bottom(), f.get(s.bottom()) + 1.0);
        Map<Integer, Double> impact = WaveletsChecker.changeImpact(s, f, f2);
        assertTrue(impact.size() >= 1);
    }

    // ---------------------------------------------------------------------
    // decompose
    // ---------------------------------------------------------------------

    @Test
    void decomposeAllApproaches() {
        StateSpace s = chain();
        Map<Integer, Double> f = rankSignal(s);
        for (String approach : List.of("spectral", "diffusion", "lattice")) {
            ScaleDecomposition d = WaveletsChecker.decompose(s, f, approach);
            assertEquals(approach, d.approach());
        }
    }

    @Test
    void decomposeDefaultSignal() {
        StateSpace s = chain();
        ScaleDecomposition d = WaveletsChecker.decompose(s);
        assertEquals("lattice", d.approach());
        assertEquals(s.states().size(), d.original().size());
    }

    @Test
    void decomposeInvalidApproach() {
        StateSpace s = chain();
        assertThrows(IllegalArgumentException.class,
                () -> WaveletsChecker.decompose(s, null, "invalid"));
    }

    // ---------------------------------------------------------------------
    // Full analysis
    // ---------------------------------------------------------------------

    @Test
    void analyzeEnd() {
        WaveletAnalysis a = WaveletsChecker.analyzeWavelets(endSs());
        assertEquals(1, a.numStates());
    }

    @Test
    void analyzeChain() {
        WaveletAnalysis a = WaveletsChecker.analyzeWavelets(chain());
        assertEquals(4, a.numStates());
        assertTrue(a.reconstructionErrors().get("diffusion") < 0.01);
        assertTrue(a.reconstructionErrors().get("lattice") < 0.01);
    }

    @Test
    void analyzeBranch() {
        WaveletAnalysis a = WaveletsChecker.analyzeWavelets(branch());
        assertEquals(2, a.numStates());
    }

    @Test
    void analyzeParallel() {
        WaveletAnalysis a = WaveletsChecker.analyzeWavelets(parallel());
        assertNotNull(a);
    }

    @Test
    void analyzeNested() {
        WaveletAnalysis a = WaveletsChecker.analyzeWavelets(nested());
        assertTrue(a.numStates() >= 3);
    }

    @Test
    void threeApproachesCompared() {
        WaveletAnalysis a = WaveletsChecker.analyzeWavelets(chain());
        assertTrue(a.reconstructionErrors().containsKey("spectral"));
        assertTrue(a.reconstructionErrors().containsKey("diffusion"));
        assertTrue(a.reconstructionErrors().containsKey("lattice"));
    }

    // ---------------------------------------------------------------------
    // Benchmarks
    // ---------------------------------------------------------------------

    @Test
    void benchmarks() {
        String[][] cases = {
                {"&{a: end, b: end}", "Branch"},
                {"&{a: &{b: &{c: end}}}", "Chain"},
                {"+{ok: end, err: end}", "Select"},
                {"(&{a: end} || &{b: end})", "Parallel"},
                {"&{a: &{c: end, d: end}, b: &{e: end}}", "Asymmetric"},
                {"&{get: +{OK: end, ERR: end}}", "REST"},
        };
        for (String[] c : cases) {
            WaveletAnalysis a = WaveletsChecker.analyzeWavelets(ss(c[0]));
            assertTrue(a.numStates() >= 2, c[1]);
            assertTrue(a.reconstructionErrors().get("lattice") < 1.0, c[1]);
        }
    }

    // ---------------------------------------------------------------------
    // Data types
    // ---------------------------------------------------------------------

    @Test
    void waveletCoefficientFields() {
        WaveletCoefficient c = new WaveletCoefficient(0, 1, 0.5);
        assertEquals(0, c.state());
        assertEquals(1, c.scale());
        assertEquals(0.5, c.value());
    }

    @Test
    void scaleDecompositionFields() {
        StateSpace s = chain();
        ScaleDecomposition d = WaveletsChecker.latticeWavelets(s, rankSignal(s));
        assertNotNull(d.original());
        assertNotNull(d.coarse());
        assertNotNull(d.details());
        assertNotNull(d.energyPerScale());
    }

    // ---------------------------------------------------------------------
    // Edge cases
    // ---------------------------------------------------------------------

    @Test
    void singleStateAnalysis() {
        WaveletAnalysis a = WaveletsChecker.analyzeWavelets(ss("end"));
        assertEquals(0, a.spectral().nScales());
    }

    @Test
    void twoStates() {
        WaveletAnalysis a = WaveletsChecker.analyzeWavelets(ss("&{a: end}"));
        assertEquals(2, a.numStates());
    }

    @Test
    void zeroSignal() {
        StateSpace s = chain();
        Map<Integer, Double> f = new LinkedHashMap<>();
        for (int x : s.states()) f.put(x, 0.0);
        ScaleDecomposition d = WaveletsChecker.latticeWavelets(s, f);
        for (WaveletCoefficient c : d.coefficients()) assertTrue(Math.abs(c.value()) < 1e-10);
    }

    @Test
    void constantSignal() {
        StateSpace s = chain();
        Map<Integer, Double> f = new LinkedHashMap<>();
        for (int x : s.states()) f.put(x, 5.0);
        ScaleDecomposition d = WaveletsChecker.latticeWavelets(s, f);
        for (Map<Integer, Double> detail : d.details()) {
            for (double v : detail.values()) assertTrue(Math.abs(v) < 1e-6);
        }
    }
}
