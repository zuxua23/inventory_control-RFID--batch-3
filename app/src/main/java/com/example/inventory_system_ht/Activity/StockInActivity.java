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
import com.example.inventory_system_ht.Helper.ErrorParser;
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.Models.GeneralResponse;
import com.example.inventory_system_ht.Models.ItemModels;
import com.example.inventory_system_ht.Models.LocationModels;
import com.example.inventory_system_ht.Models.StockInRequest;
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
    private ImageView btnBack;
    private Button btnClear, btnSave, btnListProduct, btnSumProduct;
    private Switch switchRfid;
    private EditText resultScan, etLocation;
    private TextView tvScanned;
    private RecyclerView rvTags;
    private CardView btnPowerDropdown, cardLocation;
    private ImageView ivLocationArrow;
    private TextView tvPowerLevel;

    private ItemAdapter adapter;
    private SumProductAdapter sumAdapter;

    private List<ItemModels.ItemModel> scannedItemsList;
    private List<ItemModels.SumProductModel> sumProductList = new ArrayList<>();
    private List<ItemModels.ItemResponseDto> masterItemList = new ArrayList<>();

    private ToneGenerator toneGen;
    private Handler handler = new Handler(Looper.getMainLooper());
    private CommScanner mCommScanner;
    private boolean isProcessing = false;
    private int totalScanCount = 0;
    private boolean isListProductTab = true;
    private String selectedLocation = "";
    private String selectedPower = "20 dBm";
    private PopupWindow activePopup = null;
    private boolean isSelectingLocation = false;
    private List<LocationModels.LocationModel> masterLocationList = new ArrayList<>();
    private String selectedLocationId = "";
    private List<String> locationList = new ArrayList<>();
    private final List<String> powerList = new ArrayList<>(Arrays.asList(
            "10 dBm", "15 dBm", "20 dBm", "25 dBm", "27 dBm"
    ));

    @Override
    protected CommScanner getScannerInstance() {
        return mCommScanner;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_in);

        try { toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100); } catch (Exception ignored) {}

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

        scannedItemsList = new ArrayList<>();
        adapter = new ItemAdapter(scannedItemsList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {
            if (!isListProductTab) return;
            int position = scannedItemsList.indexOf(item);
            if (position != -1) {
                showDeleteItemDialog(item, position);
            }
        });

        setupScanner();
        setupTabButtons();
        setupLocationDropdown();
        fetchMasterItems();
        fetchLocations();

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

        btnBack.setOnClickListener(v -> finish());
        btnClear.setOnClickListener(v -> clearAllData());
        btnSave.setOnClickListener(v -> {
            if (scannedItemsList.isEmpty()) {
                showSagaFeedback("Belum ada barang yang di-scan", false);
                return;
            }
            showBulkConfirmDialog();
        });

        switchRfid.setFocusable(false);
        switchRfid.setFocusableInTouchMode(false);

        setupPowerDropdown(btnPowerDropdown, switchRfid, tvPowerLevel);

        btnPowerDropdown.setOnClickListener(v ->
                showDropdownPopup(btnPowerDropdown, powerList, false));
    }

    private void setupLocationDropdown() {
        View.OnClickListener showDropdownListener = v -> {
            showDropdownPopup(cardLocation, locationList, true);
        };

        etLocation.setOnClickListener(showDropdownListener);
        ivLocationArrow.setOnClickListener(showDropdownListener);
        cardLocation.setOnClickListener(showDropdownListener);
    }

    private List<String> filterList(List<String> source, String query) {
        if (query.isEmpty()) return new ArrayList<>(source);
        List<String> result = new ArrayList<>();
        for (String item : source) {
            if (item.toLowerCase().contains(query.toLowerCase())) result.add(item);
        }
        return result;
    }

    private void showDropdownPopup(View anchor, List<String> items, boolean isLocation) {
        if (items.isEmpty()) {
            if (isLocation) {
                showSagaFeedback("Sedang memuat daftar lokasi...", false);
                fetchLocations();
            }
            return;
        }

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
                        selectedLocation = masterLocationList.get(position).getName();
                        selectedLocationId = masterLocationList.get(position).getId();

                        isSelectingLocation = true;
                        etLocation.setText(selectedLocation);
                        etLocation.setSelection(selectedLocation.length());
                        isSelectingLocation = false;
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

        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(anchor.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int measuredH = popupView.getMeasuredHeight();
        popup.setHeight(Math.min(measuredH, maxHeight));

        popup.showAsDropDown(anchor, 0, 6);
        activePopup = popup;
    }

    private void setupTabButtons() {
        setTabActive(true);

        btnListProduct.setOnClickListener(v -> {
            if (!isListProductTab) {
                isListProductTab = true;
                setTabActive(true);
                adapter = new ItemAdapter(scannedItemsList);
                adapter.setOnItemClickListener(item -> {
                    if (!isListProductTab) return;
                    int position = scannedItemsList.indexOf(item);
                    if (position != -1) {
                        showDeleteItemDialog(item, position);
                    }
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

    private void fetchMasterItems() {
        if (!isNetworkConnected()) return;

        PrefManager prefManager = new PrefManager(this);

        if (prefManager.isSessionValid()) {
            String realToken = "Bearer " + prefManager.getToken();

            ApiService api = ApiClient.getClient(this).create(ApiService.class);
            api.getAllItems(realToken).enqueue(new retrofit2.Callback<List<ItemModels.ItemResponseDto>>() {
                @Override
                public void onResponse(Call<List<ItemModels.ItemResponseDto>> call,
                                       retrofit2.Response<List<ItemModels.ItemResponseDto>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        masterItemList = response.body();
                    } else {
                        handleApiErrorFriendly(response);
                    }
                }

                @Override
                public void onFailure(Call<List<ItemModels.ItemResponseDto>> call, Throwable t) {
                    handleFailure(t);
                }
            });

        } else {
            prefManager.clearSession();
            showSagaFeedback("Sesi habis, silakan login ulang", false);
        }
    }

    private void processScannedData(String scannedData) {
        for (ItemModels.ItemModel t : scannedItemsList) {
            if (t.getEpcTag().equals(scannedData)) {
                playScanFeedback(1);
                showSagaFeedback("Barang sudah ada di daftar", false);
                return;
            }
        }

        if (!isNetworkConnected()) {
            playScanFeedback(2);
            showSagaFeedback("Tidak ada koneksi internet", false);
            return;
        }

        PrefManager pref = new PrefManager(this);
        String token = "Bearer " + pref.getToken();
        ApiService api = ApiClient.getClient(this).create(ApiService.class);

        api.getTagByCode(token, scannedData).enqueue(new Callback<TagModels.TagResponseDto>() {
            @Override
            public void onResponse(Call<TagModels.TagResponseDto> call, Response<TagModels.TagResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TagModels.TagResponseDto tag = response.body();
                    addItemToList(new ItemModels.ItemModel(
                            scannedData,
                            tag.getItemId(),
                            tag.getItemName(),
                            1
                    ));
                    playScanFeedback(0);
                } else {
                    playScanFeedback(2);
                    showSagaFeedback("Barang tidak terdaftar: " + scannedData, false);
                }
            }

            @Override
            public void onFailure(Call<TagModels.TagResponseDto> call, Throwable t) {
                handleFailure(t);
                playScanFeedback(2);
            }
        });
    }

    private void addItemToList(ItemModels.ItemModel item) {
        runOnUiThread(() -> {
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
        });
    }

    private void showDeleteItemDialog(ItemModels.ItemModel item, int position) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_regist);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvTitle);
        tvTitle.setText("Hapus " + item.getEpcTag() + " dari daftar?");

        Button btnYes = dialog.findViewById(R.id.btnSave);
        btnYes.setText("Hapus");
        btnYes.setBackgroundTintList(ColorStateList.valueOf(Color.RED));

        dialog.findViewById(R.id.btnNo).setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
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
            showSagaFeedback("Barang berhasil dihapus dari daftar", true);
            resultScan.requestFocus();
        });
        dialog.show();
    }

    private void hitApiStockIn(List<String> codes, String scannerType) {
        if (selectedLocationId.isEmpty()) {
            showSagaFeedback("Pilih lokasi terlebih dahulu", false);
            return;
        }

        showLoading();
        PrefManager pref = new PrefManager(this);
        String token = "Bearer " + pref.getToken();

        StockInRequest request = new StockInRequest(scannerType, codes, selectedLocationId);

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.stockIn(token, request).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    showSagaFeedback("Berhasil: " + response.body().getMessage(), true);
                    playScanFeedback(0);
                    clearAllData();
                } else {
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
            }
        });
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
        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi")
                .setMessage("Simpan " + totalScanCount + " barang ke gudang?")
                .setCancelable(false)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    List<String> codesToSubmit = new ArrayList<>();
                    for (ItemModels.ItemModel item : scannedItemsList) {
                        codesToSubmit.add(item.getEpcTag());
                    }
                    String currentType = switchRfid.isChecked() ? "RFID" : "QR";
                    hitApiStockIn(codesToSubmit, currentType);
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void fetchLocations() {
        if (!isNetworkConnected()) {
            showSagaFeedback("Tidak ada koneksi internet", false);
            return;
        }

        PrefManager prefManager = new PrefManager(this);
        if (!prefManager.isSessionValid()) {
            showSagaFeedback("Sesi habis, silakan login ulang", false);
            return;
        }

        String realToken = "Bearer " + prefManager.getToken();
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getLocations(realToken).enqueue(new retrofit2.Callback<List<LocationModels.LocationModel>>() {
            @Override
            public void onResponse(Call<List<LocationModels.LocationModel>> call, Response<List<LocationModels.LocationModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    masterLocationList = response.body();
                    locationList.clear();
                    for (LocationModels.LocationModel loc : masterLocationList) {
                        locationList.add(loc.getName());
                    }
                } else {
                    handleApiErrorFriendly(response);
                }
            }

            @Override
            public void onFailure(Call<List<LocationModels.LocationModel>> call, Throwable t) {
                handleFailure(t);
            }
        });
    }

    private void handleApiErrorFriendly(Response<?> response) {
        if (response.code() == 401) {
            handleApiError(response);
            return;
        }
        hideLoading();
        String rawMsg = ErrorParser.getMessage(response);
        showSagaFeedback(humanizeError(rawMsg, response.code()), false);
    }

    private String humanizeError(String rawMsg, int statusCode) {
        if (rawMsg == null) rawMsg = "";
        String lower = rawMsg.toLowerCase();

        if (statusCode == 403) return "Anda tidak punya akses untuk ini";
        if (statusCode >= 500) return "Server sedang bermasalah, coba lagi nanti";

        if (lower.contains("lokasi") && lower.contains("tidak ditemukan"))
            return "Lokasi tidak tersedia, hubungi admin";
        if (lower.contains("tag tidak ditemukan"))
            return "Barang tidak terdaftar di sistem";
        if (lower.contains("sudah berstatus in_stock") || lower.contains("sudah berstatus in stock"))
            return "Barang ini sudah ada di gudang";
        if (lower.contains("statusnya") && lower.contains("tidak bisa di stock in"))
            return "Barang ini belum siap dimasukkan ke gudang";
        if (lower.contains("tag tidak boleh kosong"))
            return "Belum ada barang yang di-scan";

        if (!rawMsg.isEmpty()
                && !rawMsg.toLowerCase().contains("exception")
                && !rawMsg.toLowerCase().contains("null")) {
            return rawMsg;
        }
        return "Terjadi kesalahan, silakan coba lagi";
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
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupScanner();
        if (getHTBatteryLevel() <= 15) {
            showSagaFeedback("Baterai HT tinggal " + getHTBatteryLevel() + "%, segera isi ulang!", false);
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