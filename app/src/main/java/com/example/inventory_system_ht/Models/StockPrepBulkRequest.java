package com.example.inventory_system_ht.Models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StockPrepBulkRequest {

    @SerializedName("doId")
    private String doId;

    @SerializedName("scannerType")
    private String scannerType;

    @SerializedName("scannedCodes")
    private List<String> scannedCodes;

    public StockPrepBulkRequest(String doId, List<String> scannedCodes, String scannerType) {
        this.doId = doId;
        this.scannedCodes = scannedCodes;
        this.scannerType = scannerType;
    }

    // Getter & Setter (Opsional kalau cuma buat dikirim via Retrofit)
    public String getDoId() { return doId; }
    public String getScannerType() { return scannerType; }
    public List<String> getScannedCodes() { return scannedCodes; }
}