package com.bica.reborn.samples;

import com.bica.reborn.annotation.Session;
import com.bica.reborn.exception.ProtocolViolationException;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.testgen.PathEnumerator;
import com.bica.reborn.testgen.TestGenConfig;
import com.bica.reborn.testgen.TestGenerator;

import java.util.List;

/**
 * Session-typed test generation protocol.
 *
 * <p>Session type:
 * {@code parseType . enumeratePaths . &{generateTests: end, getPathCount: end}}
 *
 * <p>Protocol: parse a session type, enumerate test paths, then either
 * generate test code or query path count.
 */
@Session("parseType . enumeratePaths . &{generateTests: end, getPathCount: end}")
public class TestGenSession {

    private enum State { INITIAL, BUILT, PATHS_ENUMERATED, DONE }
    private State state = State.INITIAL;

    private StateSpace stateSpace;
    private PathEnumerator.EnumerationResult enumResult;
    private String className = "GeneratedTest";

    public void parseType(String source) {
        requireState("parseType", State.INITIAL);
        var ast = Parser.parse(source);
        stateSpace = StateSpaceBuilder.build(ast);
        state = State.BUILT;
    }

    public void enumeratePaths() {
        requireState("enumeratePaths", State.BUILT);
        var config = TestGenConfig.withDefaults(className);
        enumResult = PathEnumerator.enumerate(stateSpace, config);
        state = State.PATHS_ENUMERATED;
    }

    public int getPathCount() {
        requireState("getPathCount", State.PATHS_ENUMERATED);
        state = State.DONE;
        return enumResult.validPaths().size();
    }

    public String generateTests() {
        if (state != State.BUILT && state != State.PATHS_ENUMERATED)
            throw new ProtocolViolationException("generateTests", state.name());
        state = State.DONE;
        var config = TestGenConfig.withDefaults(className);
        return TestGenerator.generate(stateSpace, config, "session type");
    }

    private void requireState(String method, State expected) {
        if (state != expected)
            throw new ProtocolViolationException(method, state.name());
    }
}
