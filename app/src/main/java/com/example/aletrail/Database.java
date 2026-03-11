package com.example.aletrail;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {
        BreweryEntity.class,
        LoyaltyCardEntity.class,
        UserEntity.class,
        VisitEntity.class,
        BadgeEntity.class,
        BeerRatingEntity.class
}, version = 5, exportSchema = false)
public abstract class Database extends RoomDatabase {
    public abstract AleTrailDAO AleDAO();
    public abstract LoyaltyCardDAO loyaltyCardDAO();
    public abstract UserDAO userDAO();
    public abstract VisitDAO visitDAO();
    public abstract BadgeDAO badgeDAO();
    public abstract BeerRatingDAO beerRatingDAO();

    private static volatile Database INSTANCE;

    public static Database getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (Database.class) {
                INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                        Database.class, "brewery_database")
                        .fallbackToDestructiveMigration()
                        .build();
            }
        }
        return INSTANCE;
    }
}
