package ma.osbt.controller;

 

 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import ma.osbt.entitie.ConversationMessage;
import ma.osbt.entitie.Utilisateur;
import ma.osbt.repository.ConversationMessageRepository;
import ma.osbt.repository.UtilisateurRepository;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/psybot")
public class PsyBotController {

    @Autowired
    private ConversationMessageRepository repository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // GET /api/psybot/historique — nécessite d'être authentifié (JWT)
    @GetMapping("/historique")
    public ResponseEntity<List<MessageDto>> getHistorique(Authentication authentication) {
        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        List<MessageDto> historique = repository
                .findByUtilisateurIdOrderByDateEnvoiAsc(utilisateur.getId())
                .stream()
                .map(m -> new MessageDto(m.getId(), m.getSender(), m.getContenu(), m.getDateEnvoi()))
                .toList();

        return ResponseEntity.ok(historique);
    }

    // POST /api/psybot/message — enregistre un message (user ou bot) pour l'utilisateur connecté
    @PostMapping("/message")
    public ResponseEntity<MessageDto> enregistrerMessage(
            @RequestBody MessageRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        ConversationMessage message = new ConversationMessage();
        message.setUtilisateur(utilisateur);
        message.setSender(request.sender());
        message.setContenu(request.contenu());
        message.setDateEnvoi(LocalDateTime.now());

        ConversationMessage saved = repository.save(message);

        return ResponseEntity.ok(new MessageDto(saved.getId(), saved.getSender(), saved.getContenu(), saved.getDateEnvoi()));
    }

    public record MessageRequest(String sender, String contenu) {}
    public record MessageDto(Long id, String sender, String contenu, LocalDateTime dateEnvoi) {}
}