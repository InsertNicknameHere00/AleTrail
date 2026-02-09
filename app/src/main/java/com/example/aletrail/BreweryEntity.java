package com.example.aletrail;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "brewery_table")
public class BreweryEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String brewery_type;
    private String street;
    private String city;
    private String state;
    private String postal_code;
    private String country;
    private Double longitude;
    private Double latitude;
    private String phone;
    private String website_url;
    private boolean isFavorite;

    // Constructor
    public BreweryEntity() {}

    // Getters and setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrewery_type() { return brewery_type; }
    public void setBrewery_type(String brewery_type) { this.brewery_type = brewery_type; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPostal_code() { return postal_code; }
    public void setPostal_code(String postal_code) { this.postal_code = postal_code; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getWebsite_url() { return website_url; }
    public void setWebsite_url(String website_url) { this.website_url = website_url; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}
