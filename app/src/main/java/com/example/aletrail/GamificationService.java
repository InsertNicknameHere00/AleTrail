package com.example.aletrail;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GamificationService {

    private static final String TAG = "GamificationService";
    private Database database;
    private AppwriteService appwriteService;
    private ExecutorService executorService;

    // Badge type keys.
    public static final String BADGE_FIRST_VISIT = "FIRST_VISIT";
    public static final String BADGE_BRONZE = "BRONZE";
    public static final String BADGE_SILVER = "SILVER";
    public static final String BADGE_GOLD = "GOLD";
    public static final String BADGE_PLATINUM = "PLATINUM";
    public static final String BADGE_DIAMOND = "DIAMOND";
    public static final String BADGE_EXPLORER = "EXPLORER";
    public static final String BADGE_EXPLORER_PRO = "EXPLORER_PRO";
    public static final String BADGE_SOCIAL_BUTTERFLY = "SOCIAL_BUTTERFLY";
    public static final String BADGE_BEER_CONNOISSEUR = "BEER_CONNOISSEUR";
    public static final String BADGE_REGULAR = "REGULAR";
    public static final String BADGE_VETERAN = "VETERAN";
    public static final String BADGE_LEGEND = "LEGEND";
    public static final String BADGE_CRITIC = "CRITIC";
    public static final String BADGE_MASTER_CRITIC = "MASTER_CRITIC";
    public static final String BADGE_WEEKEND_WARRIOR = "WEEKEND_WARRIOR";
    public static final String BADGE_NIGHT_OWL = "NIGHT_OWL";
    public static final String BADGE_CENTURION = "CENTURION";
    public static final String BADGE_WORLD_TRAVELER = "WORLD_TRAVELER";
    public static final String BADGE_LOYALTY_MASTER = "LOYALTY_MASTER";
    public static final String BADGE_HALF_CENTURY = "HALF_CENTURY";
    public static final String BADGE_HOPHEAD = "HOPHEAD";
    public static final String BADGE_TRAILBLAZER = "TRAILBLAZER";
    public static final String BADGE_BREW_MASTER = "BREW_MASTER";
    public static final String BADGE_TASTE_TESTER = "TASTE_TESTER";
    public static final String BADGE_FIVE_STAR = "FIVE_STAR";
    public static final String BADGE_GLOBETROTTER = "GLOBETROTTER";
    public static final String BADGE_DEDICATION = "DEDICATION";
    // Extra badge batch.
    public static final String BADGE_PARTY_STARTER = "PARTY_STARTER";
    public static final String BADGE_HOMEBODY = "HOMEBODY";
    public static final String BADGE_DATA_NERD = "DATA_NERD";
    public static final String BADGE_COMEBACK_KID = "COMEBACK_KID";
    public static final String BADGE_SWEET_TOOTH = "SWEET_TOOTH";
    public static final String BADGE_CITY_EXPLORER = "CITY_EXPLORER";
    public static final String BADGE_TECH_SAVVY = "TECH_SAVVY";
    public static final String BADGE_SUPPORTER = "SUPPORTER";
    public static final String BADGE_QUARTER_CENTURY = "QUARTER_CENTURY";
    public static final String BADGE_STAMP_STREAK = "STAMP_STREAK";
    public static final String BADGE_BREWPUB_FAN = "BREWPUB_FAN";
    public static final String BADGE_DOUBLE_DIGITS = "DOUBLE_DIGITS";

    public GamificationService(Context context) {
        this.database = Database.getInstance(context);
        this.appwriteService = AppwriteService.getInstance(context);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    // Inserts default badges for a new user.
    public void initializeBadgesForUser(String userId) {
        executorService.execute(() -> {
            createBadgeIfMissing(userId, BADGE_FIRST_VISIT, "🎉 First Visit", "Visit your first brewery", "🎉", 1);
            createBadgeIfMissing(userId, BADGE_BRONZE, "🥉 Bronze Collector", "Collect 10 stamps", "🥉", 10);
            createBadgeIfMissing(userId, BADGE_REGULAR, "🍻 Regular", "Collect 15 stamps", "🍻", 15);
            createBadgeIfMissing(userId, BADGE_SILVER, "🥈 Silver Collector", "Collect 25 stamps", "🥈", 25);
            createBadgeIfMissing(userId, BADGE_VETERAN, "🎖️ Veteran", "Collect 40 stamps", "🎖️", 40);
            createBadgeIfMissing(userId, BADGE_GOLD, "🥇 Gold Collector", "Collect 50 stamps", "🥇", 50);
            createBadgeIfMissing(userId, BADGE_DIAMOND, "💎 Diamond Collector", "Collect 75 stamps", "💎", 75);
            createBadgeIfMissing(userId, BADGE_LEGEND, "🏅 Legend", "Collect 90 stamps", "🏅", 90);
            createBadgeIfMissing(userId, BADGE_PLATINUM, "👑 Platinum Collector", "Collect 100 stamps", "👑", 100);
            createBadgeIfMissing(userId, BADGE_EXPLORER, "🗺️ Explorer", "Visit 10 different breweries", "🗺️", 10);
            createBadgeIfMissing(userId, BADGE_EXPLORER_PRO, "🌍 Explorer Pro", "Visit 25 different breweries", "🌍", 25);
            createBadgeIfMissing(userId, BADGE_SOCIAL_BUTTERFLY, "🦋 Social Butterfly", "Share 5 check-ins", "🦋", 5);
            createBadgeIfMissing(userId, BADGE_BEER_CONNOISSEUR, "🍺 Beer Connoisseur", "Rate 20 beers", "🍺", 20);
            createBadgeIfMissing(userId, BADGE_CRITIC, "📝 Critic", "Rate 5 beers", "📝", 5);
            createBadgeIfMissing(userId, BADGE_MASTER_CRITIC, "🎓 Master Critic", "Rate 50 beers", "🎓", 50);
            // Extra badges.
            createBadgeIfMissing(userId, BADGE_WEEKEND_WARRIOR, "🎊 Weekend Warrior", "Visit 5 breweries on weekends", "🎊", 5);
            createBadgeIfMissing(userId, BADGE_NIGHT_OWL, "🦉 Night Owl", "Collect 20 stamps after 6 PM", "🦉", 20);
            createBadgeIfMissing(userId, BADGE_CENTURION, "💯 Centurion", "Reach 100 total visits", "💯", 100);
            createBadgeIfMissing(userId, BADGE_WORLD_TRAVELER, "✈️ World Traveler", "Visit breweries in 5 states/countries", "✈️", 5);
            createBadgeIfMissing(userId, BADGE_LOYALTY_MASTER, "🏆 Loyalty Master", "Complete 3 loyalty cards", "🏆", 3);
            createBadgeIfMissing(userId, BADGE_HALF_CENTURY, "🎯 Half Century", "Reach 50 total visits", "🎯", 50);
            createBadgeIfMissing(userId, BADGE_HOPHEAD, "🌿 Hophead", "Visit 5 micro breweries", "🌿", 5);
            createBadgeIfMissing(userId, BADGE_TRAILBLAZER, "🔥 Trailblazer", "Be among the first to rate a brewery", "🔥", 1);
            createBadgeIfMissing(userId, BADGE_BREW_MASTER, "⚗️ Brew Master", "Collect 60 stamps", "⚗️", 60);
            createBadgeIfMissing(userId, BADGE_TASTE_TESTER, "👅 Taste Tester", "Rate 10 beers", "👅", 10);
            createBadgeIfMissing(userId, BADGE_FIVE_STAR, "⭐ Five Star", "Give 5 five-star ratings", "⭐", 5);
            createBadgeIfMissing(userId, BADGE_GLOBETROTTER, "🧭 Globetrotter", "Visit breweries in 10 states/countries", "🧭", 10);
            createBadgeIfMissing(userId, BADGE_DEDICATION, "❤️ Dedication", "Visit the same brewery 10 times", "❤️", 10);

            // Extra badge batch.
            createBadgeIfMissing(userId, BADGE_PARTY_STARTER, "🎈 Party Starter", "Share your first loyalty card", "🎈", 1);
            createBadgeIfMissing(userId, BADGE_HOMEBODY, "🏠 Homebody", "Visit the same brewery 5 times", "🏠", 5);
            createBadgeIfMissing(userId, BADGE_DATA_NERD, "📊 Data Nerd", "Rate 30 beers", "📊", 30);
            createBadgeIfMissing(userId, BADGE_COMEBACK_KID, "🔄 Comeback Kid", "Collect 30 stamps", "🔄", 30);
            createBadgeIfMissing(userId, BADGE_SWEET_TOOTH, "🍯 Sweet Tooth", "Give 3 five-star ratings", "🍯", 3);
            createBadgeIfMissing(userId, BADGE_CITY_EXPLORER, "🏙️ City Explorer", "Visit 5 breweries in one city", "🏙️", 5);
            createBadgeIfMissing(userId, BADGE_TECH_SAVVY, "📱 Tech Savvy", "Scan 10 QR codes", "📱", 10);
            createBadgeIfMissing(userId, BADGE_SUPPORTER, "🤝 Supporter", "Create 5 loyalty cards", "🤝", 5);
            createBadgeIfMissing(userId, BADGE_QUARTER_CENTURY, "🎯 Quarter Century", "Reach 25 total visits", "🎯", 25);
            createBadgeIfMissing(userId, BADGE_STAMP_STREAK, "🔥 Stamp Streak", "Collect 20 stamps", "🔥", 20);
            createBadgeIfMissing(userId, BADGE_BREWPUB_FAN, "🍽️ Brewpub Fan", "Visit 3 brewpubs", "🍽️", 3);
            createBadgeIfMissing(userId, BADGE_DOUBLE_DIGITS, "🔟 Double Digits", "Reach 10 total visits", "🔟", 10);

            Log.d(TAG, "Initialized badges for user: " + userId);
        });
    }

    // Безопасен insert helper, за да не правим duplicate badge редове.
    private void createBadgeIfMissing(String userId, String badgeType, String badgeName,
                                       String description, String icon, int requiredCount) {
        BadgeEntity existing = database.badgeDAO().getBadgeByTypeSync(userId, badgeType);
        if (existing == null) {
            createBadge(userId, badgeType, badgeName, description, icon, requiredCount, null);
        }
    }

    // Създава badge ред в Room.
    private void createBadge(String userId, String badgeType, String badgeName,
                            String description, String icon, int requiredCount, String breweryId) {
        BadgeEntity badge = new BadgeEntity();
        badge.setUserId(userId);
        badge.setBadgeType(badgeType);
        badge.setBadgeName(badgeName);
        badge.setBadgeDescription(description);
        badge.setBadgeIcon(icon);
        badge.setRequiredCount(requiredCount);
        badge.setBreweryId(breweryId);
        badge.setEarned(false);

        database.badgeDAO().insert(badge);
    }

    // Пуска всички badge проверки по брой/праг.
    public void checkAndUnlockBadges(String userId, int totalStamps, int totalVisits,
                                     int uniqueBreweriesCount) {
        executorService.execute(() -> {
            // Stamp milestones.
            checkBadge(userId, BADGE_BRONZE, totalStamps, 10);
            checkBadge(userId, BADGE_REGULAR, totalStamps, 15);
            checkBadge(userId, BADGE_SILVER, totalStamps, 25);
            checkBadge(userId, BADGE_VETERAN, totalStamps, 40);
            checkBadge(userId, BADGE_GOLD, totalStamps, 50);
            checkBadge(userId, BADGE_BREW_MASTER, totalStamps, 60);
            checkBadge(userId, BADGE_DIAMOND, totalStamps, 75);
            checkBadge(userId, BADGE_LEGEND, totalStamps, 90);
            checkBadge(userId, BADGE_PLATINUM, totalStamps, 100);

            // Visit milestones.
            if (totalVisits >= 1) {
                unlockBadgeByType(userId, BADGE_FIRST_VISIT);
            }
            checkBadge(userId, BADGE_DOUBLE_DIGITS, totalVisits, 10);
            checkBadge(userId, BADGE_QUARTER_CENTURY, totalVisits, 25);
            checkBadge(userId, BADGE_HALF_CENTURY, totalVisits, 50);
            checkBadge(userId, BADGE_CENTURION, totalVisits, 100);

            // Допълнителни stamp milestones.
            checkBadge(userId, BADGE_STAMP_STREAK, totalStamps, 20);
            checkBadge(userId, BADGE_COMEBACK_KID, totalStamps, 30);

            // Badge-ове за обикаляне на brewery-та.
            checkBadge(userId, BADGE_EXPLORER, uniqueBreweriesCount, 10);
            checkBadge(userId, BADGE_EXPLORER_PRO, uniqueBreweriesCount, 25);
        });
    }

    // Unlock helper, когато текущата стойност мине нужния праг.
    private void checkBadge(String userId, String badgeType, int currentCount, int required) {
        if (currentCount >= required) {
            unlockBadgeByType(userId, badgeType);
        }
    }

    // Маркира badge като earned и го sync-ва към cloud.
    private void unlockBadgeByType(String userId, String badgeType) {
        try {
            BadgeEntity badge = database.badgeDAO().getBadgeByTypeSync(userId, badgeType);
            if (badge != null && !badge.isEarned()) {
                long now = System.currentTimeMillis();
                database.badgeDAO().unlockBadge(badge.getBadgeId(), now);
                Log.d(TAG, "Badge unlocked: " + badgeType + " for user: " + userId);

                // Презареждаме обновения badge за cloud sync.
                BadgeEntity updated = database.badgeDAO().getBadgeByTypeSync(userId, badgeType);
                if (updated != null && appwriteService != null
                        && userId != null && !userId.startsWith("guest_")) {
                    appwriteService.syncBadge(updated, new AppwriteService.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Badge synced to Appwrite: " + badgeType);
                        }
                        @Override
                        public void onError(String message) {
                            Log.e(TAG, "Badge sync failed for " + badgeType + ": " + message);
                        }
                    });
                }
            } else if (badge == null) {
                Log.w(TAG, "Badge not found: " + badgeType + " for user: " + userId);
            } else {
                Log.d(TAG, "Badge already earned: " + badgeType + " for user: " + userId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unlocking badge " + badgeType + ": " + e.getMessage());
        }
    }

    // Проверява rating badge-овете.
    public void checkRatingBadges(String userId, int totalRatings) {
        executorService.execute(() -> {
            // Rating milestones.
            checkBadge(userId, BADGE_TRAILBLAZER, totalRatings, 1);
            checkBadge(userId, BADGE_CRITIC, totalRatings, 5);
            checkBadge(userId, BADGE_TASTE_TESTER, totalRatings, 10);
            checkBadge(userId, BADGE_BEER_CONNOISSEUR, totalRatings, 20);
            checkBadge(userId, BADGE_DATA_NERD, totalRatings, 30);
            checkBadge(userId, BADGE_MASTER_CRITIC, totalRatings, 50);

            // Five-star milestones.
            try {
                int fiveStarCount = database.beerRatingDAO().getFiveStarRatingCountSync(userId);
                checkBadge(userId, BADGE_SWEET_TOOTH, fiveStarCount, 3);
                checkBadge(userId, BADGE_FIVE_STAR, fiveStarCount, 5);
            } catch (Exception e) {
                Log.e(TAG, "Error checking five-star badges: " + e.getMessage());
            }
        });
    }

    // Проверява badge-овете, свързани с loyalty cards.
    public void checkCardBadges(String userId) {
        executorService.execute(() -> {
            try {
                int totalCards = database.loyaltyCardDAO().getTotalCardCountSync(userId);
                checkBadge(userId, BADGE_SUPPORTER, totalCards, 5);

                int completedCards = database.loyaltyCardDAO().getCompletedCardsCountSync(userId);
                checkBadge(userId, BADGE_LOYALTY_MASTER, completedCards, 3);
            } catch (Exception e) {
                Log.e(TAG, "Error checking card badges: " + e.getMessage());
            }
        });
    }

    // Unlock-ва share badge-а при първо споделяне.
    public void checkSharingBadges(String userId) {
        executorService.execute(() -> {
            unlockBadgeByType(userId, BADGE_PARTY_STARTER);
        });
    }

    // Проверява badge-ове по модел на посещения (пример: Homebody).
    public void checkVisitBadges(String userId) {
        executorService.execute(() -> {
            try {
                // Homebody: посещения в една и съща brewery 5+ пъти.
                int maxVisits = database.visitDAO().getMaxVisitsToSingleBrewerySync(userId);
                checkBadge(userId, BADGE_HOMEBODY, maxVisits, 5);
                checkBadge(userId, BADGE_DEDICATION, maxVisits, 10);
            } catch (Exception e) {
                Log.e(TAG, "Error checking visit badges: " + e.getMessage());
            }
        });
    }
}

