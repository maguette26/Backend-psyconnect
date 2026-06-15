package ma.osbt.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

import jakarta.servlet.http.HttpServletRequest;

import ma.osbt.service.ReservationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import ma.osbt.entitie.Role;
import ma.osbt.entitie.Utilisateur;
import ma.osbt.repository.UtilisateurRepository;

@RestController
@RequestMapping("/webhook/stripe")
public class StripeWebhookController {
	
    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Autowired
    private ReservationService reservationService;
    
    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request,
                                                      @RequestHeader("Stripe-Signature") String sigHeader) {
        System.out.println("Webhook Stripe reçu !");
        System.out.println("🔥🔥🔥 WEBHOOK REÇU");
        try {
            String payload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("🔔 Stripe Webhook reçu : " + payload);

            Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            System.out.println("✅ Événement Stripe : " + event.getType());
            if ("checkout.session.completed".equals(event.getType())) {

                com.google.gson.JsonObject jsonObject =
                    com.google.gson.JsonParser.parseString(payload)
                        .getAsJsonObject();

                com.google.gson.JsonObject metadata =
                    jsonObject
                        .getAsJsonObject("data")
                        .getAsJsonObject("object")
                        .getAsJsonObject("metadata");

                String email =
                	    metadata.get("email").getAsString();

                	Utilisateur utilisateur =
                	    utilisateurRepository.findByEmail(email)
                	        .orElse(null);
                	if (utilisateur != null) {

                	    utilisateur.setRole(Role.PREMIUM);

                	    utilisateurRepository.save(utilisateur);

                	    System.out.println(
                	        "✅ PREMIUM activé pour : "
                	        + utilisateur.getEmail()
                	    );
                	}
                return ResponseEntity.ok("Premium activé");
            }
            

            if ("payment_intent.succeeded".equals(event.getType())) {
                // Récupérer l'ID du PaymentIntent depuis le JSON brut (payload)
                // Ici, on parse le JSON pour extraire l'id sans utiliser getObjectDeserializer
                com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(payload).getAsJsonObject();
                String paymentIntentId = jsonObject
                    .getAsJsonObject("data")
                    .getAsJsonObject("object")
                    .get("id")
                    .getAsString();

                System.out.println("ID PaymentIntent extrait : " + paymentIntentId);

                // Récupérer le PaymentIntent via Stripe API
                PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

                String reservationIdStr = paymentIntent.getMetadata().get("reservation_id");
                System.out.println("📦 Metadata reçue : " + paymentIntent.getMetadata());

                if (reservationIdStr != null) {
                    Long reservationId = Long.parseLong(reservationIdStr);
                    System.out.println("🎯 Reservation ID trouvé dans metadata : " + reservationId);

                    reservationService.handlePaymentSucceeded(reservationId);

                    return ResponseEntity.ok("✅ Paiement réussi et réservation mise à jour.");
                } else {
                    System.err.println("❌ Métadonnée 'reservation_id' manquante !");
                    return ResponseEntity.badRequest().body("Métadonnée 'reservation_id' manquante.");
                }
            }

            return ResponseEntity.ok("⏩ Événement ignoré : " + event.getType());

        } catch (SignatureVerificationException e) {
            System.err.println("❌ Signature Stripe invalide : " + e.getMessage());
            return ResponseEntity.status(400).body("Signature invalide.");
        } catch (IOException e) {
            System.err.println("❌ Erreur lecture payload : " + e.getMessage());
            return ResponseEntity.status(500).body("Erreur lecture payload.");
        } catch (Exception e) {
            System.err.println("❌ Erreur serveur : " + e.getMessage());
            return ResponseEntity.status(500).body("Erreur serveur: " + e.getMessage());
        }
    }

}
