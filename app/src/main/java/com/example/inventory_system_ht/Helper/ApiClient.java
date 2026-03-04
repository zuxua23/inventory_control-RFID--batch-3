package com.example.inventory_system_ht.Helper;

import android.content.Context;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        PrefManager prefManager = new PrefManager(context);
        String baseUrl = prefManager.getBaseUrl();

        if (baseUrl.isEmpty()) {
            baseUrl = "http://localhost/";
        }

        // --- PASANG CCTV (LOGGING INTERCEPTOR) ---
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // Level BODY biar keliatan isi JSON-nya

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        // Cek retrofit null atau URL berubah
        if (retrofit == null || !retrofit.baseUrl().toString().equals(baseUrl)) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client) // Tempel Client baru
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}