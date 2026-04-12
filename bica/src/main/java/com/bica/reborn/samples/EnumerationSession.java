package com.bica.reborn.samples;

import com.bica.reborn.annotation.Session;
import com.bica.reborn.enumerate.EnumerationConfig;
import com.bica.reborn.enumerate.TypeEnumerator;
import com.bica.reborn.enumerate.UniversalityResult;
import com.bica.reborn.exception.ProtocolViolationException;

/**
 * Session-typed type enumeration protocol.
 *
 * <p>Session type:
 * {@code configure . enumerate . +{ALL_LATTICES: getCount . end, HAS_COUNTEREXAMPLE: getCounterexample . end}}
 *
 * <p>Protocol: configure enumeration parameters, enumerate all types up to a bound,
 * then the result tells whether all are lattices or a counterexample exists.
 */
@Session("configure . enumerate . +{ALL_LATTICES: getCount . end, HAS_COUNTEREXAMPLE: getCounterexample . end}")
public class EnumerationSession {

    public enum UniversalityDecision { ALL_LATTICES, HAS_COUNTEREXAMPLE }

    private enum State { INITIAL, CONFIGURED, ALL_LATTICES, HAS_COUNTEREXAMPLE, DONE }
    private State state = State.INITIAL;

    private EnumerationConfig config;
    private UniversalityResult result;

    public void configure(int depth, int width) {
        requireState("configure", State.INITIAL);
        config = EnumerationConfig.defaults()
                .withMaxDepth(depth)
                .withMaxBranchWidth(width);
        state = State.CONFIGURED;
    }

    public UniversalityDecision enumerate() {
        requireState("enumerate", State.CONFIGURED);
        result = TypeEnumerator.checkUniversality(config);
        if (result.isUniversal()) {
            state = State.ALL_LATTICES;
            return UniversalityDecision.ALL_LATTICES;
        } else {
            state = State.HAS_COUNTEREXAMPLE;
            return UniversalityDecision.HAS_COUNTEREXAMPLE;
        }
    }

    public int getCount() {
        requireState("getCount", State.ALL_LATTICES);
        state = State.DONE;
        return result.totalTypes();
    }

    public String getCounterexample() {
        requireState("getCounterexample", State.HAS_COUNTEREXAMPLE);
        state = State.DONE;
        return result.counterexamples().isEmpty()
                ? "unknown"
                : result.counterexamples().getFirst().typeString();
    }

    private void requireState(String method, State expected) {
        if (state != expected)
            throw new ProtocolViolationException(method, state.name());
    }
}
