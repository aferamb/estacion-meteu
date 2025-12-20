package com.meteuapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.meteuapp.models.Alarm;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlarmsActivity extends AppBaseActivity {

    private EditText etSensor;
    private androidx.recyclerview.widget.RecyclerView rvAlarmsActive, rvAlarmsClosed;
    private com.meteuapp.adapters.AlarmTableAdapter activeAdapter;
    private com.meteuapp.adapters.AlarmTableAdapter closedAdapter;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarms);

        etSensor = findViewById(R.id.et_sensor);
        rvAlarmsActive = findViewById(R.id.rv_alarms_active);
        rvAlarmsClosed = findViewById(R.id.rv_alarms_closed);
        swipe = findViewById(R.id.swipe);
        activeAdapter = new com.meteuapp.adapters.AlarmTableAdapter();
        closedAdapter = new com.meteuapp.adapters.AlarmTableAdapter();
        rvAlarmsActive.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvAlarmsActive.setAdapter(activeAdapter);
        rvAlarmsClosed.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvAlarmsClosed.setAdapter(closedAdapter);
        // refresh button removed from layout; pull-to-refresh via SwipeRefreshLayout remains
        swipe.setOnRefreshListener(this::refresh);

        refresh();
    }

    private void refresh() {
        String sensor = etSensor.getText().toString().trim();
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<List<Alarm>> call = api.getAlarms(sensor.isEmpty()?null:sensor, null, null, 200);
        call.enqueue(new Callback<List<Alarm>>() {
            @Override
            public void onResponse(Call<List<Alarm>> call, Response<List<Alarm>> response) {
                if (swipe != null) swipe.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null) { return; }
                // split into active and closed lists
                List<Alarm> active = new java.util.ArrayList<>();
                List<Alarm> closed = new java.util.ArrayList<>();
                for (Alarm a : response.body()) {
                    if (a.getActive() != null && a.getActive()) active.add(a); else closed.add(a);
                }
                activeAdapter.setItems(active);
                closedAdapter.setItems(closed);
            }

            @Override
            public void onFailure(Call<List<Alarm>> call, Throwable t) {
                if (swipe != null) swipe.setRefreshing(false);
            }
        });
    }
}
