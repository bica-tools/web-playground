package com.bica.reborn.morphism;

/**
 * Classification of structure-preserving maps between state spaces.
 *
 * <p>Ordered by strength (strict inclusions):
 * <pre>
 *     ISOMORPHISM &sub; EMBEDDING &sub; PROJECTION &sub; HOMOMORPHISM
 * </pre>
 *
 * <ul>
 *   <li>{@code ISOMORPHISM} &mdash; bijective + order-preserving + order-reflecting</li>
 *   <li>{@code EMBEDDING} &mdash; injective + order-preserving + order-reflecting</li>
 *   <li>{@code PROJECTION} &mdash; surjective + order-preserving</li>
 *   <li>{@code HOMOMORPHISM} &mdash; order-preserving only</li>
 * </ul>
 */
public enum MorphismKind {

    /** Bijective order-isomorphism (preserving + reflecting + injective + surjective). */
    ISOMORPHISM,

    /** Injective order-embedding (preserving + reflecting + injective). */
    EMBEDDING,

    /** Surjective order-projection (preserving + surjective). */
    PROJECTION,

    /** Order-preserving map (minimum requirement). */
    HOMOMORPHISM
}
