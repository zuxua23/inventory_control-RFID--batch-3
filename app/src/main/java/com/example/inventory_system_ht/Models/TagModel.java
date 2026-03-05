package com.example.inventory_system_ht.Models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "tb_Tag_Local")
public class TagModel implements Serializable {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "epc_tag")
    private String epcTag;

    @ColumnInfo(name = "itm_id")
    private String itmId;

    // 👇 GW BALIKIN BIAR UI ACTIVITY LU AMAN 👇
    @ColumnInfo(name = "product_name")
    private String productName;

    @ColumnInfo(name = "do_id_ref")
    private String doIdRef;

    @ColumnInfo(name = "sync_status")
    private int syncStatus;

    // Constructor Update
    public TagModel(@NonNull String epcTag, String itmId, String productName, String doIdRef, int syncStatus) {
        this.epcTag = epcTag;
        this.itmId = itmId;
        this.productName = productName;
        this.doIdRef = doIdRef;
        this.syncStatus = syncStatus;
    }

    // --- GETTER ---
    @NonNull
    public String getEpcTag() { return epcTag; }
    public String getItmId() { return itmId; }
    public String getProductName() { return productName; } // Getter UI balek lagi
    public String getDoIdRef() { return doIdRef; }
    public int getSyncStatus() { return syncStatus; }

    public void setSyncStatus(int syncStatus) { this.syncStatus = syncStatus; }
}