package com.projetotcs.instagram.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.projetotcs.instagram.config.JwtUtil;
import com.projetotcs.instagram.domain.entity.Usuario;
import com.projetotcs.instagram.dto.CadastroRequest;
import com.projetotcs.instagram.dto.CadastroResponse;
import com.projetotcs.instagram.dto.LoginRequest;
import com.projetotcs.instagram.dto.PadraoResposta;
import com.projetotcs.instagram.exception.*;
// import com.projetotcs.instagram.dto.UsuarioSchema;
import com.projetotcs.instagram.repository.UsuarioRepository;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // private UsuarioSchema mapToSchema(Usuario usuario) {
    //     return new UsuarioSchema(
    //             usuario.getId().toString(),
    //             usuario.getNomeCompleto(),
    //             usuario.getUsuario(),
    //             usuario.getEmail(),
    //             usuario.getBiografia(),
    //             usuario.getFotoUrl()
    //     );
    // }

    public CadastroResponse cadastrarUsuario(CadastroRequest request) {
        // Verificar se o usuário já existe
        if (usuarioRepository.existsByUsuario(request.getUsuario())) {
            throw new UsuarioJaExisteException("Este usuário já está cadastrado.");
        }

        // Criar um novo objeto Usuario e preencher os campos com os dados do request
        Usuario usuario = new Usuario();
        usuario.setUsuario(request.getUsuario());
        usuario.setEmail(request.getEmail());
        usuario.setSenha(request.getSenha());
        usuario.setNomeCompleto(request.getNome());
        
        // Só setar biografia se não for nula/vazia
        if (request.getBiografia() != null && !request.getBiografia().isBlank()) {
            usuario.setBiografia(request.getBiografia());
        }
        
        // Só setar fotoUrl se não for nula/vazia
        if (request.getFotoUrl() != null && !request.getFotoUrl().isBlank()) {
            usuario.setFotoUrl(request.getFotoUrl());
        }

        // Salva o usuário no banco de dados
        usuarioRepository.save(usuario);

        // Retornar a resposta de cadastro
        return new CadastroResponse();
    }

    public PadraoResposta login(LoginRequest request) {
        // Verifica se o usuário realmente existe
        Usuario usuario = usuarioRepository.findByUsuario(request.getUsuario())
            .orElse(null);

        if (!usuarioRepository.existsByUsuario(request.getUsuario()) || !usuario.getSenha().equals(request.getSenha())) {
            throw new CredenciaisInvalidasException("Credenciais inválidas.");
        }

        // Verifica se o usuário não é Null e caso a senha não correspoda a senha do banco de dados
        if (usuario == null || (request.getSenha() == usuario.getSenha())) {
            PadraoResposta erro = new PadraoResposta();
            erro.setStatus("erro");
            erro.setCodigo("CREDENCIAIS_INVALIDAS");
            erro.setMensagem("Usuário ou senha inválidos");
            return erro;
        }

        // Gera o token JWT
        String token = jwtUtil.gerarToken(usuario.getUsuario());

        // Criar objeto para o campo "dados" da resposta, contendo o token e os dados do usuário
        Map<String, Object> dados = new HashMap<>();
        dados.put("token", token);
        
        // Inclui os dados do usuário (id, nome e email) na resposta
        Map<String, Object> usuarioDados = new HashMap<>();
        usuarioDados.put("id", usuario.getId().toString());
        usuarioDados.put("nome", usuario.getNomeCompleto());
        usuarioDados.put("email", usuario.getEmail());
        dados.put("usuario", usuarioDados);

        // Constrói a resposta de sucesso
        PadraoResposta sucesso = new PadraoResposta();
        sucesso.setStatus("sucesso");
        sucesso.setCodigo("LOGIN_SUCESSO");
        sucesso.setMensagem("Login realizado com sucesso");
        sucesso.setDados(dados);
        return sucesso;
        }
}
