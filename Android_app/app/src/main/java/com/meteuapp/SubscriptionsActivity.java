package com.meteuapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubscriptionsActivity extends AppBaseActivity {

    private EditText etTopic;
    private TextView tvList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscriptions);

        etTopic = findViewById(R.id.et_topic);
        tvList = findViewById(R.id.tv_list);
        Button btnSub = findViewById(R.id.btn_subscribe);
        Button btnUnsub = findViewById(R.id.btn_unsubscribe);
        Button btnRef = findViewById(R.id.btn_refresh);

        btnSub.setOnClickListener(v -> subscribe());
        btnUnsub.setOnClickListener(v -> unsubscribe());
        btnRef.setOnClickListener(v -> refresh());

        refresh();
    }

    private void subscribe() {
        String topic = etTopic.getText().toString().trim();
        if (topic.isEmpty()) { etTopic.setError("Topic requerido"); return; }
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<java.util.Map<String,Object>> call = api.postSubscribe(topic);
        call.enqueue(new Callback<java.util.Map<String,Object>>() {
            @Override
            public void onResponse(Call<java.util.Map<String,Object>> call, Response<java.util.Map<String,Object>> response) {
                Toast.makeText(SubscriptionsActivity.this, "Subscribe: " + response.code(), Toast.LENGTH_SHORT).show();
                // Also subscribe locally via MQTT manager
                com.meteuapp.mqtt.MqttManager.getInstance(SubscriptionsActivity.this).subscribe(topic);
                refresh();
            }

            @Override
            public void onFailure(Call<java.util.Map<String,Object>> call, Throwable t) {
                Toast.makeText(SubscriptionsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void unsubscribe() {
        String topic = etTopic.getText().toString().trim();
        if (topic.isEmpty()) { etTopic.setError("Topic requerido"); return; }
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<java.util.Map<String,Object>> call = api.postUnsubscribe(topic);
        call.enqueue(new Callback<java.util.Map<String,Object>>() {
            @Override
            public void onResponse(Call<java.util.Map<String,Object>> call, Response<java.util.Map<String,Object>> response) {
                Toast.makeText(SubscriptionsActivity.this, "Unsubscribe: " + response.code(), Toast.LENGTH_SHORT).show();
                com.meteuapp.mqtt.MqttManager.getInstance(SubscriptionsActivity.this).unsubscribe(topic);
                refresh();
            }

            @Override
            public void onFailure(Call<java.util.Map<String,Object>> call, Throwable t) {
                Toast.makeText(SubscriptionsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void refresh() {
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<List<String>> call = api.getSubscriptions();
        tvList.setText("Cargando...\n");
        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (!response.isSuccessful() || response.body() == null) { tvList.setText("Error cargando"); return; }
                StringBuilder b = new StringBuilder();
                for (String s : response.body()) { b.append(s).append("\n"); }
                tvList.setText(b.toString());
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                tvList.setText("Error: " + t.getMessage());
            }
        });
    }
}
