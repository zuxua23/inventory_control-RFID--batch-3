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
import com.example.inventory_system_ht.Models.DOModels;
import com.example.inventory_system_ht.Helper.AppDatabase;
import com.example.inventory_system_ht.Helper.AppDao;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class StockPrepActivity extends BaseScannerActivity implements BarcodeDataDelegate {

    private RecyclerView rvTags;
    private DOAdapter adapter;
    private List<DOModels.DOModel> doList;
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
        showLoading();
        new Thread(() -> {
            List<DOModels.DOModel> dataDariDB = appDao.getAllDO();
            runOnUiThread(() -> {
                hideLoading();
                doList.clear();
                if (dataDariDB != null && !dataDariDB.isEmpty()) {
                    doList.addAll(dataDariDB);
                } else {
                    showSagaFeedback("DB is empty, please refresh it!", false);
                }
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void fetchDOFromServer() {
        if (!isNetworkConnected()) {
            showSagaFeedback("Offline! Read local data only.", false);
            playScanFeedback(2); // 👈 TIPE 2: Bunyi warning karena offline
            loadDataFromLocalDB();
            return;
        }

        showLoading();
        showSagaFeedback("Syncing DO List...", true);

        PrefManager pref = new PrefManager(this);
        String token = "Bearer " + pref.getToken();

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.getAllDO(token).enqueue(new retrofit2.Callback<List<DOModels.DOModel>>() {
            @Override
            public void onResponse(Call<List<DOModels.DOModel>> call, retrofit2.Response<List<DOModels.DOModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<DOModels.DOModel> remoteDOs = response.body();
                    new Thread(() -> {
                        appDao.insertDOList(remoteDOs);
                        runOnUiThread(() -> {
                            hideLoading();
                            showSagaFeedback("DO List Updated!", true);
                            playScanFeedback(0); // 👈 TIPE 0: BUNYI SUKSES REFRESH
                            loadDataFromLocalDB();
                        });
                    }).start();
                } else {
                    handleApiError(response.code());
                    playScanFeedback(2); // 👈 TIPE 2: BUNYI ERROR API + GETAR
                }
            }

            @Override
            public void onFailure(Call<List<DOModels.DOModel>> call, Throwable t) {
                handleFailure(t);
                playScanFeedback(2); // 👈 TIPE 2: BUNYI TIMEOUT + GETAR
            }
        });
    }

    private void openDetailDO(DOModels.DOModel item) {
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
                DOModels.DOModel match = null;
                for (DOModels.DOModel doItem : doList) {
                    if (doItem.getDoNo().equalsIgnoreCase(scannedDo)) {
                        match = doItem;
                        break;
                    }
                }
                if (match != null) {
                    playScanFeedback(0); // 👈 TIPE 0: Suara SUKSES (Nemu DO)
                    showSagaFeedback("DO Founded: " + scannedDo, true);
                    openDetailDO(match); // Langsung buka detail pake GUID
                } else {
                    playScanFeedback(2); // 👈 TIPE 2: Suara ERROR + GETAR (DO Gak Ada)
                    showSagaFeedback("DO " + scannedDo + " it's not on the list!", false);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupScanner();

        if (getHTBatteryLevel() <= 15) {
            showSagaFeedback("Baterai HT sisa " + getHTBatteryLevel() + "%, waktunya ngecas bre!", false);
            playScanFeedback(2); // Kasih bunyi error biar operator nyadar
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // MATIIN LISTENER SCANNER PAS KELUAR HALAMAN BIAR GAK BOCOR BATRE
        if (mCommScanner != null) {
            try {
                if (mCommScanner.getRFIDScanner() != null) {
                    mCommScanner.getRFIDScanner().setDataDelegate(null);
                }
                if (mCommScanner.getBarcodeScanner() != null) {
                    mCommScanner.getBarcodeScanner().setDataDelegate(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}