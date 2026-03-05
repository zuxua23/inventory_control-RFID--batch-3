package com.example.inventory_system_ht.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventory_system_ht.Adapter.TagAdapter;
import com.example.inventory_system_ht.Models.TagModel;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.List;

public class SearchItemActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etSearchItem, resultScan;
    private RecyclerView rvTags;
    private TagAdapter adapter;
    private List<TagModel> allItemList;
    private List<TagModel> filteredList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_item);

        btnBack = findViewById(R.id.btnBack);
        etSearchItem = findViewById(R.id.searchItem);
        resultScan = findViewById(R.id.resultScan);
        rvTags = findViewById(R.id.rvTags);

        allItemList = new ArrayList<>();
        filteredList = new ArrayList<>();

        // Dummy Data
        allItemList.add(new TagModel("EPC001", "Kemeja Sato Anti Kusut"));
        allItemList.add(new TagModel("EPC002", "Vans Japan Edition"));
        allItemList.add(new TagModel("EPC003", "Trucker Hat Custom"));
        allItemList.add(new TagModel("EPC004", "RFID Tag Sample"));

        filteredList.addAll(allItemList);

        adapter = new TagAdapter(filteredList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        // Fitur 1: Pencarian via Ngetik
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

        // Fitur 2: Kalau User Klik Item di List (Langsung pindah ke halaman Sinyal)
        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(SearchItemActivity.this, SearchSignalActivity.class);
            // Lempar data barang yang dipilih
            intent.putExtra("SELECTED_ITEM", item);
            // Defaultnya kita kasih mode RFID True pas pindah halaman
            intent.putExtra("IS_RFID_MODE", true);
            startActivity(intent);
        });

        // Fitur 3: Kalau User nge-Scan pakai Handheld Scanner (Mode Keyboard Wedge)
        resultScan.requestFocus();
        resultScan.setShowSoftInputOnFocus(false);
        resultScan.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String scannedData = resultScan.getText().toString().trim();
                if (!scannedData.isEmpty()) {
                    // Cari barang di list berdasarkan hasil scan
                    for (TagModel item : allItemList) {
                        if (item.getEpcTag().equals(scannedData)) {
                            Intent intent = new Intent(SearchItemActivity.this, SearchSignalActivity.class);
                            intent.putExtra("SELECTED_ITEM", item);
                            intent.putExtra("IS_RFID_MODE", true);
                            startActivity(intent);
                            break; // Stop looping kalau udah ketemu
                        }
                    }
                    resultScan.setText(""); // Kosongin lagi buat scan berikutnya
                }
                return true;
            }
            return false;
        });

        // Fitur 4: Tombol Back
        btnBack.setOnClickListener(v -> finish());
    }

    private void filter(String text) {
        filteredList.clear();
        for (TagModel item : allItemList) {
            if (item.getProductName().toLowerCase().contains(text.toLowerCase()) ||
                    item.getEpcTag().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }
}