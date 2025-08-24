package com.wms.billing.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String ALT_HEADER_NAME = "Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String correlationId = firstNonBlank(
                request.getHeader(HEADER_NAME),
                request.getHeader(ALT_HEADER_NAME)
        );

        if (isBlank(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        // Put into MDC for logs
        MDC.put(MDC_KEY, correlationId);

        // Make it available to downstream code if needed
        request.setAttribute(MDC_KEY, correlationId);

        // Echo to response so clients can correlate
        response.setHeader(HEADER_NAME, correlationId);

        // Allow browsers to read the header (CORS)
        response.addHeader("Access-Control-Expose-Headers", HEADER_NAME);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String firstNonBlank(String a, String b) {
        return !isBlank(a) ? a : (!isBlank(b) ? b : null);
    }
}