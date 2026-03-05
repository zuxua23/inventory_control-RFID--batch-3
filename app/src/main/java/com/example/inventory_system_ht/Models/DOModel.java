package com.example.inventory_system_ht.Models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Nama tabel disamain sama PDM lu
@Entity(tableName = "tb_DO")
public class DOModel {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "do_id")
    private String doId;

    @ColumnInfo(name = "do_number")
    private String doNo;

    @ColumnInfo(name = "status")
    private String status;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    // Constructor
    public DOModel(@NonNull String doId, String doNo, String status, String createdAt) {
        this.doId = doId;
        this.doNo = doNo;
        this.status = status;
        this.createdAt = createdAt;
    }

    // --- GETTER ---
    @NonNull
    public String getDoId() { return doId; }
    public String getDoNo() { return doNo; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}