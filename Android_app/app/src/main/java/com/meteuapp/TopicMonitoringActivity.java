package com.meteuapp;

import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.meteuapp.adapters.DynamicTableAdapter;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TopicMonitoringActivity extends AppBaseActivity {

    private Spinner spSaved;
    private EditText etCustom;
    private Button btnConnect, btnDisconnect;
    private TextView tvStatus;
    private androidx.recyclerview.widget.RecyclerView rv;
    private LinearLayout headerDynamic;

    private MqttClient client;
    private DynamicTableAdapter adapter;
    private List<String> currentColumns = new ArrayList<>();
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_monitoring);

        spSaved = findViewById(R.id.sp_saved_topics);
        etCustom = findViewById(R.id.et_custom_topic);
        btnConnect = findViewById(R.id.btn_connect_topic);
        btnDisconnect = findViewById(R.id.btn_disconnect_topic);
        tvStatus = findViewById(R.id.tv_monitoring_status);
        rv = findViewById(R.id.rv_topic_messages);
        headerDynamic = findViewById(R.id.header_dynamic);

        adapter = new DynamicTableAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        loadSavedTopics();

        btnConnect.setOnClickListener(v -> startMonitor());
        btnDisconnect.setOnClickListener(v -> stopMonitor());
    }

    private void loadSavedTopics() {
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<List<String>> call = api.getSubscriptions();
        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                List<String> topics = response.body();
                runOnUiThread(() -> spSaved.setAdapter(new ArrayAdapter<>(TopicMonitoringActivity.this, android.R.layout.simple_spinner_dropdown_item, topics)));
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                Log.w("meteu", "Could not load saved topics: " + t.getMessage());
            }
        });
    }

    private void startMonitor() {
        String topic = etCustom.getText().toString().trim();
        if (topic.isEmpty()) {
            Object sel = spSaved.getSelectedItem();
            if (sel == null) {
                Toast.makeText(this, "Selecciona o introduce un topic", Toast.LENGTH_SHORT).show();
                return;
            }
            topic = sel.toString();
        }
        connectAndSubscribe(topic);
    }

    private void connectAndSubscribe(String topic) {
        try {
            String broker = "tcp://192.168.2.156:1883"; // default broker; consider making configurable
            client = new MqttClient(broker, MqttClient.generateClientId(), new MemoryPersistence());
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setCleanSession(true);
            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) { updateStatus("Conectado"); }

                @Override
                public void connectionLost(Throwable cause) { updateStatus("Conexión perdida"); }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    handleIncomingPayload(payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) { }
            });
            client.connect(opts);
            client.subscribe(topic, (t, msg) -> { String payload = new String(msg.getPayload()); handleIncomingPayload(payload); });
            updateStatus("Suscrito a: " + topic);
        } catch (MqttException e) {
            Log.e("meteu","MQTT error: "+e.getMessage());
            updateStatus("Error MQTT: " + e.getMessage());
        }
    }

    @UiThread
    private void handleIncomingPayload(String payload) {
        try {
            Type type = new TypeToken<Map<String,Object>>(){}.getType();
            Map<String,Object> map = gson.fromJson(payload, type);
            if (map == null) return;
            // Flatten nested structures: if values are maps, convert to key.subkey
            Map<String,Object> flat = new LinkedHashMap<>();
            for (Map.Entry<String,Object> e : map.entrySet()) {
                Object v = e.getValue();
                if (v instanceof Map) {
                    Map sub = (Map) v;
                    for (Object sk : sub.keySet()) {
                        Object sv = sub.get(sk);
                        flat.put(e.getKey() + "." + sk, sv);
                    }
                } else {
                    flat.put(e.getKey(), v);
                }
            }
            List<String> cols = new ArrayList<>(flat.keySet());
            // update UI on main thread: header, adapter columns and new row
            runOnUiThread(() -> {
                try {
                    if (!cols.equals(currentColumns)) {
                        currentColumns = cols;
                        rebuildHeader(cols);
                        adapter.setColumns(cols);
                    }
                    adapter.addRow(flat);
                } catch (Exception e) {
                    Log.w("meteu", "UI update error: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.w("meteu","Error parsing incoming payload: " + e.getMessage());
        }
    }

    private void rebuildHeader(List<String> cols) {
        headerDynamic.removeAllViews();
        int minDp = 120;
        int minPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minDp, getResources().getDisplayMetrics());
        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        for (String c : cols) {
            TextView tv = new TextView(this);
            tv.setText(c);
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f);
            tv.setMinWidth(minPx);
            tv.setPadding(pad, 0, pad, 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(pad, 0, 0, 0);
            tv.setLayoutParams(lp);
            headerDynamic.addView(tv);
        }
    }

    private void updateStatus(String s) {
        runOnUiThread(() -> tvStatus.setText("Estado: " + s));
    }

    private void stopMonitor() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                updateStatus("desconectado");
            }
        } catch (MqttException e) {
            Log.w("meteu","Error disconnecting: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMonitor();
    }
}
