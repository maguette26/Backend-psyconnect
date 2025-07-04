package ma.osbt.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ma.osbt.entitie.Consultation;
import ma.osbt.entitie.Personne;
import ma.osbt.entitie.StatutConsultation;
import ma.osbt.entitie.Utilisateur;
import ma.osbt.service.ConsultationService;
import ma.osbt.service.UtilisateurService;
import java.time.format.DateTimeFormatter;
@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    @Autowired
    private ConsultationService consultationService;

    



 // ...

 @GetMapping("/mes-consultations")
 public List<Map<String, Object>> getConsultationsByUtilisateur(@AuthenticationPrincipal Personne personneConnectee) {
     if (!(personneConnectee instanceof Utilisateur utilisateur)) {
         throw new RuntimeException("Seuls les utilisateurs peuvent accéder à leurs consultations");
     }

     DateTimeFormatter formatterHeure = DateTimeFormatter.ofPattern("HH'H'mm"); // Exemple : 14H00

     List<Consultation> consultations = consultationService.getConsultationsParPersonneId(utilisateur.getId());

     List<Map<String, Object>> result = consultations.stream().map(consultation -> {
         Map<String, Object> map = new HashMap<>();
         map.put("id", consultation.getIdConsultation());
         map.put("date", consultation.getDateConsultation());

         // Formattage de l'heure
         if (consultation.getHeure() != null) {
             map.put("heure", consultation.getHeure().format(formatterHeure));
         } else {
             map.put("heure", null);
         }

         map.put("prix", consultation.getPrix());
         map.put("statut", consultation.getStatut() != null ? consultation.getStatut().name() : null);
         map.put("notesProfessionnel", consultation.getNotesProfessionnel());
         map.put("notesUtilisateur", consultation.getNotesUtilisateur());

         if (consultation.getProfessionnel() != null) {
             map.put("professionnelNom", consultation.getProfessionnel().getNom());
             map.put("professionnelPrenom", consultation.getProfessionnel().getPrenom());
             map.put("professionnelEmail", consultation.getProfessionnel().getEmail());
         }

         return map;
     }).toList();

     return result;
 }



    // ✅ Récupère les consultations de l'utilisateur connecté filtrées par statut
    @GetMapping("/mes-consultations/statut")
    public List<Consultation> getMesConsultationsParStatut(@AuthenticationPrincipal Personne personneConnectee,
                                                           @RequestParam StatutConsultation statut) {
        if (!(personneConnectee instanceof Utilisateur utilisateur)) {
            throw new RuntimeException("Seuls les utilisateurs peuvent accéder à leurs consultations");
        }
        return consultationService.getConsultationsByUtilisateurEtStatut(utilisateur.getId(), statut);
    }
}
