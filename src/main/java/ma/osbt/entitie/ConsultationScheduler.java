package ma.osbt.entitie;

import org.springframework.beans.factory.annotation.Autowired;

//ConsultationScheduler.java

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ma.osbt.service.ConsultationService;

@Component
public class ConsultationScheduler {

 @Autowired
 private ConsultationService consultationService;

 // Vérifie toutes les 5 minutes
 @Scheduled(fixedRate = 300000)
 public void verifierConsultationsTerminees() {
     consultationService.verifierEtTerminerConsultations();
 }
}
