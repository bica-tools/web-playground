package com.bica.reborn.extensions;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Language hierarchy classification for session types (L0 through L9).
 *
 * <p>Each level adds a constructor or structural pattern. This checker
 * inspects the AST to determine the minimum language level needed to
 * express a given session type.
 */
public final class LanguageHierarchyChecker {

    private LanguageHierarchyChecker() {}

    /**
     * Determine the minimum language level needed to express the given type.
     *
     * @param type a session type AST
     * @return the minimum language level
     */
    public static LanguageLevel classifyLevel(SessionType type) {
        LanguageLevel level = LanguageLevel.L0;

        if (hasBranch(type)) level = max(level, LanguageLevel.L1);
        if (hasSelection(type)) level = max(level, LanguageLevel.L2);
        if (hasParallel(type)) level = max(level, LanguageLevel.L3);
        if (hasRecursion(type)) level = max(level, LanguageLevel.L4);
        if (hasSequence(type)) level = max(level, LanguageLevel.L5);
        if (hasNestedParallel(type)) level = max(level, LanguageLevel.L6);
        if (hasRecContainingParallel(type)) level = max(level, LanguageLevel.L7);
        if (hasCrossRecReference(type)) level = max(level, LanguageLevel.L8);

        return level;
    }

    /**
     * Return a session type string that is expressible at {@code upper}
     * but not at {@code lower}.
     *
     * @param lower the lower language level
     * @param upper the upper language level
     * @return a separation witness type string
     * @throws IllegalArgumentException if lower >= upper or L9 is requested
     */
    public static String separationWitness(LanguageLevel lower, LanguageLevel upper) {
        if (lower.compareTo(upper) >= 0) {
            throw new IllegalArgumentException(
                    "Cannot construct separation witness: " + lower + " >= " + upper);
        }
        return witnessForLevel(upper);
    }

    /**
     * Return a human-readable description of the given level.
     */
    public static String levelDescription(LanguageLevel level) {
        return level.description();
    }

    // -----------------------------------------------------------------------
    // AST predicates
    // -----------------------------------------------------------------------

    /** Check if the type uses the Branch constructor. */
    public static boolean hasBranch(SessionType type) {
        return containsConstructor(type, Branch.class);
    }

    /** Check if the type uses the Select constructor. */
    public static boolean hasSelection(SessionType type) {
        return containsConstructor(type, Select.class);
    }

    /** Check if the type uses the Parallel constructor. */
    public static boolean hasParallel(SessionType type) {
        return containsConstructor(type, Parallel.class);
    }

    /** Check if the type uses Rec or Var constructors. */
    public static boolean hasRecursion(SessionType type) {
        return containsConstructor(type, Rec.class) || containsConstructor(type, Var.class);
    }

    /** Check if the type uses the Sequence constructor. */
    public static boolean hasSequence(SessionType type) {
        return containsConstructor(type, Sequence.class);
    }

    // -----------------------------------------------------------------------
    // Structural checks for L6-L8
    // -----------------------------------------------------------------------

    /** Check for Parallel nested inside another Parallel. */
    static boolean hasNestedParallel(SessionType type) {
        return checkNestedParallel(type, false);
    }

    /** Check for Rec whose body (transitively) contains Parallel. */
    static boolean hasRecContainingParallel(SessionType type) {
        return checkRecParallel(type, false);
    }

    /** Check for mutual recursion: 2+ distinct rec binders with cross-references. */
    static boolean hasCrossRecReference(SessionType type) {
        List<String> recVars = new java.util.ArrayList<>();
        collectRecVars(type, recVars);
        if (new HashSet<>(recVars).size() < 2) return false;
        return checkCrossReference(type, new HashSet<>());
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static boolean containsConstructor(SessionType type, Class<?> clazz) {
        if (clazz.isInstance(type)) return true;

        if (type instanceof Branch b) {
            for (Choice c : b.choices()) {
                if (containsConstructor(c.body(), clazz)) return true;
            }
        } else if (type instanceof Select s) {
            for (Choice c : s.choices()) {
                if (containsConstructor(c.body(), clazz)) return true;
            }
        } else if (type instanceof Parallel p) {
            return containsConstructor(p.left(), clazz)
                    || containsConstructor(p.right(), clazz);
        } else if (type instanceof Rec r) {
            return containsConstructor(r.body(), clazz);
        } else if (type instanceof Sequence seq) {
            return containsConstructor(seq.left(), clazz)
                    || containsConstructor(seq.right(), clazz);
        }
        // End, Var: no children
        return false;
    }

    private static boolean checkNestedParallel(SessionType type, boolean insideParallel) {
        if (type instanceof Parallel p) {
            if (insideParallel) return true;
            return checkNestedParallel(p.left(), true)
                    || checkNestedParallel(p.right(), true);
        } else if (type instanceof Branch b) {
            for (Choice c : b.choices()) {
                if (checkNestedParallel(c.body(), insideParallel)) return true;
            }
        } else if (type instanceof Select s) {
            for (Choice c : s.choices()) {
                if (checkNestedParallel(c.body(), insideParallel)) return true;
            }
        } else if (type instanceof Rec r) {
            return checkNestedParallel(r.body(), insideParallel);
        } else if (type instanceof Sequence seq) {
            return checkNestedParallel(seq.left(), insideParallel)
                    || checkNestedParallel(seq.right(), insideParallel);
        }
        return false;
    }

    private static boolean checkRecParallel(SessionType type, boolean insideRec) {
        if (type instanceof Parallel) {
            return insideRec;
        } else if (type instanceof Branch b) {
            for (Choice c : b.choices()) {
                if (checkRecParallel(c.body(), insideRec)) return true;
            }
        } else if (type instanceof Select s) {
            for (Choice c : s.choices()) {
                if (checkRecParallel(c.body(), insideRec)) return true;
            }
        } else if (type instanceof Rec r) {
            return checkRecParallel(r.body(), true);
        } else if (type instanceof Sequence seq) {
            return checkRecParallel(seq.left(), insideRec)
                    || checkRecParallel(seq.right(), insideRec);
        }
        return false;
    }

    private static void collectRecVars(SessionType type, List<String> out) {
        if (type instanceof Rec r) {
            out.add(r.var());
            collectRecVars(r.body(), out);
        } else if (type instanceof Branch b) {
            for (Choice c : b.choices()) collectRecVars(c.body(), out);
        } else if (type instanceof Select s) {
            for (Choice c : s.choices()) collectRecVars(c.body(), out);
        } else if (type instanceof Parallel p) {
            collectRecVars(p.left(), out);
            collectRecVars(p.right(), out);
        } else if (type instanceof Sequence seq) {
            collectRecVars(seq.left(), out);
            collectRecVars(seq.right(), out);
        }
    }

    private static boolean checkCrossReference(SessionType type, Set<String> boundRecVars) {
        if (type instanceof Var v) {
            return boundRecVars.contains(v.name());
        } else if (type instanceof Rec r) {
            // Check if body references outer-bound vars (excluding own var)
            Set<String> outerOnly = new HashSet<>(boundRecVars);
            outerOnly.remove(r.var());
            if (checkCrossReference(r.body(), outerOnly)) return true;
            // Also check with all bound vars including own
            Set<String> withOwn = new HashSet<>(boundRecVars);
            withOwn.add(r.var());
            return checkCrossReference(r.body(), withOwn);
        } else if (type instanceof Branch b) {
            for (Choice c : b.choices()) {
                if (checkCrossReference(c.body(), boundRecVars)) return true;
            }
        } else if (type instanceof Select s) {
            for (Choice c : s.choices()) {
                if (checkCrossReference(c.body(), boundRecVars)) return true;
            }
        } else if (type instanceof Parallel p) {
            return checkCrossReference(p.left(), boundRecVars)
                    || checkCrossReference(p.right(), boundRecVars);
        } else if (type instanceof Sequence seq) {
            return checkCrossReference(seq.left(), boundRecVars)
                    || checkCrossReference(seq.right(), boundRecVars);
        }
        return false;
    }

    private static String witnessForLevel(LanguageLevel level) {
        return switch (level) {
            case L0 -> throw new IllegalArgumentException(
                    "L0 has no separation witness (it is the base level)");
            case L1 -> "&{a: end}";
            case L2 -> "+{a: end}";
            case L3 -> "(&{a: end} || &{b: end})";
            case L4 -> "rec X . &{a: X, b: end}";
            case L5 -> "(&{a: end} || &{b: end}) . &{c: end}";
            case L6 -> "(&{a: end} || (&{b: end} || &{c: end}))";
            case L7 -> "rec X . (&{a: end} || &{b: X})";
            case L8 -> "rec X . &{a: rec Y . &{b: X, c: Y}, d: end}";
            case L9 -> throw new UnsupportedOperationException(
                    "L9 (higher-order) witnesses are not yet implemented");
        };
    }

    private static LanguageLevel max(LanguageLevel a, LanguageLevel b) {
        return a.compareTo(b) >= 0 ? a : b;
    }
}
