package Servlets;

import Database.UserDAO;
import Logic.Log;

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
        if (user == null || pass == null) {
            resp.sendRedirect(req.getContextPath() + "/login.html?error=1");
            return;
        }

        // check disabled
        if (UserDAO.isDisabled(user)) {
            resp.sendRedirect(req.getContextPath() + "/login.html?error=disabled");
            return;
        }

        boolean valid = UserDAO.validateCredentials(user, pass.toCharArray());
        if (valid) {
            // successful login: reset failed attempts & set last_login
            try { UserDAO.recordSuccessfulLogin(user); } catch (Exception e) { Log.log.error("Error recording successful login: {}", e); }
            // Prevent session fixation: create new session
            HttpSession old = req.getSession(false);
            if (old != null) old.invalidate();
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);
            resp.sendRedirect(req.getContextPath() + "/index.html");    // Redirect to home
            return;
        } else {
            // failed login: increment counter and possibly disable
            try {
                boolean disabled = UserDAO.recordFailedLogin(user, 5); // max attempts = 5
                if (disabled) {
                    resp.sendRedirect(req.getContextPath() + "/login.html?error=locked");
                    return;
                }
            } catch (Exception e) {
                Log.log.error("Error recording failed login: {}", e);
            }
            resp.sendRedirect(req.getContextPath() + "/login.html?error=1");
            return;
        }
    }
}
