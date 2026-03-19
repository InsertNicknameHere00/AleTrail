package com.example.aletrail;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import com.example.aletrail.CartesModels.MapListResponse;
import com.example.aletrail.CartesModels.MapResponse;
import com.example.aletrail.CartesModels.CreateMapRequest;
import com.example.aletrail.CartesModels.UpdateMapRequest;
import com.example.aletrail.CartesModels.MarkerListResponse;
import com.example.aletrail.CartesModels.MarkerResponse;
import com.example.aletrail.CartesModels.CreateMarkerRequest;
import okhttp3.ResponseBody;


public interface TheCartesAPI {

    // Map operations
    @GET("maps")
    Call<MapListResponse> getMaps();

    @GET("maps/{token}")
    Call<MapResponse> getMap(@Path("token") String token);

    @POST("maps")
    Call<ResponseBody> createMap(@Body CreateMapRequest mapRequest);

    @PATCH("maps/{token}")
    Call<MapResponse> updateMap(@Path("token") String token, @Body UpdateMapRequest mapRequest);

    // Marker operations
    @GET("maps/{token}/markers")
    Call<MarkerListResponse> getMapMarkers(@Path("token") String token);

    @POST("maps/{token}/markers")
    Call<ResponseBody> createMarker(@Path("token") String token, @Body CreateMarkerRequest markerRequest);

    @DELETE("maps/{token}/markers/{id}")
    Call<Void> deleteMarker(@Path("token") String token, @Path("id") String id);
}
