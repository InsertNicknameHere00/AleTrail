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
    private AppwriteService appwriteService;
    private ExecutorService executorService;

    public SyncService(Context context) {
        this.context = context;
        this.database = Database.getInstance(context);
        this.appwriteService = AppwriteService.getInstance(context);
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

        // Don't sync for guest users
        if (userId == null || userId.startsWith("guest_")) {
            Log.d(TAG, "Guest user, skipping cloud sync");
            return;
        }

        executorService.execute(() -> {
            try {
                syncVisits();
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
            appwriteService.syncVisit(visit, new AppwriteService.SimpleCallback() {
                @Override
                public void onSuccess() {
                    database.visitDAO().markAsSynced(visit.getVisitId());
                    Log.d(TAG, "Visit " + visit.getVisitId() + " synced to Appwrite");
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "Failed to sync visit " + visit.getVisitId() + ": " + message);
                }
            });
        }
        Log.d(TAG, "Queued " + unsyncedVisits.size() + " visits for sync");
    }

    /**
     * Синхронизира рейтинги
     */
    private void syncRatings() {
        List<BeerRatingEntity> unsyncedRatings = database.beerRatingDAO().getUnsyncedRatings();
        for (BeerRatingEntity rating : unsyncedRatings) {
            appwriteService.syncRating(rating, new AppwriteService.SimpleCallback() {
                @Override
                public void onSuccess() {
                    database.beerRatingDAO().markAsSynced(rating.getRatingId());
                    Log.d(TAG, "Rating " + rating.getRatingId() + " synced to Appwrite");
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "Failed to sync rating " + rating.getRatingId() + ": " + message);
                }
            });
        }
        Log.d(TAG, "Queued " + unsyncedRatings.size() + " ratings for sync");
    }

    // TODO: Add syncLoyaltyCards() when LoyaltyCardDAO.getUnsyncedCards() is implemented
    // TODO: Add syncBadges() when BadgeDAO.getUnsyncedBadges() is implemented
    // TODO: Add syncFavorites() — favorites are currently tracked via BreweryEntity.isFavorite

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
