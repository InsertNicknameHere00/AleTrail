package com.example.aletrail;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoyaltyCardRepository {

    private static final String TAG = "LoyaltyCardRepository";
    private LoyaltyCardDAO loyaltyCardDao;
    private VisitDAO visitDao;
    private UserDAO userDao;
    private ExecutorService executorService;
    private GamificationService gamificationService;
    private AppwriteService appwriteService;

    public LoyaltyCardRepository(Context context) {
        Database database = Database.getInstance(context);
        this.loyaltyCardDao = database.loyaltyCardDAO();
        this.visitDao = database.visitDAO();
        this.userDao = database.userDAO();
        this.executorService = Executors.newFixedThreadPool(2);
        this.gamificationService = new GamificationService(context);
        this.appwriteService = AppwriteService.getInstance(context);
    }

    /**
     * Създава нова loyalty card
     */
    public void createLoyaltyCard(String userId, String breweryId, int maxStamps,
                                  OnCardCreatedListener listener) {
        executorService.execute(() -> {
            // Generate unique QR code
            String qrValue = QRCodeService.generateUniqueQRValue(userId, breweryId);

            // Create card
            LoyaltyCardEntity card = new LoyaltyCardEntity(userId, breweryId, maxStamps, qrValue);
            long cardId = loyaltyCardDao.insert(card);

            Log.d(TAG, "Created loyalty card with ID: " + cardId);

            // Sync to Appwrite Database
            if (userId != null && !userId.startsWith("guest_")) {
                appwriteService.syncLoyaltyCard(card, new AppwriteService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Loyalty card synced to Appwrite for brewery: " + breweryId);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Failed to sync loyalty card to Appwrite: " + message);
                    }
                });
            }

            if (listener != null) {
                listener.onCardCreated(cardId);
            }
        });
    }

    /**
     * Добавя печат към карта
     */
    public void addStampToCard(int cardId, String userId, String breweryId,
                               double latitude, double longitude) {
        executorService.execute(() -> {
            // Add stamp
            loyaltyCardDao.addStamp(cardId);

            // Create visit record
            VisitEntity visit = new VisitEntity();
            visit.setUserId(userId);
            visit.setBreweryId(breweryId);
            visit.setCardId(cardId);
            visit.setStampAdded(true);
            visit.setLatitude(latitude);
            visit.setLongitude(longitude);
            visitDao.insert(visit);

            // Sync visit to Appwrite Database
            if (userId != null && !userId.startsWith("guest_")) {
                appwriteService.syncVisit(visit, new AppwriteService.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Visit synced to Appwrite for brewery: " + breweryId);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Failed to sync visit to Appwrite: " + message);
                    }
                });
            }

            // Update user stats
            userDao.incrementTotalStamps(userId);
            userDao.incrementTotalVisits(userId);

            Log.d(TAG, "Added stamp to card: " + cardId);

            // Check for badge unlocks (you'd get actual stats from DB)
            // This is simplified
            gamificationService.checkAndUnlockBadges(userId, 0, 0, 0);
        });
    }

    /**
     * Получава всички карти за потребител
     */
    public LiveData<List<LoyaltyCardEntity>> getUserCards(String userId) {
        return loyaltyCardDao.getCardsForUser(userId);
    }

    /**
     * Получава карта по ID
     */
    public LiveData<LoyaltyCardEntity> getCardById(int cardId) {
        return loyaltyCardDao.getCardById(cardId);
    }

    /**
     * Проверява дали потребител има карта за пивоварна
     */
    public LiveData<LoyaltyCardEntity> getCardForBrewery(String userId, String breweryId) {
        return loyaltyCardDao.getCardForUserAndBrewery(userId, breweryId);
    }

    /**
     * Ресетва печати (когато картата е пълна)
     */
    public void resetCard(int cardId) {
        executorService.execute(() -> {
            loyaltyCardDao.resetStamps(cardId);
            Log.d(TAG, "Reset stamps for card: " + cardId);
        });
    }

    /**
     * Изтрива loyalty card
     */
    public void deleteCard(int cardId, Runnable onComplete) {
        executorService.execute(() -> {
            loyaltyCardDao.deleteCard(cardId);
            Log.d(TAG, "Deleted card: " + cardId);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public interface OnCardCreatedListener {
        void onCardCreated(long cardId);
    }
}
