package com.example.plugin_example;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/** PluginExamplePlugin */
public class PluginExamplePlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
  private static final String TAG = "PluginExamplePlugin";
  private static final int REQUEST_CODE_SCAN = 1001;

  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private Activity activity;
  private Result pendingResult;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "plugin_example");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else if (call.method.equals("scanBarcode")) {
      if (activity == null) {
        result.error("NO_ACTIVITY", "Activity not available", null);
        return;
      }
      pendingResult = result;
      startBarcodeScanner();
    } else {
      result.notImplemented();
    }
  }

  private void startBarcodeScanner() {
    Intent intent = new Intent(activity, BarcodeScannerActivity.class);
    activity.startActivityForResult(intent, REQUEST_CODE_SCAN);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    this.activity = binding.getActivity();
    binding.addActivityResultListener(this);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity();
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivity() {
    if (activity != null) {
      activity = null;
    }
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode == REQUEST_CODE_SCAN && pendingResult != null) {
      if (resultCode == Activity.RESULT_OK && data != null) {
        String barcodeValue = data.getStringExtra("barcode_result");
        if (barcodeValue != null) {
          pendingResult.success(barcodeValue);
        } else {
          pendingResult.error("NO_RESULT", "No barcode detected", null);
        }
      } else {
        pendingResult.error("SCAN_CANCELLED", "Barcode scan cancelled", null);
      }
      pendingResult = null;
      return true;
    }
    return false;
  }
}