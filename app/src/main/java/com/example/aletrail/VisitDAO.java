package com.example.aletrail;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface VisitDAO {

    @Insert
    long insert(VisitEntity visit);

    @Query("SELECT * FROM visit_table WHERE userId = :userId ORDER BY visitTimestamp DESC")
    LiveData<List<VisitEntity>> getVisitsForUser(String userId);

    @Query("SELECT * FROM visit_table WHERE breweryId = :breweryId AND userId = :userId ORDER BY visitTimestamp DESC")
    LiveData<List<VisitEntity>> getVisitsForBrewery(String breweryId, String userId);

    @Query("SELECT COUNT(*) FROM visit_table WHERE userId = :userId")
    LiveData<Integer> getTotalVisitCount(String userId);

    @Query("SELECT COUNT(DISTINCT breweryId) FROM visit_table WHERE userId = :userId")
    LiveData<Integer> getUniqueBreweriesVisited(String userId);

    @Query("SELECT COUNT(DISTINCT breweryId) FROM visit_table WHERE userId = :userId")
    int getUniqueBreweriesVisitedSync(String userId);

    @Query("SELECT MAX(visitTimestamp) FROM visit_table WHERE userId = :userId AND breweryId = :breweryId")
    Long getLastVisitTimestampSync(String userId, String breweryId);

    @Query("SELECT * FROM visit_table WHERE cardId = :cardId ORDER BY visitTimestamp DESC")
    List<VisitEntity> getVisitsForCardSync(int cardId);

    @Query("SELECT * FROM visit_table WHERE synced = 0")
    List<VisitEntity> getUnsyncedVisits();

    @Query("UPDATE visit_table SET synced = 1 WHERE visitId = :visitId")
    void markAsSynced(int visitId);

    @Query("DELETE FROM visit_table WHERE userId = :userId")
    void deleteByUserId(String userId);

    @Query("SELECT MAX(cnt) FROM (SELECT COUNT(*) as cnt FROM visit_table WHERE userId = :userId GROUP BY breweryId)")
    int getMaxVisitsToSingleBrewerySync(String userId);

    @Query("SELECT COUNT(*) FROM visit_table WHERE userId = :userId")
    int getTotalVisitCountSync(String userId);
}
