package com.meteuapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.meteuapp.mqtt.MqttManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubscriptionsActivity extends AppBaseActivity {

    private ListView lv;
    private ProgressBar progress;
    private ArrayAdapter<String> adapter;
    private List<String> topics = new ArrayList<>();
    private Gson gson = new Gson();
    private Button btnAddSubscription;
    private Button btnUnsubscribe;
    private int selectedIndex = -1;

    private static final String PREFS = "meteu_prefs";
    private static final String KEY_SUBSCRIPTIONS = "cached_subscriptions";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscriptions);
        lv = findViewById(R.id.lv_subscriptions);
        progress = findViewById(R.id.progress);
        btnAddSubscription = findViewById(R.id.btn_add_subscription);
        btnUnsubscribe = findViewById(R.id.btn_unsubscribe);

        // start with unsubscribe disabled until selection
        btnUnsubscribe.setEnabled(false);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, new ArrayList<>());
        lv.setAdapter(adapter);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        lv.setOnItemLongClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            String topic = topics.get(position);
            confirmUnsubscribe(topic);
            return true;
        });

        lv.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            selectedIndex = position;
            lv.setItemChecked(position, true);
            // enable unsubscribe button when an item is selected
            btnUnsubscribe.setEnabled(true);
        });

        btnAddSubscription.setOnClickListener(v -> showAddDialog());
        btnUnsubscribe.setOnClickListener(v -> {
            if (selectedIndex >= 0 && selectedIndex < topics.size()) {
                String topic = topics.get(selectedIndex);
                postUnsubscribe(topic);
            } else {
                Toast.makeText(SubscriptionsActivity.this, "Selecciona un topic primero", Toast.LENGTH_SHORT).show();
            }
        });

        loadCached();
        fetchFromServer();
    }

    private void loadCached() {
        String json = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_SUBSCRIPTIONS, null);
        if (json != null) {
            try {
                Type t = new TypeToken<List<String>>(){}.getType();
                List<String> cached = gson.fromJson(json, t);
                if (cached != null) {
                    topics = cached;
                    refreshList();
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveCached() {
        try { getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_SUBSCRIPTIONS, gson.toJson(topics)).apply(); } catch (Exception ignored) {}
    }

    private void refreshList() {
        adapter.clear(); adapter.addAll(topics); adapter.notifyDataSetChanged();
        // restore selection if possible
        if (selectedIndex >= 0 && selectedIndex < topics.size()) {
            lv.setItemChecked(selectedIndex, true);
        } else {
            selectedIndex = -1;
        }
        // no legacy tv_list anymore
    }

    private void fetchFromServer() {
        progress.setVisibility(View.VISIBLE);
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<List<String>> call = api.getSubscriptions();
        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                progress.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    topics.clear(); topics.addAll(response.body());
                    saveCached();
                    refreshList();
                } else {
                    Toast.makeText(SubscriptionsActivity.this, "Error cargando suscripciones", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                progress.setVisibility(View.GONE);
                Toast.makeText(SubscriptionsActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAddDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_subscription, null);
        EditText et = v.findViewById(R.id.et_topic);
        b.setTitle("Añadir suscripción");
        b.setView(v);
        b.setPositiveButton("Añadir", (d, w) -> {
            String topic = et.getText().toString().trim();
            if (!topic.isEmpty()) postSubscribe(topic);
        });
        b.setNegativeButton("Cancelar", null);
        b.show();
    }

    private void postSubscribe(String topic) {
        progress.setVisibility(View.VISIBLE);
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<Map<String,Object>> call = api.postSubscribe(topic);
        call.enqueue(new Callback<Map<String,Object>>() {
            @Override
            public void onResponse(Call<Map<String,Object>> call, Response<Map<String,Object>> response) {
                progress.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    if (!topics.contains(topic)) topics.add(topic);
                    saveCached(); refreshList();
                    // local MQTT subscribe for instant delivery
                    try { MqttManager.getInstance(SubscriptionsActivity.this).subscribe(topic); } catch (Exception ignored) {}
                    Toast.makeText(SubscriptionsActivity.this, "Suscrito: " + topic, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SubscriptionsActivity.this, "Error suscribiendo", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String,Object>> call, Throwable t) {
                progress.setVisibility(View.GONE);
                Toast.makeText(SubscriptionsActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmUnsubscribe(String topic) {
        new AlertDialog.Builder(this)
                .setTitle("Anular suscripción")
                .setMessage("¿Desea anular la suscripción a '" + topic + "'?")
                .setPositiveButton("Sí", (d, w) -> postUnsubscribe(topic))
                .setNegativeButton("No", null)
                .show();
    }

    private void postUnsubscribe(String topic) {
        progress.setVisibility(View.VISIBLE);
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<Map<String,Object>> call = api.postUnsubscribe(topic);
        call.enqueue(new Callback<Map<String,Object>>() {
            @Override
            public void onResponse(Call<Map<String,Object>> call, Response<Map<String,Object>> response) {
                progress.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    topics.remove(topic); saveCached(); refreshList();
                    try { MqttManager.getInstance(SubscriptionsActivity.this).unsubscribe(topic); } catch (Exception ignored) {}
                    Toast.makeText(SubscriptionsActivity.this, "Anulado: " + topic, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SubscriptionsActivity.this, "Error al anular", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String,Object>> call, Throwable t) {
                progress.setVisibility(View.GONE);
                Toast.makeText(SubscriptionsActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}

