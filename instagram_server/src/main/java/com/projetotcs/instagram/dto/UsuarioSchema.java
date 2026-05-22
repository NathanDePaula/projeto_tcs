package com.projetotcs.instagram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UsuarioSchema(
    Long id,
    
    @JsonProperty("nome")
    String nomeCompleto,
    
    String usuario,
    
    String email,
    
    String biografia,
    
    @JsonProperty("foto")
    String fotoUrl
) {
}
