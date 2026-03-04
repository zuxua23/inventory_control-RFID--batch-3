package com.example.inventory_system_ht.Models;

import java.io.Serializable;

public class TagModel implements Serializable {
    private String epcTag;
    private String productName;

    public TagModel(String epcTag, String productName) {
        this.epcTag = epcTag;
        this.productName = productName;
    }

    public String getEpcTag() { return epcTag; }
    public String getProductName() { return productName; }
}