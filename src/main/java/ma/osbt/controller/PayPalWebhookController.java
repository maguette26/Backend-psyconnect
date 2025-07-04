package ma.osbt.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ma.osbt.service.ReservationService;

@RestController
@RequestMapping("/webhook/paypal")
public class PayPalWebhookController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> body) {
        System.out.println("Webhook PayPal reçu : " + body);

        String eventType = (String) body.get("event_type");
        System.out.println("Type d'événement PayPal reçu : " + eventType);

        if ("PAYMENT.SALE.COMPLETED".equals(eventType)) {
            Map<String, Object> resource = (Map<String, Object>) body.get("resource");
            if (resource != null) {
                String invoiceNumber = (String) resource.get("invoice_number");
                System.out.println("InvoiceNumber reçu : " + invoiceNumber);
                if (invoiceNumber != null) {
                    try {
                        Long reservationId = Long.parseLong(invoiceNumber);
                        System.out.println("Appel de handlePaymentSucceeded avec reservationId=" + reservationId);
                        reservationService.handlePaymentSucceeded(reservationId);
                        System.out.println("Réservation mise à jour en PAYEE avec id " + reservationId);
                    } catch (NumberFormatException e) {
                        System.err.println("InvoiceNumber non convertible en Long : " + invoiceNumber);
                        e.printStackTrace();
                    } catch (Exception e) {
                        System.err.println("Erreur lors de la mise à jour réservation : " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("invoice_number manquant dans la ressource du webhook");
                }
            } else {
                System.err.println("Ressource manquante dans le webhook");
            }
        } else {
            System.out.println("Événement ignoré : " + eventType);
        }

        return ResponseEntity.ok("ok");
    }

}
