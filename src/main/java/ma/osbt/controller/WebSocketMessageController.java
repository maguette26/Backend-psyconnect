package ma.osbt.controller;

import java.time.LocalTime;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import ma.osbt.entitie.Message;
import ma.osbt.repository.PersonneRepository;

@Controller
public class WebSocketMessageController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private PersonneRepository personneRepository;

    private final java.util.List<String> motsInterdits = java.util.List.of("suicide", "haine", "violence", "insulte");

    private boolean estInapproprie(String contenu) {
        if (contenu == null) return false;
        String lower = contenu.toLowerCase();
        return motsInterdits.stream().anyMatch(lower::contains);
    }

    @Autowired
    private ma.osbt.repository.ConsultationRepository consultationRepository;

    @MessageMapping("/send") // Le client envoie à /app/send
    public void envoyerMessage(
            Message message,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        // 1. Refuser si le contenu est inapproprié
        if (estInapproprie(message.getContenu())) return;

        // 2. Vérifie que les champs essentiels sont là
        if (message.getDestinataire() == null || message.getDestinataire().getId() == null
            || message.getExpediteur() == null || message.getExpediteur().getId() == null) {
            return;
        }

        // 3. Vérifie que l'utilisateur connecté est bien l'expéditeur
        String emailConnecte = SecurityContextHolder.getContext().getAuthentication().getName();

        var expediteur = personneRepository.findById(message.getExpediteur().getId()).orElse(null);
        if (expediteur == null || !expediteur.getEmail().equals(emailConnecte)) {
            return; // Refusé : tentative d'usurpation d'identité
        }

        // 4. Vérifie que le destinataire existe
        var destinataire = personneRepository.findById(message.getDestinataire().getId()).orElse(null);
        if (destinataire == null) return;

        // 5. Ajoute la date/heure actuelle
        message.setDate(new Date());
        message.setHeure(LocalTime.now());

        // 6. Envoi au destinataire
        messagingTemplate.convertAndSendToUser(
                destinataire.getEmail(), // "/user/{email}/queue/messages"
                "/queue/messages",
                message
        );

        // 7. Optionnel : envoi aussi à l'expéditeur
        messagingTemplate.convertAndSendToUser(
                expediteur.getEmail(),
                "/queue/messages",
                message
        );
    }


}
