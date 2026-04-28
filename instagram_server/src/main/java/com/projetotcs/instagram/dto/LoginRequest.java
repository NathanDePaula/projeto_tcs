package com.projetotcs.instagram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
@Getter
public class LoginRequest {
    
    @NotBlank(message = "O usuário é obrigatório")
    @NotNull(message = "O usuário não pode ser nulo")
    @Size(min = 3, max = 30, message = "O usuário deve ter entre 3 e 30 caracteres")
    @Pattern(regexp = "^[a-z0-9_]+$", message = "Usuário só pode conter letras minúsculas, números e underline")
    private String usuario;
    
    @NotBlank(message = "A senha é obrigatória")
    @NotNull(message = "A senha não pode ser nula")
    @Size(min = 8, max = 24, message = "A senha deve ter entre 8 e 24 caracteres")
    private String senha;
}
