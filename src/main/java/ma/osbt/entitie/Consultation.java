package ma.osbt.entitie;

import java.time.LocalTime;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor @AllArgsConstructor

public class Consultation {
	@Id
	 @GeneratedValue(strategy = GenerationType.IDENTITY)	
private Long idConsultation;
	@Temporal(TemporalType.DATE)
private Date dateConsultation;
	@Column(name = "heure", columnDefinition = "TIME(0)")
	private LocalTime heure; 
private Double prix;
private int dureeMinutes;
@Column(columnDefinition = "TEXT")
private String notesProfessionnel;
@Column(columnDefinition = "TEXT")
private String notesUtilisateur;
@Enumerated(EnumType.STRING)
private StatutConsultation statut;

@ManyToOne()
private ProfessionnelSanteMentale professionnel;
@OneToOne
@JoinColumn(name = "reservation_id")
private Reservation reservation;


}
