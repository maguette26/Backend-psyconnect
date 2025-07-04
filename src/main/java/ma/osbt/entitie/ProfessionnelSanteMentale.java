package ma.osbt.entitie;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@ToString(exclude = {"consultations", "reservations", "messagesEnvoyes", "messagesRecus", "disponibilites"})
@Data 
@NoArgsConstructor @AllArgsConstructor
@DiscriminatorValue(value="PROFESSIONNEL")
public class ProfessionnelSanteMentale extends Personne {
	private String specialite;
	private String documentJustificatif;
    private Double prixConsultation=0.0;;
		
	@OneToMany(mappedBy = "professionnel")
	@JsonIgnore
	private List<Consultation> consultations;
	
	@OneToMany(mappedBy = "professionnel")
	@JsonIgnore
    private List<Reservation> reservations;
	
	@OneToMany(mappedBy = "expediteur")
	@JsonIgnore
    private List<Message> messagesEnvoyes;
	
    @OneToMany(mappedBy = "destinataire")
    @JsonIgnore
    private List<Message> messagesRecus;
    
    @Enumerated(EnumType.STRING)
    private StatutValidation statutValidation = StatutValidation.EN_ATTENTE;

   
    @OneToMany(mappedBy = "professionnel")
    @JsonIgnore
    private List<Disponibilite> disponibilites;
	 
}
