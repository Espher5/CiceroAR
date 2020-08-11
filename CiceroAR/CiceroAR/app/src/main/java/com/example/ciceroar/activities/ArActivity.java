package com.example.guidemear.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.guidemear.CustomArFragment;
import com.example.guidemear.network.SingletonAsyncDownloadTask;
import com.example.guidemear.R;
import com.example.guidemear.texttospeech.CustomUtteranceProgressListener;
import com.example.guidemear.texttospeech.TextToSpeechManager;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.math.Vector3Evaluator;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;


public class ArActivity extends AppCompatActivity {
    private static final String TAG = "Pippo";
    private boolean detectionFlag = true;
    private boolean playingFlag = false;

    private String title;
    private String[] descriptions;
    private Bitmap[] downloadResult;
    private int narrationIndex = 0;

    private ImageButton playButton;
    private Button nextButton;
    private Button previousButton;

    private ArFragment arFragment;
    private AugmentedImage augmentedImage;
    private Node guideNode;
    private Node imageNode;
    private ViewRenderable imageRenderable;
    private ObjectAnimator objectAnimator;
    private TextToSpeechManager ttsManager;

    /**
     * Handler responsible to manage the message sent from the TTS thread when an utterance is started.
     * It places the image in the scene and interpolates it between
     * the position in which it has been generated and a forward point
     */
    private final Handler onTtsStartHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message message) {
            Log.d(TAG, "onTtsStartHandler. Message: " + message);
            if (downloadResult[narrationIndex] != null) {
                placeImageView(arFragment, downloadResult[narrationIndex]);
            }
        }
    };


    /**
     * Handler responsible to manage the message sent from the TTS thread when an utterance is completed
     * It performs a check on the next narration segments and if it is not the last one it initiates it
     */
    private final Handler onTtsDoneHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message message) {
            Log.d(TAG, "onTtsDoneHandler. Message: " + message);

            narrationIndex++;
            if(narrationIndex >=descriptions.length) {
                narrationIndex = 0;
                playingFlag = false;
                Drawable icon = getDrawable(android.R.drawable.ic_media_play);
                playButton.setImageDrawable(icon);
            } else {
                ttsManager.speak(descriptions[narrationIndex]);
            }
        }
    };


    /**
     * Retrieves the Fragment reference for the scene, adds an update listener to it
     * and initiates the TTS engine
     *
     * @param savedInstanceState the previous activity state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        assert arFragment != null;
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
        arFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
            Anchor anchor = hitResult.createAnchor();
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());
            placeGuideModel(arFragment, anchor, Uri.parse("guide.sfb"));
        });

        ttsManager = new TextToSpeechManager();
        CustomUtteranceProgressListener progressListener = new CustomUtteranceProgressListener(onTtsStartHandler, onTtsDoneHandler);
        ttsManager.initTts(getApplicationContext(), progressListener);

        Intent intent = getIntent();
        title = intent.getStringExtra("title");
        descriptions = intent.getStringArrayExtra("descriptions");
        assert descriptions!= null;
        downloadResult = SingletonAsyncDownloadTask.getDownloadResult();
        assert downloadResult != null;


        // UI buttons listeners setup
        playButton = findViewById(R.id.play_pause_button);
        playButton.setVisibility(View.GONE);
        playButton.setOnClickListener(v -> {
            if (!playingFlag) {
                /*
                 * TTS is currently paused
                 * Changes the ImageButton icon to the pause icon and resumes the narration
                 */
                Drawable icon = getDrawable(android.R.drawable.ic_media_pause);
                playButton.setImageDrawable(icon);
                playingFlag = true;

                ttsManager.speak(descriptions[narrationIndex]);
            } else {
                /*
                 * TTS is currently playing
                 * Changes the ImageButton icon to the play icon and stops the narration
                 */
                Drawable icon = getDrawable(android.R.drawable.ic_media_play);
                playButton.setImageDrawable(icon);
                playingFlag = false;
                ttsManager.stop();
                if(objectAnimator != null) {
                    objectAnimator.pause();
                }
            }
        });

        nextButton = findViewById(R.id.next_button);
        nextButton.setVisibility(View.GONE);
        nextButton.setOnClickListener(v -> {
            narrationIndex = (narrationIndex >= descriptions.length - 1 ? descriptions.length - 1 : narrationIndex + 1);
            destroyImageView();
            if(playingFlag) {
                ttsManager.stop();
                ttsManager.speak(descriptions[narrationIndex]);
            }
        });

        previousButton = findViewById(R.id.previous_button);
        previousButton.setVisibility(View.GONE);
        previousButton.setOnClickListener(v -> {
            narrationIndex = (narrationIndex > 0 ? narrationIndex - 1 : 0);
            destroyImageView();
            if(playingFlag) {
                ttsManager.stop();
                ttsManager.speak(descriptions[narrationIndex]);
            }
        });
    }


    /**
     * Method called on every camera frame update. It initially looks for the image in the scene
     * and when this is identified it proceeds to create the guide model and start the TTS narration
     * Finally it updates the rotation of the objects in the scene to face the camera in every instant
     *
     * @param frameTime the object containing the information for the current frame
     */
    private void onUpdateFrame(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        assert frame != null;

        if(detectionFlag) {
            Collection<AugmentedImage> augmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);
            augmentedImages.forEach(augmentedImage -> {
                if(augmentedImage.getTrackingState() == TrackingState.TRACKING &&
                        augmentedImage.getName().equals(title)) {
                    this.augmentedImage = augmentedImage;
                    detectionFlag = false;
                    enableUI();
                }
            });
        }

        // Rotates the image displayed to look at the viewer at all times
        if (imageNode != null) {
            Vector3 cameraPosition = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
            Vector3 imagePosition = imageNode.getWorldPosition();
            Vector3 imageDirection = Vector3.subtract(cameraPosition, imagePosition);
            Quaternion imageLookRotation = Quaternion.lookRotation(imageDirection, Vector3.up());
            imageNode.setWorldRotation(imageLookRotation);
        }
    }



    /**
     * Enables the UI buttons after the image has been recognized
     */
    private void enableUI() {
        playButton.setVisibility(View.VISIBLE);
        playButton.setBackground(null);
        nextButton.setVisibility(View.VISIBLE);
        previousButton.setVisibility(View.VISIBLE);
    }


    /**
     *Builds the model renderable object for the guide model and places it into the scene
     *
     * @param arFragment the fragment
     * @param anchor the anchor used to track the renderable
     * @param uri uri pointing to the 3D model .sfb file
     */
    private void placeGuideModel(ArFragment arFragment, Anchor anchor, Uri uri) {
        if(guideNode != null) {
            arFragment.getArSceneView().getScene().removeChild(guideNode);
            guideNode.setParent(null);
            guideNode = null;
        }
        ModelRenderable.builder()
                .setSource(Objects.requireNonNull(arFragment.getContext()), uri)
                .build()
                .thenAccept(modelRenderable -> guideNode = addNodeToScene(arFragment, anchor, modelRenderable))
                .exceptionally(throwable -> {
                    Toast.makeText(getApplicationContext(), "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    return null;
                });
    }


    /**
     * Builds the view renderable for the scene by using a reference to a standard android ImageView,
     * sets its image to the one received and places it into the scene
     *
     * @param arFragment the fragment
     * @param bitmap the bitmap containing the image data
     */
    private void placeImageView(ArFragment arFragment, Bitmap bitmap) {
        destroyImageView();

        ViewRenderable.builder()
                .setView(arFragment.getContext(), R.layout.ar_layout)
                .build()
                .thenAccept(viewRenderable -> {
                    imageRenderable = viewRenderable;
                    imageRenderable.setShadowCaster(false);
                    imageRenderable.setShadowReceiver(false);
                    ImageView imageView = (ImageView) imageRenderable.getView();
                    imageView.setImageBitmap(bitmap);

                    // Creates a pose in the center of the augmented image, shifts it down slightly,
                    // places an anchor in that point and uses it to add the image node to the scene
                    Pose imagePose = augmentedImage.getCenterPose();
                    imagePose = Pose.makeTranslation(0, -0.2f, 0).compose(imagePose);
                    Anchor startAnchor = Objects.requireNonNull(arFragment
                            .getArSceneView()
                            .getSession())
                            .createAnchor(imagePose);
                    imageNode = addNodeToScene(arFragment, startAnchor, imageRenderable);
                    animateImageNode(imageNode.getWorldPosition());
                })
                .exceptionally(throwable -> {
                    Toast.makeText(getApplicationContext(), "Error generating the image view: " +
                            throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    return null;
                });
    }


    /**
     * Animates a previously generated image node in the scene by moving it forward
     * towards the user camera.
     *
     * @param startPoint vector3 containing the image's initial position
     */
    private void animateImageNode(Vector3 startPoint) {
        Vector3 endPoint = getCameraMiddlePoint(startPoint);

        // Animates the image forward in the environment
        objectAnimator = new ObjectAnimator();
        objectAnimator.setAutoCancel(true);
        objectAnimator.setTarget(imageNode);
        objectAnimator.setObjectValues(startPoint, endPoint);
        objectAnimator.setPropertyName("worldPosition");
        objectAnimator.setEvaluator(new Vector3Evaluator());
        objectAnimator.setInterpolator(new LinearInterpolator());
        objectAnimator.setDuration(20000);
        objectAnimator.start();
    }

    /**
     * Calculates the middle point between the image node and the camera
     * and returns it
     *
     * @param startPoint vector3 containing the image's initial position
     * @return the endpoint for the animation transition
     */
    private Vector3 getCameraMiddlePoint(Vector3 startPoint) {
        // Creates a pose in the current camera position and places and anchor in the middle point
        // between the image node and the camera
        Pose cameraPose = Objects.requireNonNull(arFragment.getArSceneView().getArFrame()).getCamera().getPose();
        float distance = (float) Math.sqrt(
                Math.pow((startPoint.x - cameraPose.tx()), 2) +
                Math.pow((startPoint.y - cameraPose.ty()), 2) +
                Math.pow((startPoint.z - cameraPose.tz()), 2)
        ) / 2;
        cameraPose = Pose.makeTranslation(0f, 0f, -distance).compose(cameraPose);
        Anchor endAnchor = Objects.requireNonNull(arFragment
                .getArSceneView()
                .getSession())
                .createAnchor(cameraPose);
        AnchorNode endNode = new AnchorNode(endAnchor);
        endNode.setParent(arFragment.getArSceneView().getScene());
        return endNode.getWorldPosition();
    }


    /**
     * Destroys a previously generated image node, after detaching it from its parent node
     */
    private void destroyImageView() {
        if(imageNode != null) {
            arFragment.getArSceneView().getScene().removeChild(imageNode);
            imageNode.setParent(null);
            imageNode = null;
        }
    }


    /**
     * Creates an anchor node, sets his parent and renderable, and places it into the scene
     *
     * @param arFragment the fragment
     * @param anchor the anchor used to track the renderable
     * @param renderable the renderable to add in the scene
     * @return the created node's reference
     */
    private Node addNodeToScene(ArFragment arFragment, Anchor anchor, Renderable renderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());
        Node node = new Node();
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        return node;
    }


    /**
     * Creates the database containing the images to be recognized in the scene
     *
     * @param config the configuration holding the settings for the current AR session
     * @param session the current AR session
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
     * Loads an image from the assets folder and returns its bitmap
     *
     * @return the loaded image's Bitmap
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
     * Stops the TTS engine
     */
    @Override
    protected void onDestroy() {
        if(ttsManager != null) {
            ttsManager.destroyTts();
        }
        super.onDestroy();
    }
}
