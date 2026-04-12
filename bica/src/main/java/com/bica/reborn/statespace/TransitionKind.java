package com.bica.reborn.statespace;

/**
 * Distinguishes method calls from selection labels in the state space.
 *
 * <ul>
 *   <li>{@code METHOD} — external choice ({@code &{...}}): the client calls this method</li>
 *   <li>{@code SELECTION} — internal choice ({@code +{...}}): the object decides, client observes</li>
 * </ul>
 */
public enum TransitionKind {
    METHOD,
    SELECTION
}
