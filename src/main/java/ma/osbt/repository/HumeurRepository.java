package ma.osbt.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ma.osbt.entitie.Humeur;

public interface HumeurRepository extends JpaRepository<Humeur, Long> {
    List<Humeur> findByUtilisateurIdOrderByDateDesc(Long utilisateurId);
    boolean existsByUtilisateurIdAndDateAndEtatAndNoteJournal(
    Long utilisateurId, LocalDate date, String etat, String noteJournal);
    List<Humeur> findByUtilisateurIdAndDate(Long utilisateurId, LocalDate date);
    List<Humeur> findByUtilisateurIdAndDateBetween(Long utilisateurId, LocalDate start, LocalDate end);

}
