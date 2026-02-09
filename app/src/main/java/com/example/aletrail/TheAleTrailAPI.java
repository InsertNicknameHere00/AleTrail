package com.example.aletrail;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TheAleTrailAPI {

    @GET("breweries")
    Call<List<BreweryEntity>> getBreweries();

    @GET("breweries")
    Call<List<BreweryEntity>> getBreweriesByPage(@Query("page") int page, @Query("per_page") int perPage);

    @GET("breweries/{id}")
    Call<BreweryEntity> getBreweryById(@Path("id") String id);

    @GET("breweries/search")
    Call<List<BreweryEntity>> searchBreweries(@Query("query") String query);

    @GET("breweries")
    Call<List<BreweryEntity>> getBreweriesByCity(@Query("by_city") String city, @Query("per_page") int perPage);

    @GET("breweries")
    Call<List<BreweryEntity>> getBreweriesByLocation(
            @Query("by_dist") String location,
            @Query("per_page") int perPage
    );

    @GET("breweries")
    Call<List<BreweryEntity>> filterBreweries(
            @Query("by_city") String city,
            @Query("by_state") String state,
            @Query("by_name") String name,
            @Query("by_type") String type,
            @Query("per_page") Integer perPage
    );
}
