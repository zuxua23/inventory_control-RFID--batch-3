package com.example.inventory_system_ht.Activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventory_system_ht.Adapter.DOAdapter;
import com.example.inventory_system_ht.Models.DOModel;
import com.example.inventory_system_ht.R;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class StockPrepActivity extends AppCompatActivity {
    private RecyclerView rvTags;
    private DOAdapter adapter;
    private List<DOModel> doList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_prep_delivery_order);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvTags = findViewById(R.id.rvTags);
        doList = new ArrayList<>();

        doList.add(new DOModel("1", "DO-2026-001", "Customer A - Jakarta", "02-03-2026"));
        doList.add(new DOModel("2", "DO-2026-002", "Customer B - Bekasi", "03-03-2026"));

        adapter = new DOAdapter(doList, doItem -> {
            Intent intent = new Intent(this, StockPrepProductActivity.class);

            intent.putExtra("NO_DO", doItem.getDoNo());
            intent.putExtra("DATE_DO", doItem.getDoDate());

            startActivity(intent);
        });

        rvTags.setLayoutManager(new LinearLayoutManager(this));
        rvTags.setAdapter(adapter);

        findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            Snackbar.make(v, "Refreshing DO List...", Snackbar.LENGTH_SHORT).show();
        });
    }
}