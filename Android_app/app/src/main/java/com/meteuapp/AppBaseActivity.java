package com.meteuapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

/**
 * Base activity that enforces session presence and listens for global session-expired broadcasts.
 */
public class AppBaseActivity extends AppCompatActivity {

    private SessionManager session;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
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
    public void setContentView(int layoutResID) {
        // Inflate base layout with drawer, toolbar and a content_frame
        LayoutInflater inflater = LayoutInflater.from(this);
        super.setContentView(com.meteuapp.R.layout.activity_base);
        FrameLayout content = findViewById(com.meteuapp.R.id.content_frame);
        inflater.inflate(layoutResID, content, true);

        // toolbar and drawer setup
        Toolbar toolbar = findViewById(com.meteuapp.R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeAsUpIndicator(com.meteuapp.R.drawable.ic_menu_app);
            }
            toolbar.setNavigationOnClickListener(v -> {
                DrawerLayout dl = findViewById(com.meteuapp.R.id.drawer_layout);
                if (dl != null) dl.openDrawer(GravityCompat.START);
            });
        }

        drawerLayout = findViewById(com.meteuapp.R.id.drawer_layout);
        navigationView = findViewById(com.meteuapp.R.id.nav_view);
        if (navigationView != null) setupNavigationMenu(navigationView.getMenu());
    }

    private void setupNavigationMenu(Menu menu) {
        menu.clear();
        // Always available
        menu.add(0, 1, 0, "Dashboard");
        menu.add(0, 2, 0, "Readings");
        menu.add(0, 3, 0, "Readings Query");
        menu.add(0, 4, 0, "Monitor Topic");
        menu.add(0, 5, 0, "Monitor Estación");
        menu.add(0, 6, 0, "Subscriptions");
        // Map view for subscriptions
        menu.add(0, 11, 0, "Mapa");

        // admin-only
        SessionManager sm = new SessionManager(this);
        boolean isAdmin = "admin".equals(sm.getRole());
        if (isAdmin) {
            menu.add(0, 7, 0, "Alarms");
            menu.add(0, 8, 0, "Ranges");
            menu.add(0, 9, 0, "Publish Alert");
            menu.add(0, 10, 0, "Users");
        }

        menu.add(0, 99, 99, "Logout");

        navigationView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            int id = item.getItemId();
            switch (id) {
                case 1: startActivity(new Intent(this, DashboardActivity.class)); return true;
                case 2: startActivity(new Intent(this, ReadingsActivity.class)); return true;
                case 3: startActivity(new Intent(this, ReadingsQueryActivity.class)); return true;
                case 4: startActivity(new Intent(this, TopicMonitoringActivity.class)); return true;
                case 5: startActivity(new Intent(this, StationSelectionActivity.class)); return true;
                case 6: startActivity(new Intent(this, SubscriptionsActivity.class)); return true;
                case 11: startActivity(new Intent(this, MapActivity.class)); return true;
                case 7: startActivity(new Intent(this, AlarmsActivity.class)); return true;
                case 8: startActivity(new Intent(this, RangesActivity.class)); return true;
                case 9: startActivity(new Intent(this, PublishAlertActivity.class)); return true;
                case 10: startActivity(new Intent(this, UsersActivity.class)); return true;
                case 99:
                    // logout
                    SessionManager sess = new SessionManager(this);
                    sess.clear();
                    RetrofitClient.clearCookies();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                    return true;
            }
            return false;
        });
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
