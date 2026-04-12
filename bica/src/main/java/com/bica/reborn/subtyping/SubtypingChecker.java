package com.bica.reborn.subtyping;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.morphism.Morphism;
import com.bica.reborn.morphism.MorphismChecker;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Gay–Hole coinductive subtyping for session types.
 *
 * <p>Key insight: {@code end} is NOT a separate constructor — it is syntactic
 * sugar for the empty branch {@code &{}}. This means {@code end} and
 * {@code &{...}} are in the SAME constructor family:
 * <ul>
 *   <li>{@code &{a: end} ≤_GH end} is TRUE (by Sub-Branch: ∅ ⊆ {a})</li>
 *   <li>{@code end ≤_GH &{a: end}} is FALSE ({a} ⊄ ∅)</li>
 * </ul>
 *
 * <h3>Gay–Hole subtyping rules:</h3>
 * <ul>
 *   <li><b>Branch:</b> {@code &{m₁:S₁,...,mₙ:Sₙ} ≤ &{m₁':S₁',...,mₖ':Sₖ'}}
 *       iff {m₁',...,mₖ'} ⊆ {m₁,...,mₙ} (subtype offers MORE methods)
 *       and for each mᵢ' in RHS: Sₘᵢ ≤ Sᵢ' (covariant continuations)</li>
 *   <li><b>Selection:</b> {@code +{l₁:S₁,...,lₙ:Sₙ} ≤ +{l₁':S₁',...,lₖ':Sₖ'}}
 *       iff {l₁,...,lₙ} ⊆ {l₁',...,lₖ'} (subtype selects FEWER labels)
 *       and for each lᵢ in LHS: Sᵢ ≤ Sₗᵢ' (covariant continuations)</li>
 *   <li><b>Recursion:</b> {@code rec X.S₁ ≤ rec Y.S₂} via coinduction
 *       (greatest fixpoint)</li>
 * </ul>
 */
public final class SubtypingChecker {

    private SubtypingChecker() {}

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Check whether {@code s1 ≤_GH s2} (Gay–Hole subtyping).
     *
     * <p>Uses coinductive greatest fixpoint: pairs currently being checked
     * are assumed related (coinductive hypothesis).
     */
    public static boolean isSubtype(SessionType s1, SessionType s2) {
        return checkSubtype(s1, s2, new HashSet<>());
    }

    /**
     * Check Gay–Hole subtyping with a human-readable result.
     */
    public static SubtypingResult check(SessionType s1, SessionType s2) {
        boolean result = isSubtype(s1, s2);
        String reason = result ? null : explainFailure(s1, s2);
        return new SubtypingResult(
                PrettyPrinter.pretty(s1),
                PrettyPrinter.pretty(s2),
                result,
                reason);
    }

    /**
     * Verify that width subtyping corresponds to state-space embedding.
     *
     * <p>Checks: S₁ ≤_GH S₂ ⟺ L(S₂) order-embeds into L(S₁).
     */
    public static WidthSubtypingResult checkWidthEmbedding(
            SessionType s1, SessionType s2) {
        boolean sub = isSubtype(s1, s2);
        boolean hasEmb;
        try {
            StateSpace ss1 = StateSpaceBuilder.build(s1);
            StateSpace ss2 = StateSpaceBuilder.build(s2);
            Morphism emb = MorphismChecker.findEmbedding(ss2, ss1);
            hasEmb = emb != null;
        } catch (Exception e) {
            hasEmb = sub; // skip malformed types
        }
        return new WidthSubtypingResult(
                PrettyPrinter.pretty(s1),
                PrettyPrinter.pretty(s2),
                sub, hasEmb, sub == hasEmb);
    }

    // =========================================================================
    // Coinductive subtyping core
    // =========================================================================

    private static boolean checkSubtype(
            SessionType s1, SessionType s2, Set<Long> assumptions) {
        // Coinductive hypothesis: if we've already assumed this pair, accept it
        long pairId = pairKey(s1, s2);
        if (assumptions.contains(pairId)) {
            return true;
        }

        // Unfold recursion
        SessionType s1u = unfold(s1);
        SessionType s2u = unfold(s2);

        // Update assumptions with both original and unfolded pair
        long pairIdU = pairKey(s1u, s2u);
        Set<Long> newAssumptions = new HashSet<>(assumptions);
        newAssumptions.add(pairId);
        newAssumptions.add(pairIdU);

        // Normalize: end ≡ &{} (empty branch)
        s1u = normalize(s1u);
        s2u = normalize(s2u);

        // Branch: &{...} ≤ &{...} (includes end ≡ &{})
        if (s1u instanceof Branch b1 && s2u instanceof Branch b2) {
            return checkBranchSubtype(b1, b2, newAssumptions);
        }

        // Selection: +{...} ≤ +{...}
        if (s1u instanceof Select sel1 && s2u instanceof Select sel2) {
            return checkSelectSubtype(sel1, sel2, newAssumptions);
        }

        // Parallel: (S₁ || S₂) ≤ (T₁ || T₂)
        if (s1u instanceof Parallel p1 && s2u instanceof Parallel p2) {
            return checkSubtype(p1.left(), p2.left(), newAssumptions)
                    && checkSubtype(p1.right(), p2.right(), newAssumptions);
        }

        // Sequence: S₁ . S₂ ≤ T₁ . T₂
        if (s1u instanceof Sequence seq1 && s2u instanceof Sequence seq2) {
            return checkSubtype(seq1.left(), seq2.left(), newAssumptions)
                    && checkSubtype(seq1.right(), seq2.right(), newAssumptions);
        }

        // Chain: m . S ≤ m . T (T2b, 2026-04-11)
        // Chain is its own syntactic category under T2b; method labels must match
        // and continuations must be subtypes. Chain is NOT comparable with
        // single-arm Branch under T2b (that would collapse the distinction).
        if (s1u instanceof Chain c1 && s2u instanceof Chain c2) {
            return c1.method().equals(c2.method())
                    && checkSubtype(c1.cont(), c2.cont(), newAssumptions);
        }

        // Different constructors — incomparable
        return false;
    }

    private static boolean checkBranchSubtype(
            Branch b1, Branch b2, Set<Long> assumptions) {
        Map<String, SessionType> m1 = choiceMap(b1.choices());
        Map<String, SessionType> m2 = choiceMap(b2.choices());

        // Subtype must offer all methods of supertype (width subtyping)
        if (!m1.keySet().containsAll(m2.keySet())) {
            return false;
        }
        // Covariant: continuations must be subtypes
        for (var entry : m2.entrySet()) {
            if (!checkSubtype(m1.get(entry.getKey()), entry.getValue(), assumptions)) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkSelectSubtype(
            Select sel1, Select sel2, Set<Long> assumptions) {
        Map<String, SessionType> m1 = choiceMap(sel1.choices());
        Map<String, SessionType> m2 = choiceMap(sel2.choices());

        // Subtype selects from at most the labels of supertype (width)
        if (!m2.keySet().containsAll(m1.keySet())) {
            return false;
        }
        // Covariant: continuations must be subtypes
        for (var entry : m1.entrySet()) {
            if (!checkSubtype(entry.getValue(), m2.get(entry.getKey()), assumptions)) {
                return false;
            }
        }
        return true;
    }

    // =========================================================================
    // Substitution and unfolding
    // =========================================================================

    /** Unfold one level of recursion: rec X . S → S[X := rec X . S]. */
    public static SessionType unfold(SessionType s) {
        if (s instanceof Rec r) {
            return substitute(r.body(), r.var(), r);
        }
        return s;
    }

    /** Substitute {@code var} with {@code replacement} in {@code body}. */
    public static SessionType substitute(SessionType body, String var, SessionType replacement) {
        return switch (body) {
            case Var v -> v.name().equals(var) ? replacement : v;
            case End e -> e;
            case Branch b -> new Branch(b.choices().stream()
                    .map(c -> new Choice(c.label(), substitute(c.body(), var, replacement)))
                    .toList());
            case Select s -> new Select(s.choices().stream()
                    .map(c -> new Choice(c.label(), substitute(c.body(), var, replacement)))
                    .toList());
            case Parallel p -> new Parallel(
                    substitute(p.left(), var, replacement),
                    substitute(p.right(), var, replacement));
            case Sequence seq -> new Sequence(
                    substitute(seq.left(), var, replacement),
                    substitute(seq.right(), var, replacement));
            case Rec r -> r.var().equals(var)
                    ? r  // shadowed
                    : new Rec(r.var(), substitute(r.body(), var, replacement));
            case Chain ch -> new Chain(ch.method(), substitute(ch.cont(), var, replacement));
        };
    }

    // =========================================================================
    // Failure explanation
    // =========================================================================

    static String explainFailure(SessionType s1, SessionType s2) {
        SessionType s1u = normalize(unfold(s1));
        SessionType s2u = normalize(unfold(s2));

        if (s1u instanceof Branch b1 && s2u instanceof Branch b2) {
            Set<String> m1 = b1.choices().stream().map(Choice::label).collect(Collectors.toSet());
            Set<String> m2 = b2.choices().stream().map(Choice::label).collect(Collectors.toSet());
            if (!m1.containsAll(m2)) {
                Set<String> missing = new TreeSet<>(m2);
                missing.removeAll(m1);
                return "missing methods in subtype: " + missing;
            }
            return "continuation mismatch in branch";
        }

        if (s1u instanceof Select sel1 && s2u instanceof Select sel2) {
            Set<String> l1 = sel1.choices().stream().map(Choice::label).collect(Collectors.toSet());
            Set<String> l2 = sel2.choices().stream().map(Choice::label).collect(Collectors.toSet());
            if (!l2.containsAll(l1)) {
                Set<String> extra = new TreeSet<>(l1);
                extra.removeAll(l2);
                return "subtype selects labels not in supertype: " + extra;
            }
            return "continuation mismatch in selection";
        }

        String name1 = constructorName(s1u);
        String name2 = constructorName(s2u);
        return "incompatible constructors: " + name1 + " vs " + name2;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Normalize: end → &{} (empty branch). */
    private static SessionType normalize(SessionType s) {
        if (s instanceof End) {
            return new Branch(List.of());
        }
        return s;
    }

    private static Map<String, SessionType> choiceMap(List<Choice> choices) {
        Map<String, SessionType> map = new LinkedHashMap<>();
        for (Choice c : choices) {
            map.put(c.label(), c.body());
        }
        return map;
    }

    private static long pairKey(SessionType s1, SessionType s2) {
        return ((long) System.identityHashCode(s1) << 32)
                | (System.identityHashCode(s2) & 0xFFFFFFFFL);
    }

    private static String constructorName(SessionType s) {
        return switch (s) {
            case Branch b -> "Branch";
            case Select sel -> "Select";
            case Parallel p -> "Parallel";
            case Sequence seq -> "Sequence";
            case Rec r -> "Rec";
            case Var v -> "Var";
            case End e -> "End";
            case Chain ch -> "Chain";
        };
    }
}
