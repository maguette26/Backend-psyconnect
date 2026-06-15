package ma.osbt.service;

public interface PaymentService {

    String createPaymentIntent(
            Long amountInCents,
            String currency,
            String successUrl,
            String cancelUrl,
            Long reservationId
    ) throws Exception;

    String createPremiumPaymentIntent(
            String planId,
            String currency,
            String successUrl,
            String cancelUrl,
            String userId
    ) throws Exception;

    String createPremiumCheckoutSession(
            String planId,
            String userId
    ) throws Exception;
}