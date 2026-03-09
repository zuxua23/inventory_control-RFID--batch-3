package com.example.inventory_system_ht.Models;


public class DODetailResponseDto {
    private String itemId;
    private String itemName;
    private int qtyRequired;
    private int qtyScanned = 0; // Tambahan buat hitung progres di UI Android

    // Getters & Setters
    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public int getQtyRequired() { return qtyRequired; }
    public int getQtyScanned() { return qtyScanned; }
    public void setQtyScanned(int qty) { this.qtyScanned = qty; }
}
