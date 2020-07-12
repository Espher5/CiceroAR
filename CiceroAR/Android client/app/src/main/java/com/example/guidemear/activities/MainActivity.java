package com.example.guidemear.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.example.guidemear.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Activity providing the home screen for the application;
 * it contains a button to launch the
 * and a button to access the user manual
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int CAMERA_REQUEST_CODE = 101;
    private static final int CAMERA_PERMISSION_CODE = 101;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lockOrientation();

        /*
         * Creates a click listener for the start and help button,
         * launching the two respective activities
         */
        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(v -> {
            askCameraPermission();
        });


        Button helpButton = findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {

        });


        Button demoButton = findViewById(R.id.ar_demo_button);
        demoButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ArActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Locks the device orientation to vertical
     */
    protected void lockOrientation() {

    }


    /**
     *
     */
    private void askCameraPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            dispatchTakePictureIntent();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "Request code: " + requestCode);
        if(requestCode == CAMERA_REQUEST_CODE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            }
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Launches the camera
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File imageFile = null;
            try {
                imageFile = createImageFile();
            } catch(IOException e) {
                Log.e(TAG, "Error creating the image file: " + e);
            }

            if(imageFile != null) {
                Uri imageUri = FileProvider.getUriForFile(this, "com.example.guidemear.fileprovider", imageFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }


    /**
     * Creates a new file for the captured image
     */
    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat")
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    /**
     * Method called after selecting an image to process
     *
     * @param requestCode the camera request code
     * @param resultCode the camera result code
     * @param data the data contained in the intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(getApplicationContext(), ImagePreviewActivity.class);
            intent.putExtra("image_path", currentPhotoPath);
            startActivity(intent);
        }
    }
}
