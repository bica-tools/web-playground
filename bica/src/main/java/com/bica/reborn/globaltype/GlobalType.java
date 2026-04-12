package com.bica.reborn.globaltype;

/**
 * Sealed interface for multiparty global session type AST nodes.
 *
 * <p>A global type describes a protocol between multiple roles (participants):
 * <pre>
 *   G  ::=  sender -> receiver : { m₁ : G₁ , … , mₙ : Gₙ }
 *        |  G₁ || G₂
 *        |  rec X . G
 *        |  X
 *        |  end
 * </pre>
 */
public sealed interface GlobalType
        permits GEnd, GVar, GMessage, GParallel, GRec {
}
