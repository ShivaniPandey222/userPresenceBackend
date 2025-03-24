package com.userPresence1.userPresence1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import org.springframework.data.redis.core.RedisTemplate;
@Service
public class PresenceService {

    private final RedisTemplate<String, String> redisTemplate;
    private final Logger logger = LoggerFactory.getLogger(PresenceService.class);

    public PresenceService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addUser(Long noteId, String username) {
        String key = "presence:" + noteId;
        redisTemplate.opsForSet().add(key, username);
        logger.info("✅ Added user '{}' to Redis for note '{}'", username, noteId);
    }

    public void removeUser(Long noteId, String username) {
        String key = "presence:" + noteId;
        Long removed = redisTemplate.opsForSet().remove(key, username);
        if (removed != null && removed > 0) {
            logger.info("🗑️ Removed user '{}' from Redis for note '{}'", username, noteId);
        } else {
            logger.warn("⚠️ User '{}' not found in Redis for note '{}'", username, noteId);
        }
    }

    public Set<String> getUsersViewing(Long noteId) {
        String key = "presence:" + noteId;
        Set<String> users = redisTemplate.opsForSet().members(key);
        if (users == null) users = new HashSet<>();
        logger.info("👀 Current users viewing note '{}': {}", noteId, users);
        return users;
    }
}
