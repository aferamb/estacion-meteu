package com.meteuapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Base activity that enforces session presence and listens for global session-expired broadcasts.
 */
public class AppBaseActivity extends AppCompatActivity {

    private SessionManager session;
    private BroadcastReceiver logoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // session expired: clear and go to login
            session.clear();
            RetrofitClient.clearCookies();
            Toast.makeText(AppBaseActivity.this, "Sesión expirada. Redirigiendo al login.", Toast.LENGTH_LONG).show();
            Intent i = new Intent(AppBaseActivity.this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        // if not logged in, send to login (avoid instanceof compile issues by checking class name)
        if (!session.isLoggedIn()) {
            String clsName = this.getClass().getSimpleName();
            if (!"LoginActivity".equals(clsName)) {
                Intent i = new Intent(this, LoginActivity.class);
                startActivity(i);
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // register receiver for global session expiry
        registerReceiver(logoutReceiver, new IntentFilter(RetrofitClient.ACTION_SESSION_EXPIRED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(logoutReceiver); } catch (Exception ignored) {}
    }
}
