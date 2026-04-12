package com.bica.web.config;

import com.bica.web.entity.BlogPost;
import com.bica.web.repository.BlogPostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(BlogSeeder.class)
class BlogSeederTest {

    @Autowired
    private BlogPostRepository repository;

    @Test
    void seedsThreeArc1Posts() {
        // The seeder runs via ApplicationRunner; @DataJpaTest + @Import triggers it
        List<BlogPost> posts = repository.findAllByOrderByArcAscSequenceAsc();
        assertEquals(3, posts.size());
    }

    @Test
    void postsAreInOrder() {
        List<BlogPost> posts = repository.findAllByOrderByArcAscSequenceAsc();
        assertEquals(1, posts.get(0).getSequence());
        assertEquals(2, posts.get(1).getSequence());
        assertEquals(3, posts.get(2).getSequence());
    }

    @Test
    void allPostsArePublished() {
        List<BlogPost> posts = repository.findByPublishedTrueOrderByArcAscSequenceAsc();
        assertEquals(3, posts.size());
        for (BlogPost p : posts) {
            assertTrue(p.isPublished());
            assertNotNull(p.getPublishedAt());
        }
    }

    @Test
    void slugsAreCorrect() {
        assertTrue(repository.findBySlug("what-are-session-types").isPresent());
        assertTrue(repository.findBySlug("the-grammar").isPresent());
        assertTrue(repository.findBySlug("parsing-from-text-to-structure").isPresent());
    }

    @Test
    void contentIsSubstantial() {
        for (BlogPost p : repository.findAll()) {
            assertTrue(p.getContent().length() > 500,
                    "Post '" + p.getSlug() + "' content too short: " + p.getContent().length());
            assertTrue(p.getTitle().length() > 5);
            assertTrue(p.getSummary().length() > 20);
        }
    }

    @Test
    void idempotent() {
        // Seeder already ran; running again should not duplicate
        BlogSeeder seeder = new BlogSeeder();
        assertDoesNotThrow(() ->
                seeder.seedBlog(repository).run(new org.springframework.boot.DefaultApplicationArguments()));
        assertEquals(3, repository.count());
    }
}
