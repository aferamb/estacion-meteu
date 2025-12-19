package com.meteuapp.network;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple in-memory CookieJar implementation.
 * Stores cookies per host for the app runtime. This keeps session cookies
 * received from the server after login so subsequent requests are authenticated.
 *
 * Note: cookies are not persisted across app restarts. For persistence,
 * extend this class to save cookies to SharedPreferences or EncryptedSharedPreferences.
 */
public class InMemoryCookieJar implements CookieJar {

    private final Map<String, List<Cookie>> cookieStore = new HashMap<>();

    @Override
    public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        cookieStore.put(url.host(), new ArrayList<>(cookies));
    }

    @Override
    public synchronized List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> cookies = cookieStore.get(url.host());
        return cookies != null ? new ArrayList<>(cookies) : new ArrayList<>();
    }

    /**
     * Clear all stored cookies (e.g., on logout).
     */
    public synchronized void clear() {
        cookieStore.clear();
    }
}
