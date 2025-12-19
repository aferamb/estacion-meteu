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
    private androidx.recyclerview.widget.RecyclerView rvAlarms;
    private com.meteuapp.adapters.AlarmAdapter alarmsAdapter;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarms);

        etSensor = findViewById(R.id.et_sensor);
        rvAlarms = findViewById(R.id.rv_alarms);
        swipe = findViewById(R.id.swipe);
        alarmsAdapter = new com.meteuapp.adapters.AlarmAdapter();
        rvAlarms.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvAlarms.setAdapter(alarmsAdapter);
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
                alarmsAdapter.setItems(response.body());
            }

            @Override
            public void onFailure(Call<List<Alarm>> call, Throwable t) {
                if (swipe != null) swipe.setRefreshing(false);
            }
        });
    }
}
