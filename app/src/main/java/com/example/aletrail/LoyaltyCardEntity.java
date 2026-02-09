package com.example.aletrail;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "loyalty_card_table")
public class LoyaltyCardEntity {

    @PrimaryKey(autoGenerate = true)
    private int cardId;

    private String userId;
    private String breweryId;
    private int stamps;
    private int maxStamps;
    private String qrCodeValue;
    private boolean active;

    public LoyaltyCardEntity(String userId, String breweryId, int maxStamps, String qrCodeValue) {
        this.userId = userId;
        this.breweryId = breweryId;
        this.maxStamps = maxStamps;
        this.qrCodeValue = qrCodeValue;
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

    public String getQrCodeValue() {
        return qrCodeValue;
    }

    public void setQrCodeValue(String qrCodeValue) {
        this.qrCodeValue = qrCodeValue;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
