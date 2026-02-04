package io.github.jobs.samples.database;

import io.github.jobs.samples.database.entity.Comment;
import io.github.jobs.samples.database.entity.Post;
import io.github.jobs.samples.database.entity.User;
import io.github.jobs.samples.database.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes sample data for the database demo.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing sample database data...");

        // Create users with posts and comments
        for (int i = 1; i <= 10; i++) {
            User user = new User("User " + i, "user" + i + "@example.com");

            // Each user has 3-5 posts
            for (int j = 1; j <= 3 + (i % 3); j++) {
                Post post = new Post(
                    "Post " + j + " by User " + i,
                    "This is the content of post " + j + " written by user " + i + ". " +
                    "It contains some interesting text that demonstrates the JPA entity relationships."
                );
                user.addPost(post);

                // Each post has 2-4 comments
                for (int k = 1; k <= 2 + (j % 3); k++) {
                    Comment comment = new Comment(
                        "Comment " + k + " on post " + j,
                        "Commenter " + k
                    );
                    post.addComment(comment);
                }
            }

            userRepository.save(user);
        }

        long userCount = userRepository.count();
        log.info("Sample data initialized: {} users created", userCount);
    }
}
