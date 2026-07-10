package ma.osbt.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.osbt.config.JwtUtils;
import ma.osbt.entitie.Personne;
import ma.osbt.entitie.Role;
import ma.osbt.entitie.Utilisateur;
import ma.osbt.repository.PersonneRepository;
import ma.osbt.repository.UtilisateurRepository;
import ma.osbt.service.PasswordResetService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// ✅ Pas de @CrossOrigin ici — géré globalement par SecurityConfig
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UtilisateurRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PersonneRepository personneRepository;

    // 🔧 Ajouté : nécessaire pour régénérer un JWT à jour après un changement de rôle
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private PasswordResetService passwordResetService;
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Validated @RequestBody RegisterRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Erreur : cet email est déjà utilisé !"
            ));
        }
        if (userRepository.existsByTelephone(signUpRequest.getTelephone())) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Erreur : ce numéro de téléphone est déjà utilisé !"
            ));
        }
        if (!signUpRequest.getMotDePasse().equals(signUpRequest.getConfirmMotDePasse())) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Erreur : les mots de passe ne correspondent pas !"
            ));
        }

        Utilisateur user = new Utilisateur();
        user.setNom(signUpRequest.getNom());
        user.setPrenom(signUpRequest.getPrenom());
        user.setEmail(signUpRequest.getEmail());
        user.setTelephone(signUpRequest.getTelephone());
        user.setMotDePasse(passwordEncoder.encode(signUpRequest.getMotDePasse()));
        user.setRole(Role.USER);
        userRepository.save(user);

        return ResponseEntity.ok("Utilisateur enregistré avec succès !");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Personne)) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }

        Personne user = (Personne) principal;

        String role = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
                .findFirst()
                .orElse("UNKNOWN");

        return ResponseEntity.ok(Map.of(
            "authenticated", true,
            "id", user.getId(),
            "email", user.getEmail(),
            "nom", user.getNom(),
            "prenom", user.getPrenom(),
            "telephone", user.getTelephone(),
            "role", role
        ));
    }

   
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        String newToken = jwtUtils.generateToken(authentication);

        return ResponseEntity.ok(Map.of("token", newToken));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            Authentication authentication,
            @RequestBody Map<String, Object> updates) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Non authentifié !");
        }

        String email = authentication.getName();
        Personne personne = personneRepository.findByEmail(email).orElse(null);
        if (personne == null) {
            return ResponseEntity.badRequest().body("Personne introuvable !");
        }

        if (updates.containsKey("nom")) 
            personne.setNom((String) updates.get("nom"));
        if (updates.containsKey("prenom")) 
            personne.setPrenom((String) updates.get("prenom"));
        if (updates.containsKey("telephone")) {
            String tel = (String) updates.get("telephone");
            if (!tel.equals(personne.getTelephone()) &&
                    personneRepository.existsByTelephone(tel)) {
                return ResponseEntity.badRequest().body("Ce numéro est déjà utilisé.");
            }
            personne.setTelephone(tel);
        }
        if (updates.containsKey("motDePasse")) {
            String mdp = (String) updates.get("motDePasse");
            if (mdp != null && !mdp.isBlank()) {
                personne.setMotDePasse(passwordEncoder.encode(mdp));
            }
        }

        personneRepository.save(personne);
        return ResponseEntity.ok("Profil mis à jour avec succès !");
    }

    // ── DTOs ──────────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Le nom est obligatoire")
        private String nom;

        @NotBlank(message = "Le prénom est obligatoire")
        private String prenom;

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email doit être valide")
        private String email;

        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        private String telephone;

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
        private String motDePasse;

        @NotBlank(message = "La confirmation du mot de passe est obligatoire")
        private String confirmMotDePasse;
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {

        passwordResetService.envoyerLienReinitialisation(request.getEmail());

        return ResponseEntity.ok(
                Map.of("message",
                        "Si un compte existe avec cet email, un lien de réinitialisation a été envoyé."));
    }
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        passwordResetService.reinitialiserMotDePasse(
                request.getToken(),
                request.getNouveauMotDePasse());

        return ResponseEntity.ok(
                Map.of("message", "Mot de passe réinitialisé avec succès."));
    }

    // ── Exception handler ─────────────────────────────────
    @RestControllerAdvice
    public static class GlobalExceptionHandler {
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
            String errorMessage = ex.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(Map.of("errors", errorMessage));
        }
    }
    @Data
    public static class ForgotPasswordRequest {
        @Email
        @NotBlank
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank
        private String token;

        @NotBlank
        @Size(min = 6)
        private String nouveauMotDePasse;
    }
}