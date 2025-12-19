package com.meteuapp;

import android.app.Application;
import android.content.Context;

public class MeteuApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // initialize RetrofitClient with application context so interceptors can broadcast
        RetrofitClient.init(getApplicationContext());
    }

    // helper to access app context from static places if needed
    public static Context appContext;
}
