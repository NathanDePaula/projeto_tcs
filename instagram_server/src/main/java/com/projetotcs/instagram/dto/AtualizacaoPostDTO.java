package com.projetotcs.instagram.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtualizacaoPostDTO(
    @Size(max = 200, message = "A legenda deve ter no máximo 200 caracteres")
    String legenda
) {
}
