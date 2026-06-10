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

import com.projetotcs.instagram.dto.AtualizacaoPostDTO;
import com.projetotcs.instagram.dto.CriacaoPostDTO;
import com.projetotcs.instagram.dto.ListagemPostsResponse;
import com.projetotcs.instagram.dto.PadraoResposta;
import com.projetotcs.instagram.service.PostService;

import jakarta.validation.Valid;

@RestController
@CrossOrigin("*")
@RequestMapping("/usuarios")
public class PostController {

    @Autowired
    private PostService postService;

    @GetMapping("/{usuarioId}/posts")
    public ResponseEntity<ListagemPostsResponse> listarPosts(@PathVariable("usuarioId") Long usuarioId) {
        return ResponseEntity.ok(postService.listarPosts(usuarioId));
    }

    @PostMapping("/{usuarioId}/posts")
    public ResponseEntity<PadraoResposta> criarPost(
            @PathVariable("usuarioId") Long usuarioId,
            @Valid @RequestBody CriacaoPostDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.criarPost(usuarioId, request));
    }

    @PostMapping("/{usuarioId}/posts/{postId}")
    public ResponseEntity<PadraoResposta> curtirPost(
            @PathVariable("usuarioId") Long usuarioId,
            @PathVariable("postId") Long postId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.curtirPost(usuarioId, postId));
    }

    @PatchMapping("/{usuarioId}/posts/{postId}")
    public ResponseEntity<PadraoResposta> atualizarPost(
            @PathVariable("usuarioId") Long usuarioId,
            @PathVariable("postId") Long postId,
            @Valid @RequestBody AtualizacaoPostDTO request) {
        return ResponseEntity.ok(postService.atualizarPost(usuarioId, postId, request));
    }

    @DeleteMapping("/{usuarioId}/posts/{postId}")
    public ResponseEntity<PadraoResposta> deletarPost(
            @PathVariable("usuarioId") Long usuarioId,
            @PathVariable("postId") Long postId) {
        return ResponseEntity.ok(postService.deletarPost(usuarioId, postId));
    }
}
