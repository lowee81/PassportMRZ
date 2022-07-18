package com.zain.passportmrz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.zain.passportmrz.databinding.ActivityMainBinding;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    final int[] WEIGHTS = {7, 3, 1};

    final int REQUEST_CODE_PERMISSIONS = 10;
    final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    ActivityMainBinding binding;
    CameraHelper cameraHelper;

    ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;
    ProcessCameraProvider cameraProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        //cameraHelper = new CameraHelper(this, this.getApplicationContext(), binding.cameraView, binding);
        //cameraHelper.start();

        cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this);

        if(allPermissionsGranted()){
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this, permission
            ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false;
            }
        }
        return true;
    }

    private void startCamera() {
        cameraProviderListenableFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderListenableFuture.get();
                binding.viewFinder.post(() -> setupCamera());
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private Preview buildPreviewUseCase() {
        Display display = binding.viewFinder.getDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        int rotation = display.getRotation();
        Preview preview = new Preview.Builder().setTargetResolution(new Size(metrics.widthPixels, metrics.heightPixels)).setTargetRotation(rotation).build();
        preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());
        return preview;
    }

    private void setupCamera() {
        cameraProviderListenableFuture.addListener(() -> {
            Preview preview = buildPreviewUseCase();
            ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder().build();
            imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(this), image -> processImage(image));

            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

            UseCaseGroup useCaseGroup = new UseCaseGroup.Builder().addUseCase(preview).addUseCase(imageAnalyzer).build();

            try {
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup);
            } catch(Exception e){
                Log.e("CameraX", "Use case binding failed", e);
            }

        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void processImage(ImageProxy imageProxy){
        if(binding.viewFinder.getVisibility() == View.VISIBLE){
            //Bitmap imageBitmap = ImageUtils.convertYuv420888ImageToBitmap(imageProxy.getImage());
            //imageBitmap = rotateBitmap(imageBitmap, (float)imageProxy.getImageInfo().getRotationDegrees());
            //binding.insetView.setImageBitmap(imageBitmap);
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                // Pass image to an ML Kit Vision API
                recognizeText(image).addOnCompleteListener(runnable -> {imageProxy.close();});
            }

        }
    }

    private int calculateCheckDigit(String str){
        int sum = 0;
        for(int i = 0; i < str.length(); i++){
            char c = str.charAt(i);
            sum += (c == '<' ? 0 : Character.isDigit(c) ? c - '0' : c - 'A' + 10) * WEIGHTS[i % WEIGHTS.length];
        }
        return sum % 10;
    }

    private ArrayList<String> produceCombinations(String str){
        ArrayList<String> combinations = new ArrayList<>();
        combinations.add(str);
        for(int i = 0; i < combinations.get(0).length(); i++){
            if(combinations.get(0).charAt(i) == '0' || combinations.get(0).charAt(i)  == 'O'){
                ArrayList<String> newCombs = new ArrayList<>();
                for(String comb : combinations){
                    StringBuilder newComb = new StringBuilder(comb);
                    newComb.setCharAt(i, '0');
                    if(!combinations.contains(newComb.toString())) {
                        newCombs.add(newComb.toString());
                    }
                    newComb.setCharAt(i, 'O');
                    if(!combinations.contains(newComb.toString())) {
                        newCombs.add(newComb.toString());
                    }
                }
                combinations.addAll(newCombs);
            }
        }
        return combinations;
    }

    private String findCorrectPassportNumberFromCheck(ArrayList<String> combs, int checkDigit) {
        for(String comb : combs) {
            if(checkDigit == calculateCheckDigit(comb)){
                return comb;
            }
        }
        return null;
    }

    private String findCorrectPersonalNumberFromCheck(ArrayList<String> combs, int checkDigit, char checkChar) {
        for(String comb : combs) {
            if(checkDigit == calculateCheckDigit(comb) || (calculateCheckDigit(comb) == 0 && checkChar == '<')){
                return comb;
            }
        }
        return null;
    }

    private String trimLeading(String s, char c) {
        int index;
        for (index = 0; index < s.length() - 1; index++) {
            if (s.charAt(index) != c) {
                break;
            }
        }
        return s.substring(index);
    }

    private String trimTrailing(String s, char c) {
        int index;
        for (index = s.length() - 1; index > 0; index--) {
            if (s.charAt(index) != c) {
                break;
            }
        }
        return s.substring(0, index + 1);
    }

    private Task<Text> recognizeText(InputImage image){
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        return recognizer.process(image).addOnSuccessListener(visionText -> {
            String text = visionText.getText();
            String potentialStart = "";
            String potentialEnd = "";

            String fullName = "";

            if(text.length() > 0) {
                String[] split = text.split("\n");
                for (String line : split) {

                    String trimmedLine = line.replaceAll(" ", "").toUpperCase().replaceAll("«","<");
                    //Log.e("check", "TL: " + trimmedLine);

                    if(trimmedLine.startsWith("P<") && trimmedLine.length() > 10 && fullName.length() == 0) {
                        String nameLine = trimmedLine.substring(5).replaceAll("0","O");
                        if(trimmedLine.contains("<<<")) {
                            nameLine = nameLine.split("<<<")[0];
                        }

                        if(nameLine.contains("<<")){
                            String[] nameParts =  nameLine.split("<<");
                            for(int i = 1; i < nameParts.length; i++){
                                fullName = fullName + nameParts[i].replaceAll("<"," ");
                            }
                            fullName = fullName + " " + nameParts[0];
                        } else {
                            fullName = nameLine.replaceAll("<", " ");
                        }
                    }

                    if(trimmedLine.length() >= 28 && trimmedLine.length() != 44) {
                        String tempPassportNumber = trimmedLine.substring(0, 9);
                        ArrayList<String> tempPossibilities = produceCombinations(tempPassportNumber);
                        int tempCheckDigit10 = Character.getNumericValue(trimmedLine.charAt(9));
                        tempPassportNumber = findCorrectPassportNumberFromCheck(tempPossibilities, tempCheckDigit10);
                        if(tempPassportNumber != null) {
                            potentialStart = trimmedLine;
                        }
                    } else {
                        String tempEnd = line.replaceAll("<", "");

                        if((tempEnd.length() == 1 || tempEnd.length() == 2) && Pattern.compile("[0-9]+").matcher(tempEnd).matches()){
                            potentialEnd = tempEnd;
                        }
                    }

                    if(potentialStart.length() > 0 && potentialEnd.length() > 0){
                        trimmedLine = potentialStart.substring(0, 28) + "<<<<<<<<<<<<<<" + (potentialEnd.length() == 1 ? "<" : "") + potentialEnd;
                        potentialStart = "";
                        potentialEnd = "";
                    }

                    if(trimmedLine.length() == 44){
                        if(trimmedLine.charAt(28) == '<' && (trimmedLine.charAt(42) == '0' || trimmedLine.charAt(42) == '<')){
                            trimmedLine = trimmedLine.substring(0, 28) + "<<<<<<<<<<<<<<" + trimmedLine.substring(42);
                        }
                        String passportNumber = trimmedLine.substring(0, 9);
                        ArrayList<String> possibilities = produceCombinations(passportNumber);
                        int checkDigit10 = Character.getNumericValue(trimmedLine.charAt(9));
                        passportNumber = findCorrectPassportNumberFromCheck(possibilities, checkDigit10);
                        if(passportNumber != null){
                            trimmedLine = passportNumber + trimmedLine.substring(9);
                            String nationality = trimmedLine.substring(10, 13).replaceAll("0","O");
                            trimmedLine = trimmedLine.substring(0, 10) + nationality + trimmedLine.substring(13);
                            String dob = trimmedLine.substring(13, 19).replaceAll("O", "0");
                            trimmedLine = trimmedLine.substring(0, 13) + dob + trimmedLine.substring(19);
                            int checkDigit20 = Character.getNumericValue(trimmedLine.charAt(19));
                            if(checkDigit20 == calculateCheckDigit(dob)){
                                char sex = trimmedLine.charAt(20);
                                String passportExpiry = trimmedLine.substring(21, 27).replaceAll("O", "0");
                                trimmedLine = trimmedLine.substring(0, 21) + passportExpiry + trimmedLine.substring(27);
                                int checkDigit28 = Character.getNumericValue(trimmedLine.charAt(27));
                                if(checkDigit28 == calculateCheckDigit(passportExpiry)){
                                    String personalNumber = trimmedLine.substring(28, 42);
                                    int checkDigit43 = Character.getNumericValue(trimmedLine.charAt(42));
                                    ArrayList<String> pnPossibilities = produceCombinations(personalNumber);
                                    personalNumber = findCorrectPersonalNumberFromCheck(pnPossibilities, checkDigit43, trimmedLine.charAt(42));
                                    if(personalNumber != null) {
                                        int checkDigit44 = Character.getNumericValue(trimmedLine.charAt(43));
                                        String str = trimmedLine.substring(0, 10) + trimmedLine.substring(13, 20) + trimmedLine.substring(21, 43);
                                        if(checkDigit44 == calculateCheckDigit(str) && fullName.length() > 0){
                                            //Log.e("MRZ", "Scanned digit: " + checkDigit44);
                                            //Log.e("MRZ", "Calculated digit : " + calculateCheckDigit(str));
                                            Intent intent = new Intent(getApplicationContext(),InfoActivity.class);
                                            intent.putExtra("passportNumber", passportNumber.replaceAll("<",""));
                                            intent.putExtra("nationality", nationality);
                                            intent.putExtra("dob", dob);
                                            intent.putExtra("sex", sex);
                                            intent.putExtra("passportExpiry", passportExpiry);
                                            intent.putExtra("personalNumber", personalNumber);
                                            intent.putExtra("name", fullName);
                                            startActivity(intent);
                                            return;
                                        }
                                        
                                    }
                                }
                            }
                        }


                        /*Log.e("check", "Passport Number: " + passportNumber);
                        Log.e("check", "Scanned digit: " + checkDigit10);
                        Log.e("check", "Calculated digit : " + calculateCheckDigit(passportNumber));*/
                    }

                }
            }

            //Log.e("check", visionText.getText());
            //binding.wordFence.clearBoxes();

        })
                .addOnFailureListener(
                        e -> {
                            // Task failed with an exception
                            // ...
                        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //cameraHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private Bitmap rotateBitmap(Bitmap bitmap, float rotation) {
        Matrix matrix = new Matrix();
        matrix.preRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

}