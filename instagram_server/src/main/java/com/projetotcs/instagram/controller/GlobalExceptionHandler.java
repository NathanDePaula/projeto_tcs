package com.projetotcs.instagram.controller;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.projetotcs.instagram.dto.ErroResponse;
import com.projetotcs.instagram.exception.UsuarioJaExisteException;
import com.projetotcs.instagram.exception.CredenciaisInvalidasException;

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
    
    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> handleUsuarioJaExiste(CredenciaisInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse("credenciais_invalidas", ex.getMessage()));
    }
}
