package ma.osbt.repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ma.osbt.entitie.Consultation;
import ma.osbt.entitie.ProfessionnelSanteMentale;
import ma.osbt.entitie.Reservation;
import ma.osbt.entitie.StatutConsultation;
@Repository

public interface ConsultationRepository  extends JpaRepository<Consultation, Long>{

	List<Consultation> findByProfessionnelAndDateConsultation(ProfessionnelSanteMentale pro, Date valueOf);
	List<Consultation> findByReservationUtilisateurId(Long utilisateurId);

	Optional<Consultation> findByReservation(Reservation reservation);
    List<Consultation> findByReservation_Utilisateur_Id(Long utilisateurId);

    List<Consultation> findByReservation_Utilisateur_IdAndStatut(Long utilisateurId, StatutConsultation statut);
    List<Consultation> findByStatut(String statut);
	List<Consultation> findByStatut(StatutConsultation confirmee);
	Optional<Consultation> findByReservation_Id(Long reservationId);
	List<Consultation> findByProfessionnelId(Long professionnelId);


}
