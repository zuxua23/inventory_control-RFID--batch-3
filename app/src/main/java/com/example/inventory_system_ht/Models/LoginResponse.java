package com.example.inventory_system_ht.Models;

public class LoginResponse {
    private boolean success;
    private String token;

    public boolean isSuccess() { return success; }
    public String getToken() { return token; }
}