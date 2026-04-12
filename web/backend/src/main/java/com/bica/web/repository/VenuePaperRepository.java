package com.bica.web.repository;

import com.bica.web.entity.VenuePaper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VenuePaperRepository extends JpaRepository<VenuePaper, Long> {

    Optional<VenuePaper> findBySlug(String slug);

    List<VenuePaper> findByStatus(String status);

    /** List all venue papers that are not under double-blind review. */
    List<VenuePaper> findByDoubleBlindActiveFalse();

    boolean existsBySlug(String slug);
}
