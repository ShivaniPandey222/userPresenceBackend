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
@CrossOrigin(origins = {"http://localhost:4900", "http://localhost:4901", "http://localhost:4908","http://localhost:4800","http://localhost:4801"},
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class NoteController {
    NoteService noteService;
    private final Map<String, String> notes = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> noteEmitters = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(NoteController.class);

    public NoteController(NoteService noteService) {
        logger.info("heloo");
        // Sample Notes
        notes.put("1", "Learn about components, services, and routing.");
        notes.put("2", "Understanding controllers, services, and repositories.");
        notes.put("3", "Real-time updates using Server-Sent Events.");
        this.noteService = noteService;

        // ✅ Load existing notes from DB into the in-memory map
        for (Note note : noteService.getAllNotes()) {
            notes.put(note.getId(), note.getContent());
        }
    }

    /** ✅ Get latest note content */
    @GetMapping("/{noteId}")
    public ResponseEntity<Map<String, String>> getNote(@PathVariable String noteId) {
//        String noteIdStr = noteId.replaceAll("[^0-9]", ""); // removes non-numeric chars
//        Long noteIdd=Long.parseLong(noteIdStr);
        String content = notes.getOrDefault(noteId, "No content available.");
//        return ResponseEntity.ok(Collections.singletonMap("content", content));
        return ResponseEntity.ok(Map.of(
                "title", "Note " + noteId,  // ✅ Adding title
                "content", content
        ));
    }

    /** ✅ Save updated note content */
//    @PostMapping("/save")
//    public ResponseEntity<String> saveNote(@RequestBody Map<String, String> request) {
//        System.out.println("🟢 Received API call: /save");
//        String noteId = request.get("noteId");
//        String content = request.get("content");
//
//        if (noteId == null || content == null) {
//            return ResponseEntity.badRequest().body("Invalid request data");
//        }
//
//        notes.put(noteId, content);
//        notifyClients(noteId, content);
//
//        System.out.println("✅ Note saved: " + noteId + " -> " + content);
//        return ResponseEntity.ok("Note saved successfully");
//    }

    @PostMapping("/save")
    public ResponseEntity<String> saveNote(@RequestBody Map<String, String> request) {
        System.out.println("🟢 Received API call: /save");

        String noteId = request.get("noteId")       ;
        String content = request.get("content");

        if (noteId == null || content == null) {
            return ResponseEntity.badRequest().body("Invalid request data");
        }
        String noteIdStr = noteId.replaceAll("[^0-9]", ""); // removes non-numeric chars
        Long noteIdd=Long.parseLong(noteIdStr);
        // ✅ Update the in-memory map
        notes.put(noteId, content);

        System.out.println("entering db");
        // ✅ Update in database
        Optional<Note> existingNote = noteService.getNote(Long.parseLong(noteIdStr));
        if (existingNote.isPresent()) {
            Note note = existingNote.get();
            note.setContent(content);
            noteService.save(note);
        }

        System.out.println("db updated");
        // Notify clients
//        notifyClients(noteIdd, content);
        return ResponseEntity.ok("Note saved successfully");
    }


    /** ✅ Subscribe users to content updates */
    @GetMapping("/subscribe/{noteId}")
    public SseEmitter subscribeToNoteUpdates(@PathVariable String noteId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
//        String noteIdStr = noteId.replaceAll("[^0-9]", ""); // removes non-numeric chars
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
//        String noteIdStr = noteId.replaceAll("[^0-9]", ""); // removes non-numeric chars
//        Long noteIdd=Long.parseLong(noteIdStr);
        if (noteId != null) {
            notifyClients(noteId, notes.get(noteId));
            return ResponseEntity.ok("Users notified about content update");
        }
        return ResponseEntity.badRequest().body("Invalid request");
    }

    /** ✅ Send content updates to all subscribers */
    private void notifyClients(String noteId, String updatedContent) {
//        String noteIdStr = noteId.replaceAll("[^0-9]", ""); // removes non-numeric chars
//        Long noteIdd=Long.parseLong(noteIdStr);
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
//        String noteIdStr = noteId.replaceAll("[^0-9]", ""); // removes non-numeric chars
//        Long noteIdd=Long.parseLong(noteIdd);
        noteEmitters.computeIfPresent(noteId, (key, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createNote(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String content = request.get("content");

        if (title == null || content == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Title and content are required"));
        }

        // ✅ Save to database
        Note savedNote = noteService.createNote(title, content);

        // ✅ Store in the in-memory map
        notes.put(savedNote.getId(), savedNote.getContent());

        // Notify users with title and content
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
        for (String noteId : noteEmitters.keySet()) {
//            String noteIdStr = noteId.replaceAll("[^0-9]", ""); // removes non-numeric chars
//            Long noteIdd=Long.parseLong(noteIdStr);
            notifyClients(noteId, note.getContent());
        }
    }

    //doubt
    @GetMapping
    public ResponseEntity<List<Map<String, String>>> getAllNotes() {

//        List<Map<String, String>> noteList = noteService.getAllNotes().stream()
//                .map(note -> Map.of(
//                        "id", note.getId().toString(),
//                        "title", note.getTitle(),
//                        "content", note.getContent()
//                ))
//                .toList();

        List<Map<String, String>> noteList = new ArrayList<>();
        // ✅ Add hardcoded notes
        noteList.add(Map.of("id", "1", "title", "Note 1", "content", "Learn about components, services, and routing."));
        noteList.add(Map.of("id", "2", "title", "Note 2", "content", "Understanding controllers, services, and repositories."));
        noteList.add(Map.of("id", "3", "title", "Note 3", "content", "Real-time updates using Server-Sent Events."));

        // ✅ Add notes from the database
        noteList.addAll(noteService.getAllNotes().stream()
                .map(note -> Map.of(
                        "id", note.getId().toString().replaceAll("[^0-9]", ""),
                        "title", note.getTitle(),
                        "content", note.getContent()
                ))
                .toList());

        return ResponseEntity.ok(noteList);
    }






}
