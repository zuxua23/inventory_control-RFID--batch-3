package com.example.inventory_system_ht.Activity;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

public abstract class BaseScannerActivity extends AppCompatActivity {

    // Fungsi cek internet
    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities cap = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return cap != null && (cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        }
        return false;
    }

    // Snackbar Khusus Saga (Hijau = Sukses, Merah = Rollback/Gagal)
    public void showSagaFeedback(String pesan, boolean isSuccess) {
        View rootView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, pesan, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(isSuccess ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));
        snackbar.setTextColor(Color.WHITE);
        snackbar.show();
    }
}