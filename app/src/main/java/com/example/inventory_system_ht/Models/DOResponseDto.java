package com.example.inventory_system_ht.Models;

import java.util.List;

public class DOResponseDto {
    private String doId;
    private String doNumber;
    private List<DODetailResponseDto> details;

    // Getters
    public String getDoId() { return doId; }
    public String getDoNumber() { return doNumber; }
    public List<DODetailResponseDto> getDetails() { return details; }
}
