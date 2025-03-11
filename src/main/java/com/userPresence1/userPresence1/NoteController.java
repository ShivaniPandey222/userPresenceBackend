package com.userPresence1.userPresence1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/notes")
@CrossOrigin(origins = {"http://localhost:4900", "http://localhost:4901", "http://localhost:4902","http://localhost:4800","http://localhost:4801"},
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class NoteController {

    private final Map<String, String> notes = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> noteEmitters = new ConcurrentHashMap<>();


    public NoteController() {
        // Sample Notes
        notes.put("note-1", "Learn about components, services, and routing.");
        notes.put("note-2", "Understanding controllers, services, and repositories.");
        notes.put("note-3", "Real-time updates using Server-Sent Events.");
    }

    /** ✅ Get latest note content */
    @GetMapping("/{noteId}")
    public ResponseEntity<Map<String, String>> getNote(@PathVariable String noteId) {
        String content = notes.getOrDefault(noteId, "No content available.");
//        return ResponseEntity.ok(Collections.singletonMap("content", content));
        return ResponseEntity.ok(Map.of(
                "title", "Note " + noteId,  // ✅ Adding title
                "content", content
        ));
    }

    /** ✅ Save updated note content */
    @PostMapping("/save")
    public ResponseEntity<String> saveNote(@RequestBody Map<String, String> request) {
        System.out.println("🟢 Received API call: /save");
        String noteId = request.get("noteId");
        String content = request.get("content");

        if (noteId == null || content == null) {
            return ResponseEntity.badRequest().body("Invalid request data");
        }

        notes.put(noteId, content);
        notifyClients(noteId, content);

        System.out.println("✅ Note saved: " + noteId + " -> " + content);
        return ResponseEntity.ok("Note saved successfully");
    }

    /** ✅ Subscribe users to content updates */
    @GetMapping("/subscribe/{noteId}")
    public SseEmitter subscribeToNoteUpdates(@PathVariable String noteId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        noteEmitters.computeIfAbsent(noteId,k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(noteId, emitter));
        emitter.onTimeout(() -> removeEmitter(noteId, emitter));
        emitter.onError((e) -> removeEmitter(noteId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connection").data("Connected to SSE"));
        } catch (IOException e) {
            emitter.complete();
        }
        return emitter;
    }


    /** ✅ Notify clients when content is updated */
    @PostMapping("/notify")
    public ResponseEntity<String> notifyContentUpdate(@RequestBody Map<String, String> request) {
        String noteId = request.get("noteId");
        if (noteId != null) {
            notifyClients(noteId, notes.get(noteId));
            return ResponseEntity.ok("Users notified about content update");
        }
        return ResponseEntity.badRequest().body("Invalid request");
    }

    /** ✅ Send content updates to all subscribers */
    private void notifyClients(String noteId, String updatedContent) {
        CopyOnWriteArrayList<SseEmitter> emitters = noteEmitters.getOrDefault(noteId, new CopyOnWriteArrayList<>());

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("content-update")
                        .data(Map.of("title", "Note " + noteId, "content", updatedContent)));
            } catch (IOException e) {
                System.err.println("❌ Error sending SSE update: " + e.getMessage());
                emitters.remove(emitter); // ✅ Safe removal of disconnected clients
            }
        }

        if (emitters.isEmpty()) {
            noteEmitters.remove(noteId);
        }
    }


    /** ✅ Remove disconnected SSE subscribers */
    private void removeEmitter(String noteId, SseEmitter emitter) {
        noteEmitters.computeIfPresent(noteId, (key, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }

}
