package com.example.aletrail;

import java.util.List;
import java.util.Map;

public class CartesModels {

    // MapResponse model
    public static class MapResponse {
        private String token;
        private String uuid;
        private String title;
        private String slug;
        private String description;
        private boolean privacyPassword;
        private boolean publishedAt;
        private String createdAt;
        private String updatedAt;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    // MapListResponse model
    public static class MapListResponse {
        private List<MapResponse> data;

        public List<MapResponse> getData() { return data; }
        public void setData(List<MapResponse> data) { this.data = data; }
    }

    // CreateMapRequest model
    public static class CreateMapRequest {
        private String title;
        private String slug;
        private String description;
        private boolean privacyPassword;

        public CreateMapRequest(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    // UpdateMapRequest model
    public static class UpdateMapRequest {
        private String title;
        private String description;
        private boolean privacyPassword;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    // MarkerResponse model
    public static class MarkerResponse {
        private String id;
        private String category;
        private double[] coordinates;
        private String description;
        private String createdAt;
        private String updatedAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public double[] getCoordinates() { return coordinates; }
        public void setCoordinates(double[] coordinates) { this.coordinates = coordinates; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    // MarkerListResponse model
    public static class MarkerListResponse {
        private List<MarkerResponse> data;

        public List<MarkerResponse> getData() { return data; }
        public void setData(List<MarkerResponse> data) { this.data = data; }
    }

    // CreateMarkerRequest model
    public static class CreateMarkerRequest {
        private String map_token;
        private double lat;
        private double lng;
        private String category_name;
        private String description;

        public CreateMarkerRequest(String mapToken, double lat, double lng, String categoryName, String description) {
            this.map_token = mapToken;
            this.lat = lat;
            this.lng = lng;
            this.category_name = categoryName;
            this.description = description;
        }

        public String getMap_token() { return map_token; }
        public void setMap_token(String map_token) { this.map_token = map_token; }

        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }

        public double getLng() { return lng; }
        public void setLng(double lng) { this.lng = lng; }

        public String getCategory_name() { return category_name; }
        public void setCategory_name(String category_name) { this.category_name = category_name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
