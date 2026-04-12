package com.bica.reborn.samples;

import com.bica.reborn.annotation.Session;
import com.bica.reborn.exception.ProtocolViolationException;

/**
 * Sample implementation conforming to the session type:
 * {@code rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}}
 *
 * <p>Uses the enum-return discipline for selections:
 * <ul>
 *   <li>{@code hasNext()} returns {@link HasNextResult} — the iterator decides TRUE or FALSE</li>
 * </ul>
 *
 * <p>The client branches on the return value, never calling selection labels directly.
 */
@Session("rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}")
public class SimpleIterator {

    public enum HasNextResult { TRUE, FALSE }

    private enum State { READY, AWAITING_NEXT, DONE }

    private State state = State.READY;
    private int remaining = 3;

    public HasNextResult hasNext() {
        requireState("hasNext", State.READY);
        if (remaining > 0) {
            state = State.AWAITING_NEXT;
            return HasNextResult.TRUE;
        } else {
            state = State.DONE;
            return HasNextResult.FALSE;
        }
    }

    /** Configure how many TRUE results remain (for testing). */
    public void setRemaining(int n) {
        this.remaining = n;
    }

    public void next() {
        requireState("next", State.AWAITING_NEXT);
        remaining--;
        state = State.READY;
    }

    private void requireState(String method, State expected) {
        if (state != expected) {
            throw new ProtocolViolationException(method, state.name());
        }
    }
}
