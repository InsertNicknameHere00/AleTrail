package com.example.aletrail;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationService {

    private FusedLocationProviderClient fusedLocationClient;
    private Context context;

    public LocationService(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    /**
     * Получава текуща локация с нов API
     */
    public void getCurrentLocation(OnSuccessListener<Location> onSuccessListener) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Използваме новия API метод
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.getToken()
        ).addOnSuccessListener(onSuccessListener)
         .addOnFailureListener(e -> {
             // Fallback to last known location
             fusedLocationClient.getLastLocation().addOnSuccessListener(onSuccessListener);
         });
    }

    /**
     * Изчислява разстояние между две точки (в километри)
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Проверява дали потребителят е в близост до пивоварна (в рамките на 100м)
     */
    public static boolean isNearBrewery(Location userLocation, double breweryLat, double breweryLon) {
        if (userLocation == null) return false;

        double distance = calculateDistance(
            userLocation.getLatitude(),
            userLocation.getLongitude(),
            breweryLat,
            breweryLon
        );

        return distance <= 0.1; // 100 meters
    }

    /**
     * Форматира разстояние за показване
     */
    public static String formatDistance(double distanceInKm) {
        if (distanceInKm < 1) {
            return String.format("%.0f m", distanceInKm * 1000);
        } else {
            return String.format("%.1f km", distanceInKm);
        }
    }
}
