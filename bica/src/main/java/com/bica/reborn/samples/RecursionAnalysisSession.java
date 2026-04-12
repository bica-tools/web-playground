package com.bica.reborn.samples;

import com.bica.reborn.annotation.Session;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.exception.ProtocolViolationException;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.recursion.RecursionChecker;

/**
 * Session-typed recursion analysis protocol.
 *
 * <p>Session type:
 * {@code setType . checkGuardedness . +{GUARDED: checkContractivity . +{CONTRACTIVE: analyze . end, NOT_CONTRACTIVE: end}, NOT_GUARDED: end}}
 *
 * <p>Protocol: set a type, check guardedness (mandatory), then if guarded check
 * contractivity, then if contractive perform full analysis. Non-guarded types
 * cannot proceed to further analysis.
 */
@Session("setType . checkGuardedness . +{GUARDED: checkContractivity . +{CONTRACTIVE: analyze . end, NOT_CONTRACTIVE: end}, NOT_GUARDED: end}")
public class RecursionAnalysisSession {

    public enum GuardednessResult { GUARDED, NOT_GUARDED }
    public enum ContractivityResult { CONTRACTIVE, NOT_CONTRACTIVE }

    private enum State { INITIAL, TYPE_SET, GUARDED, NOT_GUARDED, CONTRACTIVE, DONE }
    private State state = State.INITIAL;

    private SessionType type;
    private String analysisReport;

    public void setType(String source) {
        requireState("setType", State.INITIAL);
        type = Parser.parse(source);
        state = State.TYPE_SET;
    }

    public GuardednessResult checkGuardedness() {
        requireState("checkGuardedness", State.TYPE_SET);
        var result = RecursionChecker.isGuarded(type);
        if (result) {
            state = State.GUARDED;
            return GuardednessResult.GUARDED;
        } else {
            state = State.NOT_GUARDED;
            return GuardednessResult.NOT_GUARDED;
        }
    }

    public ContractivityResult checkContractivity() {
        requireState("checkContractivity", State.GUARDED);
        var result = RecursionChecker.isContractive(type);
        if (result) {
            state = State.CONTRACTIVE;
            return ContractivityResult.CONTRACTIVE;
        } else {
            state = State.DONE;
            return ContractivityResult.NOT_CONTRACTIVE;
        }
    }

    public String analyze() {
        requireState("analyze", State.CONTRACTIVE);
        state = State.DONE;
        var analysis = RecursionChecker.analyzeRecursion(type);
        return "guarded=" + analysis.isGuarded()
                + " contractive=" + analysis.isContractive()
                + " tail=" + analysis.isTailRecursive()
                + " sccs=" + analysis.sccCount();
    }

    private void requireState(String method, State expected) {
        if (state != expected)
            throw new ProtocolViolationException(method, state.name());
    }
}
