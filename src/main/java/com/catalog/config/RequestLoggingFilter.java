package com.catalog.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestLoggingFilter implements Filter {

    private static final String HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest  httpReq = (HttpServletRequest)  req;
        HttpServletResponse httpRes = (HttpServletResponse) res;
        String correlationId = httpReq.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) correlationId = UUID.randomUUID().toString();
        MDC.put(MDC_KEY, correlationId);
        httpRes.setHeader(HEADER, correlationId);
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(req, res);
        } finally {
            log.info("HTTP method={} uri={} status={} duration={}ms",
                    httpReq.getMethod(), httpReq.getRequestURI(), httpRes.getStatus(),
                    System.currentTimeMillis() - start);
            MDC.clear();
        }
    }
}
