package com.focusflow.security;

import com.focusflow.config.TrafficMetricsFilter;
import com.focusflow.service.TrafficMetricsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests validating Phase 6 Traffic & Service Monitoring telemetry,
 * Micrometer metric collection, and filter request recording.
 */
@ExtendWith(MockitoExtension.class)
class ActuatorMonitoringSecurityTest {

    private TrafficMetricsService trafficMetricsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        trafficMetricsService = new TrafficMetricsService(
                Optional.of(new SimpleMeterRegistry()),
                Optional.empty()
        );
    }

    @Test
    @DisplayName("TrafficMetricsService should record requests, compute rates, and group status codes")
    void testTrafficMetricsRecording() {
        trafficMetricsService.recordRequest("/api/subjects/12", 200, 50);
        trafficMetricsService.recordRequest("/api/subjects/15", 200, 70);
        trafficMetricsService.recordRequest("/api/quizzes/generate", 400, 30);
        trafficMetricsService.recordRequest("/api/schedule/generate", 500, 150);

        Map<String, Object> analytics = trafficMetricsService.getTrafficAnalytics();

        assertNotNull(analytics);
        assertEquals(4L, analytics.get("totalRequests"));
        assertTrue((Long) analytics.get("avgLatencyMs") >= 70);

        @SuppressWarnings("unchecked")
        Map<String, Long> statusDist = (Map<String, Long>) analytics.get("statusDistribution");
        assertEquals(2L, statusDist.get("2xx"));
        assertEquals(1L, statusDist.get("4xx"));
        assertEquals(1L, statusDist.get("5xx"));
    }

    @Test
    @DisplayName("TrafficMetricsService should return JVM heap, thread, and disk diagnostics")
    void testServiceHealthMetrics() {
        Map<String, Object> metrics = trafficMetricsService.getServiceHealthMetrics();

        assertNotNull(metrics);
        assertEquals("UP", metrics.get("status"));
        assertTrue((Long) metrics.get("heapMaxMb") > 0);
        assertTrue((Integer) metrics.get("availableProcessors") > 0);
        assertTrue((Long) metrics.get("diskTotalGb") > 0);
        assertTrue(metrics.containsKey("connectionPool"));
        assertEquals("/actuator/prometheus", metrics.get("prometheusEndpoint"));
    }

    @Test
    @DisplayName("TrafficMetricsFilter should intercept requests and record telemetry")
    void testFilterExecution() throws ServletException, IOException {
        TrafficMetricsService mockService = mock(TrafficMetricsService.class);
        TrafficMetricsFilter filter = new TrafficMetricsFilter(mockService);

        when(request.getRequestURI()).thenReturn("/api/admin/overview");
        when(response.getStatus()).thenReturn(200);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(mockService, times(1)).recordRequest(eq("/api/admin/overview"), eq(200), anyLong());
    }
}

