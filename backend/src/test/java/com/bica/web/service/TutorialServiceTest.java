package com.bica.web.service;

import com.bica.web.dto.TutorialDto;
import com.bica.web.dto.TutorialSummaryDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TutorialServiceTest {

    private final TutorialService service = new TutorialService();

    @Test
    void getTutorialsReturns11() {
        List<TutorialSummaryDto> tutorials = service.getTutorials();
        assertEquals(11, tutorials.size());
    }

    @Test
    void tutorialsAreNumberedSequentially() {
        List<TutorialSummaryDto> tutorials = service.getTutorials();
        for (int i = 0; i < tutorials.size(); i++) {
            assertEquals(i + 1, tutorials.get(i).number());
        }
    }

    @Test
    void allTutorialsHaveNonBlankFields() {
        for (var summary : service.getTutorials()) {
            assertFalse(summary.id().isBlank(), "id should not be blank");
            assertFalse(summary.title().isBlank(), "title should not be blank");
            assertFalse(summary.subtitle().isBlank(), "subtitle should not be blank");
        }
    }

    @Test
    void getTutorialByValidId() {
        TutorialDto tutorial = service.getTutorial("quick-start");
        assertNotNull(tutorial);
        assertEquals(1, tutorial.number());
        assertEquals("Quick Start", tutorial.title());
        assertFalse(tutorial.steps().isEmpty());
    }

    @Test
    void getTutorialByInvalidIdReturnsNull() {
        assertNull(service.getTutorial("nonexistent"));
    }

    @Test
    void allTutorialsHaveSteps() {
        for (var summary : service.getTutorials()) {
            TutorialDto full = service.getTutorial(summary.id());
            assertNotNull(full, "Tutorial " + summary.id() + " should exist");
            assertFalse(full.steps().isEmpty(),
                    "Tutorial " + summary.id() + " should have steps");
        }
    }

    @Test
    void allStepsHaveNonBlankTitleAndProse() {
        for (var summary : service.getTutorials()) {
            TutorialDto full = service.getTutorial(summary.id());
            for (var step : full.steps()) {
                assertFalse(step.title().isBlank(),
                        "Step title should not be blank in " + summary.id());
                assertFalse(step.prose().isBlank(),
                        "Step prose should not be blank in " + summary.id());
            }
        }
    }

    @Test
    void quickStartHasSevenSteps() {
        TutorialDto qs = service.getTutorial("quick-start");
        assertEquals(7, qs.steps().size());
    }

    @Test
    void hasseDiagramsIsLastTutorial() {
        TutorialDto hasse = service.getTutorial("hasse-diagrams");
        assertNotNull(hasse);
        assertEquals(11, hasse.number());
    }

    @Test
    void knownTutorialIdsExist() {
        var ids = List.of("quick-start", "first-session-type", "branching-selection",
                "recursive-protocols", "parallel-constructor", "lattice-properties",
                "morphisms", "modeling-real-protocols", "test-generation",
                "bica-annotations", "hasse-diagrams");
        for (String id : ids) {
            assertNotNull(service.getTutorial(id), "Tutorial " + id + " should exist");
        }
    }
}
