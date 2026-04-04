package com.example.aletrail;

// Централно място за ID-тата на Appwrite базата и колекциите.
public final class AppwriteConstants {

    private AppwriteConstants() {
    }

    // ID на базата в Appwrite.
    public static final String DATABASE_ID = "aletrail_db";

    // ID-та на колекциите за sync.
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_FAVORITES = "favorites";
    public static final String COLLECTION_LOYALTY_CARDS = "loyalty_cards";
    public static final String COLLECTION_VISITS = "visits";
    public static final String COLLECTION_BADGES = "badges";
    public static final String COLLECTION_BEER_RATINGS = "beer_ratings";
}
