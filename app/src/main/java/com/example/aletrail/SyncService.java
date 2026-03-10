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
                syncUserProfile(userId);
                syncVisits();
                syncRatings();
                syncFavorites(userId);
                syncLoyaltyCards(userId);
                syncBadges(userId);

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
     * Syncs the user profile to Appwrite Database
     */
    private void syncUserProfile(String userId) {
        try {
            // getUserByIdSync doesn't exist yet, so we use a blocking approach
            UserEntity user = database.userDAO().getUserByIdSync(userId);
            if (user != null) {
                appwriteService.syncUserProfile(user, new AppwriteService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "User profile synced to Appwrite");
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Failed to sync user profile: " + message);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing user profile: " + e.getMessage());
        }
    }

    /**
     * Syncs favorite breweries to Appwrite
     */
    private void syncFavorites(String userId) {
        try {
            List<BreweryEntity> favorites = database.AleDAO().getFavoritesSync();
            for (BreweryEntity brewery : favorites) {
                appwriteService.syncFavorite(userId, brewery, new AppwriteService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Favorite " + brewery.getName() + " synced to Appwrite");
                    }

                    @Override
                    public void onError(String message) {
                        // May already exist — that's OK
                        Log.d(TAG, "Favorite sync note for " + brewery.getName() + ": " + message);
                    }
                });
            }
            Log.d(TAG, "Queued " + favorites.size() + " favorites for sync");
        } catch (Exception e) {
            Log.e(TAG, "Error syncing favorites: " + e.getMessage());
        }
    }

    /**
     * Syncs loyalty cards to Appwrite
     */
    private void syncLoyaltyCards(String userId) {
        try {
            List<LoyaltyCardEntity> cards = database.loyaltyCardDAO().getCardsForUserSync(userId);
            for (LoyaltyCardEntity card : cards) {
                appwriteService.syncLoyaltyCard(card, new AppwriteService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Loyalty card " + card.getCardId() + " synced to Appwrite");
                    }

                    @Override
                    public void onError(String message) {
                        Log.d(TAG, "Loyalty card sync note for " + card.getCardId() + ": " + message);
                    }
                });
            }
            Log.d(TAG, "Queued " + cards.size() + " loyalty cards for sync");
        } catch (Exception e) {
            Log.e(TAG, "Error syncing loyalty cards: " + e.getMessage());
        }
    }

    /**
     * Syncs badges to Appwrite
     */
    private void syncBadges(String userId) {
        try {
            // Sync all badges (earned and unearned) so Appwrite has the full set
            List<BadgeEntity> badges = database.badgeDAO().getAllBadgesForUserSync(userId);
            for (BadgeEntity badge : badges) {
                appwriteService.syncBadge(badge, new AppwriteService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Badge " + badge.getBadgeName() + " synced to Appwrite");
                    }

                    @Override
                    public void onError(String message) {
                        Log.d(TAG, "Badge sync note for " + badge.getBadgeName() + ": " + message);
                    }
                });
            }
            Log.d(TAG, "Queued " + badges.size() + " badges for sync");
        } catch (Exception e) {
            Log.e(TAG, "Error syncing badges: " + e.getMessage());
        }
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
