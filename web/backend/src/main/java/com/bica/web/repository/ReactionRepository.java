package com.bica.web.repository;

import com.bica.web.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    List<Reaction> findByStepPaperId(Long stepPaperId);

    /** Count reactions grouped by type for a given step paper. Returns Object[] = {reactionType, count}. */
    @Query("SELECT r.reactionType, COUNT(r) FROM Reaction r WHERE r.stepPaperId = :stepPaperId GROUP BY r.reactionType")
    List<Object[]> countByStepPaperIdGroupByReactionType(@Param("stepPaperId") Long stepPaperId);

    /** Find existing reaction for idempotency check. */
    Optional<Reaction> findByStepPaperIdAndSessionIdAndReactionType(
            Long stepPaperId, String sessionId, String reactionType);
}
