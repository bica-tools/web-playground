package com.bica.reborn.music;

/**
 * Musical forms that can be derived from session type structure.
 *
 * <p>Session type constructors map to musical form:
 * <ul>
 *   <li>Sequential branch chains → binary/ternary form</li>
 *   <li>Parallel composition → polyphony (invention, fugue)</li>
 *   <li>Branch/Select choice → theme and variations</li>
 *   <li>Recursion → rondo, ritornello</li>
 * </ul>
 */
public enum MusicalForm {
    /** Single melodic line (sequential branch chain). */
    MONOPHONIC("Monophonic"),
    /** Two independent voices (parallel composition). */
    BINARY("Binary Form"),
    /** Three-part ABA structure. */
    TERNARY("Ternary Form"),
    /** Recurring theme with episodes (recursion). */
    RONDO("Rondo"),
    /** Theme with transformations (branch/select). */
    THEME_AND_VARIATIONS("Theme and Variations"),
    /** Two-part counterpoint (parallel, Bach style). */
    INVENTION("Invention"),
    /** Imitative counterpoint with subject/answer (3+ voices). */
    FUGUE("Fugue"),
    /** Four-part harmony (SATB parallel). */
    CHORALE("Chorale"),
    /** Orchestral form with exposition, development, recapitulation. */
    SONATA("Sonata Form"),
    /** Multiple staves, general polyphony. */
    POLYPHONIC("Polyphonic");

    private final String displayName;

    MusicalForm(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
