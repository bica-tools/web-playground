package com.bica.reborn.samples;

import com.bica.reborn.annotation.Session;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.exception.ProtocolViolationException;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.subtyping.SubtypingChecker;

/**
 * Session-typed subtyping checker protocol.
 *
 * <p>Session type:
 * {@code setLeft . setRight . checkSubtype . +{SUBTYPE: end, NOT_SUBTYPE: end}}
 *
 * <p>Protocol: set two session types, check if left is subtype of right,
 * then the result is a selection (SUBTYPE or NOT_SUBTYPE).
 */
@Session("setLeft . setRight . checkSubtype . +{SUBTYPE: end, NOT_SUBTYPE: end}")
public class SubtypingSession {

    public enum SubtypeDecision { SUBTYPE, NOT_SUBTYPE }

    private enum State { INITIAL, LEFT_SET, BOTH_SET, DONE }
    private State state = State.INITIAL;

    private SessionType left;
    private SessionType right;

    public void setLeft(String source) {
        requireState("setLeft", State.INITIAL);
        left = Parser.parse(source);
        state = State.LEFT_SET;
    }

    public void setRight(String source) {
        requireState("setRight", State.LEFT_SET);
        right = Parser.parse(source);
        state = State.BOTH_SET;
    }

    public SubtypeDecision checkSubtype() {
        requireState("checkSubtype", State.BOTH_SET);
        state = State.DONE;
        return SubtypingChecker.isSubtype(left, right)
                ? SubtypeDecision.SUBTYPE : SubtypeDecision.NOT_SUBTYPE;
    }

    private void requireState(String method, State expected) {
        if (state != expected)
            throw new ProtocolViolationException(method, state.name());
    }
}
