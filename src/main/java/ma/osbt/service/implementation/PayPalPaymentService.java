package ma.osbt.service.implementation;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.paypal.api.payments.Amount;
import com.paypal.api.payments.Payer;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.RedirectUrls;
import com.paypal.api.payments.Transaction;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;

import jakarta.annotation.PostConstruct;
import ma.osbt.entitie.Reservation;
import ma.osbt.repository.ReservationRepository;
import ma.osbt.service.PaymentService;

@Service("payPalPaymentService")
public class PayPalPaymentService implements PaymentService {

    @Value("${paypal.client.id}")
    private String clientId;

    @Value("${paypal.client.secret}")
    private String clientSecret;

    @Value("${paypal.mode}")
    private String mode;

    private APIContext apiContext;

    @PostConstruct
    public void init() {
        apiContext = new APIContext(clientId, clientSecret, mode);
    }

    @Autowired
    private ReservationRepository reservationRepository;

    @Override
    public String createPaymentIntent(Long amountInCentsIgnored, String currency,
                                      String successUrl, String cancelUrl,
                                      Long reservationId) throws PayPalRESTException {

        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        // Correction ici : forcer le format US (12.34, pas 12,34)
        String total = String.format(Locale.US, "%.2f", reservation.getPrix());

        Amount amount = new Amount();
        amount.setCurrency(currency);
        amount.setTotal(total);

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription("Paiement pour consultation");
        transaction.setInvoiceNumber(String.valueOf(reservationId));

        List<Transaction> transactions = Collections.singletonList(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl);
        payment.setRedirectUrls(redirectUrls);

        Payment createdPayment = payment.create(apiContext);
        return createdPayment.getLinks().stream()
            .filter(link -> "approval_url".equals(link.getRel()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Lien d'approbation introuvable"))
            .getHref();
    }

	@Override
	public String createPremiumPaymentIntent(String planId, String currency, String successUrl, String cancelUrl,
			String userId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String createPremiumCheckoutSession(String planId, String userId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
