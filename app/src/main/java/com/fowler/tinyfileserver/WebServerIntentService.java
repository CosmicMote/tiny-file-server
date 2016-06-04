package com.fowler.tinyfileserver;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.IOException;

public class WebServerIntentService extends IntentService {

    private static final String TAG = WebServerIntentService.class.getSimpleName();

    public WebServerIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean start = intent.getBooleanExtra(Constants.START, true);
        ResultReceiver resultReceiver = intent.getParcelableExtra(Constants.RECEIVER);
        try {
            if(start) {
                HttpServer.getHttpServer().start(this);
            } else {
                HttpServer.getHttpServer().stop();
                // Clean up any temp files that may have been created
                TempFileManager.getTempFileManager().clear();
            }

            resultReceiver.send(Constants.SUCCESS, new Bundle());
            Log.i(TAG, String.format("%s http server", start ? "Started" : "Stopped"));
        } catch (IOException e) {
            resultReceiver.send(Constants.FAILURE, new Bundle());
            Log.e(TAG, String.format("Failed to %s http server", start ? "start" : "stop"), e);
        }
    }
}
