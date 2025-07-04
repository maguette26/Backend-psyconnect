package ma.osbt.service.implementation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ma.osbt.entitie.Personne;
import ma.osbt.entitie.ProfessionnelSanteMentale;
import ma.osbt.entitie.Role;
import ma.osbt.entitie.StatutValidation;
import ma.osbt.repository.PersonneRepository;
import ma.osbt.repository.ProfessionnelSanteMentaleRepository;
import ma.osbt.service.NotificationService;
import ma.osbt.service.ProfessionnelSanteService;

@Service
public class ProfessionnelSanteServiceImpl implements ProfessionnelSanteService {

    @Autowired
    private ProfessionnelSanteMentaleRepository professionnelRepo;

    @Autowired
    private PersonneRepository personneRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    public ProfessionnelSanteMentale saveProfessionnel(ProfessionnelSanteMentale professionnel) {
        professionnel.setStatutValidation(StatutValidation.EN_ATTENTE);
        ProfessionnelSanteMentale saved = professionnelRepo.save(professionnel);

        String messageProfessionnel = """
            <html>
              <body style="font-family: Arial, sans-serif; color: black; background-color: white; margin: 20px;">
                <h2>Demande d'inscription reçue</h2>
                <p>Bonjour <strong>%s</strong>,</p>
                <p>Votre demande d'inscription en tant que professionnel de santé mentale a bien été reçue et est en cours de validation par notre équipe.</p>
                <p>Nous vous informerons dès que la validation sera effectuée.</p>
                <br>
                <p>Merci de votre confiance,<br>L'équipe PsyConnect</p>
                <hr>
                <small>Ceci est un message automatique, merci de ne pas répondre.</small>
              </body>
            </html>
            """.formatted(saved.getPrenom());

        notificationService.notifier(saved, messageProfessionnel);

        List<Personne> admins = personneRepository.findByRoleIn(List.of(Role.ADMIN));
        String messageAdmin = """
            <html>
              <body style="font-family: Arial, sans-serif; color: black; background-color: white; margin: 20px;">
                <h2>Nouveau professionnel à valider</h2>
                <p><strong>Nom :</strong> %s</p>
                <p><strong>Prénom :</strong> %s</p>
                <p><strong>Email :</strong> %s</p>
                <br>
                <p>Veuillez valider ou refuser cette demande dans l'interface d'administration.</p>
                <hr>
                <small>Ceci est un message automatique, merci de ne pas répondre.</small>
              </body>
            </html>
            """.formatted(saved.getNom(), saved.getPrenom(), saved.getEmail());

        for (Personne admin : admins) {
            notificationService.notifier(admin, messageAdmin);
        }

        return saved;
    }

    @Override
    public List<ProfessionnelSanteMentale> getProfessionnelsEnAttente() {
        return professionnelRepo.findByStatutValidation(StatutValidation.EN_ATTENTE);
    }

    @Override
    public ProfessionnelSanteMentale updateStatutValidation(Long id, StatutValidation statut) {
        ProfessionnelSanteMentale p = professionnelRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Professionnel non trouvé"));
        p.setStatutValidation(statut);
        ProfessionnelSanteMentale updated = professionnelRepo.save(p);

        String message;

        if (statut == StatutValidation.VALIDE) {
            message = """
                <html>
                  <body style="font-family: Arial, sans-serif; color: black; background-color: white; margin: 20px;">
                    <h2>Compte validé</h2>
                    <p>Bonjour <strong>%s</strong>,</p>
                    <p>Votre compte PsyConnect a été validé avec succès par notre équipe.</p>
                    <p>Vous pouvez désormais accéder à toutes les fonctionnalités réservées aux professionnels.</p>
                    <br>
                    <p>Merci de votre confiance,<br>L'équipe PsyConnect</p>
                    <hr>
                    <small>Ceci est un message automatique, merci de ne pas répondre.</small>
                  </body>
                </html>
                """.formatted(p.getPrenom());
        } else if (statut == StatutValidation.REFUSE) {
            message = """
                <html>
                  <body style="font-family: Arial, sans-serif; color: black; background-color: white; margin: 20px;">
                    <h2>Demande refusée</h2>
                    <p>Bonjour <strong>%s</strong>,</p>
                    <p>Malheureusement, votre demande d'inscription a été refusée.</p>
                    <p>Veuillez vérifier les informations fournies et soumettre une nouvelle demande si nécessaire.</p>
                    <br>
                    <p>Cordialement,<br>L'équipe PsyConnect</p>
                    <hr>
                    <small>Ceci est un message automatique, merci de ne pas répondre.</small>
                  </body>
                </html>
                """.formatted(p.getPrenom());
        } else {
            message = """
                <html>
                  <body style="font-family: Arial, sans-serif; color: black; background-color: white; margin: 20px;">
                    <h2>Statut mis à jour</h2>
                    <p>Bonjour <strong>%s</strong>,</p>
                    <p>Votre statut de validation a été mis à jour : <strong>%s</strong>.</p>
                    <br>
                    <p>Merci de votre patience,<br>L'équipe PsyConnect</p>
                    <hr>
                    <small>Ceci est un message automatique, merci de ne pas répondre.</small>
                  </body>
                </html>
                """.formatted(p.getPrenom(), statut.name());
        }

        System.out.println("Envoi notification de validation à : " + p.getEmail());
        notificationService.notifier(p, message);

        return updated;
    }

    @Override
    public List<ProfessionnelSanteMentale> getAllProfessionnels() {
        return professionnelRepo.findAll();
    }
}
