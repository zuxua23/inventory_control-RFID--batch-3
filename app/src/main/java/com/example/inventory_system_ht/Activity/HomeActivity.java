package com.example.inventory_system_ht.Activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;

import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.R;

// 👇 EXTENDS KE BASE BIAR BISA PAKE showSagaFeedback & isNetworkConnected 👇
public class HomeActivity extends BaseScannerActivity {

    private ImageView ivProfile;
    private ImageButton btnStockIn, btnStockPrep, btnStockTaking, btnTagRegis, btnSearchItem;
    private TextView tvNamaOperator, tvRoleOperator, textViewStatusReader;
    private CardView cardStatus;
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Inisialisasi PrefManager
        prefManager = new PrefManager(this);

        // Init Views
        ivProfile = findViewById(R.id.ivProfile);
        btnStockIn = findViewById(R.id.ButtonStockIn);
        btnStockPrep = findViewById(R.id.ButtonStockPreparation);
        btnStockTaking = findViewById(R.id.ButtonStockTaking);
        btnTagRegis = findViewById(R.id.ButtonTagRegis);
        btnSearchItem = findViewById(R.id.ButtonSearchItem);
        cardStatus = findViewById(R.id.cardStatus);
        textViewStatusReader = findViewById(R.id.textViewStatusReader);
        tvNamaOperator = findViewById(R.id.textViewNamaOperator);
        tvRoleOperator = findViewById(R.id.textViewRoleOperator);

        // Ambil data User dari Session
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        String fullName = sharedPreferences.getString("USER_FULLNAME", "Guest");
        int roleId = sharedPreferences.getInt("ROLE_ID", 2);
        String roleName = (roleId == 1) ? "Administrator" : "Operator IT";

        // Set UI Data
        tvNamaOperator.setText("Welcome " + fullName);
        tvRoleOperator.setText(roleName);

        // Status Reader (Default False karena belum ada Sled)
        updateReaderStatus(false);

        // Cek Session Validitas
        if (!prefManager.isSessionValid()) {
            prefManager.clearSession();
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        ivProfile.setOnClickListener(v -> showLogoutPopup(v));

        // Listener Menu
        View.OnClickListener menuClickListener = v -> {
            // 👇 CEK INTERNET PAS KLIK MENU (Logic Mobile Flow) 👇
            if (!isNetworkConnected()) {
                showSagaFeedback("Warning: You're offline, bro! Check your connection..", false);
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

        // Handle Back Press
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

    private void showLogoutPopup(View anchorView) {
        CardView cardView = new CardView(this);
        cardView.setCardBackgroundColor(Color.parseColor("#C62828"));
        cardView.setRadius(40f);
        cardView.setCardElevation(8f);

        TextView tvLogout = new TextView(this);
        tvLogout.setText("Logout");
        tvLogout.setTextColor(Color.WHITE);
        tvLogout.setTextSize(16f);
        tvLogout.setPadding(50, 20, 50, 20);

        Typeface typeface = ResourcesCompat.getFont(this, R.font.raleway_bold);
        tvLogout.setTypeface(typeface);
        cardView.addView(tvLogout);

        PopupWindow popupWindow = new PopupWindow(cardView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.showAsDropDown(anchorView, 0, -20);

        cardView.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                cardView.setCardBackgroundColor(Color.parseColor("#8E1C1C"));
                cardView.animate().scaleX(0.95f).scaleY(0.95f).setDuration(50).start();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP ||
                    event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                cardView.setCardBackgroundColor(Color.parseColor("#C62828"));
                cardView.animate().scaleX(1f).scaleY(1f).setDuration(50).start();
            }
            return false;
        });

        cardView.setOnClickListener(v -> {
            popupWindow.dismiss();
            showLogoutConfirmationDialog();
        });
    }

    private void showLogoutConfirmationDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_alert_logout);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Button btnNo = dialog.findViewById(R.id.btnNo);
        Button btnYes = dialog.findViewById(R.id.btnYes);

        btnNo.setOnClickListener(v -> dialog.dismiss());

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            // Clear Session
            SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
            sharedPreferences.edit().clear().apply();
            prefManager.clearSession();

            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        dialog.show();
    }

    private void updateReaderStatus(boolean isConnected) {
        if (isConnected) {
            textViewStatusReader.setText("Reader Status : Connected");
            cardStatus.setCardBackgroundColor(getResources().getColor(R.color.green_button));
        } else {
            textViewStatusReader.setText("Reader Status : Not Connected");
            cardStatus.setCardBackgroundColor(Color.parseColor("#9E9E9E"));
        }
    }
}