package com.example.inventory_system_ht.Activity;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity; // Pake ini biar findViewById kenal
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class TagRegisActivity extends AppCompatActivity implements BarcodeDataDelegate, RFIDDataDelegate {

    private CommScanner mCommScanner;
    private ImageView btnBack;
    private EditText resultScan;
    private TextView tvScanned;
    private Switch switchRfid;
    private CardView btnRefresh;
    private RecyclerView rvTags;
    private TagAdapter adapter;
    private List<TagModel> registeredTagList;
    private int scanCount = 0;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regist);

        // Inisialisasi UI
        btnBack = findViewById(R.id.btnBack);
        resultScan = findViewById(R.id.resultScan);
        tvScanned = findViewById(R.id.tvScanned);
        switchRfid = findViewById(R.id.switchRfid);
        btnRefresh = findViewById(R.id.btnRefresh);
        rvTags = findViewById(R.id.rvTags);

        registeredTagList = new ArrayList<>();
        adapter = new TagAdapter(registeredTagList);

        // Sekarang "this" udah valid sebagai Context karena pake AppCompatActivity
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnRefresh.setOnClickListener(v -> {
            scanCount = 0;
            tvScanned.setText("Scanned: " + scanCount);
            registeredTagList.clear();
            adapter.notifyDataSetChanged();
            Snackbar.make(v, "Data session di-refresh bre!", Snackbar.LENGTH_SHORT).show();
        });

        adapter.setOnItemClickListener(selectedTag -> {
            // Buka dialog registrasi untuk Tag yang diklik
            showRegisterDialog(selectedTag);
        });
        // Panggil fungsi setup scanner
        setupScanner();

    }

    private void showRegisterDialog(TagModel scannedTag) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_regist); // Pake layout dialog lu

        TextView tvTitle = dialog.findViewById(R.id.tvTitle); // Sesuaikan ID di XML lu
        tvTitle.setText("Register Tag: " + scannedTag.getEpcTag());

        // --- TAMBAHAN PENTING: Pilihan Barang ---
        // Di sini lu butuh Spinner atau AutoCompleteTextView buat milih barang
        // Lu bisa ambil data dari allItemList (tb_item)

        Button btnYes = dialog.findViewById(R.id.btnYes);
        btnYes.setOnClickListener(v -> {
            // Logika Registrasi (Kirim ke API / Database)
            // 1. Ambil itm_id dari pilihan user
            // 2. Hubungkan dengan epc_tag

            saveToDatabase(scannedTag.getEpcTag(), "SELECTED_ITEM_ID");

            dialog.dismiss();
            Snackbar.make(rvTags, "Tag Berhasil Terdaftar!", Snackbar.LENGTH_SHORT).show();
        });

        dialog.show();
    }
    private void setupScanner() {
        // Ambil instance scanner lu di sini (contoh pake Singleton/MainApp)
        // mCommScanner = MainApplication.getCommScanner();

        if (mCommScanner != null) {
            try {
                // Register delegate biar bisa dengerin hasil scan
                mCommScanner.getRFIDScanner().setDataDelegate(this);
                mCommScanner.getBarcodeScanner().setDataDelegate(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // --- LOGIC RFID ---
    @Override
    public void onRFIDDataReceived(CommScanner scanner, RFIDDataReceivedEvent event) {
        if (!switchRfid.isChecked()) return;

        List<RFIDData> dataList = event.getRFIDData();
        for (RFIDData data : dataList) {
            String epc = bytesToHexString(data.getUII());
            handler.post(() -> processScannedData(epc));
        }
    }

    // --- LOGIC BARCODE ---
    @Override
    public void onBarcodeDataReceived(CommScanner scanner, BarcodeDataReceivedEvent event) {
        if (switchRfid.isChecked()) return;

        List<BarcodeData> dataList = event.getBarcodeData();
        if (!dataList.isEmpty()) {
            String barcode = new String(dataList.get(0).getData());
            handler.post(() -> processScannedData(barcode));
        }
    }

    private void processScannedData(String scannedData) {
        boolean isAlreadyInList = false;
        for (TagModel t : registeredTagList) {
            if (t.getEpcTag().equals(scannedData)) {
                isAlreadyInList = true;
                break;
            }
        }

        if (!isAlreadyInList) {
            showConfirmDialog(scannedData);
        }
    }

    private void showConfirmDialog(String scannedData) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_regist);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Button btnNo = dialog.findViewById(R.id.btnNo);
        Button btnYes = dialog.findViewById(R.id.btnYes);

        btnNo.setOnClickListener(v -> dialog.dismiss());

        btnYes.setOnClickListener(v -> {
            scanCount++;
            tvScanned.setText("Scanned: " + scanCount);
            registeredTagList.add(new TagModel(scannedData, "New Product Registered"));
            adapter.notifyItemInserted(registeredTagList.size() - 1);
            rvTags.scrollToPosition(registeredTagList.size() - 1);
            dialog.dismiss();
            Snackbar.make(findViewById(android.R.id.content), "Sukses terdaftar!", Snackbar.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Tutup scanner dengan aman
        if (mCommScanner != null) {
            try {
                mCommScanner.getRFIDScanner().close();
                mCommScanner.getBarcodeScanner().closeReader();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
    private void saveToDatabase(String epcTag, String itemId) {
        // Logika untuk menyimpan ke database (SQLite/Room/Retrofit) taruh di sini bre
    }
}