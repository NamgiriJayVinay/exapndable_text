
2024-11-19 17:14:39.976 12755-13104 YuvToJpegEncoder        com.android.privacyview              D  onFlyCompress
2024-11-19 17:14:40.011 12755-13104 System.err              com.android.privacyview              W  java.lang.IllegalArgumentException: Image dimension, ByteBuffer size and format don't match. Please check if the ByteBuffer is in the decalred format.
2024-11-19 17:14:40.011 12755-13104 System.err              com.android.privacyview              W  	at com.google.android.gms.common.internal.Preconditions.checkArgument(com.google.android.gms:play-services-basement@@18.3.0:2)
2024-11-19 17:14:40.011 12755-13104 System.err              com.android.privacyview              W  	at com.google.mlkit.vision.common.InputImage.<init>(com.google.mlkit:vision-common@@17.3.0:10)
2024-11-19 17:14:40.011 12755-13104 System.err              com.android.privacyview              W  	at com.google.mlkit.vision.common.InputImage.fromByteArray(com.google.mlkit:vision-common@@17.3.0:2)
2024-11-19 17:14:40.011 12755-13104 System.err              com.android.privacyview              W  	at com.android.privacyview.LiveFace.lambda$bindCameraUseCases$4$com-android-privacyview-LiveFace(LiveFace.java:351)
2024-11-19 17:14:40.011 12755-13104 System.err              com.android.privacyview              W  	at com.android.privacyview.LiveFace$$ExternalSyntheticLambda0.analyze(Unknown Source:2)
2024-11-19 17:14:40.011 12755-13104 System.err              com.android.privacyview              W  	at androidx.camera.core.ImageAnalysis.lambda$setAnalyzer$3(ImageAnalysis.java:573)
2024-11-19 17:14:40.011 12755-13104 System.err              com.android.privacyview              W  	at androidx.camera.core.ImageAnalysis$$ExternalSyntheticLambda5.analyze(Unknown Source:2)
2024-11-19 17:14:40.011 12755-13104 System.err              com.android.privacyview              W  	at androidx.camera.core.ImageAnalysisAbstractAnalyzer.lambda$analyzeImage$0$androidx-camera-core-ImageAnalysisAbstractAnalyzer(ImageAnalysisAbstractAnalyzer.java:284)
2024-11-19 17:14:40.011 12755-13104 System.err              com.android.privacyview              W  	at androidx.camera.core.ImageAnalysisAbstractAnalyzer$$ExternalSyntheticLambda1.run(Unknown Source:14)
2024-11-19 17:14:40.011 12755-13104 System.err              com.android.privacyview              W  	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145)
2024-11-19 17:14:40.011 12755-13104 System.err              com.android.privacyview              W  	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:644)
2024-11-19 17:14:40.011 12755-13104 System.err              com.android.privacyview              W  	at java.lang.Thread.run(Thread.java:1012)
2024-11-19 17:14:40.012 12755-13104 YuvToJpegEncoder        com.android.privacyview              D  onFlyCompress
2024-11-19 17:14:40.044 12755-13104 System.err              com.android.privacyview              W  java.lang.IllegalArgumentException: Image dimension, ByteBuffer size and format don't match. Please check if the ByteBuffer is in the decalred format.
2024-11-19 17:14:40.044 12755-13104 System.err              com.android.privacyview              W  	at com.google.android.gms.common.internal.Preconditions.checkArgument(com.google.android.gms:play-services-basement@@18.3.0:2)
2024-11-19 17:14:40.044 12755-13104 System.err              com.android.privacyview              W  	at com.google.mlkit.vision.common.InputImage.<init>(com.google.mlkit:vision-common@@17.3.0:10)
2024-11-19 17:14:40.044 12755-13104 System.err              com.android.privacyview              W  	at com.google.mlkit.vision.common.InputImage.fromByteArray(com.google.mlkit:vision-common@@17.3.0:2)


// FaceBoxOverlay.java
package com.example.facedetectionapp;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import com.google.mlkit.vision.face.Face;
import java.util.ArrayList;
import java.util.List;

public class FaceBoxOverlay extends View {
    private List<Face> faces = new ArrayList<>();
    private final Paint boxPaint;
    private int previewWidth = 640;
    private int previewHeight = 480;
    private Path clipPath;
    private FaceDetectionListener listener;
    private int imageWidth = 0;
    private int imageHeight = 0;
    private int facing = 1; // 1 for front camera, 0 for back camera

    public interface FaceDetectionListener {
        void onGoodFaceDetected(Face face, Rect boundingBox);
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

    public void setImageSourceInfo(int width, int height, int facing) {
        this.imageWidth = width;
        this.imageHeight = height;
        this.facing = facing;
    }

    public void setFaces(List<Face> faces) {
        this.faces = faces;
        checkFaceQuality();
        invalidate();
    }

    private void checkFaceQuality() {
        if (faces.size() == 1) {
            Face face = faces.get(0);
            Rect boundingBox = face.getBoundingBox();

            // Get normalized coordinates
            float centerX = (boundingBox.centerX() / (float) imageWidth);
            float width = (boundingBox.width() / (float) imageWidth);
            
            // Check if face is centered and of good size
            boolean isCentered = centerX > 0.3f && centerX < 0.7f;
            boolean isGoodSize = width > 0.25f && width < 0.85f;
            
            if (isCentered && isGoodSize && face.getHeadEulerAngleY() > -10 && 
                face.getHeadEulerAngleY() < 10) {
                if (listener != null) {
                    listener.onGoodFaceDetected(face, boundingBox);
                }
                return;
            }
        }
        
        if (listener != null) {
            listener.onFaceLost();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float scaleX = getWidth() / (float) imageWidth;
        float scaleY = getHeight() / (float) imageHeight;
        float scale = Math.min(scaleX, scaleY);
        
        float offsetX = (getWidth() - imageWidth * scale) / 2;
        float offsetY = (getHeight() - imageHeight * scale) / 2;

        for (Face face : faces) {
            Rect bounds = face.getBoundingBox();
            
            float left = scale * bounds.left + offsetX;
            float top = scale * bounds.top + offsetY;
            float right = scale * bounds.right + offsetX;
            float bottom = scale * bounds.bottom + offsetY;
            
            // Mirror coordinates for front camera
            if (facing == 1) {
                float temp = left;
                left = getWidth() - right;
                right = getWidth() - temp;
            }

            canvas.drawRect(left, top, right, bottom, boxPaint);
        }
    }
}

// MainActivity.java
package com.example.facedetectionapp;

import android.Manifest;
import android.content.ContentValues;
import android.graphics.*;
import android.media.ExifInterface;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.*;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity implements FaceBoxOverlay.FaceDetectionListener {
    // ... (previous constants and variable declarations)

    private Bitmap lastProcessedBitmap;
    private final Object bitmapLock = new Object();

    @Override
    public void onGoodFaceDetected(Face face, Rect boundingBox) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCaptureTime >= CAPTURE_DELAY_MS && 
            captureCount.get() < MAX_CAPTURES && lastProcessedBitmap != null) {
            synchronized (bitmapLock) {
                if (lastProcessedBitmap != null) {
                    cropAndSaveFace(lastProcessedBitmap, face, boundingBox);
                }
            }
            lastCaptureTime = currentTime;
        }
    }

    private void cropAndSaveFace(Bitmap originalBitmap, Face face, Rect boundingBox) {
        try {
            // Add padding to the bounding box
            int padding = Math.min(boundingBox.width(), boundingBox.height()) / 8;
            Rect paddedBox = new Rect(
                boundingBox.left - padding,
                boundingBox.top - padding,
                boundingBox.right + padding,
                boundingBox.bottom + padding
            );

            // Ensure the padded box is within image bounds
            paddedBox.left = Math.max(0, paddedBox.left);
            paddedBox.top = Math.max(0, paddedBox.top);
            paddedBox.right = Math.min(originalBitmap.getWidth(), paddedBox.right);
            paddedBox.bottom = Math.min(originalBitmap.getHeight(), paddedBox.bottom);

            // Create the cropped bitmap
            Bitmap croppedBitmap = Bitmap.createBitmap(
                originalBitmap,
                paddedBox.left,
                paddedBox.top,
                paddedBox.width(),
                paddedBox.height()
            );

            // Save the cropped face
            saveFaceImage(croppedBitmap);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveFaceImage(Bitmap faceBitmap) {
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

        try {
            Uri imageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
                contentValues
            );

            if (imageUri != null) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                faceBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                byte[] imageBytes = outputStream.toByteArray();

                try (OutputStream os = getContentResolver().openOutputStream(imageUri)) {
                    os.write(imageBytes);
                }

                int count = captureCount.incrementAndGet();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                        "Saved face " + count + " of " + MAX_CAPTURES,
                        Toast.LENGTH_SHORT).show();

                    if (count >= MAX_CAPTURES) {
                        Toast.makeText(MainActivity.this,
                            "All faces captured!",
                            Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        imageCapture = new ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
            .setTargetResolution(new Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            try {
                @SuppressWarnings("ConstantConditions")
                ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);

                int rotation = imageProxy.getImageInfo().getRotationDegrees();
                Size size = new Size(
                    imageProxy.getWidth(),
                    imageProxy.getHeight()
                );

                // Convert YUV to Bitmap
                YuvImage yuvImage = new YuvImage(
                    data,
                    ImageFormat.NV21,
                    size.getWidth(),
                    size.getHeight(),
                    null
                );

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(
                    new Rect(0, 0, size.getWidth(), size.getHeight()),
                    100,
                    outputStream
                );

                byte[] jpegData = outputStream.toByteArray();
                Bitmap bitmap = BitmapFactory.decodeByteArray(
                    jpegData,
                    0,
                    jpegData.length
                );

                // Rotate bitmap if needed
                if (rotation != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotation);
                    bitmap = Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.getWidth(),
                        bitmap.getHeight(),
                        matrix,
                        true
                    );
                }

                synchronized (bitmapLock) {
                    lastProcessedBitmap = bitmap;
                }

                InputImage inputImage = InputImage.fromByteArray(
                    data,
                    size.getWidth(),
                    size.getHeight(),
                    rotation,
                    InputImage.IMAGE_FORMAT_NV21
                );

                faceDetector.process(inputImage)
                    .addOnSuccessListener(faces -> {
                        faceBoxOverlay.setImageSourceInfo(
                            size.getWidth(),
                            size.getHeight(),
                            CameraSelector.LENS_FACING_FRONT
                        );
                        faceBoxOverlay.setFaces(faces);
                    })
                    .addOnCompleteListener(task -> imageProxy.close());

            } catch (Exception e) {
                imageProxy.close();
                e.printStackTrace();
            }
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

    // ... (rest of the MainActivity implementation remains the same)
}
