package com.projetotcs.instagram.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.projetotcs.instagram.repository.BlacklistRepository;
import com.projetotcs.instagram.repository.UsuarioRepository;
import com.projetotcs.instagram.service.TokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityFilter extends OncePerRequestFilter{

    @Autowired
    TokenService tokenService;

    @Autowired
    UsuarioRepository repository;

    @Autowired
    BlacklistRepository blacklistRepository;

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver resolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        try {
            var token = this.recoverToken(request);
            if (token != null) {
                    // Verifica se o token está na blacklist
                    String jti = tokenService.getJti(token);
                    if (blacklistRepository.existsByJti(jti)) {
                        throw new JWTVerificationException("Token inválido ou expirado");
                    }

                    var id = tokenService.validateToken(token); // Valida o token e recupera o ID do usuário (subject)
                    UserDetails user = repository.findById(id).orElse(null);
                    
                    // Caso o usuário exista, criar um objeto de autenticação do Spring Security
                    // e definir o contexto de segurança para o usuário autenticado
                    if (user != null) {
                        // Pega todas as informações que o Spring Security precisa para validar os endpoints
                        var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        
                        // Define o contexto de segurança do Spring Security com as informações do usuário autenticado
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
            }

            // Caso não tem token, passa para a verificação do próximo filtro da cadeia de segurança
            filterChain.doFilter(request, response);
        } catch (JWTVerificationException e) {
            // Usa o resolver para encaminhar a exceção para o GlobalExceptionHandler
            resolver.resolveException(request, response, null, e);
        }
    }

    private String recoverToken(HttpServletRequest request) {
        // Pega o header de autorização da requisição
        var authHeader = request.getHeader("Authorization");
        
        // Se o header de autorização não estiver presente, retorna null
        if (authHeader == null) return null;

        return authHeader.replace("Bearer ", ""); // Remove "Bearer " do início do token
    }
    
}
