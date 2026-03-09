package com.example.inventory_system_ht.Models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@Entity(tableName = "tb_Tag_Local")
public class TagModel implements Serializable {

    @PrimaryKey
    @NonNull
    @SerializedName("epcTag")
    @ColumnInfo(name = "epc_tag")
    private String epcTag;

    // 👇 INI YANG BIKIN ERROR TADI: ID UNIK FISIK TAG 👇
    @SerializedName("id") // Sesuaiin sama nama properti ID di tb_Tag C# lu (biasanya "Id")
    @ColumnInfo(name = "tag_id")
    private String tagId;

    @SerializedName("itemId")
    @ColumnInfo(name = "itm_id")
    private String itmId;

    @SerializedName("itemName") // Opsional buat mapping API
    @ColumnInfo(name = "product_name")
    private String productName;

    @ColumnInfo(name = "do_id_ref")
    private String doIdRef;

    @ColumnInfo(name = "sync_status")
    private int syncStatus;

    // Constructor Update (Sekarang minta 6 isian)
    public TagModel(@NonNull String epcTag, String tagId, String itmId, String productName, String doIdRef, int syncStatus) {
        this.epcTag = epcTag;
        this.tagId = tagId;
        this.itmId = itmId;
        this.productName = productName;
        this.doIdRef = doIdRef;
        this.syncStatus = syncStatus;
    }

    // --- GETTER ---
    @NonNull public String getEpcTag() { return epcTag; }

    // 👇 FUNGSI PENYELAMAT NYA 👇
    public String getTagId() { return tagId; }

    public String getItmId() { return itmId; }
    public String getProductName() { return productName; }
    public String getDoIdRef() { return doIdRef; }
    public int getSyncStatus() { return syncStatus; }

    public void setSyncStatus(int syncStatus) { this.syncStatus = syncStatus; }
}