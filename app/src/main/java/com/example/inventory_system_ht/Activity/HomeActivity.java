package com.example.inventory_system_ht.Activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;

import com.densowave.scannersdk.Common.CommScanner;
import com.example.inventory_system_ht.Helper.AppDao;
import com.example.inventory_system_ht.Helper.AppDatabase;
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.R;

public class HomeActivity extends BaseScannerActivity {
    private LinearLayout btnStockIn, btnStockPrep, btnStockTaking, btnTagRegis, btnSearchItem;
    private TextView tvNamaOperator, tvRoleOperator;
    private PrefManager prefManager;
    private ImageButton btnLogout;
    private AppDao appDao;

    private CommScanner mCommScanner;

    @Override
    protected CommScanner getScannerInstance() {
        return mCommScanner;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        prefManager = new PrefManager(this);

        if (!prefManager.isSessionValid()) {
            prefManager.clearSession();
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        btnStockIn      = findViewById(R.id.ButtonStockIn);
        btnStockPrep    = findViewById(R.id.ButtonStockPreparation);
        btnStockTaking  = findViewById(R.id.ButtonStockTaking);
        btnTagRegis     = findViewById(R.id.ButtonTagRegis);
        btnSearchItem   = findViewById(R.id.ButtonSearchItem);
        tvNamaOperator  = findViewById(R.id.textViewNamaOperator);
        tvRoleOperator  = findViewById(R.id.textViewRoleOperator);
        btnLogout       = findViewById(R.id.btnLogout);

        appDao = AppDatabase.getDatabase(this).appDao();

        String fullName = prefManager.getFullName();
        String roleName = prefManager.getRoleName();

        tvNamaOperator.setText("Welcome " + fullName);
        tvRoleOperator.setText(roleName);

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        }

        View.OnClickListener menuClickListener = v -> {
            if (!isNetworkConnected()) {
                showWarning("You're offline!");
            }

            int id = v.getId();
            Intent intent = null;

            if (id == R.id.ButtonStockIn) {
                intent = new Intent(HomeActivity.this, StockInActivity.class);
            } else if (id == R.id.ButtonStockPreparation) {
                intent = new Intent(HomeActivity.this, StockPrepActivity.class);
            } else if (id == R.id.ButtonStockTaking) {
                intent = new Intent(HomeActivity.this, StockTakingActivity.class);
            } else if (id == R.id.ButtonTagRegis) {
                intent = new Intent(HomeActivity.this, TagRegisActivity.class);
            } else if (id == R.id.ButtonSearchItem) {
                intent = new Intent(HomeActivity.this, SearchItemActivity.class);
            }

            if (intent != null) {
                startActivity(intent);
            }
        };

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(HomeActivity.this)
                        .setTitle("Exit Application")
                        .setMessage("Are you sure you want to exit Inventory Control Application?")
                        .setPositiveButton("Yes", (dialog, which) -> finish())
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        btnStockIn.setOnClickListener(menuClickListener);
        btnStockPrep.setOnClickListener(menuClickListener);
        btnStockTaking.setOnClickListener(menuClickListener);
        btnTagRegis.setOnClickListener(menuClickListener);
        btnSearchItem.setOnClickListener(menuClickListener);
    }

    private void showLogoutConfirmationDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_alert_logout);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Button btnNo  = dialog.findViewById(R.id.btnNo);
        Button btnYes = dialog.findViewById(R.id.btnYes);

        btnNo.setOnClickListener(v -> dialog.dismiss());

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            prefManager.clearSession();

            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        dialog.show();
    }
    @Override
    protected void onResume() {
        super.onResume();
        updateReaderBattery(findViewById(R.id.ivReaderBattery));
    }


}