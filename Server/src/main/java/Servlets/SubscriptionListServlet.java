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
import java.util.List;

@WebServlet({"/admin/subscriptions", "/api/subscriptions"})
public class SubscriptionListServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        SubscriptionDAO dao = new SubscriptionDAO();
        List<String> topics = dao.getAllActiveTopics();
        out.println(new Gson().toJson(topics));
    }
}
