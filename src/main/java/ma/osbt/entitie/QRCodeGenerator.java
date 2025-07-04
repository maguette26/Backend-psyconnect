package ma.osbt.entitie;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.Image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class QRCodeGenerator {

    public static Image generateQRCodeImage(String text, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(bufferedImage, "PNG", baos);
        baos.flush();

        Image itextImage = Image.getInstance(baos.toByteArray());
        itextImage.scaleAbsolute(width, height);

        baos.close();

        return itextImage;
    }
}

