package com.projetotcs.instagram.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // Remove "Bearer "
            if (jwtUtil.validarToken(token)) {
                String usuario = jwtUtil.extrairUsuario(token);
                // Cria objeto de autenticação com o usuário extraído
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(usuario, null, List.of()); // Sem roles por enquanto
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Define no contexto de segurança
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // Continua a cadeia de filtros
        filterChain.doFilter(request, response);
    }
}
