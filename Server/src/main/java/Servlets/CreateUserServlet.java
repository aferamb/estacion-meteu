package Servlets;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import Database.UserDAO;

@WebServlet("/admin/users/create")
public class CreateUserServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String role = req.getParameter("role");
        if (username == null || password == null || role == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "username,password,role required");
            return;
        }
        boolean ok = UserDAO.createUser(username, password.toCharArray(), role);
        if (ok) resp.setStatus(HttpServletResponse.SC_CREATED);
        else resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
