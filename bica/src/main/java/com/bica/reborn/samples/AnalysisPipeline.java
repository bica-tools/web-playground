package com.bica.reborn.samples;

import com.bica.reborn.annotation.Session;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.exception.ProtocolViolationException;
import com.bica.reborn.lattice.DistributivityChecker;
import com.bica.reborn.lattice.DistributivityResult;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

/**
 * Session-typed analysis pipeline.
 *
 * <p>Session type:
 * {@code parse . buildStateSpace . &{checkLattice: +{IS_LATTICE: &{checkDistributive: getResult . end, getResult: end}, NOT_LATTICE: getResult . end}, getResult: end}}
 *
 * <p>Protocol: parse a session type string, build its state space, then either
 * check lattice (and optionally distributivity) or just get the raw result.
 * The lattice check produces a selection: IS_LATTICE or NOT_LATTICE.
 */
@Session("parse . buildStateSpace . &{checkLattice: +{IS_LATTICE: &{checkDistributive: getResult . end, getResult: end}, NOT_LATTICE: getResult . end}, getResult: end}")
public class AnalysisPipeline {

    public enum LatticeDecision { IS_LATTICE, NOT_LATTICE }

    private enum State { INITIAL, PARSED, BUILT, LATTICE_CHECKED, DISTRIBUTIVE_CHECKED, DONE }
    private State state = State.INITIAL;

    private SessionType ast;
    private StateSpace stateSpace;
    private LatticeResult latticeResult;
    private DistributivityResult distributivityResult;

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
        state = State.LATTICE_CHECKED;
        return latticeResult.isLattice() ? LatticeDecision.IS_LATTICE : LatticeDecision.NOT_LATTICE;
    }

    public void checkDistributive() {
        requireState("checkDistributive", State.LATTICE_CHECKED);
        distributivityResult = DistributivityChecker.checkDistributive(stateSpace);
        state = State.DISTRIBUTIVE_CHECKED;
    }

    public String getResult() {
        if (state != State.BUILT && state != State.LATTICE_CHECKED && state != State.DISTRIBUTIVE_CHECKED)
            throw new ProtocolViolationException("getResult", state.name());
        state = State.DONE;
        if (distributivityResult != null) {
            return "classification=" + distributivityResult.classification();
        } else if (latticeResult != null) {
            return "isLattice=" + latticeResult.isLattice();
        } else {
            return "states=" + stateSpace.states().size();
        }
    }

    private void requireState(String method, State expected) {
        if (state != expected)
            throw new ProtocolViolationException(method, state.name());
    }
}
