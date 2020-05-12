package com.example.guidemear;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;


public class ImagePreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        Intent intent = getIntent();
        final String imagePath = intent.getStringExtra("image_path");
        File file = new File(imagePath);
        ImageView imagePreview = findViewById(R.id.image_preview);
        imagePreview.setImageURI(Uri.fromFile(file));

        Button uploadButton = findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //uploadImage(imagePath);

                Intent intent1 = new Intent(getApplicationContext(), ArActivity.class);
                startActivity(intent1);
            }
        });


        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ImageSelectionActivity.class);
                startActivity(intent);
            }
        });
    }



    /**
     *
     */
    private void uploadImage(String sourceFileUri) {
        String uploadServerUri = "http://192.168.43.6:8080/upload";

        HttpURLConnection httpURLConnection;
        DataOutputStream dataOutputStream;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvaliable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1024 * 1024;

        File sourceFile = new File(sourceFileUri);
        if(!sourceFile.isFile()) {
            Toast.makeText(this, "Source file does not exist", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(sourceFile);
            URL url = new URL(uploadServerUri);

            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Connection", "Kepp-Alive");
            httpURLConnection.setRequestProperty("ENCTYPE", "multipart/form-data");
            httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data;");
            httpURLConnection.setRequestProperty("image", sourceFileUri);

            dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
            dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
            dataOutputStream.writeBytes("Content-Disposition: form-data;name=image;filename=" + sourceFileUri + lineEnd);
            dataOutputStream.writeBytes(lineEnd);

            bytesAvaliable = fileInputStream.available();
            bufferSize = Math.min(bytesAvaliable, maxBufferSize);
            buffer = new byte[bufferSize];
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while(bytesRead > 0) {
                dataOutputStream.write(buffer, 0, bufferSize);
                bytesAvaliable = fileInputStream.available();
                bufferSize = Math.min(bytesAvaliable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            dataOutputStream.writeBytes(lineEnd);
            dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            //Server response here

            fileInputStream.close();
            dataOutputStream.flush();
            dataOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     *
     */
    private void redirectResponse() {

    }
}
