package Servlets;

import Database.SubscriptionDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import Mqtt.MQTTSuscriber;

@WebServlet("/admin/subscribe")
public class SubscribeServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String topic = request.getParameter("topic");
        Map<String, Object> resp = new HashMap<>();
        if (topic == null || topic.isEmpty()) {
            resp.put("ok", false);
            resp.put("error", "missing topic parameter");
            out.println(new Gson().toJson(resp));
            return;
        }
        SubscriptionDAO dao = new SubscriptionDAO();
        boolean ok = dao.addSubscription(topic);
        if (ok) {
            Object s = getServletContext().getAttribute("mqttSubscriber");
            if (s != null && s instanceof MQTTSuscriber) {
                ((MQTTSuscriber) s).subscribeTopic(topic);
            }
        }
        resp.put("ok", ok);
        out.println(new Gson().toJson(resp));
    }
}
