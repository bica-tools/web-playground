package com.bica.reborn.parser;

import java.util.Objects;

/** A lexical token with its kind, text value, and source position. */
public record Token(TokenKind kind, String value, int pos) {

    public Token {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(value, "value must not be null");
    }
}
