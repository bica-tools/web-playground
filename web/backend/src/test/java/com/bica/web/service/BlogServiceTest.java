package com.bica.web.service;

import com.bica.web.dto.BlogPostRequest;
import com.bica.web.dto.BlogPostResponse;
import com.bica.web.dto.BlogPostSummaryResponse;
import com.bica.web.entity.BlogPost;
import com.bica.web.repository.BlogPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BlogServiceTest {

    @Autowired
    private BlogPostRepository repository;

    private BlogService service;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        service = new BlogService(repository);
    }

    private BlogPostRequest makeRequest(String slug, String title, int arc, int seq, boolean published) {
        return new BlogPostRequest(slug, title, "Summary of " + title,
                "# " + title + "\n\nContent here.", arc, seq,
                "session-types,lattice", "Alexandre Zua Caldeira", published);
    }

    @Nested
    class CreateTests {

        @Test
        void createPost() {
            BlogPostResponse resp = service.create(makeRequest("what-are-session-types",
                    "What Are Session Types?", 1, 1, false));

            assertNotNull(resp.id());
            assertEquals("what-are-session-types", resp.slug());
            assertEquals("What Are Session Types?", resp.title());
            assertEquals(1, resp.arc());
            assertEquals(1, resp.sequence());
            assertFalse(resp.published());
            assertNotNull(resp.createdAt());
            assertNull(resp.publishedAt());
        }

        @Test
        void createPublishedSetsPublishedAt() {
            BlogPostResponse resp = service.create(makeRequest("the-grammar",
                    "The Grammar", 1, 2, true));

            assertTrue(resp.published());
            assertNotNull(resp.publishedAt());
        }

        @Test
        void duplicateSlugThrows() {
            service.create(makeRequest("slug-a", "Title A", 1, 1, false));
            assertThrows(IllegalArgumentException.class,
                    () -> service.create(makeRequest("slug-a", "Title B", 1, 2, false)));
        }
    }

    @Nested
    class ReadTests {

        @Test
        void listPublishedExcludesDrafts() {
            service.create(makeRequest("draft-post", "Draft", 1, 1, false));
            service.create(makeRequest("pub-post", "Published", 1, 2, true));

            List<BlogPostSummaryResponse> published = service.listPublished();
            assertEquals(1, published.size());
            assertEquals("pub-post", published.get(0).slug());
        }

        @Test
        void listByArcFilters() {
            service.create(makeRequest("arc1-post", "Arc 1", 1, 1, true));
            service.create(makeRequest("arc2-post", "Arc 2", 2, 1, true));

            List<BlogPostSummaryResponse> arc1 = service.listByArc(1);
            assertEquals(1, arc1.size());
            assertEquals("arc1-post", arc1.get(0).slug());
        }

        @Test
        void getBySlugReturnsPublished() {
            service.create(makeRequest("my-slug", "My Post", 1, 1, true));

            Optional<BlogPostResponse> result = service.getBySlug("my-slug");
            assertTrue(result.isPresent());
            assertEquals("My Post", result.get().title());
        }

        @Test
        void getBySlugHidesDrafts() {
            service.create(makeRequest("draft-slug", "Draft Post", 1, 1, false));

            Optional<BlogPostResponse> result = service.getBySlug("draft-slug");
            assertFalse(result.isPresent());
        }

        @Test
        void listAllIncludesDrafts() {
            service.create(makeRequest("draft", "Draft", 1, 1, false));
            service.create(makeRequest("pub", "Published", 1, 2, true));

            List<BlogPostResponse> all = service.listAll();
            assertEquals(2, all.size());
        }

        @Test
        void orderedByArcThenSequence() {
            service.create(makeRequest("s2", "Second", 1, 2, true));
            service.create(makeRequest("s1", "First", 1, 1, true));
            service.create(makeRequest("s3", "Third", 2, 1, true));

            List<BlogPostSummaryResponse> list = service.listPublished();
            assertEquals(3, list.size());
            assertEquals("s1", list.get(0).slug());
            assertEquals("s2", list.get(1).slug());
            assertEquals("s3", list.get(2).slug());
        }
    }

    @Nested
    class UpdateTests {

        @Test
        void updateChangesFields() {
            BlogPostResponse created = service.create(makeRequest("orig", "Original", 1, 1, false));

            Optional<BlogPostResponse> updated = service.update(created.id(),
                    makeRequest("orig", "Updated Title", 1, 1, false));

            assertTrue(updated.isPresent());
            assertEquals("Updated Title", updated.get().title());
        }

        @Test
        void publishingSetsPublishedAt() {
            BlogPostResponse created = service.create(makeRequest("draft", "Draft", 1, 1, false));
            assertNull(created.publishedAt());

            Optional<BlogPostResponse> published = service.update(created.id(),
                    makeRequest("draft", "Draft", 1, 1, true));

            assertTrue(published.isPresent());
            assertNotNull(published.get().publishedAt());
        }

        @Test
        void updateNonExistentReturnsEmpty() {
            Optional<BlogPostResponse> result = service.update(999L,
                    makeRequest("x", "X", 1, 1, false));
            assertFalse(result.isPresent());
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void deleteExisting() {
            BlogPostResponse created = service.create(makeRequest("to-delete", "Delete Me", 1, 1, false));
            assertTrue(service.delete(created.id()));
            assertEquals(0, service.listAll().size());
        }

        @Test
        void deleteNonExistent() {
            assertFalse(service.delete(999L));
        }
    }
}
