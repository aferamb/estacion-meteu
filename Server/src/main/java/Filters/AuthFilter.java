package Filters;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.annotation.WebFilter;
import Database.UserDAO;
import Logic.Log;

@WebFilter("/*")
public class AuthFilter implements Filter {
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) req;
        HttpServletResponse resp = (HttpServletResponse) res;
        String path = r.getRequestURI().substring(r.getContextPath().length());
        // public routes
        if (path.startsWith("/login") || path.startsWith("/login.html") || path.startsWith("/css/") || path.startsWith("/js/")) {
            chain.doFilter(req, res);
            return;
        }
        HttpSession s = r.getSession(false);
        // check Authorization: Bearer <token> first
        String auth = r.getHeader("Authorization");
        if (auth != null && auth.toLowerCase().startsWith("bearer ")) {
            String token = auth.substring(7).trim();
            String subject = Utils.JwtUtil.validateTokenAndGetSubject(token);
            if (subject != null) {
                // create a session for compatibility with existing servlets
                HttpSession newS = r.getSession(true);
                newS.setAttribute("user", subject);
                s = newS;
            }
        }

        if (s != null && s.getAttribute("user") != null) {
            // if accessing /admin/* enforce admin role
            String user = (String) s.getAttribute("user");
            if (path.startsWith("/admin")) {
                try {
                    String role = UserDAO.getUserRole(user);
                    if (role == null || !role.equalsIgnoreCase("admin")) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                } catch (Exception e) {
                    Log.log.error("AuthFilter error checking role: {}", e);
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            }
            chain.doFilter(req, res);
        } else {
            resp.sendRedirect(r.getContextPath() + "/login.html");
        }
    }

    public void init(FilterConfig f) {}
    public void destroy() {}
}
