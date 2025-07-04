package ma.osbt.service.implementation;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import ma.osbt.entitie.QRCodeGenerator;
import ma.osbt.entitie.Reservation;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service
public class PdfGeneratorService {

    public byte[] generateReceiptPdf(Reservation reservation) throws Exception {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, BaseColor.GRAY);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        DateTimeFormatter heureFormatter = DateTimeFormatter.ofPattern("HH'h'mm");

        // Titre principal
        Paragraph title = new Paragraph("\u25A0 Re\u00e7u de paiement PsyConnect", titleFont);
        title.setAlignment(Element.ALIGN_LEFT);
        title.setSpacingAfter(20);
        document.add(title);

        // Informations principales (réservation)
        addLabelValue(document, "N\u00b0:", String.valueOf(reservation.getId()), labelFont, valueFont);
        addLabelValue(document, "Date de r\u00e9servation:", dateFormat.format(reservation.getDateReservation()), labelFont, valueFont);

        String heureResa = (reservation.getHeureReservation() != null) ? reservation.getHeureReservation().format(heureFormatter) : "—";
        addLabelValue(document, "Heure de r\u00e9servation:", heureResa, labelFont, valueFont);

        if (reservation.getConsultation() != null) {
            addLabelValue(document, "Date de consultation:", dateFormat.format(reservation.getConsultation().getDateConsultation()), labelFont, valueFont);
            String heureConsult = (reservation.getConsultation().getHeure() != null)
                    ? reservation.getConsultation().getHeure().format(heureFormatter)
                    : "—";
            addLabelValue(document, "Heure de consultation:", heureConsult, labelFont, valueFont);
        }

        addLabelValue(document, "Docteur:", "Dr. " + reservation.getProfessionnel().getNom(), labelFont, valueFont);

        int prix = (int) Math.round(reservation.getPrix());
        addLabelValue(document, "Montant pay\u00e9:", prix + "€", labelFont, valueFont);

        addLabelValue(document, "Date du paiement:", dateFormat.format(new Date()), labelFont, valueFont);

        // QR Code
        Paragraph qrText = new Paragraph("Scanner ce QR Code pour acc\u00e9der \u00e0 la plateforme :", valueFont);
        qrText.setAlignment(Element.ALIGN_CENTER);
        qrText.setSpacingBefore(30);
        document.add(qrText);

        Image qrImage = QRCodeGenerator.generateQRCodeImage("http://localhost:5173/", 150, 150);
        qrImage.setAlignment(Image.ALIGN_CENTER);
        document.add(qrImage);

        // Footer
        Paragraph footer = new Paragraph("Merci pour votre confiance. Rendez-vous sur psyConnect pour plus de services.", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        document.add(footer);

        document.close();
        return baos.toByteArray();
    }

    private void addLabelValue(Document document, String label, String value, Font labelFont, Font valueFont) throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", labelFont));
        p.add(new Chunk(value, valueFont));
        p.setSpacingAfter(5);
        document.add(p);
    }
}
