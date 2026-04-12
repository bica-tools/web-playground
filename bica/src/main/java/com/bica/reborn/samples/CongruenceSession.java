package com.bica.reborn.samples;

import com.bica.reborn.annotation.Session;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.congruence.Congruence;
import com.bica.reborn.congruence.CongruenceChecker;
import com.bica.reborn.exception.ProtocolViolationException;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.List;

/**
 * Session-typed congruence analysis pipeline.
 *
 * <p>Session type:
 * {@code parse . buildStateSpace . checkLattice . +{IS_LATTICE: &{analyzeCongruences: getResult . end, isSimple: getResult . end, enumerateCongruences: getResult . end}, NOT_LATTICE: getResult . end}}
 *
 * <p>Protocol: parse a session type string, build its state space, check if it
 * forms a lattice (selection: IS_LATTICE or NOT_LATTICE), then either analyze
 * congruences, check simplicity, or enumerate congruences before getting the result.
 */
@Session("parse . buildStateSpace . checkLattice . +{IS_LATTICE: &{analyzeCongruences: getResult . end, isSimple: getResult . end, enumerateCongruences: getResult . end}, NOT_LATTICE: getResult . end}")
public class CongruenceSession {

    public enum LatticeDecision { IS_LATTICE, NOT_LATTICE }

    private enum State { INITIAL, PARSED, BUILT, CHECKED, ANALYZED, DONE }
    private State state = State.INITIAL;

    private SessionType ast;
    private StateSpace stateSpace;
    private LatticeResult latticeResult;
    private String resultText;

    public void parse(String input) {
        requireState("parse", State.INITIAL);
        ast = Parser.parse(input);
        state = State.PARSED;
    }

    public void buildStateSpace() {
        requireState("buildStateSpace", State.PARSED);
        stateSpace = StateSpaceBuilder.build(ast);
        state = State.BUILT;
    }

    public LatticeDecision checkLattice() {
        requireState("checkLattice", State.BUILT);
        latticeResult = LatticeChecker.checkLattice(stateSpace);
        state = State.CHECKED;
        return latticeResult.isLattice() ? LatticeDecision.IS_LATTICE : LatticeDecision.NOT_LATTICE;
    }

    public void analyzeCongruences() {
        requireState("analyzeCongruences", State.CHECKED);
        if (!latticeResult.isLattice()) throw new ProtocolViolationException("analyzeCongruences", "NOT_LATTICE");
        var analysis = CongruenceChecker.analyzeCongruences(stateSpace);
        resultText = "congruences=" + analysis.numCongruences()
                + " simple=" + analysis.isSimple()
                + " modular=" + analysis.isModular();
        state = State.ANALYZED;
    }

    public void isSimple() {
        requireState("isSimple", State.CHECKED);
        if (!latticeResult.isLattice()) throw new ProtocolViolationException("isSimple", "NOT_LATTICE");
        boolean simple = CongruenceChecker.isSimple(stateSpace);
        resultText = "simple=" + simple;
        state = State.ANALYZED;
    }

    public void enumerateCongruences() {
        requireState("enumerateCongruences", State.CHECKED);
        if (!latticeResult.isLattice()) throw new ProtocolViolationException("enumerateCongruences", "NOT_LATTICE");
        List<Congruence> congs = CongruenceChecker.enumerateCongruences(stateSpace);
        resultText = "congruences=" + congs.size();
        state = State.ANALYZED;
    }

    public String getResult() {
        if (state != State.CHECKED && state != State.ANALYZED)
            throw new ProtocolViolationException("getResult", state.name());
        state = State.DONE;
        return resultText != null ? resultText : "isLattice=" + latticeResult.isLattice();
    }

    private void requireState(String method, State expected) {
        if (state != expected)
            throw new ProtocolViolationException(method, state.name());
    }
}
