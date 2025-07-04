package ma.osbt.service;

import java.util.List;
import java.util.Optional;

import ma.osbt.entitie.Fonctionnalite;

public interface FonctionnaliteService {
	public List<Fonctionnalite> listFonctionnalites();
	public Optional<Fonctionnalite> fonctionnaliteParId(Long id); 
	public Fonctionnalite ajouterFonctionnalite(Fonctionnalite fonctionnalite);
	public void modifierFonctionnalite(Fonctionnalite fonctionnalite); 
	public void supprimerFonctionnalite(Long id);
	public List<Fonctionnalite> listCitations();
	public List<Fonctionnalite> listRessources();
    public List<Fonctionnalite> listRessourcesParCategorie(String categorie);
    public List<Fonctionnalite> listPremiumParType(String type);

}
