package com.example.inventory_system_ht.Models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StockTakingModels {

    public static class ScanReq {
        public String sttId;
        public String epc;

        public ScanReq(String sttId, String epc) {
            this.sttId = sttId;
            this.epc = epc;
        }
    }

    public static class RemoveReq {
        public String sttId;
        public String tagId;

        public RemoveReq(String sttId, String tagId) {
            this.sttId = sttId;
            this.tagId = tagId;
        }
    }

    public static class ManualAddReq {
        public String sttId;
        public String itemId;
        public String remark;

        public ManualAddReq(String sttId, String itemId, String remark) {
            this.sttId = sttId;
            this.itemId = itemId;
            this.remark = remark;
        }
    }

    public static class FinalizeReq {
        public String sttId;

        public FinalizeReq(String sttId) { this.sttId = sttId; }
    }

    public static class BulkScanReq {
        public String sttId;
        public List<BulkItemDto> items;

        public BulkScanReq(String sttId, List<String> epcs) {
            this.sttId = sttId;
            this.items = new ArrayList<>();
            for (String epc : epcs) {
                BulkItemDto dto = new BulkItemDto();
                dto.epc = epc;
                this.items.add(dto);
            }
        }
    }

    public static class BulkItemDto {
        public String epc;
    }
    public static class ActiveRes implements Serializable {
        @SerializedName("sttId") public String sttId;
        @SerializedName("remark") public String remark;
        @SerializedName("status") public String status;
        @SerializedName("location") public String location;
    }

    public static class SessionItem implements Serializable {
        @SerializedName("tagId") public String tagId;
        @SerializedName("epcTag") public String epcTag;
        @SerializedName("itemId") public String itemId;
        @SerializedName("itemName") public String itemName;
        @SerializedName("location") public String location;

        public transient String state = "PENDING";
        public transient String manualRemark = "";
    }

    @Entity(tableName = "tb_scan_queue")
    public static class ScanQueueEntity {
        @PrimaryKey(autoGenerate = true) public int id;
        @ColumnInfo(name = "stt_id") public String sttId;
        @ColumnInfo(name = "epc_tag") public String epcTag;
        @ColumnInfo(name = "action") public String action;
        @ColumnInfo(name = "item_id") public String itemId;
        @ColumnInfo(name = "remark") public String remark;
        @ColumnInfo(name = "is_synced") public boolean isSynced;
        @ColumnInfo(name = "created_at") public long createdAt;
    }

    @Entity(tableName = "tb_session_items")
    public static class SessionItemEntity {
        @PrimaryKey
        @NonNull
        @ColumnInfo(name = "epc_tag") public String epcTag  = "";
        @ColumnInfo(name = "tag_id") public String tagId;
        @ColumnInfo(name = "item_id") public String itemId;
        @ColumnInfo(name = "item_name") public String itemName;
        @ColumnInfo(name = "location") public String location;
        @ColumnInfo(name = "stt_id") public String sttId;

        public SessionItem toSessionItem() {
            SessionItem s = new SessionItem();
            s.epcTag = epcTag;
            s.tagId = tagId;
            s.itemId = itemId;
            s.itemName = itemName;
            s.location = location;
            s.state = "PENDING";
            return s;
        }

        public static SessionItemEntity from(String sttId, SessionItem s) {
            SessionItemEntity e = new SessionItemEntity();
            e.sttId = sttId;
            e.epcTag = s.epcTag != null ? s.epcTag : "";
            e.tagId = s.tagId;
            e.itemId = s.itemId;
            e.itemName = s.itemName;
            e.location = s.location;
            return e;
        }
    }
}