package Servlets;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import Database.UserDAO;

@WebServlet("/admin/users/delete")
public class DeleteUserServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        if (username == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "username required");
            return;
        }
        boolean ok = UserDAO.deleteUser(username);
        if (ok) resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        else resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
