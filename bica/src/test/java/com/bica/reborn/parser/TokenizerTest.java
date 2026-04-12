package com.bica.reborn.parser;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for the session type tokenizer. */
class TokenizerTest {

    @Test
    void emptyInput() {
        List<Token> tokens = Tokenizer.tokenize("");
        assertEquals(1, tokens.size());
        assertEquals(TokenKind.EOF, tokens.getFirst().kind());
    }

    @Test
    void singleCharTokens() {
        List<Token> tokens = Tokenizer.tokenize("{ } ( ) & + : , .");
        List<TokenKind> kinds = tokens.subList(0, tokens.size() - 1).stream()
                .map(Token::kind).toList();
        assertEquals(List.of(
                TokenKind.LBRACE, TokenKind.RBRACE,
                TokenKind.LPAREN, TokenKind.RPAREN,
                TokenKind.AMPERSAND, TokenKind.PLUS,
                TokenKind.COLON, TokenKind.COMMA,
                TokenKind.DOT), kinds);
    }

    @Test
    void parallelOperator() {
        List<Token> tokens = Tokenizer.tokenize("||");
        assertEquals(TokenKind.PAR, tokens.getFirst().kind());
        assertEquals("||", tokens.getFirst().value());
    }

    @Test
    void identifiers() {
        List<Token> tokens = Tokenizer.tokenize("end rec X foo_bar baz123");
        List<String> idents = tokens.stream()
                .filter(t -> t.kind() == TokenKind.IDENT)
                .map(Token::value).toList();
        assertEquals(List.of("end", "rec", "X", "foo_bar", "baz123"), idents);
    }

    @Test
    void positions() {
        List<Token> tokens = Tokenizer.tokenize("a . b");
        assertEquals(0, tokens.get(0).pos()); // a
        assertEquals(2, tokens.get(1).pos()); // .
        assertEquals(4, tokens.get(2).pos()); // b
    }

    @Test
    void unicodeOplus() {
        List<Token> tokens = Tokenizer.tokenize("\u2295{");
        assertEquals(TokenKind.PLUS, tokens.getFirst().kind());
    }

    @Test
    void unicodeParallel() {
        List<Token> tokens = Tokenizer.tokenize("\u2225");
        assertEquals(TokenKind.PAR, tokens.getFirst().kind());
    }

    @Test
    void unicodeMu() {
        List<Token> tokens = Tokenizer.tokenize("\u03BCX");
        assertEquals(TokenKind.IDENT, tokens.getFirst().kind());
        assertEquals("rec", tokens.getFirst().value());
    }

    @Test
    void unexpectedCharacter() {
        ParseError err = assertThrows(ParseError.class, () -> Tokenizer.tokenize("@"));
        assertTrue(err.getMessage().contains("unexpected character"));
    }
}
