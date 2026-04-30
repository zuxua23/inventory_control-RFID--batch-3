package com.example.inventory_system_ht.Activity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.densowave.scannersdk.RFID.RFIDData;
import com.densowave.scannersdk.RFID.RFIDDataReceivedEvent;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.example.inventory_system_ht.Helper.ApiClient;
import com.example.inventory_system_ht.Helper.ApiService;
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.Models.TagModels;
import com.example.inventory_system_ht.R;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchSignalActivity extends BaseScannerActivity implements RFIDDataDelegate {

    private TagModels.SearchItemListDto selectedItem;
    private TagModels.TagDetailDto selectedDetail;

    private LinearLayout containerSignalBars;
    private TextView tvItemTitle, tvRssiValue;
    private Button btnStopSearch;
    private CardView btnPowerDropdown;
    private ImageView btnBack;
    private CommScanner mCommScanner;

    private ApiService api;
    private String token;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private BottomSheetDialog currentDialog;

    @Override
    protected CommScanner getScannerInstance() { return mCommScanner; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_signal);

        PrefManager pref = new PrefManager(this);
        token = "Bearer " + pref.getToken();
        api   = ApiClient.getClient(this).create(ApiService.class);

        selectedItem   = (TagModels.SearchItemListDto) getIntent().getSerializableExtra("SELECTED_ITEM");
        selectedDetail = (TagModels.TagDetailDto) getIntent().getSerializableExtra("SELECTED_DETAIL");

        btnPowerDropdown = findViewById(R.id.btnPowerDropdown);

        initUI();

        if (selectedItem != null) {
            String location = (selectedDetail != null && selectedDetail.getLocation() != null)
                    ? selectedDetail.getLocation() : "-";
            tvItemTitle.setText("Locating: " + selectedItem.getItemName() + " | " + location);
        }

        setupScanner();

        TextView tvPowerLevel = findViewById(R.id.tvPowerLevel);
        setupPowerDropdown(btnPowerDropdown, null, tvPowerLevel);
        btnPowerDropdown.setVisibility(View.VISIBLE);

        showWarning("Pull trigger to locate item via RFID");

        btnStopSearch.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
    }

    private void initUI() {
        containerSignalBars = findViewById(R.id.containerSignalBars);
        tvItemTitle         = findViewById(R.id.tvItemTitle);
        tvRssiValue         = findViewById(R.id.tvRssiValue);
        btnStopSearch       = findViewById(R.id.btnStopSearch);
        btnBack             = findViewById(R.id.btnBack);
    }

    private void setupScanner() {
        if (mCommScanner == null) return;
        try {
            mCommScanner.getRFIDScanner().setDataDelegate(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRFIDDataReceived(CommScanner scanner, RFIDDataReceivedEvent event) {
        for (RFIDData data : event.getRFIDData()) {
            String epc  = bytesToHexString(data.getUII());
            float  rssi = data.getRSSI();

            if (selectedItem != null && epc.equalsIgnoreCase(selectedItem.getEpcTag())) {
                handler.post(() -> {
                    playScanFeedback(0);
                    updateSignalBars(rssi);
                });
            }
        }
    }

    private void showTagDetailBottomSheet(TagModels.TagDetailDto detail) {
        if (currentDialog != null && currentDialog.isShowing())
            currentDialog.dismiss();

        currentDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_tag_detail, null);

        ((TextView) view.findViewById(R.id.tvDetailItemName)).setText(detail.getItemName());
        ((TextView) view.findViewById(R.id.tvDetailTagId)).setText(detail.getTagId());
        ((TextView) view.findViewById(R.id.tvDetailEpc)).setText(detail.getEpcTag());
        ((TextView) view.findViewById(R.id.tvDetailLocation)).setText(detail.getLocation());

        TextView tvStatus = view.findViewById(R.id.tvDetailStatus);
        CardView cvStatus = view.findViewById(R.id.cvDetailStatus);
        tvStatus.setText(detail.getStatus());
        cvStatus.setCardBackgroundColor(statusColor(detail.getStatus()));

        view.findViewById(R.id.btnSearchSignal).setVisibility(View.GONE);

        currentDialog.setContentView(view);
        currentDialog.show();
    }

    private int statusColor(String status) {
        if (status == null) return Color.parseColor("#9E9E9E");
        switch (status.toUpperCase()) {
            case "STOCK IN":    return Color.parseColor("#28a745");
            case "PREPARATION": return Color.parseColor("#ffc107");
            default:            return Color.parseColor("#9E9E9E");
        }
    }

    public void updateSignalBars(float rssi) {
        int level;
        if      (rssi > -45) level = 10;
        else if (rssi > -50) level = 9;
        else if (rssi > -55) level = 8;
        else if (rssi > -60) level = 7;
        else if (rssi > -65) level = 6;
        else if (rssi > -70) level = 5;
        else if (rssi > -75) level = 4;
        else if (rssi > -80) level = 3;
        else if (rssi > -85) level = 2;
        else if (rssi > -90) level = 1;
        else                 level = 0;

        handler.post(() -> {
            tvRssiValue.setText(String.format("%.1f dBm", rssi));
            for (int i = 0; i < containerSignalBars.getChildCount(); i++) {
                containerSignalBars.getChildAt(i).setBackgroundColor(
                        i < level ? Color.parseColor("#03A9F4")
                                : Color.parseColor("#E0E0E0"));
            }
        });
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
        updateReaderBattery(findViewById(R.id.ivReaderBattery));
        if (getHTBatteryLevel() <= 15) {
            showSagaFeedback("Battery " + getHTBatteryLevel() + "%, charge now!", false);
            playScanFeedback(2);
        }
        if (selectedItem != null)
            showWarning("Battery " + getHTBatteryLevel() + "%, charge now!");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCommScanner != null) {
            try {
                mCommScanner.getRFIDScanner().setDataDelegate(null);
            } catch (Exception ignored) {}
        }
        if (currentDialog != null && currentDialog.isShowing())
            currentDialog.dismiss();
    }
}