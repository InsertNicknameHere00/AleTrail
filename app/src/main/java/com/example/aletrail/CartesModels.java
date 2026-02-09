package com.example.aletrail;

import java.util.List;
import java.util.Map;

// All models in one file for simplicity

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

        // Getters and setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        // Add remaining getters/setters as needed
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
        private String category;
        private double[] coordinates;
        private Map<String, Object> data;
        private String description;

        public CreateMarkerRequest(String category, double[] coordinates, String description) {
            this.category = category;
            this.coordinates = coordinates;
            this.description = description;
        }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public double[] getCoordinates() { return coordinates; }
        public void setCoordinates(double[] coordinates) { this.coordinates = coordinates; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }
}
