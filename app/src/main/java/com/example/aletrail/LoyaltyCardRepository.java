package com.example.aletrail;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoyaltyCardRepository {

    private static final String TAG = "LoyaltyCardRepository";
    private Database database;
    private LoyaltyCardDAO loyaltyCardDao;
    private VisitDAO visitDao;
    private UserDAO userDao;
    private ExecutorService executorService;
    private GamificationService gamificationService;
    private AppwriteService appwriteService;

    public LoyaltyCardRepository(Context context) {
        this.database = Database.getInstance(context);
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
            // Check for duplicate card
            LoyaltyCardEntity existing = loyaltyCardDao.getCardForUserAndBrewerySync(userId, breweryId);
            if (existing != null) {
                Log.d(TAG, "Duplicate card prevented for brewery: " + breweryId);
                if (listener != null) {
                    listener.onCardCreated(-1); // Signal duplicate
                }
                return;
            }

            // Create card (no per-card QR — stamps use brewery stamp QR)
            LoyaltyCardEntity card = new LoyaltyCardEntity(userId, breweryId, maxStamps);
            long cardId = loyaltyCardDao.insert(card);

            Log.d(TAG, "Created loyalty card with ID: " + cardId);

            // Sync to Appwrite Database using deterministic doc ID
            if (userId != null && !userId.startsWith("guest_")) {
                card.setCardId((int) cardId);
                syncLoyaltyCardToAppwrite(card);
            }

            if (listener != null) {
                listener.onCardCreated(cardId);
            }
        });
    }

    /**
     * Добавя печат към карта (queues on executor — use for external callers).
     */
    public void addStampToCard(int cardId, String userId, String breweryId,
                               double latitude, double longitude) {
        executorService.execute(() ->
            addStampToCardInternal(cardId, userId, breweryId, latitude, longitude)
        );
    }

    /**
     * Internal stamp logic — must be called from a background thread.
     * Adds stamp in Room, creates visit, syncs to Appwrite, updates stats & badges.
     */
    private void addStampToCardInternal(int cardId, String userId, String breweryId,
                                        double latitude, double longitude) {
        // Add stamp in Room
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

        // Update user stats
        userDao.incrementTotalStamps(userId);
        userDao.incrementTotalVisits(userId);

        // Update unique breweries count
        int uniqueBreweries = visitDao.getUniqueBreweriesVisitedSync(userId);
        userDao.updateTotalBreweriesVisited(userId, uniqueBreweries);

        Log.d(TAG, "Added stamp to card: " + cardId);

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

            // Sync the updated loyalty card stamp count to Appwrite
            LoyaltyCardEntity updatedCard = loyaltyCardDao.getCardForUserAndBrewerySync(userId, breweryId);
            if (updatedCard != null) {
                syncLoyaltyCardToAppwrite(updatedCard);
            }
        }

        // Check for badge unlocks with real stats from DB
        UserEntity user = userDao.getUserByIdSync(userId);
        if (user != null) {
            gamificationService.checkAndUnlockBadges(userId,
                    user.getTotalStamps(),
                    user.getTotalVisits(),
                    uniqueBreweries);
        }
        // Check visit-based and card-based badges
        gamificationService.checkVisitBadges(userId);
        gamificationService.checkCardBadges(userId);
    }

    /**
     * Syncs loyalty card to Appwrite using a deterministic document ID.
     * This ensures stamps are properly updated rather than creating duplicates.
     */
    private void syncLoyaltyCardToAppwrite(LoyaltyCardEntity card) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("userId", card.getUserId());
        data.put("breweryId", card.getBreweryId());
        data.put("stamps", card.getStamps());
        data.put("maxStamps", card.getMaxStamps());
        data.put("active", card.isActive());

        // Use deterministic doc ID so creates and updates always target the same document
        String docId = card.getUserId() + "_" + card.getBreweryId();
        // Appwrite doc IDs max 36 chars, only [a-zA-Z0-9._-]
        docId = docId.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (docId.length() > 36) {
            docId = docId.substring(0, 36);
        }

        final String finalDocId = docId;

        // Try update first, create if it doesn't exist
        appwriteService.updateDocument(AppwriteConstants.COLLECTION_LOYALTY_CARDS, finalDocId, data,
            new AppwriteService.DocumentCallback() {
                @Override
                public void onSuccess(io.appwrite.models.Document<java.util.Map<String, Object>> document) {
                    Log.d(TAG, "Loyalty card updated in Appwrite (stamps=" + card.getStamps() + ")");
                }
                @Override
                public void onError(String message) {
                    // Document doesn't exist yet — create it
                    Log.d(TAG, "Update failed, creating new doc: " + message);
                    appwriteService.createDocument(AppwriteConstants.COLLECTION_LOYALTY_CARDS, finalDocId, data,
                        new AppwriteService.DocumentCallback() {
                            @Override
                            public void onSuccess(io.appwrite.models.Document<java.util.Map<String, Object>> document) {
                                Log.d(TAG, "Loyalty card created in Appwrite (stamps=" + card.getStamps() + ")");
                            }
                            @Override
                            public void onError(String msg) {
                                Log.e(TAG, "Failed to create loyalty card in Appwrite: " + msg);
                            }
                        });
                }
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

    /**
     * Callback for stamp-from-QR result.
     */
    public interface StampResultCallback {
        void onSuccess(String breweryName);
        void onError(String message);
    }

    private static final long STAMP_COOLDOWN_MS = 24 * 60 * 60 * 1000; // 24 hours

    /**
     * Processes a stamp QR scan: finds the user's card for the brewery,
     * enforces a 24-hour cooldown, and adds a stamp if everything checks out.
     */
    public void processStampFromQR(String userId, String breweryId,
                                   double latitude, double longitude,
                                   StampResultCallback callback) {
        executorService.execute(() -> {
            try {
                // 1. Check if user has a loyalty card for this brewery
                LoyaltyCardEntity card = loyaltyCardDao.getCardForUserAndBrewerySync(userId, breweryId);
                if (card == null) {
                    if (callback != null) callback.onError("You don't have a loyalty card for this brewery. Create one first!");
                    return;
                }

                // 2. Check if card is already full
                if (card.getStamps() >= card.getMaxStamps()) {
                    if (callback != null) callback.onError("This loyalty card is already full! 🎉");
                    return;
                }

                // 3. Check 24-hour cooldown
                Long lastVisit = visitDao.getLastVisitTimestampSync(userId, breweryId);
                if (lastVisit != null) {
                    long elapsed = System.currentTimeMillis() - lastVisit;
                    if (elapsed < STAMP_COOLDOWN_MS) {
                        long hoursLeft = (STAMP_COOLDOWN_MS - elapsed) / (60 * 60 * 1000);
                        long minutesLeft = ((STAMP_COOLDOWN_MS - elapsed) % (60 * 60 * 1000)) / (60 * 1000);
                        String timeMsg = hoursLeft > 0
                                ? hoursLeft + "h " + minutesLeft + "m"
                                : minutesLeft + " minutes";
                        if (callback != null) callback.onError("Cooldown active! Try again in " + timeMsg);
                        return;
                    }
                }

                // 4. All checks passed — add the stamp directly (we're already on background thread)
                addStampToCardInternal(card.getCardId(), userId, breweryId, latitude, longitude);

                // Get brewery name for the success message
                String breweryName = breweryId;
                try {
                    BreweryEntity brewery = database.AleDAO().getAleByIdSync(breweryId);
                    if (brewery != null && brewery.getName() != null) {
                        breweryName = brewery.getName();
                    }
                } catch (Exception ignored) {}

                if (callback != null) callback.onSuccess(breweryName);

            } catch (Exception e) {
                Log.e(TAG, "Error processing stamp QR: " + e.getMessage(), e);
                if (callback != null) callback.onError("Something went wrong: " + e.getMessage());
            }
        });
    }
}
