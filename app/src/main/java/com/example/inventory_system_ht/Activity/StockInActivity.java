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
import android.widget.CompoundButton;
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
import com.example.inventory_system_ht.Models.ItemModel;
import com.example.inventory_system_ht.Models.ItemResponseDto;
import com.example.inventory_system_ht.Models.StockInRequest;
import com.example.inventory_system_ht.Models.TagInfoDto;
import com.example.inventory_system_ht.Models.TagModel;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.Collections;
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
    private List<ItemModel> scannedItemsList;

    // Kumpulan Master Item dari BE
    private List<ItemResponseDto> masterItemList = new ArrayList<>();

    private ToneGenerator toneGen;
    private Handler handler = new Handler(Looper.getMainLooper());
    private CommScanner mCommScanner;
    private boolean isProcessing = false;
    private int totalScanCount = 0; // Buat ngitung total fisik

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

        // Pake ItemAdapter buat nampilin format ID, NAMA, dan QTY
        adapter = new ItemAdapter(scannedItemsList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        setupScanner();

        // 1. DOWNLOAD MASTER ITEM PAS HALAMAN DIBUKA
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
                    processScannedData(data); // Cukup satu argumen
                    resultScan.setText("");
                    isProcessing = false;
                }
            }
        });

        btnBack.setOnClickListener(v -> finish());

        btnClear.setOnClickListener(v -> clearAllData());

        btnSave.setOnClickListener(v -> {
            if (scannedItemsList.isEmpty()) {
                showSagaFeedback("Belum ada barang yang di-scan!", false);
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

    // AMBIL DATA MASTER DARI BACKEND
    private void fetchMasterItems() {
        if (!isNetworkConnected()) return;
        PrefManager pref = new PrefManager(this);
        String token = "Bearer " + pref.getToken();

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getAllItems(token).enqueue(new retrofit2.Callback<List<ItemResponseDto>>() {
            @Override
            public void onResponse(Call<List<ItemResponseDto>> call, retrofit2.Response<List<ItemResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    masterItemList = response.body();
                }
            }
            @Override
            public void onFailure(Call<List<ItemResponseDto>> call, Throwable t) {}
        });
    }

    // CARI NAMA BARANG DARI LIST LOKAL
    private String findItemName(String itemId) {
        for (ItemResponseDto item : masterItemList) {
            if (item.getItemId().equals(itemId)) {
                return item.getItemName();
            }
        }
        return "Unknown Item"; // Kalau datanya ga ketemu
    }

    // EKSTRAK ITEM ID DARI EPC ATAU TAG ID
    // EKSTRAK ITEM ID DARI EPC ATAU TAG ID
    private String extractItemId(String scannedData, boolean isRfid) {
        // 1. BYPASS BUAT DATA DUMMY SATO (Biar namanya langsung keluar "Produk SATO Dummy")
        if (scannedData.startsWith("TAG0000") || scannedData.startsWith("EPC-SATO")) {
            return "ITM-001";
        }

        // 2. LOGIC ASLI (Kalo besok-besok format EPC lu dari printer adalah AITM-0010000000001)
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
        // Kita bandingkan scannedData dengan EpcTag (karena scannedData bisa TagId atau EPC)
        for (ItemModel t : scannedItemsList) {
            if (t.getEpcTag().equals(scannedData) || t.getItemId().equals(scannedData)) {
                showSagaFeedback("Barang sudah ada di list!", false);
                return;
            }
        }

        if (!isNetworkConnected()) {
            showSagaFeedback("Koneksi terputus!", false);
            return;
        }

        PrefManager pref = new PrefManager(this);
        String token = "Bearer " + pref.getToken();

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getTagInfo(token, scannedData).enqueue(new retrofit2.Callback<TagInfoDto>() {
            @Override
            public void onResponse(Call<TagInfoDto> call, retrofit2.Response<TagInfoDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TagInfoDto info = response.body();

                    if (!info.getStatus().equals("STANDBY") && !info.getStatus().equals("PRINTED")) {
                        showSagaFeedback("Tag " + info.getTagId() + " status " + info.getStatus() + "!", false);
                        playBeep(false);
                        return;
                    }

                    // PAKAI ITEMMODEL: (epcTag, itemId, itemName, qty)
                    // Kita simpen scannedData (bisa EPC atau TagId) ke epcTag biar gampang di-submit ke BE
                    scannedItemsList.add(new ItemModel(
                            scannedData,
                            info.getTagId(), // Muncul di tvTagId
                            info.getItemName(), // Muncul di tvProductName
                            1 // Qty default 1
                    ));

                    adapter.notifyItemInserted(scannedItemsList.size() - 1);
                    rvTags.scrollToPosition(scannedItemsList.size() - 1);

                    totalScanCount++; // Update counter total
                    updateScanCount();
                    playBeep(true);

                } else {
                    showSagaFeedback("Tag tidak ditemukan!", false);
                    playBeep(false);
                }
            }

            @Override
            public void onFailure(Call<TagInfoDto> call, Throwable t) {
                showSagaFeedback("Gagal server!", false);
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
        tvTitle.setText("Stock In " + totalScanCount + " Fisik?");
        Button btnYes = dialog.findViewById(R.id.btnYes);
        btnYes.setText("Stock In");

        dialog.findViewById(R.id.btnNo).setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            List<String> codesToSubmit = new ArrayList<>();
            // Ambil semua EPC / TAG ID
            for(ItemModel item : scannedItemsList) codesToSubmit.add(item.getEpcTag());

            String currentType = switchRfid.isChecked() ? "RFID" : "QR";
            hitApiStockIn(codesToSubmit, currentType);
        });
        dialog.show();
    }

    // RETROFIT API CALL TETAP SAMA KAYA SEBELUMNYA
    private void hitApiStockIn(List<String> codes, String scannerType) {
        if (!isNetworkConnected()) {
            showSagaFeedback("Koneksi Error! Cek WiFi/Data.", false);
            return;
        }

        PrefManager pref = new PrefManager(this);
        String token = "Bearer " + pref.getToken();

        StockInRequest request = new StockInRequest(scannerType, codes);
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.stockIn(token, request).enqueue(new retrofit2.Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, retrofit2.Response<GeneralResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showSagaFeedback("Success: " + response.body().getMessage() + " (" + codes.size() + " Fisik)", true);
                    clearAllData();
                } else {
                    showSagaFeedback("Gagal Stock In! Pastikan tag terdaftar.", false);
                }
                resultScan.requestFocus();
            }

            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                showSagaFeedback("Koneksi Gagal: " + t.getMessage(), false);
                resultScan.requestFocus();
            }
        });
    }

    // ... FUNGSI LAINNYA (playBeep, setupScanner, onRFIDDataReceived) TETAP SAMA KAYAK SEBELUMNYA
    private void playBeep(boolean isSuccess) {
        if (toneGen != null) toneGen.startTone(isSuccess ? ToneGenerator.TONE_PROP_BEEP : ToneGenerator.TONE_CDMA_HIGH_L, 150);
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
            handler.post(() -> processScannedData(epc)); // Hapus parameter boolean-nya
        }
    }

    @Override
    public void onBarcodeDataReceived(CommScanner scanner, BarcodeDataReceivedEvent event) {
        if (switchRfid.isChecked()) return;
        if (!event.getBarcodeData().isEmpty()) {
            String barcode = new String(event.getBarcodeData().get(0).getData());
            handler.post(() -> processScannedData(barcode)); // Hapus parameter boolean-nya
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
    protected void onResume() { super.onResume(); setupScanner(); }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCommScanner != null) {
            try { mCommScanner.getRFIDScanner().setDataDelegate(null); mCommScanner.getBarcodeScanner().setDataDelegate(null); } catch (Exception e) {}
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGen != null) { toneGen.release(); toneGen = null; }
    }
}