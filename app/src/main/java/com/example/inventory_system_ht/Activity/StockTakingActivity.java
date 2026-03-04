package com.example.inventory_system_ht.Activity;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.inventory_system_ht.R;

public class StockTakingActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Switch switchRfid;
    private CardView cardlistTag, btnRefresh;
    private Button btnSave;
    private EditText resultScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_taking_adjustment);

        btnBack = findViewById(R.id.btnBack);
        switchRfid = findViewById(R.id.switchRfid);
        cardlistTag = findViewById(R.id.cardlistTag);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnSave = findViewById(R.id.btnSave);
        resultScan = findViewById(R.id.resultScan);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        switchRfid.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Toast.makeText(StockTakingActivity.this, "RFID: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(StockTakingActivity.this, "Data di-refresh", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(StockTakingActivity.this, "Data Stock Taking disimpan", Toast.LENGTH_SHORT).show();
            }
        });

        resultScan.requestFocus();
        resultScan.setShowSoftInputOnFocus(false);
        resultScan.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {

                    String rfidData = resultScan.getText().toString().trim();
                    if (!rfidData.isEmpty()) {
                        Toast.makeText(StockTakingActivity.this, "Scan: " + rfidData, Toast.LENGTH_SHORT).show();
                        resultScan.setText("");
                    }
                    resultScan.requestFocus();
                    return true;
                }
                return false;
            }
        });

        cardlistTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAdjustmentDialog();
            }
        });
    }

    private void showAdjustmentDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        dialog.setContentView(R.layout.dialog_adj);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageButton btnFaq = dialog.findViewById(R.id.btnFaq);
        Button btnRemove = dialog.findViewById(R.id.btnRemove);
        Button btnAddManual = dialog.findViewById(R.id.btnAddManual);

        btnFaq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFaqDialog();
            }
        });

        btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(StockTakingActivity.this, "Opsi Remove dipilih", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        btnAddManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(StockTakingActivity.this, "Opsi Add Manual dipilih", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showFaqDialog() {
        Dialog faqDialog = new Dialog(this);
        faqDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        faqDialog.setContentView(R.layout.dialog_faq);

        if (faqDialog.getWindow() != null) {
            faqDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            faqDialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        faqDialog.show();
    }
}