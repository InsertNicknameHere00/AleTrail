package com.example.aletrail;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "visit_table")
public class VisitEntity {

    @PrimaryKey(autoGenerate = true)
    private int visitId;

    private String userId;
    private String breweryId;
    private int cardId;
    private long visitTimestamp;
    private boolean stampAdded;
    private String notes;
    private double latitude;
    private double longitude;
    private boolean synced;

    public VisitEntity() {
        this.visitTimestamp = System.currentTimeMillis();
        this.stampAdded = false;
        this.synced = false;
    }

    // Стандартни getters/setters за Room.
    public int getVisitId() { return visitId; }
    public void setVisitId(int visitId) { this.visitId = visitId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getBreweryId() { return breweryId; }
    public void setBreweryId(String breweryId) { this.breweryId = breweryId; }

    public int getCardId() { return cardId; }
    public void setCardId(int cardId) { this.cardId = cardId; }

    public long getVisitTimestamp() { return visitTimestamp; }
    public void setVisitTimestamp(long visitTimestamp) { this.visitTimestamp = visitTimestamp; }

    public boolean isStampAdded() { return stampAdded; }
    public void setStampAdded(boolean stampAdded) { this.stampAdded = stampAdded; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }
}
