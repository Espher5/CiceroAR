package com.example.guidemear;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.guidemear.activities.ImageSelectionActivity;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;


import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;


public class ARActivity2 extends AppCompatActivity {
    private static final String TAG = "ARActivity";
    private boolean placeGuideModel = true;

    private ArFragment arFragment;
    private AugmentedImage augmentedImage;
    private TransformableNode guideNode;
    private TransformableNode imageNode;
    private TextToSpeech tts;


    /**
     *
     */
    final Handler onTtsStartHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message message) {
            Log.d(TAG, "onTtsStartHandler. Message: " + message);

            //Places the image and animates it to move it forwards
            placeImageView(arFragment, augmentedImage.createAnchor(augmentedImage.getCenterPose()), null);
        }
    };


    /**
     *
     */
    final Handler onTtsDoneHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message message) {
            Log.d(TAG, "onTtsDoneHandler. Message: " + message);

            //
            imageNode.setParent(null);
            imageNode = null;
        }
    };


    /**
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_a_r2);

        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment_2);
        if(arFragment != null) {
            arFragment.getPlaneDiscoveryController().hide();
            arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
        } else {
            Toast.makeText(getApplicationContext(), "Could not start up the AR environment", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), ImageSelectionActivity.class);
            startActivity(intent);
        }
    }


    /**
     *
     * @param frameTime a
     */
    private void onUpdateFrame(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        if(frame == null) {
            return;
        }

        Pose cameraPose = frame.getCamera().getPose();
        /*
         * First-time guide setup
         */
        if(placeGuideModel) {
            Collection<AugmentedImage> augmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);
            augmentedImages.forEach(augmentedImage -> {
                if(augmentedImage.getTrackingState() == TrackingState.TRACKING &&
                        augmentedImage.getName().equals("Venere")) {

                    this.augmentedImage = augmentedImage;
                    /*
                    Pose pose = cameraPose.compose(Pose.makeTranslation(1, 0, -0.3f));
                    Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(pose);
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());
                    placeGuideModel(arFragment, anchor, Uri.parse("female_head.sfb"));
                    placeGuideModel = false;
                    initTTS();
                    */
                    initTTS();
                    placeGuideModel = false;
                }
            });
        }


        /*
         * Updates the guide model and the detail image to face camera
         */
        if(guideNode != null) {
            Vector3 cameraPosition = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
            Vector3 guideModelPosition = guideNode.getWorldPosition();
            Vector3 guideDirection = Vector3.subtract(cameraPosition, guideModelPosition);
            Quaternion guideLookRotation = Quaternion.lookRotation(guideDirection, Vector3.up());
            guideNode.setWorldRotation(guideLookRotation);

            if(imageNode != null) {
                Vector3 imagePosition = imageNode.getWorldPosition();
                Vector3 imageDirection = Vector3.subtract(cameraPosition, imagePosition);
                Quaternion imageLookRotation = Quaternion.lookRotation(imageDirection, Vector3.up());
                imageNode.setWorldRotation(imageLookRotation);
            }
        }
    }


    /**
     *
     * @param arFragment
     * @param anchor
     * @param uri
     */
    private void placeGuideModel(ArFragment arFragment, Anchor anchor, Uri uri) {
        ModelRenderable.builder()
                .setSource(arFragment.getContext(), uri)
                .build()
                .thenAccept(modelRenderable -> {
                    //addNodeToScene(arFragment, anchor, modelRenderable, guideNode);
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    guideNode = new TransformableNode(arFragment.getTransformationSystem());
                    guideNode.setRenderable(modelRenderable);
                    guideNode.setParent(anchorNode);
                    arFragment.getArSceneView().getScene().addChild(anchorNode);
                    guideNode.select();
                })
                .exceptionally(throwable -> {
                    Toast.makeText(getApplicationContext(), "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    return null;
                });
    }


    /**
     *
     * @param arFragment
     */
    private void placeImageView(ArFragment arFragment, Anchor anchor, Uri uri) {
        Log.d(TAG, "placeImageView called");
        ViewRenderable.builder()
                .setView(arFragment.getContext(), R.layout.ar_layout)
                .build()
                .thenAccept(viewRenderable -> {
                    ImageView imageView = (ImageView) viewRenderable.getView();
                    imageView.setImageResource(R.drawable.background_2);
                    //addNodeToScene(arFragment, anchor, viewRenderable, imageNode);
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    imageNode = new TransformableNode(arFragment.getTransformationSystem());
                    imageNode.setRenderable(viewRenderable);
                    imageNode.setParent(anchorNode);
                    arFragment.getArSceneView().getScene().addChild(anchorNode);
                    imageNode.select();
                })
                .exceptionally(throwable -> {
                    Toast.makeText(getApplicationContext(), "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    return null;
                });
    }


    /**
     *
     * @param arFragment a
     * @param anchor a
     * @param renderable a
     */
    private void addNodeToScene(ArFragment arFragment, Anchor anchor, Renderable renderable, TransformableNode node) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        node = new TransformableNode(arFragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
    }


    /**
     *
     * @param config a
     * @param session a
     * @return a boolean value indicating whether the creation was successful or not
     */
    public boolean setupAugmentedImagesDb(Config config, Session session) {
        Bitmap augmentedImageBitmap = loadAugmentedImage();
        if(augmentedImageBitmap == null) {
            return false;
        }

        AugmentedImageDatabase imageDatabase = new AugmentedImageDatabase(session);
        imageDatabase.addImage("Venere", augmentedImageBitmap);

        config.setAugmentedImageDatabase(imageDatabase);
        return true;
    }


    /**
     *
     * @return the created Bitmap
     */
    private Bitmap loadAugmentedImage() {
        try(InputStream inputStream = getAssets().open("La nascita di Venere.jpg")) {
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            Log.e("ImageLoad", "IOException", e);
        }
        return null;
    }


    /**
     * Allocates the resources for the TTS engine
     */
    private void initTTS() {
        tts = new TextToSpeech(getApplicationContext(), status -> {
            if(status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.d(TAG, "TTS initialization failed: the language is not supported or the data is missing");
                } else {
                    startTTS();
                }
            } else {
                Toast.makeText(getApplicationContext(), "Error initializing the Text-To-Speech engine", Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     *
     */
    private void welcomeTTS() {
        String welcomeMessage = "Hello, welcome! I'm here, to your left";
        tts.speak(welcomeMessage, TextToSpeech.QUEUE_ADD, null, null);
    }


    /**
     * Iterates through the image-string pairs making up the painting info
     * and feeds them to the TTS engine
     */
    private void startTTS() {
        /*
         * Example painting setup
         * ------------------------------
         */
        Map<String, String> paintingInfo = new HashMap<>();
        paintingInfo.put("1", "The main focus of the composition is the goddess of love and beauty, Venus, " +
                "born by the sea spray and blown on the island of Cyprus by the winds, Zephyr and, perhaps, Aura. " +
                "She is met by a young woman, sometimes identified as the Hora of Spring, who holds a cloak covered in flowers, " +
                "ready to cover her. A detail often overlooked is the lack of shadows in the scene; " +
                "according to some interpretations, the … is set in an alternative reality, still very similar to our own.");
        paintingInfo.put("2", "The goddess is standing on a giant scallop shell, as pure and perfect as a pearl. " +
                "She covers her nakedness with long, blond hair, which has reflections of light from the fact it has been gilded. " +
                "The fine modelling and white flesh colour gives her the appearance of a statue, an impression fortified by her stance, " +
                "which is very similar to the Venus Pudica, an ancient statue of the greek-roman period.");
        paintingInfo.put("3", "You may wonder why Venus is standing on a shell; the story goes that the God Uranus had a son named Chronus, " +
                "who overthrew his father and threw his genitals into the sea; this caused the water to be fertilised, " +
                "and thus the goddess was born.");
        paintingInfo.put("4", "In the top left of the piece we can notice Zephyrus, god of the winds; " +
                "he is  holding Aura, personification of a light breeze. The two are highlighting the pale face of the goddess, " +
                "while blowing the shell towards the coast.");
        paintingInfo.put("5", "The Hora herself may be a complementary version of the nymph Chloris. " +
                "Are they two versions of the same person then? It might be; the story of this woman is narrated in " +
                "“I Fasti” by latin author Ovidio and the painted in “The Spring”, by Botticelli himself, " +
                "where the woman gets kidnapped by Zephyrus to become a mystical figure. The theory is quite farfetched, " +
                "however there’s a detail in its favour: the roses falling around her and Zephyrus.");

        //Painting testPainting = new Painting("Sandro Botticelli", "La nascita di Venere", paintingInfo);
        /*
         * ------------------------------
         */

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Log.d(TAG, "UtteranceProgressListener.onStart called");
                Message ttsMessage = onTtsStartHandler.obtainMessage();
                ttsMessage.sendToTarget();
            }

            @Override
            public void onDone(String utteranceId) {
                Log.d(TAG, "UtteranceProgressListener.onDone called");
                imageNode = null;
            }

            @Override
            public void onError(String utteranceId) {

            }
        });


        //tts.speak("The Hora herself may be a complementary version of the nymph Chloris. Are they two versions of the same person then?", TextToSpeech.QUEUE_ADD, null, "id");
        for(Entry<String, String> entry: paintingInfo.entrySet()) {
            String imagePath = entry.getKey();
            String info = entry.getValue();

            String utteranceID = "ID" + imagePath;
            tts.speak(info, TextToSpeech.QUEUE_ADD, null, utteranceID);
        }
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
