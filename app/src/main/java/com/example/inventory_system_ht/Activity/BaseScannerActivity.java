package com.example.inventory_system_ht.Activity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.Window;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.densowave.scannersdk.Common.CommScanner;
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.R;
import com.google.android.material.snackbar.Snackbar;

public abstract class BaseScannerActivity extends AppCompatActivity {

    private Dialog loadingDialog;
    private ToneGenerator toneGen;
    private Vibrator vibrator;
    private PopupWindow activePowerPopup = null;

    protected abstract CommScanner getScannerInstance();

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities cap = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return cap != null && (cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        }
        return false;
    }

    public void showSagaFeedback(String pesan, boolean isSuccess) {
        View rootView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, pesan, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(isSuccess ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));
        snackbar.setTextColor(Color.WHITE);

        snackbar.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
        View snackbarView = snackbar.getView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
        params.gravity = Gravity.TOP;
        params.setMargins(30, 80, 30, 0);
        snackbarView.setLayoutParams(params);
        snackbarView.setTranslationY(-250f);

        snackbarView.animate()
                .translationY(0f)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.5f))
                .setDuration(400)
                .start();
        snackbar.show();
    }

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

    public void handleApiError(int statusCode) {
        hideLoading();
        if (statusCode == 401) {
            showSagaFeedback("Session expired, please login again", false);
            PrefManager pref = new PrefManager(this);
            pref.clearSession();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (statusCode >= 500) {
            showSagaFeedback("Server is having issues, please try again later", false);
        } else if (statusCode == 403) {
            showSagaFeedback("You don't have permission for this action", false);
        } else if (statusCode == 404) {
            showSagaFeedback("Data not found", false);
        } else {
            showSagaFeedback("Something went wrong, please check your input", false);
        }
    }

    public void handleApiError(retrofit2.Response<?> response) {
        hideLoading();
        int statusCode = response.code();

        // 401 tetep handle khusus karena harus logout
        if (statusCode == 401) {
            PrefManager pref = new PrefManager(this);
            pref.clearSession();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            showSagaFeedback("Session expired, please login again", false);
            return;
        }

        // sisanya: ambil dari body server aja
        String msg = com.example.inventory_system_ht.Helper.ErrorParser.getMessage(response);
        showSagaFeedback(msg, false);
    }

    public void handleFailure(Throwable t) {
        hideLoading();
        if (t instanceof java.net.SocketTimeoutException) {
            showSagaFeedback("Connection timeout, please check your internet", false);
        } else if (t instanceof java.net.ConnectException) {
            showSagaFeedback("Cannot reach server, is it online?", false);
        } else if (t instanceof java.io.IOException) {
            showSagaFeedback("Network error, check your WiFi/Data connection", false);
        } else {
            showSagaFeedback("Something went wrong, please try again", false);
        }
    }

    public void playScanFeedback(int type) {
        if (toneGen == null) toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        if (vibrator == null) vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        switch (type) {
            case 0: // SUCCESS:
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
                break;
            case 1: // DUPLICATE
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 150);
                break;
            case 2: // ERROR/FAILED
                toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 300);
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

    public void setupPowerDropdown(CardView btnPowerDropdown, @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switchRfid, TextView tvPowerLevel) {
        btnPowerDropdown.setVisibility(View.GONE);

        switchRfid.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                CommScanner currentScanner = getScannerInstance();
                boolean isRfidReady = (currentScanner != null && currentScanner.getRFIDScanner() != null);

                if (!isRfidReady) {
                    showSagaFeedback("HT not Connected to Reader RFID", false);
                    switchRfid.setChecked(false);
                    return;
                }
                btnPowerDropdown.setVisibility(View.VISIBLE);
            } else {
                btnPowerDropdown.setVisibility(View.GONE);
            }
            showSagaFeedback(isChecked ? "Mode RFID: ON" : "Mode RFID: OFF", true);
        });

        List<String> powerList = new ArrayList<>(Arrays.asList(
                "10 dBm", "15 dBm", "20 dBm", "25 dBm", "27 dBm"
        ));
        btnPowerDropdown.setOnClickListener(v ->
                showPowerDropdownPopup(btnPowerDropdown, powerList, tvPowerLevel));
    }

    private void showPowerDropdownPopup(View anchor, List<String> items, TextView tvPowerLevel) {
        View popupView = getLayoutInflater().inflate(R.layout.dropdown_popup, null);
        RecyclerView rv = popupView.findViewById(R.id.rvDropdown);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setNestedScrollingEnabled(true);

        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View v = getLayoutInflater().inflate(R.layout.item_dropdown, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                TextView tv = holder.itemView.findViewById(R.id.tvDropdownItem);
                tv.setText(items.get(position));
                holder.itemView.setOnClickListener(v -> {
                    tvPowerLevel.setText(items.get(position));
                    if (activePowerPopup != null) activePowerPopup.dismiss();
                });
            }
            @Override
            public int getItemCount() { return items.size(); }
        });

        int itemHeightPx = (int) (56 * getResources().getDisplayMetrics().density);
        int maxHeight    = itemHeightPx * 4;

        PopupWindow popup = new PopupWindow(
                popupView,
                anchor.getWidth(),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(16f);
        popup.setOutsideTouchable(true);

        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(anchor.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        popup.setHeight(Math.min(popupView.getMeasuredHeight(), maxHeight));
        popup.showAsDropDown(anchor, 0, 6);
        activePowerPopup = popup;
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