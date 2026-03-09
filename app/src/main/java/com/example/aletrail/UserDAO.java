package com.example.aletrail;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Query("SELECT * FROM user_table WHERE userId = :userId LIMIT 1")
    LiveData<UserEntity> getUserById(String userId);

    @Query("SELECT * FROM user_table WHERE userId = :userId LIMIT 1")
    UserEntity getUserByIdSync(String userId);

    @Query("SELECT * FROM user_table WHERE email = :email LIMIT 1")
    LiveData<UserEntity> getUserByEmail(String email);

    @Query("UPDATE user_table SET lastSyncTimestamp = :timestamp WHERE userId = :userId")
    void updateLastSyncTimestamp(String userId, long timestamp);

    @Query("UPDATE user_table SET totalStamps = totalStamps + 1 WHERE userId = :userId")
    void incrementTotalStamps(String userId);

    @Query("UPDATE user_table SET totalVisits = totalVisits + 1 WHERE userId = :userId")
    void incrementTotalVisits(String userId);

    @Query("UPDATE user_table SET totalBreweriesVisited = :count WHERE userId = :userId")
    void updateTotalBreweriesVisited(String userId, int count);

    @Query("DELETE FROM user_table WHERE userId = :userId")
    void deleteByUserId(String userId);
}
