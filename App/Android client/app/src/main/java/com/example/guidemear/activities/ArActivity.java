package com.example.guidemear.activities;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.example.guidemear.Painting;
import com.example.guidemear.R;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.Locale;

public class ArActivity extends AppCompatActivity {
    private static final String TAG = "ARActivity";
    private ArFragment arFragment;
    private TextToSpeech tts;

    /**
     * Creates a reference to the ArFragment component in the layout and sets
     * an OnTapArPlaneListener for it
     * Finally, it etrieves the Painting object from the previous activity and starts the TTS process
     *
     * @param savedInstanceState object containing the activity's previously saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        Intent intent;
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        if(arFragment != null) {
            arFragment.setOnTapArPlaneListener(
                    (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                        Anchor anchor = hitResult.createAnchor();
                        placeObject(arFragment, anchor, Uri.parse("statue_head.sfb"));
                    }
            );

            intent = getIntent();
            Painting painting = (Painting) intent.getSerializableExtra("painting");
            allocateTTS();
        }
        else {
            Toast.makeText(getApplicationContext(), "Could not start up the AR environment", Toast.LENGTH_SHORT).show();
            intent = new Intent(getApplicationContext(), ImageSelectionActivity.class);
            startActivity(intent);
        }
    }



    /**
     * Pla
     *
     * @param arFragment
     * @param anchor
     * @param uri uri of the object to be placed
     */
    private void placeObject(ArFragment arFragment, Anchor anchor, Uri uri) {
        ModelRenderable.builder()
                .setSource(arFragment.getContext(), uri)
                .build()
                .thenAccept(modelRenderable -> addNodeToScene(arFragment, anchor, modelRenderable))
                .exceptionally(throwable -> {
                    Toast.makeText(arFragment.getContext(), "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    return null;
                });
    }


    /**
     *
     *
     * @param arFragment
     * @param anchor
     * @param renderable
     */
    private void addNodeToScene(ArFragment arFragment, Anchor anchor, Renderable renderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
    }


    /**
     *
     */
    private void allocateTTS() {
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);

                    if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.d(TAG, "TTS initialization failed! Language not supported");
                    } else {
                        tts.speak("et up connection to ComponentInfo{com.samsung.SMT/com.samsung.SMT.SamsungTTSService}", TextToSpeech.QUEUE_ADD, null, null);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Error initializing Text-To-Speech service", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    /**
     * Stops the TTS engine
     */
    @Override
    protected void onDestroy() {
        if(tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
