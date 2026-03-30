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
    private AppwriteService appwriteService;

    public BreweryRepository(Context context) {
        Database database = Database.getInstance(context);
        this.breweryDao = database.AleDAO();
        this.api = RetrofitClient.getAleTrailAPI();
        this.executorService = Executors.newFixedThreadPool(2);
        this.appwriteService = AppwriteService.getInstance(context);
    }

    // Reads all breweries stored locally.
    public LiveData<List<BreweryEntity>> getAllBreweries() {
        return breweryDao.getAllAles();
    }

    // Local search by brewery name.
    public LiveData<List<BreweryEntity>> searchBreweries(String query) {
        return breweryDao.searchAles(query);
    }

    // Reads only favorite breweries.
    public LiveData<List<BreweryEntity>> getFavoriteBreweries() {
        return breweryDao.getFavorites();
    }

    // Filter helpers from local DB.
    public LiveData<List<BreweryEntity>> getBreweriesByState(String state) {
        return breweryDao.getBreweriesByState(state);
    }

    public LiveData<List<BreweryEntity>> getBreweriesByType(String type) {
        return breweryDao.getBreweriesByType(type);
    }

    public LiveData<List<BreweryEntity>> getBreweriesByStateAndType(String state, String type) {
        return breweryDao.getBreweriesByStateAndType(state, type);
    }

    // Fetch nearby breweries from API.
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

    // Fetch breweries by city from API.
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

    // Fetch breweries by state from API.
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

    // Fetch breweries by type from API.
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

    // Fetch breweries by state + type from API.
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

    // Callback for favorite toggle result.
    public interface ToggleFavoriteCallback {
        void onComplete(boolean newState);
    }

    // Keep old method and forward to callback version.
    public void toggleFavorite(BreweryEntity brewery) {
        toggleFavorite(brewery, null);
    }

    public void toggleFavorite(BreweryEntity brewery, ToggleFavoriteCallback callback) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "toggleFavorite called for: " + brewery.getName() + " (ID: " + brewery.getId() + ")");

                // Make sure brewery exists locally before toggle.
                BreweryEntity existingBrewery = breweryDao.getAleByIdSync(brewery.getId());

                boolean resultingState = false;

                if (existingBrewery == null) {
                    // First time seen in local DB.
                    Log.d(TAG, "Brewery not in DB, inserting first...");
                    brewery.setFavorite(true);
                    breweryDao.insert(brewery);
                    Log.d(TAG, "Inserted brewery with favorite=true");
                    resultingState = true;
                } else {
                    // Flip favorite state.
                    Log.d(TAG, "Brewery exists in DB with favorite=" + existingBrewery.isFavorite());
                    existingBrewery.setFavorite(!existingBrewery.isFavorite());
                    breweryDao.update(existingBrewery);
                    Log.d(TAG, "Updated brewery favorite=" + existingBrewery.isFavorite());
                    resultingState = existingBrewery.isFavorite();
                }

                // Re-read to confirm final state.
                BreweryEntity verifyBrewery = breweryDao.getAleByIdSync(brewery.getId());
                if (verifyBrewery != null) {
                    Log.d(TAG, "Verification - favorite state: " + verifyBrewery.isFavorite());
                    resultingState = verifyBrewery.isFavorite();
                }

                // Sync favorite change for logged-in users.
                String userId = appwriteService.getSavedUserId();
                if (userId != null && !userId.startsWith("guest_")) {
                    if (resultingState) {
                        // Added to favorites.
                        appwriteService.syncFavorite(userId, brewery, new AppwriteService.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Favorite synced to Appwrite for: " + brewery.getName());
                            }

                            @Override
                            public void onError(String message) {
                                Log.e(TAG, "Failed to sync favorite to Appwrite: " + message);
                            }
                        });
                    } else {
                        // Removed locally; cloud delete can be added later.
                        Log.d(TAG, "Favorite removed locally for: " + brewery.getName() + " (Appwrite delete not yet implemented)");
                    }
                }

                if (callback != null) {
                    callback.onComplete(resultingState);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error toggling favorite: " + e.getMessage(), e);
                if (callback != null) callback.onComplete(brewery.isFavorite());
            }
        });
    }

    // Reads one brewery by id.
    public LiveData<BreweryEntity> getBreweryById(String id) {
        return breweryDao.getAleById(id);
    }
}
