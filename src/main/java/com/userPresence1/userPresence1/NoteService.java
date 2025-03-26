package com.userPresence1.userPresence1;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NoteService {
    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public Optional<Note> getNote(UUID id) {
        return noteRepository.findById(id);
    }

    public Note save(Note note) {
        return noteRepository.save(note);
    }

    public Note createNote(String title, String content) {
        // Generate a new UUID for the note
        Note note = Note.builder()
                .id(UUID.randomUUID())
                .title(title)
                .content(content)
                .build();
        return noteRepository.save(note);
    }

    public List<Note> getAllNotes() {
        return noteRepository.findAll();
    }
}
