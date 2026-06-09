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

    LoyaltyCardRepository(Database database,
                          ExecutorService executorService,
                          GamificationService gamificationService,
                          AppwriteService appwriteService) {
        this.database = database;
        this.loyaltyCardDao = database.loyaltyCardDAO();
        this.visitDao = database.visitDAO();
        this.userDao = database.userDAO();
        this.executorService = executorService;
        this.gamificationService = gamificationService;
        this.appwriteService = appwriteService;
    }

    // Прави loyalty card, само ако user още няма за тази пивоварна.
    public void createLoyaltyCard(String userId, String breweryId, int maxStamps,
                                  OnCardCreatedListener listener) {
        executorService.execute(() -> {
            // Не допускаме дублирани карти за user + brewery.
            LoyaltyCardEntity existing = loyaltyCardDao.getCardForUserAndBrewerySync(userId, breweryId);
            if (existing != null) {
                Log.d(TAG, "Duplicate card prevented for brewery: " + breweryId);
                if (listener != null) {
                    listener.onCardCreated(-1);
                }
                return;
            }

            // Създаваме картата локално.
            LoyaltyCardEntity card = new LoyaltyCardEntity(userId, breweryId, maxStamps);
            long cardId = loyaltyCardDao.insert(card);

            Log.d(TAG, "Created loyalty card with ID: " + cardId);

            // Sync към cloud за логнати user-и.
            if (userId != null && !userId.startsWith("guest_")) {
                card.setCardId((int) cardId);
                syncLoyaltyCardToAppwrite(card);
            }

            if (listener != null) {
                listener.onCardCreated(cardId);
            }
        });
    }

    // Публичен вход за добавяне на stamp от други части на app-а.
    public void addStampToCard(int cardId, String userId, String breweryId,
                               double latitude, double longitude) {
        executorService.execute(() ->
            addStampToCardInternal(cardId, userId, breweryId, latitude, longitude)
        );
    }

    // Основният stamp flow (local save + sync + badge checks).
    private void addStampToCardInternal(int cardId, String userId, String breweryId,
                                        double latitude, double longitude) {
        // Ъпдейтваме локалния брой stamp-ове.
        loyaltyCardDao.addStamp(cardId);

        // Пазим visit запис.
        VisitEntity visit = new VisitEntity();
        visit.setUserId(userId);
        visit.setBreweryId(breweryId);
        visit.setCardId(cardId);
        visit.setStampAdded(true);
        visit.setLatitude(latitude);
        visit.setLongitude(longitude);
        visitDao.insert(visit);

        // Обновяваме user статистиката.
        userDao.incrementTotalStamps(userId);
        userDao.incrementTotalVisits(userId);

        // Държим статистиката за уникални brewery-та актуална.
        int uniqueBreweries = visitDao.getUniqueBreweriesVisitedSync(userId);
        userDao.updateTotalBreweriesVisited(userId, uniqueBreweries);

        Log.d(TAG, "Added stamp to card: " + cardId);

        // Качваме visit и card промените в cloud.
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

            // Sync-ваме и новата stamp стойност за тази карта.
            LoyaltyCardEntity updatedCard = loyaltyCardDao.getCardForUserAndBrewerySync(userId, breweryId);
            if (updatedCard != null) {
                syncLoyaltyCardToAppwrite(updatedCard);
            }
        }

        // Проверяваме badge-овете пак с най-новите стойности.
        UserEntity user = userDao.getUserByIdSync(userId);
        if (user != null) {
            gamificationService.checkAndUnlockBadges(userId,
                    user.getTotalStamps(),
                    user.getTotalVisits(),
                    uniqueBreweries);
        }
        // Проверяваме и visit/card badge-овете.
        gamificationService.checkVisitBadges(userId);
        gamificationService.checkCardBadges(userId);
    }

    // Ползваме стабилно doc ID, за да няма duplicate cloud doc-ове.
    private void syncLoyaltyCardToAppwrite(LoyaltyCardEntity card) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("userId", card.getUserId());
        data.put("breweryId", card.getBreweryId());
        data.put("stamps", card.getStamps());
        data.put("maxStamps", card.getMaxStamps());
        data.put("active", card.isActive());

        // Правим cloud-safe document id.
        String docId = card.getUserId() + "_" + card.getBreweryId();
        // Appwrite ID-то има лимит и иска safe символи.
        docId = docId.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (docId.length() > 36) {
            docId = docId.substring(0, 36);
        }

        final String finalDocId = docId;

        // Първо пробваме update, create само ако липсва.
        appwriteService.updateDocument(AppwriteConstants.COLLECTION_LOYALTY_CARDS, finalDocId, data,
            new AppwriteService.DocumentCallback() {
                @Override
                public void onSuccess(io.appwrite.models.Document<java.util.Map<String, Object>> document) {
                    Log.d(TAG, "Loyalty card updated in Appwrite (stamps=" + card.getStamps() + ")");
                }
                @Override
                public void onError(String message) {
                    // Липсващият doc е нормален случай при първи sync.
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

    // Чете всички карти на user.
    public LiveData<List<LoyaltyCardEntity>> getUserCards(String userId) {
        return loyaltyCardDao.getCardsForUser(userId);
    }

    // Чете една карта по id.
    public LiveData<LoyaltyCardEntity> getCardById(int cardId) {
        return loyaltyCardDao.getCardById(cardId);
    }

    // Чете карта за конкретна brewery.
    public LiveData<LoyaltyCardEntity> getCardForBrewery(String userId, String breweryId) {
        return loyaltyCardDao.getCardForUserAndBrewery(userId, breweryId);
    }

    // Нулира stamp-овете, когато reward цикълът започне отначало.
    public void resetCard(int cardId) {
        executorService.execute(() -> {
            loyaltyCardDao.resetStamps(cardId);
            Log.d(TAG, "Reset stamps for card: " + cardId);
        });
    }

    // Трие карта.
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

    // Callback за резултат от QR stamp flow.
    public interface StampResultCallback {
        void onSuccess(String breweryName);
        void onError(String message);
    }

    private static final long STAMP_COOLDOWN_MS = 24 * 60 * 60 * 1000; // 24h cooldown.

    // Обработва сканиран stamp QR и пуска всички проверки преди да добави stamp.
    public void processStampFromQR(String userId, String breweryId,
                                   double latitude, double longitude,
                                   StampResultCallback callback) {
        executorService.execute(() -> {
            try {
                // Проверяваме дали има карта за тази brewery.
                LoyaltyCardEntity card = loyaltyCardDao.getCardForUserAndBrewerySync(userId, breweryId);
                if (card == null) {
                    if (callback != null) callback.onError("Нямаш loyalty карта за тази brewery. Създай си първо!");
                    return;
                }

                // Спираме, ако картата е пълна.
                if (card.getStamps() >= card.getMaxStamps()) {
                    if (callback != null) callback.onError("Тази loyalty карта вече е пълна! 🎉");
                    return;
                }

                // Прилагаме cooldown между stamp-овете.
                Long lastVisit = visitDao.getLastVisitTimestampSync(userId, breweryId);
                if (lastVisit != null) {
                    long elapsed = System.currentTimeMillis() - lastVisit;
                    if (elapsed < STAMP_COOLDOWN_MS) {
                        long hoursLeft = (STAMP_COOLDOWN_MS - elapsed) / (60 * 60 * 1000);
                        long minutesLeft = ((STAMP_COOLDOWN_MS - elapsed) % (60 * 60 * 1000)) / (60 * 1000);
                        String timeMsg = hoursLeft > 0
                                ? hoursLeft + "ч " + minutesLeft + "м"
                                : minutesLeft + " минути";
                        if (callback != null) callback.onError("Активен cooldown! Опитай отново след " + timeMsg);
                        return;
                    }
                }

                // Всички проверки минаха.
                addStampToCardInternal(card.getCardId(), userId, breweryId, latitude, longitude);

                // Пробваме да върнем  име на brewery.
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
                if (callback != null) callback.onError("Нещо се обърка: " + e.getMessage());
            }
        });
    }
}
