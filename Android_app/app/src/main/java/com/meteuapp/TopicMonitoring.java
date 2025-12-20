package com.meteuapp;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class TopicMonitoring extends AppBaseActivity {
    private String street_id;
    private String street_name;
    private MqttClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_street_monitoring);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            street_id = extras.getString("street_id");
            street_name = extras.getString("street_name");
            Log.i("meteu", "ID:" + street_id + " Nombre:" + street_name);
            conectarMqtt("sensors/" + street_id);
        }
    }

    private void conectarMqtt(String topic)
    {
        try {
            client = new MqttClient(
                    "tcp://192.168.2.156:1883",
                    MqttClient.generateClientId(),
                    new MemoryPersistence()
            );

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            Log.i("meteu", "Conectando al broker...");
            client.connect(options);
            Log.i("meteu", "Conectado ✔");

            client.subscribe(topic, (receivedTopic, message) -> {
                String msg = new String(message.getPayload());
                Log.i("meteu", "[" + receivedTopic + "] " + msg);
            });

            Log.i("meteu", "Suscrito a: " + topic);

        } catch (MqttException e) {
            Log.i("meteu", "Error MQTT: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                Log.i("meteu","Desconectado del broker");
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
