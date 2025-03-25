package com.userPresence1.userPresence1;
//package com.userPresence1.userPresence1;
//import org.springframework.stereotype.Service;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Service
//public class PresenceService {
//    private final Map<String, Set<String>> usersViewingNotes = new ConcurrentHashMap<>();
//
//    /** ✅ Add user to presence list */
//    public void addUser(String noteId, String username) {
//        usersViewingNotes.computeIfAbsent(noteId, k -> ConcurrentHashMap.newKeySet()).add(username);
//    }
//
//    /** ✅ Remove user from presence list */
//    public void removeUser(String noteId, String username) {
//        usersViewingNotes.computeIfPresent(noteId, (key, users) -> {
//            users.remove(username);
//            return users.isEmpty() ? null : users;
//        });
//    }
//
//    /** ✅ Get current users viewing a note */
//    public Set<String> getUsers(String noteId) {
//        return usersViewingNotes.getOrDefault(noteId, Set.of());
//    }
//}




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

    /** Add user to Redis Set */
    public void addUser(String noteId, String username) {
        redisTemplate.opsForSet().add("presence:" + noteId, username);
        logger.info("Added user '{}' to Redis for note '{}'", username, noteId);
    }

    /** Remove user from Redis Set */
    public void removeUser(String noteId, String username) {
        redisTemplate.opsForSet().remove("presence:" + noteId, username);
        logger.info("Removed user '{}' from Redis for note '{}'", username, noteId);
    }

    /** Get users viewing a note */
    public Set<String> getUsersViewing(String noteId) {
        Set<String> users = redisTemplate.opsForSet().members("presence:" + noteId);
        return users != null ? users : new HashSet<>();
    }
}
