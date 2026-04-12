package com.bica.web.repository;

import com.bica.web.entity.StepPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StepPaperRepository extends JpaRepository<StepPaper, Long> {

    Optional<StepPaper> findBySlug(String slug);

    Optional<StepPaper> findByStepNumber(String stepNumber);

    List<StepPaper> findByPhase(String phase);

    List<StepPaper> findByStatus(String status);

    List<StepPaper> findByProofBacking(String proofBacking);

    List<StepPaper> findByGrade(String grade);

    @Query("SELECT p FROM StepPaper p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<StepPaper> searchByTitleOrAbstract(@Param("query") String query);

    List<StepPaper> findAllByOrderByStepNumberAsc();

    long countByPhase(String phase);

    long countByStatus(String status);

    long countByPhaseAndStatus(String phase, String status);

    boolean existsBySlug(String slug);

    boolean existsByStepNumber(String stepNumber);

    List<StepPaper> findByTagsContainingIgnoreCase(String tag);
}
