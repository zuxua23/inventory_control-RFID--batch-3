package com.example.inventory_system_ht.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.densowave.scannersdk.Barcode.BarcodeData;
import com.densowave.scannersdk.Barcode.BarcodeDataReceivedEvent;
import com.densowave.scannersdk.Common.CommScanner;
import com.densowave.scannersdk.Listener.BarcodeDataDelegate;
import com.example.inventory_system_ht.Adapter.DOAdapter;
import com.example.inventory_system_ht.Helper.ApiClient;
import com.example.inventory_system_ht.Helper.ApiService;
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.Models.DOModel;
import com.example.inventory_system_ht.Helper.AppDatabase;
import com.example.inventory_system_ht.Helper.AppDao;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class StockPrepActivity extends BaseScannerActivity implements BarcodeDataDelegate {

    private RecyclerView rvTags;
    private DOAdapter adapter;
    private List<DOModel> doList;
    private CommScanner mCommScanner;
    private Handler handler = new Handler(Looper.getMainLooper());
    private AppDao appDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_prep_delivery_order);

        appDao = AppDatabase.getDatabase(this).appDao();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvTags = findViewById(R.id.rvTags);
        doList = new ArrayList<>();

        // 1. UPDATE: Klik list sekarang lempar object DOModel biar dapet ID-nya
        adapter = new DOAdapter(doList, this::openDetailDO);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        setupScanner();
        loadDataFromLocalDB();

        // 2. Refresh sekarang nembak API asli, bukan cuma dummy
        findViewById(R.id.btnRefresh).setOnClickListener(v -> fetchDOFromServer());
    }

    private void loadDataFromLocalDB() {
        new Thread(() -> {
            List<DOModel> dataDariDB = appDao.getAllDO();
            runOnUiThread(() -> {
                doList.clear();
                if (dataDariDB != null && !dataDariDB.isEmpty()) {
                    doList.addAll(dataDariDB);
                } else {
                    showSagaFeedback("DB Kosong, silakan Refresh bre!", false);
                }
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    // Fungsi tarik data asli dari BE SATO
    private void fetchDOFromServer() {
        if (!isNetworkConnected()) {
            showSagaFeedback("Offline! Baca data lokal aja.", false);
            loadDataFromLocalDB();
            return;
        }

        showSagaFeedback("Syncing DO List...", true);
        PrefManager pref = new PrefManager(this);
        String token = "Bearer " + pref.getToken();

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.getAllDO(token).enqueue(new retrofit2.Callback<List<DOModel>>() {
            @Override
            public void onResponse(Call<List<DOModel>> call, retrofit2.Response<List<DOModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<DOModel> remoteDOs = response.body();
                    new Thread(() -> {
                        appDao.insertDOList(remoteDOs);
                        runOnUiThread(() -> {
                            showSagaFeedback("DO List Updated!", true);
                            loadDataFromLocalDB();
                        });
                    }).start();
                } else {
                    showSagaFeedback("Server Error: " + response.code(), false);
                }
            }

            @Override
            public void onFailure(Call<List<DOModel>> call, Throwable t) {
                showSagaFeedback("Gagal konek server bre!", false);
            }
        });
    }

    // 3. UPDATE: Pindah halaman bawa GUID (DO_ID) buat verifikasi di BE
    private void openDetailDO(DOModel item) {
        Intent intent = new Intent(this, StockPrepProductActivity.class);
        intent.putExtra("DO_ID", item.getDoId()); // GUID (Penting buat API)
        intent.putExtra("NO_DO", item.getDoNo()); // Buat tampilan Header
        intent.putExtra("DATE_DO", item.getCreatedAt());
        startActivity(intent);
    }

    // --- SDK SCANNER ---
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
                DOModel match = null;
                for (DOModel doItem : doList) {
                    if (doItem.getDoNo().equalsIgnoreCase(scannedDo)) {
                        match = doItem;
                        break;
                    }
                }
                if (match != null) {
                    showSagaFeedback("DO Ketemu: " + scannedDo, true);
                    openDetailDO(match); // Langsung buka detail pake GUID
                } else {
                    showSagaFeedback("DO " + scannedDo + " gak ada di list!", false);
                }
            });
        }
    }

    @Override protected void onResume() { super.onResume(); setupScanner(); }
    @Override protected void onPause() {
        super.onPause();
        if (mCommScanner != null) {
            try { mCommScanner.getBarcodeScanner().setDataDelegate(null); } catch (Exception e) {}
        }
    }
}