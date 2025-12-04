package Servlets;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import Database.ConectionDDBB;
import Logic.Log;

@WebServlet("/Health")
public class Health extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public Health() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        Map<String, Object> result = new HashMap<>();
        Connection con = null;
        try {
            ConectionDDBB conector = new ConectionDDBB();
            con = conector.obtainConnection(true);
            if (con != null && !con.isClosed()) {
                result.put("status", "ok");
                result.put("db", "connected");
            } else {
                result.put("status", "error");
                result.put("db", "not_connected");
            }
        } catch (Exception e) {
            Log.log.error("Health check error: " + e);
            result.put("status", "error");
            result.put("message", e.toString());
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException ignore) {}
            out.println(new Gson().toJson(result));
            out.close();
        }
    }
}
