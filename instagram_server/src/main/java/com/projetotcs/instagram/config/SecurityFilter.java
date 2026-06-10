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
import com.projetotcs.instagram.exception.TokenAusenteException;
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
                    // Valida o token e recupera o DecodedJWT
                    var decodedJWT = tokenService.getDecodedJWT(token);
                    String jti = decodedJWT.getId();
                    
                    // Verifica se o token está na blacklist
                    if (blacklistRepository.existsByJti(jti)) {
                        throw new JWTVerificationException("Token inválido ou expirado");
                    }

                    var subject = decodedJWT.getSubject(); // Recupera o ID do usuário (subject)
                    Long id = Long.valueOf(subject);
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
        } catch (JWTVerificationException | TokenAusenteException e) {
            // Usa o resolver para encaminhar a exceção para o GlobalExceptionHandler
            resolver.resolveException(request, response, null, e);
        }
    }

    private String recoverToken(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Pega o header de autorização da requisição
        var authHeader = request.getHeader("Authorization");

        // Verifica se a URI é pública (deve coincidir com SecurityConfig)
        // GET /usuarios removido pois exige admin
        boolean isPublic = (uri.equals("/usuarios/login") && method.equals("POST")) ||
                          (uri.equals("/usuarios") && method.equals("POST")) ||
                          (uri.equals("/ativos") && method.equals("GET")) ||
                          uri.equals("/error");

        if (!isPublic) {
            if (authHeader == null) {
                throw new TokenAusenteException("Header Authorization ausente.");
            }
            if (authHeader.isBlank()) {
                throw new TokenAusenteException("Header Authorization vazio.");
            }
            if (!authHeader.startsWith("Bearer ")) {
                throw new TokenAusenteException("Header Authorization deve começar com 'Bearer '.");
            }
        }
        
        // Se o header de autorização não estiver presente ou não começar com Bearer, retorna null
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;

        return authHeader.substring(7); // Remove "Bearer " do início do token
    }
    
}
