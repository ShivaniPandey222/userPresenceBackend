package com.userPresence1.userPresence1;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class PresenceService {

    private final StringRedisTemplate redisTemplate;
    private static final Logger logger = LoggerFactory.getLogger(PresenceService.class);

    public PresenceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void startupCleanup() {
        logger.info("🧹 Cleaning up old presence data...");
        Set<String> keys = redisTemplate.keys("presence:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            logger.info("Cleanup complete. Removed {} presence keys.", keys.size());
        } else {
            logger.info("No presence keys found for cleanup.");
        }
    }

    /**
     * Add a user to the presence tracking in Redis.
     * The noteId is expected as a UUID string.
     */
    public void addUser(String noteId, String username) {
        redisTemplate.opsForSet().add("presence:" + noteId, username);
        logger.info("Added user '{}' to Redis for note '{}'", username, noteId);
    }

    /**
     * Remove a user from the presence tracking in Redis.
     */
    public void removeUser(String noteId, String username) {
        Long removed = redisTemplate.opsForSet().remove("presence:" + noteId, username);
        if (removed != null && removed > 0) {
            logger.info("Removed user '{}' from Redis for note '{}'", username, noteId);
        } else {
            logger.warn("User '{}' was NOT found in Redis for note '{}'", username, noteId);
        }
    }

    /**
     * Get the list of users currently viewing a note.
     */
    public Set<String> getUsersViewing(String noteId) {
        String key = "presence:" + noteId;
        Set<String> users = redisTemplate.opsForSet().members(key);
        if (users == null) {
            users = new HashSet<>();
        }
        logger.info("Fresh users from Redis for '{}': {}", noteId, users);
        return users;
    }
}
