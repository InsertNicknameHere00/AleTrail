package com.example.aletrail;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BREWERY_BASE_URL = "https://api.openbrewerydb.org/v1/";
    private static final String CARTES_BASE_URL = "https://cartes.io/api/";

    private static Retrofit breweryRetrofit = null;
    private static Retrofit cartesRetrofit = null;

    public static Retrofit getBreweryClient() {
        if (breweryRetrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            breweryRetrofit = new Retrofit.Builder()
                    .baseUrl(BREWERY_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return breweryRetrofit;
    }

    public static Retrofit getCartesClient() {
        if (cartesRetrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            cartesRetrofit = new Retrofit.Builder()
                    .baseUrl(CARTES_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return cartesRetrofit;
    }

    public static TheAleTrailAPI getAleTrailAPI() {
        return getBreweryClient().create(TheAleTrailAPI.class);
    }

    public static TheCartesAPI getCartesAPI() {
        return getCartesClient().create(TheCartesAPI.class);
    }
}
