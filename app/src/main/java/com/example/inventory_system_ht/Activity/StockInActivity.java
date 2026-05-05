package com.example.inventory_system_ht.Activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.densowave.scannersdk.Barcode.BarcodeDataReceivedEvent;
import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.BarcodeDataDelegate;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.densowave.scannersdk.RFID.RFIDData;
import com.densowave.scannersdk.RFID.RFIDDataReceivedEvent;
import com.example.inventory_system_ht.Adapter.ItemAdapter;
import com.example.inventory_system_ht.Adapter.SumProductAdapter;
import com.example.inventory_system_ht.Helper.ApiClient;
import com.example.inventory_system_ht.Helper.ApiService;
import com.example.inventory_system_ht.Helper.AppDatabase;
import com.example.inventory_system_ht.Helper.ErrorParser;
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.Models.GeneralResponse;
import com.example.inventory_system_ht.Models.ItemModels;
import com.example.inventory_system_ht.Models.LocationModels;
import com.example.inventory_system_ht.Models.StockInRequest;
import com.example.inventory_system_ht.Models.StockInScanEntity;
import com.example.inventory_system_ht.Models.TagModels;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StockInActivity extends BaseScannerActivity implements BarcodeDataDelegate, RFIDDataDelegate {

    // ─── Views ────────────────────────────────────────────────────
    private ImageView btnBack;
    private Button btnClear, btnSave, btnListProduct, btnSumProduct;
    private Switch switchRfid;
    private EditText resultScan, etLocation;
    private TextView tvScanned;
    private RecyclerView rvTags;
    private CardView btnPowerDropdown, cardLocation;
    private ImageView ivLocationArrow;
    private TextView tvPowerLevel;

    // ─── Adapters & Data ──────────────────────────────────────────
    private ItemAdapter adapter;
    private SumProductAdapter sumAdapter;
    private List<ItemModels.ItemModel> scannedItemsList = new ArrayList<>();
    private List<ItemModels.SumProductModel> sumProductList = new ArrayList<>();
    private List<LocationModels.LocationModel> masterLocationList = new ArrayList<>();
    private List<String> locationList = new ArrayList<>();
    private final List<String> powerList = new ArrayList<>(Arrays.asList(
            "10 dBm", "15 dBm", "20 dBm", "25 dBm", "27 dBm"
    ));

    // ─── State ────────────────────────────────────────────────────
    private CommScanner mCommScanner;
    private ToneGenerator toneGen;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AppDatabase db;
    private PopupWindow activePopup = null;

    private int totalScanCount = 0;
    private boolean isListProductTab = true;
    private String selectedLocation = "";
    private String selectedLocationId = "";

    @Override
    protected CommScanner getScannerInstance() { return mCommScanner; }

    // ─── Lifecycle ────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_in);

        db = AppDatabase.getDatabase(this);
        try { toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100); } catch (Exception ignored) {}

        bindViews();
        setupRecyclerView();
        setupTabButtons();
        setupLocationDropdown();
        setupScanner();
        setupPowerDropdown(btnPowerDropdown, switchRfid, tvPowerLevel);

        resultScan.setShowSoftInputOnFocus(false);
        resultScan.postDelayed(() -> resultScan.requestFocus(), 100);

        setupBarcodeTextWatcher();
        setupButtonListeners();

        fetchLocations();
        fetchMasterItems();
        restoreFromRoom();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupScanner();
        updateReaderBattery(findViewById(R.id.ivReaderBattery));
        if (getHTBatteryLevel() <= 15) {
            showWarning("HT Battery " + getHTBatteryLevel() + "%, segera charge!");
            playScanFeedback(2);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (activePopup != null && activePopup.isShowing()) activePopup.dismiss();
        if (mCommScanner != null) {
            try {
                if (mCommScanner.getRFIDScanner() != null)
                    mCommScanner.getRFIDScanner().setDataDelegate(null);
                if (mCommScanner.getBarcodeScanner() != null)
                    mCommScanner.getBarcodeScanner().setDataDelegate(null);
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGen != null) { toneGen.release(); toneGen = null; }
    }

    // ─── Init ─────────────────────────────────────────────────────

    private void bindViews() {
        btnBack          = findViewById(R.id.btnBack);
        btnClear         = findViewById(R.id.btnClear);
        btnSave          = findViewById(R.id.btnSave);
        btnListProduct   = findViewById(R.id.btnListProduct);
        btnSumProduct    = findViewById(R.id.btnSumProduct);
        switchRfid       = findViewById(R.id.switchRfid);
        resultScan       = findViewById(R.id.resultScan);
        tvScanned        = findViewById(R.id.tvScanned);
        rvTags           = findViewById(R.id.rvTags);
        btnPowerDropdown = findViewById(R.id.btnPowerDropdown);
        cardLocation     = findViewById(R.id.cardLocation);
        etLocation       = findViewById(R.id.etLocation);
        ivLocationArrow  = findViewById(R.id.ivLocationArrow);
        tvPowerLevel     = findViewById(R.id.tvPowerLevel);

        switchRfid.setChecked(false);
        btnPowerDropdown.setVisibility(View.GONE);
        rvTags.setItemAnimator(null);
    }

    private void setupRecyclerView() {
        adapter = new ItemAdapter(scannedItemsList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);
        adapter.setOnItemClickListener(item -> {
            if (!isListProductTab) return;
            int pos = scannedItemsList.indexOf(item);
            if (pos != -1) showDeleteItemDialog(item, pos);
        });
    }

    private void setupBarcodeTextWatcher() {
        resultScan.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String data = s.toString().trim();
                if (data.length() >= 8 && !switchRfid.isChecked()) {
                    resultScan.setText("");
                    enqueueScan(data);
                }
            }
        });
        resultScan.setOnKeyListener((v, keyCode, event) ->
                keyCode == android.view.KeyEvent.KEYCODE_ENTER);
    }

    private void setupButtonListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnClear.setOnClickListener(v -> {
            if (scannedItemsList.isEmpty()) { showWarning("Tidak ada item"); return; }
            new AlertDialog.Builder(this)
                    .setTitle("Clear")
                    .setMessage("Hapus semua " + totalScanCount + " item dari list?")
                    .setPositiveButton("Hapus", (d, w) -> {
                        new Thread(() -> db.appDao().clearAllStockInScans()).start();
                        clearAllData();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });

        btnSave.setOnClickListener(v -> {
            if (scannedItemsList.isEmpty()) { showWarning("No items scanned yet"); return; }
            if (selectedLocationId.isEmpty()) { showWarning("Pilih lokasi terlebih dahulu"); return; }
            showSaveConfirmDialog();
        });
    }

    private void setupScanner() {
        if (mCommScanner != null) {
            try {
                mCommScanner.getRFIDScanner().setDataDelegate(this);
                mCommScanner.getBarcodeScanner().setDataDelegate(this);
            } catch (Exception ignored) {}
        }
    }

    // ─── Restore Session ──────────────────────────────────────────

    private void restoreFromRoom() {
        new Thread(() -> {
            List<StockInScanEntity> saved = db.appDao().getAllStockInScans();
            if (saved.isEmpty()) return;

            List<ItemModels.ItemModel> restored = new ArrayList<>();
            String locId = null;
            for (StockInScanEntity e : saved) {
                restored.add(new ItemModels.ItemModel(
                        e.epcTag,
                        e.itemId != null ? e.itemId : "",
                        e.isResolved ? e.itemName : "Pending...",
                        1
                ));
                if (locId == null && e.locationId != null && !e.locationId.isEmpty())
                    locId = e.locationId;
            }

            final String finalLocId = locId;
            handler.post(() -> {
                scannedItemsList.clear();
                scannedItemsList.addAll(restored);
                totalScanCount = scannedItemsList.size();
                adapter.notifyDataSetChanged();
                updateScanCount();

                if (finalLocId != null) {
                    selectedLocationId = finalLocId;
                    for (LocationModels.LocationModel loc : masterLocationList) {
                        if (loc.getId().equals(finalLocId)) {
                            selectedLocation = loc.getName();
                            etLocation.setText(selectedLocation);
                            break;
                        }
                    }
                }

                showWarning(totalScanCount + " item tersimpan dari sesi sebelumnya");
            });
        }).start();
    }

    // ─── Scan ─────────────────────────────────────────────────────

    private void enqueueScan(String scannedData) {
        // Cek duplikat in-memory
        for (ItemModels.ItemModel t : scannedItemsList) {
            if (t.getEpcTag().equalsIgnoreCase(scannedData)) {
                playScanFeedback(1);
                showWarning("Item already in the list");
                return;
            }
        }

        if (selectedLocationId.isEmpty()) {
            showWarning("Pilih lokasi terlebih dahulu");
            return;
        }

        // Tambah placeholder ke UI dulu
        addItemToList(new ItemModels.ItemModel(scannedData, "", "Loading...", 1));
        playScanFeedback(0);

        if (!isNetworkConnected()) {
            // Simpan ke Room langsung tanpa resolve (offline)
            new Thread(() -> {
                StockInScanEntity entity = buildEntity(scannedData, "", "Loading...", false);
                db.appDao().insertStockInScan(entity);
            }).start();
            showWarning("Offline – item disimpan lokal");
            return;
        }

        // Online → resolve tag dulu baru simpan ke Room
        String token = "Bearer " + new PrefManager(this).getToken();
        String type  = switchRfid.isChecked() ? "RFID" : "QR";

        ApiClient.getClient(this).create(ApiService.class)
                .getTagByCode(token, scannedData, type)
                .enqueue(new Callback<TagModels.TagResponseDto>() {
                    @Override
                    public void onResponse(Call<TagModels.TagResponseDto> call,
                                           Response<TagModels.TagResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            TagModels.TagResponseDto tag = response.body();
                            // Simpan ke Room (resolved)
                            new Thread(() -> {
                                StockInScanEntity entity = buildEntity(
                                        scannedData, tag.getItemId(), tag.getItemName(), true);
                                db.appDao().insertStockInScan(entity);
                            }).start();
                            // Update UI
                            runOnUiThread(() ->
                                    updateItemInList(scannedData, tag.getItemId(), tag.getItemName()));
                        } else {
                            // Tidak dikenali → hapus dari UI, jangan simpan ke Room
                            runOnUiThread(() -> {
                                removeItemFromList(scannedData);
                                playScanFeedback(2);
                                showError("Item not registered: " + scannedData);
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<TagModels.TagResponseDto> call, Throwable t) {
                        // Simpan ke Room tanpa resolve, tampilkan warning
                        new Thread(() -> {
                            StockInScanEntity entity = buildEntity(scannedData, "", "Loading...", false);
                            db.appDao().insertStockInScan(entity);
                        }).start();
                        handler.post(() -> showWarning("Jaringan bermasalah – item disimpan lokal"));
                    }
                });
    }

    private StockInScanEntity buildEntity(String epc, String itemId, String itemName, boolean resolved) {
        StockInScanEntity e = new StockInScanEntity();
        e.epcTag      = epc;
        e.itemId      = itemId;
        e.itemName    = itemName;
        e.scannerType = switchRfid.isChecked() ? "RFID" : "QR";
        e.locationId  = selectedLocationId;
        e.isResolved  = resolved;
        e.isSynced    = false;
        e.createdAt   = System.currentTimeMillis();
        return e;
    }

    // ─── Save: Room dulu → cek internet → kirim API ───────────────

    private void showSaveConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("Save " + totalScanCount + " item ke warehouse?")
                .setCancelable(false)
                .setPositiveButton("Save", (d, w) -> saveToRoomThenSubmit())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void saveToRoomThenSubmit() {
        showLoading();
        String type = switchRfid.isChecked() ? "RFID" : "QR";

        new Thread(() -> {
            // Pastikan semua item tersimpan di Room dengan locationId & scannerType terbaru
            List<StockInScanEntity> existing = db.appDao().getAllStockInScans();
            for (ItemModels.ItemModel item : scannedItemsList) {
                boolean alreadyInRoom = false;
                for (StockInScanEntity e : existing) {
                    if (e.epcTag.equalsIgnoreCase(item.getEpcTag())) { alreadyInRoom = true; break; }
                }
                if (!alreadyInRoom) {
                    db.appDao().insertStockInScan(buildEntity(
                            item.getEpcTag(), item.getItemId(), item.getItemName(),
                            !item.getItemName().equals("Loading...") && !item.getItemName().equals("Pending...")
                    ));
                }
            }
            // Update locationId & scannerType semua pending row
            db.appDao().updateStockInLocation(selectedLocationId);
            db.appDao().updateStockInScannerType(type);

            handler.post(() -> {
                hideLoading();
                if (isNetworkConnected()) {
                    // Ada internet → kirim ke API
                    hitApiStockIn(type);
                } else {
                    // Tidak ada internet → data sudah di Room, selesai
                    showWarning("Tidak ada jaringan – data tersimpan lokal");
                    resultScan.requestFocus();
                }
            });
        }).start();
    }

    private void hitApiStockIn(String scannerType) {
        showLoading();
        String token = "Bearer " + new PrefManager(this).getToken();

        List<String> codes = new ArrayList<>();
        for (ItemModels.ItemModel item : scannedItemsList) codes.add(item.getEpcTag());

        StockInRequest request = new StockInRequest(scannerType, codes, selectedLocationId);
        ApiClient.getClient(this).create(ApiService.class)
                .stockIn(token, request)
                .enqueue(new Callback<GeneralResponse>() {
                    @Override
                    public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                        hideLoading();
                        if (response.isSuccessful()) {
                            // Berhasil → hapus dari Room
                            new Thread(() -> db.appDao().clearAllStockInScans()).start();
                            showSuccess("Success: " + response.body().getMessage());
                            playScanFeedback(0);
                            clearAllData();
                        } else {
                            // Gagal → data tetap di Room
                            handleApiErrorFriendly(response);
                            playScanFeedback(2);
                        }
                        resultScan.requestFocus();
                    }

                    @Override
                    public void onFailure(Call<GeneralResponse> call, Throwable t) {
                        hideLoading();
                        handleFailure(t);
                        playScanFeedback(2);
                        showWarning("Gagal kirim – data tetap tersimpan lokal");
                    }
                });
    }

    // ─── UI Helpers ───────────────────────────────────────────────

    private void addItemToList(ItemModels.ItemModel item) {
        scannedItemsList.add(0, item);
        if (adapter != null) adapter.setLastScannedPosition(0);
        if (!isListProductTab) {
            buildSumProductList();
            if (sumAdapter != null) sumAdapter.updateData(sumProductList);
        } else {
            adapter.notifyItemInserted(0);
        }
        rvTags.scrollToPosition(0);
        totalScanCount++;
        updateScanCount();
    }

    private void updateItemInList(String epc, String itemId, String itemName) {
        for (int i = 0; i < scannedItemsList.size(); i++) {
            if (scannedItemsList.get(i).getEpcTag().equalsIgnoreCase(epc)) {
                scannedItemsList.get(i).setItemId(itemId);
                scannedItemsList.get(i).setItemName(itemName);
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void removeItemFromList(String epc) {
        for (int i = 0; i < scannedItemsList.size(); i++) {
            if (scannedItemsList.get(i).getEpcTag().equalsIgnoreCase(epc)) {
                scannedItemsList.remove(i);
                totalScanCount--;
                if (isListProductTab) {
                    adapter.notifyItemRemoved(i);
                    adapter.notifyItemRangeChanged(i, scannedItemsList.size());
                } else {
                    buildSumProductList();
                    if (sumAdapter != null) sumAdapter.updateData(sumProductList);
                }
                updateScanCount();
                break;
            }
        }
    }

    private void clearAllData() {
        scannedItemsList.clear();
        sumProductList.clear();
        if (isListProductTab) adapter.notifyDataSetChanged();
        else if (sumAdapter != null) sumAdapter.updateData(sumProductList);
        totalScanCount = 0;
        updateScanCount();
    }

    private void updateScanCount() {
        tvScanned.setText("Scanned: " + totalScanCount);
    }

    private void buildSumProductList() {
        Map<String, ItemModels.SumProductModel> map = new LinkedHashMap<>();
        for (ItemModels.ItemModel item : scannedItemsList) {
            if (map.containsKey(item.getItemId()))
                map.get(item.getItemId()).addCount(1);
            else
                map.put(item.getItemId(),
                        new ItemModels.SumProductModel(item.getItemId(), item.getItemName(), 1));
        }
        sumProductList = new ArrayList<>(map.values());
    }

    private void showDeleteItemDialog(ItemModels.ItemModel item, int position) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_regist);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ((TextView) dialog.findViewById(R.id.tvTitle))
                .setText("Remove " + item.getEpcTag() + " from list?");

        Button btnYes = dialog.findViewById(R.id.btnSave);
        btnYes.setText("Remove");
        btnYes.setBackgroundTintList(ColorStateList.valueOf(Color.RED));

        dialog.findViewById(R.id.btnNo).setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            new Thread(() -> db.appDao().deleteStockInScanByEpc(item.getEpcTag())).start();
            scannedItemsList.remove(position);
            totalScanCount--;
            if (isListProductTab) {
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, scannedItemsList.size());
            } else {
                buildSumProductList();
                if (sumAdapter != null) sumAdapter.updateData(sumProductList);
            }
            updateScanCount();
            showSuccess("Item removed");
            resultScan.requestFocus();
        });
        dialog.show();
    }

    // ─── Fetch Master Data ────────────────────────────────────────

    private void fetchMasterItems() {
        if (!isNetworkConnected()) return;
        String token = "Bearer " + new PrefManager(this).getToken();
        ApiClient.getClient(this).create(ApiService.class)
                .getAllItems(token)
                .enqueue(new Callback<List<ItemModels.ItemResponseDto>>() {
                    @Override
                    public void onResponse(Call<List<ItemModels.ItemResponseDto>> call,
                                           Response<List<ItemModels.ItemResponseDto>> response) {}
                    @Override public void onFailure(Call<List<ItemModels.ItemResponseDto>> call, Throwable t) {}
                });
    }

    private void fetchLocations() {
        if (!isNetworkConnected()) return;
        String token = "Bearer " + new PrefManager(this).getToken();
        ApiClient.getClient(this).create(ApiService.class)
                .getLocations(token)
                .enqueue(new Callback<List<LocationModels.LocationModel>>() {
                    @Override
                    public void onResponse(Call<List<LocationModels.LocationModel>> call,
                                           Response<List<LocationModels.LocationModel>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            masterLocationList = response.body();
                            locationList.clear();
                            for (LocationModels.LocationModel loc : masterLocationList)
                                locationList.add(loc.getName());
                            // Restore label lokasi kalau ada selectedLocationId dari Room
                            if (!selectedLocationId.isEmpty() && selectedLocation.isEmpty()) {
                                for (LocationModels.LocationModel loc : masterLocationList) {
                                    if (loc.getId().equals(selectedLocationId)) {
                                        selectedLocation = loc.getName();
                                        runOnUiThread(() -> etLocation.setText(selectedLocation));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    @Override public void onFailure(Call<List<LocationModels.LocationModel>> call, Throwable t) {}
                });
    }

    // ─── Location Dropdown ────────────────────────────────────────

    private void setupLocationDropdown() {
        View.OnClickListener listener = v -> showDropdownPopup(cardLocation, locationList);
        etLocation.setOnClickListener(listener);
        ivLocationArrow.setOnClickListener(listener);
        cardLocation.setOnClickListener(listener);
    }

    private void showDropdownPopup(View anchor, List<String> items) {
        if (items.isEmpty()) { showWarning("Loading location..."); fetchLocations(); return; }
        if (activePopup != null && activePopup.isShowing()) activePopup.dismiss();

        View popupView = getLayoutInflater().inflate(R.layout.dropdown_popup, null);
        RecyclerView rv = popupView.findViewById(R.id.rvDropdown);
        rv.setLayoutManager(new LinearLayoutManager(this));

        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View v = getLayoutInflater().inflate(R.layout.item_dropdown, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                ((TextView) holder.itemView.findViewById(R.id.tvDropdownItem))
                        .setText(items.get(position));
                holder.itemView.setOnClickListener(v -> {
                    selectedLocation   = masterLocationList.get(position).getName();
                    selectedLocationId = masterLocationList.get(position).getId();
                    etLocation.setText(selectedLocation);
                    // Update locationId di Room untuk semua scan yang sudah ada
                    new Thread(() -> db.appDao().updateStockInLocation(selectedLocationId)).start();
                    if (activePopup != null) activePopup.dismiss();
                });
            }
            @Override public int getItemCount() { return items.size(); }
        });

        int maxHeight = (int)(56 * getResources().getDisplayMetrics().density) * 4;
        PopupWindow popup = new PopupWindow(
                popupView, anchor.getWidth(), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(16f);
        popup.setOutsideTouchable(true);
        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(anchor.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        popup.setHeight(Math.min(popupView.getMeasuredHeight(), maxHeight));
        popup.showAsDropDown(anchor, 0, 6);
        activePopup = popup;
    }

    // ─── Tab Buttons ──────────────────────────────────────────────

    private void setupTabButtons() {
        setTabActive(true);
        btnListProduct.setOnClickListener(v -> {
            if (!isListProductTab) {
                isListProductTab = true;
                setTabActive(true);
                adapter = new ItemAdapter(scannedItemsList);
                adapter.setOnItemClickListener(item -> {
                    int pos = scannedItemsList.indexOf(item);
                    if (pos != -1) showDeleteItemDialog(item, pos);
                });
                rvTags.setAdapter(adapter);
            }
        });
        btnSumProduct.setOnClickListener(v -> {
            if (isListProductTab) {
                isListProductTab = false;
                setTabActive(false);
                buildSumProductList();
                sumAdapter = new SumProductAdapter(sumProductList);
                rvTags.setAdapter(sumAdapter);
            }
        });
    }

    private void setTabActive(boolean listActive) {
        btnListProduct.setBackgroundTintList(ColorStateList.valueOf(
                getColor(listActive ? R.color.blue_theme : R.color.white)));
        btnListProduct.setTextColor(getColor(listActive ? R.color.white : R.color.blue_theme));
        btnSumProduct.setBackgroundTintList(ColorStateList.valueOf(
                getColor(listActive ? R.color.white : R.color.blue_theme)));
        btnSumProduct.setTextColor(getColor(listActive ? R.color.blue_theme : R.color.white));
    }

    // ─── Error Handling ───────────────────────────────────────────

    private void handleApiErrorFriendly(Response<?> response) {
        if (response.code() == 401) { handleApiError(response); return; }
        hideLoading();
        String raw = ErrorParser.getMessage(response);
        showError(humanizeError(raw, response.code()));
    }

    private String humanizeError(String rawMsg, int code) {
        if (code == 403) return "You don't have access for this action";
        if (code >= 500) return "Server error, please try again later";
        if (rawMsg != null && !rawMsg.isEmpty()
                && !rawMsg.toLowerCase().contains("exception")
                && !rawMsg.toLowerCase().contains("null")) return rawMsg;
        return "Something went wrong, please try again";
    }

    // ─── RFID / Barcode ───────────────────────────────────────────

    @Override
    public void onRFIDDataReceived(CommScanner scanner, RFIDDataReceivedEvent event) {
        if (!switchRfid.isChecked()) return;
        for (RFIDData data : event.getRFIDData()) {
            String epc = bytesToHex(data.getUII());
            handler.post(() -> enqueueScan(epc));
        }
    }

    @Override
    public void onBarcodeDataReceived(CommScanner scanner, BarcodeDataReceivedEvent event) {
        if (switchRfid.isChecked()) return;
        if (!event.getBarcodeData().isEmpty()) {
            String barcode = new String(event.getBarcodeData().get(0).getData());
            handler.post(() -> enqueueScan(barcode));
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }
}