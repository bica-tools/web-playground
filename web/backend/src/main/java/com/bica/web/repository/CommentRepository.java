package com.bica.web.repository;

import com.bica.web.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** Top-level comments for a step paper (no parent), ordered oldest first. */
    List<Comment> findByStepPaperIdAndParentCommentIdIsNullOrderByCreatedAtDesc(Long stepPaperId);

    /** Replies to a specific comment, ordered oldest first. */
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    long countByStepPaperId(Long stepPaperId);
}
