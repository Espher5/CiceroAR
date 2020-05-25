package com.example.guidemear.network;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Utility class used to handle the image upload.
 * Contains the server url and a method to retrieve
 * the object containing the client information
 */
public class UploadHandler  {
    private static final String SERVER_URL = "http://192.168.43.6:8080/";

    public static Retrofit getRetrofit() {
        OkHttpClient httpClient = new OkHttpClient.Builder().build();

        return new Retrofit.Builder().baseUrl(SERVER_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();
    }
}
