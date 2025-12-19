package com.meteuapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PublishAlertActivity extends AppBaseActivity {

    private EditText etTopic, etSubscription, etAlert, etMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_alert);

        etTopic = findViewById(R.id.et_topic);
        etSubscription = findViewById(R.id.et_subscription);
        etAlert = findViewById(R.id.et_alert);
        etMessage = findViewById(R.id.et_message);
        Button btn = findViewById(R.id.btn_publish);

        btn.setOnClickListener(v -> publish());
    }

    private void publish() {
        String topic = etTopic.getText().toString().trim();
        String subscription = etSubscription.getText().toString().trim();
        String alert = etAlert.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        if ((topic.isEmpty() && subscription.isEmpty()) || (alert.isEmpty() && message.isEmpty())) {
            Toast.makeText(this, "Proporciona topic o subscription y alerta o mensaje", Toast.LENGTH_LONG).show();
            return;
        }

        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<java.util.Map<String,Object>> call = api.publishAlert(topic.isEmpty()?null:topic, subscription.isEmpty()?null:subscription, alert.isEmpty()?null:alert, message.isEmpty()?null:message);
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
