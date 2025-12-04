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

@WebServlet("/GetDataByDate")
public class GetDataByDate extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public GetDataByDate() { super(); }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Log.log.info("--Get Data by date--");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            String dateParam = request.getParameter("date"); // expected YYYY-MM-DD
            if (dateParam == null || dateParam.isEmpty()) {
                out.println("[]");
                return;
            }
            ArrayList<Measurement> values = Logic.getDataFromDate(dateParam);
            String jsonMeasurements = new Gson().toJson(values);
            Log.log.info("Values for date=" + dateParam + " =>" + jsonMeasurements);
            out.println(jsonMeasurements);
        } catch (Exception e) {
            Log.log.error("Exception: " + e);
            out.println("[]");
        } finally {
            out.close();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
