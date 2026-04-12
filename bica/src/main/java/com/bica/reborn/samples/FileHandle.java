package com.bica.reborn.samples;

import com.bica.reborn.annotation.Session;
import com.bica.reborn.exception.ProtocolViolationException;

/**
 * Sample implementation conforming to the session type:
 * {@code open . &{read: close . end, write: close . end}}
 *
 * <p>State machine:
 * <pre>
 *   INITIAL --open--> OPENED --read---> PENDING_CLOSE --close--> CLOSED
 *                             --write-->
 * </pre>
 */
@Session("open . &{read: close . end, write: close . end}")
public class FileHandle {

    private enum State { INITIAL, OPENED, PENDING_CLOSE, CLOSED }

    private State state = State.INITIAL;

    public void open() {
        requireState("open", State.INITIAL);
        state = State.OPENED;
    }

    public void read() {
        requireState("read", State.OPENED);
        state = State.PENDING_CLOSE;
    }

    public void write() {
        requireState("write", State.OPENED);
        state = State.PENDING_CLOSE;
    }

    public void close() {
        requireState("close", State.PENDING_CLOSE);
        state = State.CLOSED;
    }

    private void requireState(String method, State expected) {
        if (state != expected) {
            throw new ProtocolViolationException(method, state.name());
        }
    }
}
