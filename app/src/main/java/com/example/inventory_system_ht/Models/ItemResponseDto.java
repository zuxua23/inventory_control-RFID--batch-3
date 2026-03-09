package com.example.inventory_system_ht.Models;

public class ItemResponseDto {
    private String id;
    private String itemId; // Misal: ITM-001
    private String itemName; // Misal: Kemeja Anti Kusut

    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
}