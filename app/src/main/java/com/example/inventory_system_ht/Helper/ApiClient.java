package com.example.inventory_system_ht.Helper;

import android.content.Context;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        PrefManager prefManager = new PrefManager(context);
        String baseUrl = prefManager.getBaseUrl();

        // Fallback IP: Jangan pakai localhost di device fisik.
        // Ganti dengan IP PC lu sementara kalau user belum ngisi di Setting.
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://192.168.1.100/"; // Ganti IP ini sesuai IP IPv4 PC lu
        }

        // --- 1. PASANG CCTV (LOGGING INTERCEPTOR) ---
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // --- 2. PASANG SATPAM (AUTH INTERCEPTOR) ---
        Interceptor authInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request originalRequest = chain.request();

                // Ambil token JWT dari PrefManager
                String token = prefManager.getToken();

                Request.Builder builder = originalRequest.newBuilder()
                        .header("Accept", "application/json");

                // Kalau tokennya ada (user udah login), otomatis tempelin ke Header
                if (token != null && !token.isEmpty()) {
                    builder.header("Authorization", "Bearer " + token);
                }

                return chain.proceed(builder.build());
            }
        };

        // --- 3. SETUP KONEKSI & TIMEOUTS ---
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor) // Pasang CCTV
                .addInterceptor(authInterceptor)    // Pasang Satpam Token
                .connectTimeout(30, TimeUnit.SECONDS) // Waktu nunggu nyambung ke server
                .readTimeout(30, TimeUnit.SECONDS)    // Waktu nunggu balasan Saga dari server
                .writeTimeout(30, TimeUnit.SECONDS)   // Waktu ngirim payload data scan
                .build();

        // Cek retrofit null atau URL berubah
        if (retrofit == null || !retrofit.baseUrl().toString().equals(baseUrl)) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client) // Tempel OkHttpClient yang udah OP ini
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}