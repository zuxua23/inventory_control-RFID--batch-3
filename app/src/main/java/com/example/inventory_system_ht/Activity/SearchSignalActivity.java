package com.example.inventory_system_ht.Activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.inventory_system_ht.Models.TagModel;
import com.example.inventory_system_ht.R;

public class SearchSignalActivity extends AppCompatActivity {

    private TagModel selectedItem;
    private boolean isRfidMode;
    private LinearLayout containerSignalBars;
    private TextView tvItemTitle, tvRssiValue, tvModeLabel;
    private Switch switchRfid;
    private Button btnStopSearch;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_signal);

        // 1. Ambil data dari halaman sebelumnya
        selectedItem = (TagModel) getIntent().getSerializableExtra("SELECTED_ITEM");
        isRfidMode = getIntent().getBooleanExtra("IS_RFID_MODE", true);

        // 2. Inisialisasi UI
        initUI();

        // 3. Set Tampilan Awal
        if (selectedItem != null) {
            tvItemTitle.setText("Locating: " + selectedItem.getProductName());
        }

        tvModeLabel.setText(isRfidMode ? "RFID" : "BARCODE");
        switchRfid.setChecked(isRfidMode);

        // 4. Logika Ganti Mode
        switchRfid.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isRfidMode = isChecked;
            tvModeLabel.setText(isChecked ? "RFID" : "BARCODE");
            if (!isChecked) {
                resetBars();
                tvRssiValue.setText("Barcode Mode: Ready");
            } else {
                tvRssiValue.setText("-00 dBm");
            }
        });

        // Tombol Navigasi
        btnStopSearch.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
    }

    private void initUI() {
        containerSignalBars = findViewById(R.id.containerSignalBars);
        tvItemTitle = findViewById(R.id.tvItemTitle);
        tvRssiValue = findViewById(R.id.tvRssiValue);
        tvModeLabel = findViewById(R.id.tvModeLabel);
        switchRfid = findViewById(R.id.switchRfid);
        btnStopSearch = findViewById(R.id.btnStopSearch);
        btnBack = findViewById(R.id.btnBack);
    }

    // Fungsi sakti buat update 10 bar lu, Jan!
    public void updateSignalBars(float rssi) {
        // Mapping RSSI (-30 deket banget, -90 jauh banget) ke 10 level
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

        runOnUiThread(() -> {
            tvRssiValue.setText(String.format("%.1f dBm", rssi));
            for (int i = 0; i < containerSignalBars.getChildCount(); i++) {
                View bar = containerSignalBars.getChildAt(i);
                // Ganti warna jadi biru tema lu kalau level terpenuhi
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
}