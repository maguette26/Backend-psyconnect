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
import ma.osbt.entitie.Personne;
import ma.osbt.entitie.Role;
import ma.osbt.entitie.Utilisateur;
import ma.osbt.repository.PersonneRepository;
import ma.osbt.repository.UtilisateurRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UtilisateurRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    


    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Validated @RequestBody RegisterRequest signUpRequest) {

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Erreur : cet email est déjà utilisé !");
        }

        if (userRepository.existsByTelephone(signUpRequest.getTelephone())) {
            return ResponseEntity.badRequest().body("Erreur : ce numéro de téléphone est déjà utilisé !");
        }

        if (!signUpRequest.getMotDePasse().equals(signUpRequest.getConfirmMotDePasse())) {
            return ResponseEntity.badRequest().body("Erreur : les mots de passe ne correspondent pas !");
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

    // 📛 Exception handler global
    @RestControllerAdvice
    public static class GlobalExceptionHandler {
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
            String errorMessage = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(errorMessage);
        }
    }
    @GetMapping("/me")
    public ResponseEntity<?> getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                Map.of("authenticated", false)
            );
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Personne)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(
                Map.of("authenticated", false)
            );
        }

        Personne user = (Personne) principal;

        Long id = user.getId();
        String email = user.getEmail();
        String nom = user.getNom();
        String prenom = user.getPrenom();
        String telephone = user.getTelephone();
        String role = user.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
                .findFirst()
                .orElse("UNKNOWN");

        // Optionnel : URL photo de profil si tu la gères (voir point 3)
       // String photoUrl = user.getPhotoUrl(); // ou null si pas géré encore

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "id", id,
                "email", email,
                "nom", nom,
                "prenom", prenom,
                "telephone", telephone,
                "role", role
               // "photoUrl", photoUrl
        ));
    }

    @Autowired
    private PersonneRepository personneRepository;

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            Authentication authentication,
            @RequestBody Map<String, Object> updates) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body("Non authentifié !");
        }

        String email = authentication.getName();
        Personne personne = personneRepository.findByEmail(email).orElse(null);

        if (personne == null) {
            return ResponseEntity.badRequest().body("Personne introuvable !");
        }

        if (updates.containsKey("nom")) {
            personne.setNom((String) updates.get("nom"));
        }

        if (updates.containsKey("prenom")) {
            personne.setPrenom((String) updates.get("prenom"));
        }

        if (updates.containsKey("telephone")) {
            String nouveauTelephone = (String) updates.get("telephone");
            if (!nouveauTelephone.equals(personne.getTelephone()) &&
                    personneRepository.existsByTelephone(nouveauTelephone)) {
                return ResponseEntity.badRequest().body("Ce numéro de téléphone est déjà utilisé.");
            }
            personne.setTelephone(nouveauTelephone);
        }

        if (updates.containsKey("motDePasse")) {
            String nouveauMotDePasse = (String) updates.get("motDePasse");
            if (nouveauMotDePasse != null && !nouveauMotDePasse.isBlank()) {
                personne.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
            }
        }

        personneRepository.save(personne);

        return ResponseEntity.ok("Profil mis à jour avec succès !");
    }



    // DTOs
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

    @Data
    public static class LoginRequest {
        @NotBlank
        private String email;

        @NotBlank
        private String motDePasse;
    }
}
