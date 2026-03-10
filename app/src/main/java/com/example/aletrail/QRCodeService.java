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
     * Generates a QR code Bitmap from any string value.
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

    // ── Brewery Stamp QR (displayed at brewery / generated in-app) ──

    /**
     * Generates a stamp QR code value for a brewery.
     * Format: ALETRAIL_STAMP:breweryId:secretToken
     * The brewery displays this QR; users scan it to auto-stamp their card.
     */
    public static String generateBreweryStampQR(String breweryId, String secretToken) {
        return "ALETRAIL_STAMP:" + breweryId + ":" + secretToken;
    }

    /**
     * Generates a stamp QR with an auto-generated token.
     */
    public static String generateBreweryStampQR(String breweryId) {
        String token = UUID.randomUUID().toString().substring(0, 8);
        return generateBreweryStampQR(breweryId, token);
    }

    /**
     * Validates a stamp QR code value.
     */
    public static boolean isValidStampQR(String qrValue) {
        return qrValue != null
                && qrValue.startsWith("ALETRAIL_STAMP:")
                && qrValue.split(":").length == 3;
    }

    /**
     * Extracts breweryId from a stamp QR code.
     */
    public static String extractBreweryIdFromStampQR(String qrValue) {
        if (isValidStampQR(qrValue)) {
            return qrValue.split(":")[1];
        }
        return null;
    }

    /**
     * Extracts the secret token from a stamp QR code.
     */
    public static String extractTokenFromStampQR(String qrValue) {
        if (isValidStampQR(qrValue)) {
            return qrValue.split(":")[2];
        }
        return null;
    }
}

