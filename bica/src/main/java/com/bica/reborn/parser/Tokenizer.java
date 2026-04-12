package com.bica.reborn.parser;

import java.util.ArrayList;
import java.util.List;

/** Scans a session type source string into a list of {@link Token}s. */
public final class Tokenizer {

    private Tokenizer() {} // utility class

    /**
     * Tokenize {@code source} into a list of tokens (including a trailing {@link TokenKind#EOF}).
     *
     * @throws ParseError on unexpected characters
     */
    public static List<Token> tokenize(String source) {
        var tokens = new ArrayList<Token>();
        int i = 0;
        int n = source.length();

        while (i < n) {
            char ch = source.charAt(i);

            // skip whitespace
            if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
                i++;
                continue;
            }

            // two-character token: ||
            if (ch == '|' && i + 1 < n && source.charAt(i + 1) == '|') {
                tokens.add(new Token(TokenKind.PAR, "||", i));
                i += 2;
                continue;
            }

            // single-character tokens
            TokenKind singleKind = singleCharKind(ch);
            if (singleKind != null) {
                tokens.add(new Token(singleKind, String.valueOf(ch), i));
                i++;
                continue;
            }

            // Unicode alternatives (checked before identifier scan because
            // μ, ⊕, ∥ are all "letters" in Java's Character.isLetter)
            if (ch == '\u2295') { // ⊕
                tokens.add(new Token(TokenKind.PLUS, "\u2295", i));
                i++;
                continue;
            }
            if (ch == '\u2225') { // ∥
                tokens.add(new Token(TokenKind.PAR, "\u2225", i));
                i++;
                continue;
            }
            if (ch == '\u03BC') { // μ
                tokens.add(new Token(TokenKind.IDENT, "rec", i));
                i++;
                continue;
            }

            // identifiers: [A-Za-z_][A-Za-z0-9_]*
            if (Character.isLetter(ch) || ch == '_') {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(source.charAt(i)) || source.charAt(i) == '_')) {
                    i++;
                }
                String word = source.substring(start, i);
                tokens.add(new Token(TokenKind.IDENT, word, start));
                continue;
            }

            throw new ParseError("unexpected character '" + ch + "'", i);
        }

        tokens.add(new Token(TokenKind.EOF, "", n));
        return List.copyOf(tokens);
    }

    private static TokenKind singleCharKind(char ch) {
        return switch (ch) {
            case '{' -> TokenKind.LBRACE;
            case '}' -> TokenKind.RBRACE;
            case '(' -> TokenKind.LPAREN;
            case ')' -> TokenKind.RPAREN;
            case '&' -> TokenKind.AMPERSAND;
            case '+' -> TokenKind.PLUS;
            case ':' -> TokenKind.COLON;
            case ',' -> TokenKind.COMMA;
            case '.' -> TokenKind.DOT;
            default -> null;
        };
    }
}
