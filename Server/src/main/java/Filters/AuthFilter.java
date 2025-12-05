package Filters;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.annotation.WebFilter;

@WebFilter("/*")
public class AuthFilter implements Filter {
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) req;
        HttpServletResponse resp = (HttpServletResponse) res;
        String path = r.getRequestURI().substring(r.getContextPath().length());
        // public routes
        if (path.startsWith("/login") || path.startsWith("/login.html") || path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/index.html") || path.startsWith("/GetData") || path.startsWith("/GetDataByDate")) {
            chain.doFilter(req, res);
            return;
        }
        HttpSession s = r.getSession(false);
        if (s != null && s.getAttribute("user") != null) {
            chain.doFilter(req, res);
        } else {
            resp.sendRedirect(r.getContextPath() + "/login.html");
        }
    }

    public void init(FilterConfig f) {}
    public void destroy() {}
}
