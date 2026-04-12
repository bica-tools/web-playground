package com.bica.web.controller;

import com.bica.web.dto.BlogPostRequest;
import com.bica.web.dto.BlogPostResponse;
import com.bica.web.dto.BlogPostSummaryResponse;
import com.bica.web.service.BlogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class BlogController {

    private static final Logger log = LoggerFactory.getLogger(BlogController.class);

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    // --- Public endpoints ---

    @GetMapping("/api/blog")
    public ResponseEntity<List<BlogPostSummaryResponse>> listPublished(
            @RequestParam(required = false) Integer arc) {
        log.debug("GET /api/blog arc={}", arc);
        if (arc != null) {
            return ResponseEntity.ok(blogService.listByArc(arc));
        }
        return ResponseEntity.ok(blogService.listPublished());
    }

    @GetMapping("/api/blog/{slug}")
    public ResponseEntity<?> getBySlug(@PathVariable String slug) {
        log.debug("GET /api/blog/{}", slug);
        return blogService.getBySlug(slug)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Admin endpoints ---

    @GetMapping("/api/admin/blog")
    public ResponseEntity<List<BlogPostResponse>> listAll() {
        log.debug("GET /api/admin/blog");
        return ResponseEntity.ok(blogService.listAll());
    }

    @GetMapping("/api/admin/blog/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        log.debug("GET /api/admin/blog/{}", id);
        return blogService.getById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/admin/blog")
    public ResponseEntity<?> create(@RequestBody BlogPostRequest request) {
        log.info("POST /api/admin/blog slug={}", request.slug());
        try {
            BlogPostResponse created = blogService.create(request);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/api/admin/blog/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody BlogPostRequest request) {
        log.info("PUT /api/admin/blog/{}", id);
        return blogService.update(id, request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/admin/blog/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        log.info("DELETE /api/admin/blog/{}", id);
        if (blogService.delete(id)) {
            return ResponseEntity.ok(Map.of("deleted", true));
        }
        return ResponseEntity.notFound().build();
    }
}
