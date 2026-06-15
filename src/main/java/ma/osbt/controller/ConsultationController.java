package ma.osbt.controller;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import ma.osbt.entitie.*;
import ma.osbt.repository.PersonneRepository;
import ma.osbt.service.ConsultationService;

@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private PersonneRepository personneRepository;

    // ==============================
    // USER : mes consultations
    // ==============================
    @GetMapping("/mes-consultations")
    public List<Map<String, Object>> getConsultationsByUtilisateur(
            @AuthenticationPrincipal UserDetails user) {

        Personne personne = personneRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (!(personne instanceof Utilisateur utilisateur)) {
            throw new RuntimeException("Seuls les utilisateurs peuvent accéder à leurs consultations");
        }

        DateTimeFormatter formatterHeure = DateTimeFormatter.ofPattern("HH'H'mm");

        List<Consultation> consultations =
                consultationService.getConsultationsParPersonneId(utilisateur.getId());

        return consultations.stream().map(consultation -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", consultation.getIdConsultation());
            map.put("date", consultation.getDateConsultation());

            map.put("heure",
                    consultation.getHeure() != null
                            ? consultation.getHeure().format(formatterHeure)
                            : null
            );

            map.put("prix", consultation.getPrix());
            map.put("statut",
                    consultation.getStatut() != null
                            ? consultation.getStatut().name()
                            : null
            );
            map.put("dureeMinutes", consultation.getDureeMinutes());

            map.put("notesProfessionnel", consultation.getNotesProfessionnel());
            map.put("notesUtilisateur", consultation.getNotesUtilisateur());

            if (consultation.getProfessionnel() != null) {
                map.put("professionnelNom", consultation.getProfessionnel().getNom());
                map.put("professionnelPrenom", consultation.getProfessionnel().getPrenom());
                map.put("professionnelEmail", consultation.getProfessionnel().getEmail());
            }

            return map;
        }).toList();
    }

    // ==============================
    // USER : filtre statut
    // ==============================
    @GetMapping("/mes-consultations/statut")
    public List<Consultation> getMesConsultationsParStatut(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam StatutConsultation statut) {

        Personne personne = personneRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (!(personne instanceof Utilisateur utilisateur)) {
            throw new RuntimeException("Seuls les utilisateurs peuvent accéder à leurs consultations");
        }

        return consultationService.getConsultationsByUtilisateurEtStatut(
                utilisateur.getId(),
                statut
        );
    }

    // ==============================
    // PROFESSIONNEL
    // ==============================
    @GetMapping("/mes-consultations/professionnel")
    public List<Map<String, Object>> getConsultationsPourPro(
            @AuthenticationPrincipal UserDetails user) {

        Personne personne = personneRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (!(personne instanceof ProfessionnelSanteMentale pro)) {
            throw new RuntimeException("Réservé aux professionnels");
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH'H'mm");

        return consultationService.getConsultationsParProfessionnelId(pro.getId())
                .stream()
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getIdConsultation());
                    map.put("date", c.getDateConsultation());
                    map.put("heure",
                            c.getHeure() != null ? c.getHeure().format(fmt) : null
                    );
                    map.put("prix", c.getPrix());
                    map.put("statut", c.getStatut().name());

                    map.put("utilisateurNom", c.getReservation().getUtilisateur().getNom());
                    map.put("utilisateurPrenom", c.getReservation().getUtilisateur().getPrenom());
                    map.put("utilisateurEmail", c.getReservation().getUtilisateur().getEmail());

                    return map;
                })
                .toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getConsultationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {

        Consultation c = consultationService.findById(id);

        if (c == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> map = new HashMap<>();

        map.put("id", c.getIdConsultation());
        map.put("date", c.getDateConsultation());
        map.put("heure", c.getHeure() != null ? c.getHeure().toString() : null);
        map.put("statut", c.getStatut().name());
        map.put("prix", c.getPrix());

        if (c.getProfessionnel() != null) {
            map.put("professionnelPrenom", c.getProfessionnel().getPrenom());
            map.put("professionnelNom", c.getProfessionnel().getNom());
            map.put("specialite", c.getProfessionnel().getSpecialite());
        }

        return ResponseEntity.ok(map);
    }
    @DeleteMapping("/supprimer/{id}")
    public ResponseEntity<?> supprimerConsultation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user
    ) {
        try {
            // 1. Récupérer la consultation
            Consultation consultation = consultationService.findById(id);

            if (consultation == null) {
                return ResponseEntity.status(404).body("Consultation introuvable");
            }

            // 2. Vérifier que l'utilisateur connecté est bien propriétaire (sécurité)
            Personne personne = personneRepository.findByEmail(user.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

            if (!(personne instanceof Utilisateur utilisateur)) {
                return ResponseEntity.status(403).body("Accès interdit");
            }

            // 3. Vérifier que la consultation appartient bien à l'utilisateur
         // Ajoute une vérification null sur la reservation
            if (consultation.getReservation() == null || 
                !consultation.getReservation().getUtilisateur().getId().equals(utilisateur.getId())) {
                return ResponseEntity.status(403).body("Vous ne pouvez pas supprimer cette consultation");
            }

            // 4. Suppression
            consultationService.deleteById(id);

            return ResponseEntity.ok("Consultation supprimée avec succès");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur serveur : " + e.getMessage());
        }
    }
}