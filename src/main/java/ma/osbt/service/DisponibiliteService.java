package ma.osbt.service;

import java.time.LocalDate;
import java.util.List;

import ma.osbt.entitie.Disponibilite;

public interface DisponibiliteService {

	 public Disponibilite ajouterDisponibilite(Disponibilite disponibilite);
	 public void supprimerDisponibilite(Long id);
	 public Disponibilite modifierDisponibilite(Long id, Disponibilite updated);
	public List<Disponibilite> getDisponibilitesProfessionnelConnecte();
	List<Disponibilite> getDisponibilitesFiltrees(Long professionnelId, LocalDate date);
	public List<Disponibilite> getDisponibilitesByProId(Long proId);
	List<Disponibilite> getDisponibilitesPubliques();
	

}
