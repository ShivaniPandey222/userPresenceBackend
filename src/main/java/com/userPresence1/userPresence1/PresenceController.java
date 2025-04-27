//package com.userPresence1.userPresence1;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.io.IOException;
//import java.util.*;
//import java.util.concurrent.*;
//import java.util.UUID;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//@RestController
//@RequestMapping("/presence")
//public class PresenceController {
//
//    private static final Logger logger = LoggerFactory.getLogger(PresenceController.class);
//    private final PresenceService presenceService;
//    // Map of note UUID to list of SSE emitters
//    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> presenceEmitters = new ConcurrentHashMap<>();
//
//    public PresenceController(PresenceService presenceService) {
//        this.presenceService = presenceService;
//    }
//
//    /**
//     * Update user's presence.
//     * Expects a request body with "noteId" (UUID string) and "username".
//     */
//    @PostMapping("/update")
//    public ResponseEntity<Map<String, Object>> updateUserPresence(@RequestBody Map<String, String> request) {
//        String noteIdStr = request.get("noteId");
//        String username = request.get("username");
//
//        if (noteIdStr == null || username == null) {
//            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
//        }
//
//        UUID noteId;
//        try {
//            noteId = UUID.fromString(noteIdStr);
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.badRequest().body(Map.of("error", "Invalid note ID format"));
//        }
//
//        logger.info("Adding user '{}' to note '{}'", username, noteId);
//        long startTime = System.currentTimeMillis();
//        presenceService.addUser(noteId.toString(), username);
////        logger.info("Added user in {} ms", System.currentTimeMillis() - startTime);
//        notifyPresenceUpdate(noteId);
////        logger.info("Notified presence in {} ms", System.currentTimeMillis() - startTime);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("message", "Presence updated");
//        response.put("users", presenceService.getUsersViewing(noteId.toString()));
//        System.out.println("----1"+response);
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Remove a user from the presence tracking.
//     */
//    @PostMapping("/remove")
//    public ResponseEntity<Map<String, Object>> removeUser(@RequestBody Map<String, String> request) {
//        String noteIdStr = request.get("noteId");
//        String username = request.get("username");
//
//        if (noteIdStr == null || username == null) {
//            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
//        }
//
//        UUID noteId;
//        try {
//            noteId = UUID.fromString(noteIdStr);
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.badRequest().body(Map.of("error", "Invalid note ID format"));
//        }
//
//        presenceService.removeUser(noteId.toString(), username);
//        notifyPresenceUpdate(noteId);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("message", "User removed");
//        response.put("users", presenceService.getUsersViewing(noteId.toString()));
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Subscribe to presence updates via SSE.
//     */
////    @GetMapping("/subscribe/{noteId}")
////    public SseEmitter subscribeToPresence(@PathVariable UUID noteId) {
////        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
////        presenceEmitters.computeIfAbsent(noteId, k -> new CopyOnWriteArrayList<>()).add(emitter);
////
////        emitter.onCompletion(() -> removeEmitter(noteId, emitter));
////        emitter.onTimeout(() -> removeEmitter(noteId, emitter));
////        emitter.onError((e) -> removeEmitter(noteId, emitter));
////
////        try {
////            emitter.send(SseEmitter.event().name("connection").data("Connected to SSE"));
////        } catch (IOException e) {
////            emitter.complete();
////        }
////        return emitter;
////    }
//
//    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
//
////    @GetMapping("/subscribe/{noteId}")
////    public SseEmitter subscribeToPresence(@PathVariable UUID noteId) {
////        SseEmitter emitter = new SseEmitter(15_000L);
////        presenceEmitters.computeIfAbsent(noteId, k -> new CopyOnWriteArrayList<>()).add(emitter);
////
////        emitter.onCompletion(() -> removeEmitter(noteId, emitter));
////        emitter.onTimeout(() -> removeEmitter(noteId, emitter));
////        emitter.onError((e) -> removeEmitter(noteId, emitter));
////
////        try {
////            emitter.send(SseEmitter.event().name("connection").data("Connected to SSE"));
////        } catch (IOException e) {
////            emitter.complete();
////        }
////
////        // Heartbeat every 30s
////        scheduler.scheduleAtFixedRate(() -> {
////            try {
////                emitter.send(SseEmitter.event().name("ping").data("Still connected"));
////            } catch (IOException e) {
////                emitter.complete();
////            }
////        }, 10, 10, TimeUnit.SECONDS);
////
////        return emitter;
////    }
//
//    @GetMapping("/subscribe/{noteId}")
//    public SseEmitter subscribeToPresence(@PathVariable UUID noteId) {
//        SseEmitter emitter = new SseEmitter(0L);
//        presenceEmitters.computeIfAbsent(noteId, k -> new CopyOnWriteArrayList<>()).add(emitter);
//
//        emitter.onCompletion(() -> removeEmitter(noteId, emitter));
//        emitter.onTimeout(() -> removeEmitter(noteId, emitter));
//        emitter.onError((e) -> removeEmitter(noteId, emitter));
//
//        try {
//            emitter.send(SseEmitter.event().name("connection").data("Connected to SSE"));
////            Thread.sleep(30000);
//        } catch (IOException e) {
//            emitter.complete();
//        }
//        // Heartbeat every 30s
//        scheduler.scheduleAtFixedRate(() -> {
//            try {
//                emitter.send(SseEmitter.event().name("ping").data("Still connected"));
//            } catch (IOException e) {
//                emitter.complete();
//            }
//        }, 10, 10, TimeUnit.SECONDS);
//        return emitter;
//    }
//
////    private void removeEmitter(UUID noteId, SseEmitter emitter) {
////        presenceEmitters.getOrDefault(noteId, new CopyOnWriteArrayList<>()).remove(emitter);
////    }
//    /**
//     * Notify all SSE subscribers about the current presence for a note.
//     */
////    public void notifyPresenceUpdate(UUID noteId) {
////        String key = noteId.toString();
////        List<SseEmitter> emitters = presenceEmitters.getOrDefault(noteId, new CopyOnWriteArrayList<>());
////        Set<String> users = presenceService.getUsersViewing(key);
////
////        logger.info("Notifying presence update for note {}: {}", noteId, users);
////
////        List<SseEmitter> failedEmitters = new ArrayList<>();
////        for (SseEmitter emitter : emitters) {
////            try {
////                emitter.send(SseEmitter.event().name("user-presence").data(users));
////            } catch (IOException e) {
////                logger.error("Failed to send SSE update for note {}: {}", noteId, e.getMessage());
////                failedEmitters.add(emitter);
////                emitter.complete();
////            }
////        }
////        emitters.removeAll(failedEmitters);
////        if (emitters.isEmpty()) {
////            presenceEmitters.remove(noteId);
////        }
////    }
//
//    public void notifyPresenceUpdate(UUID noteId) {
//        String key = noteId.toString();
//        List<SseEmitter> emitters = presenceEmitters.getOrDefault(noteId, new CopyOnWriteArrayList<>());
//        Set<String> users = presenceService.getUsersViewing(key);
//
//        logger.info("Notifying presence update for note {}: {}", noteId, users);
//
//        // Use a thread-safe list to collect emitters that fail.
//        List<SseEmitter> failedEmitters = new ArrayList<>();
//
//        // For each emitter, send the event asynchronously.
//        for (SseEmitter emitter : emitters) {
//            CompletableFuture.runAsync(() -> {
//                try {
//                    emitter.send(SseEmitter.event().name("user-presence").data(users));
//                } catch (IOException e) {
//                    logger.error("Failed to send SSE update for note {}: {}", noteId, e.getMessage());
//                    failedEmitters.add(emitter);
//                    emitter.complete();
//                }
//            });
//        }
//
//        // Remove the failed emitters from the list.
//        emitters.removeAll(failedEmitters);
//        if (emitters.isEmpty()) {
//            presenceEmitters.remove(noteId);
//        }
//    }
//
//
//    /**
//     * Remove a disconnected SSE subscriber.
//     */
//    private void removeEmitter(UUID noteId, SseEmitter emitter) {
//        presenceEmitters.computeIfPresent(noteId, (key, emitters) -> {
//            emitters.remove(emitter);
//            return emitters.isEmpty() ? null : emitters;
//        });
////        notifyPresenceUpdate(noteId);
//    }
//}



//*************

package com.userPresence1.userPresence1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/presence")
public class PresenceController {

    private static final Logger logger = LoggerFactory.getLogger(PresenceController.class);
    private final PresenceService presenceService;
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> presenceEmitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateUserPresence(@RequestBody Map<String, String> request) {
        String noteIdStr = request.get("noteId");
        String username = request.get("username");

        if (noteIdStr == null || username == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
        }

        UUID noteId = UUID.fromString(noteIdStr);
        presenceService.addUser(noteId.toString(), username);
        notifyPresenceUpdate(noteId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Presence updated");
        response.put("users", presenceService.getUsersViewing(noteId.toString()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/remove")
    public ResponseEntity<Map<String, Object>> removeUser(@RequestBody Map<String, String> request) {
        String noteIdStr = request.get("noteId");
        String username = request.get("username");

        if (noteIdStr == null || username == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
        }

        UUID noteId = UUID.fromString(noteIdStr);
        presenceService.removeUser(noteId.toString(), username);
        notifyPresenceUpdate(noteId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User removed");
        response.put("users", presenceService.getUsersViewing(noteId.toString()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/subscribe/{noteId}")
    public SseEmitter subscribeToPresence(@PathVariable UUID noteId) {
        SseEmitter emitter = new SseEmitter(15000L);  // Timeout after 60 seconds
        presenceEmitters.computeIfAbsent(noteId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(noteId, emitter));
        emitter.onTimeout(() -> removeEmitter(noteId, emitter));
        emitter.onError((e) -> removeEmitter(noteId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connection").data("Connected to SSE"));
        } catch (IOException e) {
            emitter.complete();
        }

        // Heartbeat every 30 seconds
        scheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name("ping").data("Still connected"));
            } catch (IOException e) {
                emitter.complete();
            }
        }, 10, 10, TimeUnit.SECONDS);

        return emitter;
    }

    private void notifyPresenceUpdate(UUID noteId) {
        CopyOnWriteArrayList<SseEmitter> emitters = presenceEmitters.getOrDefault(noteId, new CopyOnWriteArrayList<>());
        Set<String> users = presenceService.getUsersViewing(noteId.toString());

        List<SseEmitter> failedEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("user-presence").data(users));
            } catch (IOException e) {
                failedEmitters.add(emitter);
                emitter.complete();
            }
        }

        emitters.removeAll(failedEmitters);
        if (emitters.isEmpty()) {
            presenceEmitters.remove(noteId);
        }
    }

    private void removeEmitter(UUID noteId, SseEmitter emitter) {
        presenceEmitters.computeIfPresent(noteId, (key, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}

