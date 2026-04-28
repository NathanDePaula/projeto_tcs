package com.projetotcs.instagram.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.projetotcs.instagram.dto.CadastroRequest;
import com.projetotcs.instagram.dto.LoginRequest;
import com.projetotcs.instagram.dto.PadraoResposta;
import com.projetotcs.instagram.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {
    @Autowired
    private UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<?> cadastrarUsuario(@Valid @RequestBody CadastroRequest cadastroRequest) {
        usuarioService.cadastrarUsuario(cadastroRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("Usuário cadastrado com sucesso");
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        PadraoResposta response = usuarioService.login(loginRequest);
        if ("erro".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
