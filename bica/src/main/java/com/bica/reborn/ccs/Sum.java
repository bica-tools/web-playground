package com.bica.reborn.ccs;

import java.util.List;
import java.util.Objects;

/** Non-deterministic choice {@code P + Q + ...}. */
public record Sum(List<CCSProcess> procs) implements CCSProcess {

    public Sum {
        Objects.requireNonNull(procs, "procs must not be null");
        procs = List.copyOf(procs);
    }
}
