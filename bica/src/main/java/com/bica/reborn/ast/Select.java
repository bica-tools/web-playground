package com.bica.reborn.ast;

import java.util.List;
import java.util.Objects;

/** Internal choice {@code +{ l₁ : S₁ , … , lₙ : Sₙ }}. */
public record Select(List<Branch.Choice> choices) implements SessionType {

    public Select {
        Objects.requireNonNull(choices, "choices must not be null");
        choices = List.copyOf(choices); // defensive immutable copy
    }
}
