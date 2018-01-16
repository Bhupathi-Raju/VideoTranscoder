package com.zemoso.videotranscoder;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.media.MediaCodec;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.zemoso.videotranscoder.format.MediaFormatStrategyPresets;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Future;


public class TranscoderActivity extends Activity {
    private static final String TAG = "TranscoderActivity";
    private static final String FILE_PROVIDER_AUTHORITY = "net.ypresto.androidtranscoder.example.fileprovider";
    private static final int REQUEST_CODE_PICK = 1;
    private static final int PROGRESS_BAR_MAX = 1000;
    private Future<Void> mFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transcoder);
        findViewById(R.id.select_video_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*"), REQUEST_CODE_PICK);
            }
        });
        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFuture.cancel(true);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PICK: {
                final File file;
                if (resultCode == RESULT_OK) {
                    try {
                        File outputDir = new File(getExternalFilesDir(null), "outputs");
                        //noinspection ResultOfMethodCallIgnored
                        outputDir.mkdir();
                        file = File.createTempFile("transcode_test", ".mp4", outputDir);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to create temporary file.", e);
                        Toast.makeText(this, "Failed to create temporary file.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    ContentResolver resolver = getContentResolver();
                    final ParcelFileDescriptor parcelFileDescriptor;
                    try {
                        parcelFileDescriptor = resolver.openFileDescriptor(data.getData(), "r");
                    } catch (FileNotFoundException e) {
                        Log.w("Could not open '" + data.getDataString() + "'", e);
                        Toast.makeText(TranscoderActivity.this, "File not found.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                    final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                    progressBar.setMax(PROGRESS_BAR_MAX);
                    final long startTime = SystemClock.uptimeMillis();
                    MediaTranscoder.Listener listener = new MediaTranscoder.Listener() {
                        @Override
                        public void onTranscodeProgress(double progress) {
                            if (progress < 0) {
                                progressBar.setIndeterminate(true);
                            } else {
                                progressBar.setIndeterminate(false);
                                progressBar.setProgress((int) Math.round(progress * PROGRESS_BAR_MAX));
                            }
                        }

                        @Override
                        public void onTranscodeCompleted() {
                            Log.d(TAG, "transcoding took " + (SystemClock.uptimeMillis() - startTime) + "ms");
                            onTranscodeFinished(true, "transcoded file placed on " + file, parcelFileDescriptor);
                            Uri uri = FileProvider.getUriForFile(TranscoderActivity.this, FILE_PROVIDER_AUTHORITY, file);
                            startActivity(new Intent(Intent.ACTION_VIEW)
                                    .setDataAndType(uri, "video/mp4")
                                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
                        }

                        @Override
                        public void onTranscodeCanceled() {
                            onTranscodeFinished(false, "Transcoder canceled.", parcelFileDescriptor);
                        }

                        @Override
                        public void onTranscodeFailed(Exception exception) {
                            onTranscodeFinished(false, "Transcoder error occurred.", parcelFileDescriptor);
                        }
                    };
                    Log.d(TAG, "transcoding into " + file);
                    mFuture = MediaTranscoder.getInstance().transcodeVideo(fileDescriptor, file.getAbsolutePath(),
                            MediaFormatStrategyPresets.createAndroid720pStrategy(8000 * 1000, 128 * 1000, 1), listener);
                    switchButtonEnabled(true);
                }
                break;
            }
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
      //  getMenuInflater().inflate(R.menu.transcoder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
/*
        if (id == R.id.action_settings) {
            return true;
        }
*/
        return super.onOptionsItemSelected(item);
    }

    private void onTranscodeFinished(boolean isSuccess, String toastMessage, ParcelFileDescriptor parcelFileDescriptor) {
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setIndeterminate(false);
        progressBar.setProgress(isSuccess ? PROGRESS_BAR_MAX : 0);
        progressBar.setProgress(0);
        switchButtonEnabled(false);
        Toast.makeText(TranscoderActivity.this, toastMessage, Toast.LENGTH_LONG).show();
        try {
            parcelFileDescriptor.close();
        } catch (IOException e) {
            Log.w("Error while closing", e);
        }
    }

    private void switchButtonEnabled(boolean isProgress) {
        findViewById(R.id.select_video_button).setEnabled(!isProgress);
        findViewById(R.id.cancel_button).setEnabled(isProgress);
    }
/*
    MediaCodec encoder;
    InputSurface inputSurface = new InputSurface(encoder.createInputSurface());
    */

    /**
     * Holds state associated with a Surface used for MediaCodec encoder input.
     * <p>
     * The constructor takes a Surface obtained from MediaCodec.createInputSurface(), and uses that
     * to create an EGL window surface.  Calls to eglSwapBuffers() cause a frame of data to be sent
     * to the video encoder.
     */
   /* static class InputSurface {
        private static final String TAG = "InputSurface";
        private static final int EGL_RECORDABLE_ANDROID = 0x3142;
        private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
        private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;
        private Surface mSurface;
        *//**
         * Creates an InputSurface from a Surface.
         *//*
        public InputSurface(Surface surface) {
            if (surface == null) {
                throw new NullPointerException();
            }
            mSurface = surface;
            eglSetup();
        }
        *//**
         * Prepares EGL.  We want a GLES 2.0 context and a surface that supports recording.
         *//*
        private void eglSetup() {
            mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
                throw new RuntimeException("unable to get EGL14 display");
            }
            int[] version = new int[2];
            if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
                mEGLDisplay = null;
                throw new RuntimeException("unable to initialize EGL14");
            }
            // Configure EGL for recordable and OpenGL ES 2.0.  We want enough RGB bits
            // to minimize artifacts from possible YUV conversion.
            int[] attribList = {
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL_RECORDABLE_ANDROID, 1,
                    EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                    numConfigs, 0)) {
                throw new RuntimeException("unable to find RGB888+recordable ES2 EGL config");
            }
            // Configure context for OpenGL ES 2.0.
            int[] attrib_list = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };
            mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
                    attrib_list, 0);
            checkEglError("eglCreateContext");
            if (mEGLContext == null) {
                throw new RuntimeException("null context");
            }
            // Create a window surface, and attach it to the Surface we received.
            int[] surfaceAttribs = {
                    EGL14.EGL_NONE
            };
            mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurface,
                    surfaceAttribs, 0);
            checkEglError("eglCreateWindowSurface");
            if (mEGLSurface == null) {
                throw new RuntimeException("surface was null");
            }
        }
        *//**
         * Discard all resources held by this class, notably the EGL context.  Also releases the
         * Surface that was passed to our constructor.
         *//*
        public void release() {
            if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
                EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
                EGL14.eglReleaseThread();
                EGL14.eglTerminate(mEGLDisplay);
            }
            mSurface.release();
            mEGLDisplay = EGL14.EGL_NO_DISPLAY;
            mEGLContext = EGL14.EGL_NO_CONTEXT;
            mEGLSurface = EGL14.EGL_NO_SURFACE;
            mSurface = null;
        }
        *//**
         * Makes our EGL context and surface current.
         *//*
        public void makeCurrent() {
            if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
                throw new RuntimeException("eglMakeCurrent failed");
            }
        }
        public void makeUnCurrent() {
            if (!EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT)) {
                throw new RuntimeException("eglMakeCurrent failed");
            }
        }
        *//**
         * Calls eglSwapBuffers.  Use this to "publish" the current frame.
         *//*
        public boolean swapBuffers() {
            return EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);
        }
        *//**
         * Returns the Surface that the MediaCodec receives buffers from.
         *//*
        public Surface getSurface() {
            return mSurface;
        }
        *//**
         * Queries the surface's width.
         *//*
        public int getWidth() {
            int[] value = new int[1];
            EGL14.eglQuerySurface(mEGLDisplay, mEGLSurface, EGL14.EGL_WIDTH, value, 0);
            return value[0];
        }
        *//**
         * Queries the surface's height.
         *//*
        public int getHeight() {
            int[] value = new int[1];
            EGL14.eglQuerySurface(mEGLDisplay, mEGLSurface, EGL14.EGL_HEIGHT, value, 0);
            return value[0];
        }
        *//**
         * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
         *//*
        public void setPresentationTime(long nsecs) {
            EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);
        }
        *//**
         * Checks for EGL errors.
         *//*
        private void checkEglError(String msg) {
            int error;
            if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
                throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
            }
        }
    }*/
}