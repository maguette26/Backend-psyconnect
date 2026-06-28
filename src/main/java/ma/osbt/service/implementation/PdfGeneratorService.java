package ma.osbt.service.implementation;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import ma.osbt.entitie.QRCodeGenerator;
import ma.osbt.entitie.Reservation;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service
public class PdfGeneratorService {

    // ─── Palette PsyConnect ────────────────────────────────────────────────────
    private static final BaseColor NAVY        = new BaseColor(0x0D, 0x2B, 0x55);   // #0D2B55
    private static final BaseColor TEAL        = new BaseColor(0x00, 0xA8, 0xB0);   // #00A8B0
    private static final BaseColor TEAL_LIGHT  = new BaseColor(0xE0, 0xF7, 0xF8);   // #E0F7F8
    private static final BaseColor GREEN       = new BaseColor(0x1A, 0xA1, 0x63);   // #1AA163
    private static final BaseColor GREEN_LIGHT = new BaseColor(0xD4, 0xF4, 0xE6);   // #D4F4E6
    private static final BaseColor GREY_LIGHT  = new BaseColor(0xF5, 0xF7, 0xFA);   // #F5F7FA
    private static final BaseColor GREY_LINE   = new BaseColor(0xE2, 0xE8, 0xF0);   // #E2E8F0
    private static final BaseColor GREY_TEXT   = new BaseColor(0x71, 0x80, 0x96);   // #718096
    private static final BaseColor WHITE       = BaseColor.WHITE;

    // ─── Fonts ────────────────────────────────────────────────────────────────
    private static final Font F_BRAND_TITLE  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  22, NAVY);
    private static final Font F_BRAND_SUB    = FontFactory.getFont(FontFactory.HELVETICA,        9, TEAL);
    private static final Font F_BADGE        = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, GREEN);
    private static final Font F_SECTION      = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  10, TEAL);
    private static final Font F_LABEL        = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  10, NAVY);
    private static final Font F_VALUE        = FontFactory.getFont(FontFactory.HELVETICA,        10, new BaseColor(0x2D, 0x3A, 0x4A));
    private static final Font F_AMOUNT_LBL   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  11, NAVY);
    private static final Font F_AMOUNT_VAL   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  28, GREEN);
    private static final Font F_QR_TITLE     = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  11, NAVY);
    private static final Font F_QR_SUB       = FontFactory.getFont(FontFactory.HELVETICA,         9, GREY_TEXT);
    private static final Font F_FOOTER       = FontFactory.getFont(FontFactory.HELVETICA,         8, GREY_TEXT);
    private static final Font F_FOOTER_BOLD  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   8, GREY_TEXT);

    // ─── Main method ──────────────────────────────────────────────────────────
    public byte[] generateReceiptPdf(Reservation reservation) throws Exception {
        Document document = new Document(PageSize.A4, 50, 50, 40, 40);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.open();

        SimpleDateFormat dateFormat      = new SimpleDateFormat("dd/MM/yyyy");
        DateTimeFormatter heureFormatter = DateTimeFormatter.ofPattern("HH'h'mm");

        // ── 1. En-tête ────────────────────────────────────────────────────────
        addHeader(document);

        // ── 2. Badge "Paiement confirmé" ──────────────────────────────────────
        addBadge(document);

        spacer(document, 14);

        // ── 3. Tableau d'informations ─────────────────────────────────────────
        String heureResa = (reservation.getHeureReservation() != null)
                ? reservation.getHeureReservation().format(heureFormatter) : "—";

        String dateConsult = "—", heureConsult = "—";
        if (reservation.getConsultation() != null) {
            dateConsult  = dateFormat.format(reservation.getConsultation().getDateConsultation());
            heureConsult = (reservation.getConsultation().getHeure() != null)
                    ? reservation.getConsultation().getHeure().format(heureFormatter) : "—";
        }

        String docteur = "Dr. " + reservation.getProfessionnel().getNom();
        int prix = (int) Math.round(reservation.getPrix());

        addInfoTable(document,
                String.valueOf(reservation.getId()),
                dateFormat.format(reservation.getDateReservation()),
                heureResa,
                dateConsult,
                heureConsult,
                docteur,
                dateFormat.format(new Date()));

        spacer(document, 16);

        // ── 4. Bloc montant ───────────────────────────────────────────────────
        addAmountBlock(document, prix);

        spacer(document, 20);

        // ── 5. Séparateur ─────────────────────────────────────────────────────
        addSeparator(document, writer);

        spacer(document, 16);

        // ── 6. Section QR Code ────────────────────────────────────────────────
        Long consultationId = (reservation.getConsultation() != null)
                ? reservation.getConsultation().getId() : reservation.getId();
        String consultationUrl = "https://frontend-psyconnect.vercel.app//access/consultation/" + consultationId;

        addQrSection(document, consultationUrl);

        spacer(document, 20);

        // ── 7. Pied de page ───────────────────────────────────────────────────
        addFooter(document, writer);

        document.close();
        return baos.toByteArray();
    }

    // ─── En-tête ──────────────────────────────────────────────────────────────
    private void addHeader(Document document) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1f, 1f});
        header.setSpacingAfter(10);

        // Colonne gauche : logo / nom
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.setPaddingBottom(6);

        Paragraph brand = new Paragraph();
        brand.add(new Chunk("PsyConnect", F_BRAND_TITLE));
        brand.add(Chunk.NEWLINE);
        Chunk tagline = new Chunk("PLATEFORME DE SANTÉ MENTALE", F_BRAND_SUB);
        brand.add(tagline);
        left.addElement(brand);
        header.addCell(left);

        // Colonne droite : titre reçu aligné à droite
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setVerticalAlignment(Element.ALIGN_BOTTOM);

        Paragraph receiptLabel = new Paragraph();
        Font fTitle = FontFactory.getFont(FontFactory.HELVETICA, 10, GREY_TEXT);
        Font fNum   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, NAVY);
        receiptLabel.add(new Chunk("REÇU DE PAIEMENT", fTitle));
        receiptLabel.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(receiptLabel);
        header.addCell(right);

        document.add(header);

        // Ligne de séparation sous l'en-tête (bande TEAL)
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        rule.setSpacingAfter(14);
        PdfPCell ruleCell = new PdfPCell();
        ruleCell.setBackgroundColor(TEAL);
        ruleCell.setFixedHeight(3f);
        ruleCell.setBorder(Rectangle.NO_BORDER);
        rule.addCell(ruleCell);
        document.add(rule);
    }

    // ─── Badge ────────────────────────────────────────────────────────────────
    private void addBadge(Document document) throws DocumentException {
        PdfPTable badge = new PdfPTable(1);
        badge.setWidthPercentage(40);
        badge.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(GREEN_LIGHT);
        cell.setBorderColor(GREEN);
        cell.setBorderWidth(0.8f);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph p = new Paragraph("\u2713  Paiement confirmé", F_BADGE);
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        badge.addCell(cell);
        document.add(badge);
    }

    // ─── Tableau d'informations ───────────────────────────────────────────────
    private void addInfoTable(Document document,
                              String id, String dateResa, String heureResa,
                              String dateConsult, String heureConsult,
                              String docteur, String datePaiement) throws DocumentException {

        // Titre de section
        Paragraph sectionTitle = new Paragraph("DÉTAILS DE LA RÉSERVATION", F_SECTION);
        sectionTitle.setSpacingAfter(8);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.2f, 3.8f});
        table.setSpacingAfter(6);

        String[][] rows = {
                {"N° du reçu",          id},
                {"Date de réservation", dateResa},
                {"Heure de réservation",heureResa},
                {"Date de consultation", dateConsult},
                {"Heure de consultation",heureConsult},
                {"Professionnel",        docteur},
                {"Mode de paiement",     "Carte bancaire"},
                {"Date du paiement",     datePaiement},
        };

        boolean alternate = false;
        for (String[] row : rows) {
            BaseColor rowBg = alternate ? GREY_LIGHT : WHITE;
            alternate = !alternate;

            PdfPCell labelCell = new PdfPCell(new Phrase(row[0], F_LABEL));
            labelCell.setBackgroundColor(rowBg);
            labelCell.setBorderColor(GREY_LINE);
            labelCell.setBorderWidthTop(0);
            labelCell.setBorderWidthLeft(0);
            labelCell.setBorderWidthRight(0);
            labelCell.setBorderWidthBottom(0.5f);
            labelCell.setPaddingTop(8f);
            labelCell.setPaddingBottom(8f);
            labelCell.setPaddingLeft(10f);
            labelCell.setPaddingRight(6f);

            PdfPCell valueCell = new PdfPCell(new Phrase(row[1], F_VALUE));
            valueCell.setBackgroundColor(rowBg);
            valueCell.setBorderColor(GREY_LINE);
            valueCell.setBorderWidthTop(0);
            valueCell.setBorderWidthLeft(0);
            valueCell.setBorderWidthRight(0);
            valueCell.setBorderWidthBottom(0.5f);
            valueCell.setPaddingTop(8f);
            valueCell.setPaddingBottom(8f);
            valueCell.setPaddingLeft(6f);
            valueCell.setPaddingRight(10f);

            table.addCell(labelCell);
            table.addCell(valueCell);
        }

        // Bordure extérieure simulée via une table enveloppante
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        PdfPCell wrapCell = new PdfPCell();
        wrapCell.setBorderColor(GREY_LINE);
        wrapCell.setBorderWidth(1f);
        wrapCell.setPadding(0f);
        wrapCell.addElement(table);
        wrapper.addCell(wrapCell);
        document.add(wrapper);
    }

    // ─── Bloc montant ─────────────────────────────────────────────────────────
    private void addAmountBlock(Document document, int prix) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(TEAL_LIGHT);
        cell.setBorderColor(TEAL);
        cell.setBorderWidth(1f);
        cell.setPaddingTop(14f);
        cell.setPaddingBottom(14f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph lbl = new Paragraph("MONTANT PAYÉ", F_AMOUNT_LBL);
        lbl.setAlignment(Element.ALIGN_CENTER);
        lbl.setSpacingAfter(4f);
        cell.addElement(lbl);

        Paragraph amount = new Paragraph(prix + " €", F_AMOUNT_VAL);
        amount.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(amount);

        table.addCell(cell);
        document.add(table);
    }

    // ─── Section QR Code ──────────────────────────────────────────────────────
    private void addQrSection(Document document, String url) throws Exception {
        // Titre de section
        Paragraph title = new Paragraph("ACCÈS RAPIDE", F_SECTION);
        title.setSpacingAfter(6);
        document.add(title);

        // Encadré contenant le QR code
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(55);
        outer.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(GREY_LINE);
        cell.setBorderWidth(1f);
        cell.setBackgroundColor(GREY_LIGHT);
        cell.setPaddingTop(14f);
        cell.setPaddingBottom(14f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph sub = new Paragraph(
                "Scannez ce QR Code pour accéder directement\nà votre consultation.", F_QR_SUB);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(10f);
        cell.addElement(sub);

        Image qrImage = QRCodeGenerator.generateQRCodeImage(url, 130, 130);
        qrImage.setAlignment(Image.ALIGN_CENTER);
        cell.addElement(qrImage);

        outer.addCell(cell);
        document.add(outer);
    }

    // ─── Séparateur fin ───────────────────────────────────────────────────────
    private void addSeparator(Document document, PdfWriter writer) throws DocumentException {
        PdfContentByte cb = writer.getDirectContent();
        // On utilise un tableau mono-cellule comme ligne fine
        PdfPTable sep = new PdfPTable(1);
        sep.setWidthPercentage(100);
        PdfPCell line = new PdfPCell();
        line.setBorderWidthTop(0.5f);
        line.setBorderColorTop(GREY_LINE);
        line.setBorderWidthBottom(0);
        line.setBorderWidthLeft(0);
        line.setBorderWidthRight(0);
        line.setFixedHeight(1f);
        sep.addCell(line);
        document.add(sep);
    }

    // ─── Pied de page ─────────────────────────────────────────────────────────
    private void addFooter(Document document, PdfWriter writer) throws DocumentException {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 2f, 1f});

        // Colonne gauche
        PdfPCell left = footerCell("Merci d'avoir choisi\nPsyConnect.", Element.ALIGN_LEFT);
        table.addCell(left);

        // Colonne centre
        PdfPCell center = footerCell("Ce reçu est généré automatiquement\net ne nécessite pas de signature.", Element.ALIGN_CENTER);
        table.addCell(center);

        // Colonne droite
        PdfPCell right = footerCell("www.psyconnect.com", Element.ALIGN_RIGHT);
        table.addCell(right);

        document.add(table);
    }

    private PdfPCell footerCell(String text, int align) {
        Paragraph p = new Paragraph(text, F_FOOTER);
        p.setAlignment(align);
        PdfPCell cell = new PdfPCell();
        cell.addElement(p);
        cell.setBorderWidthTop(0.5f);
        cell.setBorderColorTop(GREY_LINE);
        cell.setBorderWidthBottom(0);
        cell.setBorderWidthLeft(0);
        cell.setBorderWidthRight(0);
        cell.setPaddingTop(10f);
        cell.setHorizontalAlignment(align);
        return cell;
    }

    // ─── Utilitaire espacement ────────────────────────────────────────────────
    private void spacer(Document document, float height) throws DocumentException {
        Paragraph sp = new Paragraph(" ");
        sp.setSpacingAfter(height);
        document.add(sp);
    }
}