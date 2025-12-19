package com.meteuapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.meteuapp.models.Alarm;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlarmsActivity extends AppBaseActivity {

    private EditText etSensor;
    private TextView tvList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarms);

        etSensor = findViewById(R.id.et_sensor);
        tvList = findViewById(R.id.tv_list);
        Button btn = findViewById(R.id.btn_refresh);
        btn.setOnClickListener(v -> refresh());

        refresh();
    }

    private void refresh() {
        String sensor = etSensor.getText().toString().trim();
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<List<Alarm>> call = api.getAlarms(sensor.isEmpty()?null:sensor, null, null, 200);
        tvList.setText("Cargando...\n");
        call.enqueue(new Callback<List<Alarm>>() {
            @Override
            public void onResponse(Call<List<Alarm>> call, Response<List<Alarm>> response) {
                if (!response.isSuccessful() || response.body() == null) { tvList.setText("Error cargando"); return; }
                StringBuilder b = new StringBuilder();
                for (Alarm a : response.body()) {
                    b.append(a.getTriggeredAt()).append(" | ").append(a.getSensorId()).append(" | ").append(a.getParameter()).append(" | active:").append(a.getActive()).append("\n");
                }
                tvList.setText(b.toString());
            }

            @Override
            public void onFailure(Call<List<Alarm>> call, Throwable t) {
                tvList.setText("Error: " + t.getMessage());
            }
        });
    }
}
