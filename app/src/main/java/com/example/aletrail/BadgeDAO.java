package com.example.aletrail;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BadgeDAO {

    @Insert
    long insert(BadgeEntity badge);

    @Update
    void update(BadgeEntity badge);

    @Query("SELECT * FROM badge_table WHERE userId = :userId ORDER BY earnedTimestamp DESC")
    LiveData<List<BadgeEntity>> getAllBadgesForUser(String userId);

    @Query("SELECT * FROM badge_table WHERE userId = :userId AND isEarned = 1 ORDER BY earnedTimestamp DESC")
    LiveData<List<BadgeEntity>> getEarnedBadges(String userId);

    @Query("SELECT * FROM badge_table WHERE userId = :userId AND isEarned = 1 ORDER BY earnedTimestamp DESC")
    List<BadgeEntity> getEarnedBadgesSync(String userId);

    @Query("SELECT * FROM badge_table WHERE userId = :userId ORDER BY earnedTimestamp DESC")
    List<BadgeEntity> getAllBadgesForUserSync(String userId);

    @Query("SELECT * FROM badge_table WHERE userId = :userId AND isEarned = 0")
    LiveData<List<BadgeEntity>> getUnlockedBadges(String userId);

    @Query("SELECT * FROM badge_table WHERE userId = :userId AND badgeType = :badgeType LIMIT 1")
    BadgeEntity getBadgeByTypeSync(String userId, String badgeType);

    @Query("UPDATE badge_table SET isEarned = 1, earnedTimestamp = :timestamp WHERE badgeId = :badgeId")
    void unlockBadge(int badgeId, long timestamp);

    @Query("SELECT COUNT(*) FROM badge_table WHERE userId = :userId AND isEarned = 1")
    LiveData<Integer> getEarnedBadgeCount(String userId);

    @Query("DELETE FROM badge_table WHERE userId = :userId")
    void deleteByUserId(String userId);
}
