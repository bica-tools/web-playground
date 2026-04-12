package com.bica.reborn.duality;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.morphism.Morphism;
import com.bica.reborn.morphism.MorphismChecker;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.statespace.TransitionKind;

import java.util.Map;

/**
 * Session type duality (Step 8).
 *
 * <p>Duality swaps the locus of control:
 * <ul>
 *   <li>Branch (external choice by environment) ↔ Selection (internal choice by object)</li>
 *   <li>All other constructors are structurally preserved (covariant in sub-terms)</li>
 * </ul>
 *
 * <h3>Key properties:</h3>
 * <ul>
 *   <li><b>Involution:</b> dual(dual(S)) = S (structurally)</li>
 *   <li><b>Self-dual:</b> dual(end) = end</li>
 *   <li><b>State-space preservation:</b> L(S) ≅ L(dual(S)) as posets</li>
 *   <li><b>Selection annotation flip:</b> selection_transitions(L(dual(S)))
 *       complements selection_transitions(L(S))</li>
 * </ul>
 */
public final class DualityChecker {

    private DualityChecker() {}

    // =========================================================================
    // The dual operation
    // =========================================================================

    /**
     * Compute the dual of a session type.
     *
     * <p>Swaps Branch ↔ Select (external ↔ internal choice).
     * All other constructors are preserved structurally.
     */
    public static SessionType dual(SessionType s) {
        return switch (s) {
            case End e -> new End();
            case Var v -> new Var(v.name());
            case Branch b -> new Select(b.choices().stream()
                    .map(c -> new Choice(c.label(), dual(c.body())))
                    .toList());
            case Select sel -> new Branch(sel.choices().stream()
                    .map(c -> new Choice(c.label(), dual(c.body())))
                    .toList());
            case Parallel p -> new Parallel(dual(p.left()), dual(p.right()));
            case Sequence seq -> new Sequence(dual(seq.left()), dual(seq.right()));
            case Rec r -> new Rec(r.var(), dual(r.body()));
            case Chain ch -> new Chain(ch.method(), dual(ch.cont()));
        };
    }

    // =========================================================================
    // Duality verification
    // =========================================================================

    /**
     * Verify duality properties for a session type.
     *
     * <p>Checks:
     * <ol>
     *   <li>Involution: dual(dual(S)) == S</li>
     *   <li>State-space isomorphism: L(S) ≅ L(dual(S))</li>
     *   <li>Selection annotation flip</li>
     * </ol>
     */
    public static DualityResult check(SessionType s) {
        SessionType d = dual(s);
        SessionType dd = dual(d);

        // 1. Involution check
        boolean involution = s.equals(dd);

        // 2. State-space isomorphism + 3. Selection flip
        boolean isIso = false;
        boolean selFlipped = false;
        try {
            StateSpace ssS = StateSpaceBuilder.build(s);
            StateSpace ssD = StateSpaceBuilder.build(d);
            Morphism iso = MorphismChecker.findIsomorphism(ssS, ssD);
            isIso = iso != null;

            if (iso != null) {
                selFlipped = checkSelectionFlip(ssS, ssD, iso.mapping());
            }
        } catch (Exception e) {
            // skip malformed types
        }

        return new DualityResult(
                PrettyPrinter.pretty(s),
                PrettyPrinter.pretty(d),
                involution, isIso, selFlipped);
    }

    // =========================================================================
    // Selection flip check
    // =========================================================================

    /**
     * Check that selection annotations are flipped under the isomorphism.
     *
     * <p>For each transition (src, label, tgt) in L(S):
     * if it's a selection transition in L(S), the corresponding transition
     * in L(dual(S)) should NOT be a selection, and vice versa.
     */
    static boolean checkSelectionFlip(
            StateSpace ssS, StateSpace ssD, Map<Integer, Integer> iso) {
        for (var t : ssS.transitions()) {
            boolean isSelS = t.kind() == TransitionKind.SELECTION;
            int mappedSrc = iso.get(t.source());
            int mappedTgt = iso.get(t.target());

            // Find corresponding transition in dual
            Boolean isSelD = null;
            for (var td : ssD.transitions()) {
                if (td.source() == mappedSrc && td.label().equals(t.label())
                        && td.target() == mappedTgt) {
                    isSelD = td.kind() == TransitionKind.SELECTION;
                    break;
                }
            }

            if (isSelD == null) {
                return false; // no corresponding transition
            }
            if (isSelS == isSelD) {
                return false; // not flipped
            }
        }
        return true;
    }
}
