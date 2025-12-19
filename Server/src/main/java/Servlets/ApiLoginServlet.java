package Servlets;

import Database.UserDAO;
import Logic.Log;
import Utils.JwtUtil;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/api/login")
public class ApiLoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String user = req.getParameter("username");
        String pass = req.getParameter("password");
        if (user == null || pass == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (UserDAO.isDisabled(user)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        boolean valid = UserDAO.validateCredentials(user, pass.toCharArray());
        if (!valid) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try { UserDAO.recordSuccessfulLogin(user); } catch (Exception e) { Log.log.error("Error recording successful login: {}", e); }

        // create a server session for compatibility (optional)
        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = req.getSession(true);
        session.setAttribute("user", user);

        // issue JWT for API clients
        String token = JwtUtil.generateToken(user);
        resp.setContentType("application/json; charset=utf-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        PrintWriter out = resp.getWriter();
        out.print('{');
        out.print("\"token\":\"" + token + "\"");
        out.print('}');
        out.flush();
    }
}
