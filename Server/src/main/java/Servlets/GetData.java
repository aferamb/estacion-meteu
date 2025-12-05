package Servlets;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import Logic.Log;
import Logic.Logic;
import Logic.Measurement;

/**
 * Servlet implementation class GetData
 */
@WebServlet("/GetData")
public class GetData extends HttpServlet {
	private static final long serialVersionUID = 1L;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetData() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Log.log.info("--Set new value into the DB--");
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try 
        {
            ArrayList<Measurement> values = Logic.getDataFromDB();
            // convert Measurement objects to simple maps and format timestamps
            java.util.List<java.util.Map<String,Object>> outList = new java.util.ArrayList<>();
            for (Measurement m : values) {
                java.util.Map<String,Object> map = new java.util.HashMap<>();
                map.put("value", m.getValue());
                map.put("date", Database.SensorReadingDAO.formatTimestamp(m.getDate()));
                outList.add(map);
            }
            String jsonMeasurements = new Gson().toJson(outList);
            Log.log.info("Values=>" + jsonMeasurements);
            out.println(jsonMeasurements);
        } catch (NumberFormatException nfe) 
        {
            out.println("-1");
            Log.log.error("Number Format Exception: " + nfe);
        } catch (IndexOutOfBoundsException iobe) 
        {
            out.println("-1");
            Log.log.error("Index out of bounds Exception: " + iobe);
        } catch (Exception e) 
        {
            out.println("-1");
            Log.log.error("Exception: " + e);
        } finally 
        {
            out.close();
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}