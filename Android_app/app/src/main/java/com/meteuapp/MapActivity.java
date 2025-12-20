package com.meteuapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.meteuapp.mqtt.MqttManager;
import com.meteuapp.models.LiveResponse;
import com.meteuapp.models.SensorReading;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MapActivity: shows a map with markers for saved subscriptions.
 * It subscribes to each saved topic and listens for messages containing
 * latitude/longitude. Clicking a marker opens StationLiveActivity with
 * the topic preselected.
 *
 * Notes for the integrator:
 * - You must add a Google Maps API key in the AndroidManifest as:
 *   <meta-data android:name="com.google.android.geo.API_KEY" android:value="YOUR_API_KEY" />
 */
public class MapActivity extends AppBaseActivity implements MqttManager.MqttListener {

    private MapView map;
    private final Gson gson = new Gson();
    private final Map<String, Marker> topicMarkers = new HashMap<>();
    private boolean isUserInteracting = false;
    private boolean hasInitializedCenter = false;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable clearInteractionRunnable = () -> isUserInteracting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // osmdroid configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());
        map = findViewById(R.id.map_view);
        map.setMultiTouchControls(true);
        map.getController().setZoom(2.0);
        map.getController().setCenter(new GeoPoint(0,0));

        // detect user interaction to avoid auto-centering while the user is navigating the map
        map.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int a = event.getActionMasked();
                if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_MOVE) {
                    isUserInteracting = true;
                    uiHandler.removeCallbacks(clearInteractionRunnable);
                } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                    // keep interaction flag for a short grace period after release
                    uiHandler.postDelayed(clearInteractionRunnable, 3000);
                }
                return false;
            }
        });

        // connect MqttManager and register listener
        MqttManager mm = MqttManager.getInstance(this);
        mm.addListener(this);
        mm.connect();

        // subscribe to all cached subscriptions; if none cached, fetch from server
        String json = getSharedPreferences("meteu_prefs", MODE_PRIVATE).getString("cached_subscriptions", null);
        if (json == null) {
            // fetch from server
            ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
            Call<List<String>> call = api.getSubscriptions();
            call.enqueue(new Callback<List<String>>() {
                @Override
                public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        Toast.makeText(MapActivity.this, "No hay suscripciones guardadas", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<String> topics = response.body();
                    // cache for later
                    try { getSharedPreferences("meteu_prefs", MODE_PRIVATE).edit().putString("cached_subscriptions", gson.toJson(topics)).apply(); } catch (Exception ignored) {}
                    subscribeAndPopulate(topics);
                }

                @Override
                public void onFailure(Call<List<String>> call, Throwable t) {
                    Toast.makeText(MapActivity.this, "Error cargando suscripciones", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            try {
                Type t = new TypeToken<List<String>>(){}.getType();
                List<String> topics = gson.fromJson(json, t);
                subscribeAndPopulate(topics);
            } catch (Exception e) {
                Log.w("meteu","Error parsing cached subscriptions: " + e.getMessage());
            }
        }

        // also load recent live readings/stations from server to seed markers
        preloadLiveMarkers();
    }

    private void subscribeAndPopulate(List<String> topics) {
        if (topics == null || topics.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(this, "No hay suscripciones guardadas", Toast.LENGTH_SHORT).show());
            return;
        }
        MqttManager mm = MqttManager.getInstance(this);
        for (String top : topics) {
            try { mm.subscribe(top); } catch (Exception ignored) {}
            // try to infer sensor_id from topic and fetch last reading
            String sensorId = inferSensorIdFromTopic(top);
            if (sensorId != null && !sensorId.isEmpty()) fetchLastReadingForSensor(sensorId, top);
        }
    }

    private String inferSensorIdFromTopic(String topic) {
        if (topic == null) return null;
        String[] parts = topic.split("/");
        String cand = parts[parts.length - 1];
        // simple heuristic: if last segment contains letters/digits and not spaces
        if (cand != null && cand.matches("[A-Za-z0-9_-]+")) return cand;
        return null;
    }

    private void fetchLastReadingForSensor(String sensorId, String topic) {
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<java.util.List<SensorReading>> call = api.getSensorReadings(sensorId, null, null, null, 1);
        call.enqueue(new Callback<java.util.List<SensorReading>>() {
            @Override
            public void onResponse(Call<java.util.List<SensorReading>> call, Response<java.util.List<SensorReading>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) return;
                SensorReading r = response.body().get(0);
                if (r.getLatitude() != null && r.getLongitude() != null) {
                    runOnUiThread(() -> {
                        GeoPoint pos = new GeoPoint(r.getLatitude(), r.getLongitude());
                        Marker mk = new Marker(map);
                        mk.setPosition(pos);
                        mk.setTitle(topic);
                        mk.setRelatedObject(topic);
                        mk.setOnMarkerClickListener((marker, mapView) -> {
                            Intent i = new Intent(MapActivity.this, StationLiveActivity.class);
                            i.putExtra("topic", topic);
                            startActivity(i);
                            return true;
                        });
                        map.getOverlays().add(mk);
                        topicMarkers.put(topic, mk);
                    });
                }
            }

            @Override
            public void onFailure(Call<java.util.List<SensorReading>> call, Throwable t) { /* ignore */ }
        });
    }

    private void preloadLiveMarkers() {
        ServerApi api = RetrofitClient.getRetrofitInstance().create(ServerApi.class);
        Call<LiveResponse> call = api.getLiveApi();
        call.enqueue(new Callback<LiveResponse>() {
            @Override
            public void onResponse(Call<LiveResponse> call, Response<LiveResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                LiveResponse lr = response.body();
                if (lr.getMessages() != null) {
                    for (com.meteuapp.models.SensorMessage m : lr.getMessages()) {
                        double lat = m.getLatitudeSafe();
                        double lon = m.getLongitudeSafe();
                        if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
                            runOnUiThread(() -> {
                                GeoPoint pos = new GeoPoint(lat, lon);
                                String sensorId = m.getSensorIdSafe();
                                String streetId = null;
                                try { streetId = (String) (m.getClass().getMethod("getStreetIdSafe").invoke(m)); } catch (Exception ignored) { streetId = null; }
                                // compute full topic: prefer stored subscriptions matching sensorId, otherwise use pattern sensors/{street}/{sensor}
                                String fullTopic = null;
                                if (streetId != null && !streetId.isEmpty()) {
                                    fullTopic = "sensors/" + streetId + "/" + sensorId;
                                }
                                if (fullTopic == null || fullTopic.isEmpty()) {
                                    String json = getSharedPreferences("meteu_prefs", MODE_PRIVATE).getString("cached_subscriptions", null);
                                    if (json != null) {
                                        try {
                                            Type t = new TypeToken<List<String>>(){}.getType();
                                            List<String> subs = gson.fromJson(json, t);
                                            if (subs != null) {
                                                for (String s : subs) {
                                                    if (s != null && s.endsWith("/" + sensorId)) { fullTopic = s; break; }
                                                }
                                            }
                                        } catch (Exception ignored) {}
                                    }
                                }
                                if (fullTopic == null || fullTopic.isEmpty()) fullTopic = sensorId;

                                Marker mk = new Marker(map);
                                mk.setPosition(pos);
                                mk.setTitle(sensorId);
                                mk.setRelatedObject(fullTopic);
                                mk.setOnMarkerClickListener((marker, mapView) -> {
                                    Object related = marker.getRelatedObject();
                                    String topicToOpen = related instanceof String ? (String) related : sensorId;
                                    Intent i = new Intent(MapActivity.this, StationLiveActivity.class);
                                    i.putExtra("topic", topicToOpen);
                                    startActivity(i);
                                    return true;
                                });
                                map.getOverlays().add(mk);
                                topicMarkers.put(fullTopic, mk);
                            });
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<LiveResponse> call, Throwable t) { /* ignore */ }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }

    @Override
    public void onMessage(String topic, String payload) {
        try {
            Type type = new TypeToken<Map<String,Object>>(){}.getType();
            Map<String,Object> mapRaw = gson.fromJson(payload, type);
            if (mapRaw == null) return;
            Map<String,Object> flat = new LinkedHashMap<>();
            for (Map.Entry<String,Object> e : mapRaw.entrySet()) {
                Object v = e.getValue();
                if (v instanceof Map) {
                    Map sub = (Map) v;
                    for (Object sk : sub.keySet()) {
                        Object sv = sub.get(sk);
                        String skStr = String.valueOf(sk);
                        if (!flat.containsKey(skStr)) flat.put(skStr, sv);
                        flat.put(e.getKey() + "." + skStr, sv);
                    }
                } else {
                    flat.put(e.getKey(), v);
                }
            }

            // try common latitude/longitude keys
            Double lat = extractNumber(flat, "latitude", "lat", "gps.latitude", "location.latitude", "position.lat");
            Double lon = extractNumber(flat, "longitude", "long", "lng", "lon", "gps.longitude", "location.longitude", "position.lon");
            Double alt = extractNumber(flat, "altitude", "alt", "elevation");

            if (lat != null && lon != null) {
                runOnUiThread(() -> {
                    try {
                        GeoPoint pos = new GeoPoint(lat, lon);
                        Marker m = topicMarkers.get(topic);
                        if (m == null) {
                            Marker mk = new Marker(map);
                            mk.setPosition(pos);
                            mk.setTitle(topic);
                            mk.setOnMarkerClickListener((marker, mapView) -> {
                                Intent i = new Intent(MapActivity.this, StationLiveActivity.class);
                                i.putExtra("topic", topic);
                                startActivity(i);
                                return true;
                            });
                            map.getOverlays().add(mk);
                            topicMarkers.put(topic, mk);
                        } else {
                            m.setPosition(pos);
                        }
                        // center map behavior:
                        // - center & zoom the first time a marker is added
                        // - on subsequent updates, only center if the user is not interacting
                        if (!hasInitializedCenter) {
                            map.getController().setCenter(pos);
                            map.getController().setZoom(14.0);
                            hasInitializedCenter = true;
                        } else if (!isUserInteracting) {
                            map.getController().setCenter(pos);
                        }
                    } catch (Exception e) { Log.w("meteu","Error updating marker: " + e.getMessage()); }
                });
            }

        } catch (Exception e) {
            Log.w("meteu","Error parsing MQTT msg for map: " + e.getMessage());
        }
    }

    private Double extractNumber(Map<String,Object> flat, String... keys) {
        for (String k : keys) {
            if (flat.containsKey(k)) {
                Object v = flat.get(k);
                try {
                    if (v instanceof Number) return ((Number) v).doubleValue();
                    if (v instanceof String) return Double.parseDouble((String) v);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    @Override
    public void onConnected() { runOnUiThread(() -> Toast.makeText(this, "MQTT conectado (map)", Toast.LENGTH_SHORT).show()); }

    @Override
    public void onDisconnected() { runOnUiThread(() -> Toast.makeText(this, "MQTT desconectado (map)", Toast.LENGTH_SHORT).show()); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { MqttManager.getInstance(this).removeListener(this); } catch (Exception ignored) {}
    }
}
