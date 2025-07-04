package ma.osbt.controller;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import ma.osbt.entitie.Consultation;
import ma.osbt.entitie.Message;
import ma.osbt.entitie.Personne;
import ma.osbt.service.MessageService;
import ma.osbt.repository.ConsultationRepository;
import ma.osbt.repository.MessageRepository;
import ma.osbt.repository.PersonneRepository;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private PersonneRepository personneRepository;
    @Autowired
    private ConsultationRepository consultationRepository;
   

    // Récupérer l'utilisateur connecté (instance de Utilisateur, hérite de Personne)
    private Personne getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Auth principal class: " + authentication.getPrincipal().getClass());
        System.out.println("Auth principal: " + authentication.getPrincipal());
        if(authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        return (Personne) authentication.getPrincipal();
    }

    // Lister les messages de la consultation (uniquement pour le client et le pro)
    @GetMapping("/consultation/{id}")
    public List<Message> getMessages(@PathVariable Long id) {
        Personne currentUser = getCurrentUser();
        Consultation consultation = consultationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consultation introuvable"));

        // Vérification d’appartenance par comparaison des IDs
        boolean isParticipant = consultation.getReservation().getUtilisateur().getId().equals(currentUser.getId())
                || consultation.getProfessionnel().getId().equals(currentUser.getId());

        if (!isParticipant) {
            throw new AccessDeniedException("Accès refusé : vous ne faites pas partie de cette consultation");
        }

        return messageRepository.findByConsultationOrderByDateAsc(consultation);
    }

    // Envoyer un message dans la consultation
    @PostMapping("/consultation/{id}")
    public Message sendMessage(
            @PathVariable Long id,
            @RequestParam String contenu,
            @RequestParam(required = false, defaultValue = "false") boolean anonymat
    ) {
        Personne currentUser = getCurrentUser();
        Consultation consultation = consultationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Consultation introuvable"));

        Personne destinataire;

        if (consultation.getReservation().getUtilisateur().getId().equals(currentUser.getId())) {
            destinataire = consultation.getProfessionnel();
        } else if (consultation.getProfessionnel().getId().equals(currentUser.getId())) {
            destinataire = consultation.getReservation().getUtilisateur();
        } else {
            throw new AccessDeniedException("Vous ne faites pas partie de cette consultation");
        }

        Message message = new Message();
        message.setDate(new Date());
        message.setHeure(LocalTime.now());
        message.setContenu(contenu);
        message.setAnonymat(anonymat);
        message.setExpediteur(currentUser);
        message.setDestinataire(destinataire);
        message.setConsultation(consultation);

        return messageRepository.save(message);
    }

    

    @PostMapping("/envoyer/{destinataireId}")
    public ResponseEntity<?> envoyerMessage(
            @PathVariable Long destinataireId,
            @RequestBody Message message,
            @AuthenticationPrincipal Personne expediteur) {

        Personne destinataire = personneRepository.findById(destinataireId).orElse(null);
        if (destinataire == null) {
            return ResponseEntity.badRequest().body("Destinataire introuvable");
        }

        message.setDate(new Date());
        message.setHeure(LocalTime.now());
        message.setExpediteur(expediteur);
        message.setDestinataire(destinataire);

        try {
            Message saved = messageService.envoyerMessage(message);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Message inapproprié détecté");
        }
    }

    // ✅ Lister les messages entre l'utilisateur connecté et un autre utilisateur
    @GetMapping("/entre/{autreId}")
    public ResponseEntity<?> getMessagesAvec(
            @PathVariable Long autreId,
            @AuthenticationPrincipal Personne utilisateur) {

        Personne autre = personneRepository.findById(autreId).orElse(null);
        if (autre == null) return ResponseEntity.notFound().build();

        List<Message> messages = messageService.getMessagesEntre(utilisateur, autre);

        List<Map<String, Object>> resultats = messages.stream().map(msg -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", msg.getId());
            map.put("date", msg.getDate());
            map.put("heure", msg.getHeure());
            map.put("contenu", msg.getContenu());
            map.put("anonymat", msg.isAnonymat());

            // Affichage masqué si anonymat = true
            if (msg.isAnonymat()) {
                map.put("expediteur", "Anonyme");
            } else {
                map.put("expediteur", msg.getExpediteur().getNom()); // ou getEmail() ou autre
            }

            map.put("destinataire", msg.getDestinataire().getNom());
            return map;
        }).toList();

        return ResponseEntity.ok(resultats);
    }

    // ✅ Tous les messages pour l'utilisateur connecté (expéditeur OU destinataire)
    @GetMapping("/moi")
    public ResponseEntity<?> getMesMessages(@AuthenticationPrincipal Personne moi) {
        List<Message> messages = messageService.getMessagesPourUtilisateur(moi);

        List<Map<String, Object>> resultats = messages.stream().map(msg -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", msg.getId());
            map.put("date", msg.getDate());
            map.put("heure", msg.getHeure());
            map.put("contenu", msg.getContenu());
            map.put("anonymat", msg.isAnonymat());

            if (msg.isAnonymat() && msg.getExpediteur().getId().equals(moi.getId())) {
                map.put("expediteur", "Moi (Anonyme)");
            } else if (msg.isAnonymat()) {
                map.put("expediteur", "Anonyme");
            } else {
                map.put("expediteur", msg.getExpediteur().getNom());
            }

            map.put("destinataire", msg.getDestinataire().getNom());
            return map;
        }).toList();

        return ResponseEntity.ok(resultats);
    }
    @GetMapping("/admin/tous")
    public ResponseEntity<?> getTousLesMessages() {
        List<Message> messages = messageService.getTousMessages()
            .stream()
            .filter(msg -> msg.getConsultation() == null)  
            .toList();

        List<Map<String, Object>> resultats = messages.stream().map(msg -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", msg.getId());
            map.put("date", msg.getDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toString());
            map.put("heure", msg.getHeure().toString());
            map.put("contenu", msg.getContenu());
            map.put("inapproprie", msg.isInapproprie());
            map.put("anonymat", msg.isAnonymat());
            map.put("expediteur", msg.isAnonymat() ? "Anonyme" : msg.getExpediteur().getNom());
            map.put("destinataire", msg.getDestinataire() != null ? msg.getDestinataire().getNom() : "Public");
            return map;
        }).toList();

        return ResponseEntity.ok(resultats);
    }

    @DeleteMapping("/admin/supprimer/{id}")
    public ResponseEntity<?> supprimerMessage(@PathVariable Long id) {
        if (!messageRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        messageRepository.deleteById(id);
        return ResponseEntity.ok("Message supprimé");
    }
     
    
   
    
}
