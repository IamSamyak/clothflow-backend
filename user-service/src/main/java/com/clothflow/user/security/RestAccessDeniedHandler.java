package com.clothflow.user.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler
        implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        String instance =
                ServletUriComponentsBuilder
                        .fromRequestUri(request)
                        .build()
                        .toUriString();

        String json = """
                {
                  "type": "https://clothflow.dev/problems/forbidden",
                  "title": "Forbidden",
                  "status": 403,
                  "detail": "You do not have permission to access this resource.",
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