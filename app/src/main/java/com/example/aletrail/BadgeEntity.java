package com.example.aletrail;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "badge_table")
public class BadgeEntity {

    @PrimaryKey(autoGenerate = true)
    private int badgeId;

    private String userId;
    private String badgeType; // "BRONZE", "SILVER", "GOLD", "PLATINUM", "FIRST_VISIT", "EXPLORER", etc.
    private String badgeName;
    private String badgeDescription;
    private String badgeIcon; // emoji or resource name
    private int requiredCount; // stamps/visits needed
    private long earnedTimestamp;
    private boolean isEarned;
    private String breweryId; // optional, for brewery-specific badges

    public BadgeEntity() {
        this.isEarned = false;
    }

    // Getters and Setters
    public int getBadgeId() { return badgeId; }
    public void setBadgeId(int badgeId) { this.badgeId = badgeId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getBadgeType() { return badgeType; }
    public void setBadgeType(String badgeType) { this.badgeType = badgeType; }

    public String getBadgeName() { return badgeName; }
    public void setBadgeName(String badgeName) { this.badgeName = badgeName; }

    public String getBadgeDescription() { return badgeDescription; }
    public void setBadgeDescription(String badgeDescription) { this.badgeDescription = badgeDescription; }

    public String getBadgeIcon() { return badgeIcon; }
    public void setBadgeIcon(String badgeIcon) { this.badgeIcon = badgeIcon; }

    public int getRequiredCount() { return requiredCount; }
    public void setRequiredCount(int requiredCount) { this.requiredCount = requiredCount; }

    public long getEarnedTimestamp() { return earnedTimestamp; }
    public void setEarnedTimestamp(long earnedTimestamp) { this.earnedTimestamp = earnedTimestamp; }

    public boolean isEarned() { return isEarned; }
    public void setEarned(boolean earned) { isEarned = earned; }

    public String getBreweryId() { return breweryId; }
    public void setBreweryId(String breweryId) { this.breweryId = breweryId; }
}

