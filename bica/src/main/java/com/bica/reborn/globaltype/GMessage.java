package com.bica.reborn.globaltype;

import java.util.List;
import java.util.Objects;

/**
 * Role-to-role interaction: sender -> receiver : { m₁: G₁, …, mₙ: Gₙ }.
 *
 * <p>The sender makes an internal choice among the labels; the receiver
 * observes the choice as an external choice.
 */
public record GMessage(String sender, String receiver, List<Choice> choices)
        implements GlobalType {

    /**
     * A single choice branch within a message interaction.
     */
    public record Choice(String label, GlobalType body) {
        public Choice {
            Objects.requireNonNull(label, "label must not be null");
            Objects.requireNonNull(body, "body must not be null");
        }
    }

    public GMessage {
        Objects.requireNonNull(sender, "sender must not be null");
        Objects.requireNonNull(receiver, "receiver must not be null");
        Objects.requireNonNull(choices, "choices must not be null");
        if (choices.isEmpty()) {
            throw new IllegalArgumentException("choices must not be empty");
        }
        choices = List.copyOf(choices);
    }
}
