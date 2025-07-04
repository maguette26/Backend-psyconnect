package ma.osbt.entitie;

import java.time.LocalTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.persistence.CascadeType;
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
@NoArgsConstructor 
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Reservation  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double prix;
    @Column(nullable = false)
    private boolean estPayee = false;
    @Temporal(TemporalType.DATE)
    private Date dateReservation;

    @JsonDeserialize(using = CustomLocalTimeDeserializer.class)
    private LocalTime heureReservation;

    @JsonDeserialize(using = CustomLocalTimeDeserializer.class)
    private LocalTime heureConsultation;
    @Enumerated(EnumType.STRING)
    private ReservationStatut statut;

    @Enumerated(EnumType.STRING)
    private StatutValidation statutValidation;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id", referencedColumnName = "id")
    //@JsonIgnore
    private Utilisateur utilisateur;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "professionnel_id", referencedColumnName = "id")
    private ProfessionnelSanteMentale professionnel;

    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true )
    private Consultation consultation;

    @ManyToOne
    @JsonIgnoreProperties("reservations") 
    @JoinColumn(name = "disponibilite_id")
    private Disponibilite disponibilite;

    
}
