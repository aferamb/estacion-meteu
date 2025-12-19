package com.meteuapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.meteuapp.models.Alarm;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlarmsActivity extends AppBaseActivity {

    private EditText etSensor;
    private ListView lvAlarms;
    private ArrayAdapter<String> alarmsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarms);

        etSensor = findViewById(R.id.et_sensor);
        lvAlarms = findViewById(R.id.lv_alarms);
        alarmsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new java.util.ArrayList<>());
        lvAlarms.setAdapter(alarmsAdapter);
        Button btn = findViewById(R.id.btn_refresh);
        btn.setOnClickListener(v -> refresh());

        refresh();
    }

    private void refresh() {
        String sensor = etSensor.getText().toString().trim();
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<List<Alarm>> call = api.getAlarms(sensor.isEmpty()?null:sensor, null, null, 200);
        alarmsAdapter.clear(); alarmsAdapter.add("Cargando...");
        call.enqueue(new Callback<List<Alarm>>() {
            @Override
            public void onResponse(Call<List<Alarm>> call, Response<List<Alarm>> response) {
                if (!response.isSuccessful() || response.body() == null) { alarmsAdapter.clear(); alarmsAdapter.add("Error cargando"); return; }
                alarmsAdapter.clear();
                for (Alarm a : response.body()) {
                    alarmsAdapter.add(a.getTriggeredAt() + " | " + a.getSensorId() + " | " + a.getParameter() + " | active:" + a.getActive());
                }
            }

            @Override
            public void onFailure(Call<List<Alarm>> call, Throwable t) {
                alarmsAdapter.clear(); alarmsAdapter.add("Error: " + t.getMessage());
            }
        });
    }
}
