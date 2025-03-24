package com.userPresence1.userPresence1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService presenceService;
    private final Map<Long, List<SseEmitter>> presenceEmitters = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(PresenceController.class);

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateUserPresence(@RequestBody Map<String, String> request) {
        try {
            Long noteId = Long.valueOf(request.get("noteId"));
            String username = request.get("username");

            if (username == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }

            logger.info("🟢 Adding user '{}' to note '{}'", username, noteId);
            presenceService.addUser(noteId, username);
            notifyPresenceUpdate(noteId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Presence updated");
            response.put("users", presenceService.getUsersViewing(noteId));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("🔥 Error in updateUserPresence: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/remove")
    public ResponseEntity<Map<String, Object>> removeUserPresence(@RequestBody Map<String, String> request) {
        try {
            Long noteId = Long.valueOf(request.get("noteId"));
            String username = request.get("username");

            if (username == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }

            logger.info("🔴 Removing user '{}' from note '{}'", username, noteId);
            presenceService.removeUser(noteId, username);
            notifyPresenceUpdate(noteId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User removed");
            response.put("users", presenceService.getUsersViewing(noteId));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("🔥 Error in removeUserPresence: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/subscribe/{noteId}")
    public SseEmitter subscribeToPresence(@PathVariable Long noteId, HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        presenceEmitters.computeIfAbsent(noteId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        logger.info("📡 New SSE subscription for note '{}'", noteId);

        emitter.onCompletion(() -> cleanupEmitter(noteId, emitter));
        emitter.onTimeout(() -> cleanupEmitter(noteId, emitter));
        emitter.onError(e -> cleanupEmitter(noteId, emitter));

        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected to presence updates for note " + noteId));
        } catch (IOException e) {
            logger.error("Error sending INIT SSE: ", e);
        }

        return emitter;
    }

    private void cleanupEmitter(Long noteId, SseEmitter emitter) {
        List<SseEmitter> emitters = presenceEmitters.get(noteId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                presenceEmitters.remove(noteId);
            }
        }
    }

    private void notifyPresenceUpdate(Long noteId) {
        List<SseEmitter> emitters = presenceEmitters.getOrDefault(noteId, new CopyOnWriteArrayList<>());
        Set<String> users = presenceService.getUsersViewing(noteId);

        logger.info("📢 Notifying presence update for note '{}': {}", noteId, users);

        List<SseEmitter> failedEmitters = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("user-presence").data(users));
            } catch (IOException e) {
                logger.error("❌ Failed to send SSE for note '{}': {}", noteId, e.getMessage());
                failedEmitters.add(emitter);
                emitter.complete();
            }
        }
        emitters.removeAll(failedEmitters);
        if (emitters.isEmpty()) {
            presenceEmitters.remove(noteId);
        }
    }
}
