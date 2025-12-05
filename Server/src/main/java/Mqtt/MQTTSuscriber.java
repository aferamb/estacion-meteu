package Mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import Logic.Log;
import Database.SensorReadingDAO;
import Database.SubscriptionDAO;
import Logic.AlarmManager;

import java.util.List;

public class MQTTSuscriber implements MqttCallback {

    private MqttClient client;
    private String brokerUrl;
    private String clientId;
    private String username;
    private String password;

    public MQTTSuscriber(MQTTBroker broker) {
        this.brokerUrl = broker.getBroker();
        this.clientId = broker.getClientId();
        this.username = broker.getUsername();
        this.password = broker.getPassword();
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(brokerUrl, MQTTBroker.getSubscriberClientId(), persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            connOpts.setCleanSession(false);
            connOpts.setAutomaticReconnect(true);
            connOpts.setConnectionTimeout(10);
            client.setCallback(this);
            client.connect(connOpts);
            Log.logmqtt.info("MQTT subscriber connected to {}", brokerUrl);

            // subscribe persisted subscriptions at startup
            try {
                SubscriptionDAO dao = new SubscriptionDAO();
                List<String> topics = dao.getAllActiveTopics();
                for (String t : topics) {
                    subscribeTopic(t);
                }
            } catch (Exception e) {
                Log.logmqtt.warn("Could not load persisted subscriptions: {}", e);
            }

        } catch (MqttException e) {
            Log.logmqtt.error("Error creating MQTT subscriber client: {}", e);
        }
    }

    public void subscribeTopic(String topic) {
        try {
            if (client == null || !client.isConnected()) {
                Log.logmqtt.warn("Client not connected, cannot subscribe to {}", topic);
                return;
            }
            client.subscribe(topic, 1);
            Log.logmqtt.info("Subscribed to {}", topic);
        } catch (MqttException e) {
            Log.logmqtt.error("Error subscribing to topic: {}", e);
        }
    }

    public void unsubscribeTopic(String topic) {
        try {
            if (client == null || !client.isConnected()) {
                Log.logmqtt.warn("Client not connected, cannot unsubscribe {}", topic);
                return;
            }
            client.unsubscribe(topic);
            Log.logmqtt.info("Unsubscribed from {}", topic);
        } catch (MqttException e) {
            Log.logmqtt.error("Error unsubscribing from topic: {}", e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.logmqtt.warn("MQTT Connection lost, cause: {}", cause == null ? "unknown" : cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = message.toString();
        Log.logmqtt.info("Message arrived on {} : {}", topic, payload);
        try {
            // Persist incoming payload to sensor_readings table
            boolean ok = SensorReadingDAO.insertFromTopicPayload(topic, payload);
            if (!ok) {
                Log.logmqtt.warn("Failed to persist sensor reading for topic {}", topic);
            }
            // After persisting, run alarm checks (compare values against configured ranges)
            try {
                AlarmManager.process(topic, payload);
            } catch (Exception e) {
                Log.logmqtt.error("Error running alarm manager: {}", e);
            }
        } catch (Exception e) {
            Log.logmqtt.error("Error handling incoming MQTT message: {}", e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}
