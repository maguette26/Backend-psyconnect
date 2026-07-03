package ma.osbt.service;

public interface PasswordResetService {

    /**
     * Génère un token et envoie un mail de réinitialisation.
     */
    void envoyerLienReinitialisation(String email);

    /**
     * Réinitialise le mot de passe à partir d'un token.
     */
    void reinitialiserMotDePasse(String token, String nouveauMotDePasse);

}