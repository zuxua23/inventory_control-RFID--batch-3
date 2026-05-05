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

import com.example.inventory_system_ht.Adapter.StockTakingItemAdapter;
import com.example.inventory_system_ht.Helper.ApiClient;
import com.example.inventory_system_ht.Helper.ApiService;
import com.example.inventory_system_ht.Helper.AppDatabase;
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.Models.GeneralResponse;
import com.example.inventory_system_ht.Models.StockTakingModels;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StockTakingActivity extends BaseScannerActivity
        implements BarcodeDataDelegate, RFIDDataDelegate {

    // ── Views ─────────────────────────────────────────────────────
    private ImageView    btnBack;
    private Switch       switchRfid;
    private CardView     btnSave;
    private CardView     btnRefresh;
    private EditText     resultScan;
    private RecyclerView rvTags;
    private TextView     tvRemark, tvLocation, tvQty, tvSyncStatus;

    // ── Scanner ───────────────────────────────────────────────────
    private CommScanner   mCommScanner;
    private ToneGenerator toneGen;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // ── Data ──────────────────────────────────────────────────────
    private ApiService  api;
    private AppDatabase db;
    private String      token;
    private String      sttId  = "";
    private String      remark = "";

    private final List<StockTakingModels.SessionItem>  sessionItems  = new ArrayList<>();
    private final Map<String, Integer>                  epcIndexMap   = new HashMap<>();
    private final List<StockTakingModels.ManualAddReq> manualEntries = new ArrayList<>();

    private boolean hasChanges = false;
    private StockTakingItemAdapter adapter;

    // ─────────────────────────────────────────────────────────────

    @Override
    protected CommScanner getScannerInstance() { return mCommScanner; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_taking_adjustment);

        sttId  = getIntent().getStringExtra("sttId");
        remark = getIntent().getStringExtra("remark");
        android.util.Log.d("STT", "sttId: " + sttId + " | remark: " + remark);

        if (sttId == null || sttId.isEmpty()) {
            showError("Session ID not found!");
            finish();
            return;
        }
        if (remark == null) remark = "";

        PrefManager pref = new PrefManager(this);
        token = "Bearer " + pref.getToken();
        api   = ApiClient.getClient(this).create(ApiService.class);
        db    = AppDatabase.getDatabase(this);

        try { toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100); }
        catch (Exception e) { /* ignore */ }

        // Bind views
        btnBack      = findViewById(R.id.btnBack);
        switchRfid   = findViewById(R.id.switchRfid);
        btnSave      = findViewById(R.id.btnSave);
        btnRefresh   = findViewById(R.id.btnRefresh);
        resultScan   = findViewById(R.id.resultScan);
        rvTags       = findViewById(R.id.rvTags);
        tvRemark     = findViewById(R.id.tvRemark);
        tvLocation   = findViewById(R.id.tvLocation);
        tvQty        = findViewById(R.id.tvQty);
        tvSyncStatus = findViewById(R.id.tvSyncStatus);

        // Setup adapter
        adapter = new StockTakingItemAdapter(sessionItems);
        adapter.setOnItemClickListener(this::showAdjustmentDialog);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);
        rvTags.setItemAnimator(null);

        tvRemark.setText("Note: " + (remark.isEmpty() ? "-" : remark));

        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() { handleBackPressed(); }
                });

        setupScanner();
        setupListeners();

        // Load data: utamakan dari server, fallback ke cache Room
        if (isNetworkConnected()) {
            loadSessionTagsFromServer();
        } else {
            loadSessionTagsFromCache();
            showWarning("Offline — loading cached data.");
        }
    }

    // ── Setup ─────────────────────────────────────────────────────

    private void setupListeners() {
        btnBack.setOnClickListener(v -> handleBackPressed());

        CardView btnPowerDropdown = findViewById(R.id.btnPowerDropdown);
        TextView tvPowerLevel     = findViewById(R.id.tvPowerLevel);
        setupPowerDropdown(btnPowerDropdown, switchRfid, tvPowerLevel);

        switchRfid.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                CommScanner s = getScannerInstance();
                if (s == null || s.getRFIDScanner() == null) {
                    showError("HT is not connected to RFID Reader!");
                    switchRfid.setChecked(false);
                    return;
                }
                btnPowerDropdown.setVisibility(View.VISIBLE);
            } else {
                btnPowerDropdown.setVisibility(View.GONE);
            }
            showSuccess(isChecked ? "RFID Mode Active" : "Barcode Mode Active");
            resultScan.requestFocus();
        });

        btnSave.setOnClickListener(v -> {
            if (sttId.isEmpty()) { showWarning("No active session!"); return; }
            String msg = "Submit scan results to server? Scanned: " + countScanned();
            showCustomConfirmDialog(msg, this::handleSave);
        });

        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                if (!isNetworkConnected()) {
                    showWarning("No internet. Showing cached data.");
                    loadSessionTagsFromCache();
                    return;
                }
                showLoading();
                new Thread(() -> {
                    db.appDao().clearSessionItemsBySttId(sttId);
                    handler.post(this::loadSessionTagsFromServer);
                }).start();
            });
        }

        resultScan.setOnEditorActionListener((v, actionId, event) -> {
            String data = resultScan.getText().toString().trim();
            if (!data.isEmpty()) {
                processScan(data);
                resultScan.setText("");
            }
            return true;
        });
    }

    private void setupScanner() {
        if (mCommScanner != null) {
            try {
                mCommScanner.getRFIDScanner().setDataDelegate(this);
                mCommScanner.getBarcodeScanner().setDataDelegate(this);
            } catch (Exception e) { /* ignore */ }
        }
    }

    // ── Load Data (Server) ────────────────────────────────────────

    private void loadSessionTagsFromServer() {
        if (!isNetworkConnected()) {
            loadSessionTagsFromCache();
            showWarning("Offline — loading cached data.");
            return;
        }
        showLoading();
        api.getSessionTags(token, sttId)
                .enqueue(new Callback<List<StockTakingModels.SessionItem>>() {
                    @Override
                    public void onResponse(Call<List<StockTakingModels.SessionItem>> call,
                                           Response<List<StockTakingModels.SessionItem>> response) {
                        hideLoading();
                        if (response.isSuccessful() && response.body() != null) {
                            List<StockTakingModels.SessionItem> body = response.body();

                            sessionItems.clear();
                            epcIndexMap.clear();
                            for (StockTakingModels.SessionItem item : body) {
                                item.state = "PENDING";
                                if (item.epcTag != null)
                                    epcIndexMap.put(item.epcTag.toUpperCase(), sessionItems.size());
                                sessionItems.add(item);
                            }

                            adapter.notifyDataSetChanged();
                            updateInfo();
                            updateSyncStatus();

                            saveSessionItemsToCache(body);
                            showSuccess("Loaded: " + body.size() + " items");
                        } else {
                            showError("Error " + response.code() + " — loading from cache.");
                            android.util.Log.e("STT", "Error code: " + response.code());
                            loadSessionTagsFromCache();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<StockTakingModels.SessionItem>> call,
                                          Throwable t) {
                        hideLoading();
                        showError("Failed: " + t.getMessage() + " — loading from cache.");
                        android.util.Log.e("STT", "onFailure: " + t.getMessage());
                        loadSessionTagsFromCache();
                    }
                });
    }

    // ── Load Data (Cache Room) ────────────────────────────────────

    private void loadSessionTagsFromCache() {
        showLoading();
        new Thread(() -> {
            List<StockTakingModels.SessionItemEntity> cached =
                    db.appDao().getSessionItemsBySttId(sttId);
            handler.post(() -> {
                hideLoading();
                TextView tvEmpty = findViewById(R.id.tvEmpty);
                if (cached.isEmpty()) {
                    if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                    showWarning("No cached data. Connect to internet and tap Refresh.");
                    return;
                }
                if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                sessionItems.clear();
                epcIndexMap.clear();
                for (StockTakingModels.SessionItemEntity e : cached) {
                    StockTakingModels.SessionItem item = e.toSessionItem();
                    if (item.epcTag != null)
                        epcIndexMap.put(item.epcTag.toUpperCase(), sessionItems.size());
                    sessionItems.add(item);
                }
                adapter.notifyDataSetChanged();
                updateInfo();
                updateSyncStatus();
            });
        }).start();
    }

    // ── Save to Cache ─────────────────────────────────────────────

    private void saveSessionItemsToCache(List<StockTakingModels.SessionItem> items) {
        new Thread(() -> {
            db.appDao().clearSessionItemsBySttId(sttId);
            List<StockTakingModels.SessionItemEntity> entities = new ArrayList<>();
            for (StockTakingModels.SessionItem item : items) {
                entities.add(StockTakingModels.SessionItemEntity.from(sttId, item));
            }
            if (!entities.isEmpty()) db.appDao().insertSessionItems(entities);
        }).start();
    }

    // ── Scan Processing ───────────────────────────────────────────

    private void processScan(String epcOrBarcode) {
        if (sttId.isEmpty()) { playScanFeedback(2); return; }

        android.util.Log.d("SCAN", "Input: " + epcOrBarcode);

        Integer idx = epcIndexMap.get(epcOrBarcode.toUpperCase());

        if (idx == null) {
            for (int i = 0; i < sessionItems.size(); i++) {
                if (epcOrBarcode.equalsIgnoreCase(sessionItems.get(i).tagId)) {
                    idx = i;
                    break;
                }
            }
        }

        if (idx == null) {
            playScanFeedback(2);
            showWarning("Tag not found: " + epcOrBarcode);
            android.util.Log.d("SCAN", "Not found. Map: " + epcIndexMap.keySet());
            return;
        }

        StockTakingModels.SessionItem item = sessionItems.get(idx);
        if (!"PENDING".equals(item.state)) {
            showWarning("Already scanned.");
            return;
        }

        item.state = "FOUND";
        hasChanges = true;
        adapter.notifyItemChanged(idx);
        rvTags.scrollToPosition(idx);
        updateInfo();
        playScanFeedback(0);

        saveToQueue(item.epcTag, "FOUND", null, null);
        if (isNetworkConnected()) {
            syncSingleScan(item.epcTag);
        } else {
            updateSyncStatus();
        }
    }

    // ── Queue & Sync ──────────────────────────────────────────────

    private void saveToQueue(String epc, String action, String itemId, String remark) {
        new Thread(() -> {
            StockTakingModels.ScanQueueEntity entity = new StockTakingModels.ScanQueueEntity();
            entity.sttId     = sttId;
            entity.epcTag    = epc;
            entity.action    = action;
            entity.itemId    = itemId;
            entity.remark    = remark;
            entity.isSynced  = false;
            entity.createdAt = System.currentTimeMillis();
            db.appDao().insertScanQueue(entity);
            handler.post(this::updateSyncStatus);
        }).start();
    }

    private void syncSingleScan(String epc) {
        StockTakingModels.ScanReq req = new StockTakingModels.ScanReq(sttId, epc);
        api.scanStockTaking(token, req).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> res) {
                if (res.isSuccessful()) {
                    new Thread(() -> {
                        db.appDao().markSyncedByEpc(sttId, epc);
                        handler.post(StockTakingActivity.this::updateSyncStatus);
                    }).start();
                }
            }
            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) { /* retry nanti */ }
        });
    }

    private void syncPendingQueue() {
        if (!isNetworkConnected()) return;
        new Thread(() -> {
            List<StockTakingModels.ScanQueueEntity> pending =
                    db.appDao().getUnsyncedBySttId(sttId);
            if (pending.isEmpty()) return;

            List<String> foundEpcs = new ArrayList<>();
            for (StockTakingModels.ScanQueueEntity q : pending) {
                if ("FOUND".equals(q.action)) foundEpcs.add(q.epcTag);
            }
            if (!foundEpcs.isEmpty()) {
                try {
                    StockTakingModels.BulkScanReq req =
                            new StockTakingModels.BulkScanReq(sttId, foundEpcs);
                    Response<GeneralResponse> res =
                            api.bulkScanStockTaking(token, req).execute();
                    if (res.isSuccessful()) {
                        db.appDao().markBulkSynced(sttId, foundEpcs);
                        handler.post(() -> {
                            updateSyncStatus();
                            showSuccess("Synced " + foundEpcs.size() + " scans");
                        });
                    }
                } catch (Exception e) { /* retry nanti */ }
            }

            for (StockTakingModels.ScanQueueEntity q : pending) {
                if ("FOUND".equals(q.action)) continue;
                try {
                    if ("REMOVE".equals(q.action)) {
                        api.removeStockTaking(token,
                                new StockTakingModels.RemoveReq(q.sttId, q.epcTag)).execute();
                    } else if ("MANUAL_ADD".equals(q.action)) {
                        api.manualAddStockTaking(token,
                                new StockTakingModels.ManualAddReq(
                                        q.sttId, q.itemId, q.remark)).execute();
                    }
                    db.appDao().markSyncedById(q.id);
                } catch (Exception e) { /* retry nanti */ }
            }
            handler.post(this::updateSyncStatus);
        }).start();
    }

    // ── Save / Apply Adjustment ───────────────────────────────────

    private void handleSave() {
        if (!isNetworkConnected()) {
            showWarning("No internet. Data saved locally. Will sync when connected.");
            return;
        }
        showLoading();
        new Thread(() -> {
            syncPendingQueueSync();
            handler.post(this::sendApplyAdjustment);
        }).start();
    }

    private void syncPendingQueueSync() {
        List<StockTakingModels.ScanQueueEntity> pending =
                db.appDao().getUnsyncedBySttId(sttId);
        if (pending.isEmpty()) return;

        List<String> foundEpcs = new ArrayList<>();
        for (StockTakingModels.ScanQueueEntity q : pending) {
            if ("FOUND".equals(q.action)) foundEpcs.add(q.epcTag);
        }
        if (!foundEpcs.isEmpty()) {
            try {
                Response<GeneralResponse> res = api.bulkScanStockTaking(token,
                        new StockTakingModels.BulkScanReq(sttId, foundEpcs)).execute();
                if (res.isSuccessful()) db.appDao().markBulkSynced(sttId, foundEpcs);
            } catch (Exception e) { /* lanjut */ }
        }
        for (StockTakingModels.ScanQueueEntity q : pending) {
            if ("FOUND".equals(q.action)) continue;
            try {
                if ("REMOVE".equals(q.action)) {
                    api.removeStockTaking(token,
                            new StockTakingModels.RemoveReq(q.sttId, q.epcTag)).execute();
                } else if ("MANUAL_ADD".equals(q.action)) {
                    api.manualAddStockTaking(token,
                            new StockTakingModels.ManualAddReq(
                                    q.sttId, q.itemId, q.remark)).execute();
                }
                db.appDao().markSyncedById(q.id);
            } catch (Exception e) { /* lanjut */ }
        }
    }

    private void sendApplyAdjustment() {
        StockTakingModels.FinalizeReq req = new StockTakingModels.FinalizeReq(sttId);
        api.applyAdjustment(token, req).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    new Thread(() -> {
                        db.appDao().clearSyncedBySttId(sttId);
                        db.appDao().clearSessionItemsBySttId(sttId);
                    }).start();
                    showSuccess("Data submitted! Waiting for admin to finalize.");
                    playScanFeedback(0);
                    hasChanges = false;
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

    // ── UI Update ─────────────────────────────────────────────────

    private void updateInfo() {
        int total   = sessionItems.size();
        int scanned = countScanned();
        tvQty.setText("Qty: " + scanned + "/" + total);

        List<String> locations = new ArrayList<>();
        for (StockTakingModels.SessionItem item : sessionItems) {
            if (item.location != null && !item.location.isEmpty()
                    && !locations.contains(item.location)) {
                locations.add(item.location);
            }
        }
        tvLocation.setText("Location: " + (locations.isEmpty()
                ? "-" : String.join(", ", locations)));
    }

    private void updateSyncStatus() {
        new Thread(() -> {
            int unsynced = db.appDao().countUnsyncedBySttId(sttId);
            handler.post(() -> {
                if (tvSyncStatus == null) return;
                if (unsynced == 0) {
                    tvSyncStatus.setText("✓ All synced");
                    tvSyncStatus.setTextColor(Color.parseColor("#01C470"));
                } else {
                    tvSyncStatus.setText("⚠ " + unsynced + " pending sync");
                    tvSyncStatus.setTextColor(Color.parseColor("#FFA000"));
                }
            });
        }).start();
    }

    private int countScanned() {
        int count = 0;
        for (StockTakingModels.SessionItem item : sessionItems) {
            if ("FOUND".equals(item.state) || "MANUAL_ADD".equals(item.state)) count++;
        }
        return count;
    }

    // ── Session Status Check ──────────────────────────────────────

    private void checkSessionStatus() {
        if (!isNetworkConnected()) return;
        api.getActiveStockTaking(token).enqueue(new Callback<StockTakingModels.ActiveRes>() {
            @Override
            public void onResponse(Call<StockTakingModels.ActiveRes> call,
                                   Response<StockTakingModels.ActiveRes> response) {
                boolean sessionEnded = false;

                if (!response.isSuccessful() || response.body() == null) {
                    // Tidak ada sesi aktif — berarti sudah di-end admin
                    sessionEnded = true;
                } else {
                    StockTakingModels.ActiveRes active = response.body();
                    // SttId berbeda = sesi yang ini sudah tidak aktif
                    sessionEnded = !sttId.equals(active.sttId);
                }

                if (sessionEnded) showSessionEndedDialog();
            }

            @Override
            public void onFailure(Call<StockTakingModels.ActiveRes> call, Throwable t) {
                // Gagal check = biarkan, tidak perlu alert
            }
        });
    }

    private void showSessionEndedDialog() {
        if (isFinishing() || isDestroyed()) return;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sesi Berakhir")
                .setMessage("Sesi stock taking ini sudah diselesaikan oleh admin.\nAnda akan keluar dari halaman ini.")
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    new Thread(() -> {
                        db.appDao().clearSyncedBySttId(sttId);
                        db.appDao().clearSessionItemsBySttId(sttId);
                    }).start();
                    finish();
                })
                .show();
    }

    // ── Adjustment Dialog ─────────────────────────────────────────

    private void showAdjustmentDialog(StockTakingModels.SessionItem selectedItem, int position) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_adj);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageButton btnFaq       = dialog.findViewById(R.id.btnFaq);
        Button      btnRemove    = dialog.findViewById(R.id.btnRemove);
        Button      btnAddManual = dialog.findViewById(R.id.btnAddManual);

        btnFaq.setOnClickListener(v -> showFaqDialog());
        btnRemove.setOnClickListener(v -> {
            dialog.dismiss();
            showRemoveConfirmDialog(selectedItem, position);
        });
        btnAddManual.setOnClickListener(v -> {
            dialog.dismiss();
            showManualAddDialog(selectedItem, position);
        });

        dialog.show();
    }

    private void showRemoveConfirmDialog(StockTakingModels.SessionItem item, int position) {
        showCustomConfirmDialog(
                "Remove this item from the list?\nQty will decrease.",
                () -> {
                    if (item.tagId != null && !item.tagId.isEmpty()) {
                        saveToQueue(item.tagId, "REMOVE", null, null);
                        if (isNetworkConnected()) {
                            api.removeStockTaking(token,
                                            new StockTakingModels.RemoveReq(sttId, item.tagId))
                                    .enqueue(new Callback<GeneralResponse>() {
                                        @Override public void onResponse(
                                                Call<GeneralResponse> c, Response<GeneralResponse> r) {}
                                        @Override public void onFailure(
                                                Call<GeneralResponse> c, Throwable t) {}
                                    });
                        }
                    }
                    sessionItems.remove(position);
                    epcIndexMap.remove(item.epcTag != null ? item.epcTag.toUpperCase() : "");
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, sessionItems.size());
                    hasChanges = true;
                    updateInfo();
                    showSuccess("Item removed.");
                }
        );
    }

    private void showManualAddDialog(StockTakingModels.SessionItem item, int position) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_manual_add);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int w = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etItemId  = dialog.findViewById(R.id.etManualItemId);
        EditText etRemark  = dialog.findViewById(R.id.etManualRemark);
        Button   btnCancel = dialog.findViewById(R.id.btnCancelManual);
        Button   btnSave   = dialog.findViewById(R.id.btnSaveManual);

        etItemId.setText(item.itemId != null ? item.itemId : "");
        etItemId.setEnabled(false);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String remarkText = etRemark.getText().toString().trim();
            if (remarkText.isEmpty()) {
                showWarning("Remark cannot be empty!");
                return;
            }

            // FIX: simpan remark dengan benar ke queue
            saveToQueue(item.epcTag, "MANUAL_ADD", item.itemId, remarkText);

            if (isNetworkConnected()) {
                StockTakingModels.ManualAddReq req =
                        new StockTakingModels.ManualAddReq(sttId, item.itemId, remarkText);
                api.manualAddStockTaking(token, req)
                        .enqueue(new Callback<GeneralResponse>() {
                            @Override
                            public void onResponse(Call<GeneralResponse> c,
                                                   Response<GeneralResponse> r) {
                                if (r.isSuccessful()) {
                                    new Thread(() -> {
                                        List<StockTakingModels.ScanQueueEntity> pending =
                                                db.appDao().getUnsyncedBySttId(sttId);
                                        for (StockTakingModels.ScanQueueEntity q : pending) {
                                            if ("MANUAL_ADD".equals(q.action)
                                                    && item.itemId != null
                                                    && item.itemId.equals(q.itemId)) {
                                                db.appDao().markSyncedById(q.id);
                                                break;
                                            }
                                        }
                                        handler.post(StockTakingActivity.this::updateSyncStatus);
                                    }).start();
                                }
                            }
                            @Override
                            public void onFailure(Call<GeneralResponse> c, Throwable t) {}
                        });
            }

            item.state        = "MANUAL_ADD";
            item.manualRemark = remarkText;
            hasChanges        = true;
            adapter.notifyItemChanged(position);
            updateInfo();
            playScanFeedback(0);
            showSuccess("Manual add saved!");
            dialog.dismiss();
        });

        dialog.show();
    }

    private void handleBackPressed() {
        if (!hasChanges) { finish(); return; }
        showCustomConfirmDialog(
                "Exit without submitting?\nScanned data is saved locally.",
                () -> {
                    sessionItems.clear();
                    epcIndexMap.clear();
                    manualEntries.clear();
                    hasChanges = false;
                    finish();
                }
        );
    }

    // ── FAQ ───────────────────────────────────────────────────────

    private void showFaqDialog() {
        Dialog faqDialog = new Dialog(this);
        faqDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        faqDialog.setContentView(R.layout.dialog_faq);
        if (faqDialog.getWindow() != null) {
            faqDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int w = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            faqDialog.getWindow().setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        faqDialog.show();
    }

    private void showCustomConfirmDialog(String message, Runnable onYes) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int w = (int)(getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        TextView tvMsg  = dialog.findViewById(R.id.tvConfirmMessage);
        Button   btnNo  = dialog.findViewById(R.id.btnConfirmNo);
        Button   btnYes = dialog.findViewById(R.id.btnConfirmYes);
        tvMsg.setText(message);
        btnNo.setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> { dialog.dismiss(); onYes.run(); });
        dialog.show();
    }

    // ── Scanner Callbacks ─────────────────────────────────────────

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

    // ── Lifecycle ─────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        setupScanner();
        syncPendingQueue();
        checkSessionStatus(); // ← cek apakah sesi masih aktif

        updateReaderBattery(findViewById(R.id.ivReaderBattery));

        if (getHTBatteryLevel() <= 15) {
            showWarning("Battery " + getHTBatteryLevel() + "%, please charge!");
            playScanFeedback(2);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCommScanner != null) {
            try {
                if (mCommScanner.getRFIDScanner()    != null)
                    mCommScanner.getRFIDScanner().setDataDelegate(null);
                if (mCommScanner.getBarcodeScanner() != null)
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