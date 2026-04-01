package com.example.inventory_system_ht.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;

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
    private TagAdapter adapter;
    private List<TagModels.TagModel> allItemList;
    private List<TagModels.TagModel> filteredList;
    private CommScanner mCommScanner;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ApiService api;
    private String token;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_item);

        PrefManager pref = new PrefManager(this);
        token = "Bearer " + pref.getToken();
        api = ApiClient.getClient(this).create(ApiService.class);

        btnBack = findViewById(R.id.btnBack);
        etSearchItem = findViewById(R.id.searchItem);
        rvTags = findViewById(R.id.rvTags);

        allItemList = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new TagAdapter(filteredList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        setupScanner();

        if (isNetworkConnected()) {
            fetchStockData();
        } else {
            showSagaFeedback("Offline Mode: Unable to retrieve data from server.", false);
        }

        etSearchItem.requestFocus();

        etSearchItem.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(SearchItemActivity.this, SearchSignalActivity.class);
            intent.putExtra("SELECTED_ITEM", item);
            intent.putExtra("IS_RFID_MODE", false);
            startActivity(intent);
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void fetchStockData() {
        showLoading();
        showSagaFeedback("Pulling master data...", true);

        api.getStockData(token).enqueue(new Callback<List<TagModels.TagModel>>() {
            @Override
            public void onResponse(Call<List<TagModels.TagModel>> call, Response<List<TagModels.TagModel>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    allItemList.clear();
                    allItemList.addAll(response.body());

                    filteredList.clear();
                    filteredList.addAll(allItemList);

                    adapter.notifyDataSetChanged();
                    showSagaFeedback("Loading successful " + allItemList.size() + " data!", true);
                } else {
                    handleApiError(response.code());
                    showSagaFeedback("Failed to retrieve master data!", false);
                }
            }

            @Override
            public void onFailure(Call<List<TagModels.TagModel>> call, Throwable t) {
                hideLoading();
                handleFailure(t);
                showSagaFeedback("Failed to connect to server: " + t.getMessage(), false);
            }
        });
    }

    private void filter(String text) {
        filteredList.clear();
        String query = text.toLowerCase().trim();

        if (query.isEmpty()) {
            filteredList.addAll(allItemList);
        } else {
            for (TagModels.TagModel item : allItemList) {
                String productName = item.getProductName() != null ? item.getProductName().toLowerCase() : "";
                String epc = item.getEpcTag() != null ? item.getEpcTag().toLowerCase() : "";

                if (productName.contains(query) || epc.contains(query)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setupScanner() {
        if (mCommScanner != null) {
            try {
                mCommScanner.getRFIDScanner().setDataDelegate(this);
                mCommScanner.getBarcodeScanner().setDataDelegate(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRFIDDataReceived(CommScanner scanner, RFIDDataReceivedEvent event) {
        List<RFIDData> dataList = event.getRFIDData();
        for (RFIDData data : dataList) {
            String epc = bytesToHexString(data.getUII());
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
            String barcode = new String(dataList.get(0).getData());
            handler.post(() -> {
                etSearchItem.setText(barcode);
                etSearchItem.setSelection(etSearchItem.getText().length());
                moveScannedItemToTop(barcode);
            });
        }
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
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

        if (etSearchItem != null) {
            etSearchItem.postDelayed(() -> etSearchItem.requestFocus(), 200);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCommScanner != null) {
            try {
                mCommScanner.getRFIDScanner().setDataDelegate(null);
                mCommScanner.getBarcodeScanner().setDataDelegate(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void moveScannedItemToTop(String epcOrBarcode) {
        TagModels.TagModel foundItem = null;

        for (TagModels.TagModel item : allItemList) {
            if (item.getEpcTag().equalsIgnoreCase(epcOrBarcode)) {
                foundItem = item;
                break;
            }
        }

        if (foundItem != null) {
            playScanFeedback(0);

            filteredList.remove(foundItem);
            filteredList.add(0, foundItem);

            if (adapter != null) {
                adapter.setLastScannedPosition(0);
            }

            adapter.notifyDataSetChanged();
            rvTags.scrollToPosition(0);
            showSagaFeedback("Item Found: " + foundItem.getProductName(), true);
        } else {
            playScanFeedback(2);
            showSagaFeedback("Item not found in master data!", false);
        }
    }
}