package ma.osbt.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ma.osbt.entitie.Disponibilite;
import ma.osbt.entitie.ProfessionnelSanteMentale;
import ma.osbt.entitie.Reservation;
import ma.osbt.entitie.ReservationStatut;
import ma.osbt.entitie.StatutValidation;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByProfessionnelId(Long professionnelId);

    List<Reservation> findByProfessionnel(ProfessionnelSanteMentale professionnel);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.utilisateur u WHERE r.professionnel.id = :proId")
    List<Reservation> findByProfessionnelWithUtilisateur(@Param("proId") Long proId);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.utilisateur u JOIN FETCH r.professionnel p WHERE u.id = :utilisateurId")
    List<Reservation> findByUtilisateurId(@Param("utilisateurId") Long utilisateurId);

    boolean existsByDisponibilite(Disponibilite disponibilite);

    boolean existsByDisponibiliteAndHeureReservation(Disponibilite disponibilite, LocalTime heureChoisie);

    boolean existsByProfessionnelAndDateReservationAndHeureReservationAndStatutIn(
        ProfessionnelSanteMentale pro,
        java.sql.Date date,
        LocalTime heure,
        List<ReservationStatut> statuts
    );

    @Query("SELECT r FROM Reservation r WHERE r.professionnel.id = :proId AND r.dateReservation >= :startDate AND r.dateReservation < :endDate AND r.statut IN :statuts AND r.statutValidation IN :statutValidations")
    List<Reservation> findByProfessionnelIdAndDateReservationBetweenAndStatutInAndStatutValidationIn(
        @Param("proId") Long professionnelId,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate,
        @Param("statuts") List<ReservationStatut> statut,
        @Param("statutValidations") List<StatutValidation> statutValidation
    );

    boolean existsByProfessionnelAndDateReservationAndHeureConsultationAndStatutIn(
        ProfessionnelSanteMentale pro,
        java.sql.Date date,
        LocalTime heureConsultation,
        List<ReservationStatut> statuts
    );

    List<Reservation> findByProfessionnelAndDateReservation(ProfessionnelSanteMentale pro, java.sql.Date date);

    boolean existsByUtilisateurIdAndDisponibiliteIdAndHeureConsultationAndStatutIn(
        Long utilisateurId,
        Long disponibiliteId,
        LocalTime heureConsultation,
        List<ReservationStatut> statuts
    );

    boolean existsByUtilisateurIdAndProfessionnelIdAndDateReservationAndStatutIn(
        Long utilisateurId,
        Long professionnelId,
        LocalDate dateConsultation,
        List<ReservationStatut> statuts
    );

    List<Reservation> findByUtilisateurIdAndStatut(Long utilisateurId, ReservationStatut enAttentePaiement);

    @Query("SELECT d FROM Disponibilite d LEFT JOIN FETCH d.reservations WHERE d.professionnel.id = :proId")
    List<Disponibilite> findByProfessionnelIdWithReservations(@Param("proId") Long proId);

    List<Reservation> findByDisponibiliteIdAndStatutInAndStatutValidationIn(
        Long id,
        List<ReservationStatut> statuts,
        List<StatutValidation> statutValidations
    );

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reservation r JOIN r.disponibilite d " +
    	       "WHERE r.utilisateur.id = :userId " +
    	       "AND r.professionnel.id = :proId " +
    	       "AND d.date = :dateConsultation " +
    	       "AND r.statut IN :statuts")
    	boolean existsByUtilisateurIdAndProfessionnelIdAndDisponibiliteDateAndStatutIn(
    	    @Param("userId") Long userId,
    	    @Param("proId") Long proId,
    	    @Param("dateConsultation") java.sql.Date dateConsultation,
    	    @Param("statuts") List<ReservationStatut> statuts
    	);

	boolean existsByDisponibiliteIdAndStatutIn(Long id, List<ReservationStatut> of);

	boolean existsByUtilisateurIdAndProfessionnelIdAndDateReservationAndStatutIn(Long userId, Long id,
			java.sql.Date valueOf, List<ReservationStatut> of);







}