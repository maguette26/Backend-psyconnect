package ma.osbt.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import ma.osbt.entitie.Humeur;
import ma.osbt.entitie.Utilisateur;
import ma.osbt.service.HumeurService;
import ma.osbt.repository.UtilisateurRepository;

@RestController
@RequestMapping("/api/humeurs")
public class HumeurController {

    private final HumeurService humeurService;
    private final UtilisateurRepository utilisateurRepository;

    public HumeurController(HumeurService humeurService, UtilisateurRepository utilisateurRepository) {
        this.humeurService = humeurService;
        this.utilisateurRepository = utilisateurRepository;
    }

    // ✅ POST : ajoute humeur pour l'utilisateur connecté
    @PostMapping
    public Humeur ajouterHumeur(Authentication authentication, @RequestBody Humeur humeur) {
        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        humeur.setUtilisateur(utilisateur);
        return humeurService.ajouterHumeur(humeur);
    }

    // ✅ GET : récupère toutes les humeurs de l'utilisateur connecté
    @GetMapping
    public List<Humeur> getHumeursUtilisateurConnecte(Authentication authentication) {
        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return humeurService.getHumeursUtilisateur(utilisateur.getId());
    }

    // ✅ DELETE : supprime une humeur
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerHumeur(@PathVariable Long id) {
        humeurService.supprimerHumeur(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ PUT : modifie une humeur
    @PutMapping("/{id}")
    public ResponseEntity<Humeur> modifierHumeur(@PathVariable Long id, @RequestBody Humeur humeur) {
        return ResponseEntity.ok(humeurService.modifierHumeur(id, humeur));
    }

    // ✅ GET : récupère humeur à une date spécifique
    @GetMapping("/date")
    public List<Humeur> getHumeursParDate(
        Authentication authentication,
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return humeurService.getHumeursParDate(utilisateur.getId(), date);
    }

    // ✅ GET : récupère humeur sur une période
    @GetMapping("/periode")
    public List<Humeur> getHumeursParPeriode(
        Authentication authentication,
        @RequestParam("debut") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
        @RequestParam("fin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin
    ) {
        String email = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return humeurService.getHumeursParPeriode(utilisateur.getId(), debut, fin);
    }
}