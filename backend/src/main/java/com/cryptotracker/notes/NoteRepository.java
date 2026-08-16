package com.cryptotracker.notes;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, UUID> {

    List<Note> findByUser_IdOrderByCreatedAtDesc(UUID userId);
}
