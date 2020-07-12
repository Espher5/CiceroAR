package com.example.ciceroar.network;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/*
 * Singleton class containing the logic to download the images via asynchronous operations
 */
public class SingletonAsyncDownloadTask extends AsyncTask<String, Void, Void> {
    private final String TAG = "SingletonAsyncDownloadTask";
    private static SingletonAsyncDownloadTask instance = null;
    private static Bitmap[] downloadResult;

    public SingletonAsyncDownloadTask getInstance() {
        if(instance == null) {
            instance = new SingletonAsyncDownloadTask();
        } else {
            Log.e(TAG, "Cannot instantiate more object of this class");
        }
        return instance;
    }

    public static Bitmap[] getDownloadResult() {
        if(instance != null) {
            return downloadResult;
        }
        return null;
    }

    @Override
    protected Void doInBackground(String... strings) {
        if(instance == null) {
            return null;
        }
        try {
            downloadResult = new Bitmap[strings.length];
            for(int i = 0; i < strings.length; i++) {
                if(strings[i] == null) {
                    downloadResult[i] = null;
                   continue;
                }
                URL url = new URL(strings[i]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream inputStream = connection.getInputStream();
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                downloadResult[i] = BitmapFactory.decodeStream(bufferedInputStream);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error while retrieving the image: " + e);
        }
        return null;
    }
}