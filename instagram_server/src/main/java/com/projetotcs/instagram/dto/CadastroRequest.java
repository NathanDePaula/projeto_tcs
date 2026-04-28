package com.projetotcs.instagram.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CadastroRequest {

    @NotBlank(message = "O usuário é obrigatório")
    @NotNull(message = "O usuário não pode ser nulo")
    @Size(min = 3, max = 30, message = "O usuário deve ter entre 3 e 30 caracteres")
    @Pattern(regexp = "^[a-z0-9_]+$", message = "Usuário só pode conter letras minúsculas, números e underline")
    private String usuario;
    
    @NotBlank(message = "O e-mail é obrigatório")
    @NotNull(message = "O e-mail não pode ser nulo")
    @Email(message = "Formato de e-mail inválido")
    @Size(min = 10, max = 35, message = "O e-mail deve ter entre 10 e 35 caracteres")
    private String email;
    
    @NotBlank(message = "A senha é obrigatória")
    @NotNull(message = "A senha não pode ser nula")
    @Size(min = 8, max = 24, message = "A senha deve ter entre 8 e 24 caracteres")
    private String senha;
    
    @NotBlank(message = "O nome é obrigatório")
    @NotNull(message = "O nome não pode ser nulo")
    @Size(min = 3, max = 60, message = "O nome deve ter entre 3 e 60 caracteres")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "Nome só pode conter letras e espaços")
    private String nome;

    @Size(max = 150, message = "A biografia deve ter no máximo 150 caracteres")
    private String biografia;
    
    private String fotoUrl;

}
