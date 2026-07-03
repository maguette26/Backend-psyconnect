package ma.osbt.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ma.osbt.entitie.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long>{

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUtilisateurId(Long id);

    void deleteByUtilisateurId(Long id);
}