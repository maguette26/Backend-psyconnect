package ma.osbt.controller;

import lombok.RequiredArgsConstructor;
import ma.osbt.dto.MessageDTO;
import ma.osbt.entitie.Personne;
import ma.osbt.repository.PersonneRepository;
import ma.osbt.service.implementation.ChatService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;
    private final PersonneRepository personneRepository;

    @GetMapping("/{consultationId}/history")
    public ResponseEntity<List<MessageDTO>> getHistory(
            @PathVariable Long consultationId,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        if (consultationId == null || consultationId <= 0) {
            return ResponseEntity.badRequest().build();
        }

        Personne personne = personneRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        List<MessageDTO> history =
                chatService.getHistory(consultationId, personne.getId());

        return ResponseEntity.ok(history);
    }
}