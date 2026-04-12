package com.bica.reborn.samples;

import com.bica.reborn.annotation.Session;
import com.bica.reborn.endomorphism.EndomorphismChecker;
import com.bica.reborn.endomorphism.EndomorphismSummary;
import com.bica.reborn.exception.ProtocolViolationException;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

/**
 * Session-typed endomorphism analysis protocol.
 *
 * <p>Session type:
 * {@code parseType . analyzeEndomorphisms . &{getSummary: end, getPreservationRate: end}}
 *
 * <p>Protocol: parse a session type, analyze transition label endomorphisms,
 * then query the summary or whether all are endomorphisms.
 */
@Session("parseType . analyzeEndomorphisms . &{getSummary: end, getPreservationRate: end}")
public class EndomorphismSession {

    private enum State { INITIAL, PARSED, ANALYZED, DONE }
    private State state = State.INITIAL;

    private StateSpace stateSpace;
    private EndomorphismSummary summary;

    public void parseType(String source) {
        requireState("parseType", State.INITIAL);
        stateSpace = StateSpaceBuilder.build(Parser.parse(source));
        state = State.PARSED;
    }

    public void analyzeEndomorphisms() {
        requireState("analyzeEndomorphisms", State.PARSED);
        summary = EndomorphismChecker.checkAll(stateSpace);
        state = State.ANALYZED;
    }

    public String getSummary() {
        requireState("getSummary", State.ANALYZED);
        state = State.DONE;
        return "total=" + summary.numLabels()
                + " allEndomorphisms=" + summary.allEndomorphisms();
    }

    public boolean getPreservationRate() {
        requireState("getPreservationRate", State.ANALYZED);
        state = State.DONE;
        return summary.allEndomorphisms();
    }

    private void requireState(String method, State expected) {
        if (state != expected)
            throw new ProtocolViolationException(method, state.name());
    }
}
