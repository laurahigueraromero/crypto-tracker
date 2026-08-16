package com.cryptotracker.notes;

import com.cryptotracker.common.HtmlSanitizer;
import com.cryptotracker.users.UserRepository;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public NoteService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    public NoteResponse createNote(UUID userId, CreateNoteRequest request) {
        Note note = new Note();
        note.setUser(userRepository.getReferenceById(userId));
        note.setTitle(HtmlSanitizer.escape(request.title()));
        note.setContent(HtmlSanitizer.escape(request.content()));
        note.setType(NoteType.valueOf(request.type()));
        note.setCoinIds(new LinkedHashSet<>(request.coinIds()));
        if (request.tags() != null) {
            note.setTags(new LinkedHashSet<>(request.tags()));
        }

        Note saved = noteRepository.save(note);
        return NoteResponse.from(saved);
    }
}
