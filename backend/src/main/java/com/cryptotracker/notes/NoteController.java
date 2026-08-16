package com.cryptotracker.notes;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    public ResponseEntity<NoteResponse> create(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateNoteRequest request
    ) {
        NoteResponse response = noteService.createNote(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<NoteResponse> list(@AuthenticationPrincipal UUID userId) {
        return noteService.listNotesForUser(userId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id
    ) {
        noteService.deleteNote(userId, id);
        return ResponseEntity.noContent().build();
    }
}
