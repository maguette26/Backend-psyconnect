package ma.osbt.service.implementation;

 
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
 
import org.springframework.stereotype.Service;
 
import ma.osbt.entitie.Message;
import ma.osbt.entitie.Personne;
import ma.osbt.repository.MessageRepository;
 
import ma.osbt.service.MessageService;
import ma.osbt.service.NotificationService;
 

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private NotificationService notificationService;

    private final List<String> motsInterdits = List.of("suicide", "haine", "violence", "insulte");

    private boolean estInapproprie(String contenu) {
        if (contenu == null) return false;
        String lower = contenu.toLowerCase();
        return motsInterdits.stream().anyMatch(lower::contains);
    }

    @Override
    public Message envoyerMessage(Message message) {
        if (message.getContenu() == null) {
            throw new IllegalArgumentException("Le message ne peut pas être vide");
        }

        if (estInapproprie(message.getContenu())) {
            message.setInapproprie(true);
            // Optionnel : notifier admin de message inapproprié
            notificationService.notifierAdmin("Message inapproprié détecté de " + message.getExpediteur().getNom());
        } else {
            message.setInapproprie(false);
        }

        Message saved = messageRepository.save(message);

        if (!message.isInapproprie() && saved.getDestinataire() != null) {
            notificationService.notifier(
                saved.getDestinataire(),
                "Vous avez reçu un nouveau message de " + saved.getExpediteur().getNom() + " :<br><br>" + saved.getContenu()
            );
        }

        return saved;
    }


    @Override
    public List<Message> getMessagesEntre(Personne p1, Personne p2) {
        return messageRepository.findByExpediteurAndDestinataireOrDestinataireAndExpediteurOrderByDateDesc(p1, p2, p1, p2);
    }

    @Override
    public List<Message> getMessagesPourUtilisateur(Personne personne) {
        return messageRepository.findByExpediteurOrDestinataireOrderByDateDesc(personne, personne);
    }
    public List<Message> getTousMessages() {
        return messageRepository.findAllByOrderByDateDesc();
    }
   
}
