package com.example.ciceroar.texttospeech;

import android.os.Bundle;
import android.os.Message;
import android.speech.tts.UtteranceProgressListener;

import android.os.Handler;
import android.util.Log;

public class CustomUtteranceProgressListener extends UtteranceProgressListener {
    private static final String TAG = "CustomUtteranceProgressListener";
    private Handler onTtsStartHandler;
    private Handler onTtsDoneHandler;

    public CustomUtteranceProgressListener(Handler onTtsStartHandler, Handler onTtsDoneHandler) {
        this.onTtsStartHandler = onTtsStartHandler;
        this.onTtsDoneHandler = onTtsDoneHandler;
    }

    @Override
    public void onStart(String utteranceId) {
        Message ttsStartMessage = onTtsStartHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("UtteranceID: ", utteranceId);
        ttsStartMessage.setData(bundle);
        onTtsStartHandler.sendMessage(ttsStartMessage);
    }

    @Override
    public void onDone(String utteranceId) {
        Message ttsStartMessage = onTtsDoneHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("UtteranceID", utteranceId);
        ttsStartMessage.setData(bundle);
        onTtsDoneHandler.sendMessage(ttsStartMessage);
    }

    @Override
    public void onError(String utteranceId) {
        Log.d(TAG, "Error during the handling of the Text-To-Speech utterance; utteranceID: " + utteranceId);
    }
}
