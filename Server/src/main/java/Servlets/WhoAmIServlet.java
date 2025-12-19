package Servlets;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import Database.UserDAO;

@WebServlet("/api/me")
public class WhoAmIServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String user = (String) s.getAttribute("user");
        String role = "";
        try { role = UserDAO.getUserRole(user); } catch (Exception ignored) {}

        resp.setContentType("application/json; charset=utf-8");
        PrintWriter out = resp.getWriter();
        out.print('{');
        out.print("\"username\":\"" + user + "\"");
        if (role != null && !role.isEmpty()) {
            out.print(','); out.print("\"role\":\"" + role + "\"");
        }
        out.print('}');
        out.flush();
    }
}
