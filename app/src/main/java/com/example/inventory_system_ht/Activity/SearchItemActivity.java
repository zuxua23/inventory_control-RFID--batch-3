package com.example.inventory_system_ht.Activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
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

import com.example.inventory_system_ht.Adapter.SearchItemAdapter;
import com.example.inventory_system_ht.Helper.ApiClient;
import com.example.inventory_system_ht.Helper.ApiService;
import com.example.inventory_system_ht.Helper.AppDatabase;
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.Helper.RfidBulkHelper;
import com.example.inventory_system_ht.Helper.ScannerManager;
import com.example.inventory_system_ht.Models.TagModels;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchItemActivity extends BaseScannerActivity
        implements BarcodeDataDelegate, RFIDDataDelegate {

    // ─── Fields ───────────────────────────────────────────────────────────────
    private EditText etSearchItem;
    private RecyclerView rvTags;
    private SearchItemAdapter adapter;
    private List<TagModels.SearchItemListDto> allItemList;
    private List<TagModels.SearchItemListDto> filteredList;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ApiService api;
    private String token;
    private AppDatabase db;

    // ─── Abstract Override ────────────────────────────────────────────────────
    @Override
    protected CommScanner getScannerInstance() {
        return ScannerManager.getInstance().getScanner();
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_item);

        token = "Bearer " + new PrefManager(this).getToken();
        api = ApiClient.getClient(this).create(ApiService.class);
        db = AppDatabase.getDatabase(this);

        initViews();
        setupListeners();

        loadFromLocal();
        if (isNetworkConnected()) fetchData();
        else showWarning("Offline mode");

        etSearchItem.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CommScanner scanner = getScannerInstance();
        updateReaderBattery(findViewById(R.id.ivReaderBattery));
        if (scanner != null) RfidBulkHelper.openBarcode(scanner, this);

        int bat = getHTBatteryLevel();
        if (bat <= 15) {
            showWarning("Battery low: " + bat + "%");
            playScanFeedback(2);
        }

        if (etSearchItem != null)
            etSearchItem.postDelayed(() -> etSearchItem.requestFocus(), 200);
    }

    @Override
    protected void onPause() {
        super.onPause();
        RfidBulkHelper.closeBarcode(getScannerInstance());
    }

    // ─── Init ─────────────────────────────────────────────────────────────────
    private void initViews() {
        etSearchItem = findViewById(R.id.searchItem);
        rvTags = findViewById(R.id.rvTags);

        allItemList = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new SearchItemAdapter(filteredList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);
    }

    private void setupListeners() {
        etSearchItem.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
        });

        adapter.setOnItemClickListener(this::fetchAndShowDetail);

        findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            if (!isNetworkConnected()) { showWarning("No internet connection"); return; }
            fetchData();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    // ─── Data ─────────────────────────────────────────────────────────────────

    // Load item dari cache lokal (Room DB)
    private void loadFromLocal() {
        List<TagModels.SearchItemEntity> cached = db.appDao().getAllSearchItems();
        allItemList.clear();
        for (TagModels.SearchItemEntity e : cached) {
            TagModels.SearchItemListDto dto = new TagModels.SearchItemListDto();
            dto.setTagId(e.tagId);
            dto.setEpcTag(e.epcTag);
            dto.setItemName(e.itemName);
            dto.setLocation(e.location);
            allItemList.add(dto);
        }
        filteredList.clear();
        filteredList.addAll(allItemList);
        adapter.notifyDataSetChanged();
    }

    // Simpan data dari API ke Room DB (background thread)
    private void saveToLocal(List<TagModels.SearchItemListDto> items) {
        new Thread(() -> {
            db.appDao().deleteAllSearchItems();
            List<TagModels.SearchItemEntity> entities = new ArrayList<>();
            for (TagModels.SearchItemListDto dto : items) {
                TagModels.SearchItemEntity e = new TagModels.SearchItemEntity();
                e.tagId = dto.getTagId();
                e.epcTag = dto.getEpcTag();
                e.itemName = dto.getItemName();
                e.location = dto.getLocation();
                entities.add(e);
            }
            db.appDao().insertSearchItems(entities);
        }).start();
    }

    // Fetch list item dari API
    private void fetchData() {
        showLoading();
        api.getSearchItems(token).enqueue(new Callback<List<TagModels.SearchItemListDto>>() {
            @Override
            public void onResponse(Call<List<TagModels.SearchItemListDto>> call,
                                   Response<List<TagModels.SearchItemListDto>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    List<TagModels.SearchItemListDto> data = response.body();
                    saveToLocal(data);
                    allItemList.clear();
                    allItemList.addAll(data);
                    filteredList.clear();
                    filteredList.addAll(allItemList);
                    adapter.notifyDataSetChanged();
                } else {
                    handleApiError(response);
                }
            }

            @Override
            public void onFailure(Call<List<TagModels.SearchItemListDto>> call, Throwable t) {
                hideLoading();
                handleFailure(t);
            }
        });
    }

    // Fetch detail tag lalu tampilkan dialog
    private void fetchAndShowDetail(TagModels.SearchItemListDto item) {
        if (!isNetworkConnected()) { showWarning("Offline, cannot view detail"); return; }
        showLoading();
        api.getTagDetailSearchItem(token, item.getTagId())
                .enqueue(new Callback<TagModels.TagDetailDto>() {
                    @Override
                    public void onResponse(Call<TagModels.TagDetailDto> call,
                                           Response<TagModels.TagDetailDto> response) {
                        hideLoading();
                        if (response.isSuccessful() && response.body() != null)
                            handler.post(() -> showTagDetailDialog(item, response.body()));
                        else showError("Tag not found");
                    }

                    @Override
                    public void onFailure(Call<TagModels.TagDetailDto> call, Throwable t) {
                        hideLoading();
                        handleFailure(t);
                    }
                });
    }

    // ─── Dialog ───────────────────────────────────────────────────────────────

    // Tampilkan dialog detail tag yang dipilih
    private void showTagDetailDialog(TagModels.SearchItemListDto selectedItem,
                                     TagModels.TagDetailDto detail) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_tag_detail);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View view = dialog.getWindow().getDecorView();
        ((TextView) view.findViewById(R.id.tvDetailItemName)).setText(detail.getItemName());
        ((TextView) view.findViewById(R.id.tvDetailTagId)).setText(detail.getTagId());
        ((TextView) view.findViewById(R.id.tvDetailEpc)).setText(detail.getEpcTag());
        ((TextView) view.findViewById(R.id.tvDetailLocation)).setText(detail.getLocation());

        TextView tvStatus = view.findViewById(R.id.tvDetailStatus);
        CardView cvStatus = view.findViewById(R.id.cvDetailStatus);
        tvStatus.setText(detail.getStatus());
        cvStatus.setCardBackgroundColor(statusColor(detail.getStatus()));

        view.findViewById(R.id.btnSearchSignal).setOnClickListener(v -> {
            if (getScannerInstance() == null || getScannerInstance().getRFIDScanner() == null) {
                dialog.dismiss();
                showError("RFID not connected");
                playScanFeedback(2);
                return;
            }
            dialog.dismiss();
            Intent intent = new Intent(this, SearchSignalActivity.class);
            intent.putExtra("SELECTED_ITEM", selectedItem);
            intent.putExtra("SELECTED_DETAIL", detail);
            startActivity(intent);
        });


        dialog.show();
    }

    // ─── Scanner Callbacks ────────────────────────────────────────────────────

    @Override
    public void onRFIDDataReceived(CommScanner scanner, RFIDDataReceivedEvent event) {
        for (RFIDData data : event.getRFIDData()) {
            String epc = RfidBulkHelper.bytesToHex(data.getUII());
            handler.post(() -> {
                etSearchItem.setText(epc);
                moveScannedItemToTop(epc);
            });
        }
    }

    @Override
    public void onBarcodeDataReceived(CommScanner scanner, BarcodeDataReceivedEvent event) {
        List<BarcodeData> dataList = event.getBarcodeData();
        if (!dataList.isEmpty()) {
            String barcode = new String(dataList.get(0).getData()).trim();
            handler.post(() -> {
                etSearchItem.setText(barcode);
                etSearchItem.setSelection(etSearchItem.getText().length());
                moveScannedItemToTop(barcode);
            });
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    // Filter list berdasarkan query (nama, EPC, TagId, lokasi)
    private void filter(String text) {
        filteredList.clear();
        String query = text.toLowerCase().trim();
        if (query.isEmpty()) {
            filteredList.addAll(allItemList);
        } else {
            for (TagModels.SearchItemListDto item : allItemList) {
                String name = item.getItemName() != null ? item.getItemName().toLowerCase() : "";
                String epc = item.getEpcTag() != null ? item.getEpcTag().toLowerCase() : "";
                String tid = item.getTagId() != null ? item.getTagId().toLowerCase() : "";
                String location = item.getLocation() != null ? item.getLocation().toLowerCase() : "";
                if (name.contains(query) || epc.contains(query)
                        || tid.contains(query) || location.contains(query))
                    filteredList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Pindahkan item hasil scan ke posisi teratas list
    private void moveScannedItemToTop(String code) {
        TagModels.SearchItemListDto found = null;
        for (TagModels.SearchItemListDto item : allItemList) {
            if (code.equalsIgnoreCase(item.getEpcTag()) || code.equalsIgnoreCase(item.getTagId())) {
                found = item; break;
            }
        }
        if (found != null) {
            playScanFeedback(0);
            filteredList.remove(found);
            filteredList.add(0, found);
            adapter.setLastScannedPosition(0);
            adapter.notifyDataSetChanged();
            rvTags.scrollToPosition(0);
            showSuccess("Found: " + found.getItemName());
        } else {
            playScanFeedback(2);
            showError("Item not found");
        }
    }

    // Warna status badge berdasarkan status item
    private int statusColor(String status) {
        if (status == null) return Color.parseColor("#9E9E9E");
        switch (status.toUpperCase()) {
            case "STOCK IN": return Color.parseColor("#28a745");
            case "PREPARATION": return Color.parseColor("#ffc107");
            default: return Color.parseColor("#9E9E9E");
        }
    }
}