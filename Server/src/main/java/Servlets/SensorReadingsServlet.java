package Servlets;

import com.google.gson.Gson;
import Database.ConectionDDBB;
import Logic.Log;

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
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/sensor/readings")
public class SensorReadingsServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String sensorId = request.getParameter("sensor_id");
        String streetId = request.getParameter("street_id");
        String start = request.getParameter("start");
        String end = request.getParameter("end");
        String limitStr = request.getParameter("limit");
        int limit = 100;
        try { if (limitStr != null) limit = Integer.parseInt(limitStr); } catch (Exception e) {}

        ConectionDDBB conector = new ConectionDDBB();
        Connection con = null;
        try {
            con = conector.obtainConnection(true);
            StringBuilder sql = new StringBuilder("SELECT * FROM sensor_readings WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (sensorId != null && !sensorId.isEmpty()) { sql.append(" AND sensor_id = ?"); params.add(sensorId); }
            if (streetId != null && !streetId.isEmpty()) { sql.append(" AND street_id = ?"); params.add(streetId); }
            if (start != null && !start.isEmpty()) {
                // require strict param format yyyy-MM-ddHH:mm:ss (e.g. 2025-12-0504:02:33)
                try {
                    java.sql.Timestamp ts = Database.SensorReadingDAO.parseParamTimestampStrict(start);
                    sql.append(" AND recorded_at >= ?"); params.add(ts);
                } catch (IllegalArgumentException iae) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println("{\"error\":\"Formato de fecha inválido. Use yyyy-MM-ddHH:mm:ss\"}");
                    return;
                }
            }
            if (end != null && !end.isEmpty()) {
                try {
                    java.sql.Timestamp ts = Database.SensorReadingDAO.parseParamTimestampStrict(end);
                    sql.append(" AND recorded_at <= ?"); params.add(ts);
                } catch (IllegalArgumentException iae) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println("{\"error\":\"Formato de fecha inválido. Use yyyy-MM-ddHH:mm:ss\"}");
                    return;
                }
            }
            sql.append(" ORDER BY recorded_at DESC LIMIT ").append(limit);

            PreparedStatement ps = con.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) ps.setObject(i+1, params.get(i));

            ResultSet rs = ps.executeQuery();
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int c = 1; c <= cols; c++) {
                    Object v = rs.getObject(c);
                    String col = md.getColumnName(c);
                    if (v != null && col != null && col.equalsIgnoreCase("recorded_at") && v instanceof java.sql.Timestamp) {
                        row.put(col, Database.SensorReadingDAO.formatTimestamp((java.sql.Timestamp) v));
                    } else {
                        row.put(col, v);
                    }
                }
                rows.add(row);
            }
            out.println(new Gson().toJson(rows));
        } catch (Exception e) {
            Log.log.error("Error querying sensor_readings: {}", e);
            out.println("[]");
        } finally {
            conector.closeConnection(con);
            out.close();
        }
    }
}
