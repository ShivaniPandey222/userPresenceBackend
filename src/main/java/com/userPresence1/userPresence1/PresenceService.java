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
        if (keys != null) {
            redisTemplate.delete(keys);
        }
        logger.info("Cleanup complete. Removed {} presence keys.", (keys != null ? keys.size() : 0));
    }

    /** Add user to presence tracking in Redis */
    public void addUser(String noteId, String username) {
        String noteIdStr = noteId.replaceAll("[^0-9]", ""); // removes non-numeric chars
        Long noteIdd=Long.parseLong(noteIdStr);
        redisTemplate.opsForSet().add("presence:" + noteIdStr, username);
        logger.info("Added user '{}' to Redis for note '{}'", username, noteId);
        try {
            Thread.sleep(100); // Small delay to ensure Redis updates
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    /** ✅ Remove user from presence tracking */
    public void removeUser(String noteId, String username) {
//        redisTemplate.opsForSet().remove("presence:" + noteId, username);
//        String noteIdStr = noteId.replaceAll("[^0-9]", ""); // removes non-numeric chars
//        Long noteIdd=Long.parseLong(noteIdStr);
        Long removed = redisTemplate.opsForSet().remove("presence:" + noteId, username);

        if (removed != null && removed > 0) {
            logger.info("Removed user '{}' from Redis for note '{}'", username, noteId);
        } else {
            logger.warn("User '{}' was NOT found in Redis for note '{}'", username, noteId);
        }
    }

    /** ✅ Get list of users currently viewing a note */
    public Set<String> getUsersViewing(String noteId) {
//        return redisTemplate.opsForSet().members("presence:" + noteId);
        String noteIdStr = noteId.replaceAll("[^0-9]", ""); // removes non-numeric chars
        Long noteIdd=Long.parseLong(noteIdStr);
        String key = "presence:" + noteId;
        Set<String> users = redisTemplate.opsForSet().members(key);

        if (users == null) {
            users = new HashSet<>();
        }

        logger.info(" Fresh users from Redis for '{}': {}", noteId, users);
        return users;
    }
}
