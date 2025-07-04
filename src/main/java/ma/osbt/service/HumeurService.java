package ma.osbt.service;

import java.time.LocalDate;
import java.util.List;
import ma.osbt.entitie.Humeur;

public interface HumeurService {
    public Humeur ajouterHumeur(Humeur humeur);
    public List<Humeur> getHumeursUtilisateur(Long utilisateurId);
    public void supprimerHumeur(Long id);
    public Humeur modifierHumeur(Long id, Humeur humeurModifiee);
    public List<Humeur> getHumeursParDate(Long utilisateurId, LocalDate date);
    public List<Humeur> getHumeursParPeriode(Long utilisateurId, LocalDate debut, LocalDate fin);


}
