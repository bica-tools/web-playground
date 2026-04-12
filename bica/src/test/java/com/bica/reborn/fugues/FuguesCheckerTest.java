package com.bica.reborn.fugues;

import com.bica.reborn.fugues.FuguesChecker.FugueMorphism;
import com.bica.reborn.fugues.FuguesChecker.FugueSection;
import com.bica.reborn.fugues.FuguesChecker.FugueStructure;
import com.bica.reborn.fugues.FuguesChecker.FugueWellFormedness;
import com.bica.reborn.fugues.FuguesChecker.SubjectEntry;
import com.bica.reborn.globaltype.GEnd;
import com.bica.reborn.globaltype.GMessage;
import com.bica.reborn.globaltype.GlobalType;
import com.bica.reborn.globaltype.GlobalTypeChecker;
import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.statespace.StateSpace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for {@link FuguesChecker} (Step 56 -- Bach Fugues as MPST). */
class FuguesCheckerTest {

    // -----------------------------------------------------------------------
    // FugueStructure construction
    // -----------------------------------------------------------------------

    @Test
    void fugueRequiresVoices() {
        assertThrows(IllegalArgumentException.class,
                () -> new FugueStructure(List.of(), List.of(), 0, null));
    }

    @Test
    void fugueRejectsUnknownVoice() {
        assertThrows(IllegalArgumentException.class,
                () -> new FugueStructure(
                        List.of("S", "A"),
                        List.of(SubjectEntry.of("Z"))));
    }

    @Test
    void fugueRejectsUnknownFinalVoice() {
        assertThrows(IllegalArgumentException.class,
                () -> new FugueStructure(
                        List.of("S", "A"),
                        List.of(SubjectEntry.of("S")),
                        0,
                        SubjectEntry.of("Q")));
    }

    @Test
    void bwv846FugueShape() {
        FugueStructure f = FuguesChecker.bwv846Fugue();
        assertEquals(List.of("S", "A", "T", "B"), f.voices());
        assertEquals(4, f.exposition().size());
        assertEquals(2, f.episodes());
        assertNotNull(f.finalEntry());
        assertEquals("B", f.finalEntry().voice());
    }

    // -----------------------------------------------------------------------
    // encodeFugue
    // -----------------------------------------------------------------------

    @Test
    void encodeFugueReturnsGlobalType() {
        GlobalType g = FuguesChecker.encodeFugue(FuguesChecker.bwv846Fugue());
        assertInstanceOf(GMessage.class, g);
    }

    @Test
    void encodeFugueNoSelfMessages() {
        GlobalType g = FuguesChecker.encodeFugue(FuguesChecker.bwv846Fugue());
        GlobalType node = g;
        while (node instanceof GMessage m) {
            assertNotEquals(m.sender(), m.receiver(),
                    "self-message " + m.sender() + "->" + m.receiver());
            assertEquals(1, m.choices().size());
            node = m.choices().get(0).body();
        }
        assertInstanceOf(GEnd.class, node);
    }

    @Test
    void encodeFugueMinimalTwoVoice() {
        FugueStructure f = new FugueStructure(
                List.of("X", "Y"),
                List.of(
                        SubjectEntry.of("X"),
                        SubjectEntry.of("Y", "X")));
        GlobalType g = FuguesChecker.encodeFugue(f);
        assertInstanceOf(GMessage.class, g);
        int count = 0;
        GlobalType node = g;
        while (node instanceof GMessage m) {
            count++;
            node = m.choices().get(0).body();
        }
        assertEquals(2, count);
        assertInstanceOf(GEnd.class, node);
    }

    @Test
    void encodeFugueWithEpisodes() {
        FugueStructure f = new FugueStructure(
                List.of("X", "Y"),
                List.of(SubjectEntry.of("X"), SubjectEntry.of("Y", "X")),
                3,
                null);
        GlobalType g = FuguesChecker.encodeFugue(f);
        int count = 0;
        GlobalType node = g;
        while (node instanceof GMessage m) {
            count++;
            node = m.choices().get(0).body();
        }
        assertEquals(5, count);
    }

    @Test
    void encodeFugueWithFinalEntry() {
        FugueStructure f = new FugueStructure(
                List.of("X", "Y"),
                List.of(SubjectEntry.of("X"), SubjectEntry.of("Y", "X")),
                1,
                SubjectEntry.of("X"));
        GlobalType g = FuguesChecker.encodeFugue(f);
        int count = 0;
        GlobalType node = g;
        while (node instanceof GMessage m) {
            count++;
            node = m.choices().get(0).body();
        }
        assertEquals(4, count);
    }

    // -----------------------------------------------------------------------
    // Well-formedness
    // -----------------------------------------------------------------------

    @Test
    void checkFugueBwv846Wellformed() {
        FugueWellFormedness r = FuguesChecker.checkFugue(FuguesChecker.bwv846Fugue());
        assertTrue(r.isWellFormed());
        assertTrue(r.errors().isEmpty());
        assertEquals(Set.of("S", "A", "T", "B"), r.localTypes().keySet());
        assertTrue(r.globalSize() > 0);
    }

    @Test
    void checkFugueBwv846IsLattice() {
        FugueWellFormedness r = FuguesChecker.checkFugue(FuguesChecker.bwv846Fugue());
        assertTrue(r.globalIsLattice());
    }

    @Test
    void checkFugueLocalTypesNonempty() {
        FugueWellFormedness r = FuguesChecker.checkFugue(FuguesChecker.bwv846Fugue());
        for (var e : r.localTypes().entrySet()) {
            assertFalse(e.getValue().isEmpty(),
                    "empty local type for " + e.getKey());
            assertTrue(e.getValue().contains("end"));
        }
    }

    @Test
    void checkFugueTwoVoiceMinimal() {
        FugueStructure f = new FugueStructure(
                List.of("X", "Y"),
                List.of(SubjectEntry.of("X"), SubjectEntry.of("Y", "X")));
        FugueWellFormedness r = FuguesChecker.checkFugue(f);
        assertTrue(r.isWellFormed());
        assertTrue(r.globalIsLattice());
    }

    // -----------------------------------------------------------------------
    // Morphism phi, psi
    // -----------------------------------------------------------------------

    @Test
    void buildMorphismTotal() {
        FugueMorphism m = FuguesChecker.buildMorphism(FuguesChecker.bwv846Fugue());
        assertEquals(m.stateSpace().states(), m.phi().keySet());
    }

    @Test
    void buildMorphismSectionsCovered() {
        FugueMorphism m = FuguesChecker.buildMorphism(FuguesChecker.bwv846Fugue());
        Set<FugueSection> covered = Set.copyOf(m.phi().values());
        assertTrue(covered.contains(FugueSection.EXPOSITION));
        assertTrue(covered.contains(FugueSection.EPISODE));
        assertTrue(covered.contains(FugueSection.FINAL_ENTRY));
    }

    @Test
    void buildMorphismRoundtrip() {
        FugueMorphism m = FuguesChecker.buildMorphism(FuguesChecker.bwv846Fugue());
        assertTrue(FuguesChecker.roundTripSections(m));
    }

    @Test
    void classifyStateAndRepresentative() {
        FugueMorphism m = FuguesChecker.buildMorphism(FuguesChecker.bwv846Fugue());
        for (FugueSection sec : List.of(
                FugueSection.EXPOSITION,
                FugueSection.EPISODE,
                FugueSection.FINAL_ENTRY)) {
            int rep = FuguesChecker.sectionRepresentative(m, sec);
            assertEquals(sec, FuguesChecker.classifyState(m, rep));
        }
    }

    @Test
    void sectionRepresentativeMissingRaises() {
        FugueStructure f = new FugueStructure(
                List.of("X", "Y"),
                List.of(SubjectEntry.of("X"), SubjectEntry.of("Y", "X")));
        FugueMorphism m = FuguesChecker.buildMorphism(f);
        assertThrows(IllegalArgumentException.class,
                () -> FuguesChecker.sectionRepresentative(m, FugueSection.STRETTO));
    }

    @Test
    void morphismPhiPartitionsStates() {
        FugueMorphism m = FuguesChecker.buildMorphism(FuguesChecker.bwv846Fugue());
        for (int s : m.stateSpace().states()) {
            assertTrue(m.phi().containsKey(s));
        }
    }

    // -----------------------------------------------------------------------
    // Stretto
    // -----------------------------------------------------------------------

    @Test
    void hasStrettoFalseForBwv846() {
        assertFalse(FuguesChecker.hasStretto(FuguesChecker.bwv846Fugue()));
    }

    @Test
    void hasStrettoTrueForOverlap() {
        FugueStructure f = new FugueStructure(
                List.of("S", "A", "T", "B"),
                List.of(
                        SubjectEntry.of("A"),
                        SubjectEntry.of("A", "A"),
                        SubjectEntry.of("S", "A"),
                        SubjectEntry.of("T", "S")));
        assertTrue(FuguesChecker.hasStretto(f));
    }

    // -----------------------------------------------------------------------
    // Projection consistency
    // -----------------------------------------------------------------------

    @Test
    void globalStateSpaceLatticeForBwv846() {
        GlobalType g = FuguesChecker.encodeFugue(FuguesChecker.bwv846Fugue());
        StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
        assertTrue(LatticeChecker.checkLattice(ss).isLattice());
    }

    @Test
    void globalStateSpaceSizeMatchesMessages() {
        FugueStructure f = FuguesChecker.bwv846Fugue();
        GlobalType g = FuguesChecker.encodeFugue(f);
        StateSpace ss = GlobalTypeChecker.buildStateSpace(g);
        // 4 exposition + 2 episodes + 1 final = 7 messages, 8 states
        assertEquals(8, ss.states().size());
    }
}
