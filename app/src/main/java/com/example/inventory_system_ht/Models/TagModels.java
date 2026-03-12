package com.example.inventory_system_ht.Models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class TagModels {

    public static class TagInfoDto {
        @SerializedName("tagId") private String tagId;
        @SerializedName("epcTag") private String epcTag;
        @SerializedName("itemName") private String itemName;
        @SerializedName("itemId") private String itemId;
        @SerializedName("status") private String status;

        public TagInfoDto(String tagId, String epcTag, String itemName, String itemId, String status) {
            this.tagId = tagId;
            this.epcTag = epcTag;
            this.itemName = itemName;
            this.itemId = itemId;
            this.status = status;
        }

        public String getTagId() { return tagId; }
        public String getEpcTag() { return epcTag; }
        public String getItemName() { return itemName; }
        public String getItemId() { return itemId; }
        public String getStatus() { return status; }
    }

    @Entity(tableName = "tb_Tag_Local")
    public static class TagModel implements Serializable {

        @PrimaryKey
        @NonNull
        @SerializedName("epcTag")
        @ColumnInfo(name = "epc_tag")
        private String epcTag;

        @SerializedName("id")
        @ColumnInfo(name = "tag_id")
        private String tagId;

        @SerializedName("itemId")
        @ColumnInfo(name = "itm_id")
        private String itmId;

        @SerializedName("itemName")
        @ColumnInfo(name = "product_name")
        private String productName;

        @ColumnInfo(name = "do_id_ref")
        private String doIdRef;

        @ColumnInfo(name = "sync_status")
        private int syncStatus;

        public TagModel(@NonNull String epcTag, String tagId, String itmId, String productName, String doIdRef, int syncStatus) {
            this.epcTag = epcTag;
            this.tagId = tagId;
            this.itmId = itmId;
            this.productName = productName;
            this.doIdRef = doIdRef;
            this.syncStatus = syncStatus;
        }

        @NonNull public String getEpcTag() { return epcTag; }

        public String getTagId() { return tagId; }

        public String getItmId() { return itmId; }
        public String getProductName() { return productName; }
        public String getDoIdRef() { return doIdRef; }
        public int getSyncStatus() { return syncStatus; }

        public void setSyncStatus(int syncStatus) { this.syncStatus = syncStatus; }
    }
}
