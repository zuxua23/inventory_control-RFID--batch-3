package com.example.inventory_system_ht.Models;

import com.google.gson.annotations.SerializedName;

public class StockOutModels {

    public static class ScanReq {
        public String doId;
        public String readerId;
        public String epc;

        public ScanReq(String doId, String readerId, String epc) {
            this.doId = doId;
            this.readerId = readerId;
            this.epc = epc;
        }
    }

    public static class FinalizeReq {
        @SerializedName("DoId")
        public String doId;

        @SerializedName("ReaderId")
        public String readerId;

        public FinalizeReq(String doId, String readerId) {
            this.doId = doId;
            this.readerId = readerId;
        }
    }
}