package com.userPresence1.userPresence1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/notes")
@CrossOrigin(origins = {"http://localhost:4900", "http://localhost:4901", "http://localhost:4908", "http://localhost:4800", "http://localhost:4801"})
public class NoteController {
    private final NoteService noteService;
    private final Map<String, String> notes = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> noteEmitters = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(NoteController.class);

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
        logger.info("Initializing NoteController...");

        // Load existing notes from DB into the in-memory map
        for (Note note : noteService.getAllNotes()) {
            notes.put(note.getId(), note.getContent());
        }
    }

    /** Get latest note content */
    @GetMapping("/{noteId}")
    public ResponseEntity<Map<String, String>> getNote(@PathVariable String noteId) {
        String content = notes.getOrDefault(noteId, "No content available.");
        return ResponseEntity.ok(Map.of(
                "title", "Note " + noteId,
                "content", content
        ));
    }

    /** Save updated note content */
    @PostMapping("/save")
    public ResponseEntity<String> saveNote(@RequestBody Map<String, String> request) {
        System.out.println("🟢 Received API call: /save");

        String noteId = request.get("noteId");
        String content = request.get("content");

        if (noteId == null || content == null) {
            return ResponseEntity.badRequest().body("Invalid request data");
        }

        notes.put(noteId, content);

        Optional<Note> existingNote = noteService.getNote(noteId);
        if (existingNote.isPresent()) {
            Note note = existingNote.get();
            note.setContent(content);
            noteService.save(note);
        }

        notifyClients(noteId, content);
        return ResponseEntity.ok("Note saved successfully");
    }

    /** Subscribe users to content updates */
    @GetMapping("/subscribe/{noteId}")
    public SseEmitter subscribeToNoteUpdates(@PathVariable String noteId) {
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

    /** Notify clients when content is updated */
    @PostMapping("/notify")
    public ResponseEntity<String> notifyContentUpdate(@RequestBody Map<String, String> request) {
        String noteId = request.get("noteId");
        if (noteId != null) {
            notifyClients(noteId, notes.get(noteId));
            return ResponseEntity.ok("Users notified about content update");
        }
        return ResponseEntity.badRequest().body("Invalid request");
    }

    /** Notify subscribers about updates */
    private void notifyClients(String noteId, String updatedContent) {
        CopyOnWriteArrayList<SseEmitter> emitters = noteEmitters.getOrDefault(noteId, new CopyOnWriteArrayList<>());

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("content-update")
                        .data(Map.of("title", "Note " + noteId, "content", updatedContent)));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }

        if (emitters.isEmpty()) {
            noteEmitters.remove(noteId);
        }
    }

    /** Remove disconnected subscribers */
    private void removeEmitter(String noteId, SseEmitter emitter) {
        noteEmitters.computeIfPresent(noteId, (key, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }

    /** Create a new note */
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

        return ResponseEntity.ok(
                Map.of(
                        "noteId", savedNote.getId(),
                        "message", "Note created successfully"
                )
        );
    }

    /** Get all notes */
    @GetMapping
    public ResponseEntity<List<Map<String, String>>> getAllNotes() {
        List<Map<String, String>> noteList = noteService.getAllNotes().stream()
                .map(note -> Map.of(
                        "id", note.getId(),
                        "title", note.getTitle(),
                        "content", note.getContent()
                ))
                .toList();

        return ResponseEntity.ok(noteList);
    }
}
