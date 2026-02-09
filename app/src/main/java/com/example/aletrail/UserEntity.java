package com.example.aletrail;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_table")
public class UserEntity {

    @PrimaryKey
    @NonNull
    private String userId;

    private String email;
    private String displayName;
    private String authProvider; // "google", "facebook", "email"
    private String profileImageUrl;
    private long createdAt;
    private long lastSyncTimestamp;
    private boolean isPremium;

    // Gamification stats
    private int totalStamps;
    private int totalVisits;
    private int totalBreweriesVisited;

    public UserEntity() {
        this.createdAt = System.currentTimeMillis();
        this.lastSyncTimestamp = System.currentTimeMillis();
        this.isPremium = false;
        this.totalStamps = 0;
        this.totalVisits = 0;
        this.totalBreweriesVisited = 0;
    }

    // Getters and Setters
    @NonNull
    public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getLastSyncTimestamp() { return lastSyncTimestamp; }
    public void setLastSyncTimestamp(long lastSyncTimestamp) { this.lastSyncTimestamp = lastSyncTimestamp; }

    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }

    public int getTotalStamps() { return totalStamps; }
    public void setTotalStamps(int totalStamps) { this.totalStamps = totalStamps; }

    public int getTotalVisits() { return totalVisits; }
    public void setTotalVisits(int totalVisits) { this.totalVisits = totalVisits; }

    public int getTotalBreweriesVisited() { return totalBreweriesVisited; }
    public void setTotalBreweriesVisited(int totalBreweriesVisited) { this.totalBreweriesVisited = totalBreweriesVisited; }
}
