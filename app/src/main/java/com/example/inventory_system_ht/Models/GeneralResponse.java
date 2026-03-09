package com.example.inventory_system_ht.Models;

import com.google.gson.annotations.SerializedName;

public class GeneralResponse {
    @SerializedName("message")
    private String message;

    public String getMessage() {
        return message;
    }
}