package com.meteuapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.meteuapp.models.Range;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Simple activity to list and edit parameter ranges.
 * Caches ranges in SharedPreferences as JSON for offline viewing.
 */
public class RangesActivity extends AppBaseActivity {

    private ListView lv;
    private ProgressBar progress;
    private ArrayAdapter<String> adapter;
    private List<Range> ranges = new ArrayList<>();
    private Gson gson = new Gson();

    private static final String PREFS = "meteu_prefs";
    private static final String KEY_RANGES = "cached_ranges";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranges);
        lv = findViewById(R.id.lv_ranges);
        progress = findViewById(R.id.progress);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lv.setAdapter(adapter);

        lv.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            Range r = ranges.get(position);
            showEditDialog(r);
        });

        loadCachedRanges();
        fetchRangesFromServer();
    }

    private void loadCachedRanges() {
        String json = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_RANGES, null);
        if (json != null) {
            try {
                Type t = new TypeToken<List<Range>>(){}.getType();
                List<Range> cached = gson.fromJson(json, t);
                if (cached != null) {
                    ranges = cached;
                    refreshList();
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveCachedRanges() {
        try {
            String json = gson.toJson(ranges);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_RANGES, json).apply();
        } catch (Exception ignored) {}
    }

    private void refreshList() {
        List<String> labels = new ArrayList<>();
        for (Range r : ranges) {
            labels.add(r.getParameter() + " — min=" + r.getMin() + " max=" + r.getMax());
        }
        adapter.clear(); adapter.addAll(labels); adapter.notifyDataSetChanged();
    }

    private void fetchRangesFromServer() {
        progress.setVisibility(View.VISIBLE);
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<List<Map<String,Object>>> call = api.getRanges();
        call.enqueue(new Callback<List<Map<String,Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String,Object>>> call, Response<List<Map<String,Object>>> response) {
                progress.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    ranges.clear();
                    for (Map<String,Object> m : response.body()) {
                        Range r = new Range();
                        r.setParameter((String) m.get("parameter"));
                        Object mn = m.get("min"); r.setMin(mn == null ? null : ((Number)mn).doubleValue());
                        Object mx = m.get("max"); r.setMax(mx == null ? null : ((Number)mx).doubleValue());
                        ranges.add(r);
                    }
                    saveCachedRanges();
                    refreshList();
                } else {
                    Toast.makeText(RangesActivity.this, "Error al cargar rangos", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String,Object>>> call, Throwable t) {
                progress.setVisibility(View.GONE);
                Toast.makeText(RangesActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showEditDialog(Range r) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_edit_range, null);
        EditText etMin = v.findViewById(R.id.et_min);
        EditText etMax = v.findViewById(R.id.et_max);
        etMin.setText(r.getMin() == null ? "" : String.valueOf(r.getMin()));
        etMax.setText(r.getMax() == null ? "" : String.valueOf(r.getMax()));
        b.setTitle("Editar rango: " + r.getParameter());
        b.setView(v);
        b.setPositiveButton("Guardar", (dialog, which) -> {
            String minS = etMin.getText().toString().trim();
            String maxS = etMax.getText().toString().trim();
            String minParam = minS.isEmpty() ? null : minS;
            String maxParam = maxS.isEmpty() ? null : maxS;
            postRangeToServer(r.getParameter(), minParam, maxParam);
        });
        b.setNegativeButton("Cancelar", null);
        b.show();
    }

    private void postRangeToServer(String parameter, String min, String max) {
        progress.setVisibility(View.VISIBLE);
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<Map<String,Object>> call = api.postRange(parameter, min, max);
        call.enqueue(new Callback<Map<String,Object>>() {
            @Override
            public void onResponse(Call<Map<String,Object>> call, Response<Map<String,Object>> response) {
                progress.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && Boolean.TRUE.equals(response.body().get("ok"))) {
                    // update local copy and cache
                    for (Range rr : ranges) if (rr.getParameter().equals(parameter)) {
                        try { rr.setMin(min == null ? null : Double.parseDouble(min)); } catch (Exception ignored) {}
                        try { rr.setMax(max == null ? null : Double.parseDouble(max)); } catch (Exception ignored) {}
                    }
                    saveCachedRanges();
                    refreshList();
                    Toast.makeText(RangesActivity.this, "Rango actualizado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RangesActivity.this, "Error actualizando rango", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String,Object>> call, Throwable t) {
                progress.setVisibility(View.GONE);
                Toast.makeText(RangesActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
