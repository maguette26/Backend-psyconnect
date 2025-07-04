package ma.osbt.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import ma.osbt.entitie.Fonctionnalite;
import ma.osbt.entitie.Role;
import ma.osbt.entitie.Utilisateur;
import ma.osbt.service.FonctionnaliteService;
import ma.osbt.service.UtilisateurService;

@RestController
@RequestMapping("/api/fonctionnalites")
public class FonctionnaliteController {
	 
	private final FonctionnaliteService fonctionnaliteService;
	private   UtilisateurService utilisateurService;

    public FonctionnaliteController(FonctionnaliteService fonctionnaliteService) {
        this.fonctionnaliteService = fonctionnaliteService;
    }

    
    @GetMapping
    public List<Fonctionnalite> listFonctionnalites() {
    	
        return fonctionnaliteService.listFonctionnalites();
    }

   
    @GetMapping("/{id}")
    public Fonctionnalite fonctionnaliteParId(@PathVariable Long id) {
        return fonctionnaliteService.fonctionnaliteParId(id)
                .orElseThrow(() -> new RuntimeException("Fonctionnalité non trouvée avec id: " + id));
    }

    
    @PostMapping
    public Fonctionnalite ajouterFonctionnalite(@RequestBody Fonctionnalite fonctionnalite) {
        return fonctionnaliteService.ajouterFonctionnalite(fonctionnalite);
    }

   
    @PutMapping("/{id}")
    public void modifierFonctionnalite(@PathVariable Long id, @RequestBody Fonctionnalite fonctionnalite) {
        fonctionnalite.setId(id);  
        fonctionnaliteService.modifierFonctionnalite(fonctionnalite);
    }

  //  @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void supprimerFonctionnalite(@PathVariable Long id) {
        if (fonctionnaliteService.fonctionnaliteParId(id).isEmpty()) {
            throw new RuntimeException("Fonctionnalité non trouvée avec id: " + id);
        }
        fonctionnaliteService.supprimerFonctionnalite(id);
    }
    @GetMapping("/citations")
    public List<Fonctionnalite> getCitationsGratuites() {
        return fonctionnaliteService.listCitations();
    }

    @GetMapping("/ressources")
    public List<Fonctionnalite> getRessourcesGratuites() {
        return fonctionnaliteService.listRessources();
    }
    @GetMapping("/ressources/{categorie}")
    public List<Fonctionnalite> getRessourcesParCategorie(@PathVariable String categorie) {
        return fonctionnaliteService.listRessourcesParCategorie(categorie);
    }
    @GetMapping("/premium/{type}")
    public List<Fonctionnalite> getPremiumVideosOrPodcasts(@PathVariable String type) {
        return fonctionnaliteService.listPremiumParType(type.toUpperCase());
    }


    @GetMapping("/premium/access/{id}")
    public ResponseEntity<?> accederContenuPremium(@PathVariable Long id, @AuthenticationPrincipal Utilisateur utilisateur) {
        Fonctionnalite f = fonctionnaliteService.fonctionnaliteParId(id)
                .orElseThrow(() -> new RuntimeException("Contenu non trouvé"));

        if (!f.isPremium()) {
         return ResponseEntity.ok(f);
        }
        if (utilisateur == null) {
             return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Accès réservé aux membres PREMIUM");
        }
          if (utilisateur.getRole() == Role.PREMIUM || utilisateur.getRole() == Role.ADMIN) {
            return ResponseEntity.ok(f);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Accès réservé aux membres PREMIUM");
    }

    @PutMapping("/upgrade-to-premium/{id}")
       public ResponseEntity<?> upgradeToPremium(@PathVariable Long id) {
        Optional<Utilisateur> userOpt = utilisateurService.getUtilisateurById(id);
        if (userOpt.isPresent()) {
            Utilisateur u = userOpt.get();
            u.setRole(Role.PREMIUM);
            utilisateurService.saveUtilisateur(u);
            return ResponseEntity.ok("Utilisateur passé en PREMIUM");
        }
        return ResponseEntity.notFound().build();
    }

}
