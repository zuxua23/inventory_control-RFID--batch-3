package com.example.inventory_system_ht.Activity;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

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

import java.util.ArrayList;
import java.util.List;

// 👇 UPGRADE: Extends ke BaseScannerActivity biar fitur Offline & Saga Feedback aktif 👇
public class TagRegisActivity extends BaseScannerActivity implements BarcodeDataDelegate, RFIDDataDelegate {

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

        btnBack = findViewById(R.id.btnBack);
        resultScan = findViewById(R.id.resultScan);
        tvScanned = findViewById(R.id.tvScanned);
        switchRfid = findViewById(R.id.switchRfid);
        btnRefresh = findViewById(R.id.btnRefresh);
        rvTags = findViewById(R.id.rvTags);

        // WAJIB: Default switch dimatikan biar mulai dari mode Barcode
        switchRfid.setChecked(false);

        registeredTagList = new ArrayList<>();
        adapter = new TagAdapter(registeredTagList);

        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnRefresh.setOnClickListener(v -> {
            scanCount = 0;
            tvScanned.setText("Scanned: " + scanCount);
            registeredTagList.clear();
            adapter.notifyDataSetChanged();
            showSagaFeedback("Session data is refreshed, bro!", true);
            resultScan.requestFocus();
        });

        adapter.setOnItemClickListener(selectedTag -> {
            showRegisterDialog(selectedTag);
        });

        setupScanner();

        // Cek koneksi di awal
        if (!isNetworkConnected()) {
            showSagaFeedback("Offline Mode: Data will be saved locally.", false);
        }

        // TextWatcher buat nangkep hasil scan Barcode (Keyboard Wedge)
        resultScan.setShowSoftInputOnFocus(false);
        resultScan.postDelayed(() -> resultScan.requestFocus(), 100);

        resultScan.addTextChangedListener(new android.text.TextWatcher() {
            private final long DELAY = 500;
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
                if (hasilScan.isEmpty()) return;

                searchRunnable = () -> {
                    processScannedData(hasilScan);

                    resultScan.removeTextChangedListener(this);
                    resultScan.setText("");
                    resultScan.addTextChangedListener(this);
                    resultScan.postDelayed(() -> resultScan.requestFocus(), 50);
                };
                handler.postDelayed(searchRunnable, DELAY);
            }
        });

        // LOGIC SMART SWITCH
        CompoundButton.OnCheckedChangeListener switchListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    boolean isRfidReady = (mCommScanner != null && mCommScanner.getRFIDScanner() != null);

                    if (!isRfidReady) {
                        showSagaFeedback("The RFID reader is not installed on the HT, bro!", false);

                        switchRfid.setOnCheckedChangeListener(null);
                        switchRfid.setChecked(false);
                        switchRfid.setOnCheckedChangeListener(this);
                        return;
                    }
                }

                String msg = isChecked ? "RFID Mode: ON" : "RFID Mode: OFF";
                showSagaFeedback(msg, true);

                if (isChecked && resultScan != null) {
                    resultScan.requestFocus();
                }
            }
        };
        switchRfid.setOnCheckedChangeListener(switchListener);
    }

    private void showRegisterDialog(TagModel scannedTag) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_regist);

        TextView tvTitle = dialog.findViewById(R.id.tvTitle);
        tvTitle.setText("Register Tag: " + scannedTag.getEpcTag());

        Button btnYes = dialog.findViewById(R.id.btnYes);
        btnYes.setOnClickListener(v -> {
            saveToDatabase(scannedTag.getEpcTag(), "SELECTED_ITEM_ID");
            dialog.dismiss();
            showSagaFeedback("Tag Successfully Registered!", true);
            resultScan.requestFocus();
        });

        dialog.show();
    }

    private void setupScanner() {
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
        if (!switchRfid.isChecked()) return;

        List<RFIDData> dataList = event.getRFIDData();
        for (RFIDData data : dataList) {
            String epc = bytesToHexString(data.getUII());
            handler.post(() -> processScannedData(epc));
        }
    }

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
        } else {
            showSagaFeedback("This item is already on the list, bro!", false);
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

        btnNo.setOnClickListener(v -> {
            dialog.dismiss();
            resultScan.requestFocus();
        });

        btnYes.setOnClickListener(v -> {
            scanCount++;
            tvScanned.setText("Scanned: " + scanCount);

            // 👇 FIX: DUMMY DATA MENGGUNAKAN 5 ARGUMEN 👇
            // Karena ini registrasi awal, doIdRef bisa diisi "N/A" atau kosong
            registeredTagList.add(new TagModel(scannedData, "ITM-NEW", "New Product Registered", "N/A", 0));

            adapter.notifyItemInserted(registeredTagList.size() - 1);
            rvTags.scrollToPosition(registeredTagList.size() - 1);
            dialog.dismiss();
            showSagaFeedback("Registered successfully!", true);
            resultScan.requestFocus();
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
    protected void onResume() {
        super.onResume();
        setupScanner();

        if (resultScan != null) {
            resultScan.postDelayed(() -> resultScan.requestFocus(), 200);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCommScanner != null) {
            try {
                mCommScanner.getRFIDScanner().setDataDelegate(null);
                mCommScanner.getBarcodeScanner().setDataDelegate(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveToDatabase(String epcTag, String itemId) {
        // Nanti logic API tembak sini
    }
}