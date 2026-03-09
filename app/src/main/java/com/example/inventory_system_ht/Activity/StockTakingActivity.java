package com.example.inventory_system_ht.Activity;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;

import androidx.cardview.widget.CardView;
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
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.Models.GeneralResponse;
import com.example.inventory_system_ht.Models.StockTakingModels;
import com.example.inventory_system_ht.Models.TagModel;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StockTakingActivity extends BaseScannerActivity implements BarcodeDataDelegate, RFIDDataDelegate {

    private ImageView btnBack;
    private Switch switchRfid;
    private CardView btnRefresh;
    private EditText resultScan;
    private RecyclerView rvTags;

    // SDK & UTILS GLOBAL
    private CommScanner mCommScanner;
    private ToneGenerator toneGen;
    private Handler handler = new Handler(Looper.getMainLooper());

    // ADAPTER & DATA
    private TagAdapter adapter;
    private List<TagModel> masterStockList;

    // BACKEND TOOLS
    private ApiService api;
    private String token;
    private String activeSttId = ""; // Nyimpen Session ID dari Backend
    private TagModel selectedTag = null; // Tag yang dipilih buat di-remove

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_taking_adjustment);

        // 1. INIT BACKEND
        PrefManager pref = new PrefManager(this);
        token = "Bearer " + pref.getToken();
        api = ApiClient.getClient(this).create(ApiService.class);

        // 2. INIT UI
        try { toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100); } catch (Exception e) {}
        btnBack = findViewById(R.id.btnBack);
        switchRfid = findViewById(R.id.switchRfid);
        btnRefresh = findViewById(R.id.btnRefresh);
        resultScan = findViewById(R.id.resultScan);
        rvTags = findViewById(R.id.rvTags);

        // Setup RecyclerView
        masterStockList = new ArrayList<>();
        adapter = new TagAdapter(masterStockList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));

        // 👇 INI DIA KABELNYA BRE, JANGAN SAMPE KELUPAAN LAGI 👇
        adapter.setOnItemClickListener(item -> {
            showAdjustmentDialog(item);
        });

        rvTags.setAdapter(adapter); // Pastiin ini tetep ditaruh di paling bawah

        switchRfid.setChecked(false);
        setupScanner();
        setupListeners();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // SMART SWITCH LOGIC
        switchRfid.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                boolean isRfidReady = (mCommScanner != null && mCommScanner.getRFIDScanner() != null);
                if (!isRfidReady) {
                    showSagaFeedback("Failed: HT is not connected to the RFID Reader yet!", false);
                    switchRfid.setChecked(false);
                    return;
                }
            }
            showSagaFeedback(isChecked ? "RFID Mode Active" : "Barcode Mode Active", true);
            resultScan.requestFocus();
        });

        // REFRESH DATA -> BUKA SESI & TARIK DATA MASTER
        btnRefresh.setOnClickListener(v -> {
            if (!isNetworkConnected()) {
                showSagaFeedback("Refresh Failed: You are offline, bro!", false);
                return;
            }
            startStockTakingSession();
        });

        // WEDGE SCANNER (LASER MANUAL)
        resultScan.setOnEditorActionListener((v, actionId, event) -> {
            String data = resultScan.getText().toString().trim();
            if (!data.isEmpty()) {
                processScanResult(data);
                resultScan.setText("");
            }
            return true;
        });
    }

    // ==========================================
    // API LOGIC: SIKLUS STOCK TAKING
    // ==========================================

    private void startStockTakingSession() {
        showSagaFeedback("Membuka Sesi Stock Taking...", true);

        StockTakingModels.CreateReq req = new StockTakingModels.CreateReq("Opname Rutin PT Sato");
        api.createStockTaking(token, req).enqueue(new Callback<StockTakingModels.CreateRes>() {
            @Override
            public void onResponse(Call<StockTakingModels.CreateRes> call, Response<StockTakingModels.CreateRes> response) {
                if (response.isSuccessful() && response.body() != null) {
                    activeSttId = response.body().stockTakingId;
                    fetchMasterStock(); // Lanjut narik data IN_STOCK
                } else {
                    showSagaFeedback("Gagal bikin sesi di Server!", false);
                }
            }

            @Override
            public void onFailure(Call<StockTakingModels.CreateRes> call, Throwable t) {
                showSagaFeedback("RTO: " + t.getMessage(), false);
            }
        });
    }

    private void fetchMasterStock() {
        showSagaFeedback("Menarik data master IN_STOCK...", true);

        api.getStockData(token).enqueue(new Callback<List<TagModel>>() {
            @Override
            public void onResponse(Call<List<TagModel>> call, Response<List<TagModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    masterStockList.clear();
                    masterStockList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    showSagaFeedback("Berhasil narik " + masterStockList.size() + " data!", true);
                }
            }

            @Override
            public void onFailure(Call<List<TagModel>> call, Throwable t) {
                showSagaFeedback("Gagal narik data master!", false);
            }
        });
    }

    private void processScanResult(String data) {
        if (activeSttId.isEmpty()) {
            showSagaFeedback("Klik Refresh dulu buat buka sesi bre!", false);
            playBeep(false);
            return;
        }

        StockTakingModels.ScanReq req = new StockTakingModels.ScanReq(activeSttId, data);
        api.scanStockTaking(token, req).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                if (response.isSuccessful()) {
                    playBeep(true);
                    showSagaFeedback("FOUND: " + data, true);

                    // TODO: Update UI di list (Misal ganti warna background item di RecyclerView jadi Hijau)
                } else {
                    playBeep(false);
                    showSagaFeedback("Barang aneh/Unregistered!", false);
                }
            }

            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                showSagaFeedback("Koneksi ampas bre: " + t.getMessage(), false);
            }
        });
    }

    // ==========================================
    // DIALOGS & ADJUSTMENT (REMOVE)
    // ==========================================

    // Panggil fungsi ini dari Adapter pas item di RecyclerView diklik
    public void showAdjustmentDialog(TagModel tagToAdjust) {
        this.selectedTag = tagToAdjust;

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

        // EXECUTE REMOVE KE BACKEND
        btnRemove.setOnClickListener(v -> {
            if (!isNetworkConnected()) {
                showSagaFeedback("Adjustment Failed: Internet mati!", false);
                return;
            }
            if (activeSttId.isEmpty() || selectedTag == null) {
                showSagaFeedback("Pilih item dan pastiin sesi aktif!", false);
                return;
            }

            showSagaFeedback("Menghapus barang dari sistem...", true);

            StockTakingModels.RemoveReq req = new StockTakingModels.RemoveReq(activeSttId, selectedTag.getTagId());
            api.removeStockTaking(token, req).enqueue(new Callback<GeneralResponse>() {
                @Override
                public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                    if (response.isSuccessful()) {
                        showSagaFeedback("Barang berhasil di-Remove (Ditandai Kurang)!", true);
                        // TODO: Update UI list (Ganti warna item jadi Merah)
                        dialog.dismiss();
                    } else {
                        showSagaFeedback("Gagal Remove di Server!", false);
                    }
                }

                @Override
                public void onFailure(Call<GeneralResponse> call, Throwable t) {
                    showSagaFeedback("RTO pas Remove: " + t.getMessage(), false);
                }
            });
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

    // ==========================================
    // SDK DENSO IMPLEMENTATION
    // ==========================================

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

    private void playBeep(boolean success) {
        if (toneGen != null) toneGen.startTone(success ? ToneGenerator.TONE_PROP_BEEP : ToneGenerator.TONE_CDMA_HIGH_L, 150);
    }

    @Override protected void onResume() { super.onResume(); setupScanner(); }
    @Override protected void onPause() {
        super.onPause();
        if (mCommScanner != null) {
            try {
                mCommScanner.getRFIDScanner().setDataDelegate(null);
                mCommScanner.getBarcodeScanner().setDataDelegate(null);
            } catch (Exception e) {}
        }
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (toneGen != null) { toneGen.release(); toneGen = null; }
    }
}