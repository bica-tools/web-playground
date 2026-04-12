package com.bica.reborn.recursion;

import java.util.List;
import java.util.Objects;

/**
 * Result of contractivity analysis.
 *
 * <p>A recursive type {@code rec X . S} is <b>contractive</b> if S is not
 * itself a {@link com.bica.reborn.ast.Var} or {@link com.bica.reborn.ast.Rec}
 * — the body must begin with an observable action.
 *
 * @param isContractive      true iff every recursive body makes observable progress
 * @param nonContractiveVars variables whose bodies don't make progress
 */
public record ContractivityResult(
        boolean isContractive,
        List<String> nonContractiveVars) {

    public ContractivityResult {
        Objects.requireNonNull(nonContractiveVars, "nonContractiveVars must not be null");
        nonContractiveVars = List.copyOf(nonContractiveVars);
    }
}
