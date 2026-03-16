package com.example.inventory_system_ht.Models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StockInRequest {
    @SerializedName("scannerType")
    private final String scannerType;

    @SerializedName("scannedCodes")
    private final List<String> scannedCodes;

    public StockInRequest(String scannerType, List<String> scannedCodes) {
        this.scannerType = scannerType;
        this.scannedCodes = scannedCodes;
    }

    public String getScannerType() { return scannerType; }
    public List<String> getScannedCodes() { return scannedCodes; }
}