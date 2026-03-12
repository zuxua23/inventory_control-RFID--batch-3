package com.example.inventory_system_ht.Activity;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.example.inventory_system_ht.Models.AuthModels;
import com.example.inventory_system_ht.Models.GeneralResponse;
import com.example.inventory_system_ht.Models.TagModels;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;


public class TagRegisActivity extends BaseScannerActivity implements BarcodeDataDelegate, RFIDDataDelegate {
    private CommScanner mCommScanner;
    private ImageView btnBack;
    private EditText resultScan;
    private TextView tvScanned;
    private Switch switchRfid;
    private Button btnClear, btnSubmitRegis;
    private RecyclerView rvTags;
    private TagAdapter adapter;
    private List<TagModels.TagModel> registeredTagList;
    private Handler handler = new Handler();
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regist);

        btnBack = findViewById(R.id.btnBack);
        resultScan = findViewById(R.id.resultScan);
        tvScanned = findViewById(R.id.tvScanned);
        switchRfid = findViewById(R.id.switchRfid);
        btnClear = findViewById(R.id.btnClear);
        btnSubmitRegis = findViewById(R.id.btnSubmitRegis);
        rvTags = findViewById(R.id.rvTags);

        registeredTagList = new ArrayList<>();
        adapter = new TagAdapter(registeredTagList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnClear.setOnClickListener(v -> {
            registeredTagList.clear();
            adapter.notifyDataSetChanged();
            updateScanCount();
            showSagaFeedback("List cleared!", true);
        });

        btnSubmitRegis.setOnClickListener(v -> {
            if (registeredTagList.isEmpty()) {
                showSagaFeedback("No tags scanned yet!", false);
            } else {
                showBulkConfirmDialog();
            }
        });

        resultScan.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String data = s.toString().trim();
                if (data.length() >= 7 && !isProcessing && !switchRfid.isChecked()) {
                    isProcessing = true;
                    processScannedData(data, false);
                    resultScan.setText("");
                    isProcessing = false;
                }
            }
        });

        setupScanner();
        resultScan.requestFocus();
    }

    private void processScannedData(String scannedData, boolean isFromRfid) {
        if (isFromRfid) {
            boolean exists = false;
            for (TagModels.TagModel t : registeredTagList) {
                if (t.getEpcTag().equals(scannedData)) {
                    exists = true;
                    playScanFeedback(1);
                    break;
                }
            }
            if (!exists) {
                registeredTagList.add(0, new TagModels.TagModel(scannedData, scannedData, "TAG", "Scanned Item", "STAGING", 0));

                runOnUiThread(() -> {
                    if(adapter != null) adapter.setLastScannedPosition(0);

                    adapter.notifyItemInserted(0);
                    rvTags.scrollToPosition(0);
                    updateScanCount();
                    playScanFeedback(0);
                });
            }
        } else {
            playScanFeedback(0);
            runOnUiThread(() -> showSingleConfirmDialog(scannedData));
        }
    }

    private void updateScanCount() {
        tvScanned.setText("Qty: " + registeredTagList.size());
    }

    private void showSingleConfirmDialog(String scannedData) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_regist);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvTitle);
        tvTitle.setText("Register Tag:\n" + scannedData);

        dialog.findViewById(R.id.btnNo).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnYes).setOnClickListener(v -> {
            dialog.dismiss();
            hitApiRegisterTags(Collections.singletonList(scannedData));
        });
        dialog.show();
    }

    private void showBulkConfirmDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_regist);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = dialog.findViewById(R.id.tvTitle);
        tvTitle.setText("Register " + registeredTagList.size() + " Tags?");

        dialog.findViewById(R.id.btnNo).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnYes).setOnClickListener(v -> {
            dialog.dismiss();
            List<String> ids = new ArrayList<>();
            for(TagModels.TagModel t : registeredTagList) ids.add(t.getEpcTag());
            hitApiRegisterTags(ids);
        });
        dialog.show();
    }

    private void hitApiRegisterTags(List<String> tagIds) {
        if (!isNetworkConnected()) {
            showSagaFeedback("Offline Mode! Save your data on your phone first.", false);
            playScanFeedback(1);

            new Thread(() -> {
                AppDao localDao = AppDatabase.getDatabase(TagRegisActivity.this).appDao();
                for (String epc : tagIds) {
                    localDao.insertScannedTag(new TagModels.TagModel(epc, epc, "TAG", "Scanned Offline", "STAGING", 0));
                }

                runOnUiThread(() -> {
                    registeredTagList.clear();
                    adapter.notifyDataSetChanged();
                    updateScanCount();
                });
            }).start();

            return;
        }

        showLoading();
        PrefManager pref = new PrefManager(this);
        String token = "Bearer " + pref.getToken();

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.registerTags(token, new AuthModels.RegisterRequest(tagIds)).enqueue(new retrofit2.Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, retrofit2.Response<GeneralResponse> response) {
                hideLoading();
                if (response.isSuccessful()) {
                    showSagaFeedback("Success: " + response.body().getMessage(), true);
                    playScanFeedback(0);
                    registeredTagList.clear();
                    adapter.notifyDataSetChanged();
                    updateScanCount();
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
    }

    @Override
    public void onRFIDDataReceived(CommScanner scanner, RFIDDataReceivedEvent event) {
        if (!switchRfid.isChecked()) return;
        for (RFIDData data : event.getRFIDData()) {
            String epc = bytesToHexString(data.getUII());
            processScannedData(epc, true);
        }
    }

    @Override
    public void onBarcodeDataReceived(CommScanner scanner, BarcodeDataReceivedEvent event) {
        if (switchRfid.isChecked()) return;
        if (!event.getBarcodeData().isEmpty()) {
            String barcode = new String(event.getBarcodeData().get(0).getData());
            processScannedData(barcode, false);
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
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupScanner();
        resultScan.requestFocus();

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
}