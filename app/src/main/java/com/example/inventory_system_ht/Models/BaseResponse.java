package com.example.inventory_system_ht.Models;

import com.google.gson.annotations.SerializedName;

public class BaseResponse<T> {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    // 👇 PENTING: Buat nangkep status transaksi dari Redis/Saga 👇
    @SerializedName("saga_status")
    private String sagaStatus;

    @SerializedName("data")
    private T data; // T ini artinya "Generic", bisa diisi List<DOModel>, Profil User, dll

    // --- GETTER ---
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getSagaStatus() {
        return sagaStatus;
    }

    public T getData() {
        return data;
    }
}