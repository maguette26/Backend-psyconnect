package ma.osbt.repository;

 
 
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ma.osbt.entitie.ConversationMessage, Long> {
    List<ma.osbt.entitie.ConversationMessage> findByUtilisateurIdOrderByDateEnvoiAsc(Long utilisateurId);
}