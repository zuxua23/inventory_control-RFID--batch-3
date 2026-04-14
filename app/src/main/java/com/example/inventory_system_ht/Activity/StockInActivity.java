package com.example.inventory_system_ht.Activity;

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
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.Models.GeneralResponse;
import com.example.inventory_system_ht.Models.ItemModels;
import com.example.inventory_system_ht.Models.StockInRequest;
import com.example.inventory_system_ht.Models.TagModels;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;

public class StockInActivity extends BaseScannerActivity implements BarcodeDataDelegate, RFIDDataDelegate {

    // === Views ===
    private ImageView btnBack;
    private Button btnClear, btnSave, btnListProduct, btnSumProduct;
    private Switch switchRfid;
    private EditText resultScan, etLocation;
    private TextView tvScanned;
    private RecyclerView rvTags;
    private CardView btnPowerDropdown, cardLocation;
    private ImageView ivLocationArrow;
    private TextView tvPowerLevel;

    // === Adapters ===
    private ItemAdapter adapter;
    private SumProductAdapter sumAdapter;

    // === Data ===
    private List<ItemModels.ItemModel> scannedItemsList;
    private List<ItemModels.SumProductModel> sumProductList = new ArrayList<>();
    private List<ItemModels.ItemResponseDto> masterItemList = new ArrayList<>();

    // === State ===
    private ToneGenerator toneGen;
    private Handler handler = new Handler(Looper.getMainLooper());
    private CommScanner mCommScanner;
    private boolean isProcessing = false;
    private int totalScanCount = 0;
    private boolean isListProductTab = true;
    private String selectedLocation = "";
    private String selectedPower = "20 dBm";
    private PopupWindow activePopup = null;

    // === Dummy data (nanti ganti dari API) ===
    private final List<String> locationList = new ArrayList<>(Arrays.asList(
            "Gudang A", "Gudang B", "Gudang C", "Rak 1",
            "Rak 2", "Rak 3", "Zona 1", "Zona 2", "Loading Dock", "Area Produksi"
    ));
    private final List<String> powerList = new ArrayList<>(Arrays.asList(
            "10 dBm", "15 dBm", "20 dBm", "25 dBm", "27 dBm"
    ));

    // Implementasi abstract method wajib dari BaseScannerActivity
    @Override
    protected CommScanner getScannerInstance() {
        return mCommScanner;
    }

    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_in);

        try { toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100); } catch (Exception ignored) {}

        // === Init views ===
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

        // === Init state ===
        switchRfid.setChecked(false);
        btnPowerDropdown.setVisibility(View.GONE); // Power hidden default

        scannedItemsList = new ArrayList<>();
        adapter = new ItemAdapter(scannedItemsList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        // === Setup ===
        setupScanner();
        setupTabButtons();
        setupLocationDropdown();
        fetchMasterItems();

        // === Barcode scan listener ===
        resultScan.setShowSoftInputOnFocus(false);
        resultScan.postDelayed(() -> resultScan.requestFocus(), 100);

        resultScan.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String data = s.toString().trim();
                if (data.length() >= 8 && !isProcessing && !switchRfid.isChecked()) {
                    isProcessing = true;
                    processScannedData(data);
                    resultScan.setText("");
                    isProcessing = false;
                }
            }
        });

        resultScan.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER) return true;
            return false;
        });

        // === Button listeners ===
        btnBack.setOnClickListener(v -> finish());

        btnClear.setOnClickListener(v -> clearAllData());

        btnSave.setOnClickListener(v -> {
            if (scannedItemsList.isEmpty()) {
                showSagaFeedback("No items have been scanned yet!", false);
                return;
            }
            showBulkConfirmDialog();
        });

        // === RFID Switch listener ===
        switchRfid.setFocusable(false);
        switchRfid.setFocusableInTouchMode(false);

        // Panggil setupPowerDropdown tanpa parameter scanner
        setupPowerDropdown(btnPowerDropdown, switchRfid, tvPowerLevel);

        // Gabungkan Custom Listener
        switchRfid.setOnCheckedChangeListener((buttonView, isChecked) -> {
            CommScanner currentScanner = getScannerInstance();

            if (isChecked) {
                boolean isConnected = (currentScanner != null && currentScanner.getRFIDScanner() != null);
                if (!isConnected) {
                    showSagaFeedback("HT not Connected to Reader RFID", false);
                    switchRfid.setChecked(false);
                    return;
                }
                btnPowerDropdown.setVisibility(View.VISIBLE);
            } else {
                btnPowerDropdown.setVisibility(View.GONE);
            }
            showSagaFeedback(isChecked ? "Mode RFID: ON" : "Mode RFID: OFF", true);
            resultScan.requestFocus();
        });

        // Power dropdown click
        btnPowerDropdown.setOnClickListener(v ->
                showDropdownPopup(btnPowerDropdown, powerList, false));
    }

    // =========================================================
    // === SETUP LOCATION DROPDOWN ===
    // =========================================================

    private void setupLocationDropdown() {
        // Klik panah = tampil semua / filter sesuai teks
        ivLocationArrow.setOnClickListener(v -> {
            String query = etLocation.getText().toString().trim();
            List<String> filtered = filterList(locationList, query);
            showDropdownPopup(cardLocation, filtered, true);
        });

        // Typing realtime filter
        etLocation.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<String> filtered = filterList(locationList, s.toString().trim());
                if (!filtered.isEmpty()) {
                    showDropdownPopup(cardLocation, filtered, true);
                } else {
                    if (activePopup != null && activePopup.isShowing()) activePopup.dismiss();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Done / Enter — simpan pilihan dari teks
        etLocation.setOnEditorActionListener((v, actionId, event) -> {
            selectedLocation = etLocation.getText().toString().trim();
            if (activePopup != null && activePopup.isShowing()) activePopup.dismiss();
            return false;
        });
    }

    private List<String> filterList(List<String> source, String query) {
        if (query.isEmpty()) return new ArrayList<>(source);
        List<String> result = new ArrayList<>();
        for (String item : source) {
            if (item.toLowerCase().contains(query.toLowerCase())) result.add(item);
        }
        return result;
    }

    // =========================================================
    // === DROPDOWN POPUP ===
    // =========================================================

    private void showDropdownPopup(View anchor, List<String> items, boolean isLocation) {
        if (items.isEmpty()) return;

        // Dismiss popup sebelumnya
        if (activePopup != null && activePopup.isShowing()) activePopup.dismiss();

        View popupView = getLayoutInflater().inflate(R.layout.dropdown_popup, null);
        RecyclerView rv = popupView.findViewById(R.id.rvDropdown);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setNestedScrollingEnabled(true);

        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View v = getLayoutInflater().inflate(R.layout.item_dropdown, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                TextView tv = holder.itemView.findViewById(R.id.tvDropdownItem);
                tv.setText(items.get(position));
                holder.itemView.setOnClickListener(v -> {
                    if (isLocation) {
                        selectedLocation = items.get(position);
                        etLocation.setText(selectedLocation);
                        etLocation.setSelection(selectedLocation.length());
                    } else {
                        selectedPower = items.get(position);
                        tvPowerLevel.setText(selectedPower);
                    }
                    if (activePopup != null) activePopup.dismiss();
                });
            }

            @Override
            public int getItemCount() { return items.size(); }
        });

        // Max tinggi 4 item (56dp per item)
        int itemHeightPx = (int) (56 * getResources().getDisplayMetrics().density);
        int maxHeight    = itemHeightPx * 4;

        PopupWindow popup = new PopupWindow(
                popupView,
                anchor.getWidth(),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(16f);
        popup.setOutsideTouchable(true);

        // Ukur tinggi sesungguhnya, batasi ke maxHeight
        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(anchor.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int measuredH = popupView.getMeasuredHeight();
        popup.setHeight(Math.min(measuredH, maxHeight));

        popup.showAsDropDown(anchor, 0, 6);
        activePopup = popup;
    }

    // =========================================================
    // === TAB LIST PRODUCT / SUM PRODUCT ===
    // =========================================================

    private void setupTabButtons() {
        // Default: List Product aktif
        setTabActive(true);

        btnListProduct.setOnClickListener(v -> {
            if (!isListProductTab) {
                isListProductTab = true;
                setTabActive(true);
                adapter = new ItemAdapter(scannedItemsList);
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

    private void setTabActive(boolean listProductActive) {
        if (listProductActive) {
            btnListProduct.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.blue_theme)));
            btnListProduct.setTextColor(getColor(R.color.white));
            btnSumProduct.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.white)));
            btnSumProduct.setTextColor(getColor(R.color.blue_theme));
        } else {
            btnSumProduct.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.blue_theme)));
            btnSumProduct.setTextColor(getColor(R.color.white));
            btnListProduct.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.white)));
            btnListProduct.setTextColor(getColor(R.color.blue_theme));
        }
    }

    private void buildSumProductList() {
        Map<String, ItemModels.SumProductModel> map = new LinkedHashMap<>();
        for (ItemModels.ItemModel item : scannedItemsList) {
            if (map.containsKey(item.getItemId())) {
                map.get(item.getItemId()).addCount(1);
            } else {
                map.put(item.getItemId(),
                        new ItemModels.SumProductModel(item.getItemId(), item.getItemName(), 1));
            }
        }
        sumProductList = new ArrayList<>(map.values());
    }

    // =========================================================
    // === FETCH MASTER ITEMS ===
    // =========================================================

    private void fetchMasterItems() {
        if (!isNetworkConnected()) return;
        showLoading();
        PrefManager pref = new PrefManager(this);
        String token = "Bearer " + pref.getToken();

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getAllItems(token).enqueue(new retrofit2.Callback<List<ItemModels.ItemResponseDto>>() {
            @Override
            public void onResponse(Call<List<ItemModels.ItemResponseDto>> call,
                                   retrofit2.Response<List<ItemModels.ItemResponseDto>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    masterItemList = response.body();
                } else {
                    handleApiError(response.code());
                }
            }
            @Override
            public void onFailure(Call<List<ItemModels.ItemResponseDto>> call, Throwable t) {
                hideLoading();
                handleFailure(t);
            }
        });
    }

    // =========================================================
    // === PROCESS SCAN ===
    // =========================================================

    private void processScannedData(String scannedData) {
        // Cek duplikat
        for (ItemModels.ItemModel t : scannedItemsList) {
            if (t.getEpcTag().equals(scannedData) || t.getItemId().equals(scannedData)) {
                playScanFeedback(1);
                showSagaFeedback("The item is already on the list!", false);
                return;
            }
        }

        // Offline fallback
        if (!isNetworkConnected()) {
            playScanFeedback(0);
            showSagaFeedback("Offline! Item added locally.", false);
            ItemModels.ItemModel offlineItem =
                    new ItemModels.ItemModel(scannedData, scannedData, "Offline Scanned Item", 1);
            addItemToList(offlineItem);
            return;
        }

        PrefManager pref = new PrefManager(this);
        String token = "Bearer " + pref.getToken();
        showLoading();

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getTagInfo(token, scannedData).enqueue(new retrofit2.Callback<TagModels.TagInfoDto>() {
            @Override
            public void onResponse(Call<TagModels.TagInfoDto> call,
                                   retrofit2.Response<TagModels.TagInfoDto> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    TagModels.TagInfoDto info = response.body();

                    if (!info.getStatus().equals("STANDBY") && !info.getStatus().equals("PRINTED")) {
                        showSagaFeedback("Tag " + info.getTagId() + " status " + info.getStatus() + "!", false);
                        playScanFeedback(2);
                        return;
                    }

                    playScanFeedback(0);
                    addItemToList(new ItemModels.ItemModel(
                            scannedData, info.getTagId(), info.getItemName(), 1));
                } else {
                    handleApiError(response.code());
                    playScanFeedback(2);
                }
            }

            @Override
            public void onFailure(Call<TagModels.TagInfoDto> call, Throwable t) {
                hideLoading();
                handleFailure(t);
                playScanFeedback(2);
            }
        });
    }

    private void addItemToList(ItemModels.ItemModel item) {
        scannedItemsList.add(0, item);
        if (adapter != null) adapter.setLastScannedPosition(0);
        // Kalau lagi di tab sum, refresh sum juga
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


    private void clearAllData() {
        scannedItemsList.clear();
        sumProductList.clear();
        if (isListProductTab) {
            adapter.notifyDataSetChanged();
        } else {
            if (sumAdapter != null) sumAdapter.updateData(sumProductList);
        }
        totalScanCount = 0;
        updateScanCount();
    }

    private void updateScanCount() {
        tvScanned.setText("Scanned: " + totalScanCount);
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
            for (ItemModels.ItemModel item : scannedItemsList) codesToSubmit.add(item.getEpcTag());

            String currentType = switchRfid.isChecked() ? "RFID" : "QR";
            hitApiStockIn(codesToSubmit, currentType);
        });

        dialog.show();
    }

    private void hitApiStockIn(List<String> codes, String scannerType) {
        if (!isNetworkConnected()) {
            showSagaFeedback("Connection Error! Please find a signal first, for Stock In..", false);
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
            public void onResponse(Call<GeneralResponse> call,
                                   retrofit2.Response<GeneralResponse> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    showSagaFeedback("Success: " + response.body().getMessage()
                            + " (" + codes.size() + " Physique)", true);
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

    // =========================================================
    // === SCANNER CALLBACKS ===
    // =========================================================

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
            } catch (Exception ignored) {}
        }
    }

    // =========================================================
    // === LIFECYCLE ===
    // =========================================================

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
        if (activePopup != null && activePopup.isShowing()) activePopup.dismiss();
        if (mCommScanner != null) {
            try {
                if (mCommScanner.getRFIDScanner() != null)
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