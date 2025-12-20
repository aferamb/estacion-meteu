package com.meteuapp;

import android.os.Bundle;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.meteuapp.adapters.QueryReadingAdapter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.appcompat.app.AlertDialog;

public class ReadingsQueryActivity extends AppBaseActivity {

    private EditText etStart, etEnd, etValue, etLimit;
    private Spinner spFilter, spOp, spSortBy, spOrder;
    private RecyclerView rvReadings;
    private QueryReadingAdapter adapter;
    private SwipeRefreshLayout swipe;

    // column keys must match server ALLOWED_COLUMNS order/names
    private static final String[] COLUMN_KEYS = new String[]{
            "id","sensor_id","sensor_type","street_id","recorded_at","latitude","longitude","altitude","district","neighborhood",
            "temp","humid","aqi","lux","sound_db","atmhpa","uv_index","bsec_status","iaq","static_iaq",
            "co2_eq","breath_voc_eq","raw_temperature","raw_humidity","pressure_hpa","gas_resistance_ohm","gas_percentage",
            "stabilization_status","run_in_status","sensor_heat_comp_temp","sensor_heat_comp_hum"
    };

    private boolean[] visibleColumns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_readings_query);

        etStart = findViewById(R.id.et_start);
        etEnd = findViewById(R.id.et_end);
        etValue = findViewById(R.id.et_value);
        etLimit = findViewById(R.id.et_limit);
        spFilter = findViewById(R.id.sp_filter);
        spOp = findViewById(R.id.sp_op);
        spSortBy = findViewById(R.id.sp_sortby);
        spOrder = findViewById(R.id.sp_order);

        Button btnSelect = findViewById(R.id.btn_select_columns);
        Button btn = findViewById(R.id.btn_query);

        swipe = findViewById(R.id.swipe_readings_query);
        rvReadings = findViewById(R.id.rv_readings_query);
        rvReadings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QueryReadingAdapter();

        // default all visible
        visibleColumns = new boolean[COLUMN_KEYS.length];
        Arrays.fill(visibleColumns, true);
        adapter.setVisibleColumns(visibleColumns);
        rvReadings.setAdapter(adapter);

        // populate spinners for filter and sortBy with available columns
        ArrayAdapter<String> colsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, COLUMN_KEYS);
        colsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilter.setAdapter(colsAdapter);
        spSortBy.setAdapter(colsAdapter);

        ArrayAdapter<String> opAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"=","!=",">",">=","<","<=","LIKE"});
        opAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOp.setAdapter(opAdapter);

        ArrayAdapter<String> orderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"ASC","DESC"});
        orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOrder.setAdapter(orderAdapter);

        swipe.setOnRefreshListener(this::query);

        etStart.setOnClickListener(v -> showDateTimePicker(etStart));
        etEnd.setOnClickListener(v -> showDateTimePicker(etEnd));
        etStart.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showDateTimePicker(etStart); });
        etEnd.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) showDateTimePicker(etEnd); });

        btnSelect.setOnClickListener(v -> showColumnsDialog());
        btn.setOnClickListener(v -> query());
    }

    private void showColumnsDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Seleccionar columnas");
        CharSequence[] names = COLUMN_KEYS;
        boolean[] current = Arrays.copyOf(visibleColumns, visibleColumns.length);
        b.setMultiChoiceItems(names, current, (dialog, which, isChecked) -> current[which] = isChecked);
        b.setPositiveButton("OK", (dialog, which) -> {
            visibleColumns = current;
            adapter.setVisibleColumns(visibleColumns);
            View header = findViewById(R.id.header_readings_query);
            if (header instanceof ViewGroup) {
                ViewGroup hg = (ViewGroup) header;
                int childCount = Math.min(hg.getChildCount(), visibleColumns.length);
                for (int i = 0; i < childCount; i++) {
                    View child = hg.getChildAt(i);
                    child.setVisibility(visibleColumns[i] ? View.VISIBLE : View.GONE);
                }
            }
        });
        b.setNegativeButton("Cancelar", null);
        b.show();
    }

    private void query() {
        String start = etStart.getText().toString().trim();
        String end = etEnd.getText().toString().trim();
        String filter = (String) spFilter.getSelectedItem();
        String op = (String) spOp.getSelectedItem();
        String sortBy = (String) spSortBy.getSelectedItem();
        String order = (String) spOrder.getSelectedItem();
        String value = etValue.getText().toString().trim();

        Integer limit = null;
        try { if (!etLimit.getText().toString().trim().isEmpty()) limit = Integer.parseInt(etLimit.getText().toString().trim()); } catch (NumberFormatException ex) { Toast.makeText(this, "Limit inválido", Toast.LENGTH_SHORT).show(); return; }

        if (!start.isEmpty() && !isValidTimestamp(start)) { Toast.makeText(this, "Formato start inválido", Toast.LENGTH_SHORT).show(); swipe.setRefreshing(false); return; }
        if (!end.isEmpty() && !isValidTimestamp(end)) { Toast.makeText(this, "Formato end inválido", Toast.LENGTH_SHORT).show(); swipe.setRefreshing(false); return; }

        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        swipe.setRefreshing(true);
        Call<List<Map<String, Object>>> call = api.queryReadings(start.isEmpty()?null:start, end.isEmpty()?null:end, filter, value.isEmpty()?null:value, op, sortBy, order, limit, 0);
        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                swipe.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null) { Toast.makeText(ReadingsQueryActivity.this, "Error en la consulta", Toast.LENGTH_SHORT).show(); return; }
                adapter.setItems(response.body());
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                swipe.setRefreshing(false);
                Toast.makeText(ReadingsQueryActivity.this, "Fallo conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showDateTimePicker(final EditText target) {
        final java.util.Calendar cal = java.util.Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            cal.set(java.util.Calendar.YEAR, year);
            cal.set(java.util.Calendar.MONTH, month);
            cal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
            TimePickerDialog tp = new TimePickerDialog(ReadingsQueryActivity.this, (timeView, hourOfDay, minute) -> {
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
        try { df.parse(s); return true; } catch (java.text.ParseException ex) { return false; }
    }
}
