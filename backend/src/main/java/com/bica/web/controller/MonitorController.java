package com.bica.web.controller;

import com.bica.web.dto.AgentTypeDto;
import com.bica.web.dto.ProgrammeStatusDto;
import com.bica.web.dto.StepEvaluationDto;
import com.bica.web.service.MonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private static final Logger log = LoggerFactory.getLogger(MonitorController.class);

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    /**
     * Returns the 8 agent type definitions with their session types.
     */
    @GetMapping("/agents")
    public ResponseEntity<List<AgentTypeDto>> getAgents() {
        log.debug("GET /api/monitor/agents");
        try {
            List<AgentTypeDto> agents = monitorService.getAgentTypes();
            return ResponseEntity.ok(agents);
        } catch (Exception e) {
            log.error("Failed to get agent types", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Returns programme status (step counts, module counts, grades).
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        log.debug("GET /api/monitor/status");
        try {
            ProgrammeStatusDto status = monitorService.getProgrammeStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get programme status", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get programme status: " + e.getMessage()));
        }
    }

    /**
     * Returns the pipeline status for a specific step.
     */
    @GetMapping("/pipeline/{stepNumber}")
    public ResponseEntity<?> getPipelineStatus(@PathVariable String stepNumber) {
        log.debug("GET /api/monitor/pipeline/{}", stepNumber);
        if (stepNumber == null || stepNumber.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "stepNumber is required"));
        }
        try {
            StepEvaluationDto evaluation = monitorService.getPipelineStatus(stepNumber);
            return ResponseEntity.ok(evaluation);
        } catch (Exception e) {
            log.error("Failed to get pipeline status for step {}", stepNumber, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get pipeline status: " + e.getMessage()));
        }
    }

    /**
     * Triggers evaluation of a step, returns grade + fixes.
     */
    @PostMapping("/evaluate")
    public ResponseEntity<?> evaluate(@RequestBody Map<String, String> request) {
        String stepNumber = request.get("stepNumber");
        log.info("POST /api/monitor/evaluate stepNumber={}", stepNumber);
        if (stepNumber == null || stepNumber.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "stepNumber is required"));
        }
        try {
            StepEvaluationDto evaluation = monitorService.evaluateStep(stepNumber);
            return ResponseEntity.ok(evaluation);
        } catch (Exception e) {
            log.error("Failed to evaluate step {}", stepNumber, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Evaluation failed: " + e.getMessage()));
        }
    }
}
