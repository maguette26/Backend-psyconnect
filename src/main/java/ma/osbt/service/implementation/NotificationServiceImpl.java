package ma.osbt.service.implementation;

import ma.osbt.entitie.Personne;
import ma.osbt.entitie.Role;
import ma.osbt.repository.PersonneRepository;
import ma.osbt.service.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.core.io.ByteArrayResource;

import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

import java.util.Collections;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final PersonneRepository personneRepository;
    private final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    public NotificationServiceImpl(JavaMailSender mailSender, PersonneRepository personneRepository) {
        this.mailSender = mailSender;
        this.personneRepository = personneRepository;
    }

    @Override
    @Async
    public void notifier(Personne destinataire, String messageHtml) {
        if (destinataire == null || destinataire.getEmail() == null) return;

        logger.info("🔔 Notification pour " + destinataire.getEmail() + " : " + messageHtml);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setTo(destinataire.getEmail());
            helper.setSubject("📢 Notification PsyConnect");
            helper.setText(messageHtml, true);

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            logger.error("❌ Erreur envoi email à " + destinataire.getEmail(), e);
        }
    }
    @Async
    public void notifierAvecPieceJointe(Personne destinataire, String messageHtml, byte[] pdfData, String fileName) {
        if (destinataire == null || destinataire.getEmail() == null) return;

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(destinataire.getEmail());
            helper.setSubject("🧾 Votre reçu PsyConnect");
            helper.setText(messageHtml, true);
            helper.addAttachment(fileName, new ByteArrayResource(pdfData));

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            logger.error("❌ Erreur envoi email avec pièce jointe", e);
        }
    }

    @Override
    public void notifierAdmin(String message) {
        logger.info("Notification admin : " + message);

        List<Personne> admins = personneRepository.findByRoleIn(Collections.singletonList(Role.ADMIN));

        if (admins.isEmpty()) {
            logger.warn("Aucun administrateur trouvé pour notification");
            return;
        }

        for (Personne admin : admins) {
            try {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setTo(admin.getEmail());
                mail.setSubject("Notification - Message inapproprié détecté");
                mail.setText(message);
                mailSender.send(mail);
            } catch (Exception e) {
                logger.error("Erreur envoi email à l'admin " + admin.getEmail(), e);
            }
        }
    }
    @Override
    public void envoyerMailReinitialisation(Personne personne, String lien) {

        try {

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(personne.getEmail());

            helper.setSubject("🔐 Réinitialisation de votre mot de passe PsyConnect");

            String html = """
                <html>
                <body style="font-family:Arial,sans-serif">

                <h2 style="color:#2563EB">
                    Réinitialisation du mot de passe
                </h2>

                <p>Bonjour <b>%s</b>,</p>

                <p>
                Vous avez demandé la réinitialisation de votre mot de passe.
                </p>

                <p>
                Cliquez sur le bouton ci-dessous :
                </p>

                <a href="%s"
                   style="
                   background:#2563EB;
                   color:white;
                   padding:12px 22px;
                   text-decoration:none;
                   border-radius:8px;">
                   Réinitialiser mon mot de passe
                </a>

                <br><br>

                <p>
                Ce lien est valable pendant <b>30 minutes</b>.
                </p>

                <p>
                Si vous n'êtes pas à l'origine de cette demande,
                ignorez simplement cet e-mail.
                </p>

                <hr>

                <small>
                © PsyConnect
                </small>

                </body>
                </html>
                """.formatted(personne.getPrenom(), lien);

            helper.setText(html, true);

            mailSender.send(message);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erreur lors de l'envoi du mail.", e);

        }

    }
}
