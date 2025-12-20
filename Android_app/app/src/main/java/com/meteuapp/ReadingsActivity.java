package com.meteuapp;

import android.os.Bundle;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
    private EditText etStart, etEnd;
    private RecyclerView rvReadings;
    private ReadingTableAdapter adapter;
    private SwipeRefreshLayout swipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_readings);

        etSensor = findViewById(R.id.et_sensor);
        etStreet = findViewById(R.id.et_street);
        etStart = findViewById(R.id.et_start);
        etEnd = findViewById(R.id.et_end);
        etLimit = findViewById(R.id.et_limit);
        Button btn = findViewById(R.id.btn_query);

        swipe = findViewById(R.id.swipe_readings);
        rvReadings = findViewById(R.id.rv_readings);
        rvReadings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReadingTableAdapter();
        rvReadings.setAdapter(adapter);

        // Pull-to-refresh should perform the same query as the Consultar button
        swipe.setOnRefreshListener(() -> query());

        // Clicking the start/end fields opens a date+time picker; manual input also allowed
        etStart.setOnClickListener(v -> showDateTimePicker(etStart));
        etEnd.setOnClickListener(v -> showDateTimePicker(etEnd));
        etStart.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showDateTimePicker(etStart); });
        etEnd.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showDateTimePicker(etEnd); });

        btn.setOnClickListener(v -> query());
    }

    private void query() {
        String sensor = etSensor.getText().toString().trim();
        String street = etStreet.getText().toString().trim();
        Integer limit = null;
        try { if (!etLimit.getText().toString().trim().isEmpty()) limit = Integer.parseInt(etLimit.getText().toString().trim()); } catch (NumberFormatException ex) { Toast.makeText(this, "Limit inválido", Toast.LENGTH_SHORT).show(); return; }

        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);

        String start = etStart.getText().toString().trim();
        String end = etEnd.getText().toString().trim();
        if (!start.isEmpty() && !isValidTimestamp(start)) { Toast.makeText(this, "Formato start inválido", Toast.LENGTH_SHORT).show(); swipe.setRefreshing(false); return; }
        if (!end.isEmpty() && !isValidTimestamp(end)) { Toast.makeText(this, "Formato end inválido", Toast.LENGTH_SHORT).show(); swipe.setRefreshing(false); return; }

        Call<List<SensorReading>> call = api.getSensorReadings(sensor.isEmpty()?null:sensor, street.isEmpty()?null:street, start.isEmpty()?null:start, end.isEmpty()?null:end, limit);
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

    private void showDateTimePicker(final EditText target) {
        final java.util.Calendar cal = java.util.Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            cal.set(java.util.Calendar.YEAR, year);
            cal.set(java.util.Calendar.MONTH, month);
            cal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
            // after date chosen, open time picker
            TimePickerDialog tp = new TimePickerDialog(ReadingsActivity.this, (timeView, hourOfDay, minute) -> {
                cal.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay);
                cal.set(java.util.Calendar.MINUTE, minute);
                cal.set(java.util.Calendar.SECOND, 0);
                java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-ddHH:mm:ss", java.util.Locale.getDefault());
                target.setText(df.format(cal.getTime()));
            }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), true);
            tp.show();
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH));
        dp.show();
    }

    private boolean isValidTimestamp(String s) {
        if (s == null || s.isEmpty()) return false;
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-ddHH:mm:ss", java.util.Locale.getDefault());
        df.setLenient(false);
        try {
            df.parse(s);
            return true;
        } catch (java.text.ParseException ex) {
            return false;
        }
    }
}
