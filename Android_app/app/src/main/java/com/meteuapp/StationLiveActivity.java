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

import com.meteuapp.mqtt.MqttManager;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

public class StationLiveActivity extends AppBaseActivity implements MqttManager.MqttListener {

    private LinearLayout container;
    private TextView tvStatus;
    private Button btnDisconnect;
    private String currentTopic;
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

        currentTopic = topic;
        // register to shared MqttManager and subscribe
        MqttManager mm = MqttManager.getInstance(this);
        mm.addListener(this);
        mm.connect();
        try { mm.subscribe(topic); } catch (Exception ignored) {}
        updateStatus("Suscrito: " + topic);

        btnDisconnect.setOnClickListener(v -> {
            // unregister listener and optionally unsubscribe
            try { MqttManager.getInstance(StationLiveActivity.this).removeListener(StationLiveActivity.this); } catch (Exception ignored) {}
            try { MqttManager.getInstance(StationLiveActivity.this).unsubscribe(currentTopic); } catch (Exception ignored) {}
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

    @Override
    public void onMessage(String topic, String payload) {
        // only handle messages for the current topic (exact or suffix match)
        try {
            if (currentTopic == null) return;
            if (topic.equals(currentTopic) || topic.endsWith(currentTopic)) {
                handleIncoming(payload);
            }
        } catch (Exception e) { Log.w("meteu","onMessage handler error: " + e.getMessage()); }
    }

    @Override
    public void onConnected() { updateStatus("Conectado"); }

    @Override
    public void onDisconnected() { updateStatus("Conexión perdida"); }

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
            // helper to search multiple possible keys
            java.util.function.Function<String[], Object> findFirst = (keys) -> {
                for (String k : keys) {
                    if (k == null) continue;
                    if (flat.containsKey(k)) {
                        Object v = flat.get(k);
                        if (v != null) return v;
                    }
                }
                return null;
            };

            // update cards with values for PARAMS
            runOnUiThread(() -> {
                for (String p : PARAMS) {
                    Object val = null;
                    switch (p) {
                        case "sensor_id":
                            val = findFirst.apply(new String[]{"sensor_id","id","sensor","device","sensor.id","meta.sensor_id"});
                            break;
                        case "sensor_type":
                            val = findFirst.apply(new String[]{"sensor_type","type","sensor.type"});
                            break;
                        case "street_id":
                            val = findFirst.apply(new String[]{"street_id","street","streetId","location.street"});
                            break;
                        case "timestamp":
                            val = findFirst.apply(new String[]{"timestamp","recorded_at","time","ts","t"});
                            break;
                        case "lat":
                            val = findFirst.apply(new String[]{"lat","latitude","gps.latitude","location.latitude","position.lat"});
                            break;
                        case "long":
                            val = findFirst.apply(new String[]{"long","longitude","lng","lon","gps.longitude","location.longitude","position.lon"});
                            break;
                        case "alt":
                            val = findFirst.apply(new String[]{"alt","altitude","elevation"});
                            break;
                        case "district":
                            val = findFirst.apply(new String[]{"district","area","region"});
                            break;
                        case "neighborhood":
                            val = findFirst.apply(new String[]{"neighborhood","neighbourhood","suburb"});
                            break;
                        default:
                            // measurement params: try exact name and common variants
                            val = findFirst.apply(new String[]{p, p.replace("_",""), p.replace("temp","temperature")});
                    }

                    TextView tv = cardMap.get(p);
                    if (tv != null) {
                        String display = "—";
                        if (val != null) {
                            if (val instanceof Number) {
                                // format decimals
                                display = String.format(java.util.Locale.getDefault(), "%.2f", ((Number) val).doubleValue());
                            } else {
                                display = String.valueOf(val);
                            }
                        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { MqttManager.getInstance(this).removeListener(this); } catch (Exception ignored) {}
    }
}
