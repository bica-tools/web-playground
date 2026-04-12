package com.bica.reborn.ast;

/**
 * Sealed interface for session type AST nodes.
 *
 * <p>Session types describe communication protocols on objects. The eight
 * permitted subtypes correspond to the grammar:
 * <pre>
 * S  ::=  end                              -- terminated
 *      |  X                                -- variable
 *      |  &amp;{ m₁ : S₁ , … , mₙ : Sₙ }    -- branch (external choice)
 *      |  +{ l₁ : S₁ , … , lₙ : Sₙ }    -- selection (internal choice)
 *      |  ( S₁ || S₂ )                    -- parallel
 *      |  rec X . S                        -- recursion
 *      |  S₁ . S₂                          -- sequencing (general)
 *      |  m . S                            -- Chain (T2b, 2026-04-11)
 * </pre>
 *
 * <p>The {@code Chain} primitive is the T2b labelled-one-step extension
 * from the modularity campaign. See
 * {@code docs/specs/chain-and-seq-spec.md} §2.3 and
 * {@code Chain.java}.
 */
public sealed interface SessionType
        permits End, Var, Branch, Select, Parallel, Rec, Sequence, Chain {
}
