package com.bica.reborn.spectral_refactor;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.spectral_refactor.SpectralRefactorChecker.*;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.statespace.TransitionKind;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SpectralRefactorChecker -- Java port of Python test_spectral_refactor.py.
 * Step 31i: Spectral Protocol Refactoring.
 */
class SpectralRefactorCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    private StateSpace manualSs(Set<Integer> states, List<Transition> trans,
                                int top, int bottom) {
        Map<Integer, String> labels = new HashMap<>();
        for (int s : states) labels.put(s, String.valueOf(s));
        return new StateSpace(states, trans, top, bottom, labels);
    }

    // -----------------------------------------------------------------------
    // Data type tests
    // -----------------------------------------------------------------------

    @Test
    void spectralEmbeddingRecord() {
        var emb = new SpectralEmbedding(
                List.of(0, 1),
                List.of(new double[]{0.0, 1.0}, new double[]{1.0, 0.0}),
                new double[]{0.0, 2.0},
                2);
        assertEquals(2, emb.k());
        assertEquals(2, emb.coordinates().size());
    }

    @Test
    void degeneracyGroupRecord() {
        var dg = new DegeneracyGroup(1.5, List.of(0, 1), 2, 0.1);
        assertEquals(2, dg.multiplicity());
        assertEquals(0.1, dg.spread(), 1e-10);
    }

    @Test
    void redundantPairRecord() {
        var rp = new RedundantPair(0, 1, 0.05, new double[]{0.1}, new double[]{0.15});
        assertEquals(0.05, rp.distance(), 1e-10);
    }

    @Test
    void refactoringPlanRecord() {
        var plan = new RefactoringPlan("fiedler", "test", 0.3);
        assertEquals("fiedler", plan.approach());
        assertTrue(plan.mergePairs().isEmpty());
    }

    @Test
    void analysisRecord() {
        var emb = new SpectralEmbedding(List.of(0), List.of(new double[]{0.0}),
                new double[]{0.0}, 1);
        var analysis = new SpectralRefactorAnalysis(
                emb, List.of(), List.of(),
                new RefactoringPlan("fiedler", "", 0.0),
                new RefactoringPlan("degeneracy", "", 0.0),
                new RefactoringPlan("clustering", "", 0.0),
                0.0, 1, 1, 1);
        assertEquals(1, analysis.numStates());
    }

    // -----------------------------------------------------------------------
    // Linear algebra tests
    // -----------------------------------------------------------------------

    @Test
    void laplacianSingleState() {
        StateSpace ss = build("end");
        List<Integer> states = new ArrayList<>();
        double[][] L = SpectralRefactorChecker.buildLaplacian(ss, states);
        assertEquals(1, states.size());
        assertEquals(0.0, L[0][0], 1e-10);
    }

    @Test
    void laplacianRowSumsZero() {
        StateSpace ss = build("&{a: end, b: end}");
        List<Integer> states = new ArrayList<>();
        double[][] L = SpectralRefactorChecker.buildLaplacian(ss, states);
        for (int i = 0; i < L.length; i++) {
            double sum = 0;
            for (int j = 0; j < L[i].length; j++) sum += L[i][j];
            assertEquals(0.0, sum, 1e-10, "Row sum must be 0");
        }
    }

    @Test
    void laplacianSymmetric() {
        StateSpace ss = build("&{a: &{c: end, d: end}, b: end}");
        List<Integer> states = new ArrayList<>();
        double[][] L = SpectralRefactorChecker.buildLaplacian(ss, states);
        for (int i = 0; i < L.length; i++) {
            for (int j = 0; j < L.length; j++) {
                assertEquals(L[i][j], L[j][i], 1e-10);
            }
        }
    }

    @Test
    void eigenvaluesIdentity() {
        double[][] I = {{1.0, 0.0}, {0.0, 1.0}};
        double[] eigs = SpectralRefactorChecker.eigenvalues(I);
        assertEquals(2, eigs.length);
        for (double e : eigs) assertEquals(1.0, e, 0.01);
    }

    @Test
    void eigenvaluesZeroMatrix() {
        double[][] Z = {{0.0, 0.0}, {0.0, 0.0}};
        double[] eigs = SpectralRefactorChecker.eigenvalues(Z);
        for (double e : eigs) assertEquals(0.0, e, 0.01);
    }

    @Test
    void eigenvaluesLaplacianHasZero() {
        StateSpace ss = build("&{a: end, b: end}");
        List<Integer> states = new ArrayList<>();
        double[][] L = SpectralRefactorChecker.buildLaplacian(ss, states);
        double[] eigs = SpectralRefactorChecker.eigenvalues(L);
        Arrays.sort(eigs);
        assertTrue(Math.abs(eigs[0]) < 0.1, "Smallest eigenvalue should be ~0");
    }

    @Test
    void euclideanDistanceZero() {
        assertEquals(0.0, SpectralRefactorChecker.euclideanDistance(
                new double[]{1.0, 2.0}, new double[]{1.0, 2.0}), 1e-10);
    }

    @Test
    void euclideanDistanceUnit() {
        assertEquals(1.0, SpectralRefactorChecker.euclideanDistance(
                new double[]{0.0}, new double[]{1.0}), 1e-10);
    }

    @Test
    void euclideanDistance2d() {
        assertEquals(5.0, SpectralRefactorChecker.euclideanDistance(
                new double[]{0.0, 0.0}, new double[]{3.0, 4.0}), 1e-10);
    }

    // -----------------------------------------------------------------------
    // Approach A: Fiedler bisection
    // -----------------------------------------------------------------------

    @Test
    void fiedlerEnd() {
        var plan = SpectralRefactorChecker.fiedlerRefactoring(build("end"));
        assertEquals("fiedler", plan.approach());
        assertNull(plan.partitionA());
    }

    @Test
    void fiedlerBranch() {
        StateSpace ss = build("&{a: end, b: end}");
        var plan = SpectralRefactorChecker.fiedlerRefactoring(ss);
        assertEquals("fiedler", plan.approach());
        assertNotNull(plan.partitionA());
        assertNotNull(plan.partitionB());
        Set<Integer> all = new HashSet<>(plan.partitionA());
        all.addAll(plan.partitionB());
        assertEquals(ss.states().size(), all.size());
        // No overlap
        Set<Integer> overlap = new HashSet<>(plan.partitionA());
        overlap.retainAll(plan.partitionB());
        assertTrue(overlap.isEmpty());
    }

    @Test
    void fiedlerNested() {
        var plan = SpectralRefactorChecker.fiedlerRefactoring(build("&{a: &{c: end, d: end}, b: end}"));
        assertEquals("fiedler", plan.approach());
        assertNotNull(plan.partitionA());
        assertTrue(plan.estimatedReduction() >= 0.0);
    }

    @Test
    void fiedlerParallel() {
        StateSpace ss = build("(&{a: end} || &{b: end})");
        var plan = SpectralRefactorChecker.fiedlerRefactoring(ss);
        assertEquals("fiedler", plan.approach());
        if (plan.partitionA() != null) {
            Set<Integer> all = new HashSet<>(plan.partitionA());
            all.addAll(plan.partitionB());
            assertEquals(ss.states(), all);
        }
    }

    @Test
    void fiedlerDescription() {
        var plan = SpectralRefactorChecker.fiedlerRefactoring(build("&{a: end, b: end}"));
        assertTrue(plan.description().contains("Fiedler"));
    }

    @Test
    void fiedlerReductionBounded() {
        var plan = SpectralRefactorChecker.fiedlerRefactoring(build("&{a: &{c: end, d: end}, b: end}"));
        assertTrue(plan.estimatedReduction() >= 0.0);
        assertTrue(plan.estimatedReduction() <= 1.0);
    }

    @Test
    void fiedlerWide() {
        var plan = SpectralRefactorChecker.fiedlerRefactoring(build("&{a: end, b: end, c: end, d: end}"));
        assertNotNull(plan.partitionA());
    }

    @Test
    void fiedlerChain() {
        var plan = SpectralRefactorChecker.fiedlerRefactoring(build("&{a: &{b: &{c: end}}}"));
        assertEquals("fiedler", plan.approach());
    }

    // -----------------------------------------------------------------------
    // Approach B: Spectral embedding + degeneracy
    // -----------------------------------------------------------------------

    @Test
    void embeddingEnd() {
        var emb = SpectralRefactorChecker.spectralEmbedding(build("end"), 2);
        assertEquals(1, emb.k());
        assertEquals(1, emb.stateIds().size());
    }

    @Test
    void embeddingBranch() {
        StateSpace ss = build("&{a: end, b: end}");
        var emb = SpectralRefactorChecker.spectralEmbedding(ss, 2);
        assertEquals(ss.states().size(), emb.stateIds().size());
        assertEquals(ss.states().size(), emb.coordinates().size());
        for (double[] coord : emb.coordinates()) {
            assertEquals(emb.k(), coord.length);
        }
    }

    @Test
    void embeddingKClamped() {
        StateSpace ss = build("&{a: end}");
        var emb = SpectralRefactorChecker.spectralEmbedding(ss, 100);
        assertTrue(emb.k() <= ss.states().size());
    }

    @Test
    void embeddingDifferentTypesDiffer() {
        var emb1 = SpectralRefactorChecker.spectralEmbedding(build("&{a: end, b: end}"), 2);
        var emb2 = SpectralRefactorChecker.spectralEmbedding(build("&{a: &{b: &{c: end}}}"), 2);
        boolean differ = !emb1.stateIds().equals(emb2.stateIds());
        if (!differ) {
            for (int i = 0; i < emb1.coordinates().size(); i++) {
                if (!Arrays.equals(emb1.coordinates().get(i), emb2.coordinates().get(i))) {
                    differ = true;
                    break;
                }
            }
        }
        assertTrue(differ);
    }

    @Test
    void embeddingEigenvaluesAscending() {
        var emb = SpectralRefactorChecker.spectralEmbedding(build("&{a: &{c: end, d: end}, b: end}"), 3);
        for (int i = 0; i < emb.eigenvalues().length - 1; i++) {
            assertTrue(emb.eigenvalues()[i] <= emb.eigenvalues()[i + 1] + 0.01);
        }
    }

    // -----------------------------------------------------------------------
    // Eigenvalue degeneracy
    // -----------------------------------------------------------------------

    @Test
    void degeneracyEnd() {
        var groups = SpectralRefactorChecker.eigenvalueDegeneracy(build("end"));
        assertTrue(groups.isEmpty());
    }

    @Test
    void degeneracyBranch() {
        var groups = SpectralRefactorChecker.eigenvalueDegeneracy(build("&{a: end, b: end}"), 0.1);
        for (var g : groups) {
            assertTrue(g.multiplicity() > 1);
        }
    }

    @Test
    void degeneracySymmetricStructure() {
        var groups = SpectralRefactorChecker.eigenvalueDegeneracy(
                build("&{a: end, b: end, c: end, d: end}"), 0.5);
        assertNotNull(groups);
    }

    @Test
    void degeneracyTolerance() {
        StateSpace ss = build("&{a: end, b: end}");
        var loose = SpectralRefactorChecker.eigenvalueDegeneracy(ss, 1.0);
        var tight = SpectralRefactorChecker.eigenvalueDegeneracy(ss, 0.001);
        assertTrue(tight.size() <= loose.size());
    }

    @Test
    void degeneracyGroupSpread() {
        var groups = SpectralRefactorChecker.eigenvalueDegeneracy(
                build("&{a: &{c: end, d: end}, b: end}"), 0.5);
        for (var g : groups) {
            assertTrue(g.spread() <= 0.5 + 1e-10);
            assertTrue(g.spread() >= 0.0);
        }
    }

    // -----------------------------------------------------------------------
    // Redundant states
    // -----------------------------------------------------------------------

    @Test
    void redundantEnd() {
        var pairs = SpectralRefactorChecker.redundantStates(build("end"), 0.5);
        assertTrue(pairs.isEmpty());
    }

    @Test
    void redundantBranch() {
        var pairs = SpectralRefactorChecker.redundantStates(build("&{a: end, b: end}"), 0.5);
        for (var p : pairs) {
            assertNotEquals(p.stateA(), p.stateB());
            assertTrue(p.distance() <= 0.5 + 1e-10);
        }
    }

    @Test
    void redundantSortedByDistance() {
        var pairs = SpectralRefactorChecker.redundantStates(
                build("&{a: &{c: end, d: end}, b: end}"), 1.0);
        for (int i = 0; i < pairs.size() - 1; i++) {
            assertTrue(pairs.get(i).distance() <= pairs.get(i + 1).distance() + 1e-10);
        }
    }

    @Test
    void redundantTightTolerance() {
        var pairs = SpectralRefactorChecker.redundantStates(build("&{a: end, b: end}"), 0.0001);
        for (var p : pairs) {
            assertTrue(p.distance() < 0.0001 + 1e-10);
        }
    }

    @Test
    void redundantWide() {
        var pairs = SpectralRefactorChecker.redundantStates(
                build("&{a: end, b: end, c: end, d: end}"), 0.5);
        assertNotNull(pairs);
    }

    // -----------------------------------------------------------------------
    // Merge redundant
    // -----------------------------------------------------------------------

    @Test
    void mergeEmptyPairs() {
        StateSpace ss = build("&{a: end, b: end}");
        StateSpace merged = SpectralRefactorChecker.mergeRedundant(ss, List.of());
        assertEquals(ss.states().size(), merged.states().size());
    }

    @Test
    void mergeReducesStates() {
        StateSpace ss = build("&{a: end, b: end}");
        var pairs = SpectralRefactorChecker.redundantStates(ss, 1.0);
        if (!pairs.isEmpty()) {
            StateSpace merged = SpectralRefactorChecker.mergeRedundant(ss, pairs);
            assertTrue(merged.states().size() <= ss.states().size());
        }
    }

    @Test
    void mergePreservesTopBottom() {
        StateSpace ss = build("&{a: &{c: end, d: end}, b: end}");
        var pairs = SpectralRefactorChecker.redundantStates(ss, 1.0);
        StateSpace merged = SpectralRefactorChecker.mergeRedundant(ss, pairs);
        assertTrue(merged.states().contains(merged.top()));
        assertTrue(merged.states().contains(merged.bottom()));
    }

    @Test
    void mergeNoSelfLoops() {
        StateSpace ss = build("&{a: &{c: end, d: end}, b: end}");
        var pairs = SpectralRefactorChecker.redundantStates(ss, 1.0);
        StateSpace merged = SpectralRefactorChecker.mergeRedundant(ss, pairs);
        for (var t : merged.transitions()) {
            assertNotEquals(t.source(), t.target());
        }
    }

    @Test
    void mergeManualPair() {
        StateSpace ss = manualSs(
                Set.of(0, 1, 2, 3),
                List.of(new Transition(0, "a", 1), new Transition(0, "b", 2),
                        new Transition(1, "c", 3), new Transition(2, "c", 3)),
                0, 3);
        var pair = new RedundantPair(1, 2, 0.0, new double[]{0.0}, new double[]{0.0});
        StateSpace merged = SpectralRefactorChecker.mergeRedundant(ss, List.of(pair));
        assertTrue(merged.states().size() <= 3);
        assertTrue(merged.states().contains(merged.top()));
        assertTrue(merged.states().contains(merged.bottom()));
    }

    // -----------------------------------------------------------------------
    // K-means
    // -----------------------------------------------------------------------

    @Test
    void kmeansEmpty() {
        int[] result = SpectralRefactorChecker.kmeans(List.of(), 2, 100);
        assertEquals(0, result.length);
    }

    @Test
    void kmeansSinglePoint() {
        int[] result = SpectralRefactorChecker.kmeans(List.of(new double[]{0.0, 0.0}), 1, 100);
        assertArrayEquals(new int[]{0}, result);
    }

    @Test
    void kmeansKEqualsN() {
        var points = List.of(new double[]{0.0}, new double[]{1.0}, new double[]{2.0});
        int[] result = SpectralRefactorChecker.kmeans(points, 3, 100);
        assertEquals(3, new HashSet<>(Arrays.asList(
                result[0], result[1], result[2])).size());
    }

    @Test
    void kmeansTwoClusters() {
        var points = List.of(
                new double[]{0.0}, new double[]{0.1},
                new double[]{10.0}, new double[]{10.1});
        int[] result = SpectralRefactorChecker.kmeans(points, 2, 100);
        assertEquals(result[0], result[1]);
        assertEquals(result[2], result[3]);
        assertNotEquals(result[0], result[2]);
    }

    @Test
    void kmeansDeterministic() {
        var points = new ArrayList<double[]>();
        for (int i = 0; i < 10; i++) points.add(new double[]{(double) i});
        int[] r1 = SpectralRefactorChecker.kmeans(points, 3, 100);
        int[] r2 = SpectralRefactorChecker.kmeans(points, 3, 100);
        assertArrayEquals(r1, r2);
    }

    // -----------------------------------------------------------------------
    // Spectral clusters
    // -----------------------------------------------------------------------

    @Test
    void clustersEnd() {
        var clusters = SpectralRefactorChecker.spectralClusters(build("end"), 1);
        assertEquals(1, clusters.size());
        assertTrue(clusters.values().stream().allMatch(v -> v == 0));
    }

    @Test
    void clustersBranch() {
        StateSpace ss = build("&{a: end, b: end}");
        var clusters = SpectralRefactorChecker.spectralClusters(ss, 2);
        assertEquals(ss.states().size(), clusters.size());
        assertTrue(clusters.values().stream().allMatch(v -> v >= 0 && v < 2));
    }

    @Test
    void clustersAllStatesAssigned() {
        StateSpace ss = build("&{a: &{c: end, d: end}, b: end}");
        var clusters = SpectralRefactorChecker.spectralClusters(ss, 2);
        assertEquals(ss.states(), clusters.keySet());
    }

    @Test
    void clustersKOne() {
        var clusters = SpectralRefactorChecker.spectralClusters(build("&{a: end, b: end}"), 1);
        assertTrue(clusters.values().stream().allMatch(v -> v == 0));
    }

    @Test
    void clustersKEqualsN() {
        StateSpace ss = build("&{a: end, b: end}");
        int n = ss.states().size();
        var clusters = SpectralRefactorChecker.spectralClusters(ss, n);
        assertTrue(new HashSet<>(clusters.values()).size() <= n);
    }

    // -----------------------------------------------------------------------
    // Reconstruct from clusters
    // -----------------------------------------------------------------------

    @Test
    void reconstructIdentityClusters() {
        StateSpace ss = build("&{a: end, b: end}");
        Map<Integer, Integer> clusters = new HashMap<>();
        int i = 0;
        for (int s : new TreeSet<>(ss.states())) clusters.put(s, i++);
        StateSpace r = SpectralRefactorChecker.reconstructFromClusters(ss, clusters);
        assertEquals(ss.states().size(), r.states().size());
    }

    @Test
    void reconstructSingleCluster() {
        StateSpace ss = build("&{a: end, b: end}");
        Map<Integer, Integer> clusters = new HashMap<>();
        for (int s : ss.states()) clusters.put(s, 0);
        StateSpace r = SpectralRefactorChecker.reconstructFromClusters(ss, clusters);
        assertEquals(1, r.states().size());
    }

    @Test
    void reconstructPreservesTop() {
        StateSpace ss = build("&{a: &{c: end, d: end}, b: end}");
        var clusters = SpectralRefactorChecker.spectralClusters(ss, 2);
        StateSpace r = SpectralRefactorChecker.reconstructFromClusters(ss, clusters);
        assertTrue(r.states().contains(r.top()));
    }

    @Test
    void reconstructPreservesBottom() {
        StateSpace ss = build("&{a: &{c: end, d: end}, b: end}");
        var clusters = SpectralRefactorChecker.spectralClusters(ss, 2);
        StateSpace r = SpectralRefactorChecker.reconstructFromClusters(ss, clusters);
        assertTrue(r.states().contains(r.bottom()));
    }

    @Test
    void reconstructNoSelfLoops() {
        StateSpace ss = build("&{a: &{c: end, d: end}, b: end}");
        var clusters = SpectralRefactorChecker.spectralClusters(ss, 2);
        StateSpace r = SpectralRefactorChecker.reconstructFromClusters(ss, clusters);
        for (var t : r.transitions()) {
            assertNotEquals(t.source(), t.target());
        }
    }

    @Test
    void reconstructReducesStates() {
        StateSpace ss = build("&{a: end, b: end, c: end, d: end}");
        var clusters = SpectralRefactorChecker.spectralClusters(ss, 2);
        StateSpace r = SpectralRefactorChecker.reconstructFromClusters(ss, clusters);
        assertTrue(r.states().size() <= ss.states().size());
    }

    // -----------------------------------------------------------------------
    // Round-trip analysis
    // -----------------------------------------------------------------------

    @Test
    void roundTripEnd() {
        var rt = SpectralRefactorChecker.roundTripAnalysis(build("end"), 1);
        assertEquals(1.0, (double) rt.get("state_preservation"), 1e-10);
        assertTrue((Boolean) rt.get("top_bottom_preserved"));
    }

    @Test
    void roundTripBranch() {
        var rt = SpectralRefactorChecker.roundTripAnalysis(build("&{a: end, b: end}"), 2);
        assertTrue(rt.containsKey("state_preservation"));
        assertTrue(rt.containsKey("transition_preservation"));
        assertTrue(rt.containsKey("lattice_preserved"));
        double sp = ((Number) rt.get("state_preservation")).doubleValue();
        assertTrue(sp >= 0.0 && sp <= 1.0);
    }

    @Test
    void roundTripPreservesCounts() {
        StateSpace ss = build("&{a: &{c: end, d: end}, b: end}");
        var rt = SpectralRefactorChecker.roundTripAnalysis(ss, 2);
        assertEquals(ss.states().size(), (int) rt.get("original_states"));
        assertTrue((int) rt.get("reconstructed_states") <= ss.states().size());
    }

    @Test
    void roundTripIdentity() {
        StateSpace ss = build("&{a: end, b: end}");
        var rt = SpectralRefactorChecker.roundTripAnalysis(ss, ss.states().size());
        assertTrue((Boolean) rt.get("top_bottom_preserved"));
    }

    @Test
    void roundTripSelectionPreservation() {
        var rt = SpectralRefactorChecker.roundTripAnalysis(
                build("&{a: +{x: end, y: end}, b: end}"), 2);
        assertTrue(rt.containsKey("selection_preservation"));
    }

    // -----------------------------------------------------------------------
    // Compare original refactored
    // -----------------------------------------------------------------------

    @Test
    void compareIdentical() {
        StateSpace ss = build("&{a: end, b: end}");
        var result = SpectralRefactorChecker.compareOriginalRefactored(ss, ss);
        assertEquals(0.0, (double) result.get("state_reduction"), 1e-10);
        assertTrue((Boolean) result.get("methods_preserved"));
    }

    @Test
    void compareReduced() {
        StateSpace ss = build("&{a: &{c: end, d: end}, b: end}");
        var pairs = SpectralRefactorChecker.redundantStates(ss, 1.0);
        StateSpace merged = SpectralRefactorChecker.mergeRedundant(ss, pairs);
        var result = SpectralRefactorChecker.compareOriginalRefactored(ss, merged);
        assertEquals(ss.states().size(), (int) result.get("original_states"));
        assertTrue((int) result.get("refactored_states") <= ss.states().size());
    }

    @Test
    void compareReportsLostMethods() {
        StateSpace ss1 = manualSs(Set.of(0, 1, 2),
                List.of(new Transition(0, "a", 1), new Transition(0, "b", 2)),
                0, 2);
        StateSpace ss2 = manualSs(Set.of(0, 1),
                List.of(new Transition(0, "a", 1)),
                0, 1);
        var result = SpectralRefactorChecker.compareOriginalRefactored(ss1, ss2);
        @SuppressWarnings("unchecked")
        Set<String> lost = (Set<String>) result.get("lost_methods");
        assertTrue(lost.contains("b"));
    }

    @Test
    void compareLatticeCheck() {
        StateSpace ss = build("&{a: end, b: end}");
        var result = SpectralRefactorChecker.compareOriginalRefactored(ss, ss);
        assertTrue(result.containsKey("original_is_lattice"));
        assertTrue(result.containsKey("refactored_is_lattice"));
    }

    // -----------------------------------------------------------------------
    // Pipeline
    // -----------------------------------------------------------------------

    @Test
    void pipelineEnd() {
        var analysis = SpectralRefactorChecker.refactoringPipeline(build("end"));
        assertEquals(1, analysis.numStates());
    }

    @Test
    void pipelineBranch() {
        var analysis = SpectralRefactorChecker.refactoringPipeline(build("&{a: end, b: end}"));
        assertEquals("fiedler", analysis.fiedlerPlan().approach());
        assertEquals("degeneracy", analysis.degeneracyPlan().approach());
        assertEquals("clustering", analysis.clusteringPlan().approach());
    }

    @Test
    void pipelineEmbedding() {
        StateSpace ss = build("&{a: &{c: end, d: end}, b: end}");
        var analysis = SpectralRefactorChecker.refactoringPipeline(ss);
        assertEquals(ss.states().size(), analysis.embedding().stateIds().size());
    }

    @Test
    void pipelineDegeneracyGroups() {
        var analysis = SpectralRefactorChecker.refactoringPipeline(
                build("&{a: end, b: end, c: end, d: end}"));
        for (var g : analysis.degeneracyGroups()) {
            assertTrue(g.multiplicity() > 1);
        }
    }

    @Test
    void pipelineRedundantPairs() {
        var analysis = SpectralRefactorChecker.refactoringPipeline(
                build("&{a: &{c: end, d: end}, b: end}"));
        for (var p : analysis.redundantPairs()) {
            assertNotEquals(p.stateA(), p.stateB());
        }
    }

    @Test
    void pipelineRoundTripLossBounded() {
        var analysis = SpectralRefactorChecker.refactoringPipeline(build("&{a: end, b: end}"));
        assertTrue(analysis.roundTripLoss() >= 0.0);
        assertTrue(analysis.roundTripLoss() <= 1.0);
    }

    @Test
    void pipelineNumClusters() {
        var analysis = SpectralRefactorChecker.refactoringPipeline(
                build("&{a: &{c: end, d: end}, b: end}"));
        assertTrue(analysis.numClusters() >= 1);
    }

    @Test
    void pipelineStatesAfterMerge() {
        var analysis = SpectralRefactorChecker.refactoringPipeline(build("&{a: end, b: end}"));
        assertTrue(analysis.numStatesAfterMerge() <= analysis.numStates());
    }

    @Test
    void pipelineParallel() {
        StateSpace ss = build("(&{a: end} || &{b: end})");
        var analysis = SpectralRefactorChecker.refactoringPipeline(ss);
        assertEquals(ss.states().size(), analysis.numStates());
    }

    @Test
    void pipelineRecursive() {
        var analysis = SpectralRefactorChecker.refactoringPipeline(
                build("rec X . &{a: X, b: end}"));
        assertNotNull(analysis);
    }

    @Test
    void pipelineMixed() {
        var analysis = SpectralRefactorChecker.refactoringPipeline(
                build("&{a: +{x: end, y: end}, b: end}"));
        assertEquals("fiedler", analysis.fiedlerPlan().approach());
    }

    // -----------------------------------------------------------------------
    // Benchmark protocols
    // -----------------------------------------------------------------------

    @Test
    void smtpLike() {
        var analysis = SpectralRefactorChecker.refactoringPipeline(
                build("&{connect: &{auth: +{OK: &{send: &{quit: end}}, FAIL: &{quit: end}}}}"));
        assertTrue(analysis.numStates() > 1);
    }

    @Test
    void twoBuyer() {
        var analysis = SpectralRefactorChecker.refactoringPipeline(
                build("&{request: +{accept: &{pay: end}, reject: end}}"));
        assertEquals("fiedler", analysis.fiedlerPlan().approach());
    }

    @Test
    void iterator() {
        var analysis = SpectralRefactorChecker.refactoringPipeline(
                build("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}"));
        assertNotNull(analysis);
    }

    @Test
    void parallelProtocol() {
        var analysis = SpectralRefactorChecker.refactoringPipeline(
                build("(&{read: end} || &{write: end})"));
        assertTrue(analysis.numStates() >= 4);
    }

    @Test
    void deepNesting() {
        var analysis = SpectralRefactorChecker.refactoringPipeline(
                build("&{a: &{b: &{c: &{d: end}}}}"));
        assertEquals(5, analysis.numStates());
    }

    @Test
    void wideSelection() {
        var analysis = SpectralRefactorChecker.refactoringPipeline(
                build("+{a: end, b: end, c: end}"));
        assertTrue(analysis.numStates() > 1);
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Test
    void singleState() {
        var emb = SpectralRefactorChecker.spectralEmbedding(build("end"), 1);
        assertEquals(1, emb.stateIds().size());
    }

    @Test
    void twoStates() {
        var emb = SpectralRefactorChecker.spectralEmbedding(build("&{a: end}"), 1);
        assertEquals(2, emb.stateIds().size());
    }

    @Test
    void largeKClamped() {
        StateSpace ss = build("&{a: end}");
        var emb = SpectralRefactorChecker.spectralEmbedding(ss, 100);
        assertTrue(emb.k() <= ss.states().size());
    }

    @Test
    void zeroToleranceRedundancy() {
        var pairs = SpectralRefactorChecker.redundantStates(build("&{a: end, b: end}"), 0.0);
        for (var p : pairs) {
            assertEquals(0.0, p.distance(), 1e-10);
        }
    }

    @Test
    void mergeChainResolution() {
        StateSpace ss = manualSs(
                Set.of(0, 1, 2, 3, 4),
                List.of(new Transition(0, "a", 1), new Transition(0, "b", 2),
                        new Transition(0, "c", 3), new Transition(1, "d", 4),
                        new Transition(2, "d", 4), new Transition(3, "d", 4)),
                0, 4);
        var pairs = List.of(
                new RedundantPair(1, 2, 0.0, new double[]{0.0}, new double[]{0.0}),
                new RedundantPair(2, 3, 0.0, new double[]{0.0}, new double[]{0.0}));
        StateSpace merged = SpectralRefactorChecker.mergeRedundant(ss, pairs);
        assertTrue(merged.states().size() <= 3);
    }

    @Test
    void clustersEmptyStateSpace() {
        StateSpace ss = manualSs(Set.of(0), List.of(), 0, 0);
        var clusters = SpectralRefactorChecker.spectralClusters(ss, 1);
        assertEquals(Map.of(0, 0), clusters);
    }

    @Test
    void pipelineWithSelections() {
        var analysis = SpectralRefactorChecker.refactoringPipeline(
                build("+{ok: end, err: end}"));
        assertTrue(analysis.numStates() > 1);
    }
}
