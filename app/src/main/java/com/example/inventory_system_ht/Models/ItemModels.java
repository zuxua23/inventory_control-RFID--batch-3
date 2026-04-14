package com.example.inventory_system_ht.Models;

import java.io.Serializable;

public class ItemModels {

    public static class ItemModel implements Serializable {
        private final String epcTag;
        private final String itemId;
        private final String itemName;
        private final int qty;

        public ItemModel(String epcTag, String itemId, String itemName, int qty) {
            this.epcTag = epcTag;
            this.itemId = itemId;
            this.itemName = itemName;
            this.qty = qty;
        }

        public String getEpcTag() { return epcTag; }
        public String getItemId() { return itemId; }
        public String getItemName() { return itemName; }
        public int getQty() { return qty; }

    }

    public static class ItemResponseDto {
        private final String itemId;
        private final String itemName;

        public ItemResponseDto(String itemId, String itemName) {
            this.itemId = itemId;
            this.itemName = itemName;
        }

        public String getItemId() { return itemId; }
        public String getItemName() { return itemName; }
    }
    public static class SumProductModel {
        private String itemId;
        private String itemName;
        private int count;

        public SumProductModel(String itemId, String itemName, int count) {
            this.itemId   = itemId;
            this.itemName = itemName;
            this.count    = count;
        }

        public void addCount(int n) { this.count += n; }
        public String getItemId()   { return itemId; }
        public String getItemName() { return itemName; }
        public int getCount()       { return count; }
    }

}