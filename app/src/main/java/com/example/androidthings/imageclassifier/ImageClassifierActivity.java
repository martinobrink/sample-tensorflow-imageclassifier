/*
 * Copyright 2017 The Android Things Samples Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androidthings.imageclassifier;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.androidthings.imageclassifier.classifier.Recognition;
import com.example.androidthings.imageclassifier.classifier.TensorFlowImageClassifier;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ImageClassifierActivity extends Activity implements ImageReader.OnImageAvailableListener {
    private static final String TAG = "ImageClassifierActivity";

    // Matches the images used to train the TensorFlow model
    private static final Size MODEL_IMAGE_SIZE = new Size(224, 224);

    /* Key code used by GPIO button to trigger image capture */
    private static final int SHUTTER_KEYCODE = KeyEvent.KEYCODE_CAMERA;




    private ImagePreprocessor mImagePreprocessor;
    private TextToSpeech mTtsEngine;
    private TtsSpeaker mTtsSpeaker;
    private CameraHandler mCameraHandler;
    private TensorFlowImageClassifier mTensorFlowClassifier;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private ImageView mImage;
    private TextView mResultText;

    private AtomicBoolean mReady = new AtomicBoolean(false);
    private ButtonInputDriver mButtonDriver;
    private Gpio mReadyLED;

    private Timer mTimer = null;
    private MediaPlayer mMediaPlayer = null;
    private PushNotificationSender pushNotificationSender;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);
        mImage = findViewById(R.id.imageView);
        mResultText = findViewById(R.id.resultText);

        pushNotificationSender = new PushNotificationSender(this);

        init();

        startBackgroundThread();
    }

    private void init() {
        if (isAndroidThingsDevice(this)) {
            initPIO();
        }

        mMediaPlayer = MediaPlayer.create(this, R.raw.dog_barking);
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("BackgroundThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mBackgroundHandler.post(mInitializeOnBackground);
    }

    /**
     * This method should only be called when running on an Android Things device.
     */
    private void initPIO() {
        PeripheralManager pioManager = PeripheralManager.getInstance();
        try {
            mReadyLED = pioManager.openGpio(BoardDefaults.getGPIOForLED());
            mReadyLED.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mButtonDriver = new ButtonInputDriver(
                    GpioHelper.BUTTON_A,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_A);
            mButtonDriver.register();
        } catch (IOException e) {
            mButtonDriver = null;
            Log.w(TAG, "Could not open GPIO pins", e);
        }
    }

    private Runnable mInitializeOnBackground = new Runnable() {
        @Override
        public void run() {
            mCameraHandler = CameraHandler.getInstance();
            try {
                mCameraHandler.initializeCamera(ImageClassifierActivity.this,
                        mBackgroundHandler, MODEL_IMAGE_SIZE, ImageClassifierActivity.this);
                CameraHandler.dumpFormatInfo(ImageClassifierActivity.this);
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
            Size cameraCaptureSize = mCameraHandler.getImageDimensions();

            mImagePreprocessor =
                    new ImagePreprocessor(cameraCaptureSize.getWidth(), cameraCaptureSize.getHeight(),
                            MODEL_IMAGE_SIZE.getWidth(), MODEL_IMAGE_SIZE.getHeight());

            mTtsSpeaker = new TtsSpeaker();
            mTtsSpeaker.setHasSenseOfHumor(false);
            mTtsEngine = new TextToSpeech(ImageClassifierActivity.this,
                    status -> {
                        if (status == TextToSpeech.SUCCESS) {
                            mTtsEngine.setLanguage(Locale.US);
                            mTtsEngine.setOnUtteranceProgressListener(utteranceListener);
                            mTtsSpeaker.speakReady(mTtsEngine);
                        } else {
                            Log.w(TAG, "Could not open TTS Engine (onInit status=" + status
                                    + "). Ignoring text to speech");
                            mTtsEngine = null;
                        }
                    });

            try {
                mTensorFlowClassifier = new TensorFlowImageClassifier(ImageClassifierActivity.this,
                        MODEL_IMAGE_SIZE.getWidth(), MODEL_IMAGE_SIZE.getHeight());
            } catch (IOException e) {
                throw new IllegalStateException("Cannot initialize TFLite Classifier", e);
            }

            setReady(true);
        }
    };

    private Runnable mBackgroundClickHandler = new Runnable() {
        @Override
        public void run() {
            /*
            if (mTtsEngine != null) {
                mTtsSpeaker.speakShutterSound(mTtsEngine);
            }
            */
            mCameraHandler.takePicture();
        }
    };

    private UtteranceProgressListener utteranceListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
            setReady(false);
        }

        @Override
        public void onDone(String utteranceId) {
            setReady(true);
        }

        @Override
        public void onError(String utteranceId) {
            setReady(true);
        }
    };

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "Received key up: " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_A) {
            if (mTimer == null) {
                mTimer = new Timer();
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        attemptImageCapture();
                    }
                }, 0, 1000);
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Invoked when the user taps on the UI from a touch-enabled display
     */
    public void onShutterClick(View view) {
        Log.d(TAG, "Received screen tap");
        attemptImageCapture();
    }

    /**
     * Verify and initiate a new image capture
     */
    private void attemptImageCapture() {
        boolean isReady = mReady.get();
        Log.d(TAG, "Ready for another capture? " + isReady);
        if (isReady) {
            setReady(false);
            runOnUiThread(() -> mResultText.setText("Processing..."));
            mBackgroundHandler.post(mBackgroundClickHandler);
        } else {
            Log.i(TAG, "Sorry, processing hasn't finished. Try again in a few seconds");
        }
    }

    /**
     * Mark the system as ready for a new image capture
     */
    private void setReady(boolean ready) {
        mReady.set(ready);
        if (mReadyLED != null) {
            try {
                mReadyLED.setValue(ready);
            } catch (IOException e) {
                Log.w(TAG, "Could not set LED", e);
            }
        }
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        final Bitmap bitmap;
        try (Image image = reader.acquireNextImage()) {
            bitmap = mImagePreprocessor.preprocessImage(image);
        }

        runOnUiThread(() -> mImage.setImageBitmap(bitmap));

        final Collection<Recognition> results = mTensorFlowClassifier.doRecognize(bitmap);
        Log.d(TAG, "Got the following results from Tensorflow: " + results);

        runOnUiThread(() -> mResultText.setText(createResultsDescription(results)));

        boolean imageContainsACat = results.stream().anyMatch(x -> x.getTitle().contains("cat"));

        if (imageContainsACat && mTtsSpeaker != null) {
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                mMediaPlayer.start();
            }

            String imageRecognitionResults = results.stream().map(x -> x.getTitle()).collect(Collectors.joining(", "));
            pushNotificationSender.sendNotification("Cat detected!", "Image recognition results: " + imageRecognitionResults);
        } else {
            if (mTtsSpeaker != null && results.size() > 0)  {
                Recognition mostLikelyResult =
                        results.stream()
                                .max((lhs, rhs) -> Float.compare(lhs.getConfidence(), rhs.getConfidence()))
                                .get();

                mTtsSpeaker.speakSimpleText(mTtsEngine, mostLikelyResult.getTitle());
            }
            Log.d(TAG, "-- no cat --");
        }

        setReady(true);
    }

    private String createResultsDescription(Collection<Recognition> results) {
        if (results == null || results.isEmpty()) {
            return "I don't understand what I see";
        } else {
            StringBuilder sb = new StringBuilder();
            Iterator<Recognition> it = results.iterator();
            int counter = 0;
            while (it.hasNext()) {
                Recognition r = it.next();
                sb.append(r.getTitle());
                counter++;
                if (counter < results.size() - 1) {
                    sb.append(", ");
                } else if (counter == results.size() - 1) {
                    sb.append(" or ");
                }
            }
            return sb.toString();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mBackgroundThread != null) mBackgroundThread.quit();
        } catch (Throwable t) {
            // close quietly
        }
        mBackgroundThread = null;
        mBackgroundHandler = null;

        try {
            if (mCameraHandler != null) mCameraHandler.shutDown();
        } catch (Throwable t) {
            // close quietly
        }
        try {
            if (mTensorFlowClassifier != null) mTensorFlowClassifier.destroyClassifier();
        } catch (Throwable t) {
            // close quietly
        }
        try {
            if (mButtonDriver != null) mButtonDriver.close();
        } catch (Throwable t) {
            // close quietly
        }

        if (mTtsEngine != null) {
            mTtsEngine.stop();
            mTtsEngine.shutdown();
        }

        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
        }
    }

    /**
     * @return true if this device is running Android Things.
     * <p>
     * Source: https://stackoverflow.com/a/44171734/112705
     */
    private boolean isAndroidThingsDevice(Context context) {
        // We can't use PackageManager.FEATURE_EMBEDDED here as it was only added in API level 26,
        // and we currently target a lower minSdkVersion
        final PackageManager pm = context.getPackageManager();
        boolean isRunningAndroidThings = pm.hasSystemFeature("android.hardware.type.embedded");
        Log.d(TAG, "isRunningAndroidThings: " + isRunningAndroidThings);
        return isRunningAndroidThings;
    }
}
