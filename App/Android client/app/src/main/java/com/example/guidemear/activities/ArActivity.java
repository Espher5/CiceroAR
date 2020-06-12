package com.example.guidemear.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.guidemear.CustomArFragment;
import com.example.guidemear.painting.Painting;
import com.example.guidemear.painting.PaintingDetail;
import com.example.guidemear.R;
import com.example.guidemear.texttospeech.CustomUtteranceProgressListener;
import com.example.guidemear.texttospeech.TextToSpeechManager;
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
import com.google.ar.sceneform.math.Vector3Evaluator;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class ArActivity extends AppCompatActivity {
    private static final String TAG = "ARActivity";
    private boolean placeGuideFlag = true;
    private boolean playingFlag = false;
    private Painting painting;
    private int narrationIndex = 0;

    private ImageButton playButton;
    private Button nextButton;
    private Button previousButton;

    private ArFragment arFragment;
    private AugmentedImage augmentedImage;
    private TransformableNode guideNode;
    private TransformableNode imageNode;
    private TextToSpeechManager ttsManager;


    /**
     * Handler responsible to manage the message sent from the TTS thread when an utterance is started
     */
    private final Handler onTtsStartHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message message) {
            Log.d(TAG, "onTtsStartHandler. Message: " + message);
            String imagePath = message.getData().getString("IMAGE_PATH");


            try {
                /*
                ** Places the image in the scene and interpolates it between
                ** the position in which it has been generated and a forward point
                 */

                Pose imagePose = augmentedImage.getCenterPose();
                Pose targetPose = imagePose.compose(Pose.makeTranslation(0, 0, 0));
                Anchor anchor = arFragment
                        .getArSceneView()
                        .getSession()
                        .createAnchor(targetPose);
                InputStream in = getAssets().open(imagePath + ".jpg");
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                placeImageView(arFragment, anchor, bitmap);

                if(imageNode != null) {
                    AnchorNode targetAnchor = new AnchorNode(arFragment
                            .getArSceneView()
                            .getSession()
                            .createAnchor(imagePose.compose(Pose.makeTranslation(1, 1, 1))));

                    ObjectAnimator objectAnimator = new ObjectAnimator();
                    objectAnimator.setAutoCancel(true);
                    objectAnimator.setTarget(imageNode);

                    objectAnimator.setObjectValues(
                            imageNode.getWorldPosition(),
                            targetAnchor.getWorldPosition()
                    );
                    objectAnimator.setPropertyName("worldPosition");
                    objectAnimator.setEvaluator(new Vector3Evaluator());
                    objectAnimator.setInterpolator(new LinearInterpolator());
                    objectAnimator.setDuration(2000);
                    objectAnimator.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    /**
     * Handler responsible to manage the message sent from the TTS thread when an utterance is completed
     */
    private final Handler onTtsDoneHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message message) {
            Log.d(TAG, "onTtsDoneHandler. Message: " + message);

            /*
            ** Deletes the image node from the scene
             */
            if(imageNode != null) {
                arFragment.getArSceneView().getScene().removeChild(imageNode);
                imageNode.setParent(null);
                imageNode = null;
            }

            if(playingFlag) {
                ttsManager.stop();
                narrationIndex++;
                if(narrationIndex >= painting.getPaintingInfo().size()) {
                    narrationIndex = painting.getPaintingInfo().size() - 1;
                }
                PaintingDetail entry = painting.getPaintingInfo().get(narrationIndex);
                ttsManager.speak(entry.getDescription(), entry.getImagePath());
            }
            else {
                Log.d(TAG, "Cannot create ImageView, imageNode is null");
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

        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment_2);
        assert arFragment != null;
        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);

        ttsManager = new TextToSpeechManager();
        CustomUtteranceProgressListener progressListener = new CustomUtteranceProgressListener(onTtsStartHandler, onTtsDoneHandler);
        ttsManager.initTts(getApplicationContext(), progressListener);

        painting = getPainting();
        List<PaintingDetail> paintingInfo = painting.getPaintingInfo();

        /*
        ** UI buttons listeners
        */
        playButton = findViewById(R.id.play_pause_button);
        playButton.setVisibility(View.GONE);
        playButton.setOnClickListener(v -> {
            if (!playingFlag) {
                /*
                ** TTS is currently paused
                ** Changes the ImageButton icon to the pause icon and resumes the narration
                 */
                Drawable icon = getDrawable(android.R.drawable.ic_media_pause);
                playButton.setImageDrawable(icon);
                playingFlag = true;

                PaintingDetail entry = paintingInfo.get(narrationIndex);
                ttsManager.speak(entry.getDescription(), entry.getImagePath());
            } else {
                /*
                ** TTS is currently playing
                ** Changes the ImageButton icon to the play icon and stops the narration
                 */
                Drawable icon = getDrawable(android.R.drawable.ic_media_play);
                playButton.setImageDrawable(icon);
                playingFlag = false;
                ttsManager.stop();

            }
        });

        nextButton = findViewById(R.id.next_button);
        nextButton.setVisibility(View.GONE);
        nextButton.setOnClickListener(v -> {
            if(playingFlag) {
                ttsManager.stop();
                narrationIndex++;
                if(narrationIndex >= paintingInfo.size()) {
                    narrationIndex = paintingInfo.size() - 1;
                }
                PaintingDetail entry = paintingInfo.get(narrationIndex);
                ttsManager.speak(entry.getDescription(), entry.getImagePath());
            }
        });

        previousButton = findViewById(R.id.previous_button);
        previousButton.setVisibility(View.GONE);
        previousButton.setOnClickListener(v -> {
            if(playingFlag) {
                ttsManager.stop();
                narrationIndex = (narrationIndex > 0 ? narrationIndex - 1 : 0);
                PaintingDetail entry = paintingInfo.get(narrationIndex);
                ttsManager.speak(entry.getDescription(), entry.getImagePath());
            }
        });
    }


    /**
     * Method called on every frame update  on the scene. It initially liiks for the image in the scene
     * and when this is identified it proceeds to create the guide model and start the TTS narration
     * Finally it updates the rotation of the objects in the scene to face the camera in every instant
     *
     * @param frameTime the object containing the information for the current frame
     */
    private void onUpdateFrame(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        if(frame == null) {
            return;
        }

        Pose cameraPose = frame.getCamera().getPose();
        /*
        ** First-time guide setup
         */
        if(placeGuideFlag) {
            Collection<AugmentedImage> augmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);
            augmentedImages.forEach(augmentedImage -> {
                if(augmentedImage.getTrackingState() == TrackingState.TRACKING &&
                        augmentedImage.getName().equals("Venere")) {
                    this.augmentedImage = augmentedImage;

                    Pose pose = cameraPose.compose(Pose.makeTranslation(2, 0, -0.3f));
                    Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(pose);
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());
                    placeGuideModel(arFragment, anchor, Uri.parse("female_head.sfb"));
                    placeGuideFlag = false;
                    enableUI();
                }
            });
        }

        /*
        ** Updates the guide model and the detail image to face the camera
        if(guideNode != null) {
            Vector3 cameraPosition = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
            Vector3 guideModelPosition = guideNode.getWorldPosition();
            Vector3 guideDirection = Vector3.subtract(cameraPosition, guideModelPosition);
            Quaternion guideLookRotation = Quaternion.lookRotation(guideDirection, Vector3.up());
            guideNode.setWorldRotation(guideLookRotation);
         }
        */
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
        ModelRenderable.builder()
                .setSource(arFragment.getContext(), uri)
                .build()
                .thenAccept(modelRenderable -> {
                    guideNode = addNodeToScene(arFragment, anchor, modelRenderable);
                    guideNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0.0f, 0.0f, 0.0f), 90f));
                })
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
     * @param anchor the anchor used to track the renderable
     * @param bitmap the bitmap containing the image data
     */
    private void placeImageView(ArFragment arFragment, Anchor anchor, Bitmap bitmap) {
        Log.d(TAG, "placeImageView called");
        ViewRenderable.builder()
                .setView(arFragment.getContext(), R.layout.ar_layout)
                .build()
                .thenAccept(viewRenderable -> {
                    ImageView imageView = (ImageView) viewRenderable.getView();
                    imageView.setImageBitmap(bitmap);
                    viewRenderable.setShadowCaster(false);
                    viewRenderable.setShadowReceiver(false);
                    imageNode = addNodeToScene(arFragment, anchor, viewRenderable);
                    imageNode.setLocalRotation(Quaternion.axisAngle(new Vector3(-1f, 0, 0), 90f));
                })
                .exceptionally(throwable -> {
                    Toast.makeText(getApplicationContext(), "Error generating the image view: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    return null;
                });
    }


    /**
     *
     */
    private void destroyImageNode() {
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
    private TransformableNode addNodeToScene(ArFragment arFragment, Anchor anchor, Renderable renderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
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


    /*
    ** Test painting setup
     */
    private Painting getPainting() {
        List<PaintingDetail> paintingInfo = new ArrayList<>();


        paintingInfo.add(new PaintingDetail ("default", "The main focus of the composition is the goddess of love and beauty, Venus. " +
                "Born by the sea spray, she is blown on the island of Cyprus by the winds, Zephyr and Aura. " +
                "She is met by a young woman, sometimes identified as the Hora of Spring, who holds a cloak covered in flowers " +
                "and is ready to cover her. A detail often overlooked is the lack of shadows in the scene; " +
                "according to some interpretations, the painting is set in an alternative reality, still very similar to our own."));
        paintingInfo.add(new PaintingDetail("Venus", "The goddess is standing on a giant scallop shell, as pure and perfect as a pearl. " +
                "She covers her nakedness with long, blond hair, which has reflections of light from the fact it has been gilded. " +
                "The fine modelling and white flesh colour gives her the appearance of a statue, an impression fortified by her stance, " +
                "which is very similar to the Venus Pudica, an ancient statue of the greek-roman period."));
        paintingInfo.add(new PaintingDetail("Shell", "You may wonder why Venus is standing on a shell; the story goes that the God Uranus had a son named Chronus, " +
                "who overthrew his father and threw his genitals into the sea; this caused the water to be fertilised, " +
                "and thus the goddess was born."));
        paintingInfo.add(new PaintingDetail("Zephyrus", "In the top left of the piece we can notice Zephyrus, god of the winds; " +
                "he is  holding Aura, personification of a light breeze. The two are highlighting the pale face of the goddess, " +
                "while blowing the shell towards the coast."));
        paintingInfo.add(new PaintingDetail("Aura", "The Hora herself may be a complementary version of the nymph Chloris. " +
                "Are they two versions of the same person then? It might be; the story of this woman is narrated in " +
                "“I Fasti” by latin author Ovidio and the painted in “The Spring”, by Botticelli himself, " +
                "where the woman gets kidnapped by Zephyrus to become a mystical figure. The theory is quite farfetched, " +
                "however there’s a detail in its favour: the roses falling around her and Zephyrus."));
        return new Painting("Sandro Botticelli", "La nascita di Venere", paintingInfo);
    }
}
