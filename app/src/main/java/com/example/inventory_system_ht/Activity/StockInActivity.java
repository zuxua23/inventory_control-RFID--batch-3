package com.example.inventory_system_ht.Activity;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventory_system_ht.Adapter.ItemAdapter;
import com.example.inventory_system_ht.Models.ItemModel;
import com.example.inventory_system_ht.R;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class StockInActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Button btnClear, btnSave;
    private Switch switchRfid;
    private EditText resultScan;
    private TextView tvScanned;
    private RecyclerView rvTags;

    // Variabel Array & Adapter
    private ItemAdapter adapter;
    private List<ItemModel> scannedItemsList;
    private int scanCount = 0;

    private ToneGenerator toneGen;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_in);

        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }

        btnBack = findViewById(R.id.btnBack);
        btnClear = findViewById(R.id.btnClear);
        btnSave = findViewById(R.id.btnSave);
        switchRfid = findViewById(R.id.switchRfid);
        resultScan = findViewById(R.id.resultScan);
        tvScanned = findViewById(R.id.tvScanned);
        rvTags = findViewById(R.id.rvTags);

        // Setup RecyclerView
        scannedItemsList = new ArrayList<>();
        adapter = new ItemAdapter(scannedItemsList);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        resultScan.setShowSoftInputOnFocus(false);
        resultScan.postDelayed(() -> resultScan.requestFocus(), 100);

        resultScan.addTextChangedListener(new android.text.TextWatcher() {
            private final long DELAY = 500; // Tunggu setengah detik setelah scan
            private Runnable searchRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String hasilScan = s.toString().trim();

                // Kalau kosong, cuekin aja
                if (hasilScan.isEmpty()) return;

                // Bikin timer baru: Kalau udah 500ms diam (scan selesai), hajar datanya!
                searchRunnable = () -> {
                    prosesValidasiLokal(hasilScan);

                    // Bersihin text biar siap buat scan berikutnya, tanpa bikin IMM bawel
                    resultScan.removeTextChangedListener(this); // Stop nyimak bentar biar gak infinite loop
                    resultScan.setText("");
                    resultScan.addTextChangedListener(this); // Mulai nyimak lagi

                    resultScan.postDelayed(() -> resultScan.requestFocus(), 50);
                };
                handler.postDelayed(searchRunnable, DELAY);
            }
        });

        // Tombol Back
        btnBack.setOnClickListener(v -> finish());

        // Switch RFID / Barcode
        switchRfid.setOnCheckedChangeListener((buttonView, isChecked) -> {
            View rootView = findViewById(android.R.id.content);
            String msg = isChecked ? "Mode RFID: ON" : "Mode RFID: OFF";
            Snackbar.make(rootView, msg, 1000).show();
            if(isChecked) resultScan.requestFocus();
        });

        // Tombol Clear
        btnClear.setOnClickListener(v -> {
            scannedItemsList.clear();
            adapter.notifyDataSetChanged();
            scanCount = 0;
            tvScanned.setText("Scanned: 0");
            resultScan.requestFocus();
            Toast.makeText(this, "List dibersihkan", Toast.LENGTH_SHORT).show();
        });

        // Tombol Save
        btnSave.setOnClickListener(v -> {
            if (scannedItemsList.isEmpty()) {
                Toast.makeText(this, "Belum ada barang!", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Data " + scanCount + " item berhasil disimpan (Lokal)!", Toast.LENGTH_LONG).show();
        });
    }

    // FUNGSI PENGGANTI API (Simpan ke Array)
    private void prosesValidasiLokal(String scanData) {
        handler.removeCallbacksAndMessages(null);

        boolean isRfidMode = switchRfid.isChecked();
        ItemModel foundItem = lookupDummyData(scanData, isRfidMode);

        // Kalau barang gak ada di database dummy
        if (foundItem == null) {
            playBeep(false); // Bunyi tetoot
            String errorMsg = isRfidMode ? "Tag RFID" : "Barcode";
            Snackbar.make(findViewById(android.R.id.content), errorMsg + " " + scanData + " gak dikenali!", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Kalau barang ada, cek apakah udah ada di list (buat update Qty)
        boolean isExist = false;
        for (int i = 0; i < scannedItemsList.size(); i++) {
            if ((isRfidMode && scannedItemsList.get(i).getEpcTag().equals(scanData)) ||
                    (!isRfidMode && scannedItemsList.get(i).getItemId().equals(scanData))) {

                int currentQty = scannedItemsList.get(i).getQty();
                scannedItemsList.get(i).setQty(currentQty + 1);
                adapter.notifyItemChanged(i);
                isExist = true;
                break;
            }
        }

        // Kalau barang belum ada di list, tambah baris baru
        if (!isExist) {
            scannedItemsList.add(new ItemModel(foundItem.getEpcTag(), foundItem.getItemId(), foundItem.getItemName(), 1));
            adapter.notifyItemInserted(scannedItemsList.size() - 1);
            rvTags.scrollToPosition(scannedItemsList.size() - 1); // Auto scroll ke bawah
        }

        // Update UI Sukses
        scanCount++;
        tvScanned.setText("Scanned: " + scanCount);
        playBeep(true); // Bunyi Tiit
    }

    // FUNGSI DUMMY DATABASE (Gak Perlu API)
    private ItemModel lookupDummyData(String scanData, boolean isRfid) {
        if (isRfid) {
            if (scanData.equalsIgnoreCase("112233")) return new ItemModel("112233", "ITM001", "Kemeja Anti Kusut", 1);
            if (scanData.equalsIgnoreCase("445566")) return new ItemModel("445566", "ITM002", "Vans Japan Edition", 1);
            if (scanData.equalsIgnoreCase("778899")) return new ItemModel("778899", "ITM003", "Trucker Hat Custom", 1);
        } else {
            if (scanData.equalsIgnoreCase("ITM001")) return new ItemModel("-", "ITM001", "Kemeja Anti Kusut", 1);
            if (scanData.equalsIgnoreCase("ITM002")) return new ItemModel("-", "ITM002", "Vans Japan Edition", 1);
            if (scanData.equalsIgnoreCase("ITM003")) return new ItemModel("-", "ITM003", "Trucker Hat Custom", 1);
        }
        return null;
    }

    // FUNGSI BUNYI BEEP
    private void playBeep(boolean isSuccess) {
        if (toneGen == null) return;
        try {
            if (isSuccess) {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150); // Bunyi sukses
            } else {
                toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 300); // Bunyi error
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (resultScan != null) {
            resultScan.postDelayed(() -> {
                resultScan.requestFocus();
                // Sembunyiin keyboard otomatis
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(resultScan.getWindowToken(), 0);
                }
            }, 200);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGen != null) {
            toneGen.release(); // Matiin memori ToneGenerator
            toneGen = null;
        }
    }
}