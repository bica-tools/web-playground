package com.bica.reborn.parser;

/** Raised on invalid session-type syntax, with character position. */
public class ParseError extends RuntimeException {

    private final Integer pos;

    public ParseError(String message, Integer pos) {
        super(pos != null ? "at position " + pos + ": " + message : message);
        this.pos = pos;
    }

    public ParseError(String message) {
        this(message, null);
    }

    /** Returns the character position of the error, or {@code null} if unknown. */
    public Integer getPos() {
        return pos;
    }
}
