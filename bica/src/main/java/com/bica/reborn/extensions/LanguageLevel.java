package com.bica.reborn.extensions;

/**
 * Language levels in the session type hierarchy.
 *
 * <p>Each level strictly contains all lower levels. The hierarchy is:
 * <ul>
 *   <li>L0 &mdash; end only (terminated sessions)</li>
 *   <li>L1 &mdash; + branch (&amp;)</li>
 *   <li>L2 &mdash; + selection (+)</li>
 *   <li>L3 &mdash; + parallel (||)</li>
 *   <li>L4 &mdash; + recursion (rec X . S, X)</li>
 *   <li>L5 &mdash; + continuation (S1 || S2) . S3</li>
 *   <li>L6 &mdash; + nested parallel</li>
 *   <li>L7 &mdash; + mixed recursion and parallel</li>
 *   <li>L8 &mdash; + mutual recursion</li>
 *   <li>L9 &mdash; + higher-order (future)</li>
 * </ul>
 */
public enum LanguageLevel {

    L0("Terminated sessions (end)"),
    L1("External choice (branch)"),
    L2("Internal choice (selection)"),
    L3("Parallel composition"),
    L4("Recursive types"),
    L5("Continuation after parallel"),
    L6("Nested parallel composition"),
    L7("Mixed recursion and parallel"),
    L8("Mutual recursion"),
    L9("Higher-order session types");

    private final String description;

    LanguageLevel(String description) {
        this.description = description;
    }

    /** Human-readable description of this level. */
    public String description() {
        return description;
    }
}
