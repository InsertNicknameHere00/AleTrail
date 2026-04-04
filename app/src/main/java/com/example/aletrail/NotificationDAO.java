package com.example.aletrail;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NotificationDAO {

    @Insert
    long insert(NotificationEntity notification);

    @Query("SELECT * FROM notification_table WHERE userId = :userId ORDER BY timestamp DESC")
    LiveData<List<NotificationEntity>> getNotificationsForUser(String userId);

    @Query("UPDATE notification_table SET isRead = 1 WHERE notificationId = :notificationId")
    void markAsRead(int notificationId);

    @Query("UPDATE notification_table SET isRead = 1 WHERE userId = :userId")
    void markAllAsRead(String userId);

    @Query("DELETE FROM notification_table WHERE userId = :userId")
    void deleteByUserId(String userId);
}

