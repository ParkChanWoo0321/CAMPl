package com.example.cample.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) {
        try {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            om.writeValue(response.getWriter(), Map.of("error", "Unauthorized"));
        } catch (Exception ignored) {}
    }
}
