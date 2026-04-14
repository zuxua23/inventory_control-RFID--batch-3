package com.example.inventory_system_ht.Activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.example.inventory_system_ht.Helper.ApiClient;
import com.example.inventory_system_ht.Helper.ApiService;
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.R;
import com.example.inventory_system_ht.Models.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseScannerActivity {

    private ImageButton btnSetting;
    private Button btnLogin;
    private EditText etUsername;
    private EditText etPassword;
    private PrefManager prefManager;
    @Override
    protected CommScanner getScannerInstance() {
        return null;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnSetting = findViewById(R.id.btnSetting);
        btnLogin = findViewById(R.id.btnLogin);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        prefManager = new PrefManager(this);

        if (prefManager.isSessionValid()) {
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
        }

        btnLogin.setOnClickListener(v -> performLogin());
        btnSetting.setOnClickListener(v -> showSettingDialog());
    }

    private void performLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showSagaFeedback("Username & Password are required!", false);
            return;
        }

        if (!isNetworkConnected()) {
            showSagaFeedback("Login Failed: Your internet is down, check WiFi/Data first!", false);
            return;
        }
        showLoading();
        showSagaFeedback("Authenticating with server...", true);

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        AuthModels.LoginRequest loginRequest = new AuthModels.LoginRequest(username, password);

        apiService.login(loginRequest).enqueue(new Callback<AuthModels.LoginResponse>() {
            @Override
            public void onResponse(Call<AuthModels.LoginResponse> call, Response<AuthModels.LoginResponse> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {

                    prefManager.saveToken("SESSION_ACTIVE");

                    showSagaFeedback("Login Successful!", true);

                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    finish();
                } else {
                    handleApiError(response.code());
                    String errorMsg = "Login Failed: Wrong User/Pass or server problem.";
                    if (response.code() == 401) {
                        errorMsg = "Unauthorized: Your account is not registered!";
                    }
                    showSagaFeedback(errorMsg, false);
                }
            }

            @Override
            public void onFailure(Call<AuthModels.LoginResponse> call, Throwable t) {
                hideLoading();
                handleFailure(t);
                showSagaFeedback("Server Timeout: API is not responding!", false);
                Log.e("API_ERROR", "Login Failed", t);
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

        etIpAPI.setText(prefManager.getIp());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        buttonCekIpInside.setOnClickListener(v -> {
            String ipAddress = etIpAPI.getText().toString().trim();

            if (ipAddress.isEmpty()) {
                showSagaFeedback("Fill in the Server first, bro", false);
                return;
            }

            if (ipAddress.startsWith("http://")) {
                showSagaFeedback("The URL format is correct! Please apply..", true);
            } else {
                showSagaFeedback("Wrong Format! Must use http://", false);
            }
        });

        btnApplyIp.setOnClickListener(v -> {
            String ipAddress = etIpAPI.getText().toString().trim();

            if (!ipAddress.isEmpty()) {
                prefManager.saveIp(ipAddress);

                showSagaFeedback("Server saved successfully: " + ipAddress, true);
                dialog.dismiss();
            } else {
                showSagaFeedback("Server cannot be empty", false);
            }
        });

        dialog.show();
    }
}