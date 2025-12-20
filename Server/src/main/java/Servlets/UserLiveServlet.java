package Servlets;

import Database.ConectionDDBB;
import Logic.Log;
import com.google.gson.Gson;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserLiveServlet provides live data for authenticated (non-admin) users.
 * It returns JSON and does not redirect to HTML pages.
 */
@WebServlet("/api/live")
public class UserLiveServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;
        Map<String,Object> resp = new HashMap<>();
        try {
            con = conector.obtainConnection(true);

            // recent messages - include the extended set of fields so user view matches admin view
            PreparedStatement ps = con.prepareStatement("SELECT sensor_id, sensor_type, street_id, recorded_at, latitude, longitude, altitude, district, neighborhood, temp, humid, aqi, lux, sound_db, atmhpa, uv_index FROM sensor_readings ORDER BY recorded_at DESC LIMIT 30");
            ResultSet rs = ps.executeQuery();
            List<Map<String,Object>> msgs = new ArrayList<>();
            while (rs.next()) {
                Map<String,Object> m = new HashMap<>();
                m.put("sensor_id", rs.getString("sensor_id"));
                m.put("sensor_type", rs.getString("sensor_type"));
                m.put("street_id", rs.getString("street_id"));
                Timestamp ts = rs.getTimestamp("recorded_at");
                m.put("recorded_at", ts == null ? null : ts.toString());
                m.put("latitude", rs.getObject("latitude") == null ? null : rs.getDouble("latitude"));
                m.put("longitude", rs.getObject("longitude") == null ? null : rs.getDouble("longitude"));
                m.put("altitude", rs.getObject("altitude") == null ? null : rs.getDouble("altitude"));
                m.put("district", rs.getString("district"));
                m.put("neighborhood", rs.getString("neighborhood"));
                m.put("temp", rs.getObject("temp") == null ? null : rs.getDouble("temp"));
                m.put("humid", rs.getObject("humid") == null ? null : rs.getDouble("humid"));
                m.put("aqi", rs.getObject("aqi") == null ? null : rs.getInt("aqi"));
                m.put("lux", rs.getObject("lux") == null ? null : rs.getDouble("lux"));
                m.put("sound_db", rs.getObject("sound_db") == null ? null : rs.getDouble("sound_db"));
                m.put("atmhpa", rs.getObject("atmhpa") == null ? null : rs.getDouble("atmhpa"));
                m.put("uv_index", rs.getObject("uv_index") == null ? null : rs.getDouble("uv_index"));
                msgs.add(m);
            }

            // stations seen in the last 60 seconds
            PreparedStatement ps2 = con.prepareStatement("SELECT sensor_id, MAX(recorded_at) AS last_seen FROM sensor_readings WHERE recorded_at >= (NOW() - INTERVAL 60 SECOND) GROUP BY sensor_id");
            ResultSet rs2 = ps2.executeQuery();
            List<Map<String,Object>> stations = new ArrayList<>();
            while (rs2.next()) {
                Map<String,Object> s = new HashMap<>();
                s.put("sensor_id", rs2.getString("sensor_id"));
                Timestamp t = rs2.getTimestamp("last_seen");
                s.put("last_seen", t == null ? null : t.toString());
                stations.add(s);
            }

            resp.put("messages", msgs);
            resp.put("stations", stations);

        } catch (Exception e) {
            Log.log.error("Error building live data: {}", e);
            resp.put("error", e.toString());
        } finally {
            conector.closeConnection(con);
            out.println(gson.toJson(resp));
            out.close();
        }
    }
}
