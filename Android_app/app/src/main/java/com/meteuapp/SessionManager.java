package com.meteuapp;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Simple session manager storing a minimal logged-in flag and username.
 * For sensitive data use EncryptedSharedPreferences (AndroidX Security).
 */
public class SessionManager {
    private static final String PREFS = "meteu_prefs";
    private static final String KEY_LOGGED = "logged_in";
    private static final String KEY_USER = "username";
    private static final String KEY_JWT = "jwt_token";

    private final SharedPreferences prefs;

    public SessionManager(Context ctx) {
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void setLoggedIn(String username, boolean logged) {
        prefs.edit().putBoolean(KEY_LOGGED, logged).putString(KEY_USER, username).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED, false);
    }

    public String getUsername() { return prefs.getString(KEY_USER, null); }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public void setJwtToken(String token) {
        prefs.edit().putString(KEY_JWT, token).apply();
    }

    public String getJwtToken() { return prefs.getString(KEY_JWT, null); }
}
