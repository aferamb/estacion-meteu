package Servlets;

import Database.RangeDAO;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/ranges")
public class RangeServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        List<Map<String,Object>> results = new ArrayList<>();
        try {
            // read all ranges
            Database.ConectionDDBB conector = new Database.ConectionDDBB();
            Connection con = conector.obtainConnection(true);
            PreparedStatement ps = con.prepareStatement("SELECT parameter, min_value, max_value FROM parameter_ranges");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String,Object> m = new HashMap<>();
                m.put("parameter", rs.getString("parameter"));
                Double min = rs.getObject("min_value") == null ? null : rs.getDouble("min_value");
                Double max = rs.getObject("max_value") == null ? null : rs.getDouble("max_value");
                m.put("min", min);
                m.put("max", max);
                results.add(m);
            }
            conector.closeConnection(con);
        } catch (Exception e) {
            Log.log.error("Error listing ranges: {}", e);
        }
        out.println(new Gson().toJson(results));
        out.close();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        Map<String,Object> resp = new HashMap<>();
        String parameter = request.getParameter("parameter");
        String minS = request.getParameter("min");
        String maxS = request.getParameter("max");
        if (parameter == null || parameter.isEmpty()) {
            resp.put("ok", false);
            resp.put("error", "missing parameter");
            out.println(new Gson().toJson(resp));
            return;
        }
        Double min = null; Double max = null;
        try { if (minS != null && !minS.isEmpty()) min = Double.parseDouble(minS); } catch (Exception ignored) {}
        try { if (maxS != null && !maxS.isEmpty()) max = Double.parseDouble(maxS); } catch (Exception ignored) {}

        RangeDAO dao = new RangeDAO();
        boolean ok = dao.updateRange(parameter, min, max);
        resp.put("ok", ok);
        out.println(new Gson().toJson(resp));
        out.close();
    }
}
