package com.example.aletrail;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.UUID;

public class QRCodeService {

    /**
     * Генерира уникален QR код за loyalty card
     */
    public static String generateUniqueQRValue(String userId, String breweryId) {
        String uniqueId = UUID.randomUUID().toString();
        return "ALETRAIL:" + userId + ":" + breweryId + ":" + uniqueId;
    }

    /**
     * Генерира Bitmap изображение от QR код стойност
     */
    public static Bitmap generateQRCodeBitmap(String qrValue, int width, int height) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(qrValue, BarcodeFormat.QR_CODE, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Валидира QR код стойност
     */
    public static boolean isValidQRCode(String qrValue) {
        return qrValue != null && qrValue.startsWith("ALETRAIL:") && qrValue.split(":").length == 4;
    }

    /**
     * Извлича userId от QR код
     */
    public static String extractUserIdFromQR(String qrValue) {
        if (isValidQRCode(qrValue)) {
            return qrValue.split(":")[1];
        }
        return null;
    }

    /**
     * Извлича breweryId от QR код
     */
    public static String extractBreweryIdFromQR(String qrValue) {
        if (isValidQRCode(qrValue)) {
            return qrValue.split(":")[2];
        }
        return null;
    }
}

