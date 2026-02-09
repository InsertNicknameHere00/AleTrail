package com.example.aletrail;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "beer_rating_table")
public class BeerRatingEntity {

    @PrimaryKey(autoGenerate = true)
    private int ratingId;

    private String userId;
    private String breweryId;
    private String beerName;
    private float rating; // 0.0 to 5.0
    private String comment;
    private long ratingTimestamp;
    private boolean synced;

    public BeerRatingEntity() {
        this.ratingTimestamp = System.currentTimeMillis();
        this.synced = false;
    }

    // Getters and Setters
    public int getRatingId() { return ratingId; }
    public void setRatingId(int ratingId) { this.ratingId = ratingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getBreweryId() { return breweryId; }
    public void setBreweryId(String breweryId) { this.breweryId = breweryId; }

    public String getBeerName() { return beerName; }
    public void setBeerName(String beerName) { this.beerName = beerName; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public long getRatingTimestamp() { return ratingTimestamp; }
    public void setRatingTimestamp(long ratingTimestamp) { this.ratingTimestamp = ratingTimestamp; }

    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }
}

