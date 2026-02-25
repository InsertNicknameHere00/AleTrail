package com.example.aletrail;

/**
 * Constants for Appwrite Database and Collection IDs.
 *
 * These must match the collections created in the Appwrite Console.
 * Go to your Appwrite project → Databases → Create Database "aletrail_db"
 * Then create these collections with the attributes described below:
 *
 * Collection: users
 *   - email (string, 255)
 *   - displayName (string, 255)
 *   - authProvider (string, 50)
 *   - profileImageUrl (string, 500, optional)
 *   - createdAt (integer)
 *   - totalStamps (integer)
 *   - totalVisits (integer)
 *   - totalBreweriesVisited (integer)
 *   - isPremium (boolean)
 *
 * Collection: favorites
 *   - userId (string, 255)
 *   - breweryId (string, 255)
 *   - breweryName (string, 500)
 *   - addedAt (integer)
 *
 * Collection: loyalty_cards
 *   - userId (string, 255)
 *   - breweryId (string, 255)
 *   - stamps (integer)
 *   - maxStamps (integer)
 *   - qrCodeValue (string, 500)
 *   - active (boolean)
 *
 * Collection: visits
 *   - userId (string, 255)
 *   - breweryId (string, 255)
 *   - cardId (integer)
 *   - visitTimestamp (integer)
 *   - stampAdded (boolean)
 *   - notes (string, 1000, optional)
 *   - latitude (double)
 *   - longitude (double)
 *
 * Collection: badges
 *   - userId (string, 255)
 *   - badgeType (string, 100)
 *   - badgeName (string, 255)
 *   - badgeDescription (string, 500)
 *   - badgeIcon (string, 50)
 *   - requiredCount (integer)
 *   - earnedTimestamp (integer)
 *   - isEarned (boolean)
 *   - breweryId (string, 255, optional)
 *
 * Collection: beer_ratings
 *   - userId (string, 255)
 *   - breweryId (string, 255)
 *   - beerName (string, 255)
 *   - rating (double)
 *   - comment (string, 2000, optional)
 *   - ratingTimestamp (integer)
 */
public final class AppwriteConstants {

    private AppwriteConstants() {
    }

    // Database
    public static final String DATABASE_ID = "aletrail_db";

    // Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_FAVORITES = "favorites";
    public static final String COLLECTION_LOYALTY_CARDS = "loyalty_cards";
    public static final String COLLECTION_VISITS = "visits";
    public static final String COLLECTION_BADGES = "badges";
    public static final String COLLECTION_BEER_RATINGS = "beer_ratings";
}

