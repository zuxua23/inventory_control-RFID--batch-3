package com.example.inventory_system_ht.Models;

import java.io.Serializable;

public class ItemModels {

    public static class ItemModel implements Serializable {
        private String epcTag;
        private String itemId;
        private String itemName;
        private int qty;

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

        public void setQty(int qty) { this.qty = qty; }

        public void setEpcTag(String epcTag) { this.epcTag = epcTag; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        public void setItemName(String itemName) { this.itemName = itemName; }
    }

    public static class ItemResponseDto {
        private String id;
        private String itemId;
        private String itemName;

        public String getItemId() { return itemId; }
        public String getItemName() { return itemName; }
    }
}
