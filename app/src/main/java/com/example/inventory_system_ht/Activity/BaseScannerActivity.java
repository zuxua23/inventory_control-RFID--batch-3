package com.example.inventory_system_ht.Activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;

import com.densowave.scannersdk.Common.CommScanner;
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.R;
import com.google.android.material.snackbar.Snackbar;

public abstract class BaseScannerActivity extends AppCompatActivity {

    private Dialog loadingDialog;
    private ToneGenerator toneGen;
    private Vibrator vibrator;


    // --- UTILS KONEKSI ---
    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities cap = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return cap != null && (cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        }
        return false;
    }

    // --- UI FEEDBACK (SNACKBAR) ---
    public void showSagaFeedback(String pesan, boolean isSuccess) {
        View rootView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, pesan, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(isSuccess ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));
        snackbar.setTextColor(Color.WHITE);
        snackbar.show();
    }

    // --- LOADING DIALOG ---
    public void showLoading() {
        if (loadingDialog == null) {
            loadingDialog = new Dialog(this);
            loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            loadingDialog.setContentView(R.layout.dialog_loading);
            loadingDialog.setCancelable(false);
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                loadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
        if (!loadingDialog.isShowing()) loadingDialog.show();
    }

    public void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }

    // --- API ERROR HANDLER ---
    public void handleApiError(int statusCode) {
        hideLoading();
        if (statusCode == 401) {
            showSagaFeedback("Session Expired! Please login again.", false);
            PrefManager pref = new PrefManager(this);
            pref.clearSession();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (statusCode >= 500) {
            showSagaFeedback("Server Error (500): Backend C# is crashing, contact IT!", false);
        } else {
            showSagaFeedback("Error: " + statusCode + ". Check your data/request.", false);
        }
    }

    public void handleFailure(Throwable t) {
        hideLoading();
        if (t instanceof java.net.SocketTimeoutException) {
            showSagaFeedback("Timeout: Internet is a garbage warehouse, try again!", false);
        } else if (t instanceof java.io.IOException) {
            showSagaFeedback("Network Error: Check your WiFi/Data connection!", false);
        } else {
            showSagaFeedback("Failure: " + t.getMessage(), false);
        }
    }

    public void playScanFeedback(int type) {
        if (toneGen == null) toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        if (vibrator == null) vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        switch (type) {
            case 0: // SUCCESS: Beep Tinggi & Pendek
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
                break;

            case 1: // DUPLICATE: Beep 2x Pendek
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 200);
                break;

            case 2: // ERROR/FAILED: Beep Rendah Panjang + Getar
                toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 400);
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(500);
                    }
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGen != null) {
            toneGen.release();
            toneGen = null;
        }
    }
    public int getHTBatteryLevel() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }
}