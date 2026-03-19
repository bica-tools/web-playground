package com.bica.web.controller;

import com.bica.web.dto.*;
import com.bica.web.service.AnalysisService;
import com.bica.web.service.TutorialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeController.class);
    private static final int MAX_INPUT_LENGTH = 10_000;

    private final AnalysisService analysisService;
    private final TutorialService tutorialService;
    private final CacheManager cacheManager;
    private final RedisConnectionFactory redisConnectionFactory;

    public AnalyzeController(AnalysisService analysisService,
                             TutorialService tutorialService,
                             CacheManager cacheManager,
                             RedisConnectionFactory redisConnectionFactory) {
        this.analysisService = analysisService;
        this.tutorialService = tutorialService;
        this.cacheManager = cacheManager;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody AnalyzeRequest request) {
        var validation = validateTypeString(request.typeString());
        if (validation != null) return validation;
        try {
            AnalyzeResponse response = analysisService.analyze(request.typeString().trim());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Analysis failed for input: {}", truncate(request.typeString()), e);
            return serverError("Internal analysis error");
        }
    }

    @PostMapping("/test-gen")
    public ResponseEntity<?> testGen(@RequestBody TestGenRequest request) {
        var validation = validateTypeString(request.typeString());
        if (validation != null) return validation;
        if (request.className() == null || request.className().isBlank()) {
            return badRequest("className is required");
        }
        try {
            TestGenResponse response = analysisService.generateTests(
                    request.typeString().trim(),
                    request.className().trim(),
                    request.packageName(),
                    request.maxRevisits()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Test generation failed for input: {}", truncate(request.typeString()), e);
            return serverError("Internal error during test generation");
        }
    }

    @PostMapping("/coverage-storyboard")
    public ResponseEntity<?> coverageStoryboard(@RequestBody AnalyzeRequest request) {
        var validation = validateTypeString(request.typeString());
        if (validation != null) return validation;
        try {
            CoverageStoryboardResponse response = analysisService.coverageStoryboard(
                    request.typeString().trim());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Coverage storyboard failed for input: {}", truncate(request.typeString()), e);
            return serverError("Internal error during coverage analysis");
        }
    }

    @PostMapping("/analyze-global")
    public ResponseEntity<?> analyzeGlobal(@RequestBody GlobalAnalyzeRequest request) {
        var validation = validateTypeString(request.typeString());
        if (validation != null) return validation;
        try {
            GlobalAnalyzeResponse response = analysisService.analyzeGlobal(request.typeString().trim());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Global analysis failed for input: {}", truncate(request.typeString()), e);
            return serverError("Internal error during global type analysis");
        }
    }

    @PostMapping("/compare")
    public ResponseEntity<?> compare(@RequestBody CompareRequest request) {
        if (request.type1() == null || request.type1().isBlank()
                || request.type2() == null || request.type2().isBlank()) {
            return badRequest("Both type1 and type2 are required");
        }
        if (request.type1().length() > MAX_INPUT_LENGTH || request.type2().length() > MAX_INPUT_LENGTH) {
            return badRequest("Input too long (max " + MAX_INPUT_LENGTH + " characters)");
        }
        try {
            CompareResponse response = analysisService.compareTypes(
                    request.type1().trim(), request.type2().trim());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Comparison failed", e);
            return serverError("Internal error during type comparison");
        }
    }

    @PostMapping("/compose")
    public ResponseEntity<?> compose(@RequestBody CompositionRequest request) {
        if (request.participants() == null || request.participants().size() < 2) {
            return badRequest("At least 2 participants are required");
        }
        for (var p : request.participants()) {
            if (p.name() == null || p.name().isBlank()
                    || p.typeString() == null || p.typeString().isBlank()) {
                return badRequest("Each participant needs a name and typeString");
            }
            if (p.typeString().length() > MAX_INPUT_LENGTH) {
                return badRequest("Participant type too long (max " + MAX_INPUT_LENGTH + " characters)");
            }
        }
        try {
            CompositionResponse response = analysisService.compose(
                    request.participants(), request.globalType());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Composition failed", e);
            return serverError("Internal error during composition");
        }
    }

    @PostMapping("/game-data")
    public ResponseEntity<?> gameData(@RequestBody AnalyzeRequest request) {
        var validation = validateTypeString(request.typeString());
        if (validation != null) return validation;
        try {
            GameDataResponse response = analysisService.gameData(request.typeString().trim());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Game data failed for input: {}", truncate(request.typeString()), e);
            return serverError("Internal error during game data generation");
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

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "benchmarks", analysisService.getBenchmarks().size(),
                "tutorials", tutorialService.getTutorials().size()
        );
    }

    @GetMapping("/cache/stats")
    public Map<String, Object> cacheStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        boolean connected = false;
        try {
            redisConnectionFactory.getConnection().ping();
            connected = true;
        } catch (Exception e) {
            log.debug("Redis not available: {}", e.getMessage());
        }
        stats.put("enabled", connected);
        stats.put("cacheNames", cacheManager.getCacheNames());
        return stats;
    }

    // --- helpers ---

    private ResponseEntity<?> validateTypeString(String typeString) {
        if (typeString == null || typeString.isBlank()) {
            return badRequest("typeString is required");
        }
        if (typeString.length() > MAX_INPUT_LENGTH) {
            return badRequest("Input too long (max " + MAX_INPUT_LENGTH + " characters)");
        }
        return null;
    }

    private static ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    private static ResponseEntity<?> serverError(String message) {
        return ResponseEntity.internalServerError().body(Map.of("error", message));
    }

    private static String truncate(String s) {
        return s != null && s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
