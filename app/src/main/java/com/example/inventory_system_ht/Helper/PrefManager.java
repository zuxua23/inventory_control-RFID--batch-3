package com.example.inventory_system_ht.Helper;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;
    int PRIVATE_MODE = 0;

    private static final String PREF_NAME = "InventoryPrefs";
    private static final String KEY_IS_LOGIN = "IsLoggedIn";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "UserId";
    private static final String KEY_USERNAME = "UserUsername";
    private static final String KEY_FULLNAME = "UserFullName";
    private static final String KEY_LOGIN_TIME = "login_time";
    private static final long SESSION_DURATION = 8 * 60 * 60 * 1000;
    private static final String KEY_BASE_URL = "base_url_api";
    private static final String DEFAULT_URL = "";

    public PrefManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void saveToken(String token) {
        editor.putBoolean(KEY_IS_LOGIN, true);
        editor.putString(KEY_TOKEN, token);
        editor.putLong(KEY_LOGIN_TIME, System.currentTimeMillis());
        editor.apply();
    }

    public boolean isSessionValid() {
        boolean isLoggedIn = pref.getBoolean(KEY_IS_LOGIN, false);
        String token = pref.getString(KEY_TOKEN, null);
        long loginTime = pref.getLong(KEY_LOGIN_TIME, 0);

        if (!isLoggedIn || token == null) return false;

        long currentTime = System.currentTimeMillis();
        return (currentTime - loginTime) < SESSION_DURATION;
    }
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

    public void saveIp(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        if (!url.endsWith("/")) {
            url += "/";
        }

        editor.putString(KEY_BASE_URL, url);
        editor.apply();
    }

    public String getIp() {
        return pref.getString(KEY_BASE_URL, DEFAULT_URL);
    }
    public String getBaseUrl() {
        return pref.getString(KEY_BASE_URL, DEFAULT_URL);
    }

}