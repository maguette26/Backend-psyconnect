package ma.osbt.service.implementation;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import ma.osbt.entitie.Humeur;
import ma.osbt.repository.HumeurRepository;
import ma.osbt.service.HumeurService;

@Service
public class HumeurServiceImpl implements HumeurService {

    private final HumeurRepository humeurRepository;

    public HumeurServiceImpl(HumeurRepository humeurRepository) {
        this.humeurRepository = humeurRepository;
    }

    @Override
    public Humeur ajouterHumeur(Humeur humeur) {
    	if (humeurRepository.existsByUtilisateurIdAndDateAndEtatAndNoteJournal(
    	        humeur.getUtilisateur().getId(),
    	        humeur.getDate(),
    	        humeur.getEtat(),
    	        humeur.getNoteJournal())) {
    	    throw new RuntimeException("Cette humeur existe déjà.");
    	}
        return humeurRepository.save(humeur);
        
    }

    @Override
    public List<Humeur> getHumeursUtilisateur(Long utilisateurId) {
        return humeurRepository.findByUtilisateurIdOrderByDateDesc(utilisateurId);
    }
    @Override
    public void supprimerHumeur(Long id) {
        humeurRepository.deleteById(id);
    }
    @Override
    public Humeur modifierHumeur(Long id, Humeur humeurModifiee) {
        return humeurRepository.findById(id).map(existing -> {
            existing.setEtat(humeurModifiee.getEtat());
            existing.setNoteJournal(humeurModifiee.getNoteJournal());
            existing.setDate(humeurModifiee.getDate());
            return humeurRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Humeur non trouvée"));
    }
    @Override
    public List<Humeur> getHumeursParDate(Long utilisateurId, LocalDate date) {
        return humeurRepository.findByUtilisateurIdAndDate(utilisateurId, date);
    }

    @Override
    public List<Humeur> getHumeursParPeriode(Long utilisateurId, LocalDate debut, LocalDate fin) {
        return humeurRepository.findByUtilisateurIdAndDateBetween(utilisateurId, debut, fin);
    }

}
