package com.bica.reborn.parser;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;

import java.util.stream.Collectors;

/**
 * Renders a session type AST back to a human-readable string.
 *
 * <p>Under T2b (2026-04-11), {@code m . S} is rendered from {@link Chain}
 * nodes, not from single-arm {@link Branch} nodes. Branches always render
 * with the explicit {@code &{...}} notation, regardless of arity.
 */
public final class PrettyPrinter {

    private PrettyPrinter() {} // utility class

    /** Render an AST node to its string representation. */
    public static String pretty(SessionType node) {
        return switch (node) {
            case End e -> "end";
            case Var v -> v.name();
            case Branch b -> {
                String inner = b.choices().stream()
                        .map(c -> c.label() + ": " + pretty(c.body()))
                        .collect(Collectors.joining(", "));
                yield "&{" + inner + "}";
            }
            case Select s -> {
                String inner = s.choices().stream()
                        .map(c -> c.label() + ": " + pretty(c.body()))
                        .collect(Collectors.joining(", "));
                yield "+{" + inner + "}";
            }
            case Parallel p -> "(" + pretty(p.left()) + " || " + pretty(p.right()) + ")";
            case Rec r -> "rec " + r.var() + " . " + pretty(r.body());
            case Chain ch -> ch.method() + " . " + pretty(ch.cont());
            case Sequence seq -> pretty(seq.left()) + " . " + pretty(seq.right());
        };
    }

    /** Render in core grammar notation — no sequencing sugar. */
    public static String prettyCore(SessionType node) {
        return switch (node) {
            case End e -> "end";
            case Var v -> v.name();
            case Branch b -> {
                String inner = b.choices().stream()
                        .map(c -> c.label() + ": " + prettyCore(c.body()))
                        .collect(Collectors.joining(", "));
                yield "&{" + inner + "}";
            }
            case Select s -> {
                String inner = s.choices().stream()
                        .map(c -> c.label() + ": " + prettyCore(c.body()))
                        .collect(Collectors.joining(", "));
                yield "+{" + inner + "}";
            }
            case Parallel p -> "(" + prettyCore(p.left()) + " || " + prettyCore(p.right()) + ")";
            case Rec r -> "rec " + r.var() + " . " + prettyCore(r.body());
            case Chain ch -> ch.method() + " . " + prettyCore(ch.cont());
            case Sequence seq -> prettyCore(seq.left()) + " . " + prettyCore(seq.right());
        };
    }
}
