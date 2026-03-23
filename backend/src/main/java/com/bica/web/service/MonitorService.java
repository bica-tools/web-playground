package com.bica.web.service;

import com.bica.web.dto.AgentTypeDto;
import com.bica.web.dto.ProgrammeStatusDto;
import com.bica.web.dto.StepEvaluationDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MonitorService {

    private static final Logger log = LoggerFactory.getLogger(MonitorService.class);
    private static final String PYTHON = "python3";
    private static final long TIMEOUT_SECONDS = 60;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- Agent type definitions (matching agent_registry.py) ---

    private static final List<AgentTypeDto> AGENT_TYPES = List.of(
            new AgentTypeDto("Researcher", "MCP",
                    "&{investigate: +{report: end}}",
                    "Reads code and theory, maps the landscape",
                    "stdio"),
            new AgentTypeDto("Implementer", "A2A",
                    "rec X . &{implement: +{moduleReady: end, error: +{retry: X, abort: end}}}",
                    "Writes one Python module with clean API",
                    "Agent()"),
            new AgentTypeDto("Tester", "A2A",
                    "rec X . &{writeTests: +{testsPass: end, testsFail: +{fix: X, abort: end}}}",
                    "Writes test suite, runs tests, ensures coverage",
                    "Agent()"),
            new AgentTypeDto("Writer", "A2A",
                    "&{writePaper: +{paperReady: end}}",
                    "Writes 5000+ word educational paper",
                    "Agent()"),
            new AgentTypeDto("Prover", "A2A",
                    "&{writeProofs: +{proofsReady: end}}",
                    "Writes companion proofs with formal theorems",
                    "Agent()"),
            new AgentTypeDto("Evaluator", "MCP",
                    "&{evaluate: +{accepted: end, needsFixes: +{listFixes: end}}}",
                    "Grades deliverables, decides accept/reject",
                    "stdio"),
            new AgentTypeDto("Reviewer", "A2A",
                    "&{review: +{fixes: end}}",
                    "Reviews code quality, suggests improvements",
                    "Agent()"),
            new AgentTypeDto("Supervisor", "MCP",
                    "&{scan: +{proposals: end}}",
                    "Scans programme, proposes next steps",
                    "stdio")
    );

    /**
     * Returns the 8 agent type definitions.
     */
    public List<AgentTypeDto> getAgentTypes() {
        return AGENT_TYPES;
    }

    /**
     * Runs the evaluator on all steps and returns programme status.
     */
    public ProgrammeStatusDto getProgrammeStatus() {
        log.info("Fetching programme status via evaluator --all --json");
        try {
            String json = runPythonCommand("-m", "reticulate.evaluator", "--all", "--json");
            return parseProgrammeStatus(json);
        } catch (Exception e) {
            log.error("Failed to get programme status", e);
            return new ProgrammeStatusDto(0, 0, 0, 0, Collections.emptyList());
        }
    }

    /**
     * Evaluates a specific step and returns its grade and fixes.
     */
    public StepEvaluationDto evaluateStep(String stepNumber) {
        log.info("Evaluating step {} via evaluator --json", stepNumber);
        try {
            String json = runPythonCommand("-m", "reticulate.evaluator", stepNumber, "--json");
            return parseStepEvaluation(json);
        } catch (Exception e) {
            log.error("Failed to evaluate step {}", stepNumber, e);
            return new StepEvaluationDto(stepNumber, "Unknown", "F", 0, false,
                    List.of("Evaluation failed: " + e.getMessage()));
        }
    }

    /**
     * Gets pipeline status for a specific step (delegates to evaluator).
     */
    public StepEvaluationDto getPipelineStatus(String stepNumber) {
        return evaluateStep(stepNumber);
    }

    // --- Python process execution ---

    private String runPythonCommand(String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(PYTHON);
        Collections.addAll(command, args);

        log.debug("Running command: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new java.io.File(findReticulateDir()));
        pb.redirectErrorStream(false);

        Process process = pb.start();

        String stdout;
        String stderr;
        try (
            BufferedReader outReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            BufferedReader errReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))
        ) {
            stdout = outReader.lines().collect(Collectors.joining("\n"));
            stderr = errReader.lines().collect(Collectors.joining("\n"));
        }

        boolean completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("Python process timed out after " + TIMEOUT_SECONDS + "s");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.warn("Python process exited with code {}: {}", exitCode, stderr);
            throw new RuntimeException("Python process failed (exit " + exitCode + "): " + stderr);
        }

        return stdout;
    }

    private String findReticulateDir() {
        // Walk up from the backend to find the reticulate directory
        String projectRoot = System.getProperty("user.dir");
        java.io.File reticulateDir = new java.io.File(projectRoot, "../../reticulate");
        if (reticulateDir.exists()) {
            return reticulateDir.getAbsolutePath();
        }
        // Fallback: try the known absolute path
        reticulateDir = new java.io.File("/home/zuacaldeira/Development/SessionTypesResearch/reticulate");
        if (reticulateDir.exists()) {
            return reticulateDir.getAbsolutePath();
        }
        log.warn("Could not find reticulate directory, using current directory");
        return projectRoot;
    }

    // --- JSON parsing ---

    private ProgrammeStatusDto parseProgrammeStatus(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            int totalSteps = root.path("total_steps").asInt(0);
            int acceptedSteps = root.path("accepted_steps").asInt(0);
            int totalModules = root.path("total_modules").asInt(0);
            int totalTests = root.path("total_tests").asInt(0);

            List<StepEvaluationDto> steps = new ArrayList<>();
            JsonNode stepsNode = root.path("steps");
            if (stepsNode.isArray()) {
                for (JsonNode stepNode : stepsNode) {
                    steps.add(parseStepNode(stepNode));
                }
            }

            return new ProgrammeStatusDto(totalSteps, acceptedSteps, totalModules, totalTests, steps);
        } catch (Exception e) {
            log.error("Failed to parse programme status JSON: {}", json, e);
            return new ProgrammeStatusDto(0, 0, 0, 0, Collections.emptyList());
        }
    }

    private StepEvaluationDto parseStepEvaluation(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return parseStepNode(root);
        } catch (Exception e) {
            log.error("Failed to parse step evaluation JSON: {}", json, e);
            return new StepEvaluationDto("?", "Unknown", "F", 0, false,
                    List.of("Failed to parse evaluation output"));
        }
    }

    private StepEvaluationDto parseStepNode(JsonNode node) {
        String stepNumber = node.path("step_number").asText(node.path("step").asText("?"));
        String title = node.path("title").asText("Unknown");
        String grade = node.path("grade").asText("F");
        int score = node.path("score").asInt(0);
        boolean accepted = node.path("accepted").asBoolean(false);

        List<String> fixes = new ArrayList<>();
        JsonNode fixesNode = node.path("fixes");
        if (fixesNode.isArray()) {
            for (JsonNode fix : fixesNode) {
                fixes.add(fix.asText());
            }
        }

        return new StepEvaluationDto(stepNumber, title, grade, score, accepted, fixes);
    }
}
