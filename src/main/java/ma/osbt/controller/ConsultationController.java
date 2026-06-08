package ma.osbt.controller;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
}