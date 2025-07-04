package ma.osbt.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import ma.osbt.entitie.Consultation;
import ma.osbt.entitie.Personne;
import ma.osbt.entitie.ProfessionnelSanteMentale;
import ma.osbt.entitie.Role;
import ma.osbt.entitie.StatutValidation;
import ma.osbt.entitie.Utilisateur;
import ma.osbt.repository.ProfessionnelSanteMentaleRepository;
import ma.osbt.service.ConsultationService;
import ma.osbt.service.ProfessionnelSanteService;

@RestController
@RequestMapping("/api/professionnels")
public class ProfessionnelSanteMentaleController {

    @Autowired
    private ProfessionnelSanteService service;
    
    @Autowired
    private ConsultationService consultationService;
    @Autowired
    private  ProfessionnelSanteMentaleRepository professionnelRepository;
    private final String DOSSIER_UPLOAD = "C:\\Users\\Administrateur\\Documents\\upload\\";

    @PostMapping("/inscription")
    public ResponseEntity<?> inscrireProfessionnel(
        @RequestParam("specialite") String specialite,
        @RequestParam("document") MultipartFile documentFile,
        @RequestParam("nom") String nom,
        @RequestParam("prenom") String prenom,
        @RequestParam("email") String email,
        @RequestParam("motDePasse") String motDePasse,
        @RequestParam("telephone") String telephone
    ) {
        try {
            if (!specialite.equalsIgnoreCase("psychiatrie") && !specialite.equalsIgnoreCase("psychologie")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Spécialité invalide. Seules 'psychiatrie' ou 'psychologie' sont acceptées.");
            }

            // Vérification email existant
            if (professionnelRepository.existsByEmail(email)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Cette adresse email est déjà utilisée.");
            }

            String nomFichier = saveFile(documentFile);

            ProfessionnelSanteMentale professionnel = new ProfessionnelSanteMentale();
            professionnel.setSpecialite(specialite);
            professionnel.setDocumentJustificatif(nomFichier);
            professionnel.setStatutValidation(StatutValidation.EN_ATTENTE);
            professionnel.setNom(nom);
            professionnel.setPrenom(prenom);
            professionnel.setEmail(email);
            professionnel.setMotDePasse(new BCryptPasswordEncoder().encode(motDePasse));
            professionnel.setTelephone(telephone);
            professionnel.setRole(specialite.equalsIgnoreCase("psychiatrie") ? Role.PSYCHIATRE : Role.PSYCHOLOGUE);

            ProfessionnelSanteMentale saved = service.saveProfessionnel(professionnel);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur : " + e.getMessage());
        }
    }

    @GetMapping("/en-attente")
    public ResponseEntity<List<ProfessionnelSanteMentale>> getProfessionnelsEnAttente() {
        return ResponseEntity.ok(service.getProfessionnelsEnAttente());
    }

    @GetMapping("/tous")
    public ResponseEntity<List<ProfessionnelSanteMentale>> getAllProfessionnels() {
        return ResponseEntity.ok(service.getAllProfessionnels());
    }

    @PatchMapping("/validation/{id}")
    public ResponseEntity<?> updateValidation(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        try {
            Boolean valide = body.get("valide");
            if (valide == null) {
                return ResponseEntity.badRequest().body("Le champ 'valide' est requis.");
            }
            StatutValidation statut = valide ? StatutValidation.VALIDE : StatutValidation.REFUSE;
            ProfessionnelSanteMentale updated = service.updateStatutValidation(id, statut);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // Sauvegarde du fichier
    private String saveFile(MultipartFile file) throws IOException {
        String nomFichier = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path chemin = Paths.get(DOSSIER_UPLOAD + nomFichier);
        Files.createDirectories(chemin.getParent());
        Files.write(chemin, file.getBytes());
        return nomFichier;
    }

    // Téléchargement du fichier
    @GetMapping("/fichiers/{nomFichier}")
    public ResponseEntity<Resource> getFichier(@PathVariable String nomFichier) throws IOException {
        Path chemin = Paths.get(DOSSIER_UPLOAD + nomFichier);
        Resource resource = new UrlResource(chemin.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
            .body(resource);
    }
    @PatchMapping("/prix-consultation")
    public ResponseEntity<?> definirPrixConsultation(@AuthenticationPrincipal ProfessionnelSanteMentale pro,
                                                     @RequestParam Double nouveauPrix) {
        if (nouveauPrix == null || nouveauPrix < 0) {
            return ResponseEntity.badRequest().body("Le prix doit être un nombre positif.");
        }

        pro.setPrixConsultation(nouveauPrix);
        professionnelRepository.save(pro);
        return ResponseEntity.ok("Prix de consultation mis à jour avec succès.");
    }

    // 🔍 Obtenir le prix actuel
    @GetMapping("/prix-consultation")
    public ResponseEntity<Double> getPrixConsultation(@AuthenticationPrincipal ProfessionnelSanteMentale pro) {
        return ResponseEntity.ok(pro.getPrixConsultation());
    }
    @GetMapping("/mes-reservations")
    public List<Map<String, Object>> getReservationsPourProfessionnel(@AuthenticationPrincipal Personne personneConnectee) {
        if (!(personneConnectee instanceof ProfessionnelSanteMentale professionnel)) {
            throw new RuntimeException("Seuls les professionnels peuvent accéder à leurs réservations");
        }

        DateTimeFormatter formatterHeure = DateTimeFormatter.ofPattern("HH'H'mm");

        List<Consultation> consultations = consultationService.getConsultationsParProfessionnelId(professionnel.getId());

        return consultations.stream().map(consultation -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", consultation.getIdConsultation());
            map.put("date", consultation.getDateConsultation());

            if (consultation.getHeure() != null) {
                map.put("heure", consultation.getHeure().format(formatterHeure));
            } else {
                map.put("heure", null);
            }

            map.put("prix", consultation.getPrix());
            map.put("statut", consultation.getStatut() != null ? consultation.getStatut().name() : null);
            map.put("notesProfessionnel", consultation.getNotesProfessionnel());
            map.put("notesUtilisateur", consultation.getNotesUtilisateur());
            if (consultation.getReservation() != null && consultation.getReservation().getUtilisateur() != null) {
                Utilisateur utilisateur = consultation.getReservation().getUtilisateur();
                map.put("utilisateurNom", utilisateur.getNom());
                map.put("utilisateurPrenom", utilisateur.getPrenom());
                map.put("utilisateurEmail", utilisateur.getEmail());
            }

            return map;
        }).toList();
    }
    
}
