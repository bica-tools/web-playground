package com.bica.reborn.ccs;

/**
 * Sealed interface for CCS (Calculus of Communicating Systems) process terms.
 *
 * <p>CCS processes model concurrent communicating systems. This AST supports:
 * <pre>
 * P  ::=  0                    -- nil (terminated)
 *      |  a . P                -- action prefix
 *      |  P + Q                -- non-deterministic choice (sum)
 *      |  P | Q                -- parallel composition
 *      |  P \ {a, b, ...}     -- restriction
 *      |  fix X . P            -- recursive process
 *      |  X                    -- process variable
 * </pre>
 *
 * <p>Step 26 of the 1000 Steps Towards Session Types as Algebraic Reticulates.
 */
public sealed interface CCSProcess
        permits Nil, ActionPrefix, Sum, CCSPar, Restrict, CCSRec, CCSVar {
}
