package ma.osbt.service.implementation;

import org.springframework.stereotype.Service;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

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

        // ✅ Fallback prix
        Double prix = reservation.getPrix();

        if (prix == null || prix <= 0) {
            if (reservation.getProfessionnel() != null) {
                prix = reservation.getProfessionnel().getPrixConsultation();
                System.out.println("⚠️ Fallback pro : " + prix);
            }
        }

        if (prix == null || prix <= 0) {
            throw new RuntimeException("Prix introuvable pour la réservation #" + reservationId);
        }

        long amountInCents = Math.round(prix * 100);

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
        return intent.getClientSecret();
    }
    @Override
    public String createPremiumPaymentIntent(
            String planId,
            String currency,
            String successUrl,
            String cancelUrl,
            String userId) throws Exception {

        long amount;

        switch (planId) {
            case "monthly":
                amount = 300; // 3€
                break;

            case "quarterly":
                amount = 1000; // 10€
                break;

            case "annually":
                amount = 3000; // 30€
                break;

            default:
                throw new RuntimeException("Plan invalide");
        }

        PaymentIntentCreateParams params =
            PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency(currency.toLowerCase())
                .putMetadata("user_id", userId)
                .putMetadata("plan", planId)
                .setDescription("Abonnement Premium " + planId)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        return intent.getClientSecret();
    }
 

    public String createPremiumCheckoutSession(
            String planId,
            String userId) throws Exception {

        long amount;
        String planName;

        switch (planId) {

            case "monthly":
                amount = 300L;
                planName = "Premium Mensuel";
                break;

            case "quarterly":
                amount = 1000L;
                planName = "Premium Trimestriel";
                break;

            case "annually":
                amount = 3000L;
                planName = "Premium Annuel";
                break;

            default:
                throw new RuntimeException("Plan invalide");
        }

        SessionCreateParams params =
            SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(
                    "https://frontend-psyconnect.vercel.app/premium-success?session_id={CHECKOUT_SESSION_ID}"
                )
                .setCancelUrl(
                    "https://frontend-psyconnect.vercel.app/devenir-premium"
                )
                .putMetadata("user_id", userId)
                .putMetadata("plan", planId)
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount(amount)
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(planName)
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build();

        Session session = Session.create(params);

        return session.getUrl();
    }
}
