package com.clothflow.user.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

@Component
public class RestAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        String instance =
                ServletUriComponentsBuilder
                        .fromRequestUri(request)
                        .build()
                        .toUriString();

        String json = """
                {
                  "type": "https://clothflow.dev/problems/unauthorized",
                  "title": "Unauthorized",
                  "status": 401,
                  "detail": "Authentication is required.",
                  "instance": "%s"
                }
                """.formatted(escapeJson(instance));

        response.getWriter().write(json);
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}