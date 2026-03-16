package com.example.inventory_system_ht.Activity;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.example.inventory_system_ht.Models.StockOutModels;
import com.example.inventory_system_ht.Models.TagModels;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StockOutActivity extends BaseScannerActivity implements BarcodeDataDelegate, RFIDDataDelegate {
    private String doId = "";
    private String doNumber = "";
    private String readerId = "BHT-1800QWB-1-A7";
    private ImageView btnBack;
    private TextView tvScanned;
    private Switch switchRfid;
    private EditText resultScan;
    private RecyclerView rvTags;
    private Button btnClear, btnSave;
    private TagAdapter adapter;
    private List<TagModels.TagModel> scannedItemList;
    private int scanCount = 0;
    private ApiService api;
    private String token;
    private AppDao appDao;
    private CommScanner mCommScanner;
    private ToneGenerator toneGen;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_out);

        if (getIntent() != null) {
            doId = getIntent().getStringExtra("DO_ID");
            doNumber = getIntent().getStringExtra("NO_DO");
        }

        PrefManager pref = new PrefManager(this);
        token = "Bearer " + pref.getToken();
        api = ApiClient.getClient(this).create(ApiService.class);
        appDao = AppDatabase.getDatabase(this).appDao();
        try { toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100); } catch (Exception e) {}

        initUI();
        setupRecyclerView();
        setupListeners();
        setupScanner();
    }

    private void initUI() {
        btnBack = findViewById(R.id.btnBack);
        tvScanned = findViewById(R.id.tvScanned);
        switchRfid = findViewById(R.id.switchRfid);
        resultScan = findViewById(R.id.resultScan);
        rvTags = findViewById(R.id.rvTags);
        btnClear = findViewById(R.id.btnClear);
        btnSave = findViewById(R.id.btnSave);

        switchRfid.setChecked(false);
        updateCounterUI();
    }

    private void setupRecyclerView() {
        scannedItemList = new ArrayList<>();

        adapter = new TagAdapter(scannedItemList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        switchRfid.setOnCheckedChangeListener((buttonView, isChecked) -> {
            boolean isRfidReady = (mCommScanner != null && mCommScanner.getRFIDScanner() != null);
            if (isChecked && !isRfidReady) {
                showSagaFeedback("The RFID reader isn't connected yet!", false);
                switchRfid.setChecked(false);
            } else {
                showSagaFeedback(isChecked ? "RFID Mode ON" : "Barcode Mode ON", true);
                resultScan.requestFocus();
            }
        });

        resultScan.setOnEditorActionListener((v, actionId, event) -> {
            String data = resultScan.getText().toString().trim();
            if (!data.isEmpty()) {
                processScanResult(data);
                resultScan.setText("");
            }
            return true;
        });

        btnClear.setOnClickListener(v -> {
            scannedItemList.clear();
            scanCount = 0;
            adapter.notifyDataSetChanged();
            updateCounterUI();
            showSagaFeedback("List cleared (Local)", true);
        });

        btnSave.setOnClickListener(v -> finalizeStockOut());
    }

    private void updateCounterUI() {
        tvScanned.setText("Scanned: " + scanCount);
    }

    private void processScanResult(String epcOrBarcode) {

        for (TagModels.TagModel item : scannedItemList) {
            if (item.getEpcTag().equalsIgnoreCase(epcOrBarcode) || item.getTagId().equalsIgnoreCase(epcOrBarcode)) {
                playScanFeedback(1);
                return;
            }
        }

        if (!isNetworkConnected()) {
            playScanFeedback(0);
            showSagaFeedback("Offline! The data is saved on the cellphone first.", false);

            new Thread(() -> {

                TagModels.TagModel offlineTag = new TagModels.TagModel(
                        epcOrBarcode, epcOrBarcode, "", "Scanned Offline", doId, 0
                );
                appDao.insertScannedTag(offlineTag);

                runOnUiThread(() -> {
                    scannedItemList.add(0, offlineTag);
                    if (adapter != null) adapter.setLastScannedPosition(0);
                    scanCount++;
                    adapter.notifyItemInserted(0);
                    rvTags.scrollToPosition(0);
                    updateCounterUI();
                });
            }).start();
            return;
        }

        showLoading();
        StockOutModels.ScanReq req = new StockOutModels.ScanReq(doId, readerId, epcOrBarcode);

        api.scanStockOut(token, req).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    playScanFeedback(0);
                    showSagaFeedback("Tag " + epcOrBarcode + " marked OUT!", true);

                    TagModels.TagModel newItem = new TagModels.TagModel(
                            epcOrBarcode, epcOrBarcode, "", "Scanned Item", doId, 1
                    );

                    scannedItemList.add(0, newItem);
                    if (adapter != null) adapter.setLastScannedPosition(0);

                    scanCount++;
                    adapter.notifyDataSetChanged();
                    rvTags.scrollToPosition(0);
                    updateCounterUI();
                } else {
                    handleApiError(response.code());
                    playScanFeedback(2);
                }
            }

            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                hideLoading();
                handleFailure(t);
                playScanFeedback(2);
            }
        });
    }

    private void finalizeStockOut() {
        if (scannedItemList.isEmpty()) {
            showSagaFeedback("No items have been scanned yet!", false);
            return;
        }

        if (!isNetworkConnected()) {
            showSagaFeedback("Still Offline! Find a signal first to finalize the DO.", false);
            playScanFeedback(2);
            return;
        }

        showLoading();
        showSagaFeedback("Checking offline data...", true);

        new Thread(() -> {
            List<TagModels.TagModel> pendingTags = appDao.getPendingTags();
            List<String> tagsToSync = new ArrayList<>();

            for (TagModels.TagModel tag : pendingTags) {
                if (doId.equals(tag.getDoIdRef())) {
                    tagsToSync.add(tag.getEpcTag());
                }
            }

            runOnUiThread(() -> {
                if (!tagsToSync.isEmpty()) {
                    showSagaFeedback("Syncing " + tagsToSync.size() + " data offline...", true);
                    syncStockOutData(tagsToSync, 0);
                } else {
                    executeFinalizeAPI();
                }
            });
        }).start();
    }

    private void syncStockOutData(List<String> tags, int currentIndex) {
        if (currentIndex >= tags.size()) {
            new Thread(() -> {
                for (String epc : tags) appDao.markTagAsSynced(epc);
                runOnUiThread(this::executeFinalizeAPI);
            }).start();
            return;
        }

        StockOutModels.ScanReq req = new StockOutModels.ScanReq(doId, readerId, tags.get(currentIndex));
        api.scanStockOut(token, req).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                syncStockOutData(tags, currentIndex + 1);
            }
            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                syncStockOutData(tags, currentIndex + 1);
            }
        });
    }


    private void executeFinalizeAPI() {
        showSagaFeedback("Saving & Finalizing DO...", true);
        StockOutModels.FinalizeReq req = new StockOutModels.FinalizeReq(doId, readerId);

        api.finalizeStockOut(token, req).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    showSagaFeedback("Stock Out Successfully Stored!", true);
                    playScanFeedback(0);
                    finish();
                } else {
                    handleApiError(response.code());
                    playScanFeedback(2);
                }
            }

            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                hideLoading();
                handleFailure(t);
                playScanFeedback(2);
            }
        });
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

    @Override
    protected void onResume() {
        super.onResume();
        setupScanner();
        if (resultScan != null) resultScan.postDelayed(() -> resultScan.requestFocus(), 200);

        if (getHTBatteryLevel() <= 15) {
            showSagaFeedback("Leftover HT battery " + getHTBatteryLevel() + "%, time to charge!", false);
            playScanFeedback(2);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

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

    @Override protected void onDestroy() {
        super.onDestroy();
        if (toneGen != null) { toneGen.release(); toneGen = null; }
    }
}