package com.bica.reborn.samples;

import com.bica.reborn.annotation.Session;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.duality.DualityChecker;
import com.bica.reborn.duality.DualityResult;
import com.bica.reborn.exception.ProtocolViolationException;
import com.bica.reborn.parser.Parser;

/**
 * Session-typed duality checker protocol.
 *
 * <p>Session type:
 * {@code setType . computeDual . &{checkInvolution: +{INVOLUTION: end, NOT_INVOLUTION: end}, getDual: end}}
 *
 * <p>Protocol: set a session type, compute its dual, then either check the
 * involution property (dual of dual = original) or just get the dual.
 */
@Session("setType . computeDual . &{checkInvolution: +{INVOLUTION: end, NOT_INVOLUTION: end}, getDual: end}")
public class DualitySession {

    public enum InvolutionResult { INVOLUTION, NOT_INVOLUTION }

    private enum State { INITIAL, TYPE_SET, DUAL_COMPUTED, DONE }
    private State state = State.INITIAL;

    private SessionType original;
    private SessionType dual;

    public void setType(String source) {
        requireState("setType", State.INITIAL);
        original = Parser.parse(source);
        state = State.TYPE_SET;
    }

    public void computeDual() {
        requireState("computeDual", State.TYPE_SET);
        dual = DualityChecker.dual(original);
        state = State.DUAL_COMPUTED;
    }

    public InvolutionResult checkInvolution() {
        requireState("checkInvolution", State.DUAL_COMPUTED);
        state = State.DONE;
        DualityResult result = DualityChecker.check(original);
        return result.isInvolution() ? InvolutionResult.INVOLUTION : InvolutionResult.NOT_INVOLUTION;
    }

    public SessionType getDual() {
        requireState("getDual", State.DUAL_COMPUTED);
        state = State.DONE;
        return dual;
    }

    private void requireState(String method, State expected) {
        if (state != expected)
            throw new ProtocolViolationException(method, state.name());
    }
}
