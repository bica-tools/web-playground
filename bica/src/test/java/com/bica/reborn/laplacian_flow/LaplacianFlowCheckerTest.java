package com.bica.reborn.laplacian_flow;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Laplacian optimization of protocol connectivity (Step 31h).
 * Java port of Python {@code test_laplacian_flow.py}.
 */
class LaplacianFlowCheckerTest {

    private StateSpace build(String type) {
        return StateSpaceBuilder.build(Parser.parse(type));
    }

    // -----------------------------------------------------------------------
    // Laplacian gradient
    // -----------------------------------------------------------------------

    @Test
    void gradientEndEmpty() {
        var grad = LaplacianFlowChecker.laplacianGradient(build("end"));
        assertTrue(grad.isEmpty());
    }

    @Test
    void gradientSimpleBranchEmpty() {
        var grad = LaplacianFlowChecker.laplacianGradient(build("&{a: end}"));
        // 2 states, already complete: no candidates
        assertTrue(grad.isEmpty());
    }

    @Test
    void gradientChainHasEntries() {
        var grad = LaplacianFlowChecker.laplacianGradient(build("&{a: &{b: end}}"));
        // 3 states: chain 0-1-2, missing edge 0-2
        assertTrue(grad.size() >= 1, "chain should have gradient entries");
        for (double val : grad.values()) {
            assertTrue(val >= 0.0, "gradient value should be non-negative");
        }
    }

    @Test
    void gradientNonnegative() {
        var grad = LaplacianFlowChecker.laplacianGradient(build("&{a: &{b: &{c: end}}}"));
        for (double val : grad.values()) {
            assertTrue(val >= -1e-10, "gradient value should be non-negative");
        }
    }

    @Test
    void gradientDiamond() {
        var grad = LaplacianFlowChecker.laplacianGradient(build("&{a: &{c: end}, b: &{c: end}}"));
        for (double val : grad.values()) {
            assertTrue(val >= -1e-10, "gradient should be non-negative");
        }
    }

    // -----------------------------------------------------------------------
    // Optimize connectivity
    // -----------------------------------------------------------------------

    @Test
    void optimizeEndNoCandidates() {
        var cands = LaplacianFlowChecker.optimizeConnectivity(build("end"), 3);
        assertTrue(cands.isEmpty());
    }

    @Test
    void optimizeSimpleBranchNoCandidates() {
        var cands = LaplacianFlowChecker.optimizeConnectivity(build("&{a: end}"), 3);
        assertTrue(cands.isEmpty());
    }

    @Test
    void optimizeChainHasCandidates() {
        var cands = LaplacianFlowChecker.optimizeConnectivity(build("&{a: &{b: end}}"), 3);
        assertTrue(cands.size() >= 1);
        for (var c : cands) {
            assertTrue(c.fiedlerGain() >= -1e-10);
        }
    }

    @Test
    void candidatesSortedByGain() {
        var cands = LaplacianFlowChecker.optimizeConnectivity(build("&{a: &{b: &{c: end}}}"), 5);
        for (int i = 0; i < cands.size() - 1; i++) {
            assertTrue(cands.get(i).fiedlerGain() >= cands.get(i + 1).fiedlerGain() - 1e-10);
        }
    }

    @Test
    void kLimitsCandidateOutput() {
        var cands = LaplacianFlowChecker.optimizeConnectivity(build("&{a: &{b: &{c: end}}}"), 1);
        assertTrue(cands.size() <= 1);
    }

    @Test
    void parallelCandidates() {
        var cands = LaplacianFlowChecker.optimizeConnectivity(build("(&{a: end} || &{b: end})"), 3);
        assertNotNull(cands);
    }

    @Test
    void candidateStatesValid() {
        StateSpace ss = build("&{a: &{b: end}}");
        var cands = LaplacianFlowChecker.optimizeConnectivity(ss, 3);
        for (var c : cands) {
            assertTrue(ss.states().contains(c.src()), "src should be a valid state");
            assertTrue(ss.states().contains(c.tgt()), "tgt should be a valid state");
        }
    }

    // -----------------------------------------------------------------------
    // Connectivity score
    // -----------------------------------------------------------------------

    @Test
    void scoreEnd() {
        assertEquals(1.0, LaplacianFlowChecker.connectivityScore(build("end")));
    }

    @Test
    void scoreSimpleBranch() {
        double score = LaplacianFlowChecker.connectivityScore(build("&{a: end}"));
        assertTrue(score >= 0.0 && score <= 1.0, "score=" + score);
    }

    @Test
    void scoreChain() {
        double score = LaplacianFlowChecker.connectivityScore(build("&{a: &{b: end}}"));
        assertTrue(score > 0.0 && score <= 1.0, "score=" + score);
    }

    @Test
    void scoreBounded() {
        double score = LaplacianFlowChecker.connectivityScore(build("&{a: &{b: &{c: end}}}"));
        assertTrue(score >= 0.0 && score <= 1.0, "score=" + score);
    }

    @Test
    void widerHasHigherScore() {
        double chain = LaplacianFlowChecker.connectivityScore(build("&{a: &{b: end}}"));
        double diamond = LaplacianFlowChecker.connectivityScore(build("&{a: &{c: end}, b: &{c: end}}"));
        // Diamond should have at least as good connectivity
        assertTrue(diamond >= chain - 0.1, "diamond=" + diamond + " < chain=" + chain);
    }

    // -----------------------------------------------------------------------
    // Bottleneck edges
    // -----------------------------------------------------------------------

    @Test
    void bottleneckEndEmpty() {
        assertTrue(LaplacianFlowChecker.bottleneckEdges(build("end"), 3).isEmpty());
    }

    @Test
    void bottleneckSimpleBranchEmpty() {
        assertTrue(LaplacianFlowChecker.bottleneckEdges(build("&{a: end}"), 3).isEmpty());
    }

    @Test
    void chainAllBridges() {
        var bn = LaplacianFlowChecker.bottleneckEdges(build("&{a: &{b: end}}"), 3);
        for (var b : bn) {
            assertTrue(b.isBridge(), "chain edges should all be bridges");
        }
    }

    @Test
    void bottlenecksSortedByLoss() {
        var bn = LaplacianFlowChecker.bottleneckEdges(build("&{a: &{b: &{c: end}}}"), 5);
        for (int i = 0; i < bn.size() - 1; i++) {
            assertTrue(bn.get(i).fiedlerLoss() >= bn.get(i + 1).fiedlerLoss() - 1e-10);
        }
    }

    @Test
    void diamondHasBottlenecks() {
        var bn = LaplacianFlowChecker.bottleneckEdges(build("&{a: &{c: end}, b: &{c: end}}"), 3);
        assertTrue(bn.size() >= 1);
    }

    @Test
    void kLimitsBottleneckOutput() {
        var bn = LaplacianFlowChecker.bottleneckEdges(build("&{a: &{b: &{c: end}}}"), 1);
        assertTrue(bn.size() <= 1);
    }

    // -----------------------------------------------------------------------
    // Full improvement analysis
    // -----------------------------------------------------------------------

    @Test
    void improvementsEnd() {
        var result = LaplacianFlowChecker.suggestImprovements(build("end"), 5, 5);
        assertEquals(1, result.numStates());
    }

    @Test
    void improvementsSimpleBranch() {
        var result = LaplacianFlowChecker.suggestImprovements(build("&{a: end}"), 5, 5);
        assertEquals(2, result.numStates());
        assertTrue(result.connectivityScore() >= 0.0 && result.connectivityScore() <= 1.0);
    }

    @Test
    void improvementsChain() {
        var result = LaplacianFlowChecker.suggestImprovements(build("&{a: &{b: end}}"), 5, 5);
        assertEquals(3, result.numStates());
        assertTrue(result.fiedlerCurrent() > 0.0, "fiedler should be positive");
        assertTrue(result.improvementPotential() >= 0.0, "potential should be non-negative");
    }

    @Test
    void improvementsDiamond() {
        var result = LaplacianFlowChecker.suggestImprovements(build("&{a: &{c: end}, b: &{c: end}}"), 5, 5);
        assertTrue(result.fiedlerCurrent() > 0.0);
        assertTrue(result.maxPossibleFiedler() > 0.0);
    }

    @Test
    void improvementsParallel() {
        var result = LaplacianFlowChecker.suggestImprovements(build("(&{a: end} || &{b: end})"), 5, 5);
        assertTrue(result.numStates() >= 3);
    }

    @Test
    void improvementsRecursive() {
        var result = LaplacianFlowChecker.suggestImprovements(build("rec X . &{a: X, b: end}"), 5, 5);
        assertNotNull(result);
    }

    @Test
    void fiedlerAfterBestImprovement() {
        var result = LaplacianFlowChecker.suggestImprovements(build("&{a: &{b: &{c: end}}}"), 5, 5);
        if (!result.candidates().isEmpty()) {
            assertTrue(result.fiedlerAfterBest() >= result.fiedlerCurrent() - 1e-10);
        }
    }

    @Test
    void improvementPotentialBounded() {
        var result = LaplacianFlowChecker.suggestImprovements(build("&{a: &{b: end}}"), 5, 5);
        assertTrue(result.improvementPotential() >= 0.0
                && result.improvementPotential() <= 1.0);
    }

    @Test
    void numCandidatesParam() {
        var result = LaplacianFlowChecker.suggestImprovements(build("&{a: &{b: &{c: end}}}"), 1, 5);
        assertTrue(result.candidates().size() <= 1);
    }

    @Test
    void numBottlenecksParam() {
        var result = LaplacianFlowChecker.suggestImprovements(build("&{a: &{b: &{c: end}}}"), 5, 1);
        assertTrue(result.bottlenecks().size() <= 1);
    }
}
