package com.box.example;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFolder;
import com.helpers.BoxHelper;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class LogoutServlet extends HttpServlet {
    private String domain;
    private String clientId;


    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        domain = config.getServletContext().getInitParameter("com.auth0.domain");
        clientId = config.getServletContext().getInitParameter("com.auth0.clientId");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        BoxHelper.revokeToken(request.getParameter("accessToken"));
        if (request.getSession() != null) {
            request.getSession().invalidate();
        }

        String returnUrl = String
                .format("%s://%s:%s%s%s", request.getScheme(),
                        request.getServerName(), request.getServerPort(),
                        request.getContextPath(), "/login");

        // Build logout URL like:
        // https://{YOUR-DOMAIN}/v2/logout?client_id={YOUR-CLIENT-ID}&returnTo=http://localhost:3000/login
        String logoutUrl = String.format(
                "https://%s/v2/logout?client_id=%s&returnTo=%s",
                domain,
                clientId,
                returnUrl
        );
        response.sendRedirect(logoutUrl);
    }
}