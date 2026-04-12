package com.bica.reborn.subtyping;

import java.util.Objects;

/**
 * Result of verifying width subtyping ↔ state-space embedding.
 *
 * <p>The Step 7 claim: S₁ ≤_GH S₂ ⟺ L(S₂) order-embeds into L(S₁).
 *
 * @param type1        pretty-printed first type (candidate subtype)
 * @param type2        pretty-printed second type (candidate supertype)
 * @param isSubtype    true iff type1 ≤_GH type2
 * @param hasEmbedding true iff L(type2) embeds into L(type1)
 * @param coincides    true iff isSubtype == hasEmbedding
 */
public record WidthSubtypingResult(
        String type1, String type2,
        boolean isSubtype, boolean hasEmbedding, boolean coincides) {

    public WidthSubtypingResult {
        Objects.requireNonNull(type1, "type1 must not be null");
        Objects.requireNonNull(type2, "type2 must not be null");
    }
}
