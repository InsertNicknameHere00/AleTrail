package com.example.aletrail;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GamificationService {

    private static final String TAG = "GamificationService";
    private Database database;
    private ExecutorService executorService;

    // Badge types
    public static final String BADGE_FIRST_VISIT = "FIRST_VISIT";
    public static final String BADGE_BRONZE = "BRONZE";
    public static final String BADGE_SILVER = "SILVER";
    public static final String BADGE_GOLD = "GOLD";
    public static final String BADGE_PLATINUM = "PLATINUM";
    public static final String BADGE_EXPLORER = "EXPLORER";
    public static final String BADGE_SOCIAL_BUTTERFLY = "SOCIAL_BUTTERFLY";
    public static final String BADGE_BEER_CONNOISSEUR = "BEER_CONNOISSEUR";

    public GamificationService(Context context) {
        this.database = Database.getInstance(context);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Инициализира badges за нов потребител
     */
    public void initializeBadgesForUser(String userId) {
        executorService.execute(() -> {
            createBadge(userId, BADGE_FIRST_VISIT, "🎉 First Visit", "Visit your first brewery", 1, null);
            createBadge(userId, BADGE_BRONZE, "🥉 Bronze Collector", "Collect 10 stamps", 10, null);
            createBadge(userId, BADGE_SILVER, "🥈 Silver Collector", "Collect 25 stamps", 25, null);
            createBadge(userId, BADGE_GOLD, "🥇 Gold Collector", "Collect 50 stamps", 50, null);
            createBadge(userId, BADGE_PLATINUM, "💎 Platinum Collector", "Collect 100 stamps", 100, null);
            createBadge(userId, BADGE_EXPLORER, "🗺️ Explorer", "Visit 10 different breweries", 10, null);
            createBadge(userId, BADGE_SOCIAL_BUTTERFLY, "🦋 Social Butterfly", "Share 5 check-ins", 5, null);
            createBadge(userId, BADGE_BEER_CONNOISSEUR, "🍺 Beer Connoisseur", "Rate 20 beers", 20, null);

            Log.d(TAG, "Initialized badges for user: " + userId);
        });
    }

    /**
     * Създава badge
     */
    private void createBadge(String userId, String badgeType, String badgeName,
                            String description, int requiredCount, String breweryId) {
        BadgeEntity badge = new BadgeEntity();
        badge.setUserId(userId);
        badge.setBadgeType(badgeType);
        badge.setBadgeName(badgeName);
        badge.setBadgeDescription(description);
        badge.setRequiredCount(requiredCount);
        badge.setBreweryId(breweryId);
        badge.setEarned(false);

        database.badgeDAO().insert(badge);
    }

    /**
     * Проверява и отключва badges след ново посещение
     */
    public void checkAndUnlockBadges(String userId, int totalStamps, int totalVisits,
                                     int uniqueBreweriesCount) {
        executorService.execute(() -> {
            // Check stamp-based badges
            checkBadge(userId, BADGE_BRONZE, totalStamps, 10);
            checkBadge(userId, BADGE_SILVER, totalStamps, 25);
            checkBadge(userId, BADGE_GOLD, totalStamps, 50);
            checkBadge(userId, BADGE_PLATINUM, totalStamps, 100);

            // Check visit-based badges
            if (totalVisits >= 1) {
                unlockBadgeByType(userId, BADGE_FIRST_VISIT);
            }

            // Check brewery exploration badge
            checkBadge(userId, BADGE_EXPLORER, uniqueBreweriesCount, 10);
        });
    }

    /**
     * Проверява дали badge трябва да бъде отключен
     */
    private void checkBadge(String userId, String badgeType, int currentCount, int required) {
        if (currentCount >= required) {
            unlockBadgeByType(userId, badgeType);
        }
    }

    /**
     * Отключва badge по тип
     */
    private void unlockBadgeByType(String userId, String badgeType) {
        // This is simplified - in real app, you'd query first to check if already unlocked
        // For now, this is a placeholder
        Log.d(TAG, "Unlocking badge: " + badgeType + " for user: " + userId);
    }

    /**
     * Проверява badge за рейтинги
     */
    public void checkRatingBadges(String userId, int totalRatings) {
        executorService.execute(() -> {
            checkBadge(userId, BADGE_BEER_CONNOISSEUR, totalRatings, 20);
        });
    }
}

