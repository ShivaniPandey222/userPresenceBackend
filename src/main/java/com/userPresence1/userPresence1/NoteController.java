package com.userPresence1.userPresence1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/notes")
@CrossOrigin(origins = {"http://localhost:4900", "http://localhost:4901", "http://localhost:4908", "http://localhost:4800", "http://localhost:4801"},
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class NoteController {

    private final NoteService noteService;
    private final Map<Long, String> notes = new ConcurrentHashMap<>();
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> noteEmitters = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(NoteController.class);

    public NoteController(NoteService noteService) {
        logger.info("Initializing NoteController");
        this.noteService = noteService;

        // Load existing notes into memory
        for (Note note : noteService.getAllNotes()) {
            notes.put(note.getId(), note.getContent());
        }
    }

    /** ✅ Get a note by ID */
    @GetMapping("/{noteId}")
    public ResponseEntity<Map<String, String>> getNote(@PathVariable Long noteId) {
        String content = notes.getOrDefault(noteId, "No content available.");
        return ResponseEntity.ok(Map.of(
                "title", "Note " + noteId,
                "content", content
        ));
    }

    /** ✅ Save note content */
    @PostMapping("/save")
    public ResponseEntity<String> saveNote(@RequestBody Map<String, String> request) {
        logger.info("🟢 Received API call: /save");

        Long noteId = Long.parseLong(request.get("noteId"));
        String content = request.get("content");

        if (noteId == null || content == null) {
            return ResponseEntity.badRequest().body("Invalid request data");
        }

        notes.put(noteId, content);

        // Update in DB
        Optional<Note> existingNote = noteService.getNote(noteId);
        if (existingNote.isPresent()) {
            Note note = existingNote.get();
            note.setContent(content);
            noteService.save(note);
        }

        notifyClients(noteId, content);
        return ResponseEntity.ok("Note saved successfully");
    }

    /** ✅ SSE subscribe */
    @GetMapping("/subscribe/{noteId}")
    public SseEmitter subscribeToNoteUpdates(@PathVariable Long noteId) {
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

    /** ✅ Notify clients about content update */
    @PostMapping("/notify")
    public ResponseEntity<String> notifyContentUpdate(@RequestBody Map<String, String> request) {
        Long noteId = Long.parseLong(request.get("noteId"));
        if (noteId != null) {
            notifyClients(noteId, notes.get(noteId));
            return ResponseEntity.ok("Users notified about content update");
        }
        return ResponseEntity.badRequest().body("Invalid request");
    }

    /** ✅ Send content updates */
    private void notifyClients(Long noteId, String updatedContent) {
        CopyOnWriteArrayList<SseEmitter> emitters = noteEmitters.getOrDefault(noteId, new CopyOnWriteArrayList<>());

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("content-update")
                        .data(Map.of("title", "Note " + noteId, "content", updatedContent)));
            } catch (IOException e) {
                logger.error("❌ Error sending SSE update: {}", e.getMessage());
                emitters.remove(emitter);
            }
        }
        if (emitters.isEmpty()) {
            noteEmitters.remove(noteId);
        }
    }

    /** ✅ Remove disconnected emitters */
    private void removeEmitter(Long noteId, SseEmitter emitter) {
        noteEmitters.computeIfPresent(noteId, (key, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }

    /** ✅ Create new note */
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createNote(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String content = request.get("content");

        if (title == null || content == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Title and content are required"));
        }

        Note savedNote = noteService.createNote(title, content);
        notes.put(savedNote.getId(), savedNote.getContent());

        notifyClients(savedNote.getId(), savedNote.getContent());
        broadcastNewNote(savedNote);

        return ResponseEntity.ok(
                Map.of(
                        "noteId", String.valueOf(savedNote.getId()),
                        "message", "Note created successfully"
                )
        );
    }

    private void broadcastNewNote(Note note) {
        for (Long noteId : noteEmitters.keySet()) {
            notifyClients(noteId, note.getContent());
        }
    }

    /** ✅ Get all notes */
    @GetMapping
    public ResponseEntity<List<Map<String, String>>> getAllNotes() {
        List<Map<String, String>> noteList = new ArrayList<>();

        // Example notes (optional: remove if DB-only)
        noteList.add(Map.of("id", "1", "title", "Note 1", "content", "Learn about components, services, and routing."));
        noteList.add(Map.of("id", "2", "title", "Note 2", "content", "Understanding controllers, services, and repositories."));
        noteList.add(Map.of("id", "3", "title", "Note 3", "content", "Real-time updates using Server-Sent Events."));

        // DB notes
        noteList.addAll(noteService.getAllNotes().stream()
                .map(note -> Map.of(
                        "id", note.getId().toString(),
                        "title", note.getTitle(),
                        "content", note.getContent()
                ))
                .toList());

        return ResponseEntity.ok(noteList);
    }
}
