package com.projetotcs.instagram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UsuarioSchema(
    String id,
    
    @JsonProperty("nome_completo")
    String nomeCompleto,
    
    String usuario,
    
    String email,
    
    String biografia,
    
    @JsonProperty("foto_url")
    String fotoUrl
) {
}
