package com.example.aletrail;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BreweryRepository {

    private static final String TAG = "BreweryRepository";
    private AleTrailDAO breweryDao;
    private TheAleTrailAPI api;
    private ExecutorService executorService;

    public BreweryRepository(Context context) {
        Database database = Database.getInstance(context);
        this.breweryDao = database.AleDAO();
        this.api = RetrofitClient.getAleTrailAPI();
        this.executorService = Executors.newFixedThreadPool(2);
    }

    /**
     * Получава всички пивоварни от локалната база
     */
    public LiveData<List<BreweryEntity>> getAllBreweries() {
        return breweryDao.getAllAles();
    }

    /**
     * Търси пивоварни по име
     */
    public LiveData<List<BreweryEntity>> searchBreweries(String query) {
        return breweryDao.searchAles(query);
    }

    /**
     * Получава любими пивоварни
     */
    public LiveData<List<BreweryEntity>> getFavoriteBreweries() {
        return breweryDao.getFavorites();
    }

    /**
     * Get breweries by state from database
     */
    public LiveData<List<BreweryEntity>> getBreweriesByState(String state) {
        return breweryDao.getBreweriesByState(state);
    }

    /**
     * Get breweries by type from database
     */
    public LiveData<List<BreweryEntity>> getBreweriesByType(String type) {
        return breweryDao.getBreweriesByType(type);
    }

    /**
     * Get breweries by state and type from database
     */
    public LiveData<List<BreweryEntity>> getBreweriesByStateAndType(String state, String type) {
        return breweryDao.getBreweriesByStateAndType(state, type);
    }

    /**
     * Fetch breweries from API by location
     */
    public void fetchBreweriesByLocation(double latitude, double longitude, int perPage) {
        String location = latitude + "," + longitude;
        api.getBreweriesByLocation(location, perPage).enqueue(new Callback<List<BreweryEntity>>() {
            @Override
            public void onResponse(Call<List<BreweryEntity>> call, Response<List<BreweryEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    executorService.execute(() -> {
                        breweryDao.insertAll(response.body());
                        Log.d(TAG, "Fetched " + response.body().size() + " breweries from API");
                    });
                }
            }

            @Override
            public void onFailure(Call<List<BreweryEntity>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch breweries: " + t.getMessage());
            }
        });
    }

    /**
     * Fetch breweries by city
     */
    public void fetchBreweriesByCity(String city, int perPage) {
        api.getBreweriesByCity(city, perPage).enqueue(new Callback<List<BreweryEntity>>() {
            @Override
            public void onResponse(Call<List<BreweryEntity>> call, Response<List<BreweryEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    executorService.execute(() -> {
                        breweryDao.insertAll(response.body());
                        Log.d(TAG, "Fetched " + response.body().size() + " breweries for city: " + city);
                    });
                }
            }

            @Override
            public void onFailure(Call<List<BreweryEntity>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch breweries by city: " + t.getMessage());
            }
        });
    }

    /**
     * Fetch breweries by state/province
     */
    public void fetchBreweriesByState(String state, int perPage) {
        api.filterBreweries(null, state, null, null, perPage).enqueue(new Callback<List<BreweryEntity>>() {
            @Override
            public void onResponse(Call<List<BreweryEntity>> call, Response<List<BreweryEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    executorService.execute(() -> {
                        breweryDao.insertAll(response.body());
                        Log.d(TAG, "Fetched " + response.body().size() + " breweries for state: " + state);
                    });
                }
            }

            @Override
            public void onFailure(Call<List<BreweryEntity>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch breweries by state: " + t.getMessage());
            }
        });
    }

    /**
     * Fetch breweries by type
     */
    public void fetchBreweriesByType(String type, int perPage) {
        api.filterBreweries(null, null, null, type, perPage).enqueue(new Callback<List<BreweryEntity>>() {
            @Override
            public void onResponse(Call<List<BreweryEntity>> call, Response<List<BreweryEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    executorService.execute(() -> {
                        breweryDao.insertAll(response.body());
                        Log.d(TAG, "Fetched " + response.body().size() + " breweries for type: " + type);
                    });
                }
            }

            @Override
            public void onFailure(Call<List<BreweryEntity>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch breweries by type: " + t.getMessage());
            }
        });
    }

    /**
     * Fetch breweries by state AND type
     */
    public void fetchBreweriesByStateAndType(String state, String type, int perPage) {
        api.filterBreweries(null, state, null, type, perPage).enqueue(new Callback<List<BreweryEntity>>() {
            @Override
            public void onResponse(Call<List<BreweryEntity>> call, Response<List<BreweryEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    executorService.execute(() -> {
                        breweryDao.insertAll(response.body());
                        Log.d(TAG, "Fetched " + response.body().size() + " breweries for state: " + state + ", type: " + type);
                    });
                }
            }

            @Override
            public void onFailure(Call<List<BreweryEntity>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch breweries by state and type: " + t.getMessage());
            }
        });
    }

    /**
     * Добавя/премахва от любими
     */
    public void toggleFavorite(BreweryEntity brewery) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "toggleFavorite called for: " + brewery.getName() + " (ID: " + brewery.getId() + ")");

                // First, ensure the brewery exists in the database
                BreweryEntity existingBrewery = breweryDao.getAleByIdSync(brewery.getId());

                if (existingBrewery == null) {
                    // Brewery doesn't exist in DB yet, insert it first
                    Log.d(TAG, "Brewery not in DB, inserting first...");
                    brewery.setFavorite(true); // Mark as favorite
                    breweryDao.insert(brewery);
                    Log.d(TAG, "Inserted brewery with favorite=true");
                } else {
                    // Brewery exists, toggle its favorite status
                    Log.d(TAG, "Brewery exists in DB with favorite=" + existingBrewery.isFavorite());
                    existingBrewery.setFavorite(!existingBrewery.isFavorite());
                    breweryDao.update(existingBrewery);
                    Log.d(TAG, "Updated brewery favorite=" + existingBrewery.isFavorite());
                }

                // Verify the update
                BreweryEntity verifyBrewery = breweryDao.getAleByIdSync(brewery.getId());
                if (verifyBrewery != null) {
                    Log.d(TAG, "Verification - favorite state: " + verifyBrewery.isFavorite());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error toggling favorite: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Получава една пивоварна по ID
     */
    public LiveData<BreweryEntity> getBreweryById(String id) {
        return breweryDao.getAleById(id);
    }
}
