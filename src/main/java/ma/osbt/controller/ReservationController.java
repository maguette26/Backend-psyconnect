package ma.osbt.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import jakarta.transaction.Transactional;
import ma.osbt.entitie.Personne;
import ma.osbt.entitie.ProfessionnelSanteMentale;
import ma.osbt.entitie.Reservation;
import ma.osbt.entitie.ReservationStatut;
import ma.osbt.entitie.Utilisateur;
import ma.osbt.service.NotificationService;
import ma.osbt.service.PaymentService;
import ma.osbt.service.ReservationService;
import ma.osbt.service.implementation.PdfGeneratorService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PdfGeneratorService pdfGeneratorService;

    private final ReservationService reservationService;
    private final PaymentService paypalPaymentService;
    private final PaymentService stripePaymentService;

    public ReservationController(
            ReservationService reservationService,
            @Qualifier("payPalPaymentService") PaymentService paypalPaymentService,
            @Qualifier("stripePaymentService") PaymentService stripePaymentService) {
        this.reservationService = reservationService;
        this.paypalPaymentService = paypalPaymentService;
        this.stripePaymentService = stripePaymentService;
    }
    @Transactional
    @PostMapping
    public Reservation creerReservation(
            @RequestBody Reservation reservation,
            @AuthenticationPrincipal Personne personneConnectee) {
    	 System.out.println("création réservation appelée avec : " + reservation);
        if (!(personneConnectee instanceof Utilisateur utilisateur)) {
            throw new RuntimeException("L'utilisateur connecté n'est pas de type Utilisateur");
        }

        reservation.setUtilisateur(utilisateur);

        if (reservation.getHeureConsultation() == null) {
            throw new RuntimeException("L'heure de consultation doit être spécifiée");
        }


        if (reservation.getDateReservation() == null) {
            reservation.setDateReservation(java.sql.Date.valueOf(LocalDate.now()));
        }

        reservation.setStatut(ReservationStatut.EN_ATTENTE);
        return reservationService.save(reservation);
    }


    @GetMapping("/{id}")
    public Optional<Reservation> getReservationParId(@PathVariable Long id) {
        return reservationService.getById(id);
    }

    @GetMapping("/pro/{proId}")
    public List<Map<String, Object>> getReservationsPourProfessionnel(@PathVariable Long proId) {

        ProfessionnelSanteMentale pro = new ProfessionnelSanteMentale();
        pro.setId(proId);

        List<Reservation> reservations = reservationService.getReservationsPourPro(pro);

        DateTimeFormatter heureFormatter = DateTimeFormatter.ofPattern("HH:mm");

        return reservations.stream().map(r -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("utilisateur", r.getUtilisateur() != null ? Map.of(
            	    "id", r.getUtilisateur().getId(),
            	    "nom", r.getUtilisateur().getNom(),
            	    "prenom", r.getUtilisateur().getPrenom(),
            	    "email", r.getUtilisateur().getEmail()
            	) : null);

            	dto.put("id", r.getId());
            	dto.put("statut", r.getStatut().name());
            	dto.put("dateReservation", r.getDateReservation());

            	if (r.getHeureReservation() != null) {
            	    dto.put("heureReservation", r.getHeureReservation().format(heureFormatter));
            	}

            	if (r.getHeureConsultation() != null) {
            	    dto.put("heureConsultation", r.getHeureConsultation().format(heureFormatter));
            	}
            // ✅ HEURE FIX (PLUS DE 00:00)
            if (r.getDisponibilite() != null &&
            	    r.getDisponibilite().getHeureDebut() != null) {

            	    dto.put(
            	        "heureDebut",
            	        r.getDisponibilite().getHeureDebut().format(heureFormatter)
            	    );
            	}

            return dto;
        }).toList();
    }

    @PutMapping("/{id}/statut")
    public Reservation validerOuRefuserReservation(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Personne personne
    ) {
        String statut = body.get("statut");

        if (!(personne instanceof ProfessionnelSanteMentale)) {
            throw new RuntimeException("Utilisateur non autorisé");
        }

        ProfessionnelSanteMentale professionnel = (ProfessionnelSanteMentale) personne;

        return reservationService.validerOuRefuserReservation(id, statut, professionnel);
    }
    @PostMapping("/payer/{id}")
    public ResponseEntity<String> marquerReservationPayee(@PathVariable Long id) {
        try {
            reservationService.markAsPaid(id);
            return ResponseEntity.ok("Réservation marquée comme payée");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erreur: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur serveur: " + e.getMessage());
        }
    }


    
    @GetMapping("/telecharger-recu/{id}")
    public ResponseEntity<byte[]> telechargerRecu(@PathVariable Long id) {
        Reservation reservation = reservationService.getById(id)
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        try {
            byte[] pdf = pdfGeneratorService.generateReceiptPdf(reservation);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "recu_reservation_" + id + ".pdf");

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du reçu : " + e.getMessage());
        }
    }


    

    @GetMapping("/mes-reservations")
    public List<Map<String, Object>> getMesReservations(@AuthenticationPrincipal Personne personneConnectee) {
        if (!(personneConnectee instanceof Utilisateur utilisateur)) {
            throw new RuntimeException("Accès non autorisé.");
        }

        List<Reservation> reservations = reservationService.getReservationsPourUtilisateur(utilisateur.getId());

        DateTimeFormatter heureFormatter = DateTimeFormatter.ofPattern("HH'H'mm");

        return reservations.stream().map(r -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", r.getId());
            dto.put("dateReservation", r.getDateReservation());

            if (r.getHeureReservation() != null) {
                dto.put("heureReservation", r.getHeureReservation().format(heureFormatter));
            }

            dto.put("statut", r.getStatut().name());
            dto.put("prix", r.getPrix());

            // Infos du professionnel
            dto.put("professionnelId", r.getProfessionnel().getId());
            dto.put("professionnelNom", r.getProfessionnel().getNom());

            // Infos de consultation via Disponibilité
            if (r.getDisponibilite() != null) {
                dto.put("jourConsultation", r.getDisponibilite().getDate());

                if (r.getDisponibilite().getHeureDebut() != null) {
                    dto.put("heureConsultation", r.getDisponibilite().getHeureDebut().format(heureFormatter));
                }
            }

            return dto;
        }).toList();
    }


    @DeleteMapping("/annuler/{id}")
    public ResponseEntity<?> annulerReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal Personne personne
    ) {
        try {
            Reservation reservationAnnulee = reservationService.annulerReservation(id, personne.getId());
            return ResponseEntity.ok(reservationAnnulee);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Erreur serveur"));
        }
    }

    @GetMapping("/impayees")
    public List<Map<String, Object>> getReservationsImpayees(@AuthenticationPrincipal Personne personneConnectee) {
        if (!(personneConnectee instanceof Utilisateur utilisateur)) {
            throw new RuntimeException("Accès non autorisé.");
        }

        List<Reservation> reservations = reservationService.getReservationsEnAttentePaiement(utilisateur.getId());

        DateTimeFormatter heureFormatter = DateTimeFormatter.ofPattern("HH'H'mm");

        return reservations.stream().map(r -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", r.getId());
            dto.put("dateReservation", r.getDateReservation());

            if (r.getHeureReservation() != null) {
                dto.put("heureReservation", r.getHeureReservation().format(heureFormatter));
            }

            dto.put("statut", r.getStatut().name());
            dto.put("prix", r.getPrix());

            dto.put("professionnelId", r.getProfessionnel().getId());
            dto.put("professionnelNom", r.getProfessionnel().getNom());

            if (r.getDisponibilite() != null) {
                dto.put("jourConsultation", r.getDisponibilite().getDate());
                if (r.getDisponibilite().getHeureDebut() != null) {
                    dto.put("heureConsultation", r.getDisponibilite().getHeureDebut().format(heureFormatter));
                }
            }

            return dto;
        }).toList();
    }
 
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }


}