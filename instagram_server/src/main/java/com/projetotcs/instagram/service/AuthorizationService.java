package com.projetotcs.instagram.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.projetotcs.instagram.repository.UsuarioRepository;

@Service
public class AuthorizationService implements UserDetailsService {

    @Autowired
    UsuarioRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return repository.findByUsuario(username);
    }
}
