package com.bica.web.repository;

import com.bica.web.entity.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    Optional<BlogPost> findBySlug(String slug);

    List<BlogPost> findByPublishedTrueOrderByArcAscSequenceAsc();

    List<BlogPost> findByArcAndPublishedTrueOrderBySequenceAsc(int arc);

    List<BlogPost> findByTagsContainingIgnoreCaseAndPublishedTrue(String tag);

    List<BlogPost> findAllByOrderByArcAscSequenceAsc();

    boolean existsBySlug(String slug);
}
