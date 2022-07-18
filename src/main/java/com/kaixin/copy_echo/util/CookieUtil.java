package com.kaixin.copy_echo.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
public class CookieUtil {

    public static String getValue(HttpServletRequest request, String name) {
        if (request == null || name == null) throw new IllegalArgumentException("参数为空");

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies)
                if (cookie.getName().equals(name)) return cookie.getValue();
        }
        return null;
    }
}
