package com.projetotcs.instagram.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.projetotcs.instagram.domain.entity.Usuario;

@Service
public class TokenService {
    @Value("${api.security.token.secret}")
    private String secret;

    public Instant generateExpirationDate(){
        // Define a expiração do token para 5 minutos a partir do momento atual
        return LocalDateTime.now().plusMinutes(5).atZone(ZoneId.systemDefault()).toInstant(); 
    }

    public String generateToken(Usuario usuario) throws JWTCreationException{
        // Define o algoritmo de assinatura do projeto usando a chave secreta
        Algorithm algorithm = Algorithm.HMAC256(secret);
        // Define um ID único para o token, que pode ser usado para rastrear ou invalidar tokens específicos se necessário (blacklist)
        String jti = UUID.randomUUID().toString();

        // Cria o token utilizando o JWT
        String token = JWT.create()
                .withIssuer("instagram-api") // Define o emissor do token
                .withSubject(usuario.getId().toString()) // Define o sujeito do token (ID do usuário). Vai ser usado para verificações do usuário
                .withJWTId(jti) // Define o ID único do token (JTI)
                .withIssuedAt(new Date()) // Define a data de emissão do token
                .withExpiresAt(generateExpirationDate()) // Define a expiração do token (5 minutos)
                .sign(algorithm); // Assina o token com o algoritmo definido
        return token;
    }

    private DecodedJWT decodeToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.require(algorithm)
                .withIssuer("instagram-api")
                .build()
                .verify(token);
    }

    public String getJti(String token) {
        return decodeToken(token).getId();
    }

    public Instant getExpirationDate(String token) {
        return decodeToken(token).getExpiresAtAsInstant();
    }

    public DecodedJWT getDecodedJWT(String token) throws JWTVerificationException {
        return decodeToken(token);
    }

    public String validateToken(String token) throws JWTVerificationException{
        return decodeToken(token).getSubject(); // Retorna o ID do usuário contido no token
    }
}
