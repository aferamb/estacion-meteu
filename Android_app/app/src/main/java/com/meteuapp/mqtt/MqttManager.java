package com.meteuapp.mqtt;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.meteuapp.RetrofitClient;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.HashSet;
import java.util.Set;

/**
 * MqttManager: singleton that manages connection to an MQTT broker,
 * allows subscribing/unsubscribing, and notifies registered listeners
 * about incoming messages. Also posts simple notifications for messages.
 */
public class MqttManager {

    private static final String TAG = "MqttManager";
    private static MqttManager instance;

    private final Context ctx;
    private MqttClient client;
    private final Set<MqttListener> listeners = new HashSet<>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean connecting = false;

    public interface MqttListener {
        void onMessage(String topic, String payload);
        void onConnected();
        void onDisconnected();
    }

    private MqttManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public static synchronized MqttManager getInstance(Context ctx) {
        if (instance == null) instance = new MqttManager(ctx);
        return instance;
    }

    /**
     * Connect to broker if not connected. Non-blocking.
     */
    public synchronized void connect() {
        if (client != null && client.isConnected()) return;
        if (connecting) return;
        connecting = true;

        new Thread(() -> {
            try {
                String broker = RetrofitClient.MQTT_BROKER_URL; // e.g. tcp://192.168.2.156:1883
                client = new MqttClient(broker, MqttClient.generateClientId(), new MemoryPersistence());

                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);

                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        Log.i(TAG, "MQTT connection lost: " + cause);
                        main.post(() -> {
                            for (MqttListener l : listeners) l.onDisconnected();
                        });
                        // try reconnect after delay
                        reconnectWithDelay();
                    }

                    @Override
                    public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) throws Exception {
                        String payload = new String(message.getPayload());
                        Log.i(TAG, "MQTT msg: " + topic + " -> " + payload);
                        main.post(() -> {
                            for (MqttListener l : listeners) l.onMessage(topic, payload);
                            showNotification(topic, payload);
                        });
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) { }
                });

                client.connect(options);
                Log.i(TAG, "MQTT connected");
                main.post(() -> { for (MqttListener l : listeners) l.onConnected(); });
            } catch (MqttException e) {
                Log.e(TAG, "Error connecting MQTT: ", e);
                reconnectWithDelay();
            } finally {
                connecting = false;
            }
        }).start();
    }

    private void reconnectWithDelay() {
        main.postDelayed(this::connect, 5000);
    }

    public synchronized void subscribe(String topic) {
        connect();
        new Thread(() -> {
            try {
                if (client != null && client.isConnected()) {
                    client.subscribe(topic);
                    Log.i(TAG, "Subscribed to " + topic);
                }
            } catch (MqttException e) {
                Log.e(TAG, "Error subscribing: ", e);
            }
        }).start();
    }

    public synchronized void unsubscribe(String topic) {
        new Thread(() -> {
            try {
                if (client != null && client.isConnected()) {
                    client.unsubscribe(topic);
                    Log.i(TAG, "Unsubscribed from " + topic);
                }
            } catch (MqttException e) {
                Log.e(TAG, "Error unsubscribing: ", e);
            }
        }).start();
    }

    public synchronized void publish(String topic, String payload) {
        new Thread(() -> {
            try {
                if (client == null || !client.isConnected()) connect();
                if (client != null && client.isConnected()) {
                    client.publish(topic, payload.getBytes(), 0, false);
                }
            } catch (MqttException e) {
                Log.e(TAG, "Error publishing: ", e);
            }
        }).start();
    }

    public void addListener(MqttListener l) { listeners.add(l); }
    public void removeListener(MqttListener l) { listeners.remove(l); }

    public void disconnect() {
        new Thread(() -> {
            try {
                if (client != null && client.isConnected()) client.disconnect();
            } catch (MqttException e) { Log.e(TAG, "Error disconnecting MQTT: ", e); }
            client = null;
            main.post(() -> { for (MqttListener l : listeners) l.onDisconnected(); });
        }).start();
    }

    private void showNotification(String topic, String payload) {
        try {
            String channelId = "mqtt_messages";
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    NotificationChannel ch = new NotificationChannel(channelId, "MQTT messages", NotificationManager.IMPORTANCE_DEFAULT);
                    nm.createNotificationChannel(ch);
                }
                NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, channelId)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("MQTT: " + topic)
                        .setContentText(payload)
                        .setAutoCancel(true);
                nm.notify((int) System.currentTimeMillis(), b.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: ", e);
        }
    }
}
