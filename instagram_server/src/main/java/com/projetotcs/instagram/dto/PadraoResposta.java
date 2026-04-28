package com.projetotcs.instagram.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PadraoResposta {
    private String status;
    private String codigo;
    private String mensagem;
    private Object dados;
}
