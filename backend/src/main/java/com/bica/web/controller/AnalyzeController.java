package com.bica.web.controller;

import com.bica.web.dto.*;
import com.bica.web.service.AnalysisService;
import com.bica.web.service.TutorialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private final AnalysisService analysisService;
    private final TutorialService tutorialService;

    public AnalyzeController(AnalysisService analysisService, TutorialService tutorialService) {
        this.analysisService = analysisService;
        this.tutorialService = tutorialService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody AnalyzeRequest request) {
        if (request.typeString() == null || request.typeString().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "typeString is required"));
        }
        try {
            AnalyzeResponse response = analysisService.analyze(request.typeString().trim());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test-gen")
    public ResponseEntity<?> testGen(@RequestBody TestGenRequest request) {
        if (request.typeString() == null || request.typeString().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "typeString is required"));
        }
        if (request.className() == null || request.className().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "className is required"));
        }
        try {
            TestGenResponse response = analysisService.generateTests(
                    request.typeString().trim(),
                    request.className().trim(),
                    request.packageName(),
                    request.maxRevisits()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/coverage-storyboard")
    public ResponseEntity<?> coverageStoryboard(@RequestBody AnalyzeRequest request) {
        if (request.typeString() == null || request.typeString().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "typeString is required"));
        }
        try {
            CoverageStoryboardResponse response = analysisService.coverageStoryboard(
                    request.typeString().trim());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/analyze-global")
    public ResponseEntity<?> analyzeGlobal(@RequestBody GlobalAnalyzeRequest request) {
        if (request.typeString() == null || request.typeString().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "typeString is required"));
        }
        try {
            GlobalAnalyzeResponse response = analysisService.analyzeGlobal(request.typeString().trim());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/compare")
    public ResponseEntity<?> compare(@RequestBody CompareRequest request) {
        if (request.type1() == null || request.type1().isBlank()
                || request.type2() == null || request.type2().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Both type1 and type2 are required"));
        }
        try {
            CompareResponse response = analysisService.compareTypes(
                    request.type1().trim(), request.type2().trim());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/benchmarks")
    public List<BenchmarkDto> benchmarks() {
        return analysisService.getBenchmarks();
    }

    @GetMapping("/tutorials")
    public List<TutorialSummaryDto> tutorials() {
        return tutorialService.getTutorials();
    }

    @GetMapping("/tutorials/{id}")
    public ResponseEntity<?> tutorial(@PathVariable String id) {
        TutorialDto tutorial = tutorialService.getTutorial(id);
        if (tutorial == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tutorial);
    }
}
