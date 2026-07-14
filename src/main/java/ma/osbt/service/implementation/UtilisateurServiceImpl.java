package ma.osbt.service.implementation;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ma.osbt.entitie.Role;
import ma.osbt.entitie.Utilisateur;
import ma.osbt.repository.ProfessionnelSanteMentaleRepository;
import ma.osbt.repository.UtilisateurRepository;
import ma.osbt.service.UtilisateurService;

@Service
public class UtilisateurServiceImpl implements UtilisateurService {

    // Regex reconnaissant un hash BCrypt déjà encodé (ex: $2a$10$..., $2b$..., $2y$...)
    private static final String BCRYPT_PATTERN = "^\\$2[aby]?\\$\\d{2}\\$.{53}$";

    private final UtilisateurRepository utilisateurRepository;
    private final ProfessionnelSanteMentaleRepository professionnelRepository;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurServiceImpl(
        UtilisateurRepository utilisateurRepository,
        PasswordEncoder passwordEncoder,
        ProfessionnelSanteMentaleRepository professionnelRepository
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.professionnelRepository = professionnelRepository;
    }

    @Override
    public List<Utilisateur> getAllUtilisateurs() {
        return utilisateurRepository.findAll();
    }

    @Override
    public Optional<Utilisateur> getUtilisateurById(Long id) {
        return utilisateurRepository.findById(id);
    }

    @Override
    public Utilisateur saveUtilisateur(Utilisateur utilisateur) {
        // 🔧 Correction : on ne ré-encode le mot de passe QUE s'il n'est pas déjà
        // un hash BCrypt. Avant cette correction, un simple changement de rôle
        // (ex: upgradeToPremium) ré-encodait un mot de passe déjà haché,
        // rendant l'utilisateur incapable de se reconnecter par la suite.
        String motDePasse = utilisateur.getMotDePasse();
        if (motDePasse != null && !motDePasse.isEmpty() && !motDePasse.matches(BCRYPT_PATTERN)) {
            utilisateur.setMotDePasse(passwordEncoder.encode(motDePasse));
        }
        return utilisateurRepository.save(utilisateur);
    }

    @Override
    public void deleteUtilisateur(Long id) {
        utilisateurRepository.deleteById(id);
    }

    @Override
    public Optional<Utilisateur> modifierUtilisateur(Utilisateur utilisateur) {
        Optional<Utilisateur> optUtilisateur = getUtilisateurById(utilisateur.getId());
        if (optUtilisateur.isPresent()) {
            Utilisateur utilisateurAModifier = optUtilisateur.get();

            Role nouveauRole = utilisateur.getRole();

            if (nouveauRole != null && !nouveauRole.equals(utilisateurAModifier.getRole())) {
                utilisateurAModifier.setRole(nouveauRole);

                if ((nouveauRole == Role.PSYCHOLOGUE || nouveauRole == Role.PSYCHIATRE)
                    && !professionnelRepository.existsById(utilisateurAModifier.getId())) {

                    var professionnel = new ma.osbt.entitie.ProfessionnelSanteMentale();
                    professionnel.setId(utilisateurAModifier.getId());
                    professionnel.setNom(utilisateurAModifier.getNom());
                    professionnel.setPrenom(utilisateurAModifier.getPrenom());
                    professionnel.setEmail(utilisateurAModifier.getEmail());
                    professionnel.setTelephone(utilisateurAModifier.getTelephone());
                    professionnel.setMotDePasse(utilisateurAModifier.getMotDePasse());
                    professionnel.setRole(nouveauRole);
                    professionnel.setStatutValidation(ma.osbt.entitie.StatutValidation.EN_ATTENTE);

                    professionnelRepository.save(professionnel);
                }
            }

            utilisateurRepository.save(utilisateurAModifier);
            return Optional.of(utilisateurAModifier);
        } else {
            return Optional.empty();
        }
    }
}