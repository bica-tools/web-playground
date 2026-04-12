package com.bica.web.controller;

import com.bica.web.dto.*;
import com.bica.web.service.CommentService;
import com.bica.web.service.PaperService;
import com.bica.web.service.ReactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class PaperController {

    private static final Logger log = LoggerFactory.getLogger(PaperController.class);

    private final PaperService paperService;
    private final ReactionService reactionService;
    private final CommentService commentService;

    public PaperController(PaperService paperService,
                           ReactionService reactionService,
                           CommentService commentService) {
        this.paperService = paperService;
        this.reactionService = reactionService;
        this.commentService = commentService;
    }

    // --- Public step paper endpoints ---

    @GetMapping("/api/papers")
    public ResponseEntity<List<StepPaperSummaryResponse>> listPapers(
            @RequestParam(required = false) String phase,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String proofBacking,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tag) {
        log.debug("GET /api/papers phase={} status={} proofBacking={} grade={} search={} tag={}",
                phase, status, proofBacking, grade, search, tag);
        return ResponseEntity.ok(paperService.listStepPapers(phase, status, proofBacking, grade, search, tag));
    }

    @GetMapping("/api/papers/{slug}")
    public ResponseEntity<?> getPaper(@PathVariable String slug) {
        log.debug("GET /api/papers/{}", slug);
        return paperService.getStepPaperBySlug(slug)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/api/papers/{slug}/bibtex", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getBibtex(@PathVariable String slug) {
        log.debug("GET /api/papers/{}/bibtex", slug);
        return paperService.generateBibtex(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/papers/phases")
    public ResponseEntity<List<PhaseStatsResponse>> getPhaseStats() {
        log.debug("GET /api/papers/phases");
        return ResponseEntity.ok(paperService.getPhaseStats());
    }

    @GetMapping("/api/papers/stats")
    public ResponseEntity<ProgrammeStatsResponse> getProgrammeStats() {
        log.debug("GET /api/papers/stats");
        return ResponseEntity.ok(paperService.getProgrammeStats());
    }

    // --- Public venue paper endpoints ---

    @GetMapping("/api/venue-papers")
    public ResponseEntity<List<VenuePaperResponse>> listVenuePapers() {
        log.debug("GET /api/venue-papers");
        return ResponseEntity.ok(paperService.listPublicVenuePapers());
    }

    @GetMapping("/api/venue-papers/{slug}")
    public ResponseEntity<?> getVenuePaper(@PathVariable String slug) {
        log.debug("GET /api/venue-papers/{}", slug);
        return paperService.getVenuePaperBySlug(slug)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Reactions ---

    @PostMapping("/api/papers/{slug}/reactions")
    public ResponseEntity<?> addReaction(@PathVariable String slug,
                                         @RequestBody ReactionRequest request,
                                         @CookieValue(value = "session_id", defaultValue = "anonymous") String sessionId) {
        log.info("POST /api/papers/{}/reactions type={}", slug, request.reactionType());
        try {
            reactionService.addReaction(slug, request, sessionId);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/papers/{slug}/reactions")
    public ResponseEntity<?> getReactions(@PathVariable String slug) {
        log.debug("GET /api/papers/{}/reactions", slug);
        try {
            return ResponseEntity.ok(reactionService.getReactionCounts(slug));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- Comments ---

    @GetMapping("/api/papers/{slug}/comments")
    public ResponseEntity<?> getComments(@PathVariable String slug) {
        log.debug("GET /api/papers/{}/comments", slug);
        try {
            return ResponseEntity.ok(commentService.listComments(slug));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/api/papers/{slug}/comments")
    public ResponseEntity<?> addComment(@PathVariable String slug,
                                        @RequestBody CommentRequest request) {
        log.info("POST /api/papers/{}/comments author={}", slug, request.authorName());
        try {
            CommentResponse created = commentService.addComment(slug, request);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // --- Admin step paper endpoints ---

    @GetMapping("/api/admin/papers")
    public ResponseEntity<List<StepPaperResponse>> adminListPapers() {
        log.debug("GET /api/admin/papers");
        return ResponseEntity.ok(paperService.listAllStepPapers());
    }

    @GetMapping("/api/admin/papers/{id}")
    public ResponseEntity<?> adminGetPaper(@PathVariable Long id) {
        log.debug("GET /api/admin/papers/{}", id);
        return paperService.getStepPaperById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/admin/papers")
    public ResponseEntity<?> adminCreatePaper(@RequestBody StepPaperRequest request) {
        log.info("POST /api/admin/papers step={} slug={}", request.stepNumber(), request.slug());
        try {
            StepPaperResponse created = paperService.createStepPaper(request);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/api/admin/papers/{id}")
    public ResponseEntity<?> adminUpdatePaper(@PathVariable Long id,
                                              @RequestBody StepPaperRequest request) {
        log.info("PUT /api/admin/papers/{}", id);
        return paperService.updateStepPaper(id, request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/admin/papers/{id}")
    public ResponseEntity<?> adminDeletePaper(@PathVariable Long id) {
        log.info("DELETE /api/admin/papers/{}", id);
        if (paperService.deleteStepPaper(id)) {
            return ResponseEntity.ok(Map.of("deleted", true));
        }
        return ResponseEntity.notFound().build();
    }

    // --- Admin venue paper endpoints ---

    @GetMapping("/api/admin/venue-papers")
    public ResponseEntity<List<VenuePaperResponse>> adminListVenuePapers() {
        log.debug("GET /api/admin/venue-papers");
        return ResponseEntity.ok(paperService.listAllVenuePapers());
    }

    @GetMapping("/api/admin/venue-papers/{id}")
    public ResponseEntity<?> adminGetVenuePaper(@PathVariable Long id) {
        log.debug("GET /api/admin/venue-papers/{}", id);
        return paperService.getVenuePaperById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/admin/venue-papers")
    public ResponseEntity<?> adminCreateVenuePaper(@RequestBody VenuePaperRequest request) {
        log.info("POST /api/admin/venue-papers slug={}", request.slug());
        try {
            VenuePaperResponse created = paperService.createVenuePaper(request);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/api/admin/venue-papers/{id}")
    public ResponseEntity<?> adminUpdateVenuePaper(@PathVariable Long id,
                                                   @RequestBody VenuePaperRequest request) {
        log.info("PUT /api/admin/venue-papers/{}", id);
        return paperService.updateVenuePaper(id, request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/admin/venue-papers/{id}")
    public ResponseEntity<?> adminDeleteVenuePaper(@PathVariable Long id) {
        log.info("DELETE /api/admin/venue-papers/{}", id);
        if (paperService.deleteVenuePaper(id)) {
            return ResponseEntity.ok(Map.of("deleted", true));
        }
        return ResponseEntity.notFound().build();
    }

    // --- Admin comment management ---

    @DeleteMapping("/api/admin/comments/{id}")
    public ResponseEntity<?> adminDeleteComment(@PathVariable Long id) {
        log.info("DELETE /api/admin/comments/{}", id);
        if (commentService.deleteComment(id)) {
            return ResponseEntity.ok(Map.of("deleted", true));
        }
        return ResponseEntity.notFound().build();
    }
}
