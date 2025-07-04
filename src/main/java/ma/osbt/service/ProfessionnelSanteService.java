package ma.osbt.service;

import java.util.List;

import ma.osbt.entitie.ProfessionnelSanteMentale;
import ma.osbt.entitie.StatutValidation;

public interface ProfessionnelSanteService {
	
	public ProfessionnelSanteMentale saveProfessionnel(ProfessionnelSanteMentale professionnel);
	public List<ProfessionnelSanteMentale> getProfessionnelsEnAttente();
    public List<ProfessionnelSanteMentale> getAllProfessionnels();
	public ProfessionnelSanteMentale updateStatutValidation(Long id, StatutValidation statut);
	 
	 
     
}

