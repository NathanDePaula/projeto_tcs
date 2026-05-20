package com.projetotcs.instagram.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.projetotcs.instagram.domain.entity.Usuario;
import com.projetotcs.instagram.repository.UsuarioRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Regra 1: Criar usuário admin automaticamente se não existir
        if (!usuarioRepository.existsByUsuario("admin")) {
            Usuario admin = new Usuario();
            admin.setUsuario("admin");
            admin.setSenha(passwordEncoder.encode("admin"));
            admin.setNomeCompleto("Administrador do Sistema");
            admin.setEmail("admin@instagram.com");
            admin.setBiografia("Conta oficial de administração do sistema.");
            admin.setFoto("https://cdn-icons-png.flaticon.com/512/3135/3135715.png"); // Foto padrão de admin
            
            usuarioRepository.save(admin);
            System.out.println(">>> [DataSeeder] Usuário 'admin' criado com sucesso.");
        }
    }
}