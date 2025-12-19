package com.meteuapp.network;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * Persistent cookie jar that stores cookies to SharedPreferences as JSON.
 * Not encrypted; for production use EncryptedSharedPreferences.
 */
public class InMemoryCookieJar implements CookieJar {

    private static final String PREFS = "meteu_cookie_prefs";
    private static final String KEY_COOKIES = "cookies_json";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<SerializableCookie>>(){}.getType();

    // in-memory cache for request speed
    private List<SerializableCookie> cache;

    public InMemoryCookieJar(Context ctx) {
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.cache = loadFromPrefs();
    }

    @Override
    public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        List<SerializableCookie> list = cache != null ? cache : new ArrayList<>();
        // replace any cookies with same name+domain+path
        for (Cookie c : cookies) {
            SerializableCookie sc = new SerializableCookie(c);
            boolean replaced = false;
            Iterator<SerializableCookie> it = list.iterator();
            while (it.hasNext()) {
                SerializableCookie existing = it.next();
                if (existing.matches(sc)) { it.remove(); replaced = true; }
            }
            list.add(sc);
        }
        cache = list;
        saveToPrefs(list);
    }

    @Override
    public synchronized List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> out = new ArrayList<>();
        List<SerializableCookie> list = cache != null ? cache : loadFromPrefs();
        if (list == null) return out;
        long now = System.currentTimeMillis();
        List<SerializableCookie> kept = new ArrayList<>();
        for (SerializableCookie sc : list) {
            if (sc.expiresAt < now) continue;
            if (sc.matchesHost(url.host())) {
                out.add(sc.toCookie());
            }
            kept.add(sc);
        }
        // update cache and prefs to remove expired
        cache = kept;
        saveToPrefs(kept);
        return out;
    }

    public synchronized void clear() {
        cache = new ArrayList<>();
        prefs.edit().remove(KEY_COOKIES).apply();
    }

    private List<SerializableCookie> loadFromPrefs() {
        String json = prefs.getString(KEY_COOKIES, null);
        if (json == null) return new ArrayList<>();
        try { return gson.fromJson(json, listType); } catch (Exception e) { return new ArrayList<>(); }
    }

    private void saveToPrefs(List<SerializableCookie> list) {
        try { prefs.edit().putString(KEY_COOKIES, gson.toJson(list)).apply(); } catch (Exception ignored) {}
    }

    // helper POJO to store cookie fields
    private static class SerializableCookie {
        String name; String value; String domain; String path; long expiresAt; boolean secure; boolean httpOnly;

        SerializableCookie() {}

        SerializableCookie(Cookie c) {
            this.name = c.name(); this.value = c.value(); this.domain = c.domain(); this.path = c.path();
            this.expiresAt = c.expiresAt(); this.secure = c.secure(); this.httpOnly = c.httpOnly();
        }

        Cookie toCookie() {
            Cookie.Builder b = new Cookie.Builder().name(name).value(value).domain(domain).path(path);
            b.expiresAt(expiresAt);
            if (secure) b.secure();
            if (httpOnly) b.httpOnly();
            return b.build();
        }

        boolean matches(SerializableCookie other) {
            return this.name.equals(other.name) && this.domain.equals(other.domain) && this.path.equals(other.path);
        }

        boolean matchesHost(String host) {
            // simple domain match
            return host.equals(domain) || (domain.startsWith(".") && host.endsWith(domain.substring(1)));
        }
    }
}
