package com.example.inventory_system_ht.Models;

import com.google.gson.annotations.SerializedName;

public class TagInfoDto {
    @SerializedName("tagId") private String tagId;
    @SerializedName("epcTag") private String epcTag;
    @SerializedName("itemName") private String itemName;
    @SerializedName("itemId") private String itemId;
    @SerializedName("status") private String status;

    // --- TAMBAHKAN CONSTRUCTOR INI BRE ---
    public TagInfoDto(String tagId, String epcTag, String itemName, String itemId, String status) {
        this.tagId = tagId;
        this.epcTag = epcTag;
        this.itemName = itemName;
        this.itemId = itemId;
        this.status = status;
    }

    // Tetep jaga Getter-nya biar gak error di tempat lain
    public String getTagId() { return tagId; }
    public String getEpcTag() { return epcTag; }
    public String getItemName() { return itemName; }
    public String getItemId() { return itemId; }
    public String getStatus() { return status; }
}