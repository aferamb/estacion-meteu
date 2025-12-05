package Servlets;

import Mqtt.MQTTBroker;
import Mqtt.MQTTPublisher;
import Database.SubscriptionDAO;
import Logic.Log;
import com.google.gson.Gson;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/admin/publishAlert")
public class PublishAlertServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        Map<String,Object> resp = new HashMap<>();

        // Two modes: provide 'topic' directly, or provide 'subscription' (the saved topic)
        String topic = request.getParameter("topic");
        String subscription = request.getParameter("subscription");
        String alert = request.getParameter("alert");
        String message = request.getParameter("message");

        if ((topic == null || topic.isEmpty()) && (subscription == null || subscription.isEmpty())) {
            resp.put("ok", false);
            resp.put("error", "missing topic or subscription parameter");
            out.println(new Gson().toJson(resp));
            return;
        }

        try {
            SubscriptionDAO dao = new SubscriptionDAO();
            String publishTopic = topic;
            if (publishTopic == null || publishTopic.isEmpty()) {
                // lookup alert_topic for subscription
                publishTopic = dao.getAlertTopicFor(subscription);
                if (publishTopic == null || publishTopic.isEmpty()) {
                    // derive by appending /alerts
                    if (subscription.endsWith("/")) publishTopic = subscription + "alerts";
                    else publishTopic = subscription + "/alerts";
                    // persist the discovered alert topic
                    dao.updateAlertTopic(subscription, publishTopic);
                }
            }

            // Compose message JSON: if 'message' provided, use as-is; otherwise wrap 'alert' into {"alerta":...}
            String payload;
            if (message != null && !message.isEmpty()) {
                payload = message;
            } else if (alert != null && !alert.isEmpty()) {
                // build small JSON
                payload = new Gson().toJson(java.util.Collections.singletonMap("alerta", alert));
            } else {
                resp.put("ok", false);
                resp.put("error", "missing alert or message parameter");
                out.println(new Gson().toJson(resp));
                return;
            }

            // Publish via MQTTPublisher
            MQTTBroker broker = new MQTTBroker();
            MQTTPublisher.publish(broker, publishTopic, payload);

            // store alert_topic if not present (ensure DB has it)
            if (subscription != null && !subscription.isEmpty()) {
                dao.updateAlertTopic(subscription, publishTopic);
            }

            resp.put("ok", true);
            resp.put("topic", publishTopic);
            resp.put("payload", payload);
            out.println(new Gson().toJson(resp));
        } catch (Exception e) {
            Log.log.error("Error publishing alert: {}", e);
            resp.put("ok", false);
            resp.put("error", e.toString());
            out.println(new Gson().toJson(resp));
        } finally {
            out.close();
        }
    }
}
