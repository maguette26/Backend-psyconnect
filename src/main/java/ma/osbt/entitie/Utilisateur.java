package ma.osbt.entitie;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@DiscriminatorValue(value="UTILISATEUR")
 
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Utilisateur extends Personne {
    
    private boolean anonymat;

    @OneToMany(mappedBy = "utilisateur")
      @JsonIgnore
    @ToString.Exclude
    private List<Reservation> reservations;

    @JsonIgnore
    @OneToMany(mappedBy = "expediteur")
    @ToString.Exclude
    private List<Message> messagesEnvoyes;

    @JsonIgnore
    @OneToMany(mappedBy = "destinataire")
    @ToString.Exclude
    private List<Message> messagesRecus;
    @ToString.Exclude
    @JsonIgnore
    @OneToMany(mappedBy = "utilisateur")
    private List<Humeur> humeurs;
    
    @JsonIgnore
    @OneToMany(mappedBy = "auteur", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<SujetForum> sujets = new ArrayList<>();

 

}
