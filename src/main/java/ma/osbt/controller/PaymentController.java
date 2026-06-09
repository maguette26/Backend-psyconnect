package ma.osbt.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ma.osbt.entitie.Reservation;
import ma.osbt.repository.ReservationRepository;
import ma.osbt.service.PaymentService;
import ma.osbt.service.ReservationService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    @Qualifier("stripePaymentService")
    private PaymentService stripePaymentService;
    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    @Qualifier("payPalPaymentService")
    private PaymentService payPalPaymentService;
    @Autowired
    private ReservationService reservationService;
    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@RequestBody Map<String, Object> data) {
        try {
            Long reservationId = Long.valueOf(data.get("reservationId").toString());
            String paymentMethod = data.get("paymentMethod").toString().toLowerCase();
            String successUrl = data.getOrDefault("successUrl", "").toString();
            String cancelUrl = data.getOrDefault("cancelUrl", "").toString();
            String currency = data.getOrDefault("currency", "EUR").toString();

            // ✅ Vérifier si la réservation existe
            Reservation reservation = reservationService.findById(reservationId);
            if (reservation == null) {
                return ResponseEntity.status(404).body("Réservation introuvable");
            }

            // ✅ Vérifier si le statut est bien EN_ATTENTE_PAIEMENT
            if (!"EN_ATTENTE_PAIEMENT".equals(reservation.getStatut().name())) {
                return ResponseEntity.badRequest().body("La réservation n'est pas en attente de paiement.");
            }

            String result;
            switch (paymentMethod) {
                case "stripe":
                    result = stripePaymentService.createPaymentIntent(null, currency, successUrl, cancelUrl, reservationId);
                    return ResponseEntity.ok(Map.of("clientSecret", result));
                case "paypal":
                    result = payPalPaymentService.createPaymentIntent(null, currency, successUrl, cancelUrl, reservationId);
                    return ResponseEntity.ok(Map.of("approvalUrl", result));
                default:
                    return ResponseEntity.badRequest().body("Méthode de paiement inconnue");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors de la création du paiement : " + e.getMessage());
        }
    }
    
    @PostMapping("/create-premium")
    public ResponseEntity<?> createPremiumPayment(@RequestBody Map<String, Object> data) {
        try {
            String planId = data.get("planId").toString(); // "monthly", "quarterly", "annually"
            String paymentMethod = data.get("paymentMethod").toString().toLowerCase();
            String successUrl = data.getOrDefault("successUrl", "").toString();
            String cancelUrl = data.getOrDefault("cancelUrl", "").toString();
            String currency = data.getOrDefault("currency", "EUR").toString();
            String userId = data.get("userId").toString(); // ID utilisateur

            String result;
            switch (paymentMethod) {
                case "stripe":
                    result = stripePaymentService.createPremiumPaymentIntent(planId, currency, successUrl, cancelUrl, userId);
                    return ResponseEntity.ok(Map.of("clientSecret", result));

                case "paypal":
                    result = payPalPaymentService.createPremiumPaymentIntent(planId, currency, successUrl, cancelUrl, userId);
                    return ResponseEntity.ok(Map.of("approvalUrl", result));

                default:
                    return ResponseEntity.badRequest().body("Méthode de paiement inconnue");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors de la création du paiement Premium : " + e.getMessage());
        }
    }
    
    

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody Map<String, Object> body) {

        Long reservationId = Long.valueOf(body.get("reservationId").toString());

        Reservation res = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        // 🔥 éviter double paiement
        if ("PAYE".equals(res.getStatut())) {
            return ResponseEntity.ok("Déjà payé");
        }

        reservationService.handlePaymentSucceeded(reservationId);
        System.out.println("💳 Payment confirmé pour reservation " + reservationId);
        return ResponseEntity.ok("OK");
    }
    

}
