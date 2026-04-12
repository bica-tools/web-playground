package com.bica.reborn.parser;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser for session type strings.
 *
 * <p>Grammar:
 * <pre>
 * S  ::=  &amp;{ m₁ : S₁ , … , mₙ : Sₙ }    -- branch
 *      |  +{ l₁ : S₁ , … , lₙ : Sₙ }    -- selection
 *      |  ( S₁ || S₂ )                    -- parallel (paren notation)
 *      |  rec X . S                        -- recursion
 *      |  X                                -- variable
 *      |  end                              -- terminated
 *      |  S₁ . S₂                          -- sequencing
 * </pre>
 *
 * <p>T2b grammar rule (2026-04-11): {@code ident . S} where {@code ident}
 * is a bare identifier becomes {@code Chain(ident, S)} — a labelled
 * one-step primitive. Non-identifier left operands produce {@link Sequence}.
 */
public final class Parser {

    private final List<Token> tokens;
    private int pos;

    private Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    /**
     * Parse a session-type string into an AST.
     *
     * @throws ParseError on invalid syntax
     */
    public static SessionType parse(String source) {
        List<Token> tokens = Tokenizer.tokenize(source);
        return new Parser(tokens).parseTop();
    }

    // -- helpers ---------------------------------------------------------------

    private Token peek() {
        return tokens.get(pos);
    }

    private Token advance() {
        Token tok = tokens.get(pos);
        pos++;
        return tok;
    }

    private Token expect(TokenKind kind, String context) {
        Token tok = peek();
        if (tok.kind() != kind) {
            String ctx = context.isEmpty() ? "" : " (" + context + ")";
            throw new ParseError(
                    "expected " + kind.name() + ", got " + tok.kind().name()
                            + " ('" + tok.value() + "')" + ctx,
                    tok.pos());
        }
        return advance();
    }

    // -- grammar rules ---------------------------------------------------------

    private SessionType parseTop() {
        SessionType result = seqExpr();
        if (peek().kind() != TokenKind.EOF) {
            Token tok = peek();
            throw new ParseError(
                    "unexpected token " + tok.kind().name()
                            + " ('" + tok.value() + "') after expression",
                    tok.pos());
        }
        return result;
    }

    /**
     * Parse a sequence expression (right-associative {@code .} operator).
     *
     * <p>Under T2b (2026-04-11): if the left operand is a bare identifier
     * {@code m}, we produce a {@link Chain} node {@code Chain(m, S)} — a
     * first-class labelled one-step primitive, no longer syntactic sugar
     * for a single-arm {@link Branch}. Otherwise we produce a
     * {@link Sequence} node for general sequential composition.
     */
    private SessionType seqExpr() {
        SessionType left = atom();

        if (peek().kind() == TokenKind.DOT) {
            advance(); // consume '.'
            SessionType right = seqExpr(); // right-associative

            // T2b (2026-04-11): bare identifier becomes Chain, NOT single-arm Branch.
            if (left instanceof Var v) {
                return new Chain(v.name(), right);
            }
            return new Sequence(left, right);
        }

        return left;
    }

    /** Parse a self-delimiting construct. */
    private SessionType atom() {
        Token tok = peek();

        // &{ ... }
        if (tok.kind() == TokenKind.AMPERSAND) {
            return parseBranch();
        }

        // +{ ... } or ⊕{ ... }
        if (tok.kind() == TokenKind.PLUS) {
            return parseSelect();
        }

        // ( ... ) — either parenthesized expression or parallel
        if (tok.kind() == TokenKind.LPAREN) {
            return parenOrParallel();
        }

        // rec X . S
        if (tok.kind() == TokenKind.IDENT && tok.value().equals("rec")) {
            return parseRec();
        }

        // end
        if (tok.kind() == TokenKind.IDENT && tok.value().equals("end")) {
            advance();
            return new End();
        }

        // plain identifier (type variable or method name)
        if (tok.kind() == TokenKind.IDENT) {
            advance();
            return new Var(tok.value());
        }

        throw new ParseError(
                "unexpected token " + tok.kind().name() + " ('" + tok.value() + "')",
                tok.pos());
    }

    /** Parse {@code m₁ : S₁ , … , mₙ : Sₙ} inside braces. */
    private List<Choice> choiceList(String context) {
        var entries = new ArrayList<Choice>();
        while (true) {
            Token labelTok = expect(TokenKind.IDENT, context + " label");
            expect(TokenKind.COLON, context + " colon after '" + labelTok.value() + "'");
            SessionType body = seqExpr();
            entries.add(new Choice(labelTok.value(), body));
            if (peek().kind() == TokenKind.COMMA) {
                advance();
            } else {
                break;
            }
        }
        return entries;
    }

    private Branch parseBranch() {
        advance(); // consume '&'
        expect(TokenKind.LBRACE, "branch");
        if (peek().kind() == TokenKind.RBRACE) {
            throw new ParseError("branch must have at least one choice", peek().pos());
        }
        List<Choice> choices = choiceList("branch");
        expect(TokenKind.RBRACE, "branch closing");
        return new Branch(choices);
    }

    private Select parseSelect() {
        advance(); // consume '+'
        expect(TokenKind.LBRACE, "select");
        if (peek().kind() == TokenKind.RBRACE) {
            throw new ParseError("select must have at least one choice", peek().pos());
        }
        List<Choice> choices = choiceList("select");
        expect(TokenKind.RBRACE, "select closing");
        return new Select(choices);
    }

    /** Parse {@code ( S₁ || S₂ )} or {@code ( S )}. */
    private SessionType parenOrParallel() {
        advance(); // consume '('
        SessionType left = seqExpr();

        if (peek().kind() == TokenKind.PAR) {
            advance(); // consume '||'
            SessionType right = seqExpr();
            expect(TokenKind.RPAREN, "parallel closing");
            return new Parallel(left, right);
        }

        // Plain grouping
        expect(TokenKind.RPAREN, "parenthesized expression closing");
        return left;
    }

    private Rec parseRec() {
        advance(); // consume 'rec'
        Token varTok = expect(TokenKind.IDENT, "recursion variable");
        if (varTok.value().equals("rec") || varTok.value().equals("end")) {
            throw new ParseError(
                    "'" + varTok.value() + "' is a keyword, not a valid variable name",
                    varTok.pos());
        }
        expect(TokenKind.DOT, "recursion dot");
        SessionType body = seqExpr();
        return new Rec(varTok.value(), body);
    }
}
