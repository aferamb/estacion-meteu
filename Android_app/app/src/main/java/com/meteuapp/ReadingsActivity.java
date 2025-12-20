package com.meteuapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.meteuapp.adapters.ReadingTableAdapter;

import com.meteuapp.models.SensorReading;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReadingsActivity extends AppBaseActivity {

    private EditText etSensor, etStreet, etLimit;
    private RecyclerView rvReadings;
    private ReadingTableAdapter adapter;
    private SwipeRefreshLayout swipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_readings);

        etSensor = findViewById(R.id.et_sensor);
        etStreet = findViewById(R.id.et_street);
        etLimit = findViewById(R.id.et_limit);
        Button btn = findViewById(R.id.btn_query);

        swipe = findViewById(R.id.swipe_readings);
        rvReadings = findViewById(R.id.rv_readings);
        rvReadings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReadingTableAdapter();
        rvReadings.setAdapter(adapter);

        // Pull-to-refresh should perform the same query as the Consultar button
        swipe.setOnRefreshListener(() -> query());

        btn.setOnClickListener(v -> query());
    }

    private void query() {
        String sensor = etSensor.getText().toString().trim();
        String street = etStreet.getText().toString().trim();
        Integer limit = null;
        try { if (!etLimit.getText().toString().trim().isEmpty()) limit = Integer.parseInt(etLimit.getText().toString().trim()); } catch (NumberFormatException ex) { Toast.makeText(this, "Limit inválido", Toast.LENGTH_SHORT).show(); return; }

        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<List<SensorReading>> call = api.getSensorReadings(sensor.isEmpty()?null:sensor, street.isEmpty()?null:street, null, null, limit);
        swipe.setRefreshing(true);
        call.enqueue(new Callback<List<SensorReading>>() {
            @Override
            public void onResponse(Call<List<SensorReading>> call, Response<List<SensorReading>> response) {
                swipe.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null) { Toast.makeText(ReadingsActivity.this, "Error en la consulta", Toast.LENGTH_SHORT).show(); return; }
                adapter.setItems(response.body());
            }

            @Override
            public void onFailure(Call<List<SensorReading>> call, Throwable t) {
                swipe.setRefreshing(false);
                Toast.makeText(ReadingsActivity.this, "Fallo conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
