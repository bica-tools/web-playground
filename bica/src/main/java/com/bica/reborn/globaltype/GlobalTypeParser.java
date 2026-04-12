package com.bica.reborn.globaltype;

import com.bica.reborn.parser.ParseError;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizer and recursive-descent parser for multiparty global types.
 *
 * <p>Grammar:
 * <pre>
 *   G  ::=  atom ( '||' atom )*
 *   atom ::=  'end'
 *          |  'rec' IDENT '.' G
 *          |  '(' G ')'
 *          |  IDENT '->' IDENT ':' '{' choice (',' choice)* '}'
 *          |  IDENT             (variable)
 *   choice ::=  IDENT ':' G
 * </pre>
 */
public final class GlobalTypeParser {

    // -----------------------------------------------------------------------
    // Token kinds
    // -----------------------------------------------------------------------

    private enum TK {
        ARROW, COLON, COMMA, LBRACE, RBRACE, LPAREN, RPAREN,
        PAR, DOT, IDENT, END, REC, EOF
    }

    private record Token(TK kind, String value, int pos) {}

    // -----------------------------------------------------------------------
    // Tokenizer
    // -----------------------------------------------------------------------

    private static List<Token> tokenize(String source) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            switch (c) {
                case '-' -> {
                    if (i + 1 < source.length() && source.charAt(i + 1) == '>') {
                        tokens.add(new Token(TK.ARROW, "->", i));
                        i += 2;
                    } else {
                        throw new ParseError("unexpected '-' at position " + i);
                    }
                }
                case '|' -> {
                    if (i + 1 < source.length() && source.charAt(i + 1) == '|') {
                        tokens.add(new Token(TK.PAR, "||", i));
                        i += 2;
                    } else {
                        throw new ParseError("unexpected '|' at position " + i);
                    }
                }
                case ':' -> { tokens.add(new Token(TK.COLON, ":", i)); i++; }
                case ',' -> { tokens.add(new Token(TK.COMMA, ",", i)); i++; }
                case '{' -> { tokens.add(new Token(TK.LBRACE, "{", i)); i++; }
                case '}' -> { tokens.add(new Token(TK.RBRACE, "}", i)); i++; }
                case '(' -> { tokens.add(new Token(TK.LPAREN, "(", i)); i++; }
                case ')' -> { tokens.add(new Token(TK.RPAREN, ")", i)); i++; }
                case '.' -> { tokens.add(new Token(TK.DOT, ".", i)); i++; }
                case '\u2225' -> { // ∥
                    tokens.add(new Token(TK.PAR, "||", i));
                    i++;
                }
                case '\u03BC' -> { // μ
                    tokens.add(new Token(TK.REC, "rec", i));
                    i++;
                }
                default -> {
                    if (Character.isLetter(c) || c == '_') {
                        int start = i;
                        while (i < source.length()
                                && (Character.isLetterOrDigit(source.charAt(i))
                                    || source.charAt(i) == '_')) {
                            i++;
                        }
                        String word = source.substring(start, i);
                        TK kind = switch (word) {
                            case "end" -> TK.END;
                            case "rec" -> TK.REC;
                            default -> TK.IDENT;
                        };
                        tokens.add(new Token(kind, word, start));
                    } else {
                        throw new ParseError(
                                "unexpected character '" + c + "' at position " + i);
                    }
                }
            }
        }
        tokens.add(new Token(TK.EOF, "", source.length()));
        return tokens;
    }

    // -----------------------------------------------------------------------
    // Parser state
    // -----------------------------------------------------------------------

    private final List<Token> tokens;
    private int pos;

    private GlobalTypeParser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token advance() {
        return tokens.get(pos++);
    }

    private Token expect(TK kind, String context) {
        Token tok = advance();
        if (tok.kind() != kind) {
            throw new ParseError(
                    "expected " + kind + " in " + context
                    + ", got " + tok.kind() + " ('" + tok.value()
                    + "') at position " + tok.pos());
        }
        return tok;
    }

    // -----------------------------------------------------------------------
    // Grammar rules
    // -----------------------------------------------------------------------

    private GlobalType parseGlobal() {
        GlobalType left = parseAtom();
        while (peek().kind() == TK.PAR) {
            advance();
            GlobalType right = parseAtom();
            left = new GParallel(left, right);
        }
        return left;
    }

    private GlobalType parseAtom() {
        Token tok = peek();
        return switch (tok.kind()) {
            case END -> { advance(); yield new GEnd(); }
            case REC -> {
                advance();
                Token varTok = expect(TK.IDENT, "rec");
                expect(TK.DOT, "rec");
                GlobalType body = parseGlobal();
                yield new GRec(varTok.value(), body);
            }
            case LPAREN -> {
                advance();
                GlobalType inner = parseGlobal();
                expect(TK.RPAREN, "parenthesized global type");
                yield inner;
            }
            case IDENT -> {
                // Lookahead: IDENT ARROW → message; lone IDENT → variable
                if (pos + 1 < tokens.size()
                        && tokens.get(pos + 1).kind() == TK.ARROW) {
                    yield parseMessage();
                } else {
                    advance();
                    yield new GVar(tok.value());
                }
            }
            default -> throw new ParseError(
                    "unexpected token " + tok.kind() + " ('" + tok.value()
                    + "') at position " + tok.pos());
        };
    }

    private GMessage parseMessage() {
        Token senderTok = expect(TK.IDENT, "message sender");
        expect(TK.ARROW, "message");
        Token receiverTok = expect(TK.IDENT, "message receiver");
        expect(TK.COLON, "message");
        expect(TK.LBRACE, "message choices");

        List<GMessage.Choice> choices = new ArrayList<>();
        if (peek().kind() != TK.RBRACE) {
            choices.add(parseChoice());
            while (peek().kind() == TK.COMMA) {
                advance();
                choices.add(parseChoice());
            }
        }

        expect(TK.RBRACE, "message choices");
        return new GMessage(senderTok.value(), receiverTok.value(), choices);
    }

    private GMessage.Choice parseChoice() {
        Token labelTok = expect(TK.IDENT, "choice label");
        expect(TK.COLON, "choice");
        GlobalType body = parseGlobal();
        return new GMessage.Choice(labelTok.value(), body);
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Parse a global type string into an AST.
     *
     * @param source the global type string
     * @return the parsed global type AST
     * @throws ParseError if the source cannot be parsed
     */
    public static GlobalType parse(String source) {
        List<Token> tokens = tokenize(source);
        GlobalTypeParser parser = new GlobalTypeParser(tokens);
        GlobalType result = parser.parseGlobal();
        parser.expect(TK.EOF, "end of input");
        return result;
    }
}
