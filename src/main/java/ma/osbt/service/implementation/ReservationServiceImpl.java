package ma.osbt.service.implementation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ma.osbt.entitie.Consultation;
import ma.osbt.entitie.Disponibilite;
import ma.osbt.entitie.Reservation;
import ma.osbt.entitie.ReservationStatut;
import ma.osbt.entitie.StatutConsultation;
import ma.osbt.entitie.StatutValidation;
import ma.osbt.entitie.ProfessionnelSanteMentale;
import ma.osbt.repository.ConsultationRepository;
import ma.osbt.repository.DisponibiliteRepository;
import ma.osbt.repository.ReservationRepository;
import ma.osbt.service.NotificationService;
import ma.osbt.service.ReservationService;

@Service
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final DisponibiliteRepository disponibiliteRepository;
    private final ReservationRepository reservationRepository;
    private final ConsultationRepository consultationRepository;
    private final NotificationService notificationService;
    private final PdfGeneratorService pdfGeneratorService;

    private static final int DUREE_CONSULTATION_MINUTES = 45;

    public ReservationServiceImpl(
        DisponibiliteRepository disponibiliteRepository,
        ReservationRepository reservationRepository,
        ConsultationRepository consultationRepository,
        NotificationService notificationService,
        PdfGeneratorService pdfGeneratorService
    ) {
        this.disponibiliteRepository = disponibiliteRepository;
        this.reservationRepository = reservationRepository;
        this.consultationRepository = consultationRepository;
        this.notificationService = notificationService;
        this.pdfGeneratorService = pdfGeneratorService;
    }

    @Override
    public Reservation save(Reservation reservation) {
        System.out.println(">>> ReservationServiceImpl.save() appelée avec: " + reservation);

        // 1. Vérification de l'ID de disponibilité
        Long dispoId = reservation.getDisponibilite() != null ? reservation.getDisponibilite().getId() : null;
        if (dispoId == null) {
            throw new RuntimeException("L'id de disponibilité doit être précisé");
        }

        Disponibilite disponibilite = disponibiliteRepository.findById(dispoId)
            .orElseThrow(() -> new RuntimeException("Disponibilité non trouvée"));

        ProfessionnelSanteMentale pro = disponibilite.getProfessionnel();
        if (pro == null) {
            throw new RuntimeException("Professionnel de santé non trouvé");
        }
        if (pro.getPrixConsultation() == null || pro.getPrixConsultation() <= 0) {
            throw new RuntimeException("Ce professionnel n'a pas encore défini son prix de consultation.");
        }
        LocalDate dateConsultation = disponibilite.getDate();
        LocalTime heureConsultation = reservation.getHeureConsultation();
        if (heureConsultation == null) {
            throw new RuntimeException("L'heure de consultation doit être spécifiée");
        }

        LocalTime heureFinConsultation = heureConsultation.plusMinutes(DUREE_CONSULTATION_MINUTES);
        if (heureConsultation.isBefore(disponibilite.getHeureDebut())
            || heureFinConsultation.isAfter(disponibilite.getHeureFin())) {
            throw new RuntimeException("Créneau horaire hors des heures de disponibilité");
        }

        // Vérifie si l'utilisateur a déjà réservé ce créneau
        Long userId = reservation.getUtilisateur() != null ? reservation.getUtilisateur().getId() : null;
        if (userId == null) {
            throw new RuntimeException("Utilisateur non spécifié");
        }

        boolean dejaReserve = reservationRepository.existsByUtilisateurIdAndDisponibiliteIdAndHeureConsultationAndStatutIn(
            userId,
            dispoId,
            heureConsultation,
            List.of(
                ReservationStatut.EN_ATTENTE,
                ReservationStatut.EN_ATTENTE_PAIEMENT,
                ReservationStatut.PAYEE
            )
        );

        if (dejaReserve) {
            throw new RuntimeException("Vous avez déjà réservé ce créneau.");
        }

        // Vérifie si l'utilisateur a déjà une réservation avec ce professionnel le même jour
        boolean dejaReserveJour = reservationRepository.existsByUtilisateurIdAndProfessionnelIdAndDateReservationAndStatutIn(
            userId,
            pro.getId(),
            java.sql.Date.valueOf(dateConsultation),
            List.of(
                ReservationStatut.EN_ATTENTE,
                ReservationStatut.EN_ATTENTE_PAIEMENT,
                ReservationStatut.PAYEE
            )
        );

        if (dejaReserveJour) {
            throw new RuntimeException("Vous avez déjà une réservation avec ce professionnel ce jour-là.");
        }

        // Vérification manuelle des chevauchements
        List<Reservation> reservationsExistantes = reservationRepository.findByProfessionnelAndDateReservation(
            pro, java.sql.Date.valueOf(dateConsultation)
        );

        for (Reservation r : reservationsExistantes) {
            if (!List.of(ReservationStatut.EN_ATTENTE, ReservationStatut.EN_ATTENTE_PAIEMENT, ReservationStatut.PAYEE)
                    .contains(r.getStatut())) {
                continue;
            }

            LocalTime rDebut = r.getHeureConsultation();
            LocalTime rFin = rDebut.plusMinutes(DUREE_CONSULTATION_MINUTES);

            if (heureConsultation.isBefore(rFin) && heureFinConsultation.isAfter(rDebut)) {
                throw new RuntimeException("Ce créneau est déjà réservé.");
            }
        }

        // Réservation autorisée : finalisation
        reservation.setProfessionnel(pro);
        reservation.setPrix(pro.getPrixConsultation()); // ✅ prix correct dès la création
        reservation.setHeureReservation(LocalTime.now());
        reservation.setDateReservation(java.sql.Date.valueOf(LocalDate.now()));

        if (reservation.getStatut() == null) {
            reservation.setStatut(ReservationStatut.EN_ATTENTE);
        }

        if (reservation.getStatutValidation() == null) {
            reservation.setStatutValidation(StatutValidation.EN_ATTENTE);
        }

        reservation.setDisponibilite(disponibilite);

        // Ajout explicite dans la liste de réservations de la disponibilité
        if (disponibilite.getReservations() != null) {
            disponibilite.getReservations().add(reservation);
        }

        Reservation saved = reservationRepository.save(reservation);

        // Mise à jour du champ reservee
        disponibilite.setReservee(true);
        disponibiliteRepository.save(disponibilite);

        return saved;
    }

    private boolean estDisponible(ProfessionnelSanteMentale pro, LocalDate date, LocalTime heureDebut, int dureeMinutes) {
        List<Consultation> consultations = consultationRepository.findByProfessionnelAndDateConsultation(pro, java.sql.Date.valueOf(date));
        LocalTime heureFin = heureDebut.plusMinutes(dureeMinutes);

        for (Consultation c : consultations) {
            LocalTime cDebut = c.getHeure();
            LocalTime cFin = cDebut.plusMinutes(c.getDureeMinutes());

            if (heureDebut.isBefore(cFin) && heureFin.isAfter(cDebut)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Optional<Reservation> getById(Long id) {
        return reservationRepository.findById(id);
    }

    @Override
    public List<Reservation> getReservationsPourPro(ProfessionnelSanteMentale professionnel) {
        return reservationRepository.findByProfessionnel(professionnel);
    }

    @Override
    public Reservation validerOuRefuserReservation(Long id, String statutStr, ProfessionnelSanteMentale professionnel) {
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        if (!reservation.getProfessionnel().getId().equals(professionnel.getId())) {
            throw new RuntimeException("Action non autorisée");
        }

        StatutValidation statutValidation;
        try {
            statutValidation = StatutValidation.valueOf(statutStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide");
        }

        String prenomUtilisateur = reservation.getUtilisateur().getPrenom();
        String nomUtilisateur = reservation.getUtilisateur().getNom();

        System.out.printf("👤 Réservation faite par : %s %s%n", prenomUtilisateur, nomUtilisateur);

        switch (statutValidation) {
            case VALIDE -> {
                reservation.setStatut(ReservationStatut.EN_ATTENTE_PAIEMENT);
                reservation.setStatutValidation(StatutValidation.VALIDE);
                reservationRepository.save(reservation);

                Optional<Consultation> optConsultation = consultationRepository.findByReservation(reservation);
                if (optConsultation.isEmpty()) {
                    Consultation consultation = new Consultation();
                    consultation.setDateConsultation(
                        java.sql.Date.valueOf(reservation.getDisponibilite().getDate())
                    );
                    // ✅ CORRECTION PRINCIPALE : heure du créneau choisi, pas l'heure de début de la dispo
                    consultation.setHeure(reservation.getHeureConsultation());
                    consultation.setDureeMinutes(DUREE_CONSULTATION_MINUTES);
                    consultation.setProfessionnel(reservation.getProfessionnel());
                    consultation.setReservation(reservation);
                    // ✅ prix récupéré depuis la réservation (= prixConsultation du pro, jamais 0)
                    consultation.setPrix(reservation.getPrix());
                    consultation.setStatut(StatutConsultation.EN_ATTENTE);
                    consultationRepository.save(consultation);
                } else {
                    // ✅ Correction des anciennes consultations créées avec la mauvaise heure
                    Consultation existing = optConsultation.get();
                    boolean updated = false;
                    if (!reservation.getHeureConsultation().equals(existing.getHeure())) {
                        existing.setHeure(reservation.getHeureConsultation());
                        updated = true;
                    }
                    if (existing.getPrix() == null || existing.getPrix() == 0) {
                        existing.setPrix(reservation.getPrix());
                        updated = true;
                    }
                    if (updated) {
                        consultationRepository.save(existing);
                        System.out.println("ℹ️ Consultation corrigée — heure: "
                            + reservation.getHeureConsultation() + ", prix: " + reservation.getPrix());
                    } else {
                        System.out.println("ℹ️ Consultation déjà à jour pour cette réservation");
                    }
                }

                String html = getMessageHtml(
                    "✅ Réservation validée",
                    "Bonjour <strong>%s</strong> 😊,".formatted(prenomUtilisateur),
                    "Votre réservation a été <strong>validée</strong> par le professionnel. Veuillez procéder au paiement.",
                    "#28a745"
                );
                notificationService.notifier(reservation.getUtilisateur(), html);
            }

            case REFUSE -> {
                reservation.setStatut(ReservationStatut.REFUSE);
                reservation.setStatutValidation(StatutValidation.REFUSE);
                reservationRepository.save(reservation);

                String html = getMessageHtml(
                    "❌ Réservation refusée",
                    "Bonjour <strong>%s</strong> 😞,".formatted(prenomUtilisateur),
                    "Votre réservation a été <strong>refusée</strong>. Vous pouvez essayer un autre créneau.",
                    "#dc3545"
                );
                notificationService.notifier(reservation.getUtilisateur(), html);
            }

            case EN_ATTENTE -> {
                throw new RuntimeException("Le statut EN_ATTENTE ne peut pas être appliqué ici.");
            }
        }

        return reservation;
    }

    @Override
    public void handlePaymentSucceeded(Long reservationId) {
        System.out.println("handlePaymentSucceeded appelé avec reservationId=" + reservationId);
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new RuntimeException("Réservation introuvable avec l'id: " + reservationId));

        if (ReservationStatut.PAYEE.equals(reservation.getStatut())) {
            System.out.println("Réservation déjà payée.");
            return;
        }

        if (!StatutValidation.VALIDE.equals(reservation.getStatutValidation())) {
            reservation.setStatutValidation(StatutValidation.VALIDE);
            System.out.println("Réservation validée automatiquement.");
        }

        reservation.setStatut(ReservationStatut.PAYEE);
        reservationRepository.save(reservation);

        Consultation consultation = consultationRepository.findByReservation(reservation)
            .orElseThrow(() -> new RuntimeException("Consultation non trouvée pour cette réservation"));

        consultation.setStatut(StatutConsultation.CONFIRMEE);
        consultationRepository.save(consultation);

        byte[] pdfBytes = null;
        try {
            pdfBytes = pdfGeneratorService.generateReceiptPdf(reservation);
        } catch (Exception e) {
            System.err.println("Erreur génération PDF : " + e.getMessage());
        }

        String html = getMessageHtml(
            "🎉 Paiement reçu",
            "Bonjour <strong>" + reservation.getUtilisateur().getPrenom() + "</strong> 🥳,",
            "Votre paiement pour la consultation du <strong>" + reservation.getDateReservation() + "</strong> avec <strong>"
            + reservation.getProfessionnel().getPrenom() + " " + reservation.getProfessionnel().getNom() + "</strong> a bien été reçu.",
            "#2E86C1"
        );

        if (pdfBytes != null) {
            notificationService.notifierAvecPieceJointe(
                reservation.getUtilisateur(),
                html,
                pdfBytes,
                "recu_paiement_" + reservation.getId() + ".pdf"
            );
        } else {
            notificationService.notifier(reservation.getUtilisateur(), html);
        }

        System.out.println("Réservation " + reservationId + " marquée comme PAYEE et consultation confirmée.");
    }

    @Override
    public List<Reservation> getReservationsPourUtilisateur(Long utilisateurId) {
        return reservationRepository.findByUtilisateurId(utilisateurId);
    }

    @Override
    public Reservation annulerReservation(Long reservationId, Long utilisateurId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        if (!reservation.getUtilisateur().getId().equals(utilisateurId)) {
            throw new RuntimeException("Action non autorisée");
        }

        if (!(ReservationStatut.EN_ATTENTE.equals(reservation.getStatut()) ||
              ReservationStatut.EN_ATTENTE_PAIEMENT.equals(reservation.getStatut()))) {
            throw new RuntimeException("Seules les réservations non payées peuvent être annulées.");
        }

        reservation.setStatut(ReservationStatut.ANNULEE);
        Reservation updated = reservationRepository.save(reservation);

        Disponibilite dispo = updated.getDisponibilite();
        if (dispo != null) {
            majReserveeDisponibilite(dispo);
        }

        String html = getMessageHtml(
            "⚠️ Réservation annulée",
            "Bonjour <strong>%s</strong>".formatted(updated.getProfessionnel().getPrenom()),
            "L'utilisateur <strong>%s</strong> a annulé sa réservation.".formatted(updated.getUtilisateur().getNom()),
            "#ffc107"
        );

        notificationService.notifier(updated.getProfessionnel(), html);

        return updated;
    }

    private String getMessageHtml(String titre, String salutation, String contenu, String couleur) {
        return """
            <html>
              <body style="font-family: Arial, sans-serif; color: #333; line-height: 1.6;">
                <div style="background-color: #f9f9f9; padding: 20px; border-radius: 10px; border-left: 5px solid %s;">
                  <h2 style="color: %s;">%s</h2>
                  <p>%s</p>
                  <p>%s</p>
                  <p style="margin-top: 20px;">L'équipe PsyConnect</p>
                </div>
              </body>
            </html>
            """.formatted(couleur, couleur, titre, salutation, contenu);
    }

    @Override
    public List<Reservation> getReservationsEnAttentePaiement(Long utilisateurId) {
        return reservationRepository.findByUtilisateurIdAndStatut(utilisateurId, ReservationStatut.EN_ATTENTE_PAIEMENT);
    }

    @Transactional
    public void markAsPaid(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        if (!ReservationStatut.EN_ATTENTE_PAIEMENT.equals(reservation.getStatut())) {
            throw new RuntimeException("Réservation pas en attente de paiement");
        }

        reservation.setStatut(ReservationStatut.PAYEE);
        reservationRepository.save(reservation);
    }

    @Override
    public Reservation findById(Long id) {
        return reservationRepository.findById(id).orElse(null);
    }

    private void majReserveeDisponibilite(Disponibilite disponibilite) {
        boolean aDesReservationsActives = reservationRepository.existsByDisponibiliteIdAndStatutIn(
            disponibilite.getId(),
            List.of(
                ReservationStatut.EN_ATTENTE,
                ReservationStatut.EN_ATTENTE_PAIEMENT,
                ReservationStatut.PAYEE
            )
        );

        if (!aDesReservationsActives && disponibilite.isReservee()) {
            disponibilite.setReservee(false);
            disponibiliteRepository.save(disponibilite);
        }
    }
}