package io.github.jobs.samples.database.controller;

import io.github.jobs.samples.database.entity.Comment;
import io.github.jobs.samples.database.entity.Post;
import io.github.jobs.samples.database.entity.User;
import io.github.jobs.samples.database.repository.CommentRepository;
import io.github.jobs.samples.database.repository.PostRepository;
import io.github.jobs.samples.database.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller demonstrating various SQL query patterns.
 * Some endpoints intentionally cause N+1 queries for demonstration.
 */
@RestController
@RequestMapping("/api")
public class DatabaseController {

    private static final Logger log = LoggerFactory.getLogger(DatabaseController.class);

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public DatabaseController(
            UserRepository userRepository,
            PostRepository postRepository,
            CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    // ============================================
    // N+1 PROBLEM ENDPOINTS (for demonstration)
    // ============================================

    /**
     * BAD: Causes N+1 query problem.
     * 1 query to fetch users + N queries to fetch posts for each user.
     */
    @GetMapping("/users/n-plus-one")
    public List<Map<String, Object>> getUsersWithNPlusOne() {
        log.warn("Executing N+1 query pattern - check SQL Analyzer!");

        List<User> users = userRepository.findAll();

        // Accessing lazy-loaded posts causes N+1
        return users.stream()
            .map(user -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", user.getId());
                dto.put("name", user.getName());
                dto.put("email", user.getEmail());
                // This triggers a separate query for each user!
                dto.put("postCount", user.getPosts().size());
                return dto;
            })
            .collect(Collectors.toList());
    }

    /**
     * BAD: Causes N+1 query problem with posts and comments.
     */
    @GetMapping("/posts/n-plus-one")
    public List<Map<String, Object>> getPostsWithNPlusOne() {
        log.warn("Executing N+1 query pattern - check SQL Analyzer!");

        List<Post> posts = postRepository.findAll();

        return posts.stream()
            .map(post -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", post.getId());
                dto.put("title", post.getTitle());
                // These trigger separate queries for each post!
                dto.put("authorName", post.getAuthor().getName());
                dto.put("commentCount", post.getComments().size());
                return dto;
            })
            .collect(Collectors.toList());
    }

    // ============================================
    // OPTIMIZED ENDPOINTS (proper solutions)
    // ============================================

    /**
     * GOOD: Uses EntityGraph to fetch users with posts in a single query.
     */
    @GetMapping("/users/optimized")
    public List<Map<String, Object>> getUsersOptimized() {
        log.info("Executing optimized query with EntityGraph");

        List<User> users = userRepository.findAllWithPosts();

        return users.stream()
            .map(user -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", user.getId());
                dto.put("name", user.getName());
                dto.put("email", user.getEmail());
                dto.put("postCount", user.getPosts().size());
                return dto;
            })
            .collect(Collectors.toList());
    }

    /**
     * GOOD: Uses JOIN FETCH to fetch posts with author and comments.
     */
    @GetMapping("/posts/optimized")
    public List<Map<String, Object>> getPostsOptimized() {
        log.info("Executing optimized query with JOIN FETCH");

        List<Post> posts = postRepository.findAllWithAuthorAndCommentsJoinFetch();

        return posts.stream()
            .map(post -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", post.getId());
                dto.put("title", post.getTitle());
                dto.put("authorName", post.getAuthor().getName());
                dto.put("commentCount", post.getComments().size());
                return dto;
            })
            .collect(Collectors.toList());
    }

    // ============================================
    // SLOW QUERY ENDPOINTS (for demonstration)
    // ============================================

    /**
     * Simulates a slow query by using LIKE with leading wildcard.
     */
    @GetMapping("/users/search/{term}")
    public List<User> searchUsers(@PathVariable String term) {
        log.info("Executing potentially slow search query");
        // LIKE '%term%' can't use index efficiently
        return userRepository.findByNameContaining(term);
    }

    // ============================================
    // STANDARD CRUD ENDPOINTS
    // ============================================

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @GetMapping("/posts")
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    @GetMapping("/posts/{id}")
    public Post getPost(@PathVariable Long id) {
        return postRepository.findById(id).orElse(null);
    }

    @GetMapping("/comments")
    public List<Comment> getAllComments() {
        return commentRepository.findAll();
    }
}
