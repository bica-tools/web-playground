package com.bica.web.service;

import com.bica.web.dto.CommentRequest;
import com.bica.web.dto.CommentResponse;
import com.bica.web.entity.Comment;
import com.bica.web.entity.StepPaper;
import com.bica.web.repository.CommentRepository;
import com.bica.web.repository.StepPaperRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final StepPaperRepository stepPaperRepository;

    public CommentService(CommentRepository commentRepository,
                          StepPaperRepository stepPaperRepository) {
        this.commentRepository = commentRepository;
        this.stepPaperRepository = stepPaperRepository;
    }

    /** Add a comment (or reply) to a step paper. */
    public CommentResponse addComment(String slug, CommentRequest request) {
        StepPaper paper = stepPaperRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Step paper not found: " + slug));

        if (request.parentCommentId() != null) {
            Comment parent = commentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found: " + request.parentCommentId()));
            if (!parent.getStepPaperId().equals(paper.getId())) {
                throw new IllegalArgumentException("Parent comment does not belong to this paper");
            }
        }

        Comment comment = new Comment();
        comment.setStepPaperId(paper.getId());
        comment.setParentCommentId(request.parentCommentId());
        comment.setAuthorName(request.authorName());
        comment.setAuthorEmail(request.authorEmail());
        comment.setContent(request.content());

        Comment saved = commentRepository.save(comment);
        log.info("Added comment {} to paper={} (parent={})", saved.getId(), slug, request.parentCommentId());
        return toResponse(saved);
    }

    /** List threaded comments for a step paper (top-level with nested replies). */
    public List<CommentResponse> listComments(String slug) {
        StepPaper paper = stepPaperRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Step paper not found: " + slug));

        List<Comment> topLevel = commentRepository
                .findByStepPaperIdAndParentCommentIdIsNullOrderByCreatedAtDesc(paper.getId());

        return topLevel.stream().map(this::toResponseWithReplies).toList();
    }

    /** Delete a comment (admin). */
    public boolean deleteComment(Long id) {
        if (commentRepository.existsById(id)) {
            commentRepository.deleteById(id);
            log.info("Deleted comment: {}", id);
            return true;
        }
        return false;
    }

    // --- Mappers ---

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(), comment.getAuthorName(), comment.getContent(),
                comment.getCreatedAt(), List.of());
    }

    private CommentResponse toResponseWithReplies(Comment comment) {
        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(comment.getId());
        List<CommentResponse> replyResponses = replies.stream()
                .map(this::toResponseWithReplies)
                .toList();
        return new CommentResponse(
                comment.getId(), comment.getAuthorName(), comment.getContent(),
                comment.getCreatedAt(), replyResponses);
    }
}
