package com.example.aletrail;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AleTrailDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BreweryEntity ale);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BreweryEntity> ales);

    @Update
    void update(BreweryEntity ale);

    @Delete
    void delete(BreweryEntity ale);

    @Query("SELECT * FROM brewery_table")
    LiveData<List<BreweryEntity>> getAllAles();

    @Query("SELECT * FROM brewery_table WHERE id = :id")
    LiveData<BreweryEntity> getAleById(String id);

    @Query("SELECT * FROM brewery_table WHERE id = :id")
    BreweryEntity getAleByIdSync(String id);

    @Query("SELECT * FROM brewery_table WHERE name LIKE '%' || :searchQuery || '%'")
    LiveData<List<BreweryEntity>> searchAles(String searchQuery);

    @Query("SELECT * FROM brewery_table WHERE isFavorite = 1")
    LiveData<List<BreweryEntity>> getFavorites();

    @Query("SELECT * FROM brewery_table WHERE state = :state")
    LiveData<List<BreweryEntity>> getBreweriesByState(String state);

    @Query("SELECT * FROM brewery_table WHERE brewery_type = :type")
    LiveData<List<BreweryEntity>> getBreweriesByType(String type);

    @Query("SELECT * FROM brewery_table WHERE state = :state AND brewery_type = :type")
    LiveData<List<BreweryEntity>> getBreweriesByStateAndType(String state, String type);
}
