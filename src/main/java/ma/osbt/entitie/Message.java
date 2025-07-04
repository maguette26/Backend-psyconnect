package ma.osbt.entitie;

import java.time.LocalTime;
import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor @AllArgsConstructor

public class Message {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Date date;
    private LocalTime heure;
    private String contenu;
    private boolean anonymat;
    private boolean inapproprie = false;
    
    @ManyToOne
    @JoinColumn(name = "expediteur_id")
    private Personne expediteur;
    
    @ManyToOne
    @JoinColumn(name = "destinataire_id")
    private Personne destinataire;
    @ManyToOne
    @JoinColumn(name = "consultation_id")
    private Consultation consultation;


    
}
