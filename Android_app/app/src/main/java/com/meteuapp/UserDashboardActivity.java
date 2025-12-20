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

public class UserDashboardActivity extends AppBaseActivity {

    private SessionManager session;
    private TextView tvWelcome;
    private androidx.recyclerview.widget.RecyclerView rvMessages, rvStations;
    private com.meteuapp.adapters.RecentMessagesAdapter messageAdapter;
    private com.meteuapp.adapters.RecentStationsAdapter stationAdapter;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        rvMessages = findViewById(R.id.rv_messages);
        rvStations = findViewById(R.id.rv_stations);
        swipe = findViewById(R.id.swipe_live);
        tvWelcome = findViewById(R.id.tv_welcome);
        tvWelcome.setText("Dashboard — " + session.getUsername());

        messageAdapter = new com.meteuapp.adapters.RecentMessagesAdapter();
        stationAdapter = new com.meteuapp.adapters.RecentStationsAdapter();
        rvMessages.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvMessages.setAdapter(messageAdapter);
        rvStations.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvStations.setAdapter(stationAdapter);
        if (swipe != null) swipe.setOnRefreshListener(this::loadLive);

        // limited UI: hide admin-only buttons, but keep monitoring options
        Button btnLogout = findViewById(R.id.btn_logout);
        Button btnReadings = findViewById(R.id.btn_readings);
        Button btnReadingsQuery = findViewById(R.id.btn_readings_query);
        Button btnAlarms = findViewById(R.id.btn_alarms);
        Button btnRanges = findViewById(R.id.btn_ranges);
        Button btnSubscriptions = findViewById(R.id.btn_subscriptions);
        Button btnPublish = findViewById(R.id.btn_publish);
        Button btnUsers = findViewById(R.id.btn_users);
        Button btnTopicMonitoring = findViewById(R.id.btn_topic_monitoring);
        Button btnStationMonitoring = findViewById(R.id.btn_station_monitoring);

        // show only allowed buttons for normal users
        btnReadings.setOnClickListener(v -> startActivity(new Intent(this, ReadingsActivity.class)));
        if (btnReadingsQuery != null) btnReadingsQuery.setOnClickListener(v -> startActivity(new Intent(this, ReadingsQueryActivity.class)));
        btnLogout.setOnClickListener(v -> doLogout());

        if (btnTopicMonitoring != null) btnTopicMonitoring.setOnClickListener(v -> startActivity(new Intent(this, TopicMonitoringActivity.class)));
        if (btnStationMonitoring != null) btnStationMonitoring.setOnClickListener(v -> startActivity(new Intent(this, StationSelectionActivity.class)));

        // hide admin-only buttons for normal users
        if (btnAlarms != null) btnAlarms.setVisibility(android.view.View.GONE);
        if (btnRanges != null) btnRanges.setVisibility(android.view.View.GONE);
        if (btnSubscriptions != null) btnSubscriptions.setVisibility(android.view.View.GONE);
        if (btnPublish != null) btnPublish.setVisibility(android.view.View.GONE);
        if (btnUsers != null) btnUsers.setVisibility(android.view.View.GONE);

        // Load on start
        loadLive();
    }

    private void loadLive() {
        if (swipe != null) swipe.setRefreshing(true);
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<LiveResponse> call = api.getLiveApi();
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
                    Toast.makeText(UserDashboardActivity.this, "Error cargando datos live", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (swipe != null) swipe.setRefreshing(false);
                LiveResponse lr = response.body();
                List<SensorMessage> msgs = lr.getMessages();
                List<Station> sts = lr.getStations();
                messageAdapter.setItems(msgs);
                stationAdapter.setItems(sts);
            }

            @Override
            public void onFailure(Call<LiveResponse> call, Throwable t) {
                Log.e("meteu", "Error loading live: ", t);
                String msg = t.getMessage() == null ? "" : t.getMessage();
                // Do NOT assume JSON parse errors mean session expired; allow authWatcher/401 handling to manage expiries.
                if (swipe != null) swipe.setRefreshing(false);
                Toast.makeText(UserDashboardActivity.this, "Fallo conexión: " + msg, Toast.LENGTH_LONG).show();
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
        session.clear();
        RetrofitClient.clearCookies();
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
