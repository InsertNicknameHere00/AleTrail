package com.example.aletrail;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LoyaltyCardDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LoyaltyCardEntity card);

    @Update
    void update(LoyaltyCardEntity card);

    @Query("SELECT * FROM loyalty_card_table WHERE cardId = :cardId")
    LiveData<LoyaltyCardEntity> getCardById(int cardId);

    @Query("SELECT * FROM loyalty_card_table WHERE userId = :userId")
    LiveData<List<LoyaltyCardEntity>> getCardsForUser(String userId);

    @Query("SELECT * FROM loyalty_card_table WHERE userId = :userId AND breweryId = :breweryId LIMIT 1")
    LiveData<LoyaltyCardEntity> getCardForUserAndBrewery(String userId, String breweryId);

    @Query("UPDATE loyalty_card_table SET stamps = stamps + 1 WHERE cardId = :cardId AND stamps < maxStamps")
    void addStamp(int cardId);

    @Query("UPDATE loyalty_card_table SET stamps = 0 WHERE cardId = :cardId")
    void resetStamps(int cardId);

    @Query("DELETE FROM loyalty_card_table WHERE cardId = :cardId")
    void deleteCard(int cardId);
}
