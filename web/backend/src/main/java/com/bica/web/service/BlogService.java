package com.bica.web.service;

import com.bica.web.dto.BlogPostRequest;
import com.bica.web.dto.BlogPostResponse;
import com.bica.web.dto.BlogPostSummaryResponse;
import com.bica.web.entity.BlogPost;
import com.bica.web.repository.BlogPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BlogService {

    private static final Logger log = LoggerFactory.getLogger(BlogService.class);

    private final BlogPostRepository repository;

    public BlogService(BlogPostRepository repository) {
        this.repository = repository;
    }

    /** List all published posts (summaries only, ordered by arc + sequence). */
    public List<BlogPostSummaryResponse> listPublished() {
        return repository.findByPublishedTrueOrderByArcAscSequenceAsc()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    /** List published posts in a specific arc. */
    public List<BlogPostSummaryResponse> listByArc(int arc) {
        return repository.findByArcAndPublishedTrueOrderBySequenceAsc(arc)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    /** Get a single published post by slug. */
    public Optional<BlogPostResponse> getBySlug(String slug) {
        return repository.findBySlug(slug)
                .filter(BlogPost::isPublished)
                .map(this::toResponse);
    }

    /** List all posts (admin — includes drafts). */
    public List<BlogPostResponse> listAll() {
        return repository.findAllByOrderByArcAscSequenceAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Get any post by ID (admin). */
    public Optional<BlogPostResponse> getById(Long id) {
        return repository.findById(id).map(this::toResponse);
    }

    /** Create a new blog post. */
    public BlogPostResponse create(BlogPostRequest request) {
        if (repository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Slug already exists: " + request.slug());
        }

        BlogPost post = new BlogPost();
        applyRequest(post, request);

        if (request.published()) {
            post.setPublishedAt(LocalDateTime.now());
        }

        BlogPost saved = repository.save(post);
        log.info("Created blog post: {} (slug={})", saved.getId(), saved.getSlug());
        return toResponse(saved);
    }

    /** Update an existing blog post. */
    public Optional<BlogPostResponse> update(Long id, BlogPostRequest request) {
        return repository.findById(id).map(post -> {
            boolean wasPublished = post.isPublished();
            applyRequest(post, request);

            if (!wasPublished && request.published()) {
                post.setPublishedAt(LocalDateTime.now());
            }

            BlogPost saved = repository.save(post);
            log.info("Updated blog post: {} (slug={})", saved.getId(), saved.getSlug());
            return toResponse(saved);
        });
    }

    /** Delete a blog post. */
    public boolean delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            log.info("Deleted blog post: {}", id);
            return true;
        }
        return false;
    }

    // --- Mappers ---

    private void applyRequest(BlogPost post, BlogPostRequest request) {
        post.setSlug(request.slug());
        post.setTitle(request.title());
        post.setSummary(request.summary());
        post.setContent(request.content());
        post.setArc(request.arc());
        post.setSequence(request.sequence());
        post.setTags(request.tags());
        post.setAuthor(request.author());
        post.setPublished(request.published());
    }

    private BlogPostResponse toResponse(BlogPost post) {
        return new BlogPostResponse(
                post.getId(), post.getSlug(), post.getTitle(), post.getSummary(),
                post.getContent(), post.getArc(), post.getSequence(), post.getTags(),
                post.getAuthor(), post.isPublished(), post.getCreatedAt(),
                post.getPublishedAt(), post.getUpdatedAt());
    }

    private BlogPostSummaryResponse toSummary(BlogPost post) {
        return new BlogPostSummaryResponse(
                post.getId(), post.getSlug(), post.getTitle(), post.getSummary(),
                post.getArc(), post.getSequence(), post.getTags(),
                post.getAuthor(), post.getPublishedAt());
    }
}
