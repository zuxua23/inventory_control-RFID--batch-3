package com.example.inventory_system_ht.Activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log; // <-- IMPORT LOG DI SINI
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.inventory_system_ht.Helper.ApiClient;
import com.example.inventory_system_ht.Helper.ApiService;
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.Models.LoginRequest;
import com.example.inventory_system_ht.Models.LoginResponse;
import com.example.inventory_system_ht.R;
import com.google.android.material.snackbar.Snackbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseScannerActivity {
    private static final String TAG = "LoginActivity_DEBUG";

    private ImageButton btnSetting;
    private Button btnLogin;
    private EditText etUsername;
    private EditText etPassword;
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inisialisasi View & PrefManager
        btnSetting = findViewById(R.id.btnSetting);
        btnLogin = findViewById(R.id.btnLogin);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        prefManager = new PrefManager(this);

        Log.d(TAG, "onCreate: Aplikasi dibuka. IP tersimpan saat ini: " + prefManager.getIp());

        // Cek apakah session masih valid (8 jam), kalau iya langsung ke Home
        if (prefManager.isSessionValid()) {
            Log.d(TAG, "onCreate: Session masih valid, redirect ke HomeActivity");
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
        }

        // Listener tombol Login
        btnLogin.setOnClickListener(v -> performLogin());

        // Listener tombol Setting IP
        btnSetting.setOnClickListener(v -> showSettingDialog());
    }

    private void performLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 1. Validasi Input Kosong
        if (username.isEmpty() || password.isEmpty()) {
            showSagaFeedback("Username & Password wajib diisi, bre!", false);
            return;
        }

        // 2. Cek Koneksi Internet (Pake fungsi sakti dari BaseScannerActivity)
        if (!isNetworkConnected()) {
            showSagaFeedback("Gagal Login: Internet lu mati, cek WiFi/Data dulu!", false);
            return;
        }

        Log.d(TAG, "performLogin: Menembak API ke -> " + prefManager.getIp());
        showSagaFeedback("Authenticating with server...", true);

        // Setup Retrofit
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        LoginRequest loginRequest = new LoginRequest(username, password);

        // Hit API Login
        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                Log.d(TAG, "onResponse: HTTP Code " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "onResponse: Login Sukses!");

                    // Simpan Token JWT
                    prefManager.saveToken(response.body().getToken());

                    showSagaFeedback("Login Berhasil! Selamat bekerja, Jan.", true);

                    // Pindah ke HomeActivity
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    finish();
                } else {
                    Log.e(TAG, "onResponse: Login Gagal.");
                    String errorMsg = "Login Gagal: User/Pass salah atau server bermasalah.";
                    if (response.code() == 401) {
                        errorMsg = "Unauthorized: Akun lu gak terdaftar, bre!";
                    }
                    showSagaFeedback(errorMsg, false);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e(TAG, "onFailure: Koneksi Gagal/Timeout!", t);
                showSagaFeedback("Server Timeout: API Backend gak ngerespon!", false);
            }
        });
    }

    private void showSettingDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_setting);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnApplyIp = dialog.findViewById(R.id.btnApplyIp);
        EditText etIpAPI = dialog.findViewById(R.id.etIpAPI);
        ImageButton buttonCekIpInside = dialog.findViewById(R.id.buttonCekIp);

        // Load IP lama
        etIpAPI.setText(prefManager.getIp());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        buttonCekIpInside.setOnClickListener(v -> {
            String ipAddress = etIpAPI.getText().toString().trim();

            if (ipAddress.isEmpty()) {
                Snackbar.make(dialog.findViewById(android.R.id.content), "Isi IP-nya dulu, bre", Snackbar.LENGTH_SHORT).show();
                return;
            }

            if (ipAddress.startsWith("http://") || ipAddress.startsWith("https://")) {
                Snackbar.make(dialog.findViewById(android.R.id.content), "Format URL bener! Silahkan Apply.", Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(Color.parseColor("#01C470"))
                        .show();
            } else {
                Snackbar.make(dialog.findViewById(android.R.id.content), "Format Salah! Wajib pake http:// atau https://", Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(Color.parseColor("#C62828"))
                        .show();
            }
        });

        btnApplyIp.setOnClickListener(v -> {
            String ipAddress = etIpAPI.getText().toString().trim();

            if (!ipAddress.isEmpty()) {
                Log.d(TAG, "Setting: IP diubah ke -> " + ipAddress);
                prefManager.saveIp(ipAddress);

                showSagaFeedback("API saved successfully: " + ipAddress, true);
                dialog.dismiss();
            } else {
                Snackbar.make(dialog.findViewById(android.R.id.content), "API Gak boleh kosong!", Snackbar.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}