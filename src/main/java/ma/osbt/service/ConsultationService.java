package ma.osbt.service;

import java.util.List;

import ma.osbt.entitie.Consultation;
import ma.osbt.entitie.StatutConsultation;

public interface ConsultationService {
	   Consultation findById(Long idConsultation);
	   
	   public List<Consultation> getConsultationsByUtilisateurEtStatut(Long utilisateurId, StatutConsultation statut);
	   void verifierEtTerminerConsultations();
	List<Consultation> getConsultationsParPersonneId(Long personneId);
	public List<Consultation> getConsultationsParProfessionnelId(Long professionnelId);

	void deleteById(Long id);


}
