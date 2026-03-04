package com.example.inventory_system_ht.Helper;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;
    int PRIVATE_MODE = 0;

    private static final String PREF_NAME = "InventoryPrefs";

    // Keys untuk Data User & Session
    private static final String KEY_IS_LOGIN = "IsLoggedIn";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "UserId";
    private static final String KEY_USERNAME = "UserUsername";
    private static final String KEY_FULLNAME = "UserFullName";
    private static final String KEY_LOGIN_TIME = "login_time";

    // 8 Jam dalam milidetik
    private static final long SESSION_DURATION = 8 * 60 * 60 * 1000;

    // Key untuk Setting IP / URL
    private static final String KEY_BASE_URL = "base_url_api";
    private static final String DEFAULT_URL = ""; // Dikosongin biar user dipaksa ngisi

    public PrefManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    // ====================================================================
    // 1. BAGIAN DATA USER (LOGIN, SESSION & LOGOUT)
    // ====================================================================

    // Simpan Token dan mulai timer session 8 jam
    public void saveToken(String token) {
        editor.putBoolean(KEY_IS_LOGIN, true);
        editor.putString(KEY_TOKEN, token);
        editor.putLong(KEY_LOGIN_TIME, System.currentTimeMillis());
        editor.apply();
    }

    // Simpan data diri (Nanti dipanggil setelah hit API /auth/me)
    public void saveProfile(String userId, String username, String fullName) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_FULLNAME, fullName);
        editor.apply();
    }

    // Cek apakah session masih di bawah 8 jam
    public boolean isSessionValid() {
        boolean isLoggedIn = pref.getBoolean(KEY_IS_LOGIN, false);
        String token = pref.getString(KEY_TOKEN, null);
        long loginTime = pref.getLong(KEY_LOGIN_TIME, 0);

        if (!isLoggedIn || token == null) return false;

        long currentTime = System.currentTimeMillis();
        return (currentTime - loginTime) < SESSION_DURATION;
    }

    // Logout: Hapus session & data user, TAPI IP JANGAN DIHAPUS
    public void clearSession() {
        editor.remove(KEY_IS_LOGIN);
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_LOGIN_TIME);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_FULLNAME);
        editor.apply();
    }

    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, "");
    }

    public String getUserFullName() {
        return pref.getString(KEY_FULLNAME, "User Gudang");
    }

    public String getUserUsername() {
        return pref.getString(KEY_USERNAME, "-");
    }

    // ====================================================================
    // 2. BAGIAN SETTING IP ADDRESS (BASE URL)
    // ====================================================================

    public void saveIp(String url) {
        // Validasi otomatis ala kodingan lu: Pastikan ada http://
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        // Validasi otomatis: Pastikan akhiran ada garis miring (/)
        if (!url.endsWith("/")) {
            url += "/";
        }

        editor.putString(KEY_BASE_URL, url);
        editor.apply();
    }

    public String getIp() {
        // Balikin IP yang disimpen. Kalau belum ada, balikin string kosong aja
        return pref.getString(KEY_BASE_URL, DEFAULT_URL);
    }
    public String getBaseUrl() {
        // Defaultnya "" (Kosong), sesuai request biar dipaksa isi di awal
        return pref.getString(KEY_BASE_URL, DEFAULT_URL);
    }

}