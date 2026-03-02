package com.bica.web.controller;

import com.bica.web.dto.*;
import com.bica.web.service.AnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private final AnalysisService analysisService;

    public AnalyzeController(AnalysisService analysisService) {
        this.analysisService = analysisService;
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

    @GetMapping("/benchmarks")
    public List<BenchmarkDto> benchmarks() {
        return analysisService.getBenchmarks();
    }
}
