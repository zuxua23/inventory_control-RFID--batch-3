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

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity_DEBUG"; // <-- TAG BUAT FILTER LOGCAT

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

        // Cek apakah session 8 jam masih valid, kalau valid langsung ke Home
        if (prefManager.isSessionValid()) {
            Log.d(TAG, "onCreate: Session masih valid, langsung lempar ke HomeActivity");
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
        }

        // Listener untuk tombol Login
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin(v);
            }
        });

        // Listener untuk tombol Setting IP
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingDialog();
            }
        });
    }

    private void performLogin(View v) {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Snackbar.make(v, "Username sama Password isi dulu bre!", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String currentIp = prefManager.getIp();
        Log.d(TAG, "performLogin: Mulai proses login...");
        Log.d(TAG, "performLogin: Menembak API ke Base URL -> " + currentIp);
        Log.d(TAG, "performLogin: Payload -> Username: " + username + " | Password Length: " + password.length());

        // Setup Retrofit dengan IP dari PrefManager
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        LoginRequest loginRequest = new LoginRequest(username, password);

        // Hit API Login
        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                Log.d(TAG, "onResponse: Hit API Selesai. HTTP Status Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "onResponse: Login Sukses! Token didapat: " + response.body().getToken());

                    // Simpan Token JWT ke PrefManager
                    prefManager.saveToken(response.body().getToken());

                    Snackbar.make(v, "Login Berhasil!", Snackbar.LENGTH_SHORT).show();

                    // Pindah ke HomeActivity
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    finish();
                } else {
                    Log.e(TAG, "onResponse: Login Gagal dari Server (Bisa jadi pass salah / url salah)");
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "onResponse: Pesan Error Server -> " + response.errorBody().string());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Snackbar.make(v, "Login Gagal! Cek User/Pass atau Database.", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e(TAG, "onFailure: KONEKSI GAGAL TOTAL / RTO!");
                Log.e(TAG, "onFailure: Detail Penyebab -> ", t);

                Snackbar.make(v, "Gagal konek ke API: " + t.getMessage(), Snackbar.LENGTH_LONG).show();
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

        // Set text EditText dengan IP yang sudah tersimpan sebelumnya
        etIpAPI.setText(prefManager.getIp());

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        buttonCekIpInside.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ipAddress = etIpAPI.getText().toString().trim();

                if (ipAddress.isEmpty()) {
                    Snackbar.make(dialog.findViewById(android.R.id.content), "Isi dulu alamat IP-nya bre!", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if (ipAddress.startsWith("http://") || ipAddress.startsWith("https://")) {
                    Snackbar.make(dialog.findViewById(android.R.id.content), "Format URL Bener! Bisa lanjut Apply.", Snackbar.LENGTH_SHORT)
                            .setBackgroundTint(Color.parseColor("#01C470")) // Warna ijo
                            .show();
                } else {
                    Snackbar.make(dialog.findViewById(android.R.id.content), "Format URL salah! Harus pake http://", Snackbar.LENGTH_SHORT)
                            .setBackgroundTint(Color.parseColor("#C62828")) // Warna merah
                            .show();
                }
            }
        });

        btnApplyIp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ipAddress = etIpAPI.getText().toString().trim();

                if (!ipAddress.isEmpty()) {
                    Log.d(TAG, "Dialog Setting: IP diubah menjadi -> " + ipAddress);
                    // Simpan IP baru ke SharedPreferences
                    prefManager.saveIp(ipAddress);

                    // Tampilkan pesan sukses di Activity utama (bukan di dialog)
                    View rootView = findViewById(android.R.id.content);
                    Snackbar.make(rootView, "IP Berhasil disimpan: " + ipAddress, Snackbar.LENGTH_SHORT).show();

                    dialog.dismiss();
                } else {
                    Snackbar.make(dialog.findViewById(android.R.id.content), "IP gak boleh kosong!", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }
}