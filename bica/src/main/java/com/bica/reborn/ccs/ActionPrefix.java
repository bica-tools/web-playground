package com.bica.reborn.ccs;

import java.util.Objects;

/** Action prefix {@code a . P} — perform action then continue as P. */
public record ActionPrefix(String action, CCSProcess cont) implements CCSProcess {

    public ActionPrefix {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(cont, "cont must not be null");
    }
}
