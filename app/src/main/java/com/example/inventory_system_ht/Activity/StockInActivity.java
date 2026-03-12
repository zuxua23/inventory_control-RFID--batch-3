package com.example.inventory_system_ht.Activity;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.densowave.scannersdk.Barcode.BarcodeDataReceivedEvent;
import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.BarcodeDataDelegate;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.densowave.scannersdk.RFID.RFIDData;
import com.densowave.scannersdk.RFID.RFIDDataReceivedEvent;
import com.example.inventory_system_ht.Adapter.ItemAdapter;
import com.example.inventory_system_ht.Helper.ApiClient;
import com.example.inventory_system_ht.Helper.ApiService;
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.Models.GeneralResponse;
import com.example.inventory_system_ht.Models.ItemModels;
import com.example.inventory_system_ht.Models.StockInRequest;
import com.example.inventory_system_ht.Models.TagModels;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class StockInActivity extends BaseScannerActivity implements BarcodeDataDelegate, RFIDDataDelegate {

    private ImageView btnBack;
    private Button btnClear, btnSave;
    private Switch switchRfid;
    private EditText resultScan;
    private TextView tvScanned;
    private RecyclerView rvTags;
    private ItemAdapter adapter;
    private List<ItemModels.ItemModel> scannedItemsList;


    private List<ItemModels.ItemResponseDto> masterItemList = new ArrayList<>();

    private ToneGenerator toneGen;
    private Handler handler = new Handler(Looper.getMainLooper());
    private CommScanner mCommScanner;
    private boolean isProcessing = false;
    private int totalScanCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_in);

        try { toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100); } catch (Exception e) {}

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

        fetchMasterItems();

        resultScan.setShowSoftInputOnFocus(false);
        resultScan.postDelayed(() -> resultScan.requestFocus(), 100);

        resultScan.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String data = s.toString().trim();
                if (data.length() >= 7 && !isProcessing && !switchRfid.isChecked()) {
                    isProcessing = true;
                    processScannedData(data);
                    resultScan.setText("");
                    isProcessing = false;
                }
            }
        });

        btnBack.setOnClickListener(v -> finish());

        btnClear.setOnClickListener(v -> clearAllData());

        btnSave.setOnClickListener(v -> {
            if (scannedItemsList.isEmpty()) {
                showSagaFeedback("No items have been scanned yet!", false);
                return;
            }
            showBulkConfirmDialog();
        });

        switchRfid.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                boolean isConnected = (mCommScanner != null && mCommScanner.getRFIDScanner() != null);
                if (!isConnected) {
                    showSagaFeedback("HT not Connected to Reader RFID", false);
                    switchRfid.setChecked(false);
                    return;
                }
            }
            showSagaFeedback(isChecked ? "Mode RFID: ON" : "Mode RFID: OFF", true);
            resultScan.requestFocus();
        });
    }

    private void fetchMasterItems() {
        if (!isNetworkConnected()) return;
        showLoading();
        PrefManager pref = new PrefManager(this);
        String token = "Bearer " + pref.getToken();

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getAllItems(token).enqueue(new retrofit2.Callback<List<ItemModels.ItemResponseDto>>() {
            @Override
            public void onResponse(Call<List<ItemModels.ItemResponseDto>> call, retrofit2.Response<List<ItemModels.ItemResponseDto>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    masterItemList = response.body();
                }else {
                    handleApiError(response.code());
                }
            }
            @Override
            public void onFailure(Call<List<ItemModels.ItemResponseDto>> call, Throwable t) { hideLoading(); handleFailure(t); }
        });
    }

    private String findItemName(String itemId) {
        for (ItemModels.ItemResponseDto item : masterItemList) {
            if (item.getItemId().equals(itemId)) {
                return item.getItemName();
            }
        }
        return "Unknown Item";
    }

    private String extractItemId(String scannedData, boolean isRfid) {
        if (scannedData.startsWith("TAG0000") || scannedData.startsWith("EPC-SATO")) {
            return "ITM-001";
        }

        try {
            if (isRfid) {
                if (scannedData.startsWith("A") && scannedData.length() > 11) {
                    return scannedData.substring(1, scannedData.length() - 10); // Motong dapet ITM-001
                }
            }
        } catch (Exception e) {}

        return scannedData;
    }

    private void processScannedData(String scannedData) {
        for (ItemModels.ItemModel t : scannedItemsList) {
            if (t.getEpcTag().equals(scannedData) || t.getItemId().equals(scannedData)) {
                playScanFeedback(1);
                showSagaFeedback("The item is already on the list!", false);
                return;
            }
        }

        if (!isNetworkConnected()) {
            playScanFeedback(0);
            showSagaFeedback("Offline! Item added locally.", false);

            ItemModels.ItemModel offlineItem = new ItemModels.ItemModel(scannedData, scannedData, "Offline Scanned Item", 1);

            scannedItemsList.add(0, offlineItem);

            if (adapter != null) adapter.setLastScannedPosition(0);

            adapter.notifyItemInserted(0);
            rvTags.scrollToPosition(0);
            totalScanCount++;
            updateScanCount();
            return;
        }

        PrefManager pref = new PrefManager(this);
        String token = "Bearer " + pref.getToken();

        showLoading();

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getTagInfo(token, scannedData).enqueue(new retrofit2.Callback<TagModels.TagInfoDto>() {
            @Override
            public void onResponse(Call<TagModels.TagInfoDto> call, retrofit2.Response<TagModels.TagInfoDto> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    TagModels.TagInfoDto info = response.body();

                    if (!info.getStatus().equals("STANDBY") && !info.getStatus().equals("PRINTED")) {
                        showSagaFeedback("Tag " + info.getTagId() + " status " + info.getStatus() + "!", false);
                        playScanFeedback(2);
                        return;
                    }

                    playScanFeedback(0);

                    scannedItemsList.add(0, new ItemModels.ItemModel(
                            scannedData,
                            info.getTagId(),
                            info.getItemName(),
                            1
                    ));

                    if (adapter != null) adapter.setLastScannedPosition(0);
                    adapter.notifyItemInserted(0);
                    rvTags.scrollToPosition(0);

                    totalScanCount++;
                    updateScanCount();

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

    private void clearAllData() {
        scannedItemsList.clear();
        adapter.notifyDataSetChanged();
        totalScanCount = 0;
        updateScanCount();
    }

    private void updateScanCount() {
        tvScanned.setText("Qty: " + totalScanCount);
    }

    private void showBulkConfirmDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_regist);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvTitle);
        tvTitle.setText("Stock In " + totalScanCount + " Physique?");
        Button btnYes = dialog.findViewById(R.id.btnYes);
        btnYes.setText("Stock In");

        dialog.findViewById(R.id.btnNo).setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            List<String> codesToSubmit = new ArrayList<>();
            for(ItemModels.ItemModel item : scannedItemsList) codesToSubmit.add(item.getEpcTag());

            String currentType = switchRfid.isChecked() ? "RFID" : "QR";
            hitApiStockIn(codesToSubmit, currentType);
        });
        dialog.show();
    }

    private void hitApiStockIn(List<String> codes, String scannerType) {
        if (!isNetworkConnected()) {
            showSagaFeedback("Connection Error! Cari sinyal dulu bre buat Stock In.", false);
            playScanFeedback(2);
            return;
        }
        showLoading();

        PrefManager pref = new PrefManager(this);
        String token = "Bearer " + pref.getToken();

        StockInRequest request = new StockInRequest(scannerType, codes);
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.stockIn(token, request).enqueue(new retrofit2.Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, retrofit2.Response<GeneralResponse> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    showSagaFeedback("Success: " + response.body().getMessage() + " (" + codes.size() + " Physique)", true);
                    playScanFeedback(0);
                    clearAllData();
                } else {
                    handleApiError(response.code());
                    playScanFeedback(2);
                }
                resultScan.requestFocus();
            }

            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                hideLoading();
                handleFailure(t);
                playScanFeedback(2);
                resultScan.requestFocus();
            }
        });
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    @Override
    public void onRFIDDataReceived(CommScanner scanner, RFIDDataReceivedEvent event) {
        if (!switchRfid.isChecked()) return;
        for (RFIDData data : event.getRFIDData()) {
            String epc = bytesToHexString(data.getUII());
            handler.post(() -> processScannedData(epc));
        }
    }

    @Override
    public void onBarcodeDataReceived(CommScanner scanner, BarcodeDataReceivedEvent event) {
        if (switchRfid.isChecked()) return;
        if (!event.getBarcodeData().isEmpty()) {
            String barcode = new String(event.getBarcodeData().get(0).getData());
            handler.post(() -> processScannedData(barcode));
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGen != null) { toneGen.release(); toneGen = null; }
    }
}