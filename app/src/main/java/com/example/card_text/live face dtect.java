// circular_preview_background.xml (create in res/drawable)
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@android:color/black" />
</shape>

// activity_main.xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@android:color/white">

    <androidx.cardview.widget.CardView
        android:id="@+id/preview_container"
        android:layout_width="300dp"
        android:layout_height="300dp"
        app:cardCornerRadius="150dp"
        app:cardElevation="8dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent">

        <androidx.camera.view.PreviewView
            android:id="@+id/preview_view"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />

        <com.example.facedetectionapp.FaceBoxOverlay
            android:id="@+id/face_box_overlay"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />

    </androidx.cardview.widget.CardView>

</androidx.constraintlayout.widget.ConstraintLayout>

// CircularPreviewView.java
package com.example.facedetectionapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import androidx.camera.view.PreviewView;

public class CircularPreviewView extends PreviewView {
    private Path clipPath;

    public CircularPreviewView(Context context) {
        super(context);
        init();
    }

    public CircularPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        clipPath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        clipPath.reset();
        clipPath.addCircle(getWidth() / 2f, getHeight() / 2f,
                Math.min(getWidth() / 2f, getHeight() / 2f),
                Path.Direction.CW);
        
        canvas.clipPath(clipPath);
        super.onDraw(canvas);
    }
}

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

    public FaceBoxOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(3.0f);
        
        clipPath = new Path();
    }

    public void setFaces(List<Face> faces) {
        this.faces = faces;
        postInvalidate();
    }

    public void setPreviewSize(int width, int height) {
        previewWidth = width;
        previewHeight = height;
        calculateScaleFactor();
    }

    private void calculateScaleFactor() {
        scaleX = getWidth() / (float) previewWidth;
        scaleY = getHeight() / (float) previewHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Create circular clip path
        clipPath.reset();
        clipPath.addCircle(getWidth() / 2f, getHeight() / 2f,
                Math.min(getWidth() / 2f, getHeight() / 2f),
                Path.Direction.CW);
        canvas.clipPath(clipPath);

        for (Face face : faces) {
            Rect bounds = face.getBoundingBox();
            
            // Convert coordinates to view space
            float left = translateX(bounds.left);
            float top = translateY(bounds.top);
            float right = translateX(bounds.right);
            float bottom = translateY(bounds.bottom);

            // Draw face bounding box
            RectF adjustedRect = new RectF(left, top, right, bottom);
            canvas.drawRect(adjustedRect, boxPaint);
        }
    }

    private float translateX(float x) {
        // Mirror coordinate for front camera
        float scaledX = x * scaleX;
        return getWidth() - scaledX;
    }

    private float translateY(float y) {
        return y * scaleY;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        calculateScaleFactor();
    }
}

// MainActivity.java
package com.example.facedetectionapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CAMERA = 1001;
    private PreviewView previewView;
    private FaceBoxOverlay faceBoxOverlay;
    private ExecutorService cameraExecutor;
    private FaceDetector faceDetector;
    private static final Size PREVIEW_SIZE = new Size(640, 480);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.preview_view);
        faceBoxOverlay = findViewById(R.id.face_box_overlay);
        
        // Set the preview size for correct scaling
        faceBoxOverlay.setPreviewSize(PREVIEW_SIZE.getWidth(), PREVIEW_SIZE.getHeight());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA);
        } else {
            startCamera();
        }

        // Initialize face detector with high accuracy
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        faceDetector = FaceDetection.getClient(options);
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .setTargetResolution(PREVIEW_SIZE)
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(PREVIEW_SIZE)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            @SuppressWarnings("DefaultLocale")
            InputImage image = InputImage.fromMediaImage(
                    imageProxy.getImage(),
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            faceDetector.process(image)
                    .addOnSuccessListener(faces -> {
                        faceBoxOverlay.setFaces(faces);
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        });

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        
        // Unbind previous use cases before rebinding
        cameraProvider.unbindAll();
        
        Camera camera = cameraProvider.bindToLifecycle(
            (LifecycleOwner) this, 
            cameraSelector, 
            preview, 
            imageAnalysis
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}