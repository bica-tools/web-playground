package com.bica.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Social reaction on a step paper (anonymous, session-based).
 */
@Entity
@Table(name = "reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"step_paper_id", "session_id", "reaction_type"}))
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "step_paper_id", nullable = false)
    private Long stepPaperId;

    /** Reaction type: like, love, insightful, refute, alternative, needs-revision. */
    @Column(name = "reaction_type", nullable = false)
    private String reactionType;

    /** Anonymous session tracking via cookie (no user accounts). */
    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- Getters and setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStepPaperId() { return stepPaperId; }
    public void setStepPaperId(Long stepPaperId) { this.stepPaperId = stepPaperId; }

    public String getReactionType() { return reactionType; }
    public void setReactionType(String reactionType) { this.reactionType = reactionType; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
