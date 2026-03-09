package com.example.inventory_system_ht.Helper;

import com.example.inventory_system_ht.Models.DOModel;
import com.example.inventory_system_ht.Models.ItemResponseDto;
import com.example.inventory_system_ht.Models.LoginRequest;
import com.example.inventory_system_ht.Models.LoginResponse;
import com.example.inventory_system_ht.Models.RegisterRequest;
import com.example.inventory_system_ht.Models.GeneralResponse;
import com.example.inventory_system_ht.Models.StockInRequest;
import com.example.inventory_system_ht.Models.StockPrepBulkRequest;
import com.example.inventory_system_ht.Models.StockTakingModels;
import com.example.inventory_system_ht.Models.TagInfoDto;
import com.example.inventory_system_ht.Models.TagModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);
    @POST("tag/register")
    Call<GeneralResponse> registerTags(
            @Header("Authorization") String token,
            @Body RegisterRequest request
    );
    @POST("stock/in")
    Call<GeneralResponse> stockIn(
            @Header("Authorization") String token,
            @Body StockInRequest request
    );
    @GET("item")
    Call<List<ItemResponseDto>> getAllItems(
            @Header("Authorization") String token
    );
    @GET("stock/in/info/{code}")
    Call<TagInfoDto> getTagInfo(
            @Header("Authorization") String token,
            @Path("code") String code
    );

    @POST("stock/preparation/bulk") // Sesuaikan sama route baru lu
    Call<GeneralResponse> submitStockPrep(
            @Header("Authorization") String token,
            @Body StockPrepBulkRequest request
    );
    @GET("do")
    Call<List<DOModel>> getAllDO(@Header("Authorization") String token);

    // stock taking
    @POST("stock-taking/create")
    Call<StockTakingModels.CreateRes> createStockTaking(@Header("Authorization") String token, @Body StockTakingModels.CreateReq request);

    @GET("stock-taking/stock-data")
    Call<List<TagModel>> getStockData(@Header("Authorization") String token);

    @POST("stock-taking/scan")
    Call<GeneralResponse> scanStockTaking(@Header("Authorization") String token, @Body StockTakingModels.ScanReq request);

    @POST("stock-taking/remove")
    Call<GeneralResponse> removeStockTaking(@Header("Authorization") String token, @Body StockTakingModels.RemoveReq request);

    @POST("stock-taking/finalize")
    Call<GeneralResponse> finalizeStockTaking(@Header("Authorization") String token, @Body StockTakingModels.FinalizeReq request);
}