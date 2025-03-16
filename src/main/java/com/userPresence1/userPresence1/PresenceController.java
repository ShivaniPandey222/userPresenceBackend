package com.userPresence1.userPresence1;//package com.userPresence1.userPresence1;
//
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.io.IOException;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//@RestController
//@RequestMapping("/presence")
//@CrossOrigin(origins = {"http://localhost:4900", "http://localhost:4901", "http://localhost:4902","http://localhost:4800","http://localhost:4801"},
//        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
//public class PresenceController {
//    private static final Logger logger = LoggerFactory.getLogger(PresenceController.class);
//    private final Map<String, Set<String>> usersViewingNotes = new ConcurrentHashMap<>();
//    private final Map<String, CopyOnWriteArrayList<SseEmitter>> presenceEmitters = new ConcurrentHashMap<>();
//
//    /** ✅ Update user's presence and return current users */
//    @PostMapping("/update")
//    public ResponseEntity<Map<String, Object>> updateUserPresence(@RequestBody Map<String, String> request) {
//        String noteId = request.get("noteId");
//        String username = request.get("username");
//
//        if (noteId == null || username == null) {
//            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
//        }
//
//        usersViewingNotes.computeIfAbsent(noteId, k -> ConcurrentHashMap.newKeySet()).add(username);
//
//        logger.info("User '{}' added to note '{}'. Current users: {}", username, noteId, usersViewingNotes.get(noteId));
//
//        notifyPresenceUpdate(noteId);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("message", "Presence updated");
//        response.put("users", usersViewingNotes.get(noteId));
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping("/remove")
//    public ResponseEntity<Map<String, Object>> removeUser(@RequestBody Map<String, String> request) {
//        String noteId = request.get("noteId");
//        String username = request.get("username");
//
//        if (noteId == null || username == null) {
//            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
//        }
//
//        usersViewingNotes.computeIfPresent(noteId, (key, users) -> {
//            users.remove(username);
//            return users.isEmpty() ? null : users;
//        });
//
//        logger.info("User '{}' removed from note '{}'. Remaining users: {}", username, noteId, usersViewingNotes.getOrDefault(noteId, Collections.emptySet()));
//
//        notifyPresenceUpdate(noteId);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("message", "User removed");
//        response.put("users", usersViewingNotes.getOrDefault(noteId, Collections.emptySet()));
//
//        return ResponseEntity.ok(response);
//    }
//
//
//    /** ✅ Subscribe users to presence updates */
//    @GetMapping("/subscribe/{noteId}")
//    public SseEmitter subscribeToPresence(@PathVariable String noteId) {
//        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
//        presenceEmitters.computeIfAbsent(noteId, k -> new CopyOnWriteArrayList<>()).add(emitter);
//
//        emitter.onCompletion(() -> removePresenceEmitter(noteId, emitter));
//        emitter.onTimeout(() -> removePresenceEmitter(noteId, emitter));
//
//        notifyPresenceUpdate(noteId);
//        // ✅ HEARTBEAT: Send "ping" every 30 seconds
//        // ✅ HEARTBEAT to remove stale connections
//        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
//            try {
//                emitter.send(SseEmitter.event().name("ping").data("heartbeat"));
//            } catch (IOException e) {
//                logger.warn("Failed to send heartbeat to note {}: {}", noteId, e.getMessage());
//                removePresenceEmitter(noteId, emitter);
//                emitter.complete();
//            }
//        }, 30, 30, TimeUnit.SECONDS);
//
//        return emitter;
//    }
//
//    /** ✅ Notify clients about user presence updates */
//    private void notifyPresenceUpdate(String noteId) {
//        List<SseEmitter> emitters = presenceEmitters.getOrDefault(noteId, new CopyOnWriteArrayList<>());
//        Set<String> users = usersViewingNotes.getOrDefault(noteId, Collections.emptySet());
//
//        logger.info("Notifying presence update for note {}: {}", noteId, users);
//
//        List<SseEmitter> failedEmitters = new ArrayList<>();
//
//        for (SseEmitter emitter : emitters) {
//            try {
//                emitter.send(SseEmitter.event().name("user-presence").data(users));
//            } catch (IOException e) {
//                logger.error("Failed to send SSE update for note {}: {}", noteId, e.getMessage());
//                failedEmitters.add(emitter);
//            }
//        }
//
//        // Remove failed emitters
//        emitters.removeAll(failedEmitters);
//        if (emitters.isEmpty()) {
//            presenceEmitters.remove(noteId);
//        }
//    }
//
//
//    /** ✅ Remove disconnected SSE subscribers */
//    private void removePresenceEmitter(String noteId, SseEmitter emitter) {
//        presenceEmitters.computeIfPresent(noteId, (key, emitters) -> {
//            emitters.remove(emitter);
//            return emitters.isEmpty() ? null : emitters;
//        });
//    }
//}




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

@RestController
@RequestMapping("/presence")
@CrossOrigin(origins = {"http://localhost:4900", "http://localhost:4901", "http://localhost:4908", "http://localhost:4800", "http://localhost:4801"},
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class PresenceController {
    private static final Logger logger = LoggerFactory.getLogger(PresenceController.class);
    private final PresenceService presenceService;
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> presenceEmitters = new ConcurrentHashMap<>();

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    /** ✅ Update user's presence and return current users */
//    @PostMapping("/update")
//    public ResponseEntity<Map<String, Object>> updateUserPresence(@RequestBody Map<String, String> request) {
//        String noteId = request.get("noteId");
//        String username = request.get("username");
//
//        if (noteId == null || username == null) {
//            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
//        }
//        logger.info("🟢 Adding user '{}' to note '{}'", username, noteId);
//        presenceService.addUser(noteId, username);
//        notifyPresenceUpdate(noteId);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("message", "Presence updated");
//        response.put("users", presenceService.getUsersViewing(noteId));
//        return ResponseEntity.ok(response);
//    }

    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateUserPresence(@RequestBody Map<String, String> request) {
        try {
            String noteId = request.get("noteId");
            String username = request.get("username");

            if (noteId == null || username == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
            }

            logger.info("🟢 Adding user '{}' to note '{}'", username, noteId);
            presenceService.addUser(noteId, username); // This might be failing!
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
    public ResponseEntity<Map<String, Object>> removeUser(@RequestBody Map<String, String> request) {
        String noteId = request.get("noteId");
        String username = request.get("username");

        if (noteId == null || username == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
        }

        presenceService.removeUser(noteId, username);
        notifyPresenceUpdate(noteId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User removed");
        response.put("users", presenceService.getUsersViewing(noteId));
        return ResponseEntity.ok(response);
    }

    /** ✅ Subscribe users to presence updates */
    @GetMapping("/subscribe/{noteId}")
    public SseEmitter subscribeToPresence(@PathVariable String noteId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        presenceEmitters.computeIfAbsent(noteId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removePresenceEmitter(noteId, emitter));
        emitter.onTimeout(() -> removePresenceEmitter(noteId, emitter));

        notifyPresenceUpdate(noteId);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name("ping").data("heartbeat"));
            } catch (IOException e) {
                logger.warn("Failed to send heartbeat to note {}: {}", noteId, e.getMessage());
                removePresenceEmitter(noteId, emitter);
                emitter.complete();
            }
        }, 30, 30, TimeUnit.SECONDS);

        return emitter;
    }

    /** ✅ Notify clients about user presence updates */
    public void notifyPresenceUpdate(String noteId) {
        List<SseEmitter> emitters = presenceEmitters.getOrDefault(noteId, new CopyOnWriteArrayList<>());
        Set<String> users = presenceService.getUsersViewing(noteId);

        logger.info("Notifying presence update for note {}: {}", noteId, users);

        List<SseEmitter> failedEmitters = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("user-presence").data(users));
            } catch (IOException e) {
                logger.error("Failed to send SSE update for note {}: {}", noteId, e.getMessage());
                failedEmitters.add(emitter);
            }
        }

        // Remove failed emitters
        emitters.removeAll(failedEmitters);
        if (emitters.isEmpty()) {
            presenceEmitters.remove(noteId);
        }
    }

    /** ✅ Remove disconnected SSE subscribers */
    private void removePresenceEmitter(String noteId, SseEmitter emitter) {
        presenceEmitters.computeIfPresent(noteId, (key, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
//       presenceService.removeUser(noteId, username);
        notifyPresenceUpdate(noteId);
    }
}
