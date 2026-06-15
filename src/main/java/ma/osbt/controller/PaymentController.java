package ma.osbt.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ma.osbt.dto.PremiumRequest;
import ma.osbt.entitie.Reservation;
import ma.osbt.repository.ReservationRepository;
import ma.osbt.service.PaymentService;
import ma.osbt.service.ReservationService;
import org.springframework.security.core.Authentication;
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    @Qualifier("stripePaymentService")
    private PaymentService stripePaymentService;
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

            System.err.println("========== ERREUR PAIEMENT ==========");
            System.err.println("Type : " + e.getClass().getName());
            System.err.println("Message : " + e.getMessage());

            e.printStackTrace();

            Throwable cause = e.getCause();
            while (cause != null) {
                System.err.println("Cause : " + cause.getClass().getName());
                System.err.println("Message : " + cause.getMessage());
                cause.printStackTrace();
                cause = cause.getCause();
            }

            return ResponseEntity.status(500).body(
                Map.of(
                    "error", e.getClass().getName(),
                    "message", e.getMessage() == null ? "Erreur inconnue" : e.getMessage()
                )
            );
        }
    }
    


    @PostMapping("/premium-checkout")
    public ResponseEntity<?> premiumCheckout(
            @RequestBody PremiumRequest request,
            Authentication authentication) {

        try {

            String email = authentication.getName();

            String url =
                stripePaymentService.createPremiumCheckoutSession(
                    request.getPlan(),
                    email
                );

            return ResponseEntity.ok(
                Map.of("url", url)
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error",
                    e.getMessage()
                ));
        }
    }
    

}
