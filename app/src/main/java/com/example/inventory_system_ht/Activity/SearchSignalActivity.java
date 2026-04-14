package com.example.inventory_system_ht.Activity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.densowave.scannersdk.RFID.RFIDData;
import com.densowave.scannersdk.RFID.RFIDDataReceivedEvent;

import com.example.inventory_system_ht.Models.TagModels;
import com.example.inventory_system_ht.R;

import java.util.List;

public class SearchSignalActivity extends BaseScannerActivity implements RFIDDataDelegate {

    private TagModels.TagModel selectedItem;
    private boolean isRfidMode;
    private LinearLayout containerSignalBars;
    private TextView tvItemTitle, tvRssiValue;
    private Switch switchRfid;
    private Button btnStopSearch;
    private CardView btnPowerDropdown;
    private ImageView btnBack;
    private CommScanner mCommScanner;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected CommScanner getScannerInstance() {
        return mCommScanner;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_signal);

        selectedItem = (TagModels.TagModel) getIntent().getSerializableExtra("SELECTED_ITEM");

        isRfidMode = false;
        btnPowerDropdown = findViewById(R.id.btnPowerDropdown);

        initUI();

        if (selectedItem != null) {
            tvItemTitle.setText("Locating: " + selectedItem.getProductName());
        }

        setupScanner();
        switchRfid.setChecked(false);
        TextView tvPowerLevel = findViewById(R.id.tvPowerLevel);

        setupPowerDropdown(btnPowerDropdown, switchRfid, tvPowerLevel);

        switchRfid.setOnCheckedChangeListener((buttonView, isChecked) -> {
            CommScanner currentScanner = getScannerInstance();

            if (isChecked) {
                boolean isRfidReady = (currentScanner != null && currentScanner.getRFIDScanner() != null);

                if (!isRfidReady) {
                    showSagaFeedback("HT not Connected to Reader RFID", false);
                    switchRfid.setChecked(false);
                    return;
                }
                btnPowerDropdown.setVisibility(View.VISIBLE);
            } else {
                btnPowerDropdown.setVisibility(View.GONE);
            }

            isRfidMode = isChecked;

            String msg = isChecked ? "Search Mode: RFID ON" : "Search Mode: BARCODE (Manual)";
            showSagaFeedback(msg, true);

            if (!isChecked) {
                resetBars();
                tvRssiValue.setText("Barcode Mode: Ready");
            } else {
                tvRssiValue.setText("-00 dBm");
            }
        });

        btnStopSearch.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
    }

    private void initUI() {
        containerSignalBars = findViewById(R.id.containerSignalBars);
        tvItemTitle = findViewById(R.id.tvItemTitle);
        tvRssiValue = findViewById(R.id.tvRssiValue);
        switchRfid = findViewById(R.id.switchRfid);
        btnStopSearch = findViewById(R.id.btnStopSearch);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupScanner() {
        if (mCommScanner != null) {
            try {
                mCommScanner.getRFIDScanner().setDataDelegate(this);
                // Set barcode delegate juga buat jaga-jaga kalau mau manual scan
                // mCommScanner.getBarcodeScanner().setDataDelegate(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRFIDDataReceived(CommScanner scanner, RFIDDataReceivedEvent event) {
        if (!isRfidMode) return;

        List<RFIDData> dataList = event.getRFIDData();
        for (RFIDData data : dataList) {
            String epc = bytesToHexString(data.getUII());

            if (selectedItem != null && epc.equalsIgnoreCase(selectedItem.getEpcTag())) {
                float rssi = data.getRSSI();

                handler.post(() -> {
                    playScanFeedback(0);
                    updateSignalBars(rssi);
                });
            }
        }
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public void updateSignalBars(float rssi) {
        int level;
        if (rssi > -45) level = 10;
        else if (rssi > -50) level = 9;
        else if (rssi > -55) level = 8;
        else if (rssi > -60) level = 7;
        else if (rssi > -65) level = 6;
        else if (rssi > -70) level = 5;
        else if (rssi > -75) level = 4;
        else if (rssi > -80) level = 3;
        else if (rssi > -85) level = 2;
        else if (rssi > -90) level = 1;
        else level = 0;

        handler.post(() -> {
            tvRssiValue.setText(String.format("%.1f dBm", rssi));
            for (int i = 0; i < containerSignalBars.getChildCount(); i++) {
                View bar = containerSignalBars.getChildAt(i);
                bar.setBackgroundColor(i < level ?
                        Color.parseColor("#03A9F4") : Color.parseColor("#E0E0E0"));
            }
        });
    }

    private void resetBars() {
        for (int i = 0; i < containerSignalBars.getChildCount(); i++) {
            containerSignalBars.getChildAt(i).setBackgroundColor(Color.parseColor("#E0E0E0"));
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

        if (selectedItem != null) {
            showSagaFeedback("Ready. Turn on RFID and pull trigger to locate item.", true);
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}