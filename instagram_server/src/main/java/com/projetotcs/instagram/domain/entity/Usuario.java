package com.projetotcs.instagram.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String usuario;

    @Column(nullable = false, length = 24)
    private String senha;

    @Column(nullable = false, length = 35)
    private String email;

    @Column(name = "nome_completo", length = 60, nullable = false)
    private String nomeCompleto;

    @Column(length = 150)
    private String biografia = "Eu sou um usuário do Instagram";

    @Column(name = "foto_url")
    private String fotoUrl = "https://www.pngall.com/wp-content/uploads/5/Profile-PNG-High-Quality-Image.png";
}
