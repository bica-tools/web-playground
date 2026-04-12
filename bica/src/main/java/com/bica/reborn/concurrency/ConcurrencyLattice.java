package com.bica.reborn.concurrency;

import java.util.Objects;

/**
 * Lattice operations on {@link ConcurrencyLevel}.
 *
 * <p>The lattice has {@code SHARED} as top and {@code EXCLUSIVE} as bottom,
 * with {@code SYNC} and {@code READ_ONLY} as incomparable middle elements.
 *
 * <p>Compatibility is defined by the spec (section 6): two methods may execute
 * concurrently iff neither is {@code EXCLUSIVE}. This is intentionally different
 * from the lattice meet — {@code meet(SYNC, READ_ONLY) = EXCLUSIVE}, but
 * {@code compatible(SYNC, READ_ONLY) = true}.
 */
public final class ConcurrencyLattice {

    private ConcurrencyLattice() {}

    // Meet/join lookup tables indexed by ordinal.
    // Order: EXCLUSIVE(0), READ_ONLY(1), SYNC(2), SHARED(3)
    private static final ConcurrencyLevel[][] MEET_TABLE = {
        // EXCLUSIVE ∧ ...
        {ConcurrencyLevel.EXCLUSIVE, ConcurrencyLevel.EXCLUSIVE,
         ConcurrencyLevel.EXCLUSIVE, ConcurrencyLevel.EXCLUSIVE},
        // READ_ONLY ∧ ...
        {ConcurrencyLevel.EXCLUSIVE, ConcurrencyLevel.READ_ONLY,
         ConcurrencyLevel.EXCLUSIVE, ConcurrencyLevel.READ_ONLY},
        // SYNC ∧ ...
        {ConcurrencyLevel.EXCLUSIVE, ConcurrencyLevel.EXCLUSIVE,
         ConcurrencyLevel.SYNC, ConcurrencyLevel.SYNC},
        // SHARED ∧ ...
        {ConcurrencyLevel.EXCLUSIVE, ConcurrencyLevel.READ_ONLY,
         ConcurrencyLevel.SYNC, ConcurrencyLevel.SHARED},
    };

    private static final ConcurrencyLevel[][] JOIN_TABLE = {
        // EXCLUSIVE ∨ ...
        {ConcurrencyLevel.EXCLUSIVE, ConcurrencyLevel.READ_ONLY,
         ConcurrencyLevel.SYNC, ConcurrencyLevel.SHARED},
        // READ_ONLY ∨ ...
        {ConcurrencyLevel.READ_ONLY, ConcurrencyLevel.READ_ONLY,
         ConcurrencyLevel.SHARED, ConcurrencyLevel.SHARED},
        // SYNC ∨ ...
        {ConcurrencyLevel.SYNC, ConcurrencyLevel.SHARED,
         ConcurrencyLevel.SYNC, ConcurrencyLevel.SHARED},
        // SHARED ∨ ...
        {ConcurrencyLevel.SHARED, ConcurrencyLevel.SHARED,
         ConcurrencyLevel.SHARED, ConcurrencyLevel.SHARED},
    };

    /**
     * Returns the greatest lower bound (meet) of {@code a} and {@code b}.
     *
     * @throws NullPointerException if either argument is null
     */
    public static ConcurrencyLevel meet(ConcurrencyLevel a, ConcurrencyLevel b) {
        Objects.requireNonNull(a, "a must not be null");
        Objects.requireNonNull(b, "b must not be null");
        return MEET_TABLE[a.ordinal()][b.ordinal()];
    }

    /**
     * Returns the least upper bound (join) of {@code a} and {@code b}.
     *
     * @throws NullPointerException if either argument is null
     */
    public static ConcurrencyLevel join(ConcurrencyLevel a, ConcurrencyLevel b) {
        Objects.requireNonNull(a, "a must not be null");
        Objects.requireNonNull(b, "b must not be null");
        return JOIN_TABLE[a.ordinal()][b.ordinal()];
    }

    /**
     * Returns {@code true} iff methods at levels {@code a} and {@code b}
     * may safely execute concurrently.
     *
     * <p>The rule is: two methods are compatible iff neither is {@code EXCLUSIVE}.
     *
     * @throws NullPointerException if either argument is null
     */
    public static boolean compatible(ConcurrencyLevel a, ConcurrencyLevel b) {
        Objects.requireNonNull(a, "a must not be null");
        Objects.requireNonNull(b, "b must not be null");
        return a != ConcurrencyLevel.EXCLUSIVE && b != ConcurrencyLevel.EXCLUSIVE;
    }
}
