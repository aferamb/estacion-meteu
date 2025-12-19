package com.meteuapp;

import com.meteuapp.network.InMemoryCookieJar;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

/**
 * RetrofitClient builds a Retrofit instance backed by an OkHttpClient that
 * keeps cookies in memory (session cookies) so the app can perform a login
 * using server-side sessions (cookie-based authentication).
 */
public class RetrofitClient {

    public static final String ACTION_SESSION_EXPIRED = "com.meteuapp.ACTION_SESSION_EXPIRED";

    private static Retrofit retrofit;
    private static android.content.Context appContext;
    // Default to local server for emulator; change to match your server.
    private static final String BASE_URL = "http://192.168.2.156:8080/";
    // MQTT broker URL (tcp). Update if your broker runs on another host/port.
    public static final String MQTT_BROKER_URL = "tcp://192.168.2.156:1883";

    // keep a reference to the cookie jar so we can clear cookies on logout
    private static final com.meteuapp.network.InMemoryCookieJar cookieJar = new com.meteuapp.network.InMemoryCookieJar();

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            // OkHttp client with a simple in-memory CookieJar to preserve session cookies
                // add logging to inspect raw HTTP responses when debugging
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                OkHttpClient client = new OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

                // Use a lenient Gson parser to be more tolerant to slightly malformed JSON
                Gson gson = new GsonBuilder().setLenient().create();

                retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

        /**
         * Initialize the client with application context. Must be called from Application.onCreate().
         */
        public static void init(android.content.Context ctx) {
            appContext = ctx.getApplicationContext();
            // rebuild retrofit so that interceptors with context are active
            retrofit = null;
            retrofit = getRetrofitInstanceWithAuthInterceptor();
        }

        private static Retrofit getRetrofitInstanceWithAuthInterceptor() {
            if (retrofit != null) return retrofit;

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            okhttp3.Interceptor authWatcher = chain -> {
                okhttp3.Request req = chain.request();
                okhttp3.Response resp = chain.proceed(req);
                if (resp.code() == 401 && appContext != null) {
                    android.content.Intent i = new android.content.Intent(ACTION_SESSION_EXPIRED);
                    appContext.sendBroadcast(i);
                }
                return resp;
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    .addInterceptor(logging)
                    .addInterceptor(authWatcher)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Gson gson = new GsonBuilder().setLenient().create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            return retrofit;
        }

    /**
     * Clear cookies stored in the in-memory CookieJar (used on logout).
     */
    public static void clearCookies() {
        cookieJar.clear();
    }
}
