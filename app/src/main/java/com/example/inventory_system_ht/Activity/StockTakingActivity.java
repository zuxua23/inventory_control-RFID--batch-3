package com.example.inventory_system_ht.Activity;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.densowave.scannersdk.Barcode.BarcodeData;
import com.densowave.scannersdk.Barcode.BarcodeDataReceivedEvent;
import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.BarcodeDataDelegate;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.densowave.scannersdk.RFID.RFIDData;
import com.densowave.scannersdk.RFID.RFIDDataReceivedEvent;

import com.example.inventory_system_ht.R;

import java.util.List;

/**
 * StockTakingActivity: Handles stock adjustments.
 * Integrated with BaseScannerActivity & Saga Pattern Logic.
 */
public class StockTakingActivity extends BaseScannerActivity implements BarcodeDataDelegate, RFIDDataDelegate {

    private ImageView btnBack;
    private Switch switchRfid;
    private CardView cardlistTag, btnRefresh;
    private EditText resultScan;

    // SDK & UTILS GLOBAL
    private CommScanner mCommScanner;
    private ToneGenerator toneGen;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_taking_adjustment);

        // Initialize Beep Sound
        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        } catch (Exception e) { e.printStackTrace(); }

        // Initialize UI
        btnBack = findViewById(R.id.btnBack);
        switchRfid = findViewById(R.id.switchRfid);
        cardlistTag = findViewById(R.id.cardlistTag);
        btnRefresh = findViewById(R.id.btnRefresh);
        resultScan = findViewById(R.id.resultScan);

        // Default to Barcode Mode
        switchRfid.setChecked(false);

        setupScanner();

        btnBack.setOnClickListener(v -> finish());

        // SMART SWITCH LOGIC
        switchRfid.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Check Hardware: Is the Reader connected?
                    boolean isRfidReady = (mCommScanner != null && mCommScanner.getRFIDScanner() != null);

                    if (!isRfidReady) {
                        showSagaFeedback("Failed: HT is not connected to the RFID Reader yet!", false);

                        switchRfid.setOnCheckedChangeListener(null);
                        switchRfid.setChecked(false);
                        switchRfid.setOnCheckedChangeListener(this);
                        return;
                    }
                }

                String msg = isChecked ? "RFID Mode Active" : "Barcode Mode Active";
                showSagaFeedback(msg, true);
                resultScan.requestFocus();
            }
        });

        // REFRESH DATA (WITH INTERNET CHECK)
        btnRefresh.setOnClickListener(v -> {
            if (!isNetworkConnected()) {
                showSagaFeedback("Refresh Failed: You are offline, bro!", false);
            } else {
                showSagaFeedback("Updating stock data from server...", true);
                // TODO: Hit API GET Stock here
            }
        });

        // TextWatcher for Laser Scanner (Keyboard Wedge)
        resultScan.setShowSoftInputOnFocus(false);
        resultScan.postDelayed(() -> resultScan.requestFocus(), 100);
        resultScan.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacksAndMessages(null);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String data = s.toString().trim();
                if (data.isEmpty()) return;

                handler.postDelayed(() -> {
                    processScanResult(data);
                    resultScan.removeTextChangedListener(this);
                    resultScan.setText("");
                    resultScan.addTextChangedListener(this);
                    resultScan.requestFocus();
                }, 500);
            }
        });

        cardlistTag.setOnClickListener(v -> showAdjustmentDialog());
    }

    // ==========================================
    // SCAN PROCESS LOGIC (BARCODE/RFID)
    // ==========================================

    private void processScanResult(String data) {
        // Play Success Beep
        if (toneGen != null) toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150);

        // Show feedback for incoming data
        showSagaFeedback("Item detected: " + data, true);

        // TODO: Add to adjustment list or check local DB
    }

    // ==========================================
    // SDK DENSO IMPLEMENTATION
    // ==========================================

    private void setupScanner() {
        if (mCommScanner != null) {
            try {
                mCommScanner.getRFIDScanner().setDataDelegate(this);
                mCommScanner.getBarcodeScanner().setDataDelegate(this);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @Override
    public void onRFIDDataReceived(CommScanner scanner, RFIDDataReceivedEvent event) {
        if (!switchRfid.isChecked()) return;
        for (RFIDData data : event.getRFIDData()) {
            String epc = bytesToHexString(data.getUII());
            handler.post(() -> processScanResult(epc));
        }
    }

    @Override
    public void onBarcodeDataReceived(CommScanner scanner, BarcodeDataReceivedEvent event) {
        if (switchRfid.isChecked()) return;
        List<BarcodeData> dataList = event.getBarcodeData();
        if (!dataList.isEmpty()) {
            String barcode = new String(dataList.get(0).getData());
            handler.post(() -> processScanResult(barcode));
        }
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) { sb.append(String.format("%02X", b)); }
        return sb.toString();
    }

    // ==========================================
    // DIALOGS & SAGA LOGIC FLOW
    // ==========================================

    private void showAdjustmentDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_adj);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageButton btnFaq = dialog.findViewById(R.id.btnFaq);
        Button btnRemove = dialog.findViewById(R.id.btnRemove);
        Button btnAddManual = dialog.findViewById(R.id.btnAddManual);

        btnFaq.setOnClickListener(v -> showFaqDialog());

        // SAGA IMPLEMENTATION ON REMOVE BUTTON
        btnRemove.setOnClickListener(v -> {
            if (!isNetworkConnected()) {
                showSagaFeedback("Adjustment Failed: Internet connection lost!", false);
                return;
            }

            showSagaFeedback("Processing Adjustment (Saga)...", true);

            // Simulate API Rollback
            handler.postDelayed(() -> {
                boolean sagaSuccess = false; // Set to false to test Rollback

                if (sagaSuccess) {
                    showSagaFeedback("Adjustment Synchronized Successfully!", true);
                    dialog.dismiss();
                } else {
                    showSagaFeedback("SAGA ROLLBACK: Database Update Failed. Stock remains unchanged!", false);
                }
            }, 1500);
        });

        btnAddManual.setOnClickListener(v -> {
            showSagaFeedback("Add Manual feature is coming soon, bro!", true);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showFaqDialog() {
        Dialog faqDialog = new Dialog(this);
        faqDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        faqDialog.setContentView(R.layout.dialog_faq);

        if (faqDialog.getWindow() != null) {
            faqDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            faqDialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        faqDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupScanner();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCommScanner != null) {
            try {
                mCommScanner.getRFIDScanner().setDataDelegate(null);
                mCommScanner.getBarcodeScanner().setDataDelegate(null);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGen != null) { toneGen.release(); toneGen = null; }
    }
}