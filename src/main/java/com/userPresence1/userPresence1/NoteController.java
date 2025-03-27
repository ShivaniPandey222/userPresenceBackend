package com.userPresence1.userPresence1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@RestController
@RequestMapping("/notes")
//@CrossOrigin(origins = {"http://localhost:4900", "http://localhost:4901", "http://localhost:4908", "http://localhost:4800", "http://localhost:4801"})
public class NoteController {

    private final NoteService noteService;
    // In-memory cache: note ID (UUID) to content
    private final Map<UUID, String> notes = new ConcurrentHashMap<>();
    // Map for active SSE emitters, keyed by note ID
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> noteEmitters = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(NoteController.class);

    public NoteController(NoteService noteService) {
        logger.info("Initializing NoteController");
        this.noteService = noteService;

        // Load existing notes from the database into the in-memory map
        for (Note note : noteService.getAllNotes()) {
            notes.put(note.getId(), note.getContent());
        }
    }

    /** Get a note by ID */
    @GetMapping("/{noteId}")
    public ResponseEntity<Map<String, String>> getNote(@PathVariable UUID noteId) {
        Optional<Note> noteOptional = noteService.getNote(noteId);
        if (noteOptional.isPresent()) {
            Note note = noteOptional.get();
            return ResponseEntity.ok(Map.of(
                    "title", note.getTitle(),
                    "content", note.getContent()
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "title", "Note " + noteId.toString(),
                    "content", "No content available."
            ));
        }
    }

    /** Save updated note content */
    @PostMapping("/save")
    public ResponseEntity<String> saveNote(@RequestBody Map<String, String> request) {
        logger.info("🟢 Received API call: /save");

        if (!request.containsKey("noteId") || !request.containsKey("content")) {
            return ResponseEntity.badRequest().body("Invalid request data");
        }

        UUID noteId;

            noteId = UUID.fromString(request.get("noteId"));


        String content = request.get("content");
        notes.put(noteId, content);

        // Update the note in the database if it exists
        Optional<Note> existingNote = noteService.getNote(noteId);
        if (existingNote.isPresent()) {
            Note note = existingNote.get();
            note.setContent(content);
            noteService.save(note);
        }

//        notifyClients(noteId, content);
        return ResponseEntity.ok("Note saved successfully");
    }

    /** Subscribe users to note updates via SSE */
    @GetMapping("/subscribe/{noteId}")
    public SseEmitter subscribeToNoteUpdates(@PathVariable UUID noteId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        noteEmitters.computeIfAbsent(noteId, k -> new CopyOnWriteArrayList<>()).add(emitter);

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

    /** Notify clients about content updates */
    @PostMapping("/notify")
    public ResponseEntity<String> notifyContentUpdate(@RequestBody Map<String, String> request) {
        if (!request.containsKey("noteId")) {
            return ResponseEntity.badRequest().body("Invalid request");
        }

        UUID noteId;
        try {
            noteId = UUID.fromString(request.get("noteId"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid note ID format");
        }

        notifyClients(noteId, notes.get(noteId));
        return ResponseEntity.ok("Users notified about content update");
    }

    /** Send SSE updates to all subscribers of a note */
//    private void notifyClients(UUID noteId, String updatedContent) {
//        CopyOnWriteArrayList<SseEmitter> emitters = noteEmitters.getOrDefault(noteId, new CopyOnWriteArrayList<>());
//        List<SseEmitter> failedEmitters = new ArrayList<>();
//
//        for (SseEmitter emitter : emitters) {
//            try {
//                emitter.send(SseEmitter.event()
//                        .name("content-update")
//                        .data(Map.of("title", "Note " + noteId.toString(), "content", updatedContent)));
//            } catch (IOException e) {
//                logger.error("❌ Error sending SSE update: {}", e.getMessage());
//                failedEmitters.add(emitter);
//            }
//        }
//
//        emitters.removeAll(failedEmitters);
//        if (emitters.isEmpty()) {
//            noteEmitters.remove(noteId);
//        }
//    }

    private void notifyClients(UUID noteId, String updatedContent) {
        CopyOnWriteArrayList<SseEmitter> emitters = noteEmitters.getOrDefault(noteId, new CopyOnWriteArrayList<>());
        List<SseEmitter> failedEmitters = new ArrayList<>();
        Map<String, String> data = Map.of(
                "title", "Note " + noteId.toString(),
                "content", updatedContent
        );

        // Send SSE events asynchronously to avoid blocking the HTTP request
        for (SseEmitter emitter : emitters) {
            CompletableFuture.runAsync(() -> {
                try {
                    emitter.send(SseEmitter.event().name("content-update").data(data));
                } catch (IOException e) {
                    logger.error("❌ Error sending SSE update for note {}: {}", noteId, e.getMessage());
                    failedEmitters.add(emitter);
                    emitter.complete();
                }
            });
        }

        emitters.removeAll(failedEmitters);
        if (emitters.isEmpty()) {
            noteEmitters.remove(noteId);
        }
    }


    /** Remove disconnected SSE subscribers */
    private void removeEmitter(UUID noteId, SseEmitter emitter) {
        noteEmitters.computeIfPresent(noteId, (key, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }

    /** Create a new note */
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createNote(@RequestBody Map<String, String> request) {
        if (!request.containsKey("title") || !request.containsKey("content")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Title and content are required"));
        }

        String title = request.get("title");
        String content = request.get("content");

        Note savedNote = noteService.createNote(title, content);
        notes.put(savedNote.getId(), savedNote.getContent());

        notifyClients(savedNote.getId(), savedNote.getContent());
        broadcastNewNote(savedNote);

        return ResponseEntity.ok(Map.of(
                "noteId", savedNote.getId().toString(),
                "message", "Note created successfully"
        ));
    }

    private void broadcastNewNote(Note note) {
        Map<String, String> noteData = Map.of(
                "id", note.getId().toString(),
                "title", note.getTitle(),
                "content", note.getContent()
        );
        for (SseEmitter emitter : globalEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("new-note")
                        .data(noteData));
            } catch (IOException e) {
                emitter.complete();
                globalEmitters.remove(emitter);
            }
        }
    }

    /** Get all notes */
    @GetMapping
    public ResponseEntity<List<Map<String, String>>> getAllNotes() {
        List<Map<String, String>> noteList = new ArrayList<>();

        // Optional hardcoded notes for demonstration
        noteList.add(Map.of(
                "id", "eab35bf6-1430-417d-95fd-2568b6b94098",
                "title", "Note 1",
                "content", "Learn about components, services, and routing."
        ));
        noteList.add(Map.of(
                "id", "dc23fa4a-40e6-48cd-bdd2-a7ded358db2d",
                "title", "Note 2",
                "content", "Understanding controllers, services, and repositories."
        ));
        noteList.add(Map.of(
                "id", "ce61491d-b41f-4c85-9d7c-95418db32205",
                "title", "Note 3",
                "content", "Real-time updates using Server-Sent Events."
        ));

        // Add notes from the database
        noteService.getAllNotes().forEach(note ->
                noteList.add(Map.of(
                        "id", note.getId().toString(),
                        "title", note.getTitle(),
                        "content", note.getContent()
                ))
        );
        return ResponseEntity.ok(noteList);
    }

    // In your NoteController class, add a global list for SSE emitters:
    private final List<SseEmitter> globalEmitters = new CopyOnWriteArrayList<>();

    // Add a new endpoint to subscribe to global new-note events:
    @GetMapping("/subscribeNew/global")
    public SseEmitter subscribeToGlobalNotes() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        globalEmitters.add(emitter);

        emitter.onCompletion(() -> globalEmitters.remove(emitter));
        emitter.onTimeout(() -> globalEmitters.remove(emitter));
        emitter.onError(e -> globalEmitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("connection").data("Connected to global SSE"));
        } catch (IOException e) {
            emitter.complete();
        }
        return emitter;
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<String> deleteNote(@PathVariable UUID noteId) {
        // Remove from database
        noteService.deleteNote(noteId);
        // Remove from in-memory cache
        notes.remove(noteId);
        // Optionally, notify SSE subscribers if needed (e.g., to update the global list)
        return ResponseEntity.ok("Note deleted successfully");
    }
}
