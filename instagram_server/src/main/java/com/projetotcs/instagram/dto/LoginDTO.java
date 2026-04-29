package com.projetotcs.instagram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginDTO(
    @NotBlank(message = "Dados incompletos")
    @NotNull(message = "Dados incompletos")
    String usuario,

    @NotBlank(message = "Dados incompletos")
    @NotNull(message = "Dados incompletos")
    String senha
) {
}
