package com.example.inventory_system_ht.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// 👇 SDK DENSO 👇
import com.densowave.scannersdk.Barcode.BarcodeData;
import com.densowave.scannersdk.Barcode.BarcodeDataReceivedEvent;
import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.BarcodeDataDelegate;

import com.example.inventory_system_ht.Adapter.DOAdapter;
import com.example.inventory_system_ht.Models.DOModel;
// 👇 IMPORT ROOM DATABASE LU 👇
import com.example.inventory_system_ht.Helper.AppDatabase;
import com.example.inventory_system_ht.Helper.AppDao;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.List;

/**
 * StockPrepActivity: Menampilkan daftar Delivery Order (DO).
 * Mendukung scan barcode surat jalan untuk langsung membuka detail DO.
 */
public class StockPrepActivity extends BaseScannerActivity implements BarcodeDataDelegate {

    private RecyclerView rvTags;
    private DOAdapter adapter;
    private List<DOModel> doList;
    private CommScanner mCommScanner;
    private Handler handler = new Handler(Looper.getMainLooper());

    // Variabel Mesin Database
    private AppDatabase appDb;
    private AppDao appDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_prep_delivery_order);

        // 1. Nyalain Mesin Database Lokal
        appDb = AppDatabase.getDatabase(this);
        appDao = appDb.appDao();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvTags = findViewById(R.id.rvTags);
        doList = new ArrayList<>();

        adapter = new DOAdapter(doList, doItem -> openDetailDO(doItem.getDoNo(), doItem.getCreatedAt()));
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        setupScanner();

        if (!isNetworkConnected()) {
            showSagaFeedback("Offline Mode: Membaca data DO dari penyimpanan lokal HT.", false);
        }

        // 2. Langsung tarik data dari SQLite pas halaman dibuka
        loadDataFromLocalDB();

        // 3. Tombol Refresh disulap jadi "Penyuntik Data" ke Database
        findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            showSagaFeedback("Menyuntikkan data DO ke Database Lokal...", true);
            seedDummyData();
            loadDataFromLocalDB(); // Habis disuntik, langsung baca ulang biar muncul di layar
        });
    }

    // Fungsi narik data dari SQLite (Gak pake internet sama sekali)
    private void loadDataFromLocalDB() {
        List<DOModel> dataDariDB = appDao.getAllDO();

        doList.clear(); // Bersihin list layar

        if (dataDariDB != null && !dataDariDB.isEmpty()) {
            doList.addAll(dataDariDB); // Masukin data dari DB ke layar
        } else {
            showSagaFeedback("Database lokal kosong, klik refresh buat nyuntik data dummy.", false);
        }

        adapter.notifyDataSetChanged(); // Refresh adapter UI
    }

    // Fungsi masukin data ke SQLite (Simulasi narik API)
    private void seedDummyData() {
        List<DOModel> dummyList = new ArrayList<>();
        dummyList.add(new DOModel("1", "DO-2026-001", "PENDING", "2026-03-02"));
        dummyList.add(new DOModel("2", "DO-2026-002", "IN PROGRESS", "2026-03-03"));
        dummyList.add(new DOModel("3", "DO-2026-003", "PENDING", "2026-03-05"));

        // Insert ke Room (Aman kalau di-klik berkali-kali karena kita pake REPLACE di DAO)
        appDao.insertDOList(dummyList);
    }

    private void openDetailDO(String noDo, String dateDo) {
        Intent intent = new Intent(this, StockPrepProductActivity.class);
        intent.putExtra("NO_DO", noDo);
        intent.putExtra("DATE_DO", dateDo);
        startActivity(intent);
    }

    // ==========================================
    // SDK INTEGRATION (AUTO-OPEN BY SCAN)
    // ==========================================

    private void setupScanner() {
        if (mCommScanner != null) {
            try {
                mCommScanner.getBarcodeScanner().setDataDelegate(this);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @Override
    public void onBarcodeDataReceived(CommScanner scanner, BarcodeDataReceivedEvent event) {
        List<BarcodeData> dataList = event.getBarcodeData();
        if (!dataList.isEmpty()) {
            String scannedDo = new String(dataList.get(0).getData());

            handler.post(() -> {
                boolean found = false;
                for (DOModel doItem : doList) {
                    if (doItem.getDoNo().equalsIgnoreCase(scannedDo)) {
                        showSagaFeedback("DO Ketemu: Membuka detail " + scannedDo, true);
                        openDetailDO(doItem.getDoNo(), doItem.getCreatedAt());
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    showSagaFeedback("DO " + scannedDo + " gak ada di list bre!", false);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupScanner();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCommScanner != null) {
            try {
                mCommScanner.getBarcodeScanner().setDataDelegate(null);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}