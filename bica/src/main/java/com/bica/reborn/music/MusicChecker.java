package com.bica.reborn.music;

import com.bica.reborn.ast.*;
import com.bica.reborn.ast.Branch.Choice;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Session types to LilyPond music scores.
 *
 * <p>Translates session type state spaces into musical scores:
 * <ul>
 *   <li>Transition labels encode notes: C4q = C4 quarter note</li>
 *   <li>Parallel composition = polyphony (multiple staves)</li>
 *   <li>Paths through state space = valid performances</li>
 *   <li>Recursion = repeated sections</li>
 * </ul>
 *
 * <p>Label format: {@code <note>[s|f]<octave><duration>[d]}
 * <ul>
 *   <li>note: C D E F G A B (case-insensitive)</li>
 *   <li>s = sharp, f = flat (after note letter)</li>
 *   <li>octave: 0-8</li>
 *   <li>duration: w=whole h=half q=quarter e=eighth x=sixteenth</li>
 *   <li>d = dotted (append after duration)</li>
 * </ul>
 */
public final class MusicChecker {

    private MusicChecker() {}

    // -----------------------------------------------------------------------
    // Label parsing
    // -----------------------------------------------------------------------

    private static final Pattern NOTE_RE = Pattern.compile(
            "^([A-Ga-g])(s|f)?(\\d)([whqex])(d)?$");
    private static final Pattern REST_RE = Pattern.compile(
            "^[Rr]([whqex])(d)?$");

    private static final Map<String, String> DURATION_MAP = Map.of(
            "w", "1", "h", "2", "q", "4", "e", "8", "x", "16");

    /**
     * Parse a transition label as a musical note.
     *
     * @return the parsed note, or {@code null} if the label is not musical
     */
    public static MusicalNote parseMusicalLabel(String label) {
        // Try rest
        Matcher m = REST_RE.matcher(label);
        if (m.matches()) {
            String dur = DURATION_MAP.get(m.group(1));
            boolean dotted = m.group(2) != null;
            return new MusicalNote("r", "", -1, dur, dotted);
        }

        // Try note
        m = NOTE_RE.matcher(label);
        if (!m.matches()) return null;

        String pitch = m.group(1).toLowerCase();
        String accRaw = m.group(2) != null ? m.group(2) : "";
        int octave = Integer.parseInt(m.group(3));
        String dur = DURATION_MAP.get(m.group(4));
        boolean dotted = m.group(5) != null;

        String accidental = switch (accRaw) {
            case "s" -> "is";
            case "f" -> "es";
            default -> "";
        };

        return new MusicalNote(pitch, accidental, octave, dur, dotted);
    }

    /**
     * Check whether a label is a valid musical notation.
     */
    public static boolean isMusicalLabel(String label) {
        return parseMusicalLabel(label) != null;
    }

    // -----------------------------------------------------------------------
    // LilyPond conversion
    // -----------------------------------------------------------------------

    /**
     * Convert a MusicalNote to a LilyPond token.
     *
     * <p>Uses absolute octave mode: C3 = c (no mark), C4 = c', C5 = c'', C2 = c,
     */
    public static String noteToLilypond(MusicalNote note) {
        if (note.isRest()) {
            String s = "r" + note.duration();
            return note.dotted() ? s + "." : s;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(note.pitch()).append(note.accidental());

        if (note.octave() >= 4) {
            sb.append("'".repeat(note.octave() - 3));
        } else if (note.octave() <= 2) {
            sb.append(",".repeat(3 - note.octave()));
        }
        // octave 3 = no mark

        sb.append(note.duration());
        if (note.dotted()) sb.append(".");
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Note-to-label conversion (reverse)
    // -----------------------------------------------------------------------

    private static final Map<String, String> ACC_TO_LABEL = Map.of(
            "", "", "is", "s", "es", "f");
    private static final Map<String, String> DUR_TO_LABEL = Map.of(
            "1", "w", "2", "h", "4", "q", "8", "e", "16", "x");

    /**
     * Convert a MusicalNote back to a session type label like "C4q" or "Rh".
     */
    public static String noteToLabel(MusicalNote note) {
        if (note.isRest()) {
            String dur = DUR_TO_LABEL.getOrDefault(note.duration(), "q");
            return "R" + dur + (note.dotted() ? "d" : "");
        }
        String pitch = note.pitch().toUpperCase();
        String acc = ACC_TO_LABEL.getOrDefault(note.accidental(), "");
        String dur = DUR_TO_LABEL.getOrDefault(note.duration(), "q");
        return pitch + acc + note.octave() + dur + (note.dotted() ? "d" : "");
    }

    // -----------------------------------------------------------------------
    // Path extraction from AST
    // -----------------------------------------------------------------------

    /**
     * Extract musical notes from a session type AST by walking branches sequentially.
     */
    static List<MusicalNote> extractNotesFromAst(SessionType ast) {
        List<MusicalNote> notes = new ArrayList<>();
        extractNotesRecursive(ast, notes, new HashSet<>());
        return notes;
    }

    private static void extractNotesRecursive(SessionType node, List<MusicalNote> notes,
                                               Set<String> seenVars) {
        switch (node) {
            case End e -> { /* done */ }
            case Var v -> { /* stop at recursion variable */ }
            case Branch b -> {
                // Take the first choice (leftmost path)
                if (!b.choices().isEmpty()) {
                    Choice first = b.choices().getFirst();
                    MusicalNote note = parseMusicalLabel(first.label());
                    if (note != null) notes.add(note);
                    extractNotesRecursive(first.body(), notes, seenVars);
                }
            }
            case Select s -> {
                if (!s.choices().isEmpty()) {
                    var first = s.choices().getFirst();
                    MusicalNote note = parseMusicalLabel(first.label());
                    if (note != null) notes.add(note);
                    extractNotesRecursive(first.body(), notes, seenVars);
                }
            }
            case Parallel p -> { /* handled at top level */ }
            case Rec r -> {
                if (!seenVars.contains(r.var())) {
                    seenVars.add(r.var());
                    extractNotesRecursive(r.body(), notes, seenVars);
                }
            }
            case Sequence seq -> {
                extractNotesRecursive(seq.left(), notes, seenVars);
                extractNotesRecursive(seq.right(), notes, seenVars);
            }
            case Chain ch -> {
                // T2b: Chain carries a method label (possibly a musical note)
                MusicalNote note = parseMusicalLabel(ch.method());
                if (note != null) notes.add(note);
                extractNotesRecursive(ch.cont(), notes, seenVars);
            }
        }
    }

    /** Collect branches of a (possibly nested) binary Parallel into a flat list. */
    private static List<SessionType> flattenParallel(Parallel par) {
        List<SessionType> result = new ArrayList<>();
        if (par.left() instanceof Parallel lp) {
            result.addAll(flattenParallel(lp));
        } else {
            result.add(par.left());
        }
        if (par.right() instanceof Parallel rp) {
            result.addAll(flattenParallel(rp));
        } else {
            result.add(par.right());
        }
        return result;
    }

    /**
     * Extract voices from a parallel session type AST.
     */
    static List<MusicalPath> extractVoicesFromAst(SessionType ast,
                                                    List<String> voiceNames) {
        List<String> defaultNames = List.of("Right Hand", "Left Hand",
                "Voice 3", "Voice 4", "Voice 5", "Voice 6");
        List<String> names = voiceNames != null ? voiceNames : defaultNames;

        if (ast instanceof Parallel par) {
            List<SessionType> branches = flattenParallel(par);
            List<MusicalPath> voices = new ArrayList<>();
            for (int i = 0; i < branches.size(); i++) {
                String name = i < names.size() ? names.get(i) : "Voice " + (i + 1);
                List<MusicalNote> notes = extractNotesFromAst(branches.get(i));
                voices.add(new MusicalPath(notes, name));
            }
            return voices;
        }

        if (ast instanceof Sequence seq && seq.left() instanceof Parallel) {
            // (S1 || S2) . S3 — extract from the parallel part
            return extractVoicesFromAst(seq.left(), voiceNames);
        }

        // Single voice
        List<MusicalNote> notes = extractNotesFromAst(ast);
        String name = !names.isEmpty() ? names.getFirst() : "Melody";
        return List.of(new MusicalPath(notes, name));
    }

    // -----------------------------------------------------------------------
    // Clef guessing
    // -----------------------------------------------------------------------

    private static String guessClef(MusicalPath path) {
        double avgOctave = path.notes().stream()
                .filter(n -> !n.isRest())
                .mapToInt(MusicalNote::octave)
                .average()
                .orElse(4.0);
        return avgOctave < 4 ? "bass" : "treble";
    }

    // -----------------------------------------------------------------------
    // LilyPond generation
    // -----------------------------------------------------------------------

    /**
     * Generate a complete LilyPond source file from voice paths.
     */
    public static String generateLilypond(List<MusicalPath> voices,
                                           String title, String composer,
                                           String key, String timeSig) {
        StringBuilder sb = new StringBuilder();
        sb.append("\\version \"2.24.0\"\n\n");

        if (!title.isEmpty() || !composer.isEmpty()) {
            sb.append("\\header {\n");
            if (!title.isEmpty()) sb.append("  title = \"").append(title).append("\"\n");
            if (!composer.isEmpty()) sb.append("  composer = \"").append(composer).append("\"\n");
            sb.append("}\n\n");
        }

        if (voices.isEmpty()) {
            sb.append("{ s1 }\n");
            return sb.toString();
        }

        if (voices.size() == 1) {
            MusicalPath v = voices.getFirst();
            String clef = guessClef(v);
            String noteStr = notesToLilypond(v.notes());
            sb.append("{\n");
            sb.append("  \\clef ").append(clef).append("\n");
            sb.append("  \\key ").append(key).append("\n");
            sb.append("  \\time ").append(timeSig).append("\n");
            sb.append("  ").append(noteStr).append("\n");
            sb.append("}\n");
        } else {
            sb.append("\\new StaffGroup <<\n");
            for (MusicalPath v : voices) {
                String clef = guessClef(v);
                String noteStr = notesToLilypond(v.notes());
                sb.append("  \\new Staff \\with { instrumentName = \"")
                  .append(v.voiceName()).append("\" } {\n");
                sb.append("    \\clef ").append(clef).append("\n");
                sb.append("    \\key ").append(key).append("\n");
                sb.append("    \\time ").append(timeSig).append("\n");
                sb.append("    ").append(noteStr).append("\n");
                sb.append("  }\n");
            }
            sb.append(">>\n");
        }

        return sb.toString();
    }

    private static String notesToLilypond(List<MusicalNote> notes) {
        if (notes.isEmpty()) return "s1";
        StringJoiner sj = new StringJoiner(" ");
        for (MusicalNote n : notes) {
            sj.add(noteToLilypond(n));
        }
        return sj.toString();
    }

    // -----------------------------------------------------------------------
    // Musical form analysis
    // -----------------------------------------------------------------------

    /**
     * Analyze the musical form implied by a session type AST.
     */
    public static MusicalForm analyzeMusicalForm(SessionType ast) {
        if (ast instanceof Parallel par) {
            int voices = flattenParallel(par).size();
            if (voices == 2) return MusicalForm.INVENTION;
            if (voices == 3) return MusicalForm.FUGUE;
            if (voices >= 4) return MusicalForm.CHORALE;
            return MusicalForm.POLYPHONIC;
        }
        if (ast instanceof Sequence seq && seq.left() instanceof Parallel par) {
            int voices = flattenParallel(par).size();
            if (voices == 2) return MusicalForm.INVENTION;
            if (voices == 3) return MusicalForm.FUGUE;
            if (voices >= 4) return MusicalForm.CHORALE;
            return MusicalForm.POLYPHONIC;
        }
        if (ast instanceof Rec) return MusicalForm.RONDO;
        if (ast instanceof Branch b && b.choices().size() > 1) {
            return MusicalForm.THEME_AND_VARIATIONS;
        }
        return MusicalForm.MONOPHONIC;
    }

    // -----------------------------------------------------------------------
    // Top-level API
    // -----------------------------------------------------------------------

    /**
     * Convert a session type string to a LilyPond music score.
     *
     * @param typeStr    session type string with musical labels
     * @param title      piece title
     * @param composer   composer name
     * @param voiceNames names for voices in parallel types
     * @param key        key signature (e.g. "c \\major")
     * @param timeSig    time signature (e.g. "4/4")
     * @return MusicResult with voices and LilyPond source
     */
    public static MusicResult sessionToScore(String typeStr, String title,
                                              String composer,
                                              List<String> voiceNames,
                                              String key, String timeSig) {
        SessionType ast = Parser.parse(typeStr);
        List<MusicalPath> voices = extractVoicesFromAst(ast, voiceNames);
        boolean isPoly = voices.size() > 1;
        MusicalForm form = analyzeMusicalForm(ast);

        String ly = generateLilypond(voices, title, composer, key, timeSig);
        List<String> warnings = new ArrayList<>();

        // Check for non-musical labels
        for (MusicalPath v : voices) {
            if (v.notes().isEmpty()) {
                warnings.add("Voice '" + v.voiceName() + "' has no musical notes");
            }
        }

        return new MusicResult(voices, ly, isPoly, form, warnings);
    }

    /**
     * Convert a session type string to a LilyPond music score with default settings.
     */
    public static MusicResult sessionToScore(String typeStr) {
        return sessionToScore(typeStr, "", "", null, "c \\major", "4/4");
    }

    // -----------------------------------------------------------------------
    // Pre-composed pieces as session types
    // -----------------------------------------------------------------------

    /** Bach Invention No. 1 in C Major (simplified opening, 2 voices). */
    public static final String BACH_INVENTION_1 =
            "(&{C5e: &{D5e: &{E5e: &{F5e: &{D5e: &{E5e: &{C5e: &{G5e: end}}}}}}}}" +
            " || " +
            "&{C4e: &{D4e: &{E4e: &{F4e: &{D4e: &{E4e: &{C4e: &{G4e: end}}}}}}}})";

    /** Simple C major scale (single voice). */
    public static final String C_MAJOR_SCALE =
            "&{C4q: &{D4q: &{E4q: &{F4q: &{G4q: &{A4q: &{B4q: &{C5q: end}}}}}}}}";

    /** Chorale-style cadence (2 voices). */
    public static final String CADENCE_IV_V_I =
            "(&{F4h: &{G4h: &{C5w: end}}}" +
            " || " +
            "&{A3h: &{B3h: &{C4w: end}}})";

    /** Canon — same melody at different octaves. */
    public static final String CANON_SIMPLE =
            "(&{C5q: &{E5q: &{G5q: &{C6h: end}}}}" +
            " || " +
            "&{C4q: &{E4q: &{G4q: &{C5h: end}}}})";

    /** Minuet pattern (3/4 time). */
    public static final String MINUET_PATTERN =
            "(&{G4q: &{A4q: &{B4q: &{C5q: &{D5q: &{E5q: end}}}}}}" +
            " || " +
            "&{G3h: &{E3q: &{C3h: &{G3q: end}}}})";
}
