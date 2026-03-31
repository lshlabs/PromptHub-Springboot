package com.lshlabs.prompthubspring.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Keeps legacy client trailing-slash routes compatible while controllers
 * are normalized to canonical no-trailing-slash endpoints.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TrailingSlashCompatibilityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri != null && uri.length() > 1 && uri.endsWith("/")) {
            String normalizedUri = uri.substring(0, uri.length() - 1);
            request.getRequestDispatcher(normalizedUri).forward(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
