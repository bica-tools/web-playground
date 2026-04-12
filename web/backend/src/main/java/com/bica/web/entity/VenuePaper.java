package com.bica.web.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Venue paper entity for published/submitted conference and journal papers.
 */
@Entity
@Table(name = "venue_papers")
public class VenuePaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(name = "abstract_text", columnDefinition = "TEXT")
    private String abstractText;

    /** Venue name, e.g. "ICE 2026", "LMCS", "CONCUR 2026". */
    @Column(nullable = false)
    private String venue;

    /** Status: draft, submitted, under-review, accepted, published. */
    @Column(nullable = false)
    private String status;

    private String pdfPath;

    private LocalDate submissionDate;

    private LocalDate decisionDate;

    /** Comma-separated step numbers covered by this venue paper. */
    @Column(length = 1000)
    private String stepsCovered;

    private String doi;

    private String arxivId;

    @Column(columnDefinition = "TEXT")
    private String bibtex;

    /** When true, the paper is under double-blind review and should not be shown publicly. */
    private boolean doubleBlindActive;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- Getters and setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }

    public LocalDate getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(LocalDate submissionDate) { this.submissionDate = submissionDate; }

    public LocalDate getDecisionDate() { return decisionDate; }
    public void setDecisionDate(LocalDate decisionDate) { this.decisionDate = decisionDate; }

    public String getStepsCovered() { return stepsCovered; }
    public void setStepsCovered(String stepsCovered) { this.stepsCovered = stepsCovered; }

    public String getDoi() { return doi; }
    public void setDoi(String doi) { this.doi = doi; }

    public String getArxivId() { return arxivId; }
    public void setArxivId(String arxivId) { this.arxivId = arxivId; }

    public String getBibtex() { return bibtex; }
    public void setBibtex(String bibtex) { this.bibtex = bibtex; }

    public boolean isDoubleBlindActive() { return doubleBlindActive; }
    public void setDoubleBlindActive(boolean doubleBlindActive) { this.doubleBlindActive = doubleBlindActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
