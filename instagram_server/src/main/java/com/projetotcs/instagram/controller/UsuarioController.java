package com.projetotcs.instagram.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.projetotcs.instagram.dto.AtualizacaoDTO;
import com.projetotcs.instagram.dto.CadastroDTO;
import com.projetotcs.instagram.dto.LoginDTO;
import com.projetotcs.instagram.dto.PadraoResposta;
import com.projetotcs.instagram.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@CrossOrigin("*")
@RequestMapping("/usuarios")
public class UsuarioController {
    @Autowired
    private UsuarioService usuarioService;


    @GetMapping    
    public ResponseEntity<?> listarUsuarios() {
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.listarUsuarios());
    }

    @PostMapping
    public ResponseEntity<?> cadastrarUsuario(@Valid @RequestBody CadastroDTO cadastroRequest) {
        PadraoResposta resposta = usuarioService.cadastrarUsuario(cadastroRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO request, HttpServletRequest servletRequest) {
        String ip = servletRequest.getRemoteAddr();
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.login(request, ip));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new PadraoResposta("erro", "TOKEN_AUSENTE", "Token de autorização não enviado"));
        }
        var token = authHeader.replace("Bearer ", "");
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.logout(token));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obterUsuarioPorId(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.obterUsuarioPorId(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> atualizarUsuario(@PathVariable Long id, @Valid @RequestBody AtualizacaoDTO request, HttpServletRequest servletRequest) {
        var authHeader = servletRequest.getHeader("Authorization");
        var token = (authHeader != null) ? authHeader.replace("Bearer ", "") : null;
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.atualizarUsuario(id, request, token));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletarUsuario(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.deletarUsuario(id));
    }
}
