package com.example.plugin_example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeScannerActivity extends AppCompatActivity {
  private static final String TAG = "BarcodeScannerActivity";
  
  private PreviewView previewView;
  private BarcodeScanner barcodeScanner;
  private boolean isScanning = false;
  private TextView statusText;
  private Button closeButton;
  private ExecutorService cameraExecutor;
  private ProcessCameraProvider cameraProvider;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN
    );
    setContentView(R.layout.activity_barcode_scanner);
    
    previewView = findViewById(R.id.preview_view);
    statusText = findViewById(R.id.status_text);
    closeButton = findViewById(R.id.close_button);
    
    closeButton.setOnClickListener(v -> {
      setResult(Activity.RESULT_CANCELED);
      finish();
    });
    
    cameraExecutor = Executors.newSingleThreadExecutor();
    
    setupBarcodeScanner();
    startCamera();
  }
  
  private void setupBarcodeScanner() {
    BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13 |
            Barcode.FORMAT_EAN_8 |
            Barcode.FORMAT_UPC_A |
            Barcode.FORMAT_UPC_E |
            Barcode.FORMAT_CODE_128 |
            Barcode.FORMAT_CODE_39 |
            Barcode.FORMAT_ITF |
            Barcode.FORMAT_QR_CODE |
            Barcode.FORMAT_DATA_MATRIX
        )
        .build();
    
    barcodeScanner = BarcodeScanning.getClient(options);
    Log.d(TAG, "Barcode scanner initialized with Brazilian formats");
  }
  
  private void startCamera() {
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
    
    cameraProviderFuture.addListener(() -> {
      try {
        cameraProvider = cameraProviderFuture.get();
        bindCameraUseCases();
      } catch (Exception e) {
        Log.e(TAG, "Camera initialization failed", e);
        runOnUiThread(() -> statusText.setText("Erro ao inicializar câmera"));
      }
    }, ContextCompat.getMainExecutor(this));
  }
  
  private void bindCameraUseCases() {
    Preview preview = new Preview.Builder().build();
    preview.setSurfaceProvider(previewView.getSurfaceProvider());
    
    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
        .setTargetResolution(new Size(1280, 720))
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build();
    
    imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);
    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    cameraProvider.unbindAll();
    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    
    statusText.setText("Apontar a câmera para o código de barras");
  }
  
  private void analyzeImage(@NonNull ImageProxy imageProxy) {
    if (isScanning) {
      imageProxy.close();
      return;
    }
    
    @SuppressWarnings("UnsafeOptInUsageError")
    android.media.Image mediaImage = imageProxy.getImage();
    if (mediaImage != null) {
      InputImage image = InputImage.fromMediaImage(
          mediaImage, imageProxy.getImageInfo().getRotationDegrees());
      
      barcodeScanner.process(image)
          .addOnSuccessListener(barcodes -> {
            if (!isScanning && barcodes != null && !barcodes.isEmpty()) {
              isScanning = true;
              Barcode barcode = barcodes.get(0);
              String rawValue = barcode.getRawValue();
              if (rawValue != null && !rawValue.isEmpty()) {
                Log.d(TAG, "Barcode detected: " + rawValue + " (Format: " + barcode.getFormat() + ")");
                
                runOnUiThread(() -> {
                  Intent resultIntent = new Intent();
                  resultIntent.putExtra("barcode_result", rawValue);
                  setResult(Activity.RESULT_OK, resultIntent);
                  finish();
                });
              } else {
                isScanning = false;
              }
            }
          })
          .addOnFailureListener(e -> Log.e(TAG, "Barcode detection failed", e))
          .addOnCompleteListener(task -> imageProxy.close());
    } else {
      imageProxy.close();
    }
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    isScanning = false;
  }
  
  @Override
  protected void onPause() {
    super.onPause();
  }
  
  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (cameraExecutor != null) {
      cameraExecutor.shutdown();
    }
    if (barcodeScanner != null) {
      barcodeScanner.close();
      barcodeScanner = null;
    }
  }
}