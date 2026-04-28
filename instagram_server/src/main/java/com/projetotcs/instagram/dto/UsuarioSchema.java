package com.projetotcs.instagram.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioSchema {
    private String id;
    private String nomeCompleto;
    private String usuario;
    private String email;
    private String biografia;
    private String fotoUrl;
}
