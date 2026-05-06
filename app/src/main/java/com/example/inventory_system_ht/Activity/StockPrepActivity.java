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
import com.example.inventory_system_ht.Helper.AppDatabase;
import com.example.inventory_system_ht.Helper.AppDao;
import com.example.inventory_system_ht.Helper.PrefManager;
import com.example.inventory_system_ht.Helper.RfidBulkHelper;
import com.example.inventory_system_ht.Helper.ScannerManager;
import com.example.inventory_system_ht.Models.DOModels;
import com.example.inventory_system_ht.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class StockPrepActivity extends BaseScannerActivity implements BarcodeDataDelegate {

    private RecyclerView rvTags;
    private DOAdapter adapter;
    private List<DOModels.DOModel> doList;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AppDao appDao;

    // ── Scanner via ScannerManager ────────────────────────────────
    @Override
    protected CommScanner getScannerInstance() {
        return ScannerManager.getInstance().getScanner();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_prep_delivery_order);

        appDao = AppDatabase.getDatabase(this).appDao();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvTags = findViewById(R.id.rvTags);
        doList = new ArrayList<>();

        adapter = new DOAdapter(doList, this::openDetailDO);
        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        loadDataFromLocalDB();

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
                    showWarning("No data available, please refresh");
                }
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void fetchDOFromServer() {
        if (!isNetworkConnected()) {
            showWarning("No internet connection, showing cached data");
            playScanFeedback(2);
            loadDataFromLocalDB();
            return;
        }

        showLoading();
        String token = "Bearer " + new PrefManager(this).getToken();

        ApiClient.getClient(this).create(ApiService.class)
                .getDo(token)
                .enqueue(new retrofit2.Callback<List<DOModels.DOModel>>() {
                    @Override
                    public void onResponse(Call<List<DOModels.DOModel>> call,
                                           retrofit2.Response<List<DOModels.DOModel>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<DOModels.DOModel> remoteDOs = response.body();
                            new Thread(() -> {
                                try { appDao.deleteAllDO(); } catch (Exception ignored) {}
                                appDao.insertDOList(remoteDOs);
                                runOnUiThread(() -> {
                                    hideLoading();
                                    showSuccess("DO list updated");
                                    playScanFeedback(0);
                                    loadDataFromLocalDB();
                                });
                            }).start();
                        } else {
                            hideLoading();
                            handleApiError(response);
                            playScanFeedback(2);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<DOModels.DOModel>> call, Throwable t) {
                        hideLoading();
                        handleFailure(t);
                        playScanFeedback(2);
                    }
                });
    }

    private void openDetailDO(DOModels.DOModel item) {
        Intent intent = new Intent(this, StockPrepProductActivity.class);
        intent.putExtra("DO_ID", item.getDoId());
        intent.putExtra("NO_DO", item.getDoNo());
        intent.putExtra("DATE_DO", item.getCreatedAt());
        startActivity(intent);
    }

    // ── Barcode Callback ──────────────────────────────────────────
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
                    playScanFeedback(0);
                    showSuccess("DO Found: " + scannedDo);
                    openDetailDO(match);
                } else {
                    playScanFeedback(2);
                    showError("DO " + scannedDo + " not found in list");
                }
            });
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        CommScanner scanner = getScannerInstance();
        updateReaderBattery(findViewById(R.id.ivReaderBattery));

        // Activity ini hanya pakai barcode (scan nomor DO)
        if (scanner != null) RfidBulkHelper.openBarcode(scanner, this);

        loadDataFromLocalDB();

        if (getHTBatteryLevel() <= 15) {
            showWarning("HT Battery at " + getHTBatteryLevel() + "%, please charge now!");
            playScanFeedback(2);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        RfidBulkHelper.closeBarcode(getScannerInstance());
    }
}