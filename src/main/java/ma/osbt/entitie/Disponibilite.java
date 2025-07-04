package ma.osbt.entitie;

import java.time.LocalDate;

import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor @AllArgsConstructor
public class Disponibilite {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private boolean reservee;

    private LocalDate date;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    
    @ManyToOne
    private ProfessionnelSanteMentale professionnel;
    @JsonIgnoreProperties("disponibilite")
    @OneToMany(mappedBy = "disponibilite", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Reservation> reservations;
    @JsonIgnore
    @Transient
public List<String> getCreneauxReserves() {
    if (reservations == null) return List.of();

    return reservations.stream()
            .filter(r -> r.getStatut() == ReservationStatut.EN_ATTENTE
                      || r.getStatut() == ReservationStatut.EN_ATTENTE_PAIEMENT
                      || r.getStatut() == ReservationStatut.PAYEE)
            .map(r -> r.getHeureConsultation().toString().substring(0,5))
            .toList();
}
 
}

