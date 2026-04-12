package com.bica.reborn.samples;

import com.bica.reborn.exception.ProtocolViolationException;
import com.bica.reborn.annotation.Session;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.globaltype.*;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.statespace.StateSpace;

import java.util.Map;

/**
 * Session-typed global type analysis protocol.
 *
 * <p>Session type:
 * {@code parseGlobal . buildGlobalStateSpace . &{projectAll: checkProjections . end, checkLattice: end}}
 *
 * <p>Protocol: parse a global type, build its state space, then either project
 * onto all roles and check projection morphisms, or directly check lattice.
 */
@Session("parseGlobal . buildGlobalStateSpace . &{projectAll: checkProjections . end, checkLattice: end}")
public class GlobalTypeSession {

    private enum State { INITIAL, PARSED, BUILT, PROJECTED, DONE }
    private State state = State.INITIAL;

    private GlobalType globalType;
    private StateSpace globalSS;
    private Map<String, SessionType> projections;

    public void parseGlobal(String source) {
        requireState("parseGlobal", State.INITIAL);
        globalType = GlobalTypeParser.parse(source);
        state = State.PARSED;
    }

    public void buildGlobalStateSpace() {
        requireState("buildGlobalStateSpace", State.PARSED);
        globalSS = GlobalTypeChecker.buildStateSpace(globalType);
        state = State.BUILT;
    }

    public void projectAll() {
        requireState("projectAll", State.BUILT);
        projections = Projection.projectAll(globalType);
        state = State.PROJECTED;
    }

    public String checkProjections() {
        requireState("checkProjections", State.PROJECTED);
        state = State.DONE;
        StringBuilder sb = new StringBuilder();
        for (var entry : projections.entrySet()) {
            sb.append(entry.getKey()).append(": OK\n");
        }
        return sb.toString();
    }

    public String checkLattice() {
        requireState("checkLattice", State.BUILT);
        state = State.DONE;
        var lr = LatticeChecker.checkLattice(globalSS);
        return "isLattice=" + lr.isLattice();
    }

    private void requireState(String method, State expected) {
        if (state != expected)
            throw new ProtocolViolationException(method, state.name());
    }
}
