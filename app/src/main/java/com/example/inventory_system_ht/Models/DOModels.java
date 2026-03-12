package com.example.inventory_system_ht.Models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DOModels {

    @Entity(tableName = "tb_DO")
    public static class DOModel {
        @PrimaryKey
        @NonNull
        @SerializedName("doId")
        @ColumnInfo(name = "do_id")
        private String doId;

        @SerializedName("doNumber")
        @ColumnInfo(name = "do_number")
        private String doNo;

        @SerializedName("status")
        @ColumnInfo(name = "status")
        private String status;

        @SerializedName("createdAt")
        @ColumnInfo(name = "created_at")
        private String createdAt;

        @SerializedName("scannerType")
        @ColumnInfo(name = "scanner_type")
        private String scannerType;

        public DOModel(@NonNull String doId, String doNo, String status, String createdAt, String scannerType) {
            this.doId = doId;
            this.doNo = doNo;
            this.status = status;
            this.createdAt = createdAt;
            this.scannerType = scannerType;
        }

        @NonNull public String getDoId() { return doId; }
        public String getDoNo() { return doNo; }
        public String getStatus() { return status; }
        public String getCreatedAt() { return createdAt; }

        public String getScannerType() { return scannerType; }
    }

    public static class DODetailResponseDto {
        private String itemId;
        private String itemName;
        private int qtyRequired;
        private int qtyScanned = 0;

        public String getItemId() { return itemId; }
        public String getItemName() { return itemName; }
        public int getQtyRequired() { return qtyRequired; }
        public int getQtyScanned() { return qtyScanned; }
        public void setQtyScanned(int qty) { this.qtyScanned = qty; }
    }

    public static class DOResponseDto {
        private String doId;
        private String doNumber;
        private List<DODetailResponseDto> details;

        public String getDoId() { return doId; }
        public String getDoNumber() { return doNumber; }
        public List<DODetailResponseDto> getDetails() { return details; }
    }
}