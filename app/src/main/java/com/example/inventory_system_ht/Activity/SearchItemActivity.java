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
import android.widget.ImageView;
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
import com.example.inventory_system_ht.Models.TagModels;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchItemActivity extends BaseScannerActivity implements BarcodeDataDelegate, RFIDDataDelegate {

    private ImageView btnBack;
    private EditText etSearchItem;
    private RecyclerView rvTags;
    private CardView btnRefresh;
    private SearchItemAdapter adapter;
    private List<TagModels.SearchItemListDto> allItemList;
    private List<TagModels.SearchItemListDto> filteredList;
    private CommScanner mCommScanner;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ApiService api;
    private String token;
    private AppDatabase db;

    @Override
    protected CommScanner getScannerInstance() { return mCommScanner; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_item);

        PrefManager pref = new PrefManager(this);
        token = "Bearer " + pref.getToken();
        api   = ApiClient.getClient(this).create(ApiService.class);
        db    = AppDatabase.getDatabase(this);

        btnBack      = findViewById(R.id.btnBack);
        etSearchItem = findViewById(R.id.searchItem);
        rvTags       = findViewById(R.id.rvTags);
        btnRefresh   = findViewById(R.id.btnRefresh);

        allItemList  = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new SearchItemAdapter(filteredList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        setupScanner();
        loadFromLocal();

        if (isNetworkConnected()) {
            fetchData();
        } else {
            showWarning("Offline Mode.");
        }

        etSearchItem.requestFocus();

        etSearchItem.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
        });

        adapter.setOnItemClickListener(this::fetchAndShowDetail);

        btnRefresh.setOnClickListener(v -> {
            if (!isNetworkConnected()) {
                showWarning("No internet connection!");
                return;
            }
            fetchData();
        });

        btnBack.setOnClickListener(v -> finish());
    }

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
        if (!allItemList.isEmpty())
            showSuccess("Loaded " + allItemList.size());
    }

    private void saveToLocal(List<TagModels.SearchItemListDto> items) {
        db.appDao().deleteAllSearchItems();
        List<TagModels.SearchItemEntity> entities = new ArrayList<>();
        for (TagModels.SearchItemListDto dto : items) {
            TagModels.SearchItemEntity e = new TagModels.SearchItemEntity();
            e.tagId    = dto.getTagId();
            e.epcTag   = dto.getEpcTag();
            e.itemName = dto.getItemName();
            e.location = dto.getLocation();
            entities.add(e);
        }
        db.appDao().insertSearchItems(entities);
    }

    private void fetchData() {
        showLoading();

        api.getSearchItems(token).enqueue(new Callback<List<TagModels.SearchItemListDto>>() {
            @Override
            public void onResponse(Call<List<TagModels.SearchItemListDto>> call,
                                   Response<List<TagModels.SearchItemListDto>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    List<TagModels.SearchItemListDto> data = response.body();
                    saveToLocal(data); // simpan ke lokal
                    allItemList.clear();
                    allItemList.addAll(data);
                    filteredList.clear();
                    filteredList.addAll(allItemList);
                    adapter.notifyDataSetChanged();
                } else {
                    handleApiError(response.code());
                    showError("Failed to retrieve data!");
                }
            }

            @Override
            public void onFailure(Call<List<TagModels.SearchItemListDto>> call, Throwable t) {
                hideLoading();
                handleFailure(t);
                showError("Failed to connect: " + t.getMessage());
            }
        });
    }

    private void fetchAndShowDetail(TagModels.SearchItemListDto item) {
        if (!isNetworkConnected()) {
            showWarning("Offline: Cannot view detail.");
            return;
        }
        showLoading();
        api.getTagDetailSearchItem(token, item.getTagId()).enqueue(new Callback<TagModels.TagDetailDto>() {
            @Override
            public void onResponse(Call<TagModels.TagDetailDto> call,
                                   Response<TagModels.TagDetailDto> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    handler.post(() -> showTagDetailDialog(item, response.body()));
                } else {
                    showError("Tag not found or deleted");
                }
            }

            @Override
            public void onFailure(Call<TagModels.TagDetailDto> call, Throwable t) {
                hideLoading();
                showError("Error: " + t.getMessage());
            }
        });
    }

    private void showTagDetailDialog(TagModels.SearchItemListDto selectedItem, TagModels.TagDetailDto detail) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_tag_detail);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
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
            CommScanner cs = getScannerInstance();
            boolean rfidReady = (cs != null && cs.getRFIDScanner() != null);
            if (!rfidReady) {
                showError("RFID reader not connected!");
                playScanFeedback(2);
                return;
            }
            dialog.dismiss();
            Intent intent = new Intent(SearchItemActivity.this, SearchSignalActivity.class);
            intent.putExtra("SELECTED_ITEM", selectedItem);
            intent.putExtra("SELECTED_DETAIL", detail);
            startActivity(intent);
        });
        dialog.show();
    }

    private int statusColor(String status) {
        if (status == null) return Color.parseColor("#9E9E9E");
        switch (status.toUpperCase()) {
            case "STOCK IN":    return Color.parseColor("#28a745");
            case "PREPARATION": return Color.parseColor("#ffc107");
            default:            return Color.parseColor("#9E9E9E");
        }
    }

    private void filter(String text) {
        filteredList.clear();
        String query = text.toLowerCase().trim();
        if (query.isEmpty()) {
            filteredList.addAll(allItemList);
        } else {
            for (TagModels.SearchItemListDto item : allItemList) {
                String name     = item.getItemName() != null ? item.getItemName().toLowerCase() : "";
                String epc      = item.getEpcTag()   != null ? item.getEpcTag().toLowerCase()   : "";
                String tid      = item.getTagId()    != null ? item.getTagId().toLowerCase()    : "";
                String location = item.getLocation() != null ? item.getLocation().toLowerCase() : "";
                if (name.contains(query) || epc.contains(query) || tid.contains(query) || location.contains(query))
                    filteredList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setupScanner() {
        if (mCommScanner != null) {
            try {
                mCommScanner.getRFIDScanner().setDataDelegate(this);
                mCommScanner.getBarcodeScanner().setDataDelegate(this);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @Override
    public void onRFIDDataReceived(CommScanner scanner, RFIDDataReceivedEvent event) {
        for (RFIDData data : event.getRFIDData()) {
            String epc = bytesToHexString(data.getUII());
            handler.post(() -> { etSearchItem.setText(epc); moveScannedItemToTop(epc); });
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
            showSuccess("Item Found: " + found.getItemName());
        } else {
            playScanFeedback(2);
            showError("Item not found in data!");
        }
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupScanner();
        if (getHTBatteryLevel() <= 15) {
            showWarning("Battery " + getHTBatteryLevel() + "%, charge now!");
            playScanFeedback(2);
        }
        if (etSearchItem != null)
            etSearchItem.postDelayed(() -> etSearchItem.requestFocus(), 200);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCommScanner != null) {
            try {
                mCommScanner.getRFIDScanner().setDataDelegate(null);
                mCommScanner.getBarcodeScanner().setDataDelegate(null);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}