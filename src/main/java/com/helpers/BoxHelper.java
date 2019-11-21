package com.helpers;

import com.auth0.SessionUtils;
import com.auth0.exception.APIException;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.UserInfo;
import com.auth0.net.Request;
import com.box.example.LoginServlet;
import com.box.sdk.*;
import com.box.sdk.http.HttpMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoxHelper {
    private static final Logger LOGGER = LogManager.getLogger(BoxHelper.class);
    private static final String BOX_CONFIG = "config.json";

    private static final String BOX_USER_ID_KEY = "box_id";
    private static final int MAX_CACHE_ENTRIES = 100;

    private static final String BOX_REVOKE_URL = "https://api.box.com/oauth2/revoke";

    private static IAccessTokenCache accessTokenCache;
    private static BoxConfig boxConfig;

    static {
        LOGGER.debug("Searching for file..");
        final String rootPath = Thread.currentThread()
                .getContextClassLoader().getResource("").getPath();
        Path configPath = Paths.get(String.join("", rootPath, BOX_CONFIG).substring(1));
        LOGGER.debug(configPath.toString());
        try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            boxConfig = BoxConfig.readFrom(reader);
            accessTokenCache = new InMemoryLRUAccessTokenCache(MAX_CACHE_ENTRIES);
        } catch (java.io.IOException e1) {
            e1.printStackTrace();
        }
    }

    public static BoxAPIConnection adminClient() {
        return BoxDeveloperEditionAPIConnection
                .getAppEnterpriseConnection(boxConfig, accessTokenCache);
    }

    public static BoxAPIConnection userClient(HttpServletRequest request, HttpServletResponse response) {
        String boxId = boxIdFromRequest(request);
        if (boxId == null) {
            try {
                response.sendRedirect(request.getContextPath() + "/login");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return userClient(boxId);
    }

    public static BoxAPIConnection userClient(String userId) {
        return BoxDeveloperEditionAPIConnection.getAppUserConnection(userId,
                boxConfig);
    }

    public static void revokeToken(String accessToken) {
        BoxAPIConnection adminClient = adminClient();
        URL url;
        try {
            url = new URL(BOX_REVOKE_URL);
            BoxAPIRequest request = new BoxAPIRequest(adminClient, url, HttpMethod.POST);
            LOGGER.debug(adminClient.getClientID());
            String body = String.format("client_id=%s&client_secret=%s&token=%s", adminClient.getClientID(),
                    adminClient.getClientSecret(), accessToken);
            request.addHeader("content-type", "application/x-www-form-urlencoded");
            request.setBody(body);
            request.send();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void prepareBoxUser(HttpServletRequest request) {

        String boxId = boxIdFromRequest(request);

        //if boxId is still not found that means we need
        // to create a Box app user and associate with the Auth0 user
        if (boxId == null) {
            String auth0Id = SessionUtils.get(request, "auth0UserId").toString();
            String auth0Email = SessionUtils.get(request, "auth0UserEmail").toString();

            CreateUserParams params = new CreateUserParams();
            params.setSpaceAmount(1073741824); //1 GB
            params.setExternalAppUserId(auth0Id);
            try {
                BoxUser.Info user = BoxUser.createAppUser(adminClient(), auth0Email, params);
                boxId = user.getID();
            } catch (BoxAPIException e) {
                if (e.getResponseCode() == 409) {
                    boxId = getIdFromConflict(e.getResponse());
                }
            }

            if (boxId == null) {
                throw new RuntimeException("Couldn't create or retrieve Box App User.");
            }

            SessionUtils.set(request, BOX_USER_ID_KEY, boxId);
            BoxAPIConnection boxUserClient = userClient(boxId);
            try {
                var nickname = Optional
                        .ofNullable((String)
                                SessionUtils.get(request, "userNickname"))
                        .orElse("Default");
                System.out.println(nickname);
                BoxFolder.getRootFolder(boxUserClient).createFolder(nickname);
            } catch (BoxAPIException e) {
                if (e.getResponseCode() == 409) {
                    LOGGER.debug("Existing Folder ID:");
                    LOGGER.debug(getIdFromConflict(e.getResponse()));
                } else {
                    throw e;
                }
            }

            InputStream file = request.getServletContext()
                    .getResourceAsStream("/assets/test.txt");
            try {
                BoxFolder.getRootFolder(boxUserClient).uploadFile(file, "test.txt");
            } catch (BoxAPIException e) {
                if (e.getResponseCode() == 409) {
                    LOGGER.debug("Existing File ID:");
                    LOGGER.debug(getIdFromConflict(e.getResponse()));
                } else {
                    throw e;
                }
            }
        }

        request.getSession().setAttribute(BOX_USER_ID_KEY, boxId);
    }

    private static String boxIdFromRequest(HttpServletRequest request) {
        return (String) SessionUtils.get(request, BOX_USER_ID_KEY);
    }

    private static String getIdFromConflict(String message) {
        String id = "";
        Pattern p = Pattern.compile("\"id\":\"[0-9]+\"");
        Pattern p2 = Pattern.compile("[0-9]+");
        Matcher m = p.matcher(message);
        if (m.find()) {
            String sub = m.group();
            Matcher m2 = p2.matcher(sub);
            if (m2.find()) {
                id = m2.group();
            }
        }
        return id;
    }
}
