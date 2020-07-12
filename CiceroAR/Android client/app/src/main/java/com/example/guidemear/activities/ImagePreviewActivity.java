package com.example.guidemear.activities;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.guidemear.network.SingletonAsyncDownloadTask;
import com.example.guidemear.painting.Painting;
import com.example.guidemear.R;
import com.example.guidemear.network.UploadAPI;
import com.example.guidemear.network.UploadHandler;
import com.example.guidemear.painting.PaintingDetail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


/**
 * Activity responsible for displaying the captured image and allowing the user to
 * upload it or cancel the operation
 */
public class ImagePreviewActivity extends AppCompatActivity {
    private static final String TAG = "ImagePreviewActivity";
    private ProgressBar progressBar;
    private Bitmap[] downloadResult;

    /**
     * Retrieves the path of the selected image from the intent,
     * creates a preview to display in the UI and
     * adds the listeners for the upload and cancel buttons
     *
     * @param savedInstanceState the previous activity state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        Intent intent = getIntent();
        final String imagePath = intent.getStringExtra("image_path");

        if(imagePath == null) {
            Log.e(TAG, "Error retrieving the image file");
        } else {
            File file = new File(imagePath);
            ImageView imagePreview = findViewById(R.id.image_preview);
            imagePreview.setImageURI(Uri.fromFile(file));
        }

        progressBar = findViewById(R.id.progressBar_cyclic);
        progressBar.setVisibility(View.GONE);

        Button uploadButton = findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            uploadImage(imagePath);
        });

        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> {
            Intent intent1 = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent1);
        });
    }


    /**
     * Creates a multipart request and uses an UploadHandler object to upload it to the server
     *
     * @param imagePath the image's path in the file system
     */
    private void uploadImage(String imagePath) {
        File file = new File(imagePath);

        RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData("image", file.getName(), requestBody);

        Toast.makeText(getApplicationContext(), "Uploading image...", Toast.LENGTH_LONG).show();

        Retrofit retrofit = UploadHandler.getRetrofit();
        UploadAPI uploadAPI = retrofit.create(UploadAPI.class);
        Call<Painting> call = uploadAPI.uploadImage(part);
        call.enqueue(new Callback<Painting>() {
            @Override
            public void onResponse(@NonNull Call<Painting> call, @NonNull Response<Painting> response) {
                progressBar.setVisibility(View.GONE);
                if(response.body() == null) {
                    Log.e(TAG, "Response error: no body in response");
                }
                else {
                    Toast.makeText(getApplicationContext(), "Image successfully uploaded", Toast.LENGTH_SHORT).show();
                    List<PaintingDetail> paintingDetails = response.body().getPaintingDetails();

                    List<String> urls = new ArrayList<>();
                    List<String> descriptions = new ArrayList<>();
                    paintingDetails.forEach(entry -> {
                        urls.add(entry.getImagePath());
                        descriptions.add(entry.getDescription());
                    });
                    new SingletonAsyncDownloadTask().getInstance().execute(urls.toArray(new String[0]));

                    Intent intent = new Intent(getApplicationContext(), ArActivity.class);
                    intent.putExtra("artist", response.body().getArtist());
                    intent.putExtra("title", response.body().getTitle());
                    intent.putExtra("descriptions", descriptions.toArray(new String[0]));
                    startActivity(intent);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Painting> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                t.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error! No response from server", Toast.LENGTH_SHORT).show();
            }
        });
    }
}