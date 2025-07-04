package ma.osbt.service.implementation;

import java.time.LocalDate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import ma.osbt.entitie.Disponibilite;
import ma.osbt.entitie.ProfessionnelSanteMentale;
import ma.osbt.entitie.Reservation;
import ma.osbt.entitie.ReservationStatut;
import ma.osbt.entitie.StatutValidation;
import ma.osbt.repository.DisponibiliteRepository;
import ma.osbt.repository.ProfessionnelSanteMentaleRepository;
import ma.osbt.repository.ReservationRepository;
import ma.osbt.service.DisponibiliteService;
import ma.osbt.service.NotificationService;

@Service
public class DisponibiliteServiceImpl implements DisponibiliteService {

    @Autowired
    private DisponibiliteRepository disponibiliteRepository;

    @Autowired
    private ProfessionnelSanteMentaleRepository professionnelRepository;

    @Autowired
    private UtilisateurConnecteService utilisateurConnecteService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ReservationRepository reservationRepository;

    private static final int DUREE_CONSULTATION_MINUTES = 45;

    private boolean hasRole(String role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_" + role));
    }

    @Override
    public Disponibilite ajouterDisponibilite(Disponibilite disponibilite) {
        if (!(hasRole("PSYCHOLOGUE") || hasRole("PSYCHIATRE"))) {
            throw new RuntimeException("Accès refusé : rôle insuffisant");
        }

        String email = utilisateurConnecteService.getEmailConnecte();
        ProfessionnelSanteMentale professionnel = professionnelRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Professionnel non trouvé"));

        disponibilite.setProfessionnel(professionnel);
        Disponibilite saved = disponibiliteRepository.save(disponibilite);

        notificationService.notifier(professionnel,
                "Nouvelle disponibilité ajoutée pour le " + saved.getDate() +
                        " de " + saved.getHeureDebut() + " à " + saved.getHeureFin());

        return saved;
    }

    @Override
    public List<Disponibilite> getDisponibilitesProfessionnelConnecte() {
        if (!(hasRole("PSYCHOLOGUE") || hasRole("PSYCHIATRE"))) {
            throw new RuntimeException("Accès refusé : rôle insuffisant");
        }

        String email = utilisateurConnecteService.getEmailConnecte();
        ProfessionnelSanteMentale professionnel = professionnelRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Professionnel non trouvé"));

        return disponibiliteRepository.findByProfessionnelId(professionnel.getId());
    }

    @Override
    public Disponibilite modifierDisponibilite(Long id, Disponibilite updated) {
        if (!(hasRole("PSYCHOLOGUE") || hasRole("PSYCHIATRE"))) {
            throw new RuntimeException("Accès refusé : rôle insuffisant");
        }

        String email = utilisateurConnecteService.getEmailConnecte();
        Disponibilite dispo = disponibiliteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Disponibilité non trouvée"));

        if (!dispo.getProfessionnel().getEmail().equals(email)) {
            throw new RuntimeException("Accès interdit");
        }

        dispo.setDate(updated.getDate());
        dispo.setHeureDebut(updated.getHeureDebut());
        dispo.setHeureFin(updated.getHeureFin());
        Disponibilite modifiee = disponibiliteRepository.save(dispo);

        notificationService.notifier(dispo.getProfessionnel(),
                "Vous avez modifié une disponibilité pour le " + dispo.getDate());

        return modifiee;
    }

    @Override
    public void supprimerDisponibilite(Long id) {
        if (!(hasRole("PSYCHOLOGUE") || hasRole("PSYCHIATRE"))) {
            throw new RuntimeException("Accès refusé : rôle insuffisant");
        }

        String email = utilisateurConnecteService.getEmailConnecte();
        Disponibilite dispo = disponibiliteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Disponibilité non trouvée"));

        if (!dispo.getProfessionnel().getEmail().equals(email)) {
            throw new RuntimeException("Accès interdit");
        }

        disponibiliteRepository.deleteById(id);

        notificationService.notifier(dispo.getProfessionnel(),
                "Une disponibilité pour le " + dispo.getDate() + " a été supprimée.");
    }

    @Override
    public List<Disponibilite> getDisponibilitesFiltrees(Long professionnelId, LocalDate date) {
        List<Disponibilite> dispos = disponibiliteRepository.findByProfessionnelIdAndDate(professionnelId, date);

        Date dateUtil = java.sql.Date.valueOf(date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        Date startDate = java.sql.Timestamp.valueOf(startOfDay);
        Date endDate = java.sql.Timestamp.valueOf(endOfDay);

        List<Reservation> reservations = reservationRepository
                .findByProfessionnelIdAndDateReservationBetweenAndStatutInAndStatutValidationIn(
                        professionnelId,
                        startDate,
                        endDate,
                        List.of(ReservationStatut.PAYEE),
                        List.of(StatutValidation.VALIDE)
                );

        List<Disponibilite> disponibles = new ArrayList<>();
        for (Disponibilite dispo : dispos) {
            disponibles.addAll(decouperDisponibiliteParReservations(dispo, reservations));
        }

        return disponibles;
    }
    private List<Disponibilite> decouperDisponibiliteParReservations(Disponibilite dispo, List<Reservation> reservations) {
        List<Disponibilite> sousCreneaux = new ArrayList<>();

        LocalTime currentStart = dispo.getHeureDebut();
        LocalTime heureFin = dispo.getHeureFin();

        while (currentStart.plusMinutes(DUREE_CONSULTATION_MINUTES).isBefore(heureFin) ||
               currentStart.plusMinutes(DUREE_CONSULTATION_MINUTES).equals(heureFin)) {

            Disponibilite sousCreneau = new Disponibilite();

            // Ici on fixe l'id du sous-creneau au même que la dispo parent
            sousCreneau.setId(dispo.getId());

            sousCreneau.setDate(dispo.getDate());
            sousCreneau.setHeureDebut(currentStart);
            sousCreneau.setHeureFin(currentStart.plusMinutes(DUREE_CONSULTATION_MINUTES));

            // Si tu as un champ reservee ou autre, tu peux gérer ici selon reservations

            sousCreneaux.add(sousCreneau);

            currentStart = currentStart.plusMinutes(DUREE_CONSULTATION_MINUTES);
        }

        return sousCreneaux;
    }

    @Override
    public List<Disponibilite> getDisponibilitesByProId(Long proId) {
        try {
            // Charge toutes les disponibilités du professionnel avec leurs réservations associées
            List<Disponibilite> dispos = disponibiliteRepository.findByProfessionnelIdWithReservations(proId);

            // Si besoin, tu peux filtrer ou traiter les dispos ici (ex: filtrer celles sans date)
            List<Disponibilite> disposValides = new ArrayList<>();
            for (Disponibilite dispo : dispos) {
                if (dispo.getDate() == null) {
                    System.err.println("Disponibilité avec date NULL pour id=" + dispo.getId());
                    continue;
                }
                disposValides.add(dispo);
            }

            return disposValides;
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des disponibilités : " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Disponibilite> getDisponibilitesPubliques() {
        List<Disponibilite> toutesDispos = disponibiliteRepository.findAll();

        return toutesDispos.stream()
                .filter(dispo -> !dispo.isReservee())  
                .collect(Collectors.toList());
    }

}