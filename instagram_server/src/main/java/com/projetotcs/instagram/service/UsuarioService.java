package com.projetotcs.instagram.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.projetotcs.instagram.domain.entity.BlacklistedToken;
import com.projetotcs.instagram.domain.entity.UserRole;
import com.projetotcs.instagram.domain.entity.Usuario;
import com.projetotcs.instagram.dto.AtualizacaoDTO;
import com.projetotcs.instagram.dto.CadastroDTO;
import com.projetotcs.instagram.dto.LoginDTO;
import com.projetotcs.instagram.dto.PadraoResposta;
import com.projetotcs.instagram.exception.*;
import com.projetotcs.instagram.dto.UsuarioSchema;
import com.projetotcs.instagram.repository.BlacklistedTokenRepository;
import com.projetotcs.instagram.repository.UsuarioRepository;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BlacklistedTokenRepository blacklistRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;


    private UsuarioSchema mapToSchema(Usuario usuario) {
        return new UsuarioSchema(
                usuario.getId().toString(),
                usuario.getNomeCompleto(),
                usuario.getUsuario(),
                usuario.getEmail(),
                usuario.getBiografia(),
                usuario.getFoto()
        );
    }

    public PadraoResposta listarUsuarios() {
        List<UsuarioSchema> usuarios = usuarioRepository.findAll()
                .stream()
                .map(this::mapToSchema)
                .collect(Collectors.toList());
        
        if (usuarios.isEmpty()) throw new NenhumUsuarioEncontradoException("Nenhum usuário encontrado.");

        PadraoResposta resposta = new PadraoResposta();
        resposta.setStatus("sucesso");
        resposta.setCodigo("LISTAGEM_SUCESSO");
        resposta.setMensagem("Usuários listados com sucesso");
        resposta.setDados(usuarios);
        return resposta;
    }

    public PadraoResposta cadastrarUsuario(CadastroDTO request) {
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

        // Setar role padrão como USER
        usuario.setRole(UserRole.USER);

        // Salva o usuário no banco de dados
        usuarioRepository.save(usuario);

        // Retornar a resposta de cadastro
        return new PadraoResposta("sucesso", "USUARIO_CRIADO", "Usuário cadastrado com sucesso");
    }

    public PadraoResposta login(LoginDTO request){
        // Criando um token de autenticação (não autenticado ainda)
        var usuarioSenha = new UsernamePasswordAuthenticationToken(request.usuario(), request.senha());
        
        // Autenticar o usuário usando o AuthenticationManager (Valida senha e carrega o principal)
        var auth = this.authenticationManager.authenticate(usuarioSenha);
        
        // Recupera o usuário autenticado do principal
        Usuario usuarioAutenticado = (Usuario) auth.getPrincipal();

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
        BlacklistedToken blacklistedToken = new BlacklistedToken(jti, expirationDate);
        blacklistRepository.save(blacklistedToken);

        return new PadraoResposta("sucesso", "LOGOUT_SUCESSO", "Logout realizado com sucesso");
    }

    public PadraoResposta obterUsuarioPorId(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado."));
        
        PadraoResposta resposta = new PadraoResposta("sucesso", "USUARIO_ENCONTRADO", "Dados do usuário recuperados");
        resposta.setDados(mapToSchema(usuario));
        return resposta;
    }

    public PadraoResposta atualizarUsuario(String id, AtualizacaoDTO request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado."));

        boolean atualizou = false;

        if (request.nome() != null) {
            usuario.setNomeCompleto(request.nome());
            atualizou = true;
        }

        if (request.email() != null) {
            usuario.setEmail(request.email());
            atualizou = true;
        }

        if (request.usuario() != null) {
            // Verificar se o novo nome de usuário já está em uso por outro ID
            if (!usuario.getUsuario().equals(request.usuario()) && usuarioRepository.existsByUsuario(request.usuario())) {
                throw new UsuarioJaExisteException("Este nome de usuário já está em uso.");
            }
            usuario.setUsuario(request.usuario());
            atualizou = true;
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

        PadraoResposta resposta = new PadraoResposta("sucesso", "USUARIO_ATUALIZADO", "Usuário atualizado com sucesso");
        resposta.setDados(mapToSchema(usuario));
        return resposta;
    }

    public PadraoResposta deletarUsuario(String id) {
        if (!usuarioRepository.existsById(id)) {
            throw new UsuarioNaoEncontradoException("Usuário não encontrado.");
        }
        usuarioRepository.deleteById(id);
        return new PadraoResposta("sucesso", "USUARIO_DELETADO", "Usuário deletado com sucesso");
    }
}
