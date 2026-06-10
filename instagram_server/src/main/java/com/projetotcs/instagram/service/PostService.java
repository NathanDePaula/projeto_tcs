package com.projetotcs.instagram.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.projetotcs.instagram.domain.entity.Curtida;
import com.projetotcs.instagram.domain.entity.Post;
import com.projetotcs.instagram.domain.entity.Usuario;
import com.projetotcs.instagram.dto.AtualizacaoPostDTO;
import com.projetotcs.instagram.dto.CriacaoPostDTO;
import com.projetotcs.instagram.dto.ListagemPostsResponse;
import com.projetotcs.instagram.dto.PadraoResposta;
import com.projetotcs.instagram.dto.PostSchema;
import com.projetotcs.instagram.exception.ImagemInvalidaException;
import com.projetotcs.instagram.exception.NenhumPostEncontradoException;
import com.projetotcs.instagram.exception.PostNaoEncontradoException;
import com.projetotcs.instagram.exception.UsuarioNaoEncontradoException;
import com.projetotcs.instagram.repository.CurtidaRepository;
import com.projetotcs.instagram.repository.PostRepository;
import com.projetotcs.instagram.repository.UsuarioRepository;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CurtidaRepository curtidaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private void validarPropriedade(Long usuarioId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Usuario)) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        Usuario usuarioAutenticado = (Usuario) authentication.getPrincipal();

        // Se não for admin (username 'admin') e o ID for diferente do ID do usuário autenticado, nega o acesso
        if (!"admin".equals(usuarioAutenticado.getUsuario()) && !usuarioAutenticado.getId().equals(usuarioId)) {
            throw new AccessDeniedException("Você não tem permissão para realizar esta operação.");
        }
    }

    private Usuario getUsuarioOuThrow(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado."));
    }

    private Post getPostOuThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostNaoEncontradoException("Post não encontrado."));
    }

    private void validarImagem(String imagem) {
        if (imagem == null || imagem.isBlank()) {
            throw new ImagemInvalidaException("A imagem é obrigatória.");
        }
        
        String cleanBase64 = imagem.trim();
        
        // Remove data URI prefix if present
        String rawBase64 = cleanBase64;
        if (cleanBase64.contains(",")) {
            rawBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
        }
        
        // Check size limit: 10 MB (RNF07)
        double sizeInBytes = (rawBase64.length() * 0.75);
        double sizeInMb = sizeInBytes / (1024 * 1024);
        if (sizeInMb > 10.0) {
            throw new ImagemInvalidaException("A imagem excede o tamanho máximo de 10 MB.");
        }
        
        // Check formats: JPG, JPEG, PNG (RNF07)
        boolean formatOk = false;
        if (cleanBase64.startsWith("data:image/png") || cleanBase64.startsWith("data:image/jpeg") || cleanBase64.startsWith("data:image/jpg")) {
            formatOk = true;
        } else {
            // Check magic prefix of raw base64
            // PNG: iVBORw
            // JPEG: /9j/
            if (rawBase64.startsWith("iVBORw") || rawBase64.startsWith("/9j/")) {
                formatOk = true;
            }
        }
        
        if (!formatOk) {
            throw new ImagemInvalidaException("Formato de imagem inválido. Formatos aceitos: JPG, JPEG e PNG.");
        }
    }

    public ListagemPostsResponse listarPosts(Long usuarioId) {
        // Check if user exists
        getUsuarioOuThrow(usuarioId);

        List<Post> posts = postRepository.findByUsuarioId(usuarioId);
        if (posts.isEmpty()) {
            throw new NenhumPostEncontradoException("Nenhum post encontrado.");
        }

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuarioAutenticado = null;
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof Usuario) {
            usuarioAutenticado = (Usuario) authentication.getPrincipal();
        }
        final Usuario currentUser = usuarioAutenticado;

        List<PostSchema> postSchemas = posts.stream().map(post -> {
            long count = curtidaRepository.countByPostId(post.getId());
            boolean likedByMe = false;
            if (currentUser != null) {
                likedByMe = curtidaRepository.findByUsuarioIdAndPostId(currentUser.getId(), post.getId()).isPresent();
            }
            return new PostSchema(
                String.valueOf(post.getId()),
                post.getLegenda() != null ? post.getLegenda() : "",
                post.getImagem(),
                String.valueOf(count),
                likedByMe
            );
        }).collect(Collectors.toList());

        return new ListagemPostsResponse(
            "sucesso",
            "LISTAGEM_POSTS_SUCESSO",
            "Posts listados com sucesso",
            postSchemas
        );
    }

    public PadraoResposta criarPost(Long usuarioId, CriacaoPostDTO request) {
        // Enforce ownership: only authenticated user can post to their own ID (or admin)
        validarPropriedade(usuarioId);

        Usuario usuario = getUsuarioOuThrow(usuarioId);
        validarImagem(request.imagem());

        Post post = new Post();
        post.setUsuario(usuario);
        post.setImagem(request.imagem());
        post.setLegenda(request.legenda());

        postRepository.save(post);

        return new PadraoResposta("sucesso", "OPERACAO_SUCESSO", "Operação realizada com sucesso");
    }

    public PadraoResposta atualizarPost(Long usuarioId, Long postId, AtualizacaoPostDTO request) {
        Post post = getPostOuThrow(postId);

        // Verify post belongs to the path user
        if (!post.getUsuario().getId().equals(usuarioId)) {
            throw new PostNaoEncontradoException("Post não encontrado.");
        }

        // Validate ownership: only author or admin can update
        validarPropriedade(post.getUsuario().getId());

        if (request.legenda() != null) {
            post.setLegenda(request.legenda());
        }

        postRepository.save(post);

        return new PadraoResposta("sucesso", "OPERACAO_SUCESSO", "Operação realizada com sucesso");
    }

    public PadraoResposta deletarPost(Long usuarioId, Long postId) {
        Post post = getPostOuThrow(postId);

        // Verify post belongs to the path user
        if (!post.getUsuario().getId().equals(usuarioId)) {
            throw new PostNaoEncontradoException("Post não encontrado.");
        }

        // Validate ownership: only author or admin can delete
        validarPropriedade(post.getUsuario().getId());

        // Delete likes first to satisfy foreign key constraint
        curtidaRepository.deleteByPostId(postId);

        postRepository.delete(post);

        return new PadraoResposta("sucesso", "OPERACAO_SUCESSO", "Operação realizada com sucesso");
    }

    public PadraoResposta curtirPost(Long usuarioId, Long postId) {
        // Verify post exists
        Post post = getPostOuThrow(postId);

        // Retrieve authenticated user
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Usuario)) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        Usuario usuarioAutenticado = (Usuario) authentication.getPrincipal();

        Optional<Curtida> existingLike = curtidaRepository.findByUsuarioIdAndPostId(usuarioAutenticado.getId(), postId);

        if (existingLike.isPresent()) {
            // Toggle behavior: remove like
            curtidaRepository.delete(existingLike.get());
        } else {
            // Toggle behavior: add like
            Curtida newLike = new Curtida();
            newLike.setUsuario(usuarioAutenticado);
            newLike.setPost(post);
            curtidaRepository.save(newLike);
        }

        return new PadraoResposta("sucesso", "OPERACAO_SUCESSO", "Operação realizada com sucesso");
    }
}
