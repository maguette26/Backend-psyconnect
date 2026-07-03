package ma.osbt.service.implementation;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ma.osbt.entitie.PasswordResetToken;
import ma.osbt.entitie.Utilisateur;
import ma.osbt.repository.PasswordResetTokenRepository;
import ma.osbt.repository.UtilisateurRepository;
import ma.osbt.service.NotificationService;
import ma.osbt.service.PasswordResetService;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UtilisateurRepository utilisateurRepository;

    private final PasswordResetTokenRepository tokenRepository;

    private final NotificationService notificationService;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void envoyerLienReinitialisation(String email) {

        utilisateurRepository.findByEmail(email).ifPresent(utilisateur -> {

            tokenRepository.findByUtilisateurId(utilisateur.getId())
                    .ifPresent(tokenRepository::delete);

            String token = UUID.randomUUID().toString();

            PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                    .token(token)
                    .utilisateur(utilisateur)
                    .expiration(LocalDateTime.now().plusMinutes(30))
                    .build();

            tokenRepository.save(passwordResetToken);

            String lien = "http://localhost:5173/reset-password/" + token;

            notificationService.envoyerMailReinitialisation(utilisateur, lien);

        });

    }

    @Override
    public void reinitialiserMotDePasse(String token, String nouveauMotDePasse) {

        PasswordResetToken passwordResetToken = tokenRepository.findByToken(token)
                .orElseThrow(() ->
                        new RuntimeException("Lien de réinitialisation invalide."));

        if (passwordResetToken.getExpiration().isBefore(LocalDateTime.now())) {

            tokenRepository.delete(passwordResetToken);

            throw new RuntimeException("Le lien de réinitialisation a expiré.");

        }

        Utilisateur utilisateur = passwordResetToken.getUtilisateur();

        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));

        utilisateurRepository.save(utilisateur);

        tokenRepository.delete(passwordResetToken);

    }

}