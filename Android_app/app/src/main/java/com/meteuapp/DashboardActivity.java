package com.meteuapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.meteuapp.models.LiveResponse;
import com.meteuapp.models.SensorMessage;
import com.meteuapp.models.Station;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * DashboardActivity shows the live data (messages and stations) calling /admin/live.
 * It requires the user to be logged in (session cookie preserved by CookieJar).
 */
public class DashboardActivity extends AppBaseActivity {

    private SessionManager session;
    private TextView tvWelcome;
    private androidx.recyclerview.widget.RecyclerView rvMessages, rvStations;
    private com.meteuapp.adapters.RecentMessagesAdapter recentMessagesAdapter;
    private com.meteuapp.adapters.RecentStationsAdapter recentStationsAdapter;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            // Not logged in -> go to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        rvMessages = findViewById(R.id.rv_messages);
        rvStations = findViewById(R.id.rv_stations);
        swipe = findViewById(R.id.swipe_live);
        tvWelcome = findViewById(R.id.tv_welcome);
        tvWelcome.setText("Dashboard — " + session.getUsername());

        recentMessagesAdapter = new com.meteuapp.adapters.RecentMessagesAdapter();
        recentStationsAdapter = new com.meteuapp.adapters.RecentStationsAdapter();
        rvMessages.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvMessages.setAdapter(recentMessagesAdapter);
        rvStations.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvStations.setAdapter(recentStationsAdapter);
        if (swipe != null) swipe.setOnRefreshListener(this::loadLive);

        // show cached subscriptions count in welcome
        updateSubscriptionsCount();

        Button btnLogout = findViewById(R.id.btn_logout);
        Button btnUsers = findViewById(R.id.btn_users);
        btnLogout.setOnClickListener(v -> doLogout());

        findViewById(R.id.btn_readings).setOnClickListener(v -> startActivity(new android.content.Intent(this, ReadingsActivity.class)));
        findViewById(R.id.btn_readings_query).setOnClickListener(v -> startActivity(new android.content.Intent(this, ReadingsQueryActivity.class)));
        findViewById(R.id.btn_topic_monitoring).setOnClickListener(v -> startActivity(new android.content.Intent(this, TopicMonitoringActivity.class)));
        findViewById(R.id.btn_station_monitoring).setOnClickListener(v -> startActivity(new android.content.Intent(this, StationSelectionActivity.class)));
        findViewById(R.id.btn_subscriptions).setOnClickListener(v -> startActivity(new android.content.Intent(this, SubscriptionsActivity.class)));
        findViewById(R.id.btn_alarms).setOnClickListener(v -> startActivity(new android.content.Intent(this, AlarmsActivity.class)));
        findViewById(R.id.btn_publish).setOnClickListener(v -> startActivity(new android.content.Intent(this, PublishAlertActivity.class)));
        findViewById(R.id.btn_users).setOnClickListener(v -> startActivity(new android.content.Intent(this, UsersActivity.class)));
        findViewById(R.id.btn_ranges).setOnClickListener(v -> startActivity(new android.content.Intent(this, RangesActivity.class)));

        // Load on start
        loadLive();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // refresh subscriptions count when returning
        updateSubscriptionsCount();
    }

    private void updateSubscriptionsCount() {
        try {
            String json = getSharedPreferences("meteu_prefs", MODE_PRIVATE).getString("cached_subscriptions", null);
            int cnt = 0;
            if (json != null) {
                com.google.gson.Gson g = new com.google.gson.Gson();
                java.util.List<String> list = g.fromJson(json, new com.google.gson.reflect.TypeToken<java.util.List<String>>(){}.getType());
                if (list != null) cnt = list.size();
            }
            // Show only username in the welcome header (remove subscriptions count)
            tvWelcome.setText("Dashboard — " + session.getUsername());
        } catch (Exception ignored) {}
    }

    private void loadLive() {
        if (swipe != null) swipe.setRefreshing(true);
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<LiveResponse> call = api.getLive();
        call.enqueue(new Callback<LiveResponse>() {
            @Override
            public void onResponse(Call<LiveResponse> call, Response<LiveResponse> response) {
                // If server returns 401 or a non-JSON HTML page (login redirect) treat as session expired
                if (response.code() == 401) {
                    handleSessionExpired();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    // try to inspect error body for HTML/login page
                    try {
                        if (response.errorBody() != null) {
                            String err = response.errorBody().string();
                            if (err != null && err.trim().startsWith("<")) {
                                handleSessionExpired();
                                return;
                            }
                        }
                    } catch (Exception ignore) {}
                    if (swipe != null) swipe.setRefreshing(false);
                    Toast.makeText(DashboardActivity.this, "Error cargando datos live", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (swipe != null) swipe.setRefreshing(false);
                LiveResponse lr = response.body();
                List<SensorMessage> msgs = lr.getMessages();
                List<Station> sts = lr.getStations();
                // Populate recent messages/stations with required subset columns
                recentMessagesAdapter.setItems(msgs);
                recentStationsAdapter.setItems(sts);
            }

            @Override
            public void onFailure(Call<LiveResponse> call, Throwable t) {
                Log.e("meteu", "Error loading live: ", t);
                String msg = t.getMessage() == null ? "" : t.getMessage();
                // Do NOT assume JSON parse errors mean session expired; allow authWatcher/401 handling to manage expiries.
                if (swipe != null) swipe.setRefreshing(false);
                Toast.makeText(DashboardActivity.this, "Fallo conexión: " + msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleSessionExpired() {
        // clear stored session flag and cookies and force re-login
        session.clear();
        RetrofitClient.clearCookies();
        Toast.makeText(this, "Sesión expirada. Por favor inicia sesión de nuevo.", Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void doLogout() {
        // Clear session flag and cookies
        session.clear();
        RetrofitClient.clearCookies();
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
