package com.example.guidemear.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.example.guidemear.ARActivity2;
import com.example.guidemear.R;


/**
 * Activity providing the home screen for the application;
 * it contains a button to launch the
 * and a button to access the user manual
 */
public class MainActivity extends AppCompatActivity {

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
            Intent intent = new Intent(getApplicationContext(), ImageSelectionActivity.class);
            startActivity(intent);
        });


        Button helpButton = findViewById(R.id.help_button);
        helpButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ARActivity2.class);
            startActivity(intent);
        });
    }

    /**
     * Locks the device orientation to vertical
     */
    protected void lockOrientation() {

    }
}
