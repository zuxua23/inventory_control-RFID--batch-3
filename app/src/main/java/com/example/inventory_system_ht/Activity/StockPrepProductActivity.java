package com.example.inventory_system_ht.Activity;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.densowave.scannersdk.Barcode.BarcodeData;
import com.densowave.scannersdk.Barcode.BarcodeDataReceivedEvent;
import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.BarcodeDataDelegate;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.densowave.scannersdk.RFID.RFIDData;
import com.densowave.scannersdk.RFID.RFIDDataReceivedEvent;
import com.example.inventory_system_ht.Adapter.TagAdapter;
import com.example.inventory_system_ht.Helper.ApiClient;
import com.example.inventory_system_ht.Helper.ApiService;
import com.example.inventory_system_ht.Helper.AppDao;
import com.example.inventory_system_ht.Helper.AppDatabase;
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.Models.GeneralResponse;
import com.example.inventory_system_ht.Models.StockPrepBulkRequest;
import com.example.inventory_system_ht.Models.TagModels;
import com.example.inventory_system_ht.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StockPrepProductActivity extends BaseScannerActivity implements BarcodeDataDelegate, RFIDDataDelegate {

    private EditText resultScan;
    private TextView tvScanned, tvNoDo, tvDateDo;
    private int scanCount = 0;
    private TagAdapter adapter;
    private List<TagModels.TagModel> scannedList;
    private Switch switchRfid;
    private RecyclerView rvTags;

    private String currentDoId = ""; // GUID dari server
    private String currentDoNo = ""; // Nomor tampilan

    // UTILS & SDK
    private CommScanner mCommScanner;
    private ToneGenerator toneGen;
    private Handler handler = new Handler(Looper.getMainLooper());

    // BACKEND TOOLS
    private ApiService api;
    private String token;
    private AppDao appDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_prep_product);

        // 1. Inisialisasi Database & Retrofit
        appDao = AppDatabase.getDatabase(this).appDao();
        PrefManager pref = new PrefManager(this);
        token = "Bearer " + pref.getToken();
        api = ApiClient.getClient(this).create(ApiService.class);

        initUI();
        setupScanner();

        // Di dalam onCreate, ganti bagian ini:
        if (getIntent() != null) {
            currentDoId = getIntent().getStringExtra("DO_ID");
            currentDoNo = getIntent().getStringExtra("NO_DO");
            tvNoDo.setText("No : " + currentDoNo);

            // 👇 UPDATE BAGIAN INI BRE 👇
            String rawDate = getIntent().getStringExtra("DATE_DO");
            tvDateDo.setText("Date : " + formatToEnglishDate(rawDate));
        }

        loadPendingScans();
        setupListeners();
    }

    private void initUI() {
        try { toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100); } catch (Exception e) {}
        tvScanned = findViewById(R.id.tvScanned);
        tvNoDo = findViewById(R.id.tvNoDo);
        tvDateDo = findViewById(R.id.tvDateDo);
        resultScan = findViewById(R.id.resultScan);
        switchRfid = findViewById(R.id.switchRfid);
        rvTags = findViewById(R.id.rvTags);

        scannedList = new ArrayList<>();
        adapter = new TagAdapter(scannedList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        resultScan.setShowSoftInputOnFocus(false);
        resultScan.postDelayed(() -> resultScan.requestFocus(), 100);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Laser Barcode Wedge Handler
        resultScan.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String data = s.toString().trim();
                if (data.length() >= 7) {
                    processScan(data);
                    resultScan.setText("");
                }
            }
        });

        // Toggle RFID
        switchRfid.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && (mCommScanner == null || mCommScanner.getRFIDScanner() == null)) {
                showSagaFeedback("HT not Connected to Reader RFID", false);
                switchRfid.setChecked(false);
                return;
            }
            showSagaFeedback(isChecked ? "Mode RFID: ON" : "Mode RFID: OFF", true);
        });

        // BULK SAVE TO SERVER
        findViewById(R.id.btnSave).setOnClickListener(v -> submitToBackend());

        findViewById(R.id.btnClear).setOnClickListener(v -> {
            scannedList.clear();
            adapter.notifyDataSetChanged();
            scanCount = 0;
            tvScanned.setText("Scanned : 0");
            showSagaFeedback("The screen is cleared!", true);
        });
    }

    private void processScan(String scannedData) {
        for (TagModels.TagModel item : scannedList) {
            if (item.getEpcTag().equalsIgnoreCase(scannedData)) {
                playScanFeedback(1);
                showSagaFeedback("The item has been scanned!", false);
                return;
            }
        }

        if (!isNetworkConnected()) {
            showSagaFeedback("Offline! Data will be saved locally.", false);
            saveToLocalDB(new TagModels.TagInfoDto(scannedData, scannedData, "Pending Sync", "Unknown", "STANDBY"));
            return;
        }

        showLoading();
        api.getTagInfo(token, scannedData).enqueue(new Callback<TagModels.TagInfoDto>() {
            @Override
            public void onResponse(Call<TagModels.TagInfoDto> call, Response<TagModels.TagInfoDto> response) {
                hideLoading();

                if (response.isSuccessful() && response.body() != null) {
                    TagModels.TagInfoDto info = response.body();
                    if (!info.getStatus().equals("IN_STOCK")) {
                        showSagaFeedback("Tag " + info.getStatus() + ", must be IN_STOCK!", false);
                        playScanFeedback(2);
                        return;
                    }
                    saveToLocalDB(info);
                } else {
                    handleApiError(response.code());
                    playScanFeedback(2);
                }
            }

            @Override
            public void onFailure(Call<TagModels.TagInfoDto> call, Throwable t) {
                handleFailure(t);
                playScanFeedback(2);
            }
        });
    }

    private void saveToLocalDB(TagModels.TagInfoDto tag) {
        TagModels.TagModel newScan = new TagModels.TagModel(
                tag.getEpcTag(),
                tag.getTagId(),
                tag.getItemId(),
                tag.getItemName(),
                currentDoNo,
                0
        );

        new Thread(() -> {
            appDao.insertScannedTag(newScan);
            runOnUiThread(() -> {
                // 👇 SMART SORTING: Masuk ke index 0
                scannedList.add(0, newScan);
                if (adapter != null) adapter.setLastScannedPosition(0); // Highlight biru

                adapter.notifyItemInserted(0);
                rvTags.scrollToPosition(0);

                scanCount++;
                tvScanned.setText("Scanned : " + scanCount);
                playScanFeedback(0);
            });
        }).start();
    }

    private void submitToBackend() {
        if (scannedList.isEmpty()) return;

        // 👇 CEK INTERNET SEBELUM SUBMIT
        if (!isNetworkConnected()) {
            showSagaFeedback("Offline! Find connection to submit.", false);
            playScanFeedback(2);
            return;
        }

        showLoading();
        List<String> codes = new ArrayList<>();
        for (TagModels.TagModel t : scannedList) codes.add(t.getEpcTag());

        String scannerType = switchRfid.isChecked() ? "RFID" : "QR";
        StockPrepBulkRequest request = new StockPrepBulkRequest(currentDoId, codes, scannerType);

        showSagaFeedback("Sending data to the server...", true);

        api.submitStockPrep(token, request).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    new Thread(() -> {
                        for (TagModels.TagModel t : scannedList) appDao.markTagAsSynced(t.getEpcTag());
                        runOnUiThread(() -> {
                            showSagaFeedback("SUCCESS: Goods successfully prepared!", true);
                            playScanFeedback(0); // 👈 TIPE 0: SUKSES SUBMIT KE C#
                            scannedList.clear();
                            adapter.notifyDataSetChanged();
                            scanCount = 0;
                            tvScanned.setText("Scanned : 0");
                        });
                    }).start();
                } else {
                    handleApiError(response.code());
                    playScanFeedback(2); // 👈 TIPE 2: GAGAL SUBMIT
                }
            }

            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                hideLoading();
                handleFailure(t);
                playScanFeedback(2); // 👈 TIPE 2: RTO PAS SUBMIT
            }
        });
    }

    private void loadPendingScans() {
        showLoading();
        new Thread(() -> {
            List<TagModels.TagModel> pending = appDao.getPendingTags();
            runOnUiThread(() -> {
                hideLoading();
                for (TagModels.TagModel t : pending) {
                    if (t.getDoIdRef().equalsIgnoreCase(currentDoNo)) {
                        scannedList.add(t);
                        scanCount++;
                    }
                }
                adapter.notifyDataSetChanged();
                tvScanned.setText("Scanned : " + scanCount);
            });
        }).start();
    }

    // --- SDK DENSO INTEGRATION ---
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
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    private void setupScanner() {
        if (mCommScanner != null) {
            try {
                mCommScanner.getRFIDScanner().setDataDelegate(this);
                mCommScanner.getBarcodeScanner().setDataDelegate(this);
            } catch (Exception e) {}
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupScanner();

        if (getHTBatteryLevel() <= 15) {
            showSagaFeedback("Baterai HT sisa " + getHTBatteryLevel() + "%, waktunya ngecas bre!", false);
            playScanFeedback(2);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // MATIIN LISTENER SCANNER PAS KELUAR HALAMAN BIAR GAK BOCOR BATRE
        if (mCommScanner != null) {
            try {
                if (mCommScanner.getRFIDScanner() != null) {
                    mCommScanner.getRFIDScanner().setDataDelegate(null);
                }
                if (mCommScanner.getBarcodeScanner() != null) {
                    mCommScanner.getBarcodeScanner().setDataDelegate(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private String formatToEnglishDate(String rawDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);

            java.util.Date date = inputFormat.parse(rawDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            return rawDate;
        }
    }


}