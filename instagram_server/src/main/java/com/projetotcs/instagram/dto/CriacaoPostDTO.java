package com.projetotcs.instagram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriacaoPostDTO(
    @NotBlank(message = "A imagem é obrigatória")
    @NotNull(message = "A imagem não pode ser nula")
    String imagem,

    @Size(max = 200, message = "A legenda deve ter no máximo 200 caracteres")
    String legenda
) {
}
