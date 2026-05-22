package com.projetotcs.instagram.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.projetotcs.instagram.domain.entity.Blacklist;
import com.projetotcs.instagram.domain.entity.Usuario;
import com.projetotcs.instagram.dto.AtualizacaoDTO;
import com.projetotcs.instagram.dto.CadastroDTO;
import com.projetotcs.instagram.dto.LoginDTO;
import com.projetotcs.instagram.dto.PadraoResposta;
import com.projetotcs.instagram.exception.*;
import com.projetotcs.instagram.dto.UsuarioSchema;
import com.projetotcs.instagram.repository.BlacklistRepository;
import com.projetotcs.instagram.repository.UsuarioRepository;

@Service
public class UsuarioService {
    private final Map<String, String> usuariosAtivos = new ConcurrentHashMap<>();

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BlacklistRepository blacklistRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String getUsuariosAtivosLog() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== USUÁRIOS ATIVOS (IPs) ===\n");
        if (usuariosAtivos.isEmpty()) {
            sb.append("Nenhum usuário ativo no momento.");
        } else {
            usuariosAtivos.forEach((user, ip) -> sb.append("- Usuário: ").append(user).append(" | IP: ").append(ip).append("\n"));
        }
        sb.append("=============================\n");
        return sb.toString();
    }

    private void validarPropriedade(Long id) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Usuario)) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        Usuario usuarioAutenticado = (Usuario) authentication.getPrincipal();
        
        // Se não for admin (username 'admin') e o ID for diferente do ID do usuário autenticado, nega o acesso
        if (!"admin".equals(usuarioAutenticado.getUsuario()) && !usuarioAutenticado.getId().equals(id)) {
            throw new AccessDeniedException("Você não tem permissão para alterar ou deletar este usuário.");
        }
    }

    private void validarAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Usuario)) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        Usuario usuarioAutenticado = (Usuario) authentication.getPrincipal();
        
        if (!"admin".equals(usuarioAutenticado.getUsuario())) {
            throw new AccessDeniedException("Você não tem permissão para listar todos os usuários.");
        }
    }

    private UsuarioSchema mapToSchema(Usuario usuario) {
        return new UsuarioSchema(
                usuario.getId(),
                usuario.getNomeCompleto(),
                usuario.getUsuario(),
                usuario.getEmail(),
                usuario.getBiografia(),
                usuario.getFoto()
        );
    }

    public PadraoResposta listarUsuarios() {
        validarAdmin();

        List<UsuarioSchema> usuarios = usuarioRepository.findAll()
                .stream()
                .map(this::mapToSchema)
                .collect(Collectors.toList());
        
        if (usuarios.isEmpty()) throw new NenhumUsuarioEncontradoException("Nenhum usuário encontrado.");

        PadraoResposta resposta = new PadraoResposta();
        resposta.setStatus("sucesso");
        resposta.setCodigo("LISTAGEM_SUCESSO");
        resposta.setMensagem("Usuários listados com sucesso");
        
        Map<String, Object> dados = new HashMap<>();
        dados.put("usuarios", usuarios);
        resposta.setDados(dados);
        
        return resposta;
    }

    public PadraoResposta cadastrarUsuario(CadastroDTO request) {
        // Não é permitido criar novas contas com o username "admin"
        if ("admin".equalsIgnoreCase(request.usuario())) {
            throw new UsuarioJaExisteException("O username 'admin' é reservado e não pode ser utilizado.");
        }

        // Verificar se o usuário já existe
        if (usuarioRepository.existsByUsuario(request.usuario())) throw new UsuarioJaExisteException("Este usuário já existe.");
        
        // Criar um novo objeto Usuario e preencher os campos com os dados do request
        Usuario usuario = new Usuario();
        usuario.setUsuario(request.usuario());
        usuario.setEmail(request.email());
        // Encriptando a senha antes de salvar no banco de dados
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setNomeCompleto(request.nome());
        
        // Só setar biografia se não for nula/vazia
        if (request.biografia() != null && !request.biografia().isBlank()) {
            usuario.setBiografia(request.biografia());
        }
        
        // Só setar foto se não for nula/vazia
        if (request.foto() != null && !request.foto().isBlank()) {
            usuario.setFoto(request.foto());
        }

        // Salva o usuário no banco de dados
        usuarioRepository.save(usuario);

        // Retornar a resposta de cadastro
        return new PadraoResposta("sucesso", "USUARIO_CRIADO", "Usuário cadastrado com sucesso");
    }

    public PadraoResposta login(LoginDTO request, String ip){
        // Criando um token de autenticação (não autenticado ainda)
        var usuarioSenha = new UsernamePasswordAuthenticationToken(request.usuario(), request.senha());
        
        // Autenticar o usuário usando o AuthenticationManager (Valida senha e carrega o principal)
        var auth = this.authenticationManager.authenticate(usuarioSenha);
        
        // Recupera o usuário autenticado do principal
        Usuario usuarioAutenticado = (Usuario) auth.getPrincipal();

        // Adiciona à lista de usuários ativos e loga
        usuariosAtivos.put(usuarioAutenticado.getUsuario(), ip);

        // Gera o token JWT
        var token = tokenService.generateToken(usuarioAutenticado);
        
        // Monta o objeto de dados final contendo o token e informações do usuário
        Map<String, Object> dados = new HashMap<>();
        dados.put("token", token);
        
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("id", usuarioAutenticado.getId());
        usuario.put("nome", usuarioAutenticado.getNomeCompleto());
        usuario.put("email", usuarioAutenticado.getEmail());
        dados.put("usuario", usuario);

        return new PadraoResposta("sucesso", "LOGIN_SUCESSO", "Login realizado com sucesso", dados);
    }

    public PadraoResposta logout(String token) {
        // Recupera o JTI e a data de expiração do token
        String jti = tokenService.getJti(token);
        var expirationDate = tokenService.getExpirationDate(token);
        
        // Salva o token na blacklist
        Blacklist blacklistedToken = new Blacklist(jti, expirationDate);
        blacklistRepository.save(blacklistedToken);

        // Remover da lista de usuários ativos
        try {
            String subject = tokenService.validateToken(token);
            Long userId = Long.valueOf(subject);
            usuarioRepository.findById(userId).ifPresent(u -> {
                usuariosAtivos.remove(u.getUsuario());
            });
        } catch (Exception e) {
            // Se o token já for inválido/expirado, apenas ignoramos a remoção manual
        }

        return new PadraoResposta("sucesso", "LOGOUT_SUCESSO", "Logout realizado com sucesso");
    }

    public PadraoResposta obterUsuarioPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado."));
        
        PadraoResposta resposta = new PadraoResposta("sucesso", "USUARIO_ENCONTRADO", "Dados do usuário recuperados");
        resposta.setDados(mapToSchema(usuario));
        return resposta;
    }

    public PadraoResposta atualizarUsuario(Long id, AtualizacaoDTO request, String token) {
        validarPropriedade(id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado."));

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuarioAutenticado = (Usuario) authentication.getPrincipal();

        boolean atualizou = false;
        boolean usernameAlterado = false;

        if (request.nome() != null) {
            usuario.setNomeCompleto(request.nome());
            atualizou = true;
        }

        if (request.email() != null) {
            usuario.setEmail(request.email());
            atualizou = true;
        }

        if (request.usuario() != null) {
            // O username da conta admin original não pode ser editado.
            if ("admin".equals(usuario.getUsuario()) && !usuario.getUsuario().equals(request.usuario())) {
                throw new AccessDeniedException("O username da conta admin original não pode ser alterado.");
            }

            // Não é permitido que usuários comuns alterem seus usernames para "admin".
            if (!"admin".equals(usuario.getUsuario()) && "admin".equalsIgnoreCase(request.usuario())) {
                throw new AccessDeniedException("Não é permitido alterar o username para 'admin'.");
            }

            // Verificar se o novo nome de usuário já está em uso por outro ID
            if (!usuario.getUsuario().equals(request.usuario()) && usuarioRepository.existsByUsuario(request.usuario())) {
                throw new UsuarioJaExisteException("Este nome de usuário já está em uso.");
            }
            
            if (!usuario.getUsuario().equals(request.usuario())) {
                usuario.setUsuario(request.usuario());
                atualizou = true;
                usernameAlterado = true;
            }
        }
        
        if (request.biografia() != null) {
            usuario.setBiografia(request.biografia());
            atualizou = true;
        }
        
        if (request.foto() != null) {
            usuario.setFoto(request.foto());
            atualizou = true;
        }

        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.senha()));
            atualizou = true;
        }

        if (!atualizou) {
            throw new IllegalArgumentException("Ao menos um campo deve ser enviado para atualização.");
        }

        usuarioRepository.save(usuario);

        // Quando o username de um usuário comum é editado por ele mesmo (não por um admin), o token utilizado na requisição deve ser adicionado a blacklist.
        if (usernameAlterado && !"admin".equals(usuarioAutenticado.getUsuario())) {
            this.logout(token);
        }

        PadraoResposta resposta = new PadraoResposta("sucesso", "USUARIO_ATUALIZADO", "Usuário atualizado com sucesso");
        resposta.setDados(mapToSchema(usuario));
        return resposta;
    }

    public PadraoResposta deletarUsuario(Long id) {
        validarPropriedade(id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado."));
                
        if ("admin".equals(usuario.getUsuario())) {
            throw new AccessDeniedException("A conta admin original não pode ser excluída.");
        }
        
        usuarioRepository.deleteById(id);
        return new PadraoResposta("sucesso", "USUARIO_DELETADO", "Usuário deletado com sucesso");
    }
}
