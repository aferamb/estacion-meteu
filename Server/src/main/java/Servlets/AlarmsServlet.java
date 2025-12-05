package Servlets;

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

@WebServlet("/admin/alarms")
public class AlarmsServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        List<Map<String,Object>> results = new ArrayList<>();

        String sensorId = request.getParameter("sensor_id");
        String parameter = request.getParameter("parameter");
        String activeParam = request.getParameter("active"); // expected '1' or '0' or 'true'/'false'
        String limitParam = request.getParameter("limit");
        int limit = 200;
        try { if (limitParam != null) limit = Integer.parseInt(limitParam); } catch (Exception ignored) {}

        try {
            Database.ConectionDDBB conector = new Database.ConectionDDBB();
            Connection con = conector.obtainConnection(true);

            StringBuilder sql = new StringBuilder("SELECT id, sensor_id, street_id, parameter, triggered_value, triggered_at, resolved_at, resolved_value, active FROM sensor_alarms");
            List<Object> params = new ArrayList<>();
            boolean where = false;
            if (activeParam != null && !activeParam.isEmpty()) {
                if (!where) { sql.append(" WHERE "); where = true; } else { sql.append(" AND "); }
                if (activeParam.equalsIgnoreCase("true")) sql.append("active = 1");
                else if (activeParam.equalsIgnoreCase("false")) sql.append("active = 0");
                else {
                    // numeric
                    sql.append("active = ?");
                    try { params.add(Integer.parseInt(activeParam)); } catch (Exception ex) { params.add(1); }
                }
            }
            if (sensorId != null && !sensorId.isEmpty()) {
                if (!where) { sql.append(" WHERE "); where = true; } else { sql.append(" AND "); }
                sql.append("(sensor_id = ? OR (? IS NULL AND sensor_id IS NULL))");
                params.add(sensorId);
                params.add(sensorId);
            }
            if (parameter != null && !parameter.isEmpty()) {
                if (!where) { sql.append(" WHERE "); where = true; } else { sql.append(" AND "); }
                sql.append("parameter = ?");
                params.add(parameter);
            }

            sql.append(" ORDER BY triggered_at DESC LIMIT ?");
            params.add(limit);

            PreparedStatement ps = con.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p == null) ps.setNull(i+1, java.sql.Types.VARCHAR);
                else if (p instanceof Integer) ps.setInt(i+1, (Integer)p);
                else ps.setString(i+1, p.toString());
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String,Object> row = new HashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("sensor_id", rs.getString("sensor_id"));
                row.put("street_id", rs.getString("street_id"));
                row.put("parameter", rs.getString("parameter"));
                row.put("triggered_value", rs.getObject("triggered_value") == null ? null : rs.getDouble("triggered_value"));
                Timestamp t1 = rs.getTimestamp("triggered_at");
                row.put("triggered_at", t1 == null ? null : t1.toString());
                Timestamp t2 = rs.getTimestamp("resolved_at");
                row.put("resolved_at", t2 == null ? null : t2.toString());
                row.put("resolved_value", rs.getObject("resolved_value") == null ? null : rs.getDouble("resolved_value"));
                row.put("active", rs.getInt("active") == 1);
                results.add(row);
            }

            conector.closeConnection(con);
        } catch (Exception e) {
            Log.log.error("Error listing alarms: {}", e);
        }

        out.println(new Gson().toJson(results));
        out.close();
    }
}
