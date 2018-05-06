/***
 Copyright (c) 2015 CommonsWare, LLC
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 by applicable law or agreed to in writing, software distributed under the
 License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 OF ANY KIND, either express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 Covered in detail in the book _The Busy Coder's Guide to Android Development_
 https://commonsware.com/Android
 */

package com.kru13.httpserver.service;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.WindowManager;

import com.kru13.httpserver.BuildConfig;
import com.kru13.httpserver.util.ImageTransmogrifier;

import java.io.File;
import java.io.FileOutputStream;


public class ScreenshotService extends Service {

    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_RESULT_INTENT = "resultIntent";
    public static final String ACTION_RECORD = BuildConfig.APPLICATION_ID + ".RECORD";
    public static final String SCREEN_FILE_NAME = "screenshot.png";

    static final String ACTION_SHUTDOWN = BuildConfig.APPLICATION_ID + ".SHUTDOWN";
    static final int VIRT_DISPLAY_FLAGS =
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;

    private MediaProjection projection;
    private VirtualDisplay vdisplay;
    private MediaProjectionManager mgr;
    private WindowManager wmgr;
    private ImageTransmogrifier it;
    private int resultCode;
    private Intent resultData;
    final private ToneGenerator beeper = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);


    @Override
    public void onCreate() {
        super.onCreate();

        mgr = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        wmgr = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        resultCode = i.getIntExtra(EXTRA_RESULT_CODE, 1337);
        resultData = i.getParcelableExtra(EXTRA_RESULT_INTENT);

        if (ACTION_RECORD.equals(i.getAction())) {
            if (resultData != null) {
                startCapture();
            }
        }

        if (ACTION_SHUTDOWN.equals(i.getAction())) {
            beeper.startTone(ToneGenerator.TONE_PROP_NACK);
            stopForeground(true);
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopCapture();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new IllegalStateException("Binding not supported. Go away.");
    }

    public WindowManager getWindowManager() {
        return wmgr;
    }

    public void processImage(final byte[] png) {
        new Thread() {
            @Override
            public void run() {
                File output = new File(getExternalFilesDir(null), SCREEN_FILE_NAME);
                Log.d("ScreenshotService", "Screen saved to " + output.getPath());
                try {
                    FileOutputStream fos = new FileOutputStream(output);

                    fos.write(png);
                    fos.flush();
                    fos.getFD().sync();
                    fos.close();

                } catch (Exception e) {
                    Log.e("ScreenshotService", "Exception writing out screenshot", e);
                }
            }
        }.start();

        beeper.startTone(ToneGenerator.TONE_PROP_ACK);
        stopCapture();
    }

    private void stopCapture() {
        if (projection != null) {
            projection.stop();
            vdisplay.release();
            projection = null;
        }
    }

    private void startCapture() {
        projection = mgr.getMediaProjection(resultCode, resultData);
        it = new ImageTransmogrifier(this);

        vdisplay = projection.createVirtualDisplay("andshooter",
                it.getWidth(),
                it.getHeight(),
                getResources().getDisplayMetrics().densityDpi,
                VIRT_DISPLAY_FLAGS,
                it.getSurface(),
                null,
                null);
    }
}
