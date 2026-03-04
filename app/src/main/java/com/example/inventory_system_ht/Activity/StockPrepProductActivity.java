package com.example.inventory_system_ht.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventory_system_ht.Adapter.TagAdapter;
import com.example.inventory_system_ht.Models.TagModel;
import com.example.inventory_system_ht.R;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class StockPrepProductActivity extends AppCompatActivity {
    private EditText resultScan;
    private TextView tvScanned, tvNoDo, tvDateDo;
    private int scanCount = 0;
    private TagAdapter adapter;
    private List<TagModel> scannedList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_prep_product);

        tvScanned = findViewById(R.id.tvScanned);
        tvNoDo = findViewById(R.id.tvNoDo);
        tvDateDo = findViewById(R.id.tvDateDo);
        resultScan = findViewById(R.id.resultScan);

        RecyclerView rvTags = findViewById(R.id.rvTags);
        scannedList = new ArrayList<>();
        adapter = new TagAdapter(scannedList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        Intent intent = getIntent();
        if (intent != null) {
            tvNoDo.setText("No : " + intent.getStringExtra("NO_DO"));
            tvDateDo.setText("Date : " + intent.getStringExtra("DATE_DO"));
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        resultScan.requestFocus();
        resultScan.setShowSoftInputOnFocus(false);
        resultScan.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String rfidData = resultScan.getText().toString().trim();
                if (!rfidData.isEmpty()) {
                    processScan(rfidData);
                    resultScan.setText("");
                }
                return true;
            }
            return false;
        });

        findViewById(R.id.btnClear).setOnClickListener(v -> {
            scannedList.clear();
            adapter.notifyDataSetChanged();
            scanCount = 0;
            tvScanned.setText("Scanned : 0");
            Snackbar.make(v, "Data dibersihkan bre", Snackbar.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnSave).setOnClickListener(v ->
                Snackbar.make(v, "Stock Preparation Disimpan", Snackbar.LENGTH_SHORT).show()
        );
    }

    private void processScan(String rfid) {
        scanCount++;
        tvScanned.setText("Scanned : " + scanCount);
        scannedList.add(new TagModel(rfid, "Product Verified"));
        adapter.notifyItemInserted(scannedList.size() - 1);
        Snackbar.make(findViewById(android.R.id.content), "Scan: " + rfid, Snackbar.LENGTH_SHORT).show();
    }
}