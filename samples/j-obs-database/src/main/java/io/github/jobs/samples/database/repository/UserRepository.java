package io.github.jobs.samples.database.repository;

import io.github.jobs.samples.database.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User repository with various query patterns for demonstrating SQL analysis.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Simple query - no N+1 issue
    Optional<User> findByEmail(String email);

    // Query without JOIN FETCH - will cause N+1 when accessing posts
    List<User> findByNameContaining(String name);

    // Properly optimized query with EntityGraph - avoids N+1
    @EntityGraph(attributePaths = {"posts"})
    @Query("SELECT u FROM User u")
    List<User> findAllWithPosts();

    // Properly optimized query with JOIN FETCH - avoids N+1
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.posts")
    List<User> findAllWithPostsJoinFetch();
}
