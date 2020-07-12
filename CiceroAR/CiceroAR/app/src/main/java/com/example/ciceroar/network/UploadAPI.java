package com.example.ciceroar.network;

import com.example.ciceroar.painting.Painting;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface UploadAPI {
    @Multipart
    @POST("upload")
    Call<Painting> uploadImage(@Part MultipartBody.Part part);
}
