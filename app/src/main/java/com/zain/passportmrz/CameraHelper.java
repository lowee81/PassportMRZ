package com.zain.passportmrz;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.Image;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.zain.passportmrz.databinding.ActivityMainBinding;


class CameraHelper {

    final int REQUEST_CODE_PERMISSIONS = 42;

    AppCompatActivity owner;
    Context context;
    PreviewView viewFinder;
    ActivityMainBinding binding;

    public CameraHelper(AppCompatActivity owner,
                        Context context,
                        PreviewView viewFinder, ActivityMainBinding binding) {
        this.owner = owner;
        this.context = context;
        this.viewFinder = viewFinder;
        this.binding = binding;
    }

    ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    int lensFacing = CameraSelector.LENS_FACING_BACK;
    Camera camera = null;
    ProcessCameraProvider cameraProvider = null;

    void start() {
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(owner, new String[]{Manifest.permission.CAMERA}, 42);
        }
    }

    void stop() {
        cameraExecutor.shutdown();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if(cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)){
                    lensFacing = CameraSelector.LENS_FACING_BACK;
                } else if(cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)){
                    lensFacing = CameraSelector.LENS_FACING_FRONT;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


            bindCameraUseCases();
        }, ContextCompat.getMainExecutor(context));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            throw new IllegalStateException("Camera initialization failed.");
        }

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        Preview previewView = getPreviewUseCase();
        ImageAnalysis textRecognizer = getTextAnalyzerUseCase();

        UseCaseGroup useCaseGroup = new UseCaseGroup.Builder().addUseCase(previewView).addUseCase(textRecognizer).build();

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(owner, cameraSelector, useCaseGroup);
            previewView.setSurfaceProvider(viewFinder.getSurfaceProvider());
        } catch (Exception e) {
            Log.e("Camera", "Use case binding failed " + e);
        }
    }

    private int aspectRatio() {
        return AspectRatio.RATIO_16_9;
    }

    private Preview getPreviewUseCase() {
        DisplayMetrics metrics = new DisplayMetrics();
        viewFinder.getDisplay().getRealMetrics(metrics);
        int rotation = (int) viewFinder.getRotation();

        Preview preview = new Preview.Builder().setTargetResolution(new Size(metrics.widthPixels, metrics.heightPixels))
                .setTargetRotation(rotation)
                .build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
        return preview;
    }

    private ImageAnalysis getTextAnalyzerUseCase() {

        ImageAnalysis analyzer = new ImageAnalysis.Builder()
                .setTargetAspectRatio(aspectRatio())
                .setTargetRotation(viewFinder.getDisplay().getRotation())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setImageQueueDepth(10)
                .build();
        analyzer.setAnalyzer(cameraExecutor, new TextAnalyzer());

        return analyzer;
    }

    private class TextAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        @SuppressLint("UnsafeOptInUsageError")
        public void analyze(ImageProxy imageProxy) {
             Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {
                                Log.e("check", visionText.getText());
                                //binding.textResult.setText(visionText.getText());

                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
            }
        }

    }

    void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(
                        context,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private boolean allPermissionsGranted() {
        String[] permissions = new String[]{Manifest.permission.CAMERA};
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(
                    context, permission
            ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false;
            }
        }
        return true;
    }
}

