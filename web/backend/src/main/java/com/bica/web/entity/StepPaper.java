package com.bica.web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Step paper entity for bica-tools.org/papers.
 * Represents one of the 383+ step research papers in the programme.
 */
@Entity
@Table(name = "step_papers")
public class StepPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Step number, e.g. "1", "5b", "155b". Unique identifier within the programme. */
    @Column(nullable = false, unique = true)
    private String stepNumber;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(name = "abstract_text", columnDefinition = "TEXT")
    private String abstractText;

    /** Phase: core, spectral, cross-domain, industry, applications, morphisms, language,
     *  lattice-theory, language-hierarchy, philosophy, physics, biology, engineering, game-theory. */
    @Column(nullable = false)
    private String phase;

    /** Domain: theory, tools, applications, cross-domain. */
    @Column(nullable = false)
    private String domain;

    /** Status: draft, complete, proved, revised, superseded, retracted. */
    @Column(nullable = false)
    private String status;

    /** Proof backing: mechanised, partial, empirical. */
    private String proofBacking;

    /** Grade: A+, A, B, C. */
    private String grade;

    private Integer wordCount;

    /** Comma-separated tags. */
    @Column(length = 1000)
    private String tags;

    /** Path to the PDF file. */
    private String pdfPath;

    /** Whether this step has a companion proofs.tex file. */
    private boolean hasProofsTex;

    /** Lean formalization file paths (comma-separated). */
    @Column(length = 1000)
    private String leanFiles;

    /** Corresponding reticulate Python module name. */
    private String reticulateModule;

    /** Corresponding BICA Java package name. */
    private String bicaPackage;

    /** Comma-separated step numbers this paper depends on. */
    @Column(length = 1000)
    private String dependsOn;

    /** Comma-separated related step numbers. */
    @Column(length = 1000)
    private String relatedSteps;

    /** Step number of the paper that supersedes this one (if any). */
    private String supersededBy;

    /** Version number, starting at 1. */
    @Column(nullable = false)
    private int version = 1;

    /** Revision notes explaining changes between versions. */
    @Column(columnDefinition = "TEXT")
    private String revisionNotes;

    /** Comma-separated blog post slugs that reference this paper. */
    @Column(length = 1000)
    private String blogSlugs;

    /** Slug of the venue paper that covers this step. */
    private String venuePaperSlug;

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

    public String getStepNumber() { return stepNumber; }
    public void setStepNumber(String stepNumber) { this.stepNumber = stepNumber; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProofBacking() { return proofBacking; }
    public void setProofBacking(String proofBacking) { this.proofBacking = proofBacking; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }

    public boolean isHasProofsTex() { return hasProofsTex; }
    public void setHasProofsTex(boolean hasProofsTex) { this.hasProofsTex = hasProofsTex; }

    public String getLeanFiles() { return leanFiles; }
    public void setLeanFiles(String leanFiles) { this.leanFiles = leanFiles; }

    public String getReticulateModule() { return reticulateModule; }
    public void setReticulateModule(String reticulateModule) { this.reticulateModule = reticulateModule; }

    public String getBicaPackage() { return bicaPackage; }
    public void setBicaPackage(String bicaPackage) { this.bicaPackage = bicaPackage; }

    public String getDependsOn() { return dependsOn; }
    public void setDependsOn(String dependsOn) { this.dependsOn = dependsOn; }

    public String getRelatedSteps() { return relatedSteps; }
    public void setRelatedSteps(String relatedSteps) { this.relatedSteps = relatedSteps; }

    public String getSupersededBy() { return supersededBy; }
    public void setSupersededBy(String supersededBy) { this.supersededBy = supersededBy; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getRevisionNotes() { return revisionNotes; }
    public void setRevisionNotes(String revisionNotes) { this.revisionNotes = revisionNotes; }

    public String getBlogSlugs() { return blogSlugs; }
    public void setBlogSlugs(String blogSlugs) { this.blogSlugs = blogSlugs; }

    public String getVenuePaperSlug() { return venuePaperSlug; }
    public void setVenuePaperSlug(String venuePaperSlug) { this.venuePaperSlug = venuePaperSlug; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
