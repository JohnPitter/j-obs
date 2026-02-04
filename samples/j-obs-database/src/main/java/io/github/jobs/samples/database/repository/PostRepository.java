package io.github.jobs.samples.database.repository;

import io.github.jobs.samples.database.entity.Post;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Post repository with various query patterns for demonstrating SQL analysis.
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Query without JOIN FETCH - will cause N+1 when accessing comments
    List<Post> findByTitleContaining(String title);

    // Query without JOIN FETCH - will cause N+1 when accessing author
    List<Post> findByAuthorId(Long authorId);

    // Properly optimized query with EntityGraph - avoids N+1
    @EntityGraph(attributePaths = {"author", "comments"})
    @Query("SELECT p FROM Post p")
    List<Post> findAllWithAuthorAndComments();

    // Properly optimized query with JOIN FETCH - avoids N+1
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.comments")
    List<Post> findAllWithAuthorAndCommentsJoinFetch();
}
