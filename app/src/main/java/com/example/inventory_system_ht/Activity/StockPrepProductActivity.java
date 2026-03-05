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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// 👇 SDK DENSO 👇
import com.densowave.scannersdk.Barcode.BarcodeData;
import com.densowave.scannersdk.Barcode.BarcodeDataReceivedEvent;
import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.BarcodeDataDelegate;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.densowave.scannersdk.RFID.RFIDData;
import com.densowave.scannersdk.RFID.RFIDDataReceivedEvent;

import com.example.inventory_system_ht.Adapter.TagAdapter;
import com.example.inventory_system_ht.Models.TagModel;
// 👇 IMPORT DATABASE LOKAL LU 👇
import com.example.inventory_system_ht.Helper.AppDatabase;
import com.example.inventory_system_ht.Helper.AppDao;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.List;

/**
 * StockPrepProductActivity: Verifies products based on DO.
 * Integrated with BaseScannerActivity, Offline Mode (Room SQLite) & Saga Rollback Logic.
 */
public class StockPrepProductActivity extends BaseScannerActivity implements BarcodeDataDelegate, RFIDDataDelegate {

    private EditText resultScan;
    private TextView tvScanned, tvNoDo, tvDateDo;
    private int scanCount = 0;
    private TagAdapter adapter;
    private List<TagModel> scannedList;
    private Switch switchRfid;
    private RecyclerView rvTags;

    private String currentDoNo = "";

    // SDK & UTILS
    private CommScanner mCommScanner;
    private ToneGenerator toneGen;
    private Handler handler = new Handler(Looper.getMainLooper());

    // 👇 MESIN DATABASE 👇
    private AppDatabase appDb;
    private AppDao appDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_prep_product);

        // 1. Inisialisasi Database Lokal
        appDb = AppDatabase.getDatabase(this);
        appDao = appDb.appDao();

        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        } catch (Exception e) { e.printStackTrace(); }

        tvScanned = findViewById(R.id.tvScanned);
        tvNoDo = findViewById(R.id.tvNoDo);
        tvDateDo = findViewById(R.id.tvDateDo);
        resultScan = findViewById(R.id.resultScan);
        switchRfid = findViewById(R.id.switchRfid);
        rvTags = findViewById(R.id.rvTags);

        switchRfid.setChecked(false);

        scannedList = new ArrayList<>();
        adapter = new TagAdapter(scannedList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        setupScanner();

        // Nangkep Data Intent
        if (getIntent() != null) {
            currentDoNo = getIntent().getStringExtra("NO_DO");
            tvNoDo.setText("No : " + currentDoNo);
            tvDateDo.setText("Date : " + getIntent().getStringExtra("DATE_DO"));
        }

        // 2. AUTO-LOAD: Tarik data yang gantung (Pending) dari database buat DO ini
        loadPendingScans();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // TextWatcher for Laser Barcode Wedge
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
                    processScan(data);
                    resultScan.removeTextChangedListener(this);
                    resultScan.setText("");
                    resultScan.addTextChangedListener(this);
                    resultScan.requestFocus();
                }, 500);
            }
        });

        // SMART SWITCH LOGIC
        switchRfid.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    boolean isRfidReady = (mCommScanner != null && mCommScanner.getRFIDScanner() != null);
                    if (!isRfidReady) {
                        showSagaFeedback("HT not Connected to Reader RFID", false);
                        switchRfid.setOnCheckedChangeListener(null);
                        switchRfid.setChecked(false);
                        switchRfid.setOnCheckedChangeListener(this);
                        return;
                    }
                }
                String msg = isChecked ? "Mode RFID: ON" : "Mode RFID: OFF";
                showSagaFeedback(msg, true);
                resultScan.requestFocus();
            }
        });

        findViewById(R.id.btnClear).setOnClickListener(v -> {
            scannedList.clear();
            adapter.notifyDataSetChanged();
            scanCount = 0;
            tvScanned.setText("Scanned : 0");
            showSagaFeedback("Data session cleared from screen. (Pending data remains in DB)", true);
        });

        // ==========================================
        // 👇 LOGIC SYNC DENGAN SAGA ROLLBACK FLOW 👇
        // ==========================================
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            if (scannedList.isEmpty()) {
                showSagaFeedback("Scan the items first, bro!", false);
                return;
            }

            if (!isNetworkConnected()) {
                showSagaFeedback("Connection Lost! Data is safe locally in SQLite, find a signal.", false);
                return;
            }

            showSagaFeedback("Syncing Stock Prep to Server...", true);

            // Simulasi Request API ke Backend ASP.NET
            handler.postDelayed(() -> {
                // Skenario: 1 = Sukses (Saga Finish), 2 = Rollback (Error di Redis/Server)
                int sagaStatus = 1; // Kita set 1 buat ngetes skenario sukses

                if (sagaStatus == 1) {
                    // 3. UPDATE DATABASE: Tandai semua barang di layar ini sebagai "Terkirim" (sync_status = 1)
                    for (TagModel tag : scannedList) {
                        appDao.markTagAsSynced(tag.getEpcTag());
                    }

                    showSagaFeedback("SUCCESS: Data synced & Saga Transaction Committed!", true);
                    scannedList.clear();
                    adapter.notifyDataSetChanged();
                    scanCount = 0;
                    tvScanned.setText("Scanned : 0");
                } else {
                    showSagaFeedback("SAGA ROLLBACK: Failed to sync to database. Try Saving again!", false);
                }
            }, 2000);
        });
    }

    // Fungsi narik data pending dari SQLite
    private void loadPendingScans() {
        List<TagModel> pendingTags = appDao.getPendingTags(); // Ambil semua yang sync_status = 0

        for (TagModel tag : pendingTags) {
            // Filter: Cuma masukin ke layar kalau nomor DO-nya cocok sama yang lagi dibuka
            if (tag.getDoIdRef().equalsIgnoreCase(currentDoNo)) {
                scannedList.add(tag);
                scanCount++;
            }
        }

        if (scanCount > 0) {
            tvScanned.setText("Scanned : " + scanCount);
            adapter.notifyDataSetChanged();
            showSagaFeedback("Recovered " + scanCount + " pending scans from Offline Local DB!", true);
        }
    }

    private void processScan(String data) {
        // Cek duplikat di layar biar gak scan barang yang sama 2x
        for (TagModel existingTag : scannedList) {
            if (existingTag.getEpcTag().equalsIgnoreCase(data)) {
                showSagaFeedback("Item already scanned!", false);
                playBeep(false);
                return;
            }
        }

        scanCount++;
        tvScanned.setText("Scanned : " + scanCount);

        TagModel newScan = new TagModel(data, "ITM-SCAN", "Product Verified", currentDoNo, 0);

        // 👇 AUTO-SAVE: Langsung amankan ke memori fisik SQLite 👇
        appDao.insertScannedTag(newScan);

        scannedList.add(newScan);
        adapter.notifyItemInserted(scannedList.size() - 1);
        rvTags.scrollToPosition(scannedList.size() - 1);
        playBeep(true);
    }

    // ==========================================
    // SDK METHODS
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
            handler.post(() -> processScan(epc));
        }
    }

    @Override
    public void onBarcodeDataReceived(CommScanner scanner, BarcodeDataReceivedEvent event) {
        if (switchRfid.isChecked()) return;
        List<BarcodeData> dataList = event.getBarcodeData();
        if (!dataList.isEmpty()) {
            String barcode = new String(dataList.get(0).getData());
            handler.post(() -> processScan(barcode));
        }
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) { sb.append(String.format("%02X", b)); }
        return sb.toString();
    }

    private void playBeep(boolean success) {
        if (toneGen != null) toneGen.startTone(success ? ToneGenerator.TONE_PROP_BEEP : ToneGenerator.TONE_CDMA_HIGH_L, 150);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupScanner();
        if(!isNetworkConnected()) showSagaFeedback("Warning: You are offline! Scans will be saved locally.", false);
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
}