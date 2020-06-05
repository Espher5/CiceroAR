package com.example.guidemear.activities;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.guidemear.Painting;
import com.example.guidemear.R;
import com.example.guidemear.network.UploadAPI;
import com.example.guidemear.network.UploadHandler;

import java.io.File;

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

        Button uploadButton = findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(v -> uploadImage(imagePath));

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

        Retrofit retrofit = UploadHandler.getRetrofit();
        UploadAPI uploadAPI = retrofit.create(UploadAPI.class);
        Call<Painting> call = uploadAPI.uploadImage(part);
        call.enqueue(new Callback<Painting>() {
            @Override
            public void onResponse(@NonNull Call<Painting> call, @NonNull Response<Painting> response) {
                if(response.body() == null) {
                    Log.e(TAG, "Response error: no body in response");
                }
                else {
                    Painting painting = new Painting(
                            response.body().getArtist(),
                            response.body().getTitle(),
                            null
                    );
                    Intent intent = new Intent(getApplicationContext(), ArActivity.class);
                    intent.putExtra("painting", painting);
                    startActivity(intent);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Painting> call, @NonNull Throwable t) {
                t.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error! No response from server", Toast.LENGTH_SHORT).show();
            }
        });
    }
}