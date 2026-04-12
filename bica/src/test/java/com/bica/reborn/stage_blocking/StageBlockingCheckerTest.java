package com.bica.reborn.stage_blocking;

import com.bica.reborn.globaltype.GEnd;
import com.bica.reborn.globaltype.GMessage;
import com.bica.reborn.globaltype.GlobalType;
import com.bica.reborn.stage_blocking.StageBlockingChecker.Beat;
import com.bica.reborn.stage_blocking.StageBlockingChecker.BlockingMorphism;
import com.bica.reborn.stage_blocking.StageBlockingChecker.BlockingSnapshot;
import com.bica.reborn.stage_blocking.StageBlockingChecker.CueKind;
import com.bica.reborn.stage_blocking.StageBlockingChecker.CueSheetDiff;
import com.bica.reborn.stage_blocking.StageBlockingChecker.Position;
import com.bica.reborn.stage_blocking.StageBlockingChecker.Scene;
import com.bica.reborn.stage_blocking.StageBlockingChecker.SceneWellFormedness;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.bica.reborn.stage_blocking.StageBlockingChecker.STAGE_MANAGER;
import static org.junit.jupiter.api.Assertions.*;

/** Tests for {@link StageBlockingChecker} (Step 58 -- Stage Blocking as MPST). */
class StageBlockingCheckerTest {

    // -----------------------------------------------------------------------
    // Position / Beat / Scene construction
    // -----------------------------------------------------------------------

    @Test
    void positionRejectsUnknownCode() {
        assertThrows(IllegalArgumentException.class, () -> new Position("nowhere"));
    }

    @Test
    void positionAcceptsAllCodes() {
        for (String c : List.of("DSL", "DSC", "DSR", "USL", "USC", "USR", "CS", "OFF")) {
            assertEquals(c, new Position(c).code());
        }
    }

    @Test
    void sceneRequiresActors() {
        assertThrows(IllegalArgumentException.class,
                () -> new Scene("empty", List.of(), List.of()));
    }

    @Test
    void sceneRejectsReservedSmName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Scene("bad", List.of("SM"), List.of()));
    }

    @Test
    void sceneRejectsUnknownSender() {
        assertThrows(IllegalArgumentException.class,
                () -> new Scene("bad", List.of("A"),
                        List.of(new Beat("Z", "A", "x"))));
    }

    @Test
    void sceneRejectsUnknownReceiver() {
        assertThrows(IllegalArgumentException.class,
                () -> new Scene("bad", List.of("A"),
                        List.of(new Beat("A", "Z", "x"))));
    }

    @Test
    void sceneRejectsSelfLoopBeat() {
        assertThrows(IllegalArgumentException.class,
                () -> new Scene("bad", List.of("A"),
                        List.of(new Beat("A", "A", "x"))));
    }

    // -----------------------------------------------------------------------
    // Encoding
    // -----------------------------------------------------------------------

    @Test
    void encodeEmptySceneIsGEnd() {
        Scene s = new Scene("silent", List.of("A"), List.of());
        assertInstanceOf(GEnd.class, StageBlockingChecker.encodeScene(s));
    }

    @Test
    void encodeSingleBeatIsSingleMessage() {
        Scene s = new Scene("one", List.of("A"),
                List.of(new Beat(STAGE_MANAGER, "A", "cue1")));
        GlobalType g = StageBlockingChecker.encodeScene(s);
        assertInstanceOf(GMessage.class, g);
        GMessage gm = (GMessage) g;
        assertEquals(STAGE_MANAGER, gm.sender());
        assertEquals("A", gm.receiver());
        assertEquals("cue1", gm.choices().get(0).label());
        assertInstanceOf(GEnd.class, gm.choices().get(0).body());
    }

    @Test
    void encodeSequenceOrderPreserved() {
        Scene s = new Scene("two", List.of("A", "B"), List.of(
                new Beat(STAGE_MANAGER, "A", "c1"),
                new Beat(STAGE_MANAGER, "B", "c2")));
        GMessage g = (GMessage) StageBlockingChecker.encodeScene(s);
        assertEquals("c1", g.choices().get(0).label());
        GlobalType inner = g.choices().get(0).body();
        assertInstanceOf(GMessage.class, inner);
        assertEquals("c2", ((GMessage) inner).choices().get(0).label());
    }

    // -----------------------------------------------------------------------
    // Well-formedness / lattice
    // -----------------------------------------------------------------------

    @Test
    void macbethSceneShape() {
        Scene s = StageBlockingChecker.macbethWitchesScene();
        assertEquals("Macbeth I.i", s.title());
        assertEquals(List.of("W1", "W2", "W3"), s.actors());
        assertEquals(10, s.beats().size());
    }

    @Test
    void macbethWellFormed() {
        SceneWellFormedness wf = StageBlockingChecker.checkScene(
                StageBlockingChecker.macbethWitchesScene());
        assertTrue(wf.isWellFormed());
        assertTrue(wf.errors().isEmpty());
        assertTrue(wf.roles().contains(STAGE_MANAGER));
        assertTrue(wf.roles().contains("W1"));
        assertTrue(wf.globalSize() >= 2);
    }

    @Test
    void macbethGlobalIsLattice() {
        SceneWellFormedness wf = StageBlockingChecker.checkScene(
                StageBlockingChecker.macbethWitchesScene());
        assertTrue(wf.globalIsLattice());
    }

    @Test
    void macbethProjectsOntoEveryActor() {
        SceneWellFormedness wf = StageBlockingChecker.checkScene(
                StageBlockingChecker.macbethWitchesScene());
        for (String r : List.of("SM", "W1", "W2", "W3")) {
            assertTrue(wf.localTypes().containsKey(r));
            assertNotNull(wf.localTypes().get(r));
            assertFalse(wf.localTypes().get(r).isEmpty());
        }
    }

    @Test
    void emptySceneLattice() {
        Scene s = new Scene("silent", List.of("A"), List.of());
        SceneWellFormedness wf = StageBlockingChecker.checkScene(s);
        assertTrue(wf.isWellFormed());
        assertTrue(wf.globalIsLattice());
    }

    @Test
    void sequentialSceneIsLattice() {
        Scene s = new Scene("seq", List.of("A", "B"), List.of(
                new Beat(STAGE_MANAGER, "A", "c1"),
                new Beat(STAGE_MANAGER, "B", "c2"),
                new Beat("A", STAGE_MANAGER, "ack1")));
        SceneWellFormedness wf = StageBlockingChecker.checkScene(s);
        assertTrue(wf.isWellFormed());
        assertTrue(wf.globalIsLattice());
    }

    // -----------------------------------------------------------------------
    // Bidirectional morphism
    // -----------------------------------------------------------------------

    @Test
    void buildMorphismOnMacbeth() {
        BlockingMorphism m = StageBlockingChecker.buildMorphism(
                StageBlockingChecker.macbethWitchesScene());
        assertNotNull(m);
        assertEquals(m.stateSpace().states().size(), m.phi().size());
        assertTrue(m.psi().size() >= 1);
    }

    @Test
    void morphismInitialSnapshotAllOff() {
        BlockingMorphism m = StageBlockingChecker.buildMorphism(
                StageBlockingChecker.macbethWitchesScene());
        BlockingSnapshot init = m.phi().get(m.stateSpace().top());
        assertTrue(init.firedCues().isEmpty());
        for (Map.Entry<String, String> p : init.positions()) {
            assertEquals("OFF", p.getValue());
        }
    }

    @Test
    void morphismRoundTrip() {
        BlockingMorphism m = StageBlockingChecker.buildMorphism(
                StageBlockingChecker.macbethWitchesScene());
        assertTrue(StageBlockingChecker.roundTripBlocking(m));
    }

    @Test
    void classifyStateReturnsSnapshot() {
        BlockingMorphism m = StageBlockingChecker.buildMorphism(
                StageBlockingChecker.macbethWitchesScene());
        int any = m.stateSpace().states().iterator().next();
        BlockingSnapshot sn = StageBlockingChecker.classifyState(m, any);
        assertNotNull(sn);
    }

    @Test
    void snapshotsAccumulateCues() {
        BlockingMorphism m = StageBlockingChecker.buildMorphism(
                StageBlockingChecker.macbethWitchesScene());
        List<Integer> sizes = new ArrayList<>();
        for (BlockingSnapshot sn : m.phi().values()) {
            int n = sn.firedCues().size();
            if (!sizes.contains(n)) sizes.add(n);
        }
        Collections.sort(sizes);
        assertEquals(0, sizes.get(0));
        assertTrue(sizes.get(sizes.size() - 1) >= 1);
    }

    @Test
    void actorsOnStageReflectsEntrances() {
        Scene s = new Scene("one-entry", List.of("A"),
                List.of(new Beat(STAGE_MANAGER, "A", "enter",
                        CueKind.ENTRANCE, new Position("DSC"))));
        BlockingMorphism m = StageBlockingChecker.buildMorphism(s);
        boolean any = false;
        for (BlockingSnapshot sn : m.phi().values()) {
            if (sn.actorsOnStage().contains("A")) {
                any = true;
                break;
            }
        }
        assertTrue(any);
    }

    @Test
    void exitPutsActorOff() {
        Scene s = new Scene("in-out", List.of("A"), List.of(
                new Beat(STAGE_MANAGER, "A", "enter",
                        CueKind.ENTRANCE, new Position("DSC")),
                new Beat(STAGE_MANAGER, "A", "exit", CueKind.EXIT)));
        BlockingMorphism m = StageBlockingChecker.buildMorphism(s);
        BlockingSnapshot fullest = null;
        for (BlockingSnapshot sn : m.phi().values()) {
            if (fullest == null || sn.firedCues().size() > fullest.firedCues().size()) {
                fullest = sn;
            }
        }
        assertNotNull(fullest);
        String pos = null;
        for (Map.Entry<String, String> e : fullest.positions()) {
            if (e.getKey().equals("A")) pos = e.getValue();
        }
        assertEquals("OFF", pos);
    }

    // -----------------------------------------------------------------------
    // Collision detection
    // -----------------------------------------------------------------------

    @Test
    void noCollisionsInMacbeth() {
        assertTrue(StageBlockingChecker.detectCollisions(
                StageBlockingChecker.macbethWitchesScene()).isEmpty());
    }

    @Test
    void collisionDetectedWhenTwoActorsSamePosition() {
        Scene s = new Scene("collide", List.of("A", "B"), List.of(
                new Beat(STAGE_MANAGER, "A", "eA",
                        CueKind.ENTRANCE, new Position("DSC")),
                new Beat(STAGE_MANAGER, "B", "eB",
                        CueKind.ENTRANCE, new Position("DSC"))));
        List<String> errors = StageBlockingChecker.detectCollisions(s);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("DSC"));
        assertTrue(errors.get(0).contains("B"));
    }

    @Test
    void noCollisionIfFirstActorExitsFirst() {
        Scene s = new Scene("sequential", List.of("A", "B"), List.of(
                new Beat(STAGE_MANAGER, "A", "eA",
                        CueKind.ENTRANCE, new Position("DSC")),
                new Beat(STAGE_MANAGER, "A", "xA", CueKind.EXIT),
                new Beat(STAGE_MANAGER, "B", "eB",
                        CueKind.ENTRANCE, new Position("DSC"))));
        assertTrue(StageBlockingChecker.detectCollisions(s).isEmpty());
    }

    // -----------------------------------------------------------------------
    // Cue sheet verification
    // -----------------------------------------------------------------------

    @Test
    void cueSheetExactMatch() {
        Scene s = new Scene("cues", List.of("A"), List.of(
                new Beat(STAGE_MANAGER, "A", "thunder", CueKind.SOUND),
                new Beat(STAGE_MANAGER, "A", "lights_up", CueKind.LIGHT)));
        CueSheetDiff d = StageBlockingChecker.verifyCueSheet(
                s, List.of("thunder", "lights_up"));
        assertTrue(d.missing().isEmpty());
        assertTrue(d.extra().isEmpty());
    }

    @Test
    void cueSheetDetectsMissingCue() {
        Scene s = new Scene("cues", List.of("A"),
                List.of(new Beat(STAGE_MANAGER, "A", "thunder", CueKind.SOUND)));
        CueSheetDiff d = StageBlockingChecker.verifyCueSheet(
                s, List.of("thunder", "lights_up"));
        assertEquals(List.of("lights_up"), d.missing());
        assertTrue(d.extra().isEmpty());
    }

    @Test
    void cueSheetDetectsExtraCue() {
        Scene s = new Scene("cues", List.of("A"), List.of(
                new Beat(STAGE_MANAGER, "A", "thunder", CueKind.SOUND),
                new Beat(STAGE_MANAGER, "A", "lights_up", CueKind.LIGHT)));
        CueSheetDiff d = StageBlockingChecker.verifyCueSheet(s, List.of("thunder"));
        assertTrue(d.missing().isEmpty());
        assertEquals(List.of("lights_up"), d.extra());
    }

    @Test
    void cueSheetIgnoresActorToSmLines() {
        Scene s = new Scene("lines", List.of("A"), List.of(
                new Beat(STAGE_MANAGER, "A", "thunder", CueKind.SOUND),
                new Beat("A", STAGE_MANAGER, "speech", CueKind.LINE)));
        CueSheetDiff d = StageBlockingChecker.verifyCueSheet(s, List.of("thunder"));
        assertTrue(d.missing().isEmpty());
        assertTrue(d.extra().isEmpty());
    }
}
