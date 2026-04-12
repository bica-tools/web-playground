package com.bica.reborn.ast;

/**
 * Desugaring and ensugaring of session type ASTs.
 *
 * <p>Under the T2b grammar lock-in (2026-04-11), {@code m . S} is a
 * first-class primitive represented by {@link Chain}. It is no longer
 * syntactic sugar for a single-arm {@link Branch}: {@code Chain} and
 * {@code Branch} with a single {@link Branch.Choice} are deliberately
 * distinct syntactic categories, even though they are lattice-isomorphic
 * at the state-space level (see
 * {@code lean4-formalization/Reticulate/ChainAndSeq.lean}).
 *
 * <p>{@link #desugar(SessionType)} normalizes a legacy
 * {@code Sequence(Var(m), S)} to {@code Chain(m, desugar(S))}. All other
 * nodes — including {@link Chain} itself — recurse structurally.
 *
 * <p>{@link #ensugar(SessionType)} is now identity on {@link Chain} and
 * on single-arm {@link Branch}: it must not collapse the two forms, since
 * preserving their distinction is the whole point of T2b.
 */
public final class Sugar {

    private Sugar() {} // utility class

    /**
     * Normalize an AST to the T2b core grammar.
     *
     * <p>Rewrites {@code Sequence(Var(m), S)} to {@code Chain(m, desugar(S))}.
     * {@link Chain} nodes are preserved and recursed into.
     */
    public static SessionType desugar(SessionType node) {
        return switch (node) {
            case End e -> e;
            case Var v -> v;
            case Branch b -> new Branch(
                b.choices().stream()
                    .map(c -> new Branch.Choice(c.label(), desugar(c.body())))
                    .toList()
            );
            case Select s -> new Select(
                s.choices().stream()
                    .map(c -> new Branch.Choice(c.label(), desugar(c.body())))
                    .toList()
            );
            case Parallel p -> new Parallel(desugar(p.left()), desugar(p.right()));
            case Rec r -> new Rec(r.var(), desugar(r.body()));
            case Chain ch -> new Chain(ch.method(), desugar(ch.cont()));
            case Sequence seq -> {
                if (seq.left() instanceof Var v) {
                    // m . S  →  Chain(m, S)   [T2b, 2026-04-11]
                    yield new Chain(v.name(), desugar(seq.right()));
                }
                yield new Sequence(desugar(seq.left()), desugar(seq.right()));
            }
        };
    }

    /**
     * Structural recursion through an AST.
     *
     * <p>Under T2b this is identity on {@link Chain} and on single-arm
     * {@link Branch}: it must not collapse the two forms. Legacy
     * {@link Sequence} nodes with a {@link Var} left-hand side are
     * rewritten to {@link Chain} for consistency with {@link #desugar}.
     */
    public static SessionType ensugar(SessionType node) {
        return switch (node) {
            case End e -> e;
            case Var v -> v;
            case Branch b -> new Branch(
                b.choices().stream()
                    .map(c -> new Branch.Choice(c.label(), ensugar(c.body())))
                    .toList()
            );
            case Select s -> new Select(
                s.choices().stream()
                    .map(c -> new Branch.Choice(c.label(), ensugar(c.body())))
                    .toList()
            );
            case Parallel p -> new Parallel(ensugar(p.left()), ensugar(p.right()));
            case Rec r -> new Rec(r.var(), ensugar(r.body()));
            case Chain ch -> new Chain(ch.method(), ensugar(ch.cont()));
            case Sequence seq -> {
                if (seq.left() instanceof Var v) {
                    // m . S  →  Chain(m, S)   [T2b, 2026-04-11]
                    yield new Chain(v.name(), ensugar(seq.right()));
                }
                yield new Sequence(ensugar(seq.left()), ensugar(seq.right()));
            }
        };
    }
}
