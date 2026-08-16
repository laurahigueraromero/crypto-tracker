package com.cryptotracker.notes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NoteResponse(
        UUID id,
        String title,
        String content,
        NoteType type,
        List<String> coinIds,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt
) {
    public static NoteResponse from(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getType(),
                List.copyOf(note.getCoinIds()),
                List.copyOf(note.getTags()),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
