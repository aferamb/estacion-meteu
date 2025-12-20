package com.meteuapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PublishAlertActivity extends AppBaseActivity {

    private EditText etTopic, etAlert, etMessage;
    private Spinner spinnerSubs;
    private Switch swUseSubscription;
    private List<String> cachedSubs = new ArrayList<>();
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_alert);

        etTopic = findViewById(R.id.et_topic);
        spinnerSubs = findViewById(R.id.spinner_subs);
        swUseSubscription = findViewById(R.id.sw_use_subscription);
        etAlert = findViewById(R.id.et_alert);
        etMessage = findViewById(R.id.et_message);
        Button btn = findViewById(R.id.btn_publish);

        // load cached subscriptions and populate spinner
        loadCachedSubscriptions();

        swUseSubscription.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                spinnerSubs.setVisibility(View.VISIBLE);
                etTopic.setEnabled(false);
                etTopic.setAlpha(0.6f);
            } else {
                spinnerSubs.setVisibility(View.GONE);
                etTopic.setEnabled(true);
                etTopic.setAlpha(1f);
            }
        });

        // default: use subscription if available, else manual
        swUseSubscription.setChecked(cachedSubs.size() > 0);

        btn.setOnClickListener(v -> publish());
    }

    private void loadCachedSubscriptions() {
        try {
            String json = getSharedPreferences("meteu_prefs", MODE_PRIVATE).getString("cached_subscriptions", null);
            if (json != null) {
                Type t = new TypeToken<List<String>>(){}.getType();
                List<String> list = gson.fromJson(json, t);
                if (list != null) cachedSubs.addAll(list);
            }
        } catch (Exception ignored) {}
        // ensure a fallback item
        List<String> adapterList = new ArrayList<>();
        adapterList.add("-- Selecciona --");
        adapterList.addAll(cachedSubs);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, adapterList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubs.setAdapter(adapter);
    }

    private void publish() {
        String topic = etTopic.getText().toString().trim();
        String alert = etAlert.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        String subscription = null;
        if (swUseSubscription.isChecked()) {
            int pos = spinnerSubs.getSelectedItemPosition();
            if (pos > 0) subscription = (String) spinnerSubs.getSelectedItem();
        }

        // validation: must supply either a saved subscription or a manual topic
        if ((subscription == null || subscription.isEmpty()) && topic.isEmpty()) {
            Toast.makeText(this, "Selecciona una suscripción o escribe un topic manual", Toast.LENGTH_LONG).show();
            return;
        }

        // alert key required
        if (alert.isEmpty()) {
            Toast.makeText(this, "Introduce la clave de alerta (ej. WTH001)", Toast.LENGTH_LONG).show();
            return;
        }

        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<java.util.Map<String,Object>> call = api.publishAlert(topic.isEmpty()?null:topic, subscription==null?null:subscription, alert, message.isEmpty()?null:message);
        call.enqueue(new Callback<java.util.Map<String,Object>>() {
            @Override
            public void onResponse(Call<java.util.Map<String,Object>> call, Response<java.util.Map<String,Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PublishAlertActivity.this, "Publicado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PublishAlertActivity.this, "Error al publicar: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<java.util.Map<String,Object>> call, Throwable t) {
                Toast.makeText(PublishAlertActivity.this, "Fallo conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
