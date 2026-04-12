package com.bica.reborn.fugues;

import com.bica.reborn.ast.SessionType;
import com.bica.reborn.globaltype.GEnd;
import com.bica.reborn.globaltype.GMessage;
import com.bica.reborn.globaltype.GlobalType;
import com.bica.reborn.globaltype.GlobalTypeChecker;
import com.bica.reborn.globaltype.Projection;
import com.bica.reborn.globaltype.ProjectionError;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.PrettyPrinter;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpace.Transition;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Bach fugues as multiparty session types (Step 56) -- Java port of
 * {@code reticulate/reticulate/fugues.py}.
 *
 * <p>A fugue is encoded as a sequential multiparty global type: each
 * subject entry is a unary-choice role-to-role message; episodes and
 * the final entry extend the chain. Projection then yields per-voice
 * local session types; the global state space is checked for lattice
 * structure; and a bidirectional morphism pair (phi, psi) maps lattice
 * states to sectional labels (exposition / episode / final-entry /
 * cadence).
 */
public final class FuguesChecker {

    private FuguesChecker() {}

    // -----------------------------------------------------------------------
    // Sections
    // -----------------------------------------------------------------------

    /** Coarse sectional label for a state in the fugue lattice. */
    public enum FugueSection {
        EXPOSITION,
        EPISODE,
        STRETTO,
        FINAL_ENTRY,
        CADENCE
    }

    // -----------------------------------------------------------------------
    // Structural records
    // -----------------------------------------------------------------------

    /**
     * A single subject entry: {@code voice} enters after being cued by
     * {@code cue}. {@code cue} is {@code null} for the very first entry.
     */
    public record SubjectEntry(String voice, String cue, String label) {
        public SubjectEntry {
            Objects.requireNonNull(voice, "voice must not be null");
            Objects.requireNonNull(label, "label must not be null");
        }

        public static SubjectEntry of(String voice) {
            return new SubjectEntry(voice, null, "enter");
        }

        public static SubjectEntry of(String voice, String cue) {
            return new SubjectEntry(voice, cue, "enter");
        }

        public static SubjectEntry of(String voice, String cue, String label) {
            return new SubjectEntry(voice, cue, label);
        }
    }

    /** A fugue as an ordered list of subject entries plus episodes. */
    public record FugueStructure(
            List<String> voices,
            List<SubjectEntry> exposition,
            int episodes,
            SubjectEntry finalEntry) {

        public FugueStructure {
            Objects.requireNonNull(voices, "voices must not be null");
            Objects.requireNonNull(exposition, "exposition must not be null");
            if (voices.isEmpty()) {
                throw new IllegalArgumentException("fugue must have at least one voice");
            }
            Set<String> vs = new HashSet<>(voices);
            for (SubjectEntry e : exposition) {
                if (!vs.contains(e.voice())) {
                    throw new IllegalArgumentException(
                            "unknown voice in entry: " + e.voice());
                }
            }
            if (finalEntry != null && !vs.contains(finalEntry.voice())) {
                throw new IllegalArgumentException("unknown voice in final entry");
            }
            voices = List.copyOf(voices);
            exposition = List.copyOf(exposition);
        }

        public FugueStructure(
                List<String> voices,
                List<SubjectEntry> exposition) {
            this(voices, exposition, 0, null);
        }
    }

    /** Result of checking a fugue's well-formedness. */
    public record FugueWellFormedness(
            boolean isWellFormed,
            boolean globalIsLattice,
            List<String> voices,
            Map<String, String> localTypes,
            int globalSize,
            List<String> errors) {

        public FugueWellFormedness {
            Objects.requireNonNull(voices, "voices must not be null");
            Objects.requireNonNull(localTypes, "localTypes must not be null");
            Objects.requireNonNull(errors, "errors must not be null");
            voices = List.copyOf(voices);
            localTypes = Map.copyOf(localTypes);
            errors = List.copyOf(errors);
        }
    }

    /** Bidirectional morphism pair for a fugue lattice. */
    public record FugueMorphism(
            Map<Integer, FugueSection> phi,
            Map<FugueSection, Integer> psi,
            StateSpace stateSpace) {

        public FugueMorphism {
            Objects.requireNonNull(phi, "phi must not be null");
            Objects.requireNonNull(psi, "psi must not be null");
            Objects.requireNonNull(stateSpace, "stateSpace must not be null");
            phi = Map.copyOf(phi);
            psi = Map.copyOf(psi);
        }
    }

    // -----------------------------------------------------------------------
    // Encoding: FugueStructure -> GlobalType
    // -----------------------------------------------------------------------

    private record Triple(String sender, String receiver, String label) {}

    private static List<Triple> pairEach(
            List<SubjectEntry> entries, List<String> voices) {
        List<Triple> triples = new ArrayList<>();
        String prev = null;
        for (SubjectEntry e : entries) {
            String sender;
            if (e.cue() != null) {
                sender = e.cue();
            } else if (prev != null) {
                sender = prev;
            } else {
                String other = firstOther(voices, e.voice());
                sender = other != null ? other : e.voice();
            }
            if (sender.equals(e.voice())) {
                String other = firstOther(voices, e.voice());
                if (other != null) sender = other;
            }
            triples.add(new Triple(sender, e.voice(), e.label()));
            prev = e.voice();
        }
        return triples;
    }

    private static String firstOther(List<String> voices, String exclude) {
        for (String v : voices) {
            if (!v.equals(exclude)) return v;
        }
        return null;
    }

    /** Build a {@link GlobalType} from a {@link FugueStructure}. */
    public static GlobalType encodeFugue(FugueStructure fugue) {
        List<String> voices = fugue.voices();
        List<Triple> triples = new ArrayList<>(pairEach(fugue.exposition(), voices));

        int leaderIdx = fugue.exposition().isEmpty()
                ? 0
                : voices.indexOf(fugue.exposition().get(fugue.exposition().size() - 1).voice());
        int n = voices.size();
        for (int i = 0; i < fugue.episodes(); i++) {
            String s = voices.get(((leaderIdx % n) + n) % n);
            String r = voices.get((((leaderIdx + 1) % n) + n) % n);
            if (s.equals(r)) {
                r = voices.get((((leaderIdx + 2) % n) + n) % n);
            }
            triples.add(new Triple(s, r, "episode"));
            leaderIdx++;
        }

        if (fugue.finalEntry() != null) {
            SubjectEntry fe = fugue.finalEntry();
            String sender;
            if (fe.cue() != null) {
                sender = fe.cue();
            } else {
                sender = voices.get(((leaderIdx % n) + n) % n);
            }
            if (sender.equals(fe.voice())) {
                String other = firstOther(voices, fe.voice());
                if (other != null) sender = other;
            }
            triples.add(new Triple(sender, fe.voice(), fe.label()));
        }

        GlobalType g = new GEnd();
        for (int i = triples.size() - 1; i >= 0; i--) {
            Triple t = triples.get(i);
            g = new GMessage(
                    t.sender(),
                    t.receiver(),
                    List.of(new GMessage.Choice(t.label(), g)));
        }
        return g;
    }

    // -----------------------------------------------------------------------
    // Well-formedness
    // -----------------------------------------------------------------------

    /** Check well-formedness of a fugue: project, size, lattice. */
    public static FugueWellFormedness checkFugue(FugueStructure fugue) {
        GlobalType g = encodeFugue(fugue);
        List<String> errors = new ArrayList<>();
        Map<String, String> localTypes = new LinkedHashMap<>();

        for (String v : fugue.voices()) {
            try {
                SessionType lt = Projection.project(g, v);
                localTypes.put(v, PrettyPrinter.pretty(lt));
            } catch (ProjectionError exc) {
                errors.add("projection onto " + v + " failed: " + exc.getMessage());
            }
        }

        StateSpace gss;
        int globalSize;
        try {
            gss = GlobalTypeChecker.buildStateSpace(g);
            globalSize = gss.states().size();
        } catch (RuntimeException exc) {
            errors.add("global state space build failed: " + exc.getMessage());
            gss = null;
            globalSize = 0;
        }

        boolean isWf = errors.isEmpty();
        boolean globalIsLattice = false;
        if (isWf && gss != null) {
            LatticeResult lr = LatticeChecker.checkLattice(gss);
            globalIsLattice = lr.isLattice();
        }

        return new FugueWellFormedness(
                isWf,
                globalIsLattice,
                fugue.voices(),
                localTypes,
                globalSize,
                errors);
    }

    // -----------------------------------------------------------------------
    // Bidirectional morphism pair (phi, psi)
    // -----------------------------------------------------------------------

    /** Compute phi and psi for a fugue. */
    public static FugueMorphism buildMorphism(FugueStructure fugue) {
        GlobalType g = encodeFugue(fugue);
        StateSpace ss = GlobalTypeChecker.buildStateSpace(g);

        int nExpo = fugue.exposition().size();
        int nEpi = fugue.episodes();
        boolean hasFinal = fugue.finalEntry() != null;

        // BFS order from the initial (top) state.
        Map<Integer, Integer> order = new LinkedHashMap<>();
        Set<Integer> seen = new HashSet<>();
        List<Integer> frontier = new ArrayList<>();
        frontier.add(ss.top());
        seen.add(ss.top());
        int idx = 0;
        while (!frontier.isEmpty()) {
            List<Integer> next = new ArrayList<>();
            for (int s : frontier) {
                order.put(s, idx++);
                for (Transition t : ss.transitions()) {
                    if (t.source() == s && seen.add(t.target())) {
                        next.add(t.target());
                    }
                }
            }
            frontier = next;
        }
        // Append any unreachable states.
        for (int s : ss.states()) {
            if (!order.containsKey(s)) {
                order.put(s, idx++);
            }
        }

        Map<Integer, FugueSection> phi = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> e : order.entrySet()) {
            int s = e.getKey();
            int k = e.getValue();
            if (k < nExpo) {
                phi.put(s, FugueSection.EXPOSITION);
            } else if (k < nExpo + nEpi) {
                phi.put(s, FugueSection.EPISODE);
            } else if (hasFinal && k == nExpo + nEpi) {
                phi.put(s, FugueSection.FINAL_ENTRY);
            } else {
                phi.put(s, FugueSection.CADENCE);
            }
        }

        // First state per section in BFS order is the representative.
        List<Map.Entry<Integer, Integer>> ordered = new ArrayList<>(order.entrySet());
        ordered.sort(Map.Entry.comparingByValue());
        Map<FugueSection, Integer> psi = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> e : ordered) {
            FugueSection sec = phi.get(e.getKey());
            psi.putIfAbsent(sec, e.getKey());
        }

        return new FugueMorphism(phi, psi, ss);
    }

    /** Return the fugue section for {@code state} (phi lookup). */
    public static FugueSection classifyState(FugueMorphism morphism, int state) {
        FugueSection sec = morphism.phi().get(state);
        if (sec == null) {
            throw new IllegalArgumentException("unknown state: " + state);
        }
        return sec;
    }

    /** Return psi(section): the representative state for the section. */
    public static int sectionRepresentative(FugueMorphism morphism, FugueSection section) {
        Integer rep = morphism.psi().get(section);
        if (rep == null) {
            throw new IllegalArgumentException("section not present in fugue: " + section);
        }
        return rep;
    }

    /** Verify phi(psi(sec)) == sec for every section present. */
    public static boolean roundTripSections(FugueMorphism morphism) {
        for (Map.Entry<FugueSection, Integer> e : morphism.psi().entrySet()) {
            if (morphism.phi().get(e.getValue()) != e.getKey()) {
                return false;
            }
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Stretto
    // -----------------------------------------------------------------------

    /** Rough structural test for stretto (overlap of entries). */
    public static boolean hasStretto(FugueStructure fugue) {
        Set<String> seen = new LinkedHashSet<>();
        for (SubjectEntry e : fugue.exposition()) {
            if (seen.contains(e.voice()) && seen.size() < fugue.voices().size()) {
                return true;
            }
            seen.add(e.voice());
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Running example: BWV 846
    // -----------------------------------------------------------------------

    /**
     * J. S. Bach, Well-Tempered Clavier Book I, Fugue No. 1 in C major
     * (BWV 846). 4-voice fugue with exposition A -> S -> T -> B, two
     * episodes, and a final entry in the bass.
     */
    public static FugueStructure bwv846Fugue() {
        List<String> voices = List.of("S", "A", "T", "B");
        List<SubjectEntry> expo = List.of(
                new SubjectEntry("A", null, "subject"),
                new SubjectEntry("S", "A", "answer"),
                new SubjectEntry("T", "S", "subject"),
                new SubjectEntry("B", "T", "answer"));
        SubjectEntry finalEntry = new SubjectEntry("B", "S", "final");
        return new FugueStructure(voices, expo, 2, finalEntry);
    }
}
