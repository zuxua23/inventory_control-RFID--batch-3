package com.example.inventory_system_ht.Activity;

import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// SDK DENSO
import com.densowave.scannersdk.Barcode.BarcodeData;
import com.densowave.scannersdk.Barcode.BarcodeDataReceivedEvent;
import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.BarcodeDataDelegate;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.densowave.scannersdk.RFID.RFIDData;
import com.densowave.scannersdk.RFID.RFIDDataReceivedEvent;

import com.example.inventory_system_ht.Adapter.ItemAdapter;
import com.example.inventory_system_ht.Models.ItemModel;
import com.example.inventory_system_ht.R;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

// 👇 SEKARANG EXTENDS KE BASE BIAR DAPET FITUR SAGA & CEK KONEKSI 👇
public class StockInActivity extends BaseScannerActivity implements BarcodeDataDelegate, RFIDDataDelegate {

    private ImageView btnBack;
    private Button btnClear, btnSave;
    private Switch switchRfid;
    private EditText resultScan;
    private TextView tvScanned;
    private RecyclerView rvTags;
    private ItemAdapter adapter;
    private List<ItemModel> scannedItemsList;
    private int scanCount = 0;

    private ToneGenerator toneGen;
    private Handler handler = new Handler(Looper.getMainLooper());
    private CommScanner mCommScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_in);

        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        } catch (Exception e) { e.printStackTrace(); }

        btnBack = findViewById(R.id.btnBack);
        btnClear = findViewById(R.id.btnClear);
        btnSave = findViewById(R.id.btnSave);
        switchRfid = findViewById(R.id.switchRfid);
        resultScan = findViewById(R.id.resultScan);
        tvScanned = findViewById(R.id.tvScanned);
        rvTags = findViewById(R.id.rvTags);

        switchRfid.setChecked(false);
        scannedItemsList = new ArrayList<>();
        adapter = new ItemAdapter(scannedItemsList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        setupScanner();

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
                String hasilScan = s.toString().trim();
                if (hasilScan.isEmpty()) return;

                handler.postDelayed(() -> {
                    prosesValidasiLokal(hasilScan);
                    resultScan.removeTextChangedListener(this);
                    resultScan.setText("");
                    resultScan.addTextChangedListener(this);
                    resultScan.postDelayed(() -> resultScan.requestFocus(), 50);
                }, 500);
            }
        });

        btnBack.setOnClickListener(v -> finish());

        btnClear.setOnClickListener(v -> {
            scannedItemsList.clear();
            adapter.notifyDataSetChanged();
            scanCount = 0;
            tvScanned.setText("Scanned: 0");
            resultScan.requestFocus();
            showSagaFeedback("List cleared bro!", true);
        });

        // ==========================================
        // 👇 LOGIC SAVE DENGAN FLOW SAGA & ROLLBACK 👇
        // ==========================================
        btnSave.setOnClickListener(v -> {
            if (scannedItemsList.isEmpty()) {
                showSagaFeedback("No items have been scanned yet, bro!", false);
                return;
            }

            // 1. CEK INTERNET (Pake fungsi dari BaseScannerActivity)
            if (!isNetworkConnected()) {
                showSagaFeedback("CONNECTION LOOSE! Check your WiFi/Data before saving.", false);
                return;
            }

            // 2. SIMULASI KIRIM KE BACKEND (SAGA FLOW)
            showSagaFeedback("Saving to server (Saga Process)...", true);

            handler.postDelayed(() -> {
                // Simulasi Response: 1=Sukses, 2=Rollback (Gagal di Tengah)
                int skenarioSaga = 2;

                if (skenarioSaga == 1) {
                    showSagaFeedback("SUCCESS: Data is saved permanently!", true);

                    // Clear list hanya jika benar-benar sukses (Saga Completed)
                    scannedItemsList.clear();
                    adapter.notifyDataSetChanged();
                    scanCount = 0;
                    tvScanned.setText("Scanned: 0");
                }
                else if (skenarioSaga == 2) {
                    // SERVER BALIKIN STATUS ROLLBACK
                    String errorMsg = "SAGA ROLLBACK: Transaction aborted by server (Redis Stream Timeout)!";
                    showSagaFeedback(errorMsg, false);

                    // LIST TIDAK DIHAPUS (Biar operator tinggal coba Save lagi nanti)
                }
            }, 2000);
        });

        // SWITCH LOGIC
        switchRfid.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    boolean isConnected = (mCommScanner != null && mCommScanner.getRFIDScanner() != null);
                    if (!isConnected) {
                        showSagaFeedback("HT not Connected to Reader RFID", false);
                        switchRfid.setOnCheckedChangeListener(null);
                        switchRfid.setChecked(false);
                        switchRfid.setOnCheckedChangeListener(this);
                        return;
                    }
                }
                String msg = isChecked ? "Mode RFID: ON" : "Mode RFID: OFF";
                Snackbar.make(findViewById(android.R.id.content), msg, 1000).show();
                resultScan.requestFocus();
            }
        });
    }

    private void setupScanner() {
        // mCommScanner = MyApplication.getCommScanner();
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
            handler.post(() -> prosesValidasiLokal(epc));
        }
    }

    @Override
    public void onBarcodeDataReceived(CommScanner scanner, BarcodeDataReceivedEvent event) {
        if (switchRfid.isChecked()) return;
        if (!event.getBarcodeData().isEmpty()) {
            String barcode = new String(event.getBarcodeData().get(0).getData());
            handler.post(() -> prosesValidasiLokal(barcode));
        }
    }

    private void prosesValidasiLokal(String scanData) {
        ItemModel foundItem = lookupDummyData(scanData, switchRfid.isChecked());
        if (foundItem == null) {
            playBeep(false);
            showSagaFeedback("Tag/Barcode not recognized!", false);
            return;
        }

        boolean isExist = false;
        for (int i = 0; i < scannedItemsList.size(); i++) {
            if ((switchRfid.isChecked() && scannedItemsList.get(i).getEpcTag().equals(scanData)) ||
                    (!switchRfid.isChecked() && scannedItemsList.get(i).getItemId().equals(scanData))) {
                scannedItemsList.get(i).setQty(scannedItemsList.get(i).getQty() + 1);
                adapter.notifyItemChanged(i);
                isExist = true;
                break;
            }
        }

        if (!isExist) {
            scannedItemsList.add(new ItemModel(foundItem.getEpcTag(), foundItem.getItemId(), foundItem.getItemName(), 1));
            adapter.notifyItemInserted(scannedItemsList.size() - 1);
            rvTags.scrollToPosition(scannedItemsList.size() - 1);
        }
        scanCount++;
        tvScanned.setText("Scanned: " + scanCount);
        playBeep(true);
    }

    private ItemModel lookupDummyData(String scanData, boolean isRfid) {
        if (isRfid) {
            if (scanData.equalsIgnoreCase("112233")) return new ItemModel("112233", "ITM001", "Kemeja Anti Kusut", 1);
        } else {
            if (scanData.equalsIgnoreCase("ITM001")) return new ItemModel("-", "ITM001", "Kemeja Anti Kusut", 1);
        }
        return null;
    }

    private void playBeep(boolean isSuccess) {
        if (toneGen != null) toneGen.startTone(isSuccess ? ToneGenerator.TONE_PROP_BEEP : ToneGenerator.TONE_CDMA_HIGH_L, 150);
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) { sb.append(String.format("%02X", b)); }
        return sb.toString();
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
    protected void onResume() {
        super.onResume();
        setupScanner();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGen != null) { toneGen.release(); toneGen = null; }
    }
}