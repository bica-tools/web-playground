package com.bica.reborn.samples;

import com.bica.reborn.annotation.Session;
import com.bica.reborn.exception.ProtocolViolationException;

/**
 * Sample implementation conforming to the session type:
 * {@code insertCard . enterPIN . +{AUTH: rec X . &{checkBalance: X, withdraw: +{OK: X, INSUFFICIENT: X}, deposit: X, ejectCard: end}, REJECTED: ejectCard . end}}
 *
 * <p>Uses the enum-return discipline for selections:
 * <ul>
 *   <li>{@code enterPIN()} returns {@link PinResult} — the ATM decides AUTH or REJECTED</li>
 *   <li>{@code withdraw()} returns {@link WithdrawResult} — the ATM decides OK or INSUFFICIENT</li>
 * </ul>
 *
 * <p>The client branches on return values, never calling selection labels directly.
 */
@Session("insertCard . enterPIN . +{AUTH: rec X . &{checkBalance: X, withdraw: +{OK: X, INSUFFICIENT: X}, deposit: X, ejectCard: end}, REJECTED: ejectCard . end}")
public class ATM {

    public enum PinResult { AUTH, REJECTED }
    public enum WithdrawResult { OK, INSUFFICIENT }

    private enum State { INITIAL, CARD_INSERTED, AUTHENTICATED, REJECTED_STATE, EJECTED }
    private State state = State.INITIAL;

    private boolean pinValid = true;
    private double balance = 1000.00;

    public void insertCard() {
        requireState("insertCard", State.INITIAL);
        state = State.CARD_INSERTED;
    }

    public PinResult enterPIN() {
        requireState("enterPIN", State.CARD_INSERTED);
        if (pinValid) {
            state = State.AUTHENTICATED;
            return PinResult.AUTH;
        } else {
            state = State.REJECTED_STATE;
            return PinResult.REJECTED;
        }
    }

    /** Configure what the next enterPIN() call will return (for testing). */
    public void setPinValid(boolean valid) {
        this.pinValid = valid;
    }

    public double checkBalance() {
        requireState("checkBalance", State.AUTHENTICATED);
        return balance;
    }

    public WithdrawResult withdraw() {
        requireState("withdraw", State.AUTHENTICATED);
        if (balance > 0) {
            balance -= 100;
            return WithdrawResult.OK;
        } else {
            return WithdrawResult.INSUFFICIENT;
        }
    }

    public void deposit() {
        requireState("deposit", State.AUTHENTICATED);
        balance += 100;
    }

    public void ejectCard() {
        if (state != State.AUTHENTICATED && state != State.REJECTED_STATE)
            throw new ProtocolViolationException("ejectCard", state.name());
        state = State.EJECTED;
    }

    private void requireState(String method, State expected) {
        if (state != expected)
            throw new ProtocolViolationException(method, state.name());
    }
}
