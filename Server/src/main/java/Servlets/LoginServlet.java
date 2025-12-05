package Servlets;

import Database.UserDAO;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/login.html").forward(req, resp);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String user = req.getParameter("username");
        String pass = req.getParameter("password");
        if (user != null && pass != null && UserDAO.validateCredentials(user, pass.toCharArray())) {
            // Prevent session fixation: create new session
            HttpSession old = req.getSession(false);
            if (old != null) old.invalidate();
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);
            resp.sendRedirect(req.getContextPath() + "/index.html");    // Redirect to home
        } else {
            resp.sendRedirect(req.getContextPath() + "/login.html?error=1");
        }
    }
}
