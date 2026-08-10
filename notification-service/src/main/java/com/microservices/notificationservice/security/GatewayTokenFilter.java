package com.microservices.notificationservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class GatewayTokenFilter extends OncePerRequestFilter {

    public static final String GATEWAY_HEADER = "X-Gateway-Token";

    private final String expectedToken;

    public GatewayTokenFilter(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (expectedToken == null || expectedToken.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(GATEWAY_HEADER);
        if (!expectedToken.equals(provided)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("{\"error\":\"Direct access forbidden. Use API Gateway.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
