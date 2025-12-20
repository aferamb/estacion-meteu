package com.meteuapp;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

public class StationLiveActivity extends AppBaseActivity {

    private LinearLayout container;
    private TextView tvStatus;
    private Button btnDisconnect;
    private MqttClient client;
    private final Gson gson = new Gson();
    // Keep map of parameter -> TextView for fast updates
    private final Map<String, TextView> cardMap = new LinkedHashMap<>();

    // parameters we will monitor (in this order)
    private static final String[] PARAMS = new String[]{
            "sensor_id","sensor_type","street_id","timestamp","lat","long","alt","district","neighborhood",
            "temp","humid","aqi","lux","sound_db","atmhpa","uv_index"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_live);
        container = findViewById(R.id.container_cards);
        tvStatus = findViewById(R.id.tv_station_status);
        btnDisconnect = findViewById(R.id.btn_disconnect_station);

        // build initial empty cards
        buildCards();

        String topic = getIntent().getStringExtra("topic");
        if (topic == null) {
            Toast.makeText(this, "No topic seleccionado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        connectAndSubscribe(topic);

        btnDisconnect.setOnClickListener(v -> {
            stopMonitor();
            finish();
        });
    }

    private void buildCards() {
        container.removeAllViews();
        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        for (String p : PARAMS) {
            TextView tv = new TextView(this);
            tv.setText(p + "\n—");
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            tv.setPadding(pad, pad, pad, pad);
            tv.setBackgroundResource(R.drawable.bg_card_round);
            tv.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, pad, 0, 0);
            tv.setLayoutParams(lp);
            container.addView(tv);
            cardMap.put(p, tv);
        }
    }

    private void connectAndSubscribe(String topic) {
        try {
            String broker = "tcp://192.168.2.156:1883"; // consider making configurable later
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
                    handleIncoming(payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) { }
            });
            client.connect(opts);
            client.subscribe(topic, (t, msg) -> handleIncoming(new String(msg.getPayload())));
            updateStatus("Suscrito: " + topic);
        } catch (MqttException e) {
            Log.e("meteu","MQTT error: "+e.getMessage());
            updateStatus("Error MQTT");
        }
    }

    private void handleIncoming(String payload) {
        try {
            Type type = new TypeToken<Map<String,Object>>(){}.getType();
            Map<String,Object> map = gson.fromJson(payload, type);
            if (map == null) return;
            // flatten: expose both parent.child and child keys so metrics nested under
            // objects like "data": {"temp":...} are reachable as "temp" as well.
            Map<String,Object> flat = new LinkedHashMap<>();
            for (Map.Entry<String,Object> e : map.entrySet()) {
                Object v = e.getValue();
                if (v instanceof Map) {
                    Map sub = (Map) v;
                    for (Object sk : sub.keySet()) {
                        Object sv = sub.get(sk);
                        String skStr = String.valueOf(sk);
                        // put plain child key (so "temp" is available) and parent.child
                        if (!flat.containsKey(skStr)) flat.put(skStr, sv);
                        flat.put(e.getKey() + "." + skStr, sv);
                    }
                } else {
                    flat.put(e.getKey(), v);
                }
            }
            // update cards with values for PARAMS
            runOnUiThread(() -> {
                for (String p : PARAMS) {
                    Object val = null;
                    // try direct keys and common variants
                    if (flat.containsKey(p)) val = flat.get(p);
                    else if (flat.containsKey(p.replace("long", "longitude"))) val = flat.get(p.replace("long", "longitude"));
                    else if (flat.containsKey(p.replace("lat", "latitude"))) val = flat.get(p.replace("lat", "latitude"));
                    TextView tv = cardMap.get(p);
                    if (tv != null) {
                        String display = val == null ? "—" : String.valueOf(val);
                        tv.setText(p + "\n" + display);
                    }
                }
            });
        } catch (Exception e) {
            Log.w("meteu","Error parsing payload: " + e.getMessage());
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
