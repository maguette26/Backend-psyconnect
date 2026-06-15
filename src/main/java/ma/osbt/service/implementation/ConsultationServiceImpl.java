package ma.osbt.service.implementation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ma.osbt.entitie.Consultation;
import ma.osbt.entitie.StatutConsultation;
import ma.osbt.repository.ConsultationRepository;
import ma.osbt.service.ConsultationService;

@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationRepository consultationRepository;

    @Override
    public Consultation findById(Long idConsultation) {
        return consultationRepository.findById(idConsultation)
                .orElseThrow(() -> new RuntimeException("Consultation introuvable"));
    }

    @Override
    public List<Consultation> getConsultationsParPersonneId(Long utilisateurId) {
        return consultationRepository.findByReservation_Utilisateur_Id(utilisateurId);
    }

    @Override
    public List<Consultation> getConsultationsByUtilisateurEtStatut(Long utilisateurId, StatutConsultation statut) {
        return consultationRepository.findByReservation_Utilisateur_IdAndStatut(utilisateurId, statut);
    }

    @Scheduled(cron = "0 0 * * * *") // toutes les heures
    public void verifierEtTerminerConsultations() {
        List<Consultation> consultations = consultationRepository.findAll();

        for (Consultation consultation : consultations) {
            if (consultation.getStatut() == StatutConsultation.CONFIRMEE && consultation.getDateConsultation() != null && consultation.getHeure() != null) {
                
                Date dateConsult = consultation.getDateConsultation();
                Instant instant = Instant.ofEpochMilli(dateConsult.getTime());
                LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();

                LocalDateTime dateHeure = LocalDateTime.of(localDate, consultation.getHeure());

                if (dateHeure.isBefore(LocalDateTime.now())) {
                    consultation.setStatut(StatutConsultation.TERMINEE);
                    consultationRepository.save(consultation);
                }
            }
        }
}
    @Override
    public List<Consultation> getConsultationsParProfessionnelId(Long professionnelId) {
        return consultationRepository.findByProfessionnelId(professionnelId);
    }
    public void deleteById(Long id) {
        consultationRepository.deleteById(id);
    }
    }
