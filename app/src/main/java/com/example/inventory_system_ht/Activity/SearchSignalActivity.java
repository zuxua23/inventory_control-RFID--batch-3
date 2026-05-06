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
import com.example.inventory_system_ht.Helper.RfidBulkHelper;
import com.example.inventory_system_ht.Helper.ScannerManager;
import com.example.inventory_system_ht.Models.TagModels;
import com.example.inventory_system_ht.R;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchSignalActivity extends BaseScannerActivity implements RFIDDataDelegate {

    private TagModels.SearchItemListDto selectedItem;
    private TagModels.TagDetailDto selectedDetail;

    private LinearLayout containerSignalBars;
    private TextView tvItemTitle, tvRssiValue;
    private CardView btnPowerDropdown;
    private TextView tvPowerLevel;

    private ApiService api;
    private String token;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private BottomSheetDialog currentDialog;

    private final List<String> powerList = Arrays.asList(
            "10 dBm", "15 dBm", "20 dBm", "25 dBm", "27 dBm"
    );

    // ── Scanner via ScannerManager ────────────────────────────────
    @Override
    protected CommScanner getScannerInstance() {
        return ScannerManager.getInstance().getScanner();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_signal);

        token = "Bearer " + new PrefManager(this).getToken();
        api   = ApiClient.getClient(this).create(ApiService.class);

        selectedItem   = (TagModels.SearchItemListDto) getIntent().getSerializableExtra("SELECTED_ITEM");
        selectedDetail = (TagModels.TagDetailDto)      getIntent().getSerializableExtra("SELECTED_DETAIL");

        initUI();

        if (selectedItem != null) {
            String location = (selectedDetail != null && selectedDetail.getLocation() != null)
                    ? selectedDetail.getLocation() : "-";
            tvItemTitle.setText("Locating: " + selectedItem.getItemName() + " | " + location);
        }

        // Power dropdown — activity ini RFID only, selalu visible
        btnPowerDropdown.setVisibility(View.VISIBLE);
        btnPowerDropdown.setOnClickListener(v ->
                showPowerDropdownPopup(btnPowerDropdown, powerList, tvPowerLevel));

        findViewById(R.id.btnStopSearch).setOnClickListener(v -> finish());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        showWarning("Pull trigger to locate item via RFID");
    }

    private void initUI() {
        containerSignalBars = findViewById(R.id.containerSignalBars);
        tvItemTitle         = findViewById(R.id.tvItemTitle);
        tvRssiValue         = findViewById(R.id.tvRssiValue);
        btnPowerDropdown    = findViewById(R.id.btnPowerDropdown);
        tvPowerLevel        = findViewById(R.id.tvPowerLevel);
    }

    // ── RFID Callback ─────────────────────────────────────────────
    // Activity ini hanya listen RFID signal dari 1 tag spesifik (locating)
    @Override
    public void onRFIDDataReceived(CommScanner scanner, RFIDDataReceivedEvent event) {
        for (RFIDData data : event.getRFIDData()) {
            String epc  = RfidBulkHelper.bytesToHex(data.getUII());
            float  rssi = data.getRSSI() / 10f; // SDK return x10, e.g. -605 = -60.5 dBm

            if (selectedItem != null && epc.equalsIgnoreCase(selectedItem.getEpcTag())) {
                handler.post(() -> {
                    playScanFeedback(0);
                    updateSignalBars(rssi);
                });
            }
        }
    }

    // ── Signal Bars ───────────────────────────────────────────────

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

    // ── Bottom Sheet Detail ───────────────────────────────────────

    private void showTagDetailBottomSheet(TagModels.TagDetailDto detail) {
        if (currentDialog != null && currentDialog.isShowing()) currentDialog.dismiss();
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

    // ── Lifecycle ─────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        CommScanner scanner = getScannerInstance();
        updateReaderBattery(findViewById(R.id.ivReaderBattery));

        // Activity ini RFID only — langsung openInventory saat resume
        if (scanner != null) {
            int power = parsePower(tvPowerLevel.getText().toString(), 20);
            RfidBulkHelper.closeBarcode(scanner);
            RfidBulkHelper.openInventory(scanner, this, power);
        } else {
            showWarning("SP1 Reader not connected!");
        }

        if (getHTBatteryLevel() <= 15) {
            showWarning("Battery " + getHTBatteryLevel() + "%, charge now!");
            playScanFeedback(2);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CommScanner scanner = getScannerInstance();
        RfidBulkHelper.closeInventory(scanner);
        if (currentDialog != null && currentDialog.isShowing()) currentDialog.dismiss();
    }

    // ── Helper ────────────────────────────────────────────────────

    private int parsePower(String text, int defaultVal) {
        try { return Integer.parseInt(text.replace(" dBm", "").trim()); }
        catch (NumberFormatException e) { return defaultVal; }
    }
}