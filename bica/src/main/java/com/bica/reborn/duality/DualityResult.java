package com.bica.reborn.duality;

import java.util.Objects;

/**
 * Result of duality verification between S and dual(S).
 *
 * @param typeStr           pretty-printed original type S
 * @param dualStr           pretty-printed dual type dual(S)
 * @param isInvolution      true iff dual(dual(S)) == S structurally
 * @param isIsomorphic      true iff L(S) ≅ L(dual(S)) as posets
 * @param selectionFlipped  true iff selection annotations are complementary
 */
public record DualityResult(
        String typeStr, String dualStr,
        boolean isInvolution, boolean isIsomorphic, boolean selectionFlipped) {

    public DualityResult {
        Objects.requireNonNull(typeStr, "typeStr must not be null");
        Objects.requireNonNull(dualStr, "dualStr must not be null");
    }
}
