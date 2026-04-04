package com.example.aletrail;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "loyalty_card_table",
        indices = {@androidx.room.Index(value = {"userId", "breweryId"}, unique = true)})
public class LoyaltyCardEntity {

    @PrimaryKey(autoGenerate = true)
    private int cardId;

    private String userId;
    private String breweryId;
    private int stamps;
    private int maxStamps;
    private boolean active;

    @Ignore
    public LoyaltyCardEntity(String userId, String breweryId, int maxStamps) {
        this.userId = userId;
        this.breweryId = breweryId;
        this.maxStamps = maxStamps;
        this.stamps = 0;
        this.active = true;
    }

    public LoyaltyCardEntity() {
    }

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBreweryId() {
        return breweryId;
    }

    public void setBreweryId(String breweryId) {
        this.breweryId = breweryId;
    }

    public int getStamps() {
        return stamps;
    }

    public void setStamps(int stamps) {
        this.stamps = stamps;
    }

    public int getMaxStamps() {
        return maxStamps;
    }

    public void setMaxStamps(int maxStamps) {
        this.maxStamps = maxStamps;
    }


    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
