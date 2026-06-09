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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
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

    // Make search case-insensitive
    @Query("SELECT * FROM brewery_table WHERE LOWER(name) LIKE '%' || LOWER(:searchQuery) || '%'")
    LiveData<List<BreweryEntity>> searchAles(String searchQuery);

    @Query("SELECT * FROM brewery_table WHERE isFavorite = 1")
    LiveData<List<BreweryEntity>> getFavorites();

    @Query("SELECT * FROM brewery_table WHERE isFavorite = 1")
    List<BreweryEntity> getFavoritesSync();

    // Case-insensitive state/type queries
    @Query("SELECT * FROM brewery_table WHERE LOWER(state) = LOWER(:state)")
    LiveData<List<BreweryEntity>> getBreweriesByState(String state);

    @Query("SELECT * FROM brewery_table WHERE LOWER(brewery_type) = LOWER(:type)")
    LiveData<List<BreweryEntity>> getBreweriesByType(String type);

    @Query("SELECT * FROM brewery_table WHERE LOWER(state) = LOWER(:state) AND LOWER(brewery_type) = LOWER(:type)")
    LiveData<List<BreweryEntity>> getBreweriesByStateAndType(String state, String type);

    @Query("UPDATE brewery_table SET isFavorite = 0 WHERE isFavorite = 1")
    void clearAllFavorites();
}
