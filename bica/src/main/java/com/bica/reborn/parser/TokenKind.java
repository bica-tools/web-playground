package com.bica.reborn.parser;

/** Token kinds for the session type lexer. */
public enum TokenKind {
    LBRACE,    // {
    RBRACE,    // }
    LPAREN,    // (
    RPAREN,    // )
    AMPERSAND, // &
    PLUS,      // + or ⊕
    COLON,     // :
    COMMA,     // ,
    DOT,       // .
    PAR,       // || or ∥
    IDENT,     // identifier or keyword (end, rec)
    EOF
}
