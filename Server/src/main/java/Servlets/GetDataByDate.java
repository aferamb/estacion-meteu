package Servlets;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import Logic.Log;

@WebServlet("/GetDataByDate")
public class GetDataByDate extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public GetDataByDate() { super(); }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Log.log.info("--Get Data by date--");
        response.setContentType("application/json;charset=UTF-8");
        // This endpoint has been removed. Return 410 Gone.
        response.sendError(HttpServletResponse.SC_GONE, "Endpoint /GetDataByDate removed");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
