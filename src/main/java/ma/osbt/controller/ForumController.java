package ma.osbt.controller;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import ma.osbt.entitie.Message;
import ma.osbt.entitie.Personne;
import ma.osbt.entitie.ReponseForum;
import ma.osbt.entitie.SujetForum;
import ma.osbt.repository.ReponseForumRepository;
import ma.osbt.service.SujetForumService;

@RestController
@RequestMapping("/api/forum")
public class ForumController {

    @Autowired
    private SujetForumService sujetService;

    @Autowired
    private ReponseForumRepository reponseRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final List<String> motsInterdits = List.of("suicide", "haine", "violence", "insulte");

    private boolean estInapproprie(String contenu) {
        if (contenu == null) return false;
        String lower = contenu.toLowerCase();
        return motsInterdits.stream().anyMatch(lower::contains);
    }

    @GetMapping("/sujets")
    public List<SujetForum> getAllSujets() {
        return sujetService.listerSujets();
    }

    @PostMapping("/sujets")
    public ResponseEntity<?> creerSujet(@RequestBody SujetForum sujet,
                                        @AuthenticationPrincipal Personne auteur) {
        sujet.setAuteur(auteur);
        sujet.setDateCreation(LocalDateTime.now());

        if (estInapproprie(sujet.getContenu())) {
            Message alert = new Message();
            alert.setContenu("⚠️ Contenu inapproprié détecté dans un sujet par " + auteur.getNom());
            alert.setDate(new Date());
            alert.setHeure(LocalTime.now());

            messagingTemplate.convertAndSend("/topic/messages", alert);
            return ResponseEntity.badRequest().body("Contenu inapproprié détecté");
        }

        return ResponseEntity.ok(sujetService.creerSujet(sujet));
    }

    @GetMapping("/sujets/reponses/{id}")
    public List<ReponseForum> getReponses(@PathVariable Long id) {
        return reponseRepo.findBySujetIdOrderByDateReponseAsc(id);
    }
    @PostMapping("/sujets/reponses/{id}")
    public ResponseEntity<?> repondre(@PathVariable Long id,
                                      @RequestBody ReponseForum reponse,
                                      @AuthenticationPrincipal Personne auteur) {

        SujetForum sujet = sujetService.getSujet(id)
                .orElseThrow(() -> new RuntimeException("Sujet non trouvé"));

        if (estInapproprie(reponse.getMessage())) {
            Message alert = new Message();
            alert.setContenu("⚠️ Contenu inapproprié détecté dans une réponse par " + auteur.getNom());
            alert.setDate(new Date());
            alert.setHeure(LocalTime.now());

            messagingTemplate.convertAndSend("/topic/messages", alert);
            return ResponseEntity.badRequest().body("Contenu inapproprié détecté");
        }

        reponse.setSujet(sujet);
        reponse.setAuteur(auteur);
        reponse.setDateReponse(LocalDateTime.now());

        // Sauvegarde de la réponse
        ReponseForum saved = reponseRepo.save(reponse);

        // Mise à jour de la collection du sujet
        sujet.getReponses().add(saved);

        // Sauvegarde du sujet
        sujetService.creerSujet(sujet);

        return ResponseEntity.ok(saved);
    }
    // Modifier un sujet
    @PutMapping("/sujets/{id}")
    public ResponseEntity<?> modifierSujet(@PathVariable Long id,
                                           @RequestBody SujetForum sujetModifie,
                                           @AuthenticationPrincipal Personne utilisateur) {
        SujetForum sujet = sujetService.getSujet(id)
                .orElseThrow(() -> new RuntimeException("Sujet non trouvé"));

        if (!sujet.getAuteur().getId().equals(utilisateur.getId())) {
            return ResponseEntity.status(403).body("Non autorisé à modifier ce sujet.");
        }

        sujet.setTitre(sujetModifie.getTitre());
        sujet.setContenu(sujetModifie.getContenu());
        return ResponseEntity.ok(sujetService.creerSujet(sujet));  
    }

    // Supprimer un sujet
    @DeleteMapping("/sujets/{id}")
    public ResponseEntity<?> supprimerSujet(@PathVariable Long id,
                                            @AuthenticationPrincipal Personne utilisateur) {
        SujetForum sujet = sujetService.getSujet(id)
                .orElseThrow(() -> new RuntimeException("Sujet non trouvé"));

        if (!sujet.getAuteur().getId().equals(utilisateur.getId())) {
            return ResponseEntity.status(403).body("Non autorisé à supprimer ce sujet.");
        }

        sujetService.supprimerSujet(id);
        return ResponseEntity.ok("Sujet supprimé avec succès.");
    }

    // Modifier une réponse
    @PutMapping("/sujets/reponses/modifier/{reponseId}")
    public ResponseEntity<?> modifierReponse(@PathVariable Long reponseId,
                                             @RequestBody ReponseForum reponseModifiee,
                                             @AuthenticationPrincipal Personne utilisateur) {
        ReponseForum reponse = reponseRepo.findById(reponseId)
                .orElseThrow(() -> new RuntimeException("Réponse non trouvée"));

        if (!reponse.getAuteur().getId().equals(utilisateur.getId())) {
            return ResponseEntity.status(403).body("Non autorisé à modifier cette réponse.");
        }

        reponse.setMessage(reponseModifiee.getMessage());
        return ResponseEntity.ok(reponseRepo.save(reponse));
    }

    // Supprimer une réponse
    @DeleteMapping("/sujets/reponses/supprimer/{reponseId}")
    public ResponseEntity<?> supprimerReponse(@PathVariable Long reponseId,
                                              @AuthenticationPrincipal Personne utilisateur) {
        ReponseForum reponse = reponseRepo.findById(reponseId)
                .orElseThrow(() -> new RuntimeException("Réponse non trouvée"));

        if (!reponse.getAuteur().getId().equals(utilisateur.getId())) {
            return ResponseEntity.status(403).body("Non autorisé à supprimer cette réponse.");
        }

        reponseRepo.delete(reponse);
        return ResponseEntity.ok("Réponse supprimée avec succès.");
    }
    @GetMapping("/admin/tous")
    public ResponseEntity<?> getTousLesMessagesForum() {
        List<SujetForum> sujets = sujetService.listerSujets();

        // Pour chaque sujet, on veut renvoyer ses réponses triées par date
        List<Map<String, Object>> resultats = sujets.stream().map(sujet -> {
            Map<String, Object> mapSujet = new HashMap<>();
            mapSujet.put("idSujet", sujet.getId());
            mapSujet.put("titre", sujet.getTitre());
            mapSujet.put("contenu", sujet.getContenu());
            mapSujet.put("auteur", sujet.getAuteur().getNom());
            mapSujet.put("dateCreation", sujet.getDateCreation());

            // On récupère les réponses liées au sujet, triées par date asc
            List<ReponseForum> reponses = reponseRepo.findBySujetIdOrderByDateReponseAsc(sujet.getId());

            List<Map<String, Object>> reponsesMap = reponses.stream().map(reponse -> {
                Map<String, Object> mapRep = new HashMap<>();
                mapRep.put("idReponse", reponse.getId());
                mapRep.put("message", reponse.getMessage());
                mapRep.put("auteur", reponse.getAuteur().getNom());
                mapRep.put("dateReponse", reponse.getDateReponse());
                return mapRep;
            }).toList();

            mapSujet.put("reponses", reponsesMap);

            return mapSujet;
        }).toList();

        return ResponseEntity.ok(resultats);
    }
    @DeleteMapping("/admin/supprimer-sujet/{id}")
    public ResponseEntity<?> supprimerSujetParAdmin(@PathVariable Long id) {
        SujetForum sujet = sujetService.getSujet(id)
                .orElseThrow(() -> new RuntimeException("Sujet non trouvé"));

        sujetService.supprimerSujet(id);
        return ResponseEntity.ok("Sujet supprimé par l'administrateur.");
    }
}
