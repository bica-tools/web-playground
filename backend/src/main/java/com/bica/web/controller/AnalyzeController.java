package com.bica.web.controller;

import com.bica.web.dto.*;
import com.bica.web.service.AnalysisService;
import com.bica.web.service.TutorialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeController.class);
    private static final int MAX_INPUT_LENGTH = 10_000;

    private final AnalysisService analysisService;
    private final TutorialService tutorialService;
    private final com.bica.web.service.ExplainService explainService;
    private final com.bica.web.service.GameRoomService gameRoomService;
    private final CacheManager cacheManager;
    private final RedisConnectionFactory redisConnectionFactory;

    public AnalyzeController(AnalysisService analysisService,
                             TutorialService tutorialService,
                             com.bica.web.service.ExplainService explainService,
                             com.bica.web.service.GameRoomService gameRoomService,
                             CacheManager cacheManager,
                             RedisConnectionFactory redisConnectionFactory) {
        this.analysisService = analysisService;
        this.tutorialService = tutorialService;
        this.explainService = explainService;
        this.gameRoomService = gameRoomService;
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

    @PostMapping("/game-plays")
    public ResponseEntity<?> gamePlays(@RequestBody AnalyzeRequest request) {
        var validation = validateTypeString(request.typeString());
        if (validation != null) return validation;
        try {
            var response = analysisService.gamePlays(request.typeString().trim());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Game plays failed for input: {}", truncate(request.typeString()), e);
            return serverError("Internal error during game plays generation");
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

    private static final Pattern SESSION_ANNOTATION = Pattern.compile(
            "@Session\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]+)\"");
    private static final Pattern TYPE_DECLARATION = Pattern.compile(
            "(?:class|interface|record|enum)\\s+(\\w+)");

    @PostMapping(value = "/extract-session", consumes = "multipart/form-data")
    public ResponseEntity<?> extractSession(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return badRequest("No file uploaded");
        }
        try {
            String source = new String(file.getBytes(), StandardCharsets.UTF_8);
            var results = new ArrayList<Map<String, String>>();
            Matcher annMatcher = SESSION_ANNOTATION.matcher(source);
            while (annMatcher.find()) {
                String typeString = annMatcher.group(1);
                // Find the class name after the annotation
                String remaining = source.substring(annMatcher.end());
                Matcher classMatcher = TYPE_DECLARATION.matcher(remaining);
                String className = classMatcher.find() ? classMatcher.group(1) : "Unknown";
                results.add(Map.of("className", className, "typeString", typeString));
            }
            if (results.isEmpty()) {
                return ResponseEntity.ok(Map.of("found", false, "message", "No @Session annotations found", "annotations", List.of()));
            }
            return ResponseEntity.ok(Map.of("found", true, "annotations", results));
        } catch (Exception e) {
            log.warn("Extract session failed", e);
            return serverError("Failed to process file: " + e.getMessage());
        }
    }

    // --- Multiplayer Game Rooms ---

    @PostMapping("/room/create")
    public ResponseEntity<?> createRoom(@RequestBody Map<String, String> request) {
        String typeString = request.get("typeString");
        String player = request.get("player");
        if (typeString == null || player == null) return badRequest("typeString and player are required");
        var room = gameRoomService.createRoom(typeString, player);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/room/join")
    public ResponseEntity<?> joinRoom(@RequestBody Map<String, String> request) {
        String roomId = request.get("roomId");
        String player = request.get("player");
        if (roomId == null || player == null) return badRequest("roomId and player are required");
        var room = gameRoomService.joinRoom(roomId, player);
        if (room == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(room);
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable String roomId) {
        var room = gameRoomService.getRoom(roomId);
        if (room == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(room);
    }

    @PostMapping("/room/move")
    public ResponseEntity<?> makeMove(@RequestBody Map<String, String> request) {
        String roomId = request.get("roomId");
        String player = request.get("player");
        String method = request.get("method");
        if (roomId == null || player == null || method == null)
            return badRequest("roomId, player, and method are required");
        var room = gameRoomService.makeMove(roomId, player, method);
        if (room == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(room);
    }

    @PostMapping("/validate-trace")
    public ResponseEntity<?> validateTrace(@RequestBody Map<String, Object> request) {
        String typeString = (String) request.get("typeString");
        @SuppressWarnings("unchecked")
        List<String> trace = (List<String>) request.get("trace");
        if (typeString == null || typeString.isBlank()) return badRequest("typeString is required");
        if (trace == null) return badRequest("trace is required");
        try {
            var ast = com.bica.reborn.parser.Parser.parse(typeString.trim());
            var ss = com.bica.reborn.statespace.StateSpaceBuilder.build(ast);

            var path = new ArrayList<Map<String, Object>>();
            int current = ss.top();
            boolean valid = true;
            String violationAt = null;

            for (String method : trace) {
                boolean found = false;
                for (var t : ss.transitions()) {
                    if (t.source() == current && t.label().equals(method)) {
                        path.add(Map.of("method", method, "from", t.source(), "to", t.target(), "valid", true));
                        current = t.target();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    var enabled = new ArrayList<String>();
                    for (var t : ss.transitions()) {
                        if (t.source() == current) enabled.add(t.label());
                    }
                    path.add(Map.of("method", method, "from", current, "to", current, "valid", false,
                            "enabled", enabled));
                    valid = false;
                    violationAt = method;
                    break;
                }
            }

            boolean atEnd = current == ss.bottom();
            return ResponseEntity.ok(Map.of(
                    "valid", valid, "complete", valid && atEnd,
                    "currentState", current, "path", path,
                    "violationAt", violationAt != null ? violationAt : "",
                    "atEnd", atEnd
            ));
        } catch (Exception e) {
            return badRequest("Trace validation failed: " + e.getMessage());
        }
    }

    @GetMapping("/proof-replay")
    public ResponseEntity<?> proofReplay() {
        // Curated proof-to-visual mappings for key theorems
        var replays = List.of(
                Map.of("theorem", "UniqueExtrema",
                        "leanModule", "ReticulateTheorem",
                        "description", "Every session type state space has a unique top (initial) and bottom (end) state.",
                        "exampleType", "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                        "highlights", List.of("top", "bottom"),
                        "steps", List.of(
                                Map.of("text", "The initial state (before any method call) is the unique top element.", "highlight", "top"),
                                Map.of("text", "The end state (after the protocol terminates) is the unique bottom element.", "highlight", "bottom"),
                                Map.of("text", "By the Reticulate Theorem, these are the only extrema — guaranteed for all well-formed types.", "highlight", "all")
                        )),
                Map.of("theorem", "ProductLattice",
                        "leanModule", "ProductReticulate",
                        "description", "The parallel constructor yields a product lattice: L(S1 || S2) = L(S1) x L(S2).",
                        "exampleType", "(read . end || write . end)",
                        "highlights", List.of("product"),
                        "steps", List.of(
                                Map.of("text", "L(read.end) has 2 states: {before read, end}.", "highlight", "left"),
                                Map.of("text", "L(write.end) has 2 states: {before write, end}.", "highlight", "right"),
                                Map.of("text", "The product L(S1) x L(S2) has 2x2 = 4 states, ordered componentwise.", "highlight", "product"),
                                Map.of("text", "Meets and joins are computed componentwise — guaranteed to exist.", "highlight", "all")
                        )),
                Map.of("theorem", "RecursionLemma",
                        "leanModule", "RecursionReticulate",
                        "description", "SCC quotient preserves the lattice property for recursive types.",
                        "exampleType", "rec X . &{a: X, b: end}",
                        "highlights", List.of("scc"),
                        "steps", List.of(
                                Map.of("text", "Recursion creates cycles: variable X loops back to the entry state.", "highlight", "cycle"),
                                Map.of("text", "The SCC quotient collapses each cycle into a single node.", "highlight", "scc"),
                                Map.of("text", "The resulting DAG preserves meets, joins, and the lattice property.", "highlight", "all")
                        )),
                Map.of("theorem", "SubtypingAntitonicity",
                        "leanModule", "SubtypingReticulate",
                        "description", "Gay-Hole subtyping reverses the lattice embedding: S1 <: S2 implies L(S2) embeds in L(S1).",
                        "exampleType", "&{read: end, write: end}",
                        "highlights", List.of("embedding"),
                        "steps", List.of(
                                Map.of("text", "&{read: end, write: end} has more methods, so it is a subtype of &{read: end}.", "highlight", "subtype"),
                                Map.of("text", "The subtype has MORE states (more methods = more reachable configurations).", "highlight", "larger"),
                                Map.of("text", "The supertype's lattice embeds into the subtype's lattice — antitonicity.", "highlight", "embedding")
                        )),
                Map.of("theorem", "DualityInvolution",
                        "leanModule", "DualityReticulate",
                        "description", "Duality is an involution: dual(dual(S)) = S, and preserves state-space isomorphism.",
                        "exampleType", "&{request: +{OK: end, ERROR: end}}",
                        "highlights", List.of("dual"),
                        "steps", List.of(
                                Map.of("text", "The dual swaps Branch <-> Select: &{...} becomes +{...} and vice versa.", "highlight", "swap"),
                                Map.of("text", "dual(S) has the same state space structure — isomorphic lattice.", "highlight", "iso"),
                                Map.of("text", "Applying dual twice returns the original type: dual(dual(S)) = S.", "highlight", "involution")
                        ))
        );
        return ResponseEntity.ok(replays);
    }

    @GetMapping("/lattice-zoo")
    public ResponseEntity<?> latticeZoo() {
        try {
            var benchmarks = analysisService.getBenchmarks();
            // Select visually interesting benchmarks (varied sizes)
            var zoo = benchmarks.stream()
                    .filter(b -> b.numStates() >= 3 && b.numStates() <= 30)
                    .map(b -> Map.of(
                            "name", b.name(),
                            "numStates", b.numStates(),
                            "numTransitions", b.numTransitions(),
                            "isLattice", b.isLattice(),
                            "svgHtml", b.svgHtml(),
                            "typeString", b.typeString(),
                            "tags", b.tags()
                    ))
                    .toList();
            return ResponseEntity.ok(zoo);
        } catch (Exception e) {
            return serverError("Zoo generation failed: " + e.getMessage());
        }
    }

    @GetMapping("/reverse-search")
    public ResponseEntity<?> reverseSearch(@RequestParam String q) {
        if (q == null || q.isBlank()) {
            return badRequest("q parameter is required");
        }
        try {
            String query = q.trim().toLowerCase();
            var benchmarks = analysisService.getBenchmarks();
            var matches = benchmarks.stream()
                    .filter(b -> b.name().toLowerCase().contains(query)
                            || b.description().toLowerCase().contains(query)
                            || b.methods().stream().anyMatch(m -> m.toLowerCase().contains(query))
                            || b.tags().stream().anyMatch(t -> t.toLowerCase().contains(query))
                            || b.typeString().toLowerCase().contains(query))
                    .map(b -> Map.of(
                            "name", b.name(),
                            "description", b.description(),
                            "typeString", b.typeString(),
                            "numStates", String.valueOf(b.numStates()),
                            "numTransitions", String.valueOf(b.numTransitions()),
                            "isLattice", String.valueOf(b.isLattice())
                    ))
                    .toList();
            return ResponseEntity.ok(Map.of("results", matches, "total", matches.size()));
        } catch (Exception e) {
            return serverError("Search failed: " + e.getMessage());
        }
    }

    @GetMapping("/explain")
    public ResponseEntity<?> explain(@RequestParam String type) {
        if (type == null || type.isBlank()) {
            return badRequest("type parameter is required");
        }
        if (type.length() > MAX_INPUT_LENGTH) {
            return badRequest("Input too long");
        }
        try {
            String explanation = explainService.explain(type.trim());
            return ResponseEntity.ok(Map.of("explanation", explanation));
        } catch (Exception e) {
            log.warn("Explain failed for type: {}", truncate(type), e);
            return badRequest("Could not explain: " + e.getMessage());
        }
    }

    @GetMapping("/story")
    public ResponseEntity<?> story(@RequestParam String type,
                                   @RequestParam(required = false) Map<String, String> allParams) {
        if (type == null || type.isBlank()) {
            return badRequest("type parameter is required");
        }
        if (type.length() > MAX_INPUT_LENGTH) {
            return badRequest("Input too long");
        }
        try {
            // Extract glossary entries: any param that isn't "type" is a glossary mapping
            Map<String, String> glossary = new java.util.HashMap<>();
            allParams.forEach((k, v) -> {
                if (!"type".equals(k)) glossary.put(k, v);
            });
            String narrative = explainService.story(type.trim(), glossary.isEmpty() ? null : glossary);
            return ResponseEntity.ok(Map.of("story", narrative));
        } catch (Exception e) {
            log.warn("Story failed for type: {}", truncate(type), e);
            return badRequest("Could not generate story: " + e.getMessage());
        }
    }

    @GetMapping(value = "/og-image", produces = "image/svg+xml")
    public ResponseEntity<?> ogImage(@RequestParam String type) {
        if (type == null || type.isBlank()) {
            return badRequest("type parameter is required");
        }
        if (type.length() > MAX_INPUT_LENGTH) {
            return badRequest("Input too long");
        }
        try {
            String svg = analysisService.renderOgSvg(type.trim());
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("image/svg+xml"))
                    .header("Cache-Control", "public, max-age=86400")
                    .body(svg);
        } catch (Exception e) {
            log.warn("OG image failed for type: {}", truncate(type), e);
            return ResponseEntity.notFound().build();
        }
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
