package ma.osbt.entitie;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SujetForum {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;

    @Lob
    private String contenu;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateCreation = LocalDateTime.now();
    
     
    @ManyToOne
     private Personne auteur;
    private boolean anonyme = false;
    @JsonIgnore
    @OneToMany(mappedBy = "sujet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ReponseForum> reponses = new ArrayList<>();
    
    @JsonProperty("reponsesCount")
    public int getReponsesCount() {
        return reponses == null ? 0 : reponses.size();
    }

    @JsonProperty("derniereActivite")
    public LocalDateTime getDerniereActivite() {

        if (reponses == null || reponses.isEmpty()) {
            return dateCreation;
        }

        return reponses.stream()
                .map(ReponseForum::getDateReponse)
                .max(LocalDateTime::compareTo)
                .orElse(dateCreation);
    }
}

