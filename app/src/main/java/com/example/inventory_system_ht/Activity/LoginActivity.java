package com.example.inventory_system_ht.Activity;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

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
            showWarning("Username & Password are required!");
            return;
        }

        if (!isNetworkConnected()) {
            showWarning("Login Failed: Your internet is down");
            return;
        }
        showLoading();

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        AuthModels.LoginRequest loginRequest = new AuthModels.LoginRequest(username, password);

        apiService.login(loginRequest).enqueue(new Callback<AuthModels.LoginResponse>() {
            @Override
            public void onResponse(Call<AuthModels.LoginResponse> call, Response<AuthModels.LoginResponse> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthModels.LoginResponse body = response.body();
                    String token = body.getToken();
                    AuthModels.UserModel user = body.getUser();

                    if (token == null || token.isEmpty() || user == null) {
                        showError("Login Failed: Invalid server response");
                        return;
                    }

                    String fullName = user.getUsrFullname();
                    if (fullName == null || fullName.trim().isEmpty()) {
                        fullName = user.getUsrName();
                    }

                    // Serialize permissions ke JSON buat disimpen
                    String permissionsJson = new com.google.gson.Gson()
                            .toJson(user.getPermissions() != null ? user.getPermissions() : new java.util.ArrayList<>());

                    prefManager.saveUserSession(
                            token,
                            user.getUsrId(),
                            user.getUsrName(),
                            fullName,
                            user.getPrimaryRole(),
                            permissionsJson
                    );

                    showSuccess("Login Successful! Welcome " + fullName);

                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    handleApiError(response.code());
                    String errorMsg = "Login Failed: Wrong User/Pass or server problem.";
                    if (response.code() == 401) {
                        errorMsg = "Unauthorized: Your account is not registered!";
                    }
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<AuthModels.LoginResponse> call, Throwable t) {
                hideLoading();
                handleFailure(t);
                showError("Server Timeout");
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
                showWarning("Fill in the Server first");
                return;
            }

            if (!ipAddress.startsWith("http://") && !ipAddress.startsWith("https://")) {
                showError("Wrong Format! Must start with http://");
                return;
            }

            showLoading();

            prefManager.saveIp(ipAddress);

            ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
            apiService.ping().enqueue(new Callback<GeneralResponse>() {
                @Override
                public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                    hideLoading();
                    if (response.isSuccessful()) {
                        showSuccess("Server Connected!");
                    } else {
                        showError("Server unreachable! Code: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<GeneralResponse> call, Throwable t) {
                    hideLoading();
                    showError("Cannot connect to server!");
                }
            });
        });

        btnApplyIp.setOnClickListener(v -> {
            String ipAddress = etIpAPI.getText().toString().trim();

            if (!ipAddress.isEmpty()) {
                prefManager.saveIp(ipAddress);

                showSuccess("Server saved successfully");
                dialog.dismiss();
            } else {
                showWarning("Server cannot be empty");
            }
        });

        dialog.show();
    }
}