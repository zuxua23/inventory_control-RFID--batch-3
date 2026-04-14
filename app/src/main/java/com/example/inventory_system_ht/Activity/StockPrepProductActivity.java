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

import com.densowave.scannersdk.Barcode.BarcodeData;
import com.densowave.scannersdk.Barcode.BarcodeDataReceivedEvent;
import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.BarcodeDataDelegate;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.densowave.scannersdk.RFID.RFIDData;
import com.densowave.scannersdk.RFID.RFIDDataReceivedEvent;
import com.example.inventory_system_ht.Adapter.SumProductAdapter;
import com.example.inventory_system_ht.Adapter.TagAdapter;
import com.example.inventory_system_ht.Helper.ApiClient;
import com.example.inventory_system_ht.Helper.ApiService;
import com.example.inventory_system_ht.Helper.AppDao;
import com.example.inventory_system_ht.Helper.AppDatabase;
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.Models.GeneralResponse;
import com.example.inventory_system_ht.Models.ItemModels;
import com.example.inventory_system_ht.Models.StockPrepBulkRequest;
import com.example.inventory_system_ht.Models.TagModels;
import com.example.inventory_system_ht.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StockPrepProductActivity extends BaseScannerActivity implements BarcodeDataDelegate, RFIDDataDelegate {

    // === Views ===
    private EditText resultScan, etLocation;
    private TextView tvScanned, tvNoDo, tvDateDo, tvLocation, tvPowerLevel;
    private Switch switchRfid;
    private ImageView ivLocationArrow;
    private RecyclerView rvTags;
    private CardView btnLocationDropdown, btnPowerDropdown;
    private Button btnListProduct, btnSumProduct;
    private TagAdapter adapter;
    private SumProductAdapter sumAdapter;
    private List<TagModels.TagModel> scannedList;
    private List<ItemModels.SumProductModel> sumProductList = new ArrayList<>();
    private int scanCount = 0;
    private boolean isListProductTab = true;
    private String selectedLocation = "";
    private String selectedPower = "20 dBm";
    private PopupWindow activePopup = null;
    private String currentDoId = "";
    private String currentDoNo = "";
    private CommScanner mCommScanner;
    private ToneGenerator toneGen;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ApiService api;
    private String token;
    private AppDao appDao;

    private final List<String> locationList = new ArrayList<>(Arrays.asList(
            "Gudang A", "Gudang B", "Gudang C", "Rak 1",
            "Rak 2", "Rak 3", "Zona 1", "Zona 2", "Loading Dock", "Area Produksi"
    ));
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
        setContentView(R.layout.activity_stock_prep_product);

        appDao = AppDatabase.getDatabase(this).appDao();
        PrefManager pref = new PrefManager(this);
        token = "Bearer " + pref.getToken();
        api = ApiClient.getClient(this).create(ApiService.class);

        initUI();
        setupScanner();
        setupTabButtons();

        if (getIntent() != null) {
            currentDoId = getIntent().getStringExtra("DO_ID");
            currentDoNo = getIntent().getStringExtra("NO_DO");
            tvNoDo.setText("No : " + currentDoNo);
            String rawDate = getIntent().getStringExtra("DATE_DO");
            tvDateDo.setText("Date : " + formatToEnglishDate(rawDate));
        }

        loadPendingScans();
        setupListeners();
    }

    private void initUI() {
        try { toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100); } catch (Exception ignored) {}

        tvScanned = findViewById(R.id.tvScanned);
        tvNoDo = findViewById(R.id.tvNoDo);
        tvDateDo = findViewById(R.id.tvDateDo);
        resultScan = findViewById(R.id.resultScan);
        switchRfid = findViewById(R.id.switchRfid);
        rvTags = findViewById(R.id.rvTags);
        btnLocationDropdown = findViewById(R.id.btnLocationDropdown);
        btnPowerDropdown = findViewById(R.id.btnPowerDropdown);
        tvLocation = findViewById(R.id.tvLocation);
        tvPowerLevel = findViewById(R.id.tvPowerLevel);
        btnListProduct = findViewById(R.id.btnListProduct);
        btnSumProduct = findViewById(R.id.btnSumProduct);
        etLocation       = findViewById(R.id.etLocation);
        ivLocationArrow  = findViewById(R.id.ivLocationArrow);

        btnPowerDropdown.setVisibility(View.GONE);

        scannedList = new ArrayList<>();
        adapter = new TagAdapter(scannedList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        resultScan.setShowSoftInputOnFocus(false);
        resultScan.postDelayed(() -> resultScan.requestFocus(), 100);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        resultScan.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String data = s.toString().trim();
                if (data.length() >= 7) {
                    processScan(data);
                    resultScan.setText("");
                }
            }
        });

        switchRfid.setFocusable(false);
        switchRfid.setFocusableInTouchMode(false);

        setupPowerDropdown(btnPowerDropdown, switchRfid, tvPowerLevel);

        btnLocationDropdown.setOnClickListener(v ->
                showDropdownPopup(btnLocationDropdown, locationList, true));

        findViewById(R.id.btnSave).setOnClickListener(v -> submitToBackend());

        findViewById(R.id.btnClear).setOnClickListener(v -> {
            scannedList.clear();
            sumProductList.clear();

            if (isListProductTab) {
                adapter.notifyDataSetChanged();
            } else {
                if (sumAdapter != null) sumAdapter.updateData(sumProductList);
            }
            scanCount = 0;
            tvScanned.setText("Scanned : 0");
            showSagaFeedback("The screen is cleared!", true);
        });
    }

    private void setupTabButtons() {
        setTabActive(true);

        btnListProduct.setOnClickListener(v -> {
            if (!isListProductTab) {
                isListProductTab = true;
                setTabActive(true);
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
        for (TagModels.TagModel item : scannedList) {
            if (map.containsKey(item.getItmId())) {
                map.get(item.getItmId()).addCount(1);
            } else {
                map.put(item.getItmId(),
                        new ItemModels.SumProductModel(item.getItmId(), item.getProductName(), 1));
            }
        }
        sumProductList = new ArrayList<>(map.values());
    }

    private void showDropdownPopup(View anchor, List<String> items, boolean isLocation) {
        if (items.isEmpty()) return;
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
                        tvLocation.setText(selectedLocation);
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

    private void processScan(String scannedData) {
        for (TagModels.TagModel item : scannedList) {
            if (item.getEpcTag().equalsIgnoreCase(scannedData)) {
                playScanFeedback(1);
                showSagaFeedback("The item has been scanned!", false);
                return;
            }
        }

        if (!isNetworkConnected()) {
            showSagaFeedback("Offline! Data will be saved locally.", false);
            saveToLocalDB(new TagModels.TagInfoDto(scannedData, scannedData, "Pending Sync", "Unknown", "STANDBY"));
            return;
        }

        showLoading();
        api.getTagInfo(token, scannedData).enqueue(new Callback<TagModels.TagInfoDto>() {
            @Override
            public void onResponse(Call<TagModels.TagInfoDto> call, Response<TagModels.TagInfoDto> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    TagModels.TagInfoDto info = response.body();
                    if (!info.getStatus().equals("IN_STOCK")) {
                        showSagaFeedback("Tag " + info.getStatus() + ", must be IN_STOCK!", false);
                        playScanFeedback(2);
                        return;
                    }
                    saveToLocalDB(info);
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

    private void saveToLocalDB(TagModels.TagInfoDto tag) {
        TagModels.TagModel newScan = new TagModels.TagModel(
                tag.getEpcTag(),
                tag.getTagId(),
                tag.getItemId(),
                tag.getItemName(),
                currentDoNo,
                0
        );

        new Thread(() -> {
            appDao.insertScannedTag(newScan);
            runOnUiThread(() -> {
                scannedList.add(0, newScan);
                if (adapter != null) adapter.setLastScannedPosition(0);

                if (!isListProductTab) {
                    buildSumProductList();
                    if (sumAdapter != null) sumAdapter.updateData(sumProductList);
                } else {
                    adapter.notifyItemInserted(0);
                }

                rvTags.scrollToPosition(0);
                scanCount++;
                tvScanned.setText("Scanned : " + scanCount);
                playScanFeedback(0);
            });
        }).start();
    }

    private void submitToBackend() {
        if (scannedList.isEmpty()) {
            showSagaFeedback("No items have been scanned yet!", false);
            return;
        }

        if (!isNetworkConnected()) {
            showSagaFeedback("Offline! Find connection to submit.", false);
            playScanFeedback(2);
            return;
        }

        showLoading();
        List<String> codes = new ArrayList<>();
        for (TagModels.TagModel t : scannedList) codes.add(t.getEpcTag());

        String scannerType = switchRfid.isChecked() ? "RFID" : "QR";
        StockPrepBulkRequest request = new StockPrepBulkRequest(currentDoId, codes, scannerType);
        showSagaFeedback("Sending data to the server...", true);

        api.submitStockPrep(token, request).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    new Thread(() -> {
                        for (TagModels.TagModel t : scannedList) appDao.markTagAsSynced(t.getEpcTag());
                        runOnUiThread(() -> {
                            showSagaFeedback("SUCCESS: Goods successfully prepared!", true);
                            playScanFeedback(0);
                            scannedList.clear();
                            sumProductList.clear();
                            adapter.notifyDataSetChanged();
                            if (sumAdapter != null) sumAdapter.updateData(sumProductList);
                            scanCount = 0;
                            tvScanned.setText("Scanned : 0");
                        });
                    }).start();
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

    private void loadPendingScans() {
        showLoading();
        new Thread(() -> {
            List<TagModels.TagModel> pending = appDao.getPendingTags();
            runOnUiThread(() -> {
                hideLoading();
                for (TagModels.TagModel t : pending) {
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
            } catch (Exception ignored) {}
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

    private String formatToEnglishDate(String rawDate) {
        try {
            SimpleDateFormat inputFormat  = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);
            java.util.Date date = inputFormat.parse(rawDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            return rawDate;
        }
    }
}