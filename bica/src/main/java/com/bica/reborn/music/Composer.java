package com.bica.reborn.music;

import com.bica.reborn.parser.Parser;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.*;

/**
 * Algorithmic composition grounded in session types.
 *
 * <p>Every composition is grounded in a session type: the type IS the score.
 * The pipeline is:
 * <pre>
 *     compose -> session type string -> parse -> build_statespace -> extract paths -> LilyPond
 * </pre>
 *
 * <p>Compositional structure maps to session type constructors:
 * <ul>
 *   <li>Parallel voices -> (S1 || S2)</li>
 *   <li>Sequential sections -> (S1 || S2) . (S3 || S4) . end</li>
 *   <li>Repeated material -> rec X . (S || S') . X</li>
 *   <li>Harmonic choices -> &amp;{option_a: S1, option_b: S2}</li>
 * </ul>
 */
public final class Composer {

    private Composer() {}

    // -----------------------------------------------------------------------
    // Music theory: pitch/transposition
    // -----------------------------------------------------------------------

    private static final Map<String, Integer> PITCH_TO_SEMITONE = new HashMap<>();
    private static final Map<Integer, String[]> SEMITONE_TO_PITCH = new HashMap<>();

    static {
        PITCH_TO_SEMITONE.put("c:", 0);   PITCH_TO_SEMITONE.put("c:is", 1);
        PITCH_TO_SEMITONE.put("d:es", 1); PITCH_TO_SEMITONE.put("d:", 2);
        PITCH_TO_SEMITONE.put("d:is", 3); PITCH_TO_SEMITONE.put("e:es", 3);
        PITCH_TO_SEMITONE.put("e:", 4);   PITCH_TO_SEMITONE.put("f:", 5);
        PITCH_TO_SEMITONE.put("f:is", 6); PITCH_TO_SEMITONE.put("g:es", 6);
        PITCH_TO_SEMITONE.put("g:", 7);   PITCH_TO_SEMITONE.put("g:is", 8);
        PITCH_TO_SEMITONE.put("a:es", 8); PITCH_TO_SEMITONE.put("a:", 9);
        PITCH_TO_SEMITONE.put("a:is", 10);PITCH_TO_SEMITONE.put("b:es", 10);
        PITCH_TO_SEMITONE.put("b:", 11);

        SEMITONE_TO_PITCH.put(0, new String[]{"c", ""});
        SEMITONE_TO_PITCH.put(1, new String[]{"c", "is"});
        SEMITONE_TO_PITCH.put(2, new String[]{"d", ""});
        SEMITONE_TO_PITCH.put(3, new String[]{"e", "es"});
        SEMITONE_TO_PITCH.put(4, new String[]{"e", ""});
        SEMITONE_TO_PITCH.put(5, new String[]{"f", ""});
        SEMITONE_TO_PITCH.put(6, new String[]{"f", "is"});
        SEMITONE_TO_PITCH.put(7, new String[]{"g", ""});
        SEMITONE_TO_PITCH.put(8, new String[]{"g", "is"});
        SEMITONE_TO_PITCH.put(9, new String[]{"a", ""});
        SEMITONE_TO_PITCH.put(10, new String[]{"b", "es"});
        SEMITONE_TO_PITCH.put(11, new String[]{"b", ""});
    }

    private static int pitchToSemitone(MusicalNote note) {
        String key = note.pitch() + ":" + note.accidental();
        return PITCH_TO_SEMITONE.getOrDefault(key, 0);
    }

    static MusicalNote transposeNote(MusicalNote note, int semitones) {
        if (note.isRest()) return note;
        int semi = pitchToSemitone(note);
        int newAbsolute = semi + semitones;
        int newSemi = ((newAbsolute % 12) + 12) % 12; // proper modulo
        int octaveShift = Math.floorDiv(newAbsolute, 12);
        String[] pa = SEMITONE_TO_PITCH.get(newSemi);
        return new MusicalNote(pa[0], pa[1], note.octave() + octaveShift,
                note.duration(), note.dotted());
    }

    static List<MusicalNote> transposePhrase(List<MusicalNote> notes, int semitones) {
        return notes.stream().map(n -> transposeNote(n, semitones)).toList();
    }

    static List<MusicalNote> augment(List<MusicalNote> notes) {
        Map<String, String> durMap = Map.of("16", "8", "8", "4", "4", "2", "2", "1", "1", "1");
        return notes.stream().map(n ->
                new MusicalNote(n.pitch(), n.accidental(), n.octave(),
                        durMap.getOrDefault(n.duration(), n.duration()), n.dotted())
        ).toList();
    }

    // -----------------------------------------------------------------------
    // Shorthand note constructor
    // -----------------------------------------------------------------------

    static MusicalNote n(String spec) {
        MusicalNote note = MusicChecker.parseMusicalLabel(spec);
        if (note == null) throw new IllegalArgumentException("Invalid note: " + spec);
        return note;
    }

    static MusicalNote rest(String duration) {
        return new MusicalNote("r", "", -1, duration, false);
    }

    // -----------------------------------------------------------------------
    // Session type builders
    // -----------------------------------------------------------------------

    static String phraseToSessionType(List<MusicalNote> notes) {
        if (notes.isEmpty()) return "end";
        StringBuilder result = new StringBuilder("end");
        for (int i = notes.size() - 1; i >= 0; i--) {
            String label = MusicChecker.noteToLabel(notes.get(i));
            result = new StringBuilder("&{" + label + ": " + result + "}");
        }
        return result.toString();
    }

    static String parallelSection(List<List<MusicalNote>> voices) {
        if (voices.isEmpty()) return "end";
        if (voices.size() == 1) return phraseToSessionType(voices.getFirst());
        List<String> types = voices.stream()
                .map(Composer::phraseToSessionType).toList();
        // Right-associate
        String result = types.getLast();
        for (int i = types.size() - 2; i >= 0; i--) {
            result = "(" + types.get(i) + " || " + result + ")";
        }
        return result;
    }

    static String chainSections(List<List<List<MusicalNote>>> sections) {
        if (sections.isEmpty()) return "end";
        List<String> parts = sections.stream()
                .map(Composer::parallelSection).toList();
        String result = "end";
        for (int i = parts.size() - 1; i >= 0; i--) {
            result = parts.get(i) + " . " + result;
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Core: compose from structured sections
    // -----------------------------------------------------------------------

    private static CompositionResult composeFromSections(
            List<List<List<MusicalNote>>> sections,
            List<String> sectionNames,
            List<String> voiceNames,
            String title, String composer,
            String key, String timeSig) {

        String typeStr = chainSections(sections);

        // Verify it parses and forms a valid state space
        var ast = Parser.parse(typeStr);
        StateSpace ss = StateSpaceBuilder.build(ast);

        // Assemble full note sequences per voice
        int nVoices = sections.stream().mapToInt(List::size).max().orElse(0);
        List<List<MusicalNote>> voiceNotes = new ArrayList<>();
        for (int i = 0; i < nVoices; i++) voiceNotes.add(new ArrayList<>());

        for (var sec : sections) {
            for (int i = 0; i < sec.size() && i < nVoices; i++) {
                voiceNotes.get(i).addAll(sec.get(i));
            }
        }

        List<MusicalPath> paths = new ArrayList<>();
        for (int i = 0; i < nVoices; i++) {
            String name = i < voiceNames.size() ? voiceNames.get(i) : "Voice " + (i + 1);
            paths.add(new MusicalPath(voiceNotes.get(i), name));
        }

        String ly = MusicChecker.generateLilypond(paths, title, composer, key, timeSig);
        MusicalForm form = nVoices >= 4 ? MusicalForm.CHORALE :
                           nVoices == 3 ? MusicalForm.FUGUE :
                           nVoices == 2 ? MusicalForm.INVENTION : MusicalForm.MONOPHONIC;

        MusicResult music = new MusicResult(paths, ly, nVoices > 1, form, List.of());

        return new CompositionResult(typeStr, music, sectionNames,
                ss.states().size(), ss.transitions().size());
    }

    // -----------------------------------------------------------------------
    // Two-Part Invention
    // -----------------------------------------------------------------------

    /**
     * Compose a two-part invention in Bach style.
     *
     * <p>Structure:
     * <ul>
     *   <li>Exposition: Subject in RH, Answer in LH</li>
     *   <li>Episode: Sequential development</li>
     *   <li>Middle entry: Subject in LH, countersubject in RH</li>
     *   <li>Coda: Cadential formula</li>
     * </ul>
     */
    public static CompositionResult composeInvention() {
        return composeInvention("Two-Part Invention", "Composed via Session Types");
    }

    public static CompositionResult composeInvention(String title, String composer) {
        List<MusicalNote> subject = List.of(
                n("C5e"), n("D5e"), n("E5e"), n("F5e"),
                n("D5e"), n("E5e"), n("C5e"), n("G5e"));

        List<MusicalNote> countersubject = List.of(
                n("E4e"), n("D4e"), n("C4e"), n("B3e"),
                n("D4e"), n("C4e"), n("E4e"), n("G3e"));

        List<MusicalNote> answer = transposePhrase(transposePhrase(subject, 7), -12);
        List<MusicalNote> counterAnswer = transposePhrase(countersubject, 7);

        List<MusicalNote> codaRh = List.of(
                n("G5q"), n("F5q"), n("E5q"), n("D5q"),
                n("C5w"));
        List<MusicalNote> codaLh = List.of(
                n("E3q"), n("F3q"), n("G3q"), n("A3q"),
                n("C3w"));

        List<List<List<MusicalNote>>> sections = List.of(
                List.of(subject, countersubject),
                List.of(counterAnswer, answer),
                List.of(codaRh, codaLh));

        return composeFromSections(sections,
                List.of("Exposition (subject)", "Exposition (answer)", "Coda"),
                List.of("Right Hand", "Left Hand"),
                title, composer, "c \\major", "4/4");
    }

    // -----------------------------------------------------------------------
    // Chorale
    // -----------------------------------------------------------------------

    /**
     * Compose a 4-part chorale (SATB) with standard harmonic progression.
     *
     * <p>Progression: I - IV - V - I - vi - IV - V7 - I
     */
    public static CompositionResult composeChorale() {
        return composeChorale("Chorale in C Major", "Composed via Session Types");
    }

    public static CompositionResult composeChorale(String title, String composer) {
        List<MusicalNote> soprano = List.of(
                n("E5q"), n("F5q"), n("G5q"), n("E5q"),
                n("C5q"), n("F5q"), n("G5q"), n("C5q"),
                n("C5w"));
        List<MusicalNote> alto = List.of(
                n("C5q"), n("C5q"), n("D5q"), n("C5q"),
                n("A4q"), n("A4q"), n("B4q"), n("G4q"),
                n("E4w"));
        List<MusicalNote> tenor = List.of(
                n("G4q"), n("A4q"), n("B4q"), n("G4q"),
                n("E4q"), n("F4q"), n("G4q"), n("E4q"),
                n("G3w"));
        List<MusicalNote> bass = List.of(
                n("C3q"), n("F3q"), n("G3q"), n("C3q"),
                n("A2q"), n("F2q"), n("G2q"), n("C3q"),
                n("C3w"));

        return composeFromSections(
                List.of(List.of(soprano, alto, tenor, bass)),
                List.of("Chorale"),
                List.of("Soprano", "Alto", "Tenor", "Bass"),
                title, composer, "c \\major", "4/4");
    }

    // -----------------------------------------------------------------------
    // Fugue Exposition
    // -----------------------------------------------------------------------

    /**
     * Compose a 3-voice fugue exposition.
     *
     * <p>Structure:
     * <ul>
     *   <li>Voice 1 (Soprano): Subject -> Free</li>
     *   <li>Voice 2 (Alto): Rest -> Answer</li>
     *   <li>Voice 3 (Bass): Rest -> Rest -> Subject (lower octave)</li>
     * </ul>
     */
    public static CompositionResult composeFugue() {
        return composeFugue("Fugue Exposition in C Minor", "Composed via Session Types");
    }

    public static CompositionResult composeFugue(String title, String composer) {
        List<MusicalNote> subject = List.of(
                n("C5q"), n("Ef5q"), n("G5q"), n("C5q"),
                n("Af4e"), n("Bf4e"), n("C5e"), n("D5e"), n("Ef5q"), n("D5q"));

        List<MusicalNote> answer = transposePhrase(transposePhrase(subject, 7), -12);
        List<MusicalNote> bassSubject = transposePhrase(subject, -12);

        List<MusicalNote> twoMeasuresRest = List.of(rest("1"), rest("1"));

        List<MusicalNote> freeS = List.of(
                n("G5e"), n("F5e"), n("Ef5e"), n("D5e"),
                n("C5q"), n("Bf4q"),
                n("Af4e"), n("Bf4e"), n("C5e"), n("D5e"), n("Ef5q"), n("D5q"));

        List<MusicalNote> conclusion = List.of(n("C5w"));

        return composeFromSections(
                List.of(
                        List.of(subject, twoMeasuresRest, twoMeasuresRest),
                        List.of(freeS, answer, twoMeasuresRest),
                        List.of(conclusion, conclusion, bassSubject)),
                List.of("Entry 1 (Soprano)", "Entry 2 (Alto)", "Entry 3 (Bass)"),
                List.of("Soprano", "Alto", "Bass"),
                title, composer, "c \\minor", "4/4");
    }

    // -----------------------------------------------------------------------
    // Theme and Variations
    // -----------------------------------------------------------------------

    /**
     * Compose a theme with variations.
     *
     * <ul>
     *   <li>Theme: Simple melody in G major</li>
     *   <li>Var 1: Minor mode</li>
     *   <li>Var 2: Augmented (doubled durations)</li>
     *   <li>Coda: Theme returns</li>
     * </ul>
     */
    public static CompositionResult composeThemeAndVariations() {
        return composeThemeAndVariations(
                "Theme and Variations in G Major", "Composed via Session Types");
    }

    public static CompositionResult composeThemeAndVariations(String title, String composer) {
        List<MusicalNote> themeA = List.of(
                n("G4q"), n("A4q"), n("B4q"), n("A4q"),
                n("G4q"), n("Fs4q"), n("E4q"), n("D4q"));
        List<MusicalNote> themeB = List.of(
                n("E4q"), n("Fs4q"), n("G4q"), n("A4q"),
                n("B4q"), n("A4q"), n("G4h"));
        List<MusicalNote> theme = new ArrayList<>(themeA);
        theme.addAll(themeB);

        List<MusicalNote> bassA = List.of(n("G3h"), n("D3h"), n("E3h"), n("B2h"));
        List<MusicalNote> bassB = List.of(n("C3h"), n("D3h"), n("G3q"), n("D3q"), n("G3h"));
        List<MusicalNote> bassTheme = new ArrayList<>(bassA);
        bassTheme.addAll(bassB);

        // Variation: minor mode
        List<MusicalNote> var2 = List.of(
                n("G4q"), n("A4q"), n("Bf4q"), n("A4q"),
                n("G4q"), n("F4q"), n("Ef4q"), n("D4q"),
                n("Ef4q"), n("F4q"), n("G4q"), n("A4q"),
                n("Bf4q"), n("A4q"), n("G4h"));
        List<MusicalNote> bassVar2 = List.of(
                n("G3h"), n("D3h"), n("Ef3h"), n("Bf2h"),
                n("C3h"), n("D3h"), n("G3q"), n("D3q"), n("G3h"));

        // Variation: augmented
        List<MusicalNote> var3 = new ArrayList<>(augment(themeA));
        var3.addAll(augment(themeB));
        List<MusicalNote> bassVar3 = List.of(
                n("G3w"), n("D3w"), n("E3w"), n("B2w"),
                n("C3w"), n("D3w"), n("G3h"), n("D3h"), n("G3w"));

        // Coda
        List<MusicalNote> codaRh = new ArrayList<>(themeA);
        codaRh.addAll(List.of(n("D5q"), n("C5q"), n("B4q"), n("A4q"), n("G4w")));
        List<MusicalNote> codaLh = new ArrayList<>(bassA);
        codaLh.addAll(List.of(n("D3h"), n("G2h"), n("G3w")));

        return composeFromSections(
                List.of(
                        List.of(theme, bassTheme),
                        List.of(var2, bassVar2),
                        List.of(var3, bassVar3),
                        List.of(codaRh, codaLh)),
                List.of("Theme", "Var. 2 (Minor)", "Var. 3 (Augmented)", "Coda"),
                List.of("Right Hand", "Left Hand"),
                title, composer, "g \\major", "4/4");
    }
}
