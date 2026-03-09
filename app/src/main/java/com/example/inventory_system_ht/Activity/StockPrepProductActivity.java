package com.example.inventory_system_ht.Activity;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.CompoundButton;
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
import com.example.inventory_system_ht.Models.DODetailResponseDto;
import com.example.inventory_system_ht.Models.GeneralResponse;
import com.example.inventory_system_ht.Models.StockPrepBulkRequest;
import com.example.inventory_system_ht.Models.TagInfoDto;
import com.example.inventory_system_ht.Models.TagModel;
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
    private List<TagModel> scannedList;
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
            showSagaFeedback("Layar dibersihkan!", true);
        });
    }

    // FUNGSI UTAMA: VALIDASI & MATCHING
    private void processScan(String scannedData) {
        // A. Cek duplikasi di list layar
        for (TagModel item : scannedList) {
            if (item.getEpcTag().equalsIgnoreCase(scannedData)) {
                showSagaFeedback("Barang sudah di-scan!", false);
                return;
            }
        }

        if (!isNetworkConnected()) {
            showSagaFeedback("Offline! Data akan disimpan lokal.", false);
            // Kalo offline, kita asumsikan barang bener dulu, nanti validasi pas Save
            saveToLocalDB(new TagInfoDto(scannedData, scannedData, "Pending Sync", "Unknown", "STANDBY"));
            return;
        }

        // B. Nanya ke Server: Ini barang apa?
        api.getTagInfo(token, scannedData).enqueue(new Callback<TagInfoDto>() {
            @Override
            public void onResponse(Call<TagInfoDto> call, Response<TagInfoDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TagInfoDto info = response.body();

                    // C. VALIDASI STATUS (Harus IN_STOCK baru bisa di-Prep)
                    if (!info.getStatus().equals("IN_STOCK")) {
                        showSagaFeedback("Tag " + info.getStatus() + ", harus IN_STOCK!", false);
                        playBeep(false);
                        return;
                    }

                    // D. Simpan ke SQLite & Update UI
                    saveToLocalDB(info);
                } else {
                    showSagaFeedback("Tag tidak terdaftar di sistem!", false);
                    playBeep(false);
                }
            }

            @Override
            public void onFailure(Call<TagInfoDto> call, Throwable t) {
                showSagaFeedback("Gagal cek tag: " + t.getMessage(), false);
            }
        });
    }

    private void saveToLocalDB(TagInfoDto tag) {
        TagModel newScan = new TagModel(
                tag.getEpcTag(),
                tag.getTagId(),
                tag.getItemId(), // 👇 INI YANG KELUPAAN BRE! (ID Produk misal: ITM-001) 👇
                tag.getItemName(),
                currentDoNo,
                0
        );

        new Thread(() -> {
            appDao.insertScannedTag(newScan);
            runOnUiThread(() -> {
                scannedList.add(newScan);
                adapter.notifyItemInserted(scannedList.size() - 1);
                rvTags.scrollToPosition(scannedList.size() - 1);
                scanCount++;
                tvScanned.setText("Scanned : " + scanCount);
                playBeep(true);
            });
        }).start();
    }

    private void submitToBackend() {
        if (scannedList.isEmpty()) return;

        List<String> codes = new ArrayList<>();
        for (TagModel t : scannedList) codes.add(t.getEpcTag());

        String scannerType = switchRfid.isChecked() ? "RFID" : "QR";
        StockPrepBulkRequest request = new StockPrepBulkRequest(currentDoId, codes, scannerType);

        showSagaFeedback("Mengirim data ke server...", true);

        api.submitStockPrep(token, request).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                if (response.isSuccessful()) {
                    new Thread(() -> {
                        for (TagModel t : scannedList) appDao.markTagAsSynced(t.getEpcTag());
                        runOnUiThread(() -> {
                            showSagaFeedback("SUCCESS: Barang Berhasil di-Prepare!", true);
                            scannedList.clear();
                            adapter.notifyDataSetChanged();
                            scanCount = 0;
                            tvScanned.setText("Scanned : 0");
                        });
                    }).start();
                } else {
                    showSagaFeedback("Gagal: Cek kembali daftar DO lu Jan.", false);
                }
            }

            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                showSagaFeedback("Error Koneksi: " + t.getMessage(), false);
            }
        });
    }

    private void loadPendingScans() {
        new Thread(() -> {
            List<TagModel> pending = appDao.getPendingTags();
            runOnUiThread(() -> {
                for (TagModel t : pending) {
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

    private void playBeep(boolean success) {
        if (toneGen != null) toneGen.startTone(success ? ToneGenerator.TONE_PROP_BEEP : ToneGenerator.TONE_CDMA_HIGH_L, 150);
    }

    @Override protected void onResume() { super.onResume(); setupScanner(); }
    @Override protected void onPause() {
        super.onPause();
        if (mCommScanner != null) {
            try { mCommScanner.getRFIDScanner().setDataDelegate(null); mCommScanner.getBarcodeScanner().setDataDelegate(null); } catch (Exception e) {}
        }
    }
    private String formatToEnglishDate(String rawDate) {
        try {
            // 1. Definisikan format asal (sesuai kiriman BE/Database lu)
            // Kalau dari BE biasanya: 2026-03-09T11:11:31 atau 2026-03-09
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            // 2. Definisikan format tujuan (24 May 2025)
            // "dd" untuk tanggal, "MMMM" untuk nama bulan penuh, "yyyy" untuk tahun
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);

            // 3. Eksekusi
            java.util.Date date = inputFormat.parse(rawDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            // Kalau error (misal format gak cocok), balikin string aslinya aja biar gak crash
            return rawDate;
        }
    }
}