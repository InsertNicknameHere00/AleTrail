package com.example.aletrail;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.UUID;

public class QRCodeService {

    // Прави QR bitmap от подаден текст.
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

    // Методите отдолу са за brewery stamp QR стойности.

    // Генерира stamp стойността, която слагаме в QR.
    public static String generateBreweryStampQR(String breweryId, String secretToken) {
        return "ALETRAIL_STAMP:" + breweryId + ":" + secretToken;
    }

    // Помощен метод, ако искаме auto token.
    public static String generateBreweryStampQR(String breweryId) {
        String token = UUID.randomUUID().toString().substring(0, 8);
        return generateBreweryStampQR(breweryId, token);
    }

    // Проверява дали сканираният текст е валиден stamp QR.
    public static boolean isValidStampQR(String qrValue) {
        return qrValue != null
                && qrValue.startsWith("ALETRAIL_STAMP:")
                && qrValue.split(":").length == 3;
    }

    // Вади brewery id от валиден stamp QR.
    public static String extractBreweryIdFromStampQR(String qrValue) {
        if (isValidStampQR(qrValue)) {
            return qrValue.split(":")[1];
        }
        return null;
    }

    // Вади token от валиден stamp QR.
    public static String extractTokenFromStampQR(String qrValue) {
        if (isValidStampQR(qrValue)) {
            return qrValue.split(":")[2];
        }
        return null;
    }
}
