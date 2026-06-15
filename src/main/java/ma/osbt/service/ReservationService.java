package ma.osbt.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import ma.osbt.entitie.ProfessionnelSanteMentale;
import ma.osbt.entitie.Reservation;

public interface ReservationService {
	Reservation save(Reservation reservation);
    Optional<Reservation> getById(Long id);
    List<Reservation> getReservationsPourPro(ProfessionnelSanteMentale professionnel);
    Reservation validerOuRefuserReservation(Long id, String statut, ProfessionnelSanteMentale professionnel);
    List<Reservation> getReservationsPourUtilisateur(Long utilisateurId);
    Reservation annulerReservation(Long reservationId, Long utilisateurId);
    List<Reservation> getReservationsEnAttentePaiement(Long utilisateurId);

   // Optional<Reservation> findByPaymentIntentId(String paymentIntentId);
	void handlePaymentSucceeded(Long reservationId);
	void markAsPaid(Long id);
	  Reservation findById(Long id);
	//boolean existsReservationForUserProDate(Long id, Long professionnelId, LocalDate date);
	 void deleteById(Long id);
	 
    
 }
