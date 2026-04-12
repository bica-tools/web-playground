package com.bica.reborn.modularity;

/**
 * Result of a session-type modularity check.
 *
 * <p>Java mirror of the Python {@code reticulate.modularity.ModularityResult}
 * dataclass. Per {@code feedback_java_python_parity} the two types must
 * carry the same field names (modulo language idiom) and the same
 * verdict strings.
 *
 * <p>The {@code certificate} field distinguishes the two provenances of
 * the verdict:
 * <ul>
 *   <li>{@code "syntactic"} — the verdict was emitted by the
 *       T2b disciplined-fragment fast path
 *       ({@link ModularityChecker#isDisciplined}), appealing to the
 *       Lean {@code T2b_DF} closure theorem. No pentagon/diamond search
 *       was performed.</li>
 *   <li>{@code "semantic"} — the verdict was emitted by the state-space
 *       pentagon (N5) / diamond (M3) search in
 *       {@link com.bica.reborn.lattice.DistributivityChecker}.</li>
 *   <li>{@code "not_applicable"} — the state space is not even a lattice,
 *       so modularity is undefined.</li>
 * </ul>
 *
 * @param isModular                True iff the lattice is modular
 *                                 (= distributive under our unification).
 * @param isLattice                True iff the state space is a lattice.
 * @param isDistributive           True iff no M3 or N5 sublattice.
 * @param isAlgebraicallyModular   True iff no N5 (lattice-theoretic modularity).
 * @param classification           One of {@code "boolean"},
 *                                 {@code "distributive"}, {@code "modular"},
 *                                 {@code "lattice"}, {@code "not_lattice"}.
 * @param numStates                Number of states in the underlying lattice.
 * @param certificate              One of {@code "syntactic"},
 *                                 {@code "semantic"}, {@code "not_applicable"}.
 */
public record ModularityResult(
        boolean isModular,
        boolean isLattice,
        boolean isDistributive,
        boolean isAlgebraicallyModular,
        String classification,
        int numStates,
        String certificate) {

    /** Canonical certificate strings, pinned to match Python. */
    public static final String CERT_SYNTACTIC = "syntactic";
    public static final String CERT_SEMANTIC = "semantic";
    public static final String CERT_NOT_APPLICABLE = "not_applicable";
}
