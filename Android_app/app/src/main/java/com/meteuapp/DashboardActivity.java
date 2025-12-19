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
    private TextView tvMessages, tvStations, tvWelcome;

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

        tvMessages = findViewById(R.id.tv_messages);
        tvStations = findViewById(R.id.tv_stations);
        tvWelcome = findViewById(R.id.tv_welcome);
        tvWelcome.setText("Dashboard — " + session.getUsername());

        Button btnRefresh = findViewById(R.id.btn_refresh);
        Button btnLogout = findViewById(R.id.btn_logout);
        btnRefresh.setOnClickListener(v -> loadLive());
        btnLogout.setOnClickListener(v -> doLogout());

        findViewById(R.id.btn_readings).setOnClickListener(v -> startActivity(new android.content.Intent(this, ReadingsActivity.class)));
        findViewById(R.id.btn_subscriptions).setOnClickListener(v -> startActivity(new android.content.Intent(this, SubscriptionsActivity.class)));
        findViewById(R.id.btn_alarms).setOnClickListener(v -> startActivity(new android.content.Intent(this, AlarmsActivity.class)));
        findViewById(R.id.btn_publish).setOnClickListener(v -> startActivity(new android.content.Intent(this, PublishAlertActivity.class)));

        // Load on start
        loadLive();
    }

    private void loadLive() {
        tvMessages.setText("Mensajes recientes:\n");
        tvStations.setText("Estaciones recientes:\n");

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
                    Toast.makeText(DashboardActivity.this, "Error cargando datos live", Toast.LENGTH_SHORT).show();
                    return;
                }
                LiveResponse lr = response.body();
                List<SensorMessage> msgs = lr.getMessages();
                List<Station> sts = lr.getStations();

                StringBuilder mb = new StringBuilder();
                if (msgs != null) {
                    for (SensorMessage m : msgs) {
                        mb.append(m.getSensorId())
                                .append(" @ ")
                                .append(m.getRecordedAt())
                                .append(" — t:")
                                .append(m.getTemp())
                                .append(" h:")
                                .append(m.getHumid())
                                .append(" aqi:")
                                .append(m.getAqi())
                                .append(" lux:")
                                .append(m.getLux())
                                .append("\n");
                    }
                }
                tvMessages.append(mb.toString());

                StringBuilder sb = new StringBuilder();
                if (sts != null) {
                    for (Station s : sts) {
                        sb.append(s.getSensorId())
                                .append(" last_seen: ")
                                .append(s.getLastSeen())
                                .append("\n");
                    }
                }
                tvStations.append(sb.toString());
            }

            @Override
            public void onFailure(Call<LiveResponse> call, Throwable t) {
                Log.e("meteu", "Error loading live: ", t);
                // If JSON conversion fails or server returned HTML, assume session expired or invalid
                String msg = t.getMessage() == null ? "" : t.getMessage();
                if (msg.toLowerCase().contains("malformed") || msg.toLowerCase().contains("json") || msg.toLowerCase().contains("expected")) {
                    handleSessionExpired();
                    return;
                }
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
