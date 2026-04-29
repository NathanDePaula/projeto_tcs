package com.projetotcs.instagram.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.projetotcs.instagram.domain.entity.Usuario;

@Service
public class TokenService {
    @Value("${api.security.token.secret}")
    private String secret;

    private Instant generateExpirationDate(){
        // Define a expiração do token para 5 minutos a partir do momento atual
        return LocalDateTime.now().plusMinutes(5).toInstant(ZoneOffset.of("-03:00")); 
    }

    public String generateToken(Usuario usuario) throws JWTCreationException{
        // Define o algoritmo de assinatura do projeto usando a chave secreta
        Algorithm algorithm = Algorithm.HMAC256(secret);

        // Cria o token utilizando o JWT
        String token = JWT.create()
                .withIssuer("instagram-api") // Define o emissor do token
                .withSubject(usuario.getId().toString()) // Define o sujeito do token (ID do usuário). Vai ser usado para verificações do usuário
                .withExpiresAt(generateExpirationDate()) // Define a expiração do token (5 minutos)
                .sign(algorithm); // Assina o token com o algoritmo definido
        return token;
    }

    public String validateToken(String token) throws JWTVerificationException{
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.require(algorithm) // Define o algoritmo de verificação do token
                .withIssuer("instagram-api") // Define o emissor esperado do token para validação
                .build() // Monta o token para validação
                .verify(token) // Verifica o token e lança uma exceção se for inválido ou expirado
                .getSubject(); // Retorna o ID do usuário contido no token
    }
}
