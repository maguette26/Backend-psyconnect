package ma.osbt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import jakarta.servlet.http.HttpServletRequest;
import ma.osbt.entitie.Role;
import ma.osbt.entitie.Utilisateur;
import ma.osbt.repository.UtilisateurRepository;
import ma.osbt.service.ReservationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/webhook/stripe")
public class StripeWebhookController {

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;
    
    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            HttpServletRequest request,
            @RequestHeader("Stripe-Signature") String sigHeader) {
    	   System.out.println("========== WEBHOOK RECU ==========");
    	    System.out.println("Signature : " + sigHeader);
    	    
        try {
            String payload = new String(
                    request.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            Event event = Webhook.constructEvent(
                    payload,
                    sigHeader,
                    endpointSecret
            );

            System.out.println("Stripe Event: " + event.getType());

            if ("checkout.session.completed".equals(event.getType())) {

                System.out.println("========== ACTIVATION PREMIUM ==========");

                Session session = mapper.readValue(
                        event.getData().getObject().toJson(),
                        Session.class
                );

                if (session == null) {
                    System.out.println("❌ Session NULL");
                    return ResponseEntity.badRequest().body("Session null");
                }

                System.out.println("Session ID : " + session.getId());
                System.out.println("Metadata : " + session.getMetadata());

                if (session.getMetadata() == null) {
                    return ResponseEntity.badRequest().body("Metadata absentes");
                }

                String email = session.getMetadata().get("email");

                System.out.println("Email : " + email);

                if (email == null) {
                    System.out.println("❌ Email manquant dans les metadata");
                    return ResponseEntity.badRequest().body("Email metadata manquant");
                }

                Utilisateur user = utilisateurRepository.findByEmail(email).orElse(null);

                if (user == null) {
                    System.out.println("❌ Utilisateur introuvable : " + email);
                    return ResponseEntity.badRequest().body("Utilisateur introuvable");
                }

                System.out.println("Ancien rôle : " + user.getRole());

                user.setRole(Role.PREMIUM);
                utilisateurRepository.save(user);

                System.out.println("Nouveau rôle : " + user.getRole());
                System.out.println("✅ PREMIUM activé pour : " + email);

                return ResponseEntity.ok("Premium activé");
            }

            // =========================
            // 2. PAYMENT INTENT (reservation)
            // =========================
            if ("payment_intent.succeeded".equals(event.getType())) {

            	PaymentIntent intent = mapper.readValue(
            	        event.getData().getObject().toJson(),
            	        PaymentIntent.class
            	);

                if (intent == null) {
                    System.out.println("PaymentIntent introuvable");
                    return ResponseEntity.ok("Ignored");
                }

                String reservationIdStr = null;

                if (intent.getMetadata() != null) {
                    reservationIdStr = intent.getMetadata().get("reservation_id");
                }

                if (reservationIdStr != null) {
                    Long reservationId = Long.parseLong(reservationIdStr);
                    reservationService.handlePaymentSucceeded(reservationId);
                }

                return ResponseEntity.ok("Payment OK");
            }

            return ResponseEntity.ok("Ignored event");

        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(400).body("Signature invalide");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Erreur IO");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur serveur: " + e.getMessage());
        }
    }
    
}