package com.example.inventory_system_ht.Activity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
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

public class SearchItemActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etSearchItem, resultScan;
    private Switch switchRfid;
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
        switchRfid = findViewById(R.id.switchRfid);
        rvTags = findViewById(R.id.rvTags);

        allItemList = new ArrayList<>();
        filteredList = new ArrayList<>();

        allItemList.add(new TagModel("EPC001", "Kemeja Sato Anti Kusut"));
        allItemList.add(new TagModel("EPC002", "Vans Japan Edition"));
        allItemList.add(new TagModel("EPC003", "Trucker Hat Custom"));
        allItemList.add(new TagModel("EPC004", "RFID Tag Sample"));

        filteredList.addAll(allItemList);

        adapter = new TagAdapter(filteredList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

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

        resultScan.requestFocus();
        resultScan.setShowSoftInputOnFocus(false);
// Di dalam onCreate SearchItemActivity.java tambahin ini:

        // Di dalam adapter setOnItemClickListener
        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(SearchItemActivity.this, SearchSignalActivity.class);
            intent.putExtra("SELECTED_ITEM", item);
            intent.putExtra("IS_RFID_MODE", switchRfid.isChecked());
            startActivity(intent);
        });

// Dan di resultScan (kalau user nge-scan barcode/tag buat nyari barangnya)
        resultScan.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String scannedData = resultScan.getText().toString().trim();
                if (!scannedData.isEmpty()) {
                    // Cari dulu di list, ada gak barangnya?
                    for (TagModel item : allItemList) {
                        if (item.getEpcTag().equals(scannedData)) {
                            // Kalau ketemu, langsung gas ke halaman Search
                            Intent intent = new Intent(SearchItemActivity.this, SearchSignalActivity.class);
                            intent.putExtra("PRODUCT_NAME", item.getProductName());
                            intent.putExtra("TARGET_EPC", item.getEpcTag());
                            intent.putExtra("IS_RFID_MODE", switchRfid.isChecked());
                            startActivity(intent);
                            break;
                        }
                    }
                    resultScan.setText("");
                }
                return true;
            }
            return false;
        });

        btnBack.setOnClickListener(v -> finish());
        switchRfid.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String msg = isChecked ? "RFID Mode: ON" : "RFID Mode: OFF";
            Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show();
            if (isChecked) resultScan.requestFocus();
        });
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

    private void updateSignalLevel(LinearLayout container, int level) {
        if (container == null) return;
        for (int i = 0; i < container.getChildCount(); i++) {
            View bar = container.getChildAt(i);
            bar.setBackgroundColor(i < level ? Color.BLACK : Color.parseColor("#E0E0E0"));
        }
    }
}