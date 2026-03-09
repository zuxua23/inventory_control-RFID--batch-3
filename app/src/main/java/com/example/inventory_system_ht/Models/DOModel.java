package com.example.inventory_system_ht.Models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "tb_DO")
public class DOModel {

    @PrimaryKey
    @NonNull
    @SerializedName("doId") // Sesuai C# PascalCase/camelCase
    @ColumnInfo(name = "do_id")
    private String doId;

    @SerializedName("doNumber") // Jembatan dari DoNumber ke doNo
    @ColumnInfo(name = "do_number")
    private String doNo;

    @SerializedName("status")
    @ColumnInfo(name = "status")
    private String status;

    @SerializedName("createdAt")
    @ColumnInfo(name = "created_at")
    private String createdAt;

    @SerializedName("scannerType") // Tambahin ini bre, penting buat logic RFID vs QR
    @ColumnInfo(name = "scanner_type")
    private String scannerType;

    // Constructor
    public DOModel(@NonNull String doId, String doNo, String status, String createdAt, String scannerType) {
        this.doId = doId;
        this.doNo = doNo;
        this.status = status;
        this.createdAt = createdAt;
        this.scannerType = scannerType;
    }

    // --- GETTER ---
    @NonNull public String getDoId() { return doId; }
    public String getDoNo() { return doNo; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getScannerType() { return scannerType; }
}