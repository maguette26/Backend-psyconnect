package ma.osbt.controller;

import java.time.LocalDate;

import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import ma.osbt.entitie.Disponibilite;
import ma.osbt.service.DisponibiliteService;

@RestController
@RequestMapping("/api/disponibilites")
@RequiredArgsConstructor
public class DisponibiliteController {

    private final DisponibiliteService disponibiliteService;

    @PostMapping()
    public ResponseEntity<Disponibilite> ajouter(@RequestBody Disponibilite disponibilite) {
        return ResponseEntity.ok(disponibiliteService.ajouterDisponibilite(disponibilite));
    }

    @GetMapping
    public ResponseEntity<List<Disponibilite>> getDisponibilites() {
        return ResponseEntity.ok(disponibiliteService.getDisponibilitesProfessionnelConnecte());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Disponibilite> modifier(@PathVariable Long id, @RequestBody Disponibilite disponibilite) {
        return ResponseEntity.ok(disponibiliteService.modifierDisponibilite(id, disponibilite));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        disponibiliteService.supprimerDisponibilite(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filtrees/{proId}")
    public ResponseEntity<List<Disponibilite>> getDisponibilitesFiltrees(
            @PathVariable Long proId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        boolean authorized = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_USER") || r.equals("ROLE_PSYCHOLOGUE") || r.equals("ROLE_PSYCHIATRE"));

        if (!authorized) {
            throw new RuntimeException("Accès refusé : rôle insuffisant");
        }

        return ResponseEntity.ok(disponibiliteService.getDisponibilitesFiltrees(proId, date));
    }

    @GetMapping("/{proId}")
    public ResponseEntity<List<Disponibilite>> getDisponibilitesByProId(@PathVariable Long proId) {
        boolean authorized = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_USER") || r.equals("ROLE_PSYCHOLOGUE") || r.equals("ROLE_PSYCHIATRE"));

        if (!authorized) {
            throw new RuntimeException("Accès refusé : rôle insuffisant");
        }

        List<Disponibilite> disponibilites = disponibiliteService.getDisponibilitesByProId(proId);

        // Charger explicitement les réservations pour chaque disponibilité si nécessaire
        disponibilites.forEach(d -> {
            if (d.getReservations() != null) {
                d.getReservations().size(); // Force le chargement si lazy
            }
        });

        return ResponseEntity.ok(disponibilites);
    }
    @GetMapping("/publiques")
    public ResponseEntity<?> getDisponibilitesPubliques() {
        return ResponseEntity.ok(disponibiliteService.getDisponibilitesPubliques());
    }
}
