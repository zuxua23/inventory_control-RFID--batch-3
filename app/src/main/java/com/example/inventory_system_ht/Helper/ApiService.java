package com.example.inventory_system_ht.Helper;

import com.example.inventory_system_ht.Models.LoginRequest;
import com.example.inventory_system_ht.Models.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);
}