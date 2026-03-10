package com.example.inventory_system_ht.Models;

import com.google.gson.annotations.SerializedName;

public class StockTakingModels {

    public static class CreateReq {
        public String remark;
        public CreateReq(String remark) { this.remark = remark; }
    }

    public static class CreateRes {
        @SerializedName("stockTakingId")
        public String stockTakingId;
    }

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
}