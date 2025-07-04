package ma.osbt.service.implementation;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ma.osbt.entitie.Fonctionnalite;
import ma.osbt.repository.FonctionnaliteRepository;
import ma.osbt.service.FonctionnaliteService;

@Service
public class FonctionnaliteServiceImpl implements FonctionnaliteService{
	 @Autowired
	private FonctionnaliteRepository fonctionnaliteRepository;
	@Override
	public List<Fonctionnalite> listFonctionnalites() {
		// TODO Auto-generated method stub
		return fonctionnaliteRepository.findAll();
	}

	@Override
	public Optional<Fonctionnalite> fonctionnaliteParId(Long id) {
		// TODO Auto-generated method stub
		return fonctionnaliteRepository.findById(id);
	}

	@Override
	public Fonctionnalite ajouterFonctionnalite(Fonctionnalite fonctionnalite) {
		return fonctionnaliteRepository.save(fonctionnalite);
		
	}

	@Override
	public void modifierFonctionnalite(Fonctionnalite fonctionnalite) {
		// TODO Auto-generated method stub
		  Optional<Fonctionnalite> fonctionnaliteExistante = fonctionnaliteRepository.findById(fonctionnalite.getId());
		    if (fonctionnaliteExistante.isPresent()) {
		        Fonctionnalite f = fonctionnaliteExistante.get();
		        f.setNom(fonctionnalite.getNom());
		        f.setType(fonctionnalite.getType());
		        f.setDescription(fonctionnalite.getDescription());
		        f.setPremium(fonctionnalite.isPremium());
		        f.setStatut(fonctionnalite.isStatut());
		        f.setCategorie(fonctionnalite.getCategorie());
		        fonctionnaliteRepository.save(f);
		    } else {
		        throw new RuntimeException("Fonctionnalité avec ID " + fonctionnalite.getId() + " non trouvée");
		    }
		}
	 

	@Override
	public void supprimerFonctionnalite(Long id) {
		fonctionnaliteRepository.deleteById(id);
		
	}
	
	@Override
	public List<Fonctionnalite> listCitations() {
	    return fonctionnaliteRepository.findByTypeAndPremiumFalseAndStatutTrue("CITATION");
	}

	@Override
	public List<Fonctionnalite> listRessources() {
	    return fonctionnaliteRepository.findByTypeAndPremiumFalseAndStatutTrue("RESSOURCE");
	}
	@Override
	public List<Fonctionnalite> listRessourcesParCategorie(String categorie) {
	    return fonctionnaliteRepository.findByCategorieAndTypeAndPremiumFalseAndStatutTrue(
	        categorie, "RESSOURCE"
	    );
	}
	@Override
	public List<Fonctionnalite> listPremiumParType(String type) {
	    return fonctionnaliteRepository.findByTypeAndPremiumTrueAndStatutTrue(type);
	}


}
