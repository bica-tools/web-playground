package com.bica.reborn.ast;

import java.util.Objects;

/**
 * T2b labelled one-step extension {@code m . S}.
 *
 * <p>{@code Chain(method, cont)} is the BICA Reborn counterpart of the
 * reticulate (Python) {@code Chain} AST node, landed in the T2b
 * modularity campaign (2026-04-11). It represents "perform method
 * {@code m}, then continue with {@code cont}" as a first-class
 * sequential-composition primitive, distinct from
 * {@code Branch} with a single arm.
 *
 * <p>The two {@code Branch([(m, S)])} and {@code Chain(m, S)} produce
 * lattice-isomorphic state spaces (see
 * {@code lean4-formalization/Reticulate/ChainAndSeq.lean} theorem
 * {@code chain_branch_iso_lattice_level}), but {@code Chain}
 * declares the sequential-step intent syntactically, enabling
 * structural induction on the disciplined fragment from
 * {@code docs/specs/modularity-classification-spec.md} §3.4.
 *
 * <p>This record is the Java companion to the Python
 * {@code reticulate.parser.Chain} class added in the same dispatch.
 * Per {@code feedback_java_python_parity}, both tools must recognise
 * the T2b Chain/Branch distinction.
 *
 * @param method the method label triggering the single step
 * @param cont   the continuation session type after the method call
 */
public record Chain(String method, SessionType cont) implements SessionType {

    public Chain {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(cont, "cont must not be null");
    }
}
