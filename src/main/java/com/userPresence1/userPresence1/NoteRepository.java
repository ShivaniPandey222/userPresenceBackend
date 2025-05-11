package com.userPresence1.userPresence1;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface NoteRepository extends MongoRepository<Note, UUID> {
    // No additional methods needed – standard CRUD operations are provided by JpaRepository.
}
