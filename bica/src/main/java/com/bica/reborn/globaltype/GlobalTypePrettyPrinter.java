package com.bica.reborn.globaltype;

import java.util.stream.Collectors;

/**
 * Pretty-printer for multiparty global type ASTs.
 */
public final class GlobalTypePrettyPrinter {

    private GlobalTypePrettyPrinter() {}

    /**
     * Pretty-print a global type to a human-readable string.
     */
    public static String pretty(GlobalType g) {
        return switch (g) {
            case GEnd e -> "end";
            case GVar v -> v.name();
            case GMessage m -> {
                String choices = m.choices().stream()
                        .map(c -> c.label() + ": " + pretty(c.body()))
                        .collect(Collectors.joining(", "));
                yield m.sender() + " -> " + m.receiver()
                        + " : {" + choices + "}";
            }
            case GParallel p ->
                    "(" + pretty(p.left()) + " || " + pretty(p.right()) + ")";
            case GRec r ->
                    "rec " + r.var() + " . " + pretty(r.body());
        };
    }
}
