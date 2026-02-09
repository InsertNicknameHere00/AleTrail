package com.example.aletrail;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncService {

    private static final String TAG = "SyncService";
    private Context context;
    private Database database;
    private ExecutorService executorService;

    public SyncService(Context context) {
        this.context = context;
        this.database = Database.getInstance(context);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Проверява за интернет свързаност
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Синхронизира всички неsinc-нати данни
     */
    public void syncAllData(String userId) {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No network available, skipping sync");
            return;
        }

        executorService.execute(() -> {
            try {
                // Sync visits
                syncVisits();

                // Sync ratings
                syncRatings();

                // Update last sync timestamp
                long currentTime = System.currentTimeMillis();
                database.userDAO().updateLastSyncTimestamp(userId, currentTime);

                Log.d(TAG, "Sync completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error during sync: " + e.getMessage());
            }
        });
    }

    /**
     * Синхронизира посещения
     */
    private void syncVisits() {
        List<VisitEntity> unsyncedVisits = database.visitDAO().getUnsyncedVisits();
        for (VisitEntity visit : unsyncedVisits) {
            // TODO: Send to cloud/API
            // For now, just mark as synced
            database.visitDAO().markAsSynced(visit.getVisitId());
        }
        Log.d(TAG, "Synced " + unsyncedVisits.size() + " visits");
    }

    /**
     * Синхронизира рейтинги
     */
    private void syncRatings() {
        List<BeerRatingEntity> unsyncedRatings = database.beerRatingDAO().getUnsyncedRatings();
        for (BeerRatingEntity rating : unsyncedRatings) {
            // TODO: Send to cloud/API
            // For now, just mark as synced
            database.beerRatingDAO().markAsSynced(rating.getRatingId());
        }
        Log.d(TAG, "Synced " + unsyncedRatings.size() + " ratings");
    }

    /**
     * Планира автоматична синхронизация
     */
    public void scheduleAutoSync(String userId) {
        executorService.execute(() -> {
            while (true) {
                try {
                    Thread.sleep(300000); // 5 minutes
                    if (isNetworkAvailable()) {
                        syncAllData(userId);
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "Auto sync interrupted: " + e.getMessage());
                    break;
                }
            }
        });
    }
}

