package com.bica.reborn.concurrency;

/**
 * Concurrency classification for methods on session-typed objects.
 *
 * <p>The four levels form a lattice ordered by permissiveness:
 * <pre>
 *          SHARED          (top — any concurrent access)
 *         /      \
 *     SYNC      READ_ONLY  (incomparable middle elements)
 *         \      /
 *        EXCLUSIVE          (bottom — no concurrent access)
 * </pre>
 *
 * <p>Compatibility rule: two methods may execute concurrently iff
 * neither is {@code EXCLUSIVE}. See {@link ConcurrencyLattice#compatible}.
 */
public enum ConcurrencyLevel {

    /** Exclusive access — no concurrent calls permitted. */
    EXCLUSIVE("⊥"),

    /** Read-only access — concurrent with other read-only or sync methods. */
    READ_ONLY("R"),

    /** Synchronized access — concurrent with other sync or read-only methods. */
    SYNC("S"),

    /** Shared access — concurrent with any method. */
    SHARED("⊤");

    private final String symbol;

    ConcurrencyLevel(String symbol) {
        this.symbol = symbol;
    }

    /** Returns the symbolic representation of this level. */
    public String symbol() {
        return symbol;
    }
}
