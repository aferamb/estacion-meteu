package Logic;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import Database.AlarmDAO;
import Database.Range;
import Database.RangeDAO;
import Database.SubscriptionDAO;
import Mqtt.MQTTBroker;
import Mqtt.MQTTPublisher;

import java.util.Map;

public class AlarmManager {

    private static final Gson gson = new Gson();

    public static void process(String topic, String payload) {
        try {
            JsonObject root = JsonParser.parseString(payload).getAsJsonObject();

            String sensorId = null;
            String streetId = null;
            if (root.has("sensor_id")) sensorId = root.get("sensor_id").getAsString();
            if (root.has("street_id")) streetId = root.get("street_id").getAsString();

            JsonObject data = null;
            if (root.has("data") && root.get("data").isJsonObject()) {
                data = root.getAsJsonObject("data");
            } else if (root.has("DATA") && root.get("DATA").isJsonObject()) {
                data = root.getAsJsonObject("DATA");
            }

            if (data == null) return; // nothing to check

            RangeDAO rangeDao = new RangeDAO();
            AlarmDAO alarmDao = new AlarmDAO();
            SubscriptionDAO subDao = new SubscriptionDAO();

            for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
                String param = entry.getKey();
                JsonElement el = entry.getValue();
                if (el == null || el.isJsonNull()) continue;
                Double value = null;
                try {
                    if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                        value = el.getAsDouble();
                    } else {
                        // skip non-numeric values
                        continue;
                    }
                } catch (Exception ex) {
                    continue;
                }

                Range r = rangeDao.getRangeForParameter(param);
                if (r == null) {
                    // no configured range: skip
                    continue;
                }

                boolean outOfRange = false;
                if (r.getMin() != null && value < r.getMin()) outOfRange = true;
                if (r.getMax() != null && value > r.getMax()) outOfRange = true;

                boolean hadActive = alarmDao.hasActiveAlarm(sensorId, param);

                if (outOfRange && !hadActive) {
                    // create alarm and send WTH001
                    alarmDao.createAlarm(sensorId, streetId, param, value);
                    sendAlertFor(topic, sensorId, param, value, "WTH001");
                } else if (!outOfRange && hadActive) {
                    // resolve alarm and send WTH002
                    alarmDao.resolveAlarm(sensorId, param, value);
                    sendAlertFor(topic, sensorId, param, value, "WTH002");
                }
            }

        } catch (Exception e) {
            Log.log.error("AlarmManager error processing payload: {}", e);
        }
    }

    private static void sendAlertFor(String subscriptionTopic, String sensorId, String parameter, Double value, String alertCode) {
        try {
            SubscriptionDAO subDao = new SubscriptionDAO();
            String alertTopic = subDao.getAlertTopicFor(subscriptionTopic);
            if (alertTopic == null || alertTopic.isEmpty()) {
                if (subscriptionTopic.endsWith("/")) alertTopic = subscriptionTopic + "alerts";
                else alertTopic = subscriptionTopic + "/alerts";
                subDao.updateAlertTopic(subscriptionTopic, alertTopic);
            }

            JsonObject msg = new JsonObject();
            msg.addProperty("alerta", alertCode);
            msg.addProperty("parameter", parameter);
            if (sensorId != null) msg.addProperty("sensor_id", sensorId);
            if (value != null) msg.addProperty("value", value);
            msg.addProperty("timestamp", System.currentTimeMillis());

            MQTTBroker broker = new MQTTBroker();
            MQTTPublisher.publish(broker, alertTopic, gson.toJson(msg));
        } catch (Exception e) {
            Log.log.error("Error sending alert {} for {}: {}", alertCode, parameter, e);
        }
    }
}
