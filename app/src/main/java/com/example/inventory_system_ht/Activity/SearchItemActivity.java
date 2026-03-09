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

// 👇 IMPORT SDK DENSO 👇
import com.densowave.scannersdk.Barcode.BarcodeData;
import com.densowave.scannersdk.Barcode.BarcodeDataReceivedEvent;
import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.BarcodeDataDelegate;
import com.densowave.scannersdk.Listener.RFIDDataDelegate;
import com.densowave.scannersdk.RFID.RFIDData;
import com.densowave.scannersdk.RFID.RFIDDataReceivedEvent;

import com.example.inventory_system_ht.Adapter.TagAdapter;
import com.example.inventory_system_ht.Models.TagModel;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.List;

/**
 * SearchItemActivity: Mencari barang dalam list.
 * Sudah terintegrasi dengan BaseScannerActivity untuk cek koneksi & feedback.
 */
public class SearchItemActivity extends BaseScannerActivity implements BarcodeDataDelegate, RFIDDataDelegate {

    private ImageView btnBack;
    private EditText etSearchItem;
    private RecyclerView rvTags;
    private TagAdapter adapter;
    private List<TagModel> allItemList;
    private List<TagModel> filteredList;

    // Integrasi SDK Denso
    private CommScanner mCommScanner;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_item);

        btnBack = findViewById(R.id.btnBack);
        etSearchItem = findViewById(R.id.searchItem);
        rvTags = findViewById(R.id.rvTags);

        allItemList = new ArrayList<>();
        filteredList = new ArrayList<>();

        allItemList.add(new TagModel("EPC001", "TAG-001", "ITM-001", "Kemeja Anti Kusut", "DO-001", 0));
        allItemList.add(new TagModel("EPC002", "TAG-002", "ITM-002", "Vans Japan Edition", "DO-001", 0));
        allItemList.add(new TagModel("EPC003", "TAG-003", "ITM-003", "Trucker Hat Custom", "DO-002", 0));
        allItemList.add(new TagModel("EPC004", "TAG-004", "ITM-004", "RFID Tag Sample", "DO-002", 0));
        filteredList.addAll(allItemList);

        adapter = new TagAdapter(filteredList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        // Setup Scanner pas awal activity dibuat
        setupScanner();

        // Cek koneksi di awal sebagai peringatan halus
        if (!isNetworkConnected()) {
            showSagaFeedback("Offline Mode: Search uses only local data.", false);
        }

        // Focus ke search bar otomatis biar operator gak usah ngeklik box-nya
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
            // Default ke false karena reader RFID fisik belum terpasang
            intent.putExtra("IS_RFID_MODE", false);
            startActivity(intent);
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void filter(String text) {
        filteredList.clear();
        String query = text.toLowerCase().trim();

        for (TagModel item : allItemList) {
            if (item.getProductName().toLowerCase().contains(query) ||
                    item.getEpcTag().toLowerCase().contains(query)) {
                filteredList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    // ==========================================
    // SDK DENSO IMPLEMENTATION
    // ==========================================

    private void setupScanner() {
        // Ambil instance scanner (Sesuaikan dengan cara lu naruh CommScanner di project)
        // mCommScanner = MyApplication.getCommScanner();

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
        // Jika scan RFID, masukkan EPC ke kolom search
        List<RFIDData> dataList = event.getRFIDData();
        for (RFIDData data : dataList) {
            String epc = bytesToHexString(data.getUII());
            handler.post(() -> {
                etSearchItem.setText(epc);
                etSearchItem.setSelection(etSearchItem.getText().length()); // Pindah kursor ke akhir
                showSagaFeedback("RFID Scanned: " + epc, true);
            });
        }
    }

    @Override
    public void onBarcodeDataReceived(CommScanner scanner, BarcodeDataReceivedEvent event) {
        // Jika scan Barcode, masukkan kode ke kolom search
        List<BarcodeData> dataList = event.getBarcodeData();
        if (!dataList.isEmpty()) {
            String barcode = new String(dataList.get(0).getData());
            handler.post(() -> {
                etSearchItem.setText(barcode);
                etSearchItem.setSelection(etSearchItem.getText().length());
                showSagaFeedback("Barcode Scanned: " + barcode, true);
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
        // Pasang ulang delegate saat aplikasi kembali ke foreground
        setupScanner();

        // Pastikan kursor tetap standby di kolom pencarian
        if (etSearchItem != null) {
            etSearchItem.postDelayed(() -> etSearchItem.requestFocus(), 200);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Lepas delegate agar tidak bentrok dengan activity lain
        if (mCommScanner != null) {
            try {
                mCommScanner.getRFIDScanner().setDataDelegate(null);
                mCommScanner.getBarcodeScanner().setDataDelegate(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}