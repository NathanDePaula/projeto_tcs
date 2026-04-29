package com.projetotcs.instagram.controller;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.projetotcs.instagram.dto.ErroResponse;
import com.projetotcs.instagram.exception.UsuarioJaExisteException;
import com.projetotcs.instagram.exception.CredenciaisInvalidasException;
import com.projetotcs.instagram.exception.NenhumUsuarioEncontradoException;
import com.projetotcs.instagram.exception.UsuarioNaoEncontradoException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidationException(MethodArgumentNotValidException ex) {
        boolean temCamposObrigatoriosErro = ex.getBindingResult().getFieldErrors().stream()
                .anyMatch(error -> error.getCodes() != null &&
                        java.util.Arrays.stream(error.getCodes())
                                .anyMatch(code -> code.contains("NotBlank") || code.contains("NotNull")));

        String codigo = temCamposObrigatoriosErro ? "dados_incompletos" : "dados_invalidos";

        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining("; "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(codigo, mensagem));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErroResponse("acesso_negado", "Acesso negado: " + ex.getMessage()));
    }

    @ExceptionHandler(UsuarioJaExisteException.class)
    public ResponseEntity<ErroResponse> handleUsuarioJaExiste(UsuarioJaExisteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse("usuario_existente", ex.getMessage()));
    }
    
    @ExceptionHandler(NenhumUsuarioEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNenhumUsuarioEncontrado(NenhumUsuarioEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse("nenhum_usuario_encontrado", ex.getMessage()));
    }
    
    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> handleCredenciaisInvalidas(CredenciaisInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErroResponse("credenciais_invalidas", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ErroResponse> handleBadCredentials(org.springframework.security.authentication.BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErroResponse("credenciais_invalidas", "Usuário ou senha inválidos"));
    }

    @ExceptionHandler(JWTCreationException.class)
    public ResponseEntity<ErroResponse> handleJWTCreationException(JWTCreationException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroResponse("token_error", "Erro ao gerar token: " + ex.getMessage()));
    }

    @ExceptionHandler(JWTVerificationException.class)
    public ResponseEntity<ErroResponse> handleJWTVerificationException(JWTVerificationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErroResponse("token_invalido", "Token inválido ou expirado"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroResponse("erro_autenticacao", "Credenciais inválidas"));
    }

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleUsuarioNaoEncontrado(UsuarioNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse("usuario_nao_encontrado", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroResponse("erro_interno", "Ocorreu um erro inesperado: " + ex.getMessage()));
    }
}
