package com.meteuapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.meteuapp.models.SensorReading;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReadingsActivity extends AppBaseActivity {

    private EditText etSensor, etStreet, etLimit;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_readings);

        etSensor = findViewById(R.id.et_sensor);
        etStreet = findViewById(R.id.et_street);
        etLimit = findViewById(R.id.et_limit);
        Button btn = findViewById(R.id.btn_query);
        tvResult = findViewById(R.id.tv_result);

        btn.setOnClickListener(v -> query());
    }

    private void query() {
        String sensor = etSensor.getText().toString().trim();
        String street = etStreet.getText().toString().trim();
        Integer limit = null;
        try { if (!etLimit.getText().toString().trim().isEmpty()) limit = Integer.parseInt(etLimit.getText().toString().trim()); } catch (NumberFormatException ex) { Toast.makeText(this, "Limit inválido", Toast.LENGTH_SHORT).show(); return; }

        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<List<SensorReading>> call = api.getSensorReadings(sensor.isEmpty()?null:sensor, street.isEmpty()?null:street, null, null, limit);
        tvResult.setText("Cargando...\n");
        call.enqueue(new Callback<List<SensorReading>>() {
            @Override
            public void onResponse(Call<List<SensorReading>> call, Response<List<SensorReading>> response) {
                if (!response.isSuccessful() || response.body() == null) { tvResult.setText("Error en la consulta"); return; }
                StringBuilder b = new StringBuilder();
                for (SensorReading r : response.body()) {
                    b.append(r.getRecordedAt()).append(" | sensor:").append(r.getSensorId()).append(" | t:").append(r.getTemp())
                            .append(" h:").append(r.getHumid()).append(" aqi:").append(r.getAqi()).append(" lux:").append(r.getLux()).append("\n");
                }
                tvResult.setText(b.toString());
            }

            @Override
            public void onFailure(Call<List<SensorReading>> call, Throwable t) {
                tvResult.setText("Fallo conexión: " + t.getMessage());
            }
        });
    }
}
