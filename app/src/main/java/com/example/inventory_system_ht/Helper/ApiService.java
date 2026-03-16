package com.example.inventory_system_ht.Helper;

import com.example.inventory_system_ht.Models.AuthModels;
import com.example.inventory_system_ht.Models.DOModels;
import com.example.inventory_system_ht.Models.GeneralResponse;
import com.example.inventory_system_ht.Models.ItemModels;
import com.example.inventory_system_ht.Models.StockInRequest;
import com.example.inventory_system_ht.Models.StockOutModels;
import com.example.inventory_system_ht.Models.StockPrepBulkRequest;
import com.example.inventory_system_ht.Models.StockTakingModels;
import com.example.inventory_system_ht.Models.TagModels;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("auth/login")
    Call<AuthModels.LoginResponse> login(@Body AuthModels.LoginRequest loginRequest);
    @POST("tag/register")
    Call<GeneralResponse> registerTags(
            @Header("Authorization") String token,
            @Body AuthModels.RegisterRequest request
    );
    @POST("stock/in")
    Call<GeneralResponse> stockIn(
            @Header("Authorization") String token,
            @Body StockInRequest request
    );
    @GET("item")
    Call<List<ItemModels.ItemResponseDto>> getAllItems(
            @Header("Authorization") String token
    );
    @GET("stock/in/info/{code}")
    Call<TagModels.TagInfoDto> getTagInfo(
            @Header("Authorization") String token,
            @Path("code") String code
    );

    @POST("stock/preparation/bulk")
    Call<GeneralResponse> submitStockPrep(
            @Header("Authorization") String token,
            @Body StockPrepBulkRequest request
    );
    @GET("do")
    Call<List<DOModels.DOModel>> getAllDO(@Header("Authorization") String token);

    // stock taking
    @POST("stock-taking/create")
    Call<StockTakingModels.CreateRes> createStockTaking(@Header("Authorization") String token, @Body StockTakingModels.CreateReq request);
    @GET("stock-taking/stock-data")
    Call<List<TagModels.TagModel>> getStockData(@Header("Authorization") String token);
    @POST("stock-taking/scan")
    Call<GeneralResponse> scanStockTaking(@Header("Authorization") String token, @Body StockTakingModels.ScanReq request);
    @POST("stock-taking/remove")
    Call<GeneralResponse> removeStockTaking(@Header("Authorization") String token, @Body StockTakingModels.RemoveReq request);
    @POST("stock-taking/manual-add")
    Call<GeneralResponse> manualAddStockTaking(@Header("Authorization") String token, @Body StockTakingModels.ManualAddReq request);
    @POST("stock-taking/finalize")
    Call<GeneralResponse> finalizeStockTaking(@Header("Authorization") String token, @Body StockTakingModels.FinalizeReq request);

    // Stock Out
    @POST("stock/out/scan")
    Call<GeneralResponse> scanStockOut(@Header("Authorization") String token, @Body StockOutModels.ScanReq request);
    @POST("stock/out")
    Call<GeneralResponse> finalizeStockOut(@Header("Authorization") String token, @Body StockOutModels.FinalizeReq request);
}