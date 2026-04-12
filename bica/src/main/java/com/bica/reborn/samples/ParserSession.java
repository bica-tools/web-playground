package com.bica.reborn.samples;

import com.bica.reborn.annotation.Session;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.exception.ProtocolViolationException;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.parser.PrettyPrinter;

/**
 * Session-typed parser protocol.
 *
 * <p>Session type:
 * {@code setInput . parse . +{OK: &{prettyPrint: end, getAST: end}, ERROR: getError . end}}
 *
 * <p>Protocol: set input string, parse it, then the result is either OK (from which
 * you can pretty-print or get the AST) or ERROR (from which you get the error message).
 */
@Session("setInput . parse . +{OK: &{prettyPrint: end, getAST: end}, ERROR: getError . end}")
public class ParserSession {

    public enum ParseResult { OK, ERROR }

    private enum State { INITIAL, INPUT_SET, PARSED_OK, PARSED_ERROR, DONE }
    private State state = State.INITIAL;

    private String input;
    private SessionType ast;
    private String errorMessage;

    public void setInput(String source) {
        requireState("setInput", State.INITIAL);
        input = source;
        state = State.INPUT_SET;
    }

    public ParseResult parse() {
        requireState("parse", State.INPUT_SET);
        try {
            ast = Parser.parse(input);
            state = State.PARSED_OK;
            return ParseResult.OK;
        } catch (Exception e) {
            errorMessage = e.getMessage();
            state = State.PARSED_ERROR;
            return ParseResult.ERROR;
        }
    }

    public String prettyPrint() {
        requireState("prettyPrint", State.PARSED_OK);
        state = State.DONE;
        return PrettyPrinter.pretty(ast);
    }

    public SessionType getAST() {
        requireState("getAST", State.PARSED_OK);
        state = State.DONE;
        return ast;
    }

    public String getError() {
        requireState("getError", State.PARSED_ERROR);
        state = State.DONE;
        return errorMessage;
    }

    private void requireState(String method, State expected) {
        if (state != expected)
            throw new ProtocolViolationException(method, state.name());
    }
}
