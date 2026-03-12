package com.example.aletrail;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BeerRatingDAO {

    @Insert
    long insert(BeerRatingEntity rating);

    @Update
    void update(BeerRatingEntity rating);

    @Query("SELECT * FROM beer_rating_table WHERE userId = :userId ORDER BY ratingTimestamp DESC")
    LiveData<List<BeerRatingEntity>> getRatingsForUser(String userId);

    @Query("SELECT * FROM beer_rating_table WHERE breweryId = :breweryId ORDER BY ratingTimestamp DESC")
    LiveData<List<BeerRatingEntity>> getRatingsForBrewery(String breweryId);

    @Query("SELECT AVG(rating) FROM beer_rating_table WHERE breweryId = :breweryId")
    LiveData<Float> getAverageRatingForBrewery(String breweryId);

    @Query("SELECT * FROM beer_rating_table WHERE synced = 0")
    List<BeerRatingEntity> getUnsyncedRatings();

    @Query("UPDATE beer_rating_table SET synced = 1 WHERE ratingId = :ratingId")
    void markAsSynced(int ratingId);

    @Query("SELECT COUNT(*) FROM beer_rating_table WHERE userId = :userId")
    int getTotalRatingCountSync(String userId);

    @Query("SELECT COUNT(*) FROM beer_rating_table WHERE userId = :userId AND rating >= 5.0")
    int getFiveStarRatingCountSync(String userId);

    @Query("DELETE FROM beer_rating_table WHERE userId = :userId")
    void deleteByUserId(String userId);
}
