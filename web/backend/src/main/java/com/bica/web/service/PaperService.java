package com.bica.web.service;

import com.bica.web.dto.*;
import com.bica.web.entity.StepPaper;
import com.bica.web.entity.VenuePaper;
import com.bica.web.repository.CommentRepository;
import com.bica.web.repository.ReactionRepository;
import com.bica.web.repository.StepPaperRepository;
import com.bica.web.repository.VenuePaperRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PaperService {

    private static final Logger log = LoggerFactory.getLogger(PaperService.class);

    private static final List<String> ALL_PHASES = List.of(
            "core", "spectral", "cross-domain", "industry", "applications",
            "morphisms", "language", "lattice-theory", "language-hierarchy",
            "philosophy", "physics", "biology", "engineering", "game-theory");

    private final StepPaperRepository stepPaperRepository;
    private final VenuePaperRepository venuePaperRepository;
    private final ReactionRepository reactionRepository;
    private final CommentRepository commentRepository;

    public PaperService(StepPaperRepository stepPaperRepository,
                        VenuePaperRepository venuePaperRepository,
                        ReactionRepository reactionRepository,
                        CommentRepository commentRepository) {
        this.stepPaperRepository = stepPaperRepository;
        this.venuePaperRepository = venuePaperRepository;
        this.reactionRepository = reactionRepository;
        this.commentRepository = commentRepository;
    }

    // --- Step Paper listing and filtering ---

    /** List all step papers with optional filtering. */
    public List<StepPaperSummaryResponse> listStepPapers(String phase, String status,
                                                          String proofBacking, String grade,
                                                          String search, String tag) {
        List<StepPaper> papers;

        if (search != null && !search.isBlank()) {
            papers = stepPaperRepository.searchByTitleOrAbstract(search.trim());
        } else if (phase != null) {
            papers = stepPaperRepository.findByPhase(phase);
        } else if (status != null) {
            papers = stepPaperRepository.findByStatus(status);
        } else if (proofBacking != null) {
            papers = stepPaperRepository.findByProofBacking(proofBacking);
        } else if (grade != null) {
            papers = stepPaperRepository.findByGrade(grade);
        } else if (tag != null) {
            papers = stepPaperRepository.findByTagsContainingIgnoreCase(tag);
        } else {
            papers = stepPaperRepository.findAllByOrderByStepNumberAsc();
        }

        return papers.stream().map(this::toSummary).toList();
    }

    /** Get a single step paper by slug (full detail). */
    public Optional<StepPaperResponse> getStepPaperBySlug(String slug) {
        return stepPaperRepository.findBySlug(slug).map(this::toResponse);
    }

    /** List all step papers (admin — full detail). */
    public List<StepPaperResponse> listAllStepPapers() {
        return stepPaperRepository.findAllByOrderByStepNumberAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Get any step paper by ID (admin). */
    public Optional<StepPaperResponse> getStepPaperById(Long id) {
        return stepPaperRepository.findById(id).map(this::toResponse);
    }

    /** Create a new step paper. */
    public StepPaperResponse createStepPaper(StepPaperRequest request) {
        if (stepPaperRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Slug already exists: " + request.slug());
        }
        if (stepPaperRepository.existsByStepNumber(request.stepNumber())) {
            throw new IllegalArgumentException("Step number already exists: " + request.stepNumber());
        }

        StepPaper paper = new StepPaper();
        applyStepPaperRequest(paper, request);

        StepPaper saved = stepPaperRepository.save(paper);
        log.info("Created step paper: {} (step={}, slug={})", saved.getId(), saved.getStepNumber(), saved.getSlug());
        return toResponse(saved);
    }

    /** Update an existing step paper. */
    public Optional<StepPaperResponse> updateStepPaper(Long id, StepPaperRequest request) {
        return stepPaperRepository.findById(id).map(paper -> {
            applyStepPaperRequest(paper, request);
            StepPaper saved = stepPaperRepository.save(paper);
            log.info("Updated step paper: {} (step={}, slug={})", saved.getId(), saved.getStepNumber(), saved.getSlug());
            return toResponse(saved);
        });
    }

    /** Delete a step paper. */
    public boolean deleteStepPaper(Long id) {
        if (stepPaperRepository.existsById(id)) {
            stepPaperRepository.deleteById(id);
            log.info("Deleted step paper: {}", id);
            return true;
        }
        return false;
    }

    // --- Venue Papers ---

    /** List venue papers visible to the public (excludes double-blind). */
    public List<VenuePaperResponse> listPublicVenuePapers() {
        return venuePaperRepository.findByDoubleBlindActiveFalse()
                .stream()
                .map(this::toVenueResponse)
                .toList();
    }

    /** Get a venue paper by slug (excludes double-blind from public view). */
    public Optional<VenuePaperResponse> getVenuePaperBySlug(String slug) {
        return venuePaperRepository.findBySlug(slug)
                .filter(vp -> !vp.isDoubleBlindActive())
                .map(this::toVenueResponse);
    }

    /** List all venue papers (admin — includes double-blind). */
    public List<VenuePaperResponse> listAllVenuePapers() {
        return venuePaperRepository.findAll()
                .stream()
                .map(this::toVenueResponse)
                .toList();
    }

    /** Get any venue paper by ID (admin). */
    public Optional<VenuePaperResponse> getVenuePaperById(Long id) {
        return venuePaperRepository.findById(id).map(this::toVenueResponse);
    }

    /** Create a new venue paper. */
    public VenuePaperResponse createVenuePaper(VenuePaperRequest request) {
        if (venuePaperRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Slug already exists: " + request.slug());
        }

        VenuePaper paper = new VenuePaper();
        applyVenuePaperRequest(paper, request);

        VenuePaper saved = venuePaperRepository.save(paper);
        log.info("Created venue paper: {} (slug={})", saved.getId(), saved.getSlug());
        return toVenueResponse(saved);
    }

    /** Update an existing venue paper. */
    public Optional<VenuePaperResponse> updateVenuePaper(Long id, VenuePaperRequest request) {
        return venuePaperRepository.findById(id).map(paper -> {
            applyVenuePaperRequest(paper, request);
            VenuePaper saved = venuePaperRepository.save(paper);
            log.info("Updated venue paper: {} (slug={})", saved.getId(), saved.getSlug());
            return toVenueResponse(saved);
        });
    }

    /** Delete a venue paper. */
    public boolean deleteVenuePaper(Long id) {
        if (venuePaperRepository.existsById(id)) {
            venuePaperRepository.deleteById(id);
            log.info("Deleted venue paper: {}", id);
            return true;
        }
        return false;
    }

    // --- BibTeX generation ---

    /** Generate BibTeX entry for a step paper. */
    public Optional<String> generateBibtex(String slug) {
        return stepPaperRepository.findBySlug(slug).map(paper -> {
            String key = "caldeira2026step" + paper.getStepNumber().replaceAll("[^a-zA-Z0-9]", "");
            return String.format("""
                    @techreport{%s,
                      author    = {Alexandre Zua Caldeira},
                      title     = {%s},
                      year      = {2026},
                      note      = {Step %s, Session Types as Algebraic Reticulates programme},
                      url       = {https://bica-tools.org/papers/%s}
                    }""", key, paper.getTitle(), paper.getStepNumber(), paper.getSlug());
        });
    }

    // --- Statistics ---

    /** Get per-phase statistics. */
    public List<PhaseStatsResponse> getPhaseStats() {
        return ALL_PHASES.stream().map(phase -> {
            long total = stepPaperRepository.countByPhase(phase);
            long completed = stepPaperRepository.countByPhaseAndStatus(phase, "complete")
                    + stepPaperRepository.countByPhaseAndStatus(phase, "proved");
            long proved = stepPaperRepository.countByPhaseAndStatus(phase, "proved");
            return new PhaseStatsResponse(phase, total, completed, proved);
        }).filter(ps -> ps.totalPapers() > 0).toList();
    }

    /** Get programme-wide statistics. */
    public ProgrammeStatsResponse getProgrammeStats() {
        List<StepPaper> all = stepPaperRepository.findAll();
        long totalPapers = all.size();
        long totalWords = all.stream()
                .mapToLong(p -> p.getWordCount() != null ? p.getWordCount() : 0)
                .sum();
        long provedCount = stepPaperRepository.countByStatus("proved");
        List<PhaseStatsResponse> phases = getPhaseStats();
        return new ProgrammeStatsResponse(totalPapers, totalWords, provedCount, phases);
    }

    // --- Mappers ---

    private void applyStepPaperRequest(StepPaper paper, StepPaperRequest request) {
        paper.setStepNumber(request.stepNumber());
        paper.setSlug(request.slug());
        paper.setTitle(request.title());
        paper.setAbstractText(request.abstractText());
        paper.setPhase(request.phase());
        paper.setDomain(request.domain());
        paper.setStatus(request.status());
        paper.setProofBacking(request.proofBacking());
        paper.setGrade(request.grade());
        paper.setWordCount(request.wordCount());
        paper.setTags(request.tags());
        paper.setPdfPath(request.pdfPath());
        paper.setHasProofsTex(request.hasProofsTex());
        paper.setLeanFiles(request.leanFiles());
        paper.setReticulateModule(request.reticulateModule());
        paper.setBicaPackage(request.bicaPackage());
        paper.setDependsOn(request.dependsOn());
        paper.setRelatedSteps(request.relatedSteps());
        paper.setSupersededBy(request.supersededBy());
        paper.setVersion(request.version());
        paper.setRevisionNotes(request.revisionNotes());
        paper.setBlogSlugs(request.blogSlugs());
        paper.setVenuePaperSlug(request.venuePaperSlug());
    }

    private void applyVenuePaperRequest(VenuePaper paper, VenuePaperRequest request) {
        paper.setSlug(request.slug());
        paper.setTitle(request.title());
        paper.setAbstractText(request.abstractText());
        paper.setVenue(request.venue());
        paper.setStatus(request.status());
        paper.setPdfPath(request.pdfPath());
        paper.setSubmissionDate(request.submissionDate());
        paper.setDecisionDate(request.decisionDate());
        paper.setStepsCovered(request.stepsCovered());
        paper.setDoi(request.doi());
        paper.setArxivId(request.arxivId());
        paper.setBibtex(request.bibtex());
        paper.setDoubleBlindActive(request.doubleBlindActive());
    }

    private List<ReactionCountResponse> getReactionCounts(Long stepPaperId) {
        return reactionRepository.countByStepPaperIdGroupByReactionType(stepPaperId)
                .stream()
                .map(row -> new ReactionCountResponse((String) row[0], (Long) row[1]))
                .toList();
    }

    private StepPaperSummaryResponse toSummary(StepPaper paper) {
        return new StepPaperSummaryResponse(
                paper.getId(), paper.getStepNumber(), paper.getSlug(), paper.getTitle(),
                paper.getPhase(), paper.getStatus(), paper.getProofBacking(),
                paper.getGrade(), paper.getWordCount(), paper.getTags(),
                getReactionCounts(paper.getId()));
    }

    private StepPaperResponse toResponse(StepPaper paper) {
        return new StepPaperResponse(
                paper.getId(), paper.getStepNumber(), paper.getSlug(), paper.getTitle(),
                paper.getAbstractText(), paper.getPhase(), paper.getDomain(),
                paper.getStatus(), paper.getProofBacking(), paper.getGrade(),
                paper.getWordCount(), paper.getTags(), paper.getPdfPath(),
                paper.isHasProofsTex(), paper.getLeanFiles(), paper.getReticulateModule(),
                paper.getBicaPackage(), paper.getDependsOn(), paper.getRelatedSteps(),
                paper.getSupersededBy(), paper.getVersion(), paper.getRevisionNotes(),
                paper.getBlogSlugs(), paper.getVenuePaperSlug(),
                paper.getCreatedAt(), paper.getUpdatedAt(),
                getReactionCounts(paper.getId()),
                commentRepository.countByStepPaperId(paper.getId()));
    }

    private VenuePaperResponse toVenueResponse(VenuePaper paper) {
        return new VenuePaperResponse(
                paper.getId(), paper.getSlug(), paper.getTitle(), paper.getAbstractText(),
                paper.getVenue(), paper.getStatus(), paper.getPdfPath(),
                paper.getSubmissionDate(), paper.getDecisionDate(),
                paper.getStepsCovered(), paper.getDoi(), paper.getArxivId(),
                paper.getBibtex(), paper.isDoubleBlindActive(),
                paper.getCreatedAt(), paper.getUpdatedAt());
    }
}
