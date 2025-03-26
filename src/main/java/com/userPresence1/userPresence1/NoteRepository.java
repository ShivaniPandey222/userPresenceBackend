package com.userPresence1.userPresence1;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {
    // No additional methods needed – standard CRUD operations are provided by JpaRepository.
}
