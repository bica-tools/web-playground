package com.bica.reborn.ast;

import java.util.List;
import java.util.Objects;

/**
 * External choice {@code &{ m₁ : S₁ , … , mₙ : Sₙ }}.
 *
 * <p>Single-method branches also represent desugared sequencing:
 * {@code m . S} becomes {@code Branch(List.of(new Choice("m", S)))}.
 */
public record Branch(List<Choice> choices) implements SessionType {

    /** A single labeled choice within a branch or selection. */
    public record Choice(String label, SessionType body) {

        public Choice {
            Objects.requireNonNull(label, "label must not be null");
            Objects.requireNonNull(body, "body must not be null");
        }
    }

    public Branch {
        Objects.requireNonNull(choices, "choices must not be null");
        choices = List.copyOf(choices); // defensive immutable copy
    }
}
