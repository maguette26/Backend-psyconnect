package ma.osbt.service.implementation;

import org.springframework.stereotype.Service;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ma.osbt.entitie.Reservation;
import ma.osbt.repository.ReservationRepository;
import ma.osbt.service.PaymentService;

@Service("stripePaymentService")
public class StripePaymentService implements PaymentService {

    @Value("${stripe.api.key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }
    @Autowired
    private ReservationRepository reservationRepository;
    @Override
    public String createPaymentIntent(Long amountInCentsIgnored, String currency,
                                      String successUrl, String cancelUrl,
                                      Long reservationId) throws StripeException {

        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        long amountInCents = (long) (reservation.getPrix() * 100);  

        System.out.println("➡️ Création du PaymentIntent pour la réservation ID = " + reservationId);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(amountInCents)
            .setCurrency(currency.toLowerCase())
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .build()
            )
            .putMetadata("reservation_id", reservationId.toString())
            .setDescription("Réservation #" + reservationId)
            .build();

        PaymentIntent intent = PaymentIntent.create(params);

        // ✅ Vérifie ce que Stripe a bien reçu
        System.out.println("✅ PaymentIntent créé : " + intent.getId());
        System.out.println("📦 Metadata envoyée : " + intent.getMetadata());

        return intent.getClientSecret();
    }

}
