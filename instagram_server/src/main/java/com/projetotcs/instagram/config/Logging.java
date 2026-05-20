package com.projetotcs.instagram.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.projetotcs.instagram.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Component
public class Logging extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(Logging.class);
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Autowired
    @Lazy
    private UsuarioService usuarioService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, 10000);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        StringBuilder fullLog = new StringBuilder();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            // Constrói log de Requisição
            appendRequestLog(fullLog, requestWrapper);
            
            // Constrói log de Resposta
            appendResponseLog(fullLog, responseWrapper);

            // Se for login ou logout, anexa a lista de usuários ativos ao final
            String uri = requestWrapper.getRequestURI();
            if (uri.contains("/login") || uri.contains("/logout")) {
                fullLog.append("\n\n").append(usuarioService.getUsuariosAtivosLog());
            }

            // Envia tudo em uma única mensagem
            logger.info(fullLog.toString());
            
            responseWrapper.copyBodyToResponse();
        }
    }

    private void appendRequestLog(StringBuilder sb, ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        byte[] content = request.getContentAsByteArray();

        sb.append(">>> REQUEST: ").append(method).append(" ").append(uri).append("\n");
        appendRequestHeaders(sb, request);
        if (content.length > 0) {
            appendBody(sb, content, ">>> REQUEST Body");
        }
    }

    private void appendResponseLog(StringBuilder sb, ContentCachingResponseWrapper response) {
        int status = response.getStatus();
        byte[] content = response.getContentAsByteArray();

        sb.append("\n<<< RESPONSE Status: ").append(status).append("\n");
        appendResponseHeaders(sb, response);
        if (content.length > 0) {
            appendBody(sb, content, "<<< RESPONSE Body");
        }
    }

    private void appendRequestHeaders(StringBuilder sb, HttpServletRequest request) {
        java.util.StringJoiner headers = new java.util.StringJoiner("\n");
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.add(headerName + ": " + request.getHeader(headerName));
            }
            if (headers.length() > 0) {
                sb.append(">>> REQUEST Headers:\n").append(headers.toString()).append("\n");
            }
        }
    }

    private void appendResponseHeaders(StringBuilder sb, HttpServletResponse response) {
        java.util.StringJoiner headers = new java.util.StringJoiner("\n");
        java.util.Collection<String> headerNames = response.getHeaderNames();
        if (headerNames != null && !headerNames.isEmpty()) {
            for (String headerName : headerNames) {
                headers.add(headerName + ": " + response.getHeader(headerName));
            }
            sb.append("<<< RESPONSE Headers:\n").append(headers.toString()).append("\n");
        }
    }

    private void appendBody(StringBuilder sb, byte[] content, String prefix) {
        try {
            Object json = objectMapper.readValue(content, Object.class);
            String prettyJson = objectMapper.writeValueAsString(json);
            sb.append(prefix).append(":\n").append(prettyJson).append("\n");
        } catch (Exception e) {
            String body = new String(content);
            if (body.length() > 0 && body.length() < 2000) {
                sb.append(prefix).append(": ").append(body).append("\n");
            }
        }
    }
}
