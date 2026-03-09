package com.example.inventory_system_ht.Models;
import com.google.gson.annotations.SerializedName;

public class StockTakingModels {

    // Request Models
    public static class CreateReq {
        @SerializedName("remark") public String remark;
        public CreateReq(String remark) { this.remark = remark; }
    }

    public static class ScanReq {
        @SerializedName("sttId") public String sttId;
        @SerializedName("epc") public String epc;
        public ScanReq(String sttId, String epc) { this.sttId = sttId; this.epc = epc; }
    }

    public static class RemoveReq {
        @SerializedName("sttId") public String sttId;
        @SerializedName("tagId") public String tagId;
        public RemoveReq(String sttId, String tagId) { this.sttId = sttId; this.tagId = tagId; }
    }

    public static class FinalizeReq {
        @SerializedName("sttId") public String sttId;
        public FinalizeReq(String sttId) { this.sttId = sttId; }
    }

    // Response Models
    public static class CreateRes {
        @SerializedName("stockTakingId") public String stockTakingId;
    }
}