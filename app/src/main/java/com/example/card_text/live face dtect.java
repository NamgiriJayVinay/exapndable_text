// AndroidManifest.xml (add storage permission)
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
                     android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
                     android:maxSdkVersion="32" />
    <!-- For Android 13 and above -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    
    <!-- Rest of your manifest -->
</manifest>

// FaceBoxOverlay.java
package com.example.facedetectionapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.google.mlkit.vision.face.Face;
import java.util.ArrayList;
import java.util.List;

public class FaceBoxOverlay extends View {
    private List<Face> faces = new ArrayList<>();
    private final Paint boxPaint;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private int previewWidth = 640;
    private int previewHeight = 480;
    private Path clipPath;
    private FaceDetectionListener listener;
    private boolean isGoodFaceDetection = false;

    public interface FaceDetectionListener {
        void onGoodFaceDetected(RectF faceBounds);
        void onFaceLost();
    }

    public FaceBoxOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(3.0f);
        
        clipPath = new Path();
    }

    public void setFaceDetectionListener(FaceDetectionListener listener) {
        this.listener = listener;
    }

    public void setFaces(List<Face> faces) {
        this.faces = faces;
        checkFaceQuality();
        postInvalidate();
    }

    private void checkFaceQuality() {
        if (faces.size() == 1) {
            Face face = faces.get(0);
            
            // Get face bounds
            Rect bounds = face.getBoundingBox();
            float faceWidth = bounds.width() * scaleX;
            float faceHeight = bounds.height() * scaleY;
            
            // Check if face is centered and of good size
            boolean isFaceCentered = Math.abs(bounds.centerX() - previewWidth/2f) < previewWidth/6f;
            boolean isGoodSize = faceWidth > getWidth()/4f && faceHeight > getHeight()/4f;
            
            if (isFaceCentered && isGoodSize) {
                isGoodFaceDetection = true;
                if (listener != null) {
                    RectF faceBounds = new RectF(
                        translateX(bounds.left),
                        translateY(bounds.top),
                        translateX(bounds.right),
                        translateY(bounds.bottom)
                    );
                    listener.onGoodFaceDetected(faceBounds);
                }
                return;
            }
        }
        
        isGoodFaceDetection = false;
        if (listener != null) {
            listener.onFaceLost();
        }
    }

    // ... (rest of the FaceBoxOverlay methods remain the same)
}

// MainActivity.java
package com.example.facedetectionapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity implements FaceBoxOverlay.FaceDetectionListener {
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int MAX_CAPTURES = 6;
    
    private PreviewView previewView;
    private FaceBoxOverlay faceBoxOverlay;
    private ExecutorService cameraExecutor;
    private FaceDetector faceDetector;
    private ImageCapture imageCapture;
    private AtomicInteger captureCount = new AtomicInteger(0);
    private long lastCaptureTime = 0;
    private static final long CAPTURE_DELAY_MS = 1000; // 1 second delay between captures
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.preview_view);
        faceBoxOverlay = findViewById(R.id.face_box_overlay);
        faceBoxOverlay.setFaceDetectionListener(this);

        requestPermissions();
        
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        faceDetector = FaceDetection.getClient(options);
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void requestPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        } else {
            startCamera();
        }
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onGoodFaceDetected(RectF faceBounds) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCaptureTime >= CAPTURE_DELAY_MS && 
            captureCount.get() < MAX_CAPTURES) {
            captureFace(faceBounds);
            lastCaptureTime = currentTime;
        }
    }

    @Override
    public void onFaceLost() {
        // Face is no longer detected or not in good position
    }

    private void captureFace(RectF faceBounds) {
        if (imageCapture == null) return;

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String filename = "FACE_" + timestamp + ".jpg";

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, 
                Environment.DIRECTORY_PICTURES + "/FaceCaptures");
        }

        ImageCapture.OutputFileOptions outputFileOptions = 
            new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build();

        imageCapture.takePicture(outputFileOptions, cameraExecutor,
            new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                    int count = captureCount.incrementAndGet();
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                            "Captured image " + count + " of " + MAX_CAPTURES, 
                            Toast.LENGTH_SHORT).show();
                        
                        if (count >= MAX_CAPTURES) {
                            Toast.makeText(MainActivity.this, 
                                "All captures completed!", 
                                Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, 
                        "Error capturing image: " + exception.getMessage(), 
                        Toast.LENGTH_SHORT).show());
                }
            });
    }

    private void startCamera() {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();
                bindCameraUseCases(cameraProvider);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        
        imageCapture = new ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
            );

            faceDetector.process(image)
                .addOnSuccessListener(faces -> faceBoxOverlay.setFaces(faces))
                .addOnCompleteListener(task -> imageProxy.close());
        });

        CameraSelector cameraSelector = new CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build();

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                this, 
                cameraSelector,
                preview, 
                imageCapture, 
                imageAnalysis
            );
            
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions required to use the app", 
                    Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}