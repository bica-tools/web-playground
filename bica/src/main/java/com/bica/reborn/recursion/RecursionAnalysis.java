package com.bica.reborn.recursion;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Complete recursion analysis of a session type.
 *
 * @param numRecBinders    number of rec binders in the type
 * @param maxNestingDepth  maximum nesting depth of rec binders
 * @param isGuarded        true iff all recursive variables are guarded
 * @param isContractive    true iff all recursive bodies are contractive
 * @param recursiveVars    set of all bound recursive variables
 * @param sccCount         number of SCCs in the state space
 * @param sccSizes         sizes of non-trivial SCCs (size &gt; 1)
 * @param isTailRecursive  true iff all recursive calls are in tail position
 */
public record RecursionAnalysis(
        int numRecBinders,
        int maxNestingDepth,
        boolean isGuarded,
        boolean isContractive,
        Set<String> recursiveVars,
        int sccCount,
        List<Integer> sccSizes,
        boolean isTailRecursive) {

    public RecursionAnalysis {
        Objects.requireNonNull(recursiveVars, "recursiveVars must not be null");
        Objects.requireNonNull(sccSizes, "sccSizes must not be null");
        recursiveVars = Set.copyOf(recursiveVars);
        sccSizes = List.copyOf(sccSizes);
    }
}
