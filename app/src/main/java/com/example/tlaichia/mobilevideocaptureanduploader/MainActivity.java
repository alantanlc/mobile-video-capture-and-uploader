package com.example.tlaichia.mobilevideocaptureanduploader;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 1;
    private static final int NUM_FRAMES_PER_REQUEST = 90;
    private static final String MEDIA_CODEC_ENCODER_TYPE = "video/avc";
    private TextureView mTextureView;
    private CameraDevice mCameraDevice;
    private String mCameraId;
    private Size mPreviewSize;
    private Size mVideoSize;
    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private ImageButton mRecordVideoImageButton;
    private boolean mIsRecording;
    private File mVideoFolder;
    private String mVideoFileName;
    private int mFrameCount;
    private FileOutputStream mFileOutputStream;
    private byte[] csdData;
    private boolean isFirstFrame;

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {

            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            if (mIsRecording) {
                try {
                    File f = createVideoFileName();
                    mFileOutputStream = new FileOutputStream(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startRecord();
                mMediaCodec.start();
            } else {
                startPreview();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // Instantiate variables
            mMediaCodec = MediaCodec.createEncoderByType(MEDIA_CODEC_ENCODER_TYPE);
            mTextureView = (TextureView) findViewById(R.id.textureView);
            mIsRecording = false;
            //csdData = new byte[]{0,0,0,1,103,66,-128,31,-38,1,64,22,-23,72,40,48,48,54,-123,9,-88,0,0,0,1,104,-50,6,-30};
            csdData = new byte[]{0,0,0,1,103,66,-128,31,-38,1,64,22,-23,72,40,48,48,54,-123,9,-88,0,0,0,1,104,-50,6,-30};

            // OnClickListener
            mRecordVideoImageButton = (ImageButton) findViewById(R.id.recordVideoImageButton);
            mRecordVideoImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mIsRecording) {
                        mIsRecording = false;
                        mRecordVideoImageButton.setImageResource(R.mipmap.btn_video_record);

                        try {// AsyncTask to perform MP4Parser operations
                            new MP4UploaderTask().execute(mVideoFileName);

                            // Close file and create new file
                            mFileOutputStream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        mMediaCodec.stop();
                        mMediaCodec.reset();
                        startPreview();
                    } else {
                        checkWriteStoragePermission();
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        startBackgroundThread();

        if(mTextureView.isAvailable()) {
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "App will not run without camera services", Toast.LENGTH_SHORT).show();
            }
        }

        if(requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mIsRecording = true;
                mRecordVideoImageButton.setImageResource(R.mipmap.btn_video_recording);
                Toast.makeText(this, "Permission successfully granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "App needs to write to external storage permissions to run", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        closeCamera();

        stopBackgroundThread();

        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = (View) getWindow().getDecorView();
        if(hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                mPreviewSize = new Size(1280, 720);
                mVideoSize = new Size(1280, 720);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                } else {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        Toast.makeText(this, "App requires access to camera", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[] { Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
                }
            } else {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startRecord() {
        try {
            setupMediaCodec();

            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);

            Surface recordSurface = mMediaCodec.createInputSurface();

            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCaptureRequestBuilder.addTarget(recordSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, null);

            mMediaCodec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(MediaCodec codec, int index) {

                }

                @Override
                public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                    try {
                        // Get ByteBuffer
                        ByteBuffer outputBuffer = codec.getOutputBuffer(index);
                        byte[] b = new byte[info.size];
                        outputBuffer.get(b);

                        // Write data to file
                        mFileOutputStream.write(b);

                        // Increment frame count
                        if(!isFirstFrame) {
                            mFrameCount++;
                        } else {
                            isFirstFrame = false;
                        }

                        if(mFrameCount == NUM_FRAMES_PER_REQUEST) {
                            // AsyncTask to perform MP4Parser operations
                            new MP4UploaderTask().execute(mVideoFileName);

                            // Close file and create new file
                            mFileOutputStream.close();

                            // Create new FileOutputStream
                            File f = createVideoFileName();
                            mFileOutputStream = new FileOutputStream(f);

                            // Write h264 codec-specific data
                            mFileOutputStream.write(csdData);

                            // Reset frameCount
                            mFrameCount = 0;

                            Log.i("MainActivity", "Resetting frame count");
                        }

                        // Release buffer
                        codec.releaseOutputBuffer(index, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(MediaCodec codec, MediaCodec.CodecException e) {
                    Toast.makeText(getApplicationContext(), "mMediaCodec onError!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                    mMediaFormat = format;
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if(mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("Camera2VideoImage");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setupMediaCodec() throws IOException {
        mMediaFormat = MediaFormat.createVideoFormat(MEDIA_CODEC_ENCODER_TYPE, mVideoSize.getWidth(), mVideoSize.getHeight());
        int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 5000000);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    private void createVideoFolder() {
        File movieFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "MVCAU_" + timeStamp;
        mVideoFolder = new File(movieFolder, prepend);
        if(!mVideoFolder.exists()) {
            mVideoFolder.mkdirs();
        }
    }

    private File createVideoFileName() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "VIDEO_" + timeStamp + "_";
        File videoFile = File.createTempFile(prepend, ".h264", mVideoFolder);
        mVideoFileName = videoFile.getAbsolutePath();
        return videoFile;
    }

    private void checkWriteStoragePermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                mIsRecording = true;
                mFrameCount = 0;
                isFirstFrame = true;
                mRecordVideoImageButton.setImageResource(R.mipmap.btn_video_recording);
                try {
                    createVideoFolder();
                    File f = createVideoFileName();
                    mFileOutputStream = new FileOutputStream(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startRecord();
                mMediaCodec.start();
            } else {
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "App needs to be able to save videos", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT);
            }
        } else {
            mIsRecording = true;
            mFrameCount = 0;
            isFirstFrame = true;
            mRecordVideoImageButton.setImageResource(R.mipmap.btn_video_recording);
            try {
                createVideoFolder();
                File f = createVideoFileName();
                mFileOutputStream = new FileOutputStream(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
            startRecord();
            mMediaCodec.start();
        }
    }
}