package com.ems.config;

import com.ems.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
// @Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitingFilter implements Filter {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    // Rates config: 100 max capacity, 10 tokens refilled per second
    private static final long BUCKET_CAPACITY = 100;
    private static final long REFILL_RATE_PER_SECOND = 5;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            String ip = getClientIP(httpRequest);
            
            // Exclude common static resource paths if requested, otherwise rate limit everything
            String path = httpRequest.getRequestURI();
            if (path.contains("/h2-console") || path.contains("/swagger-ui") || path.contains("/v3/api-docs")) {
                chain.doFilter(request, response);
                return;
            }

            TokenBucket bucket = buckets.computeIfAbsent(ip, k -> new TokenBucket(BUCKET_CAPACITY, REFILL_RATE_PER_SECOND));

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for IP: {} requesting {}", ip, path);
                sendErrorResponse(httpResponse, "Too many requests. Please try again later. Rate limit is " + REFILL_RATE_PER_SECOND + " requests/sec.");
                return;
            }
        }
        
        chain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}

    /**
     * Inner class representing a thread-safe token bucket rate limiter.
     */
    private static class TokenBucket {
        private final long capacity;
        private final long refillRate;
        private double tokens;
        private long lastRefillTimestamp;

        public TokenBucket(long capacity, long refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume(int numTokens) {
            refill();
            if (tokens >= numTokens) {
                tokens -= numTokens;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double timeElapsed = (now - lastRefillTimestamp) / 1000.0;
            
            if (timeElapsed > 0) {
                tokens = Math.min(capacity, tokens + (timeElapsed * refillRate));
                lastRefillTimestamp = now;
            }
        }
    }
}
