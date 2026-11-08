package com.focusflow.config;

import com.focusflow.service.TrafficMetricsService;
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
 * Filter that records request counts, latency, and response status codes
 * for the Admin Dashboard traffic & service diagnostics.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrafficMetricsFilter extends OncePerRequestFilter {

    private final TrafficMetricsService trafficMetricsService;

    public TrafficMetricsFilter(TrafficMetricsService trafficMetricsService) {
        this.trafficMetricsService = trafficMetricsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            trafficMetricsService.recordRequest(request.getRequestURI(), response.getStatus(), duration);
        }
    }
}

