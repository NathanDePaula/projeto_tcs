package com.projetotcs.instagram.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Component
public class Logging extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(Logging.class);
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, 10000);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            logRequest(requestWrapper);
            logResponse(responseWrapper);
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        byte[] content = request.getContentAsByteArray();

        logger.info(">>> REQUEST: {} {}", method, uri);
        logRequestHeaders(request);
        if (content.length > 0) {
            logBody(content, ">>> REQUEST Body");
        }
    }

    private void logResponse(ContentCachingResponseWrapper response) {
        int status = response.getStatus();
        byte[] content = response.getContentAsByteArray();

        logger.info("<<< RESPONSE Status: {}", status);
        logResponseHeaders(response);
        if (content.length > 0) {
            logBody(content, "(\"<<< RESPONSE Body");
        }
    }

    private void logRequestHeaders(HttpServletRequest request) {
        java.util.StringJoiner headers = new java.util.StringJoiner("\n");
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.add(headerName + ": " + request.getHeader(headerName));
            }
            if (headers.length() > 0) {
                logger.info(">>> REQUEST Headers:\n{}", headers.toString());
            }
        }
    }

    private void logResponseHeaders(HttpServletResponse response) {
        java.util.StringJoiner headers = new java.util.StringJoiner("\n");
        java.util.Collection<String> headerNames = response.getHeaderNames();
        if (headerNames != null && !headerNames.isEmpty()) {
            for (String headerName : headerNames) {
                headers.add(headerName + ": " + response.getHeader(headerName));
            }
            logger.info("<<< RESPONSE Headers:\n{}", headers.toString());
        }
    }

    private void logBody(byte[] content, String prefix) {
        try {
            Object json = objectMapper.readValue(content, Object.class);
            String prettyJson = objectMapper.writeValueAsString(json);
            logger.info("{}:\n{}", prefix, prettyJson);
        } catch (Exception e) {
            String body = new String(content);
            if (body.length() > 0 && body.length() < 2000) {
                logger.info("{}: {}", prefix, body);
            }
        }
    }
}
