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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

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
import com.example.inventory_system_ht.Helper.AppDao;
import com.example.inventory_system_ht.Helper.AppDatabase;
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.Models.GeneralResponse;
import com.example.inventory_system_ht.Models.StockTakingModels;
import com.example.inventory_system_ht.Models.TagModels;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StockTakingActivity extends BaseScannerActivity implements BarcodeDataDelegate, RFIDDataDelegate {
    private ImageView btnBack;
    private Switch switchRfid;
    private CardView btnRefresh, btnFinalize;
    private EditText resultScan;
    private RecyclerView rvTags;
    private CommScanner mCommScanner;
    private ToneGenerator toneGen;
    private Handler handler = new Handler(Looper.getMainLooper());
    private AppDao appDao;
    private TagAdapter adapter;
    private List<TagModels.TagModel> masterStockList;
    private ApiService api;
    private String token;
    private String activeSttId = "";
    private TagModels.TagModel selectedTag = null;

    @Override
    protected CommScanner getScannerInstance() {
        return mCommScanner;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_taking_adjustment);

        PrefManager pref = new PrefManager(this);
        token = "Bearer " + pref.getToken();
        api = ApiClient.getClient(this).create(ApiService.class);

        try { toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100); } catch (Exception e) {}

        btnBack = findViewById(R.id.btnBack);
        switchRfid = findViewById(R.id.switchRfid);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnFinalize = findViewById(R.id.btnSave);
        resultScan = findViewById(R.id.resultScan);
        rvTags = findViewById(R.id.rvTags);

        masterStockList = new ArrayList<>();
        adapter = new TagAdapter(masterStockList);
        appDao = AppDatabase.getDatabase(this).appDao();
        rvTags.setLayoutManager(new LinearLayoutManager(this));

        adapter.setOnItemClickListener(this::showAdjustmentDialog);
        rvTags.setAdapter(adapter);

        switchRfid.setChecked(false);
        setupScanner();
        setupListeners();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        CardView btnPowerDropdown = findViewById(R.id.btnPowerDropdown);
        TextView tvPowerLevel     = findViewById(R.id.tvPowerLevel);

        setupPowerDropdown(btnPowerDropdown, switchRfid, tvPowerLevel);

        switchRfid.setOnCheckedChangeListener((buttonView, isChecked) -> {
            CommScanner currentScanner = getScannerInstance();

            if (isChecked) {
                boolean isRfidReady = (currentScanner != null && currentScanner.getRFIDScanner() != null);
                if (!isRfidReady) {
                    showSagaFeedback("Failed: HT is not connected to the RFID Reader yet!", false);
                    switchRfid.setChecked(false);
                    return;
                }
                btnPowerDropdown.setVisibility(View.VISIBLE);
            } else {
                btnPowerDropdown.setVisibility(View.GONE);
            }

            showSagaFeedback(isChecked ? "RFID Mode Active" : "Barcode Mode Active", true);
            resultScan.requestFocus();
        });

        btnRefresh.setOnClickListener(v -> {
            if (!isNetworkConnected()) {
                showSagaFeedback("Offline bro, check connection!", false);
                return;
            }
            startStockTakingSession();
        });

        btnFinalize.setOnClickListener(v -> {
            if (activeSttId.isEmpty()) {
                showSagaFeedback("There are no sessions running yet bro!", false);
                return;
            }
            finalizeSession();
        });

        resultScan.setOnEditorActionListener((v, actionId, event) -> {
            String data = resultScan.getText().toString().trim();
            if (!data.isEmpty()) {
                processScanResult(data);
                resultScan.setText("");
            }
            return true;
        });
    }

    private void startStockTakingSession() {
        showLoading();
        showSagaFeedback("Opening a Session...", true);
        EditText etNote = findViewById(R.id.etAdjustmentNote);
        String note = etNote.getText().toString().trim();

        StockTakingModels.CreateReq req = new StockTakingModels.CreateReq(note.isEmpty() ? "PT Sato Routine Inventory" : note);
        api.createStockTaking(token, req).enqueue(new Callback<StockTakingModels.CreateRes>() {
            @Override
            public void onResponse(Call<StockTakingModels.CreateRes> call, Response<StockTakingModels.CreateRes> response) {
                if (response.isSuccessful() && response.body() != null) {
                    activeSttId = response.body().stockTakingId;
                    fetchMasterStock();
                } else {
                    hideLoading();
                    showSagaFeedback("Failed to create session!", false);
                }
            }
            @Override
            public void onFailure(Call<StockTakingModels.CreateRes> call, Throwable t) {
                hideLoading();
                showSagaFeedback("RTO: " + t.getMessage(), false);
            }
        });
    }

    private void fetchMasterStock() {
        showLoading();
        api.getStockData(token).enqueue(new Callback<List<TagModels.TagModel>>() {
            @Override
            public void onResponse(Call<List<TagModels.TagModel>> call, Response<List<TagModels.TagModel>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    masterStockList.clear();
                    masterStockList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    showSagaFeedback("Pulled successfully " + masterStockList.size() + " data!", true);
                }
            }
            @Override
            public void onFailure(Call<List<TagModels.TagModel>> call, Throwable t) {
                hideLoading();
                showSagaFeedback("Failed to retrieve master data!", false);
            }
        });
    }

    private void processScanResult(String data) {
        if (activeSttId.isEmpty()) {
            playScanFeedback(2);
            return;
        }

        if (!isNetworkConnected()) {
            playScanFeedback(0);
            showSagaFeedback("Offline! Data is saved on your phone first.", false);

            new Thread(() -> {
                TagModels.TagModel offlineTag = new TagModels.TagModel(data, data, "STT_OFFLINE", "Stock Taking", activeSttId, 0);
                appDao.insertScannedTag(offlineTag);

                runOnUiThread(() -> markItemAsScanned(data));
            }).start();
            return;
        }

        StockTakingModels.ScanReq req = new StockTakingModels.ScanReq(activeSttId, data);
        api.scanStockTaking(token, req).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                if (response.isSuccessful()) {
                    playScanFeedback(0);
                    markItemAsScanned(data);
                } else {
                    handleApiError(response.code());
                    playScanFeedback(2);
                }
            }
            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                playScanFeedback(2);
            }
        });
    }

    private void finalizeSession() {
        if (!isNetworkConnected()) {
            showSagaFeedback("Still Offline! Find a signal first to finalize.", false);
            playScanFeedback(2);
            return;
        }

        showLoading();
        showSagaFeedback("Checking offline data...", true);

        new Thread(() -> {
            List<TagModels.TagModel> pendingTags = appDao.getPendingTags();
            List<String> tagsToSync = new ArrayList<>();

            for (TagModels.TagModel tag : pendingTags) {
                if (tag.getProductName().equals("Stock Taking") && tag.getDoIdRef().equals(activeSttId)) {
                    tagsToSync.add(tag.getEpcTag());
                }
            }

            runOnUiThread(() -> {
                if (!tagsToSync.isEmpty()) {
                    showSagaFeedback("Syncing " + tagsToSync.size() + " data offline...", true);
                    syncStockTakingData(tagsToSync, 0);
                } else {
                    executeFinalizeAPI();
                }
            });
        }).start();
    }

    private void syncStockTakingData(List<String> tags, int currentIndex) {
        if (currentIndex >= tags.size()) {
            new Thread(() -> {
                for (String epc : tags) appDao.markTagAsSynced(epc);
                runOnUiThread(this::executeFinalizeAPI);
            }).start();
            return;
        }

        StockTakingModels.ScanReq req = new StockTakingModels.ScanReq(activeSttId, tags.get(currentIndex));
        api.scanStockTaking(token, req).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                syncStockTakingData(tags, currentIndex + 1);
            }
            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                syncStockTakingData(tags, currentIndex + 1);
            }
        });
    }

    private void executeFinalizeAPI() {
        showSagaFeedback("Finalizing Session...", true);
        StockTakingModels.FinalizeReq req = new StockTakingModels.FinalizeReq(activeSttId);
        api.finalizeStockTaking(token, req).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                hideLoading();
                if(response.isSuccessful()) {
                    showSagaFeedback("Session Completed and Data Saved!", true);
                    playScanFeedback(0);
                    activeSttId = "";
                    masterStockList.clear();
                    adapter.notifyDataSetChanged();
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

    private void markItemAsScanned(String epcOrBarcode) {
        for (int i = 0; i < masterStockList.size(); i++) {
            TagModels.TagModel tag = masterStockList.get(i);
            if (tag.getEpcTag().equalsIgnoreCase(epcOrBarcode) || tag.getTagId().equalsIgnoreCase(epcOrBarcode)) {

                tag.setScanned(true);
                adapter.notifyItemChanged(i);
                rvTags.smoothScrollToPosition(i);
                break;
            }
        }
    }

    public void showAdjustmentDialog(TagModels.TagModel tagToAdjust) {
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

        btnRemove.setOnClickListener(v -> {
            if (activeSttId.isEmpty() || selectedTag == null) return;

            showLoading();
            StockTakingModels.RemoveReq req = new StockTakingModels.RemoveReq(activeSttId, selectedTag.getTagId());
            api.removeStockTaking(token, req).enqueue(new Callback<GeneralResponse>() {
                @Override
                public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                    hideLoading();
                    if (response.isSuccessful()) {
                        showSagaFeedback("Items Removed!", true);
                        dialog.dismiss();
                        fetchMasterStock();
                    } else {
                        handleApiError(response.code());
                    }
                }
                @Override
                public void onFailure(Call<GeneralResponse> call, Throwable t) {
                    handleFailure(t);
                }
            });
        });

        btnAddManual.setOnClickListener(v -> {
            dialog.dismiss();
            showManualAddDialog();
        });

        dialog.show();
    }

    private void showManualAddDialog() {
        if (activeSttId.isEmpty()) {
            showSagaFeedback("Open a session first before adding manually!", false);
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_manual_add);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText inputItemId = dialog.findViewById(R.id.etManualItemId);
        EditText inputRemark = dialog.findViewById(R.id.etManualRemark);
        Button btnCancel = dialog.findViewById(R.id.btnCancelManual);
        Button btnSave = dialog.findViewById(R.id.btnSaveManual);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String itemId = inputItemId.getText().toString().trim();
            String remark = inputRemark.getText().toString().trim();

            if (itemId.isEmpty()) {
                showSagaFeedback("Item ID cannot be empty!", false);
                return;
            }
            showLoading();
            showSagaFeedback("Save manual data...", true);

            StockTakingModels.ManualAddReq req = new StockTakingModels.ManualAddReq(activeSttId, itemId, remark);
            api.manualAddStockTaking(token, req).enqueue(new Callback<GeneralResponse>() {
                @Override
                public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                    hideLoading();
                    if(response.isSuccessful()) {
                        showSagaFeedback("Manual Add Successful!", true);
                        playScanFeedback(0);
                        dialog.dismiss();
                    } else {
                        handleApiError(response.code());
                        playScanFeedback(2);
                    }
                }
                @Override
                public void onFailure(Call<GeneralResponse> call, Throwable t) {
                    handleFailure(t);
                    playScanFeedback(2);
                }
            });
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