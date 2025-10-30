package com.example.cample.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtils {
    public static void addHttpOnlyCookie(HttpServletResponse res, String name, String value,
                                         int maxAgeSeconds, boolean secure, String domain) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        if (domain != null && !domain.isBlank()) cookie.setDomain(domain);
        cookie.setMaxAge(maxAgeSeconds);
        res.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletResponse res, String name, boolean secure, String domain) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        if (domain != null && !domain.isBlank()) cookie.setDomain(domain);
        cookie.setMaxAge(0);
        res.addCookie(cookie);
    }

    public static String getCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) if (c.getName().equals(name)) return c.getValue();
        return null;
    }
}
