package com.bica.reborn.quantum_walk;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.quantum_walk.QuantumWalkChecker.QuantumWalkResult;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java port of {@code reticulate/tests/test_quantum_walk.py} (Step 31c).
 */
class QuantumWalkCheckerTest {

    private static StateSpace ss(String t) {
        return StateSpaceBuilder.build(Parser.parse(t));
    }

    // -------------------- Evolution --------------------

    @Test
    void evolutionEnd() {
        double[][] P = QuantumWalkChecker.quantumEvolution(ss("end"), 1.0);
        assertEquals(1, P.length);
        assertEquals(1.0, P[0][0], 0.01);
    }

    @ParameterizedTest
    @ValueSource(strings = {"&{a: end}", "&{a: &{b: end}}", "(&{a: end} || &{b: end})"})
    void evolutionProbsNonneg(String typ) {
        double[][] P = QuantumWalkChecker.quantumEvolution(ss(typ), 1.0);
        for (double[] row : P)
            for (double val : row)
                assertTrue(val >= -0.01, "Probability should be non-negative, got " + val);
    }

    @ParameterizedTest
    @ValueSource(strings = {"&{a: end}", "&{a: &{b: end}}"})
    void evolutionRowsSumApproxOne(String typ) {
        double[][] P = QuantumWalkChecker.quantumEvolution(ss(typ), 1.0);
        for (double[] row : P) {
            double sum = 0.0;
            for (double v : row) sum += v;
            assertEquals(1.0, sum, 0.1, "Row should sum to ~1");
        }
    }

    @Test
    void evolutionAtTZero() {
        double[][] P = QuantumWalkChecker.quantumEvolution(ss("&{a: end}"), 0.0);
        int n = P.length;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                double expected = (i == j) ? 1.0 : 0.0;
                assertEquals(expected, P[i][j], 0.01);
            }
    }

    // -------------------- Return probability --------------------

    @Test
    void returnProbabilityEnd() {
        assertEquals(1.0, QuantumWalkChecker.returnProbability(ss("end"), 1.0), 0.01);
    }

    @ParameterizedTest
    @ValueSource(strings = {"&{a: end}", "&{a: &{b: end}}"})
    void returnProbabilityPositive(String typ) {
        double rp = QuantumWalkChecker.returnProbability(ss(typ), 1.0);
        assertTrue(rp >= 0.0 && rp <= 1.01, "Return probability should be in [0,1], got " + rp);
    }

    // -------------------- Top to bottom --------------------

    @Test
    void topToBottomEnd() {
        assertEquals(1.0, QuantumWalkChecker.topToBottomProbability(ss("end"), 1.0), 0.01);
    }

    @ParameterizedTest
    @ValueSource(strings = {"&{a: end}", "(&{a: end} || &{b: end})"})
    void topToBottomRange(String typ) {
        double p = QuantumWalkChecker.topToBottomProbability(ss(typ), 1.0);
        assertTrue(p >= 0.0 && p <= 1.01, "Probability should be in [0,1], got " + p);
    }

    // -------------------- Spread --------------------

    @Test
    void spreadEnd() {
        assertTrue(QuantumWalkChecker.quantumSpread(ss("end"), 1.0) < 0.01);
    }

    @ParameterizedTest
    @ValueSource(strings = {"&{a: end}", "&{a: &{b: end}}"})
    void spreadNonneg(String typ) {
        double s = QuantumWalkChecker.quantumSpread(ss(typ), 1.0);
        assertTrue(s >= -0.01, "Spread should be non-negative, got " + s);
    }

    // -------------------- Mixing --------------------

    @Test
    void mixingEnd() {
        assertEquals(0.0, QuantumWalkChecker.quantumMixingEstimate(ss("end")));
    }

    @Test
    void mixingPositive() {
        double m = QuantumWalkChecker.quantumMixingEstimate(ss("&{a: end}"));
        assertTrue(m >= 0.0, "Mixing estimate should be non-negative, got " + m);
    }

    // -------------------- Full analysis --------------------

    @Test
    void analyzeEnd() {
        QuantumWalkResult r = QuantumWalkChecker.analyzeQuantumWalk(ss("end"));
        assertEquals(1, r.numStates());
    }

    @Test
    void analyzeChain() {
        QuantumWalkResult r = QuantumWalkChecker.analyzeQuantumWalk(ss("&{a: end}"));
        assertEquals(2, r.numStates());
        assertTrue(r.returnProbability() >= 0.0 && r.returnProbability() <= 1.01);
    }

    @Test
    void analyzeParallel() {
        QuantumWalkResult r = QuantumWalkChecker.analyzeQuantumWalk(ss("(&{a: end} || &{b: end})"));
        assertEquals(4, r.numStates());
    }

    // -------------------- Benchmarks --------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "&{open: &{read: end, write: end}}",
            "&{EHLO: &{MAIL: &{RCPT: &{DATA: end}}}}",
            "&{a: &{b: &{c: end}}}",
            "(&{a: end} || &{b: end})"
    })
    void benchmarkQuantumWalkRuns(String typ) {
        StateSpace s = ss(typ);
        QuantumWalkResult r = QuantumWalkChecker.analyzeQuantumWalk(s);
        assertEquals(s.states().size(), r.numStates());
        assertTrue(r.returnProbability() >= 0.0 && r.returnProbability() <= 1.01);
        assertTrue(r.spread() >= -0.01);
    }
}
