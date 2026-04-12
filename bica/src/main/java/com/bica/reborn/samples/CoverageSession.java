package com.bica.reborn.samples;

import com.bica.reborn.annotation.Session;
import com.bica.reborn.coverage.CoverageChecker;
import com.bica.reborn.coverage.CoverageResult;
import com.bica.reborn.exception.ProtocolViolationException;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.testgen.PathEnumerator;
import com.bica.reborn.testgen.TestGenConfig;

/**
 * Session-typed coverage analysis protocol.
 *
 * <p>Session type:
 * {@code parseType . enumeratePaths . computeCoverage . &{getStateCoverage: end, getTransitionCoverage: end}}
 *
 * <p>Protocol: parse, enumerate test paths, compute coverage, then query either
 * state coverage or transition coverage.
 */
@Session("parseType . enumeratePaths . computeCoverage . &{getStateCoverage: end, getTransitionCoverage: end}")
public class CoverageSession {

    private enum State { INITIAL, PARSED, PATHS_READY, COVERAGE_COMPUTED, DONE }
    private State state = State.INITIAL;

    private StateSpace stateSpace;
    private PathEnumerator.EnumerationResult enumResult;
    private CoverageResult coverageResult;

    public void parseType(String source) {
        requireState("parseType", State.INITIAL);
        stateSpace = StateSpaceBuilder.build(Parser.parse(source));
        state = State.PARSED;
    }

    public void enumeratePaths() {
        requireState("enumeratePaths", State.PARSED);
        var config = TestGenConfig.withDefaults("CoverageTarget");
        enumResult = PathEnumerator.enumerate(stateSpace, config);
        state = State.PATHS_READY;
    }

    public void computeCoverage() {
        requireState("computeCoverage", State.PATHS_READY);
        coverageResult = CoverageChecker.computeCoverage(stateSpace, enumResult);
        state = State.COVERAGE_COMPUTED;
    }

    public double getStateCoverage() {
        requireState("getStateCoverage", State.COVERAGE_COMPUTED);
        state = State.DONE;
        return coverageResult.stateCoverage();
    }

    public double getTransitionCoverage() {
        requireState("getTransitionCoverage", State.COVERAGE_COMPUTED);
        state = State.DONE;
        return coverageResult.transitionCoverage();
    }

    private void requireState(String method, State expected) {
        if (state != expected)
            throw new ProtocolViolationException(method, state.name());
    }
}
