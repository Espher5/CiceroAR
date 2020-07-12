package com.example.guidemear.texttospeech;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;


/**
 * Wrapper class for the Text-To-Speech functionalities
 */
public class TextToSpeechManager {
    private static final String TAG = "TextToSpeechManager";
    private TextToSpeech tts;

    public void initTts(Context context, CustomUtteranceProgressListener progressListener) {
        tts = new TextToSpeech(context, status -> {
            if(status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.d(TAG, "TTS initialization failed: the language is not supported or the data is missing");
                } else {
                    tts.setOnUtteranceProgressListener(progressListener);
                }
            } else {
                Log.e(TAG, "Error initializing the Text-To-Speech engine");
            }
        });
    }


    public void speak(String message, String utteranceID) {
        //TTS params setup
        Bundle params = new Bundle();

        tts.speak(message, TextToSpeech.QUEUE_ADD, params, utteranceID);
    }


    public void destroyTts() {
        if(tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
