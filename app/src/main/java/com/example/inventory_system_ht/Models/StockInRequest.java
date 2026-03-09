package com.example.inventory_system_ht.Models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StockInRequest {
    @SerializedName("scannerType")
    private String scannerType;

    @SerializedName("scannedCodes")
    private List<String> scannedCodes;

    public StockInRequest(String scannerType, List<String> scannedCodes) {
        this.scannerType = scannerType;
        this.scannedCodes = scannedCodes;
    }

    public String getScannerType() { return scannerType; }
    public List<String> getScannedCodes() { return scannedCodes; }
}