package com.bica.web.service;

import com.bica.web.dto.ReactionCountResponse;
import com.bica.web.dto.ReactionRequest;
import com.bica.web.entity.Reaction;
import com.bica.web.entity.StepPaper;
import com.bica.web.repository.ReactionRepository;
import com.bica.web.repository.StepPaperRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReactionService {

    private static final Logger log = LoggerFactory.getLogger(ReactionService.class);

    private final ReactionRepository reactionRepository;
    private final StepPaperRepository stepPaperRepository;

    public ReactionService(ReactionRepository reactionRepository,
                           StepPaperRepository stepPaperRepository) {
        this.reactionRepository = reactionRepository;
        this.stepPaperRepository = stepPaperRepository;
    }

    /**
     * Add a reaction to a step paper. Idempotent per session + reaction type:
     * if the same session has already reacted with the same type, the existing reaction is returned.
     */
    public void addReaction(String slug, ReactionRequest request, String sessionId) {
        StepPaper paper = stepPaperRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Step paper not found: " + slug));

        Optional<Reaction> existing = reactionRepository
                .findByStepPaperIdAndSessionIdAndReactionType(paper.getId(), sessionId, request.reactionType());

        if (existing.isPresent()) {
            log.debug("Reaction already exists for session={} type={} paper={}", sessionId, request.reactionType(), slug);
            return;
        }

        Reaction reaction = new Reaction();
        reaction.setStepPaperId(paper.getId());
        reaction.setReactionType(request.reactionType());
        reaction.setSessionId(sessionId);

        reactionRepository.save(reaction);
        log.info("Added reaction type={} to paper={} session={}", request.reactionType(), slug, sessionId);
    }

    /** Get reaction counts grouped by type for a step paper. */
    public List<ReactionCountResponse> getReactionCounts(String slug) {
        StepPaper paper = stepPaperRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Step paper not found: " + slug));

        return reactionRepository.countByStepPaperIdGroupByReactionType(paper.getId())
                .stream()
                .map(row -> new ReactionCountResponse((String) row[0], (Long) row[1]))
                .toList();
    }
}
