package Servlets;

import Database.QueryParams;
import Database.SensorReadingDAO;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import Logic.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@WebServlet("/api/readings/query")
public class ReadingsQueryServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String startS = request.getParameter("start");
            String endS = request.getParameter("end");
            String filter = request.getParameter("filter");
            String value = request.getParameter("value");
            String operator = request.getParameter("op");
            String sortBy = request.getParameter("sortBy");
            String order = request.getParameter("order");
            String limitS = request.getParameter("limit");
            String offsetS = request.getParameter("offset");

            QueryParams params = new QueryParams();

            try {
                if (startS != null && !startS.isEmpty()) params.setStart(SensorReadingDAO.parseParamTimestampStrict(startS));
                if (endS != null && !endS.isEmpty()) params.setEnd(SensorReadingDAO.parseParamTimestampStrict(endS));
            } catch (IllegalArgumentException iae) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("{\"error\":\"Formato de fecha inválido. Use yyyy-MM-ddHH:mm:ss\"}");
                return;
            }

            if (filter != null) params.setFilter(filter);
            if (value != null) params.setValue(value);
            if (operator != null) params.setOperator(operator);
            if (sortBy != null) params.setSortBy(sortBy);
            if (order != null) params.setOrder(order);

            int limit = 200;
            try { if (limitS != null) limit = Integer.parseInt(limitS); } catch (Exception ignored) {}
            if (limit <= 0) limit = 200; if (limit > 200) limit = 200;
            params.setLimit(limit);

            int offset = 0;
            try { if (offsetS != null) offset = Integer.parseInt(offsetS); } catch (Exception ignored) {}
            if (offset < 0) offset = 0;
            params.setOffset(offset);

            List<Map<String, Object>> rows = SensorReadingDAO.queryReadings(params);
            out.println(new Gson().toJson(rows));
        } catch (IllegalArgumentException e) {
            Log.log.warn("Bad request in readings query: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            Log.log.error("Error in ReadingsQueryServlet: {}", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("{\"error\":\"internal error\"}");
        } finally {
            out.close();
        }
    }
}
